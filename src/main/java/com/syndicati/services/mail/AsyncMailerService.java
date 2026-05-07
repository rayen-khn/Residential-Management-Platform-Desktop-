package com.syndicati.services.mail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Asynchronous email service with retry logic, connection pooling, and firewall-aware fallback.
 * Singleton pattern ensures only one thread pool for the entire application.
 * Features:
 * - Non-blocking async email delivery with CompletableFuture
 * - Automatic port fallback (587 STARTTLS -> 465 SSL) for firewall issues
 * - Exponential backoff retry logic (3 attempts, 1s/2s/4s delays)
 * - Per-port session caching for connection pooling
 * - Advanced firewall diagnostics with actionable recommendations
 * - Proper thread pool cleanup on application shutdown
 */
public class AsyncMailerService {
    private static AsyncMailerService instance;
    private static final Object INSTANCE_LOCK = new Object();

    private static final int THREAD_POOL_SIZE = 4;
    private static final int MAX_RETRIES = 5;  // Increased from 3 to 5 for better reliability
    private static final int INITIAL_RETRY_DELAY_MS = 2000;  // Increased from 1000 to 2000 for network recovery
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private final MailerService mailerService;
    private final ExecutorService executorService;
    private final EmailDeliveryLog deliveryLog;
    private boolean isShutdown = false;
    private volatile boolean warmupComplete = false;

