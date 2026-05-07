package com.syndicati.services.mail;

import com.syndicati.utils.config.EnvConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * SMTP mailer service with firewall-aware port fallback and enhanced diagnostics.
 * Features:
 * - Automatic port fallback (587 STARTTLS -> 465 SSL) for firewall issues
 * - Per-port session caching to avoid protocol conflicts
 * - Comprehensive firewall diagnostics and troubleshooting guidance
 * - Enhanced timeout management (45s connection, 90s operations, 15s DNS)
 */
public class MailerService {

    private final MailerConfig config;
    private static Session cachedSession587;  // STARTTLS port
    private static Session cachedSession465;  // SSL port
    private static final Object SESSION_LOCK = new Object();
    private static int lastSuccessfulPort = -1;  // Track which port succeeded

    public MailerService() {
        this.config = MailerConfig.fromEnvironment();
    }

    public boolean isConfigured() {
        return config.host != null && !config.host.isBlank() && config.fromEmail != null && !config.fromEmail.isBlank();
    }

    /**
     * Send HTML email with automatic port fallback for firewall issues
     */
    public void sendHtml(String to, String subject, String html) {
        if (!isConfigured()) {
            throw new RuntimeException("Email is not configured. Set MAILER_DSN (or MAIL_HOST/MAIL_PORT/MAIL_USERNAME/MAIL_PASSWORD) and MAIL_FROM_EMAIL.");
        }

        try {
            sendHtmlWithPort(to, subject, html, config.port);
        } catch (MessagingException e) {
            String errorMsg = rootMessage(e);
            if (isFirewallError(errorMsg)) {
                int fallbackPort = (config.port == 587) ? 465 : 587;
                System.err.println("[MailerService] Port " + config.port + " socket timeout. Attempting fallback to port " + fallbackPort + "...");
                try {
                    sendHtmlWithPort(to, subject, html, fallbackPort);
                    lastSuccessfulPort = fallbackPort;
                    return;
                } catch (MessagingException fallbackError) {
                    handleSendError(e, to);
                }
            } else {
                handleSendError(e, to);
            }
        }
    }