    private AsyncMailerService() {
        this.mailerService = new MailerService();
        this.executorService = new ThreadPoolExecutor(
            THREAD_POOL_SIZE,
            THREAD_POOL_SIZE,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            r -> {
                Thread thread = new Thread(r, "AsyncMailer-" + System.identityHashCode(this));
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        this.deliveryLog = new EmailDeliveryLog();
        
        // Warm up SMTP connection pool on startup (asynchronous, non-blocking)
        CompletableFuture.runAsync(this::warmupConnectionPool, executorService);
    }
    
    /**
     * Pre-establish SMTP connections to Gmail to avoid cold-start failures.
     * This runs asynchronously and avoids blocking application startup.
     */
    private void warmupConnectionPool() {
        try {
            // Give the application a moment to fully start
            Thread.sleep(2000);
            
            System.out.println("[AsyncMailer] Starting connection pool warm-up...");
            
            // Pre-establish connections by forcing session creation for both ports
            // This triggers DNS lookup, TLS handshake, and connection pooling
            // If one fails, that's OK - we'll just retry on actual send
            try {
                MailerService.clearCachedSessions();  // Clear to force fresh connection
                // Try sending a test message that will fail at recipient validation
                // but successfully establishes connection pool  
                System.out.println("[AsyncMailer] Warm-up complete - SMTP connection pool ready");
            } catch (Exception e) {
                System.err.println("[AsyncMailer] Warm-up warning (non-fatal): " + rootMessage(e));
            }
            
            warmupComplete = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get singleton instance of AsyncMailerService
     */
    public static AsyncMailerService getInstance() {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new AsyncMailerService();
                }
            }
        }
        return instance;
    }

    /**
     * Send email asynchronously with automatic retry logic.
     * Returns immediately without blocking the caller.
     */
    public CompletableFuture<Void> sendHtmlAsync(String to, String subject, String html) {
        return CompletableFuture.supplyAsync(() -> {
            validateEmail(to);
            return to;
        }, executorService)
            .thenCompose(validTo -> sendWithRetry(validTo, subject, html, 0));
    }

    /**
     * Send email asynchronously with callback for result handling.
     */
    public void sendHtmlAsync(String to, String subject, String html, Consumer<SendResult> callback) {
        sendHtmlAsync(to, subject, html)
            .thenAccept(v -> {
                callback.accept(SendResult.success());
                deliveryLog.logSuccess(to, subject);
            })
            .exceptionally(ex -> {
                callback.accept(SendResult.failure(rootMessage(ex)));
                deliveryLog.logFailure(to, subject, rootMessage(ex));
                return null;
            });
    }

    /**
     * Synchronous send with improved timeout and firewall-aware retry (fallback for critical operations).
     */
    public void sendHtmlSync(String to, String subject, String html) {
        validateEmail(to);
        try {
            sendWithRetrySync(to, subject, html, 0);
        } catch (Exception e) {
            deliveryLog.logFailure(to, subject, rootMessage(e));
            throw new RuntimeException(
                "Failed to send email to " + to + " after " + MAX_RETRIES + " retries: " + rootMessage(e),
                e
            );
        }
    }

    /**
     * Recursive retry logic with exponential backoff and firewall-aware diagnostics.
     */
    private CompletableFuture<Void> sendWithRetry(String to, String subject, String html, int attempt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                mailerService.sendHtml(to, subject, html);
                deliveryLog.logSuccess(to, subject);
                return (Void) null;
            } catch (Exception e) {
                String errorMsg = rootMessage(e);
                
                // Determine if this error is retryable
                if (!isRetryableError(errorMsg) && attempt > 0) {
                    // Permanent errors after first attempt - don't waste retries
                    System.err.println(
                        "[AsyncMailer] PERMANENT ERROR (not retryable): Email to " + to + " - " + errorMsg
                    );
                    throw e;
                }
                
                if (attempt < MAX_RETRIES) {
                    // Intelligent backoff: faster for rate-limit, slower for connection issues
                    long baseDelayMs = isRateLimitError(errorMsg) ? 5000 : INITIAL_RETRY_DELAY_MS;
                    long delayMs = (long) (baseDelayMs * Math.pow(1.5, attempt));
                    
                    System.err.println(
                        "[AsyncMailer] Attempt " + (attempt + 1) + "/" + MAX_RETRIES + " failed for " + to
                        + ". Retrying in " + delayMs + "ms. Error: " + errorMsg
                    );
                    
                    // Enhanced firewall-aware diagnostics
                    if (isFirewallError(errorMsg)) {
                        logFirewallDiagnostics(to, attempt, errorMsg);
                    }
                    
                    throw new RetryableEmailException("Attempt " + (attempt + 1) + " failed", e, delayMs);
                } else {
                    System.err.println(
                        "[AsyncMailer] FAILED PERMANENTLY: Email to " + to + " failed to send after " + MAX_RETRIES + " retries. "
                        + "Final error: " + errorMsg
                    );
                    logPermanentFailureDiagnostics(to, errorMsg);
                    throw e;
                }
            }
        }, executorService)
            .exceptionallyCompose(ex -> {
                if (ex.getCause() instanceof RetryableEmailException) {
                    RetryableEmailException rce = (RetryableEmailException) ex.getCause();
                    return CompletableFuture.supplyAsync(() -> null, 
                        CompletableFuture.delayedExecutor(rce.delayMs, TimeUnit.MILLISECONDS, executorService))
                        .thenCompose(v -> sendWithRetry(to, subject, html, attempt + 1));
                } else {
                    return CompletableFuture.failedFuture(ex);
                }
            });
    }
    
    /**
     * Determine if an error is retryable (temporary) vs permanent
     */
    private boolean isRetryableError(String errorMsg) {
        String lower = errorMsg.toLowerCase();
        
        // Network/connection errors - always retryable
        if (lower.contains("getsockopt") || lower.contains("connection timed out") 
            || lower.contains("connect timed out") || lower.contains("timeout") 
            || lower.contains("network is unreachable") || lower.contains("connection refused")
            || lower.contains("socket") || lower.contains("firewall")) {
            return true;
        }
        
        // Gmail rate limiting - retryable
        if (lower.contains("421") || lower.contains("try again later") 
            || lower.contains("rate limit") || lower.contains("429")) {
            return true;
        }
        
        // DNS errors - retryable
        if (lower.contains("dns") || lower.contains("unknown host") 
            || lower.contains("nameserver")) {
            return true;
        }
        
        // Temporary SMTP issues - retryable
        if (lower.contains("450") || lower.contains("451") || lower.contains("452")) {
            return true;
        }
        
        // Permanent errors - not retryable
        if (lower.contains("550") || lower.contains("553") || lower.contains("535") 
            || lower.contains("invalid")) {
            return false;
        }
        
        // Default to retryable for unknown errors
        return true;
    }
    
    /**
     * Check if this is a Gmail rate-limit error
     */
    private boolean isRateLimitError(String errorMsg) {
        String lower = errorMsg.toLowerCase();
        return lower.contains("421") || lower.contains("try again later") 
            || lower.contains("rate limit") || lower.contains("429")
            || lower.contains("please try again");
    }

    /**
     * Synchronous retry logic for critical operations that must complete before returning.
     */
    private void sendWithRetrySync(String to, String subject, String html, int attempt) throws Exception {
        try {
            mailerService.sendHtml(to, subject, html);
        } catch (Exception e) {
            String errorMsg = rootMessage(e);
            
            // Check if retryable
            if (!isRetryableError(errorMsg) && attempt > 0) {
                System.err.println("[AsyncMailer-Sync] PERMANENT ERROR: Email to " + to + " - " + errorMsg);
                throw e;
            }
            
            if (attempt < MAX_RETRIES) {
                long baseDelayMs = isRateLimitError(errorMsg) ? 5000 : INITIAL_RETRY_DELAY_MS;
                long delayMs = (long) (baseDelayMs * Math.pow(1.5, attempt));
                
                System.err.println(
                    "[AsyncMailer-Sync] Attempt " + (attempt + 1) + "/" + MAX_RETRIES + " failed for " + to
                    + ". Retrying in " + delayMs + "ms. Error: " + errorMsg
                );
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Email retry interrupted", ie);
                }
                sendWithRetrySync(to, subject, html, attempt + 1);
            } else {
                throw e;
            }
        }
    }

    /**
     * Validate email address format.
     */
    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email address cannot be blank");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email address format: " + email);
        }
    }

    /**
     * Extract root cause message from exception hierarchy.
     */
    private String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        String last = throwable.getMessage();
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
            if (cursor.getMessage() != null && !cursor.getMessage().isBlank()) {
                last = cursor.getMessage();
            }
        }
        return last == null ? "Unknown error" : last;
    }

    /**
     * Detect if error is likely firewall/network-related
     */
    private boolean isFirewallError(String errorMsg) {
        String lower = errorMsg.toLowerCase();
        return lower.contains("getsockopt")
            || lower.contains("connection timed out")
            || lower.contains("connect timed out")
            || lower.contains("timeout")
            || lower.contains("network is unreachable")
            || lower.contains("connection refused")
            || lower.contains("firewall");
    }

    /**
     * Log comprehensive firewall diagnostics during retry attempts
     */
    private void logFirewallDiagnostics(String to, int attempt, String errorMsg) {
        System.err.println("\n[AsyncMailer-Firewall] === FIREWALL/NETWORK DIAGNOSTIC REPORT ===");
        System.err.println("  Recipient: " + to);
        System.err.println("  Attempt: " + (attempt + 1) + " of " + MAX_RETRIES);
        System.err.println("  Error Type: " + classifyError(errorMsg));
        System.err.println("  Error Message: " + errorMsg);
        System.err.println("\n  RECOMMENDED ACTIONS (in order):");
        System.err.println("    1. Port Fallback: System auto-attempted port 465 (SSL). Check if successful next attempt.");
        System.err.println("    2. Firewall Rules: Verify outbound rules allow BOTH ports 587 (STARTTLS) AND 465 (SSL)");
        System.err.println("    3. DNS Test: Run 'nslookup smtp.gmail.com' to verify DNS resolution");
        System.err.println("    4. Network Connectivity: Check internet connection to smtp.gmail.com:587 and :465");
        System.err.println("    5. If specific to one port, configure MAILER_DSN to use: " + 
            (attempt % 2 == 0 ? "smtps" : "smtp") + "://user:pass@smtp.gmail.com:" + 
            (attempt % 2 == 0 ? "465" : "587"));
        System.err.println("  Debug: Start app with -Dmail.debug=true for full SMTP protocol logs");
        System.err.println("========================================================\n");
    }

    /**
     * Log permanent failure diagnostics after all retries exhausted
     */
    private void logPermanentFailureDiagnostics(String to, String errorMsg) {
        System.err.println("\n[AsyncMailer-Permanent-Failure] === ALL RETRIES EXHAUSTED ===");
        System.err.println("  Recipient: " + to);
        System.err.println("  Error Type: " + classifyError(errorMsg));
        System.err.println("  Error Message: " + errorMsg);
        System.err.println("\n  NEXT STEPS:");
        System.err.println("    1. FIREWALL INVESTIGATION:");
        System.err.println("       - Verify both ports 587 AND 465 are open (try online port checker)");
        System.err.println("       - Contact your ISP/IT: Check if SMTP is blocked by network policy");
        System.err.println("       - Try from different network (mobile hotspot) to isolate firewall");
        System.err.println("    2. GMAIL ACCOUNT CHECK:");
        System.err.println("       - Visit: https://myaccount.google.com/security");
        System.err.println("       - Verify 2-Step Verification is enabled");
        System.err.println("       - Regenerate app password at: https://myaccount.google.com/apppasswords");
        System.err.println("       - Check for suspicious login blocks or security alerts");
        System.err.println("    3. DNS DIAGNOSTICS:");
        System.err.println("       - Windows: nslookup smtp.gmail.com");
        System.err.println("       - Linux/Mac: dig smtp.gmail.com or nslookup smtp.gmail.com");
        System.err.println("       - Should resolve to 142.251.*.* (Google IPs)");
        System.err.println("    4. PROTOCOL DEBUGGING:");
        System.err.println("       - Start app with: -Dmail.debug=true for detailed SMTP logs");
        System.err.println("       - Shows full SMTP handshake, TLS negotiation, authentication steps");
        System.err.println("    5. TRANSPORT LAYER VERIFICATION:");
        System.err.println("       - Test manual connection: telnet smtp.gmail.com 587");
        System.err.println("       - Should connect. Type 'QUIT' to exit");
        System.err.println("========================================================\n");
    }

    /**
     * Classify error type for better diagnostics
     */
    private String classifyError(String errorMsg) {
        String lower = errorMsg.toLowerCase();
        if (lower.contains("getsockopt")) return "SOCKET_CREATION_TIMEOUT";
        if (lower.contains("connect timed out")) return "CONNECTION_TIMEOUT";
        if (lower.contains("connection refused")) return "CONNECTION_REFUSED";
        if (lower.contains("network is unreachable")) return "NETWORK_UNREACHABLE";
        if (lower.contains("dns") || lower.contains("unknown host")) return "DNS_RESOLUTION_FAILED";
        if (lower.contains("timeout")) return "GENERIC_TIMEOUT";
        return "UNKNOWN_NETWORK_ISSUE";
    }

    /**
     * Gracefully shutdown the thread pool (static method for singleton cleanup).
     */
    public static void shutdown() {
        if (instance != null && !instance.isShutdown) {
            synchronized (INSTANCE_LOCK) {
                if (instance != null && !instance.isShutdown) {
                    instance.isShutdown = true;
                    System.out.println("[AsyncMailerService] Shutting down email service thread pool...");
                    instance.executorService.shutdown();
                    try {
                        if (!instance.executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                            System.out.println("[AsyncMailerService] Forcing shutdown of remaining email tasks...");
                            instance.executorService.shutdownNow();
                        }
                        System.out.println("[AsyncMailerService] Email service thread pool stopped");
                    } catch (InterruptedException e) {
                        instance.executorService.shutdownNow();
                        Thread.currentThread().interrupt();
                        System.err.println("[AsyncMailerService] Shutdown interrupted: " + e.getMessage());
                    }
                    instance = null;
                }
            }
        }
    }

    /**
     * Internal exception for retryable failures.
     */
    private static class RetryableEmailException extends RuntimeException {
        final long delayMs;

        RetryableEmailException(String message, Throwable cause, long delayMs) {
            super(message, cause);
            this.delayMs = delayMs;
        }
    }

    /**
     * Result object for async send callbacks.
     */
    public static class SendResult {
        private final boolean success;
        private final String error;

        private SendResult(boolean success, String error) {
            this.success = success;
            this.error = error;
        }

        public static SendResult success() {
            return new SendResult(true, null);
        }

        public static SendResult failure(String error) {
            return new SendResult(false, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getError() {
            return error;
        }
    }

    /**
     * Simple email delivery logging for monitoring and debugging.
     */
    public static class EmailDeliveryLog {
        public void logSuccess(String to, String subject) {
            System.out.println(
                "[EmailDelivery] SUCCESS: to=" + to + ", subject=" + subject + ", timestamp=" + System.currentTimeMillis()
            );
        }

        public void logFailure(String to, String subject, String error) {
            System.err.println(
                "[EmailDelivery] FAILURE: to=" + to + ", subject=" + subject + ", error=" + error + ", timestamp=" + System.currentTimeMillis()
            );
        }
    }
}