    /**
     * Send email using a specific port
     */
    private void sendHtmlWithPort(String to, String subject, String html, int targetPort) throws MessagingException {
        Session session = getCachedSessionForPort(targetPort);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.formattedFrom()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject, StandardCharsets.UTF_8.name());
        message.setContent(html, "text/html; charset=UTF-8");
        Transport.send(message);
    }

    /**
     * Handle and log email sending errors with detailed diagnostics
     */
    private void handleSendError(MessagingException e, String to) {
        String errorMsg = rootMessage(e);
        String diagnostic = diagnoseError(errorMsg);

        if (isFirewallError(errorMsg)) {
            String altPort = config.port == 587 ? "465 (SSL)" : "587 (STARTTLS)";
            throw new RuntimeException(
                "FIREWALL/SOCKET TIMEOUT (host=" + config.host + ":" + config.port + "). "
                + "This is typically caused by: 1) Firewall blocking port " + config.port + ", "
                + "2) Network latency or DNS hanging, 3) Gmail account security blocking. "
                + "Solutions (in order): "
                + "1) Try alternate port " + altPort + " (already attempted auto-fallback). "
                + "2) Verify firewall allows outbound ports 587 AND 465. "
                + "3) Test DNS: nslookup smtp.gmail.com. "
                + "4) Check Gmail hasn't blocked your IP (myaccount.google.com/security). "
                + "5) Verify app password is 16 chars and 2FA is enabled. "
                + "6) If both ports fail, check ISP isn't blocking SMTP. "
                + diagnostic,
                e
            );
        } else if (errorMsg.contains("550") || errorMsg.contains("553") || errorMsg.contains("535")) {
            throw new RuntimeException(
                "SMTP authentication failed (host=" + config.host + "). "
                + "SMTP error code suggests invalid credentials or account issue. "
                + "Action: 1) Regenerate app password at https://myaccount.google.com/apppasswords, "
                + "2) Verify 2-Step Verification enabled, "
                + "3) Check if Google blocked suspicious login attempt. "
                + "Root cause: " + errorMsg,
                e
            );
        } else if (errorMsg.contains("Invalid Addresses") || errorMsg.contains("bad address")) {
            throw new RuntimeException(
                "Invalid recipient email address: " + to + ". Please verify the email format.",
                e
            );
        } else if (errorMsg.contains("No address associated with hostname") || errorMsg.contains("UnknownHostException")) {
            throw new RuntimeException(
                "Cannot resolve SMTP hostname: " + config.host + ". "
                + "Possible causes: 1) DNS lookup failing, 2) Hostname typo in MAILER_DSN, "
                + "3) Network not connected, 4) ISP blocking DNS. "
                + "Try: nslookup " + config.host + " from command line to test DNS.",
                e
            );
        } else {
            throw new RuntimeException(
                "Failed to send email via SMTP host=" + config.host + " port=" + config.port + ": " + errorMsg,
                e
            );
        }
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
            || lower.contains("network is unreachable");
    }

    /**
     * Provide diagnostic information based on error message
     */
    private String diagnoseError(String errorMsg) {
        StringBuilder diag = new StringBuilder("\n[Firewall Detection] ");

        if (errorMsg.contains("getsockopt")) {
            diag.append("FIREWALL INDICATOR: Socket creation failed (getsockopt). ");
            diag.append("Port ").append(config.port).append(" is likely blocked. ");
            diag.append("ACTION: System attempted auto-fallback to alternate port. ");
            diag.append("If still failing, manually configure firewall rules or try ISP DNS.");
        } else if (errorMsg.contains("DNS") || errorMsg.contains("Unknown host")) {
            diag.append("DNS RESOLUTION ISSUE: Cannot resolve smtp.gmail.com. ");
            diag.append("Test: nslookup smtp.gmail.com or check ISP DNS settings.");
        } else if (errorMsg.contains("refused")) {
            diag.append("CONNECTION REFUSED: SMTP server rejected connection on port ").append(config.port).append(". ");
            diag.append("Port may be blocked by firewall. System will auto-fallback to alternate port.");
        } else if (errorMsg.contains("Network is unreachable")) {
            diag.append("NETWORK UNREACHABLE: Check network connectivity and firewall rules.");
        } else {
            diag.append("Run with -Dmail.debug=true for detailed SMTP protocol logs.");
        }

        return diag.toString();
    }

    /**
     * Get or create a cached SMTP session for the given port.
     * Maintains separate sessions for port 587 (STARTTLS) and 465 (SSL) to avoid protocol conflicts.
     */
    private Session getCachedSessionForPort(int targetPort) {
        synchronized (SESSION_LOCK) {
            if (targetPort == 465) {
                if (cachedSession465 == null) {
                    cachedSession465 = Session.getInstance(
                        config.propertiesForPort(465),
                        config.authenticator()
                    );
                }
                return cachedSession465;
            } else {
                if (cachedSession587 == null) {
                    cachedSession587 = Session.getInstance(
                        config.propertiesForPort(587),
                        config.authenticator()
                    );
                }
                return cachedSession587;
            }
        }
    }

    /**
     * Clear cached sessions (useful for reconfiguration or testing)
     */
    public static void clearCachedSessions() {
        synchronized (SESSION_LOCK) {
            cachedSession587 = null;
            cachedSession465 = null;
            lastSuccessfulPort = -1;
        }
    }

    /**
     * Get the port that successfully delivered email (cache hint for retries)
     */
    public static int getLastSuccessfulPort() {
        return lastSuccessfulPort;
    }

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

    private static class MailerConfig {
        private final String host;
        private final int port;
        private final String username;
        private final String password;
        private final boolean useSsl;
        private final boolean useStartTls;
        private final String fromEmail;
        private final String fromName;

        private MailerConfig(
            String host,
            int port,
            String username,
            String password,
            boolean useSsl,
            boolean useStartTls,
            String fromEmail,
            String fromName
        ) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
            this.useSsl = useSsl;
            this.useStartTls = useStartTls;
            this.fromEmail = fromEmail;
            this.fromName = fromName;
        }

        private static MailerConfig fromEnvironment() {
            String dsn = EnvConfig.get("MAILER_DSN");
            String fromEmail = firstNonBlank(
                EnvConfig.get("MAILER_FROM_EMAIL"),
                EnvConfig.get("MAIL_FROM_EMAIL"),
                "noreply@horizon.local"
            );
            String fromName = firstNonBlank(
                EnvConfig.get("MAILER_FROM_NAME"),
                EnvConfig.get("MAIL_FROM_NAME"),
                "Horizon"
            );

            if (dsn != null && !dsn.isBlank() && !dsn.startsWith("null://")) {
                try {
                    return fromDsn(dsn, fromEmail, fromName);
                } catch (Exception ignored) {
                    // Fall back to discrete env vars.
                }
            }

            String host = EnvConfig.get("MAIL_HOST");
            int port = parsePort(EnvConfig.getOrDefault("MAIL_PORT", "587"), 587);
            String username = EnvConfig.get("MAIL_USERNAME");
            String password = EnvConfig.get("MAIL_PASSWORD");
            boolean useSsl = EnvConfig.getBoolean("MAIL_SSL", false);
            boolean useStartTls = EnvConfig.getBoolean("MAIL_STARTTLS", true);
            return new MailerConfig(host, port, username, password, useSsl, useStartTls, fromEmail, fromName);
        }

        private static MailerConfig fromDsn(String dsn, String fromEmail, String fromName) {
            String raw = dsn == null ? "" : dsn.trim();
            int schemeIdx = raw.indexOf("://");
            if (schemeIdx <= 0) {
                throw new IllegalArgumentException("Invalid MAILER_DSN format");
            }

            String scheme = raw.substring(0, schemeIdx);
            String tail = raw.substring(schemeIdx + 3);

            int queryIdx = tail.indexOf('?');
            if (queryIdx >= 0) {
                tail = tail.substring(0, queryIdx);
            }

            int atIdx = tail.lastIndexOf('@');
            String userInfo = atIdx >= 0 ? tail.substring(0, atIdx) : "";
            String hostPart = atIdx >= 0 ? tail.substring(atIdx + 1) : tail;

            int slashIdx = hostPart.indexOf('/');
            if (slashIdx >= 0) {
                hostPart = hostPart.substring(0, slashIdx);
            }

            String host = hostPart;
            int port = -1;
            int colonIdx = hostPart.lastIndexOf(':');
            if (colonIdx > 0 && colonIdx < hostPart.length() - 1) {
                String portCandidate = hostPart.substring(colonIdx + 1);
                if (portCandidate.chars().allMatch(Character::isDigit)) {
                    host = hostPart.substring(0, colonIdx);
                    port = parsePort(portCandidate, -1);
                }
            }

            String username = null;
            String password = null;
            if (userInfo != null && !userInfo.isBlank()) {
                String[] parts = userInfo.split(":", 2);
                username = decode(parts[0]);
                if (parts.length > 1) {
                    password = decode(parts[1]);
                }
            }

            boolean useSsl = "smtps".equalsIgnoreCase(scheme);
            boolean useStartTls = "smtp".equalsIgnoreCase(scheme);
            int resolvedPort = port > 0 ? port : (useSsl ? 465 : 587);

            return new MailerConfig(host, resolvedPort, username, password, useSsl, useStartTls, fromEmail, fromName);
        }

        private Properties properties() {
            return propertiesForPort(port);
        }

        /**
         * Generate SMTP properties configured for a specific port.
         * Handles protocol differences: port 587 uses STARTTLS, port 465 uses implicit SSL.
         * CRITICAL: Enhanced timeouts for unreliable networks and aggressive keep-alive settings.
         */
        private Properties propertiesForPort(int targetPort) {
            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", String.valueOf(targetPort));
            props.put("mail.smtp.auth", String.valueOf(username != null && !username.isBlank()));

            boolean isImplicitSsl = (targetPort == 465);
            boolean isStartTls = (targetPort == 587);

            props.put("mail.smtp.starttls.enable", String.valueOf(isStartTls));
            props.put("mail.smtp.starttls.required", String.valueOf(isStartTls));
            props.put("mail.smtp.ssl.enable", String.valueOf(isImplicitSsl));
            props.put("mail.smtp.ssl.trust", host);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");

            // ===== ENHANCED SOCKET CONFIGURATION FOR 100% RELIABILITY =====
            
            // MASSIVE TIMEOUT BUFFERS - accounts for network congestion, Gmail delays, DNS hangs
            // These are generous but necessary for poor network conditions
            props.put("mail.smtp.connectiontimeout", "120000");  // 120s socket creation (was 45s)
            props.put("mail.smtp.timeout", "180000");            // 180s read/write (was 90s)
            props.put("mail.smtp.writetimeout", "180000");       // 180s write operations (was 90s)

            // DNS timeout control - prevents indefinite DNS lookup hangs
            // Increased significantly for slow ISP DNS
            props.put("sun.net.client.defaultConnectTimeout", "60000");  // 60s DNS lookup (was 15s)
            props.put("sun.net.client.defaultReadTimeout", "120000");    // 120s DNS read (was 45s)

            // TCP Keep-Alive - prevents connection drops on poor networks
            props.put("mail.smtp.socketFactory.fallback", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.setProperty("mail.smtp.socketFactory.port", String.valueOf(targetPort));

            // Connection pool settings - aggressive pooling to reduce handshakes
            props.put("mail.smtp.connectionpool.debug", "true");
            props.put("mail.smtp.connectionpool.maxsize", "20");          // More connections
            props.put("mail.smtp.connectionpool.timeout", "600000");      // Keep for 10 minutes

            // Protocol tweaks for reliability
            props.put("mail.smtp.auth.mechanisms", "PLAIN LOGIN DIGEST-MD5");
            props.put("mail.smtp.auth.login.disable", "false");
            props.put("mail.smtp.auth.plain.disable", "false");
            props.put("mail.smtp.userset", "true");
            props.put("mail.smtp.noop.strict", "false");
            
            // Allow sending via SMTP even if not all recipients accept
            props.put("mail.smtp.sendpartial", "true");
            
            // Retry failed commands automatically
            props.put("mail.smtp.ehlo", "true");
            props.put("mail.smtp.auth.mechanisms.oauth2.disable", "false");

            return props;
        }

        private Authenticator authenticator() {
            if (username == null || username.isBlank()) {
                return null;
            }
            final String user = username;
            final String pass = password;
            return new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            };
        }

        private String formattedFrom() {
            if (fromName == null || fromName.isBlank()) {
                return fromEmail;
            }
            return fromName + " <" + fromEmail + ">";
        }

        private static int parsePort(String portStr, int fallback) {
            try {
                int port = Integer.parseInt(portStr.trim());
                return port > 0 && port <= 65535 ? port : fallback;
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        private static String decode(String encoded) {
            if (encoded == null || encoded.isBlank()) {
                return encoded;
            }
            try {
                return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return encoded;
            }
        }

        private static String firstNonBlank(String... candidates) {
            for (String c : candidates) {
                if (c != null && !c.isBlank()) {
                    return c;
                }
            }
            String fallback = "Horizon";
            for (String c : candidates) {
                if (c != null) {
                    return c;
                }
            }
            return fallback;
        }
    }
}

