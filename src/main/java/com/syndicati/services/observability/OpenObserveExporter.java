package com.syndicati.services.observability;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.syndicati.models.log.AppEventLog;
import okhttp3.*;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.Base64;

/**
 * OpenObserve log exporter service.
 *
 * Asynchronously exports AppEventLog entries to OpenObserve via HTTP API.
 * Implements:
 * - Batch ingestion (groups logs before sending)
 * - Retry with exponential backoff
 * - Circuit breaker pattern (fails gracefully after N consecutive errors)
 * - Thread-safe async processing
 *
 * Configuration via application.local.properties:
 *   openobserve.enabled=true
 *   openobserve.url=http://localhost:5080
 *   openobserve.username=admin
 *   openobserve.password=Complexpass#123
 *   openobserve.stream_name=syndicati
 *   openobserve.batch_size=10
 *   openobserve.batch_timeout_ms=5000
 */
public class OpenObserveExporter {

    private static final Gson gson = new Gson();
    private static final String LOG_TAG = "[OpenObserveExporter]";

    private final OpenObserveConfig config;
    private final OkHttpClient httpClient;
    private final ScheduledExecutorService executorService;
    private final Queue<AppEventLog> batchQueue;
    private final Object batchLock = new Object();
    private volatile boolean circuitBreakerOpen = false;
    private volatile int consecutiveErrors = 0;
    private long lastBatchTime = System.currentTimeMillis();

    // Retry configuration
    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_BACKOFF_MS = 100;
    private static final int MAX_BACKOFF_MS = 5000;
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;

    public OpenObserveExporter(OpenObserveConfig config) {
        this.config = config;
        this.batchQueue = new ConcurrentLinkedQueue<>();
        this.executorService = Executors.newScheduledThreadPool(2, runnable -> {
            Thread t = new Thread(runnable, "OpenObserveExporter-Thread");
            t.setDaemon(true);
            return t;
        });

        // Create HTTP client with timeouts
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .readTimeout(java.time.Duration.ofSeconds(15))
                .writeTimeout(java.time.Duration.ofSeconds(15))
                .build();

        // Start background batch processor
        startBatchProcessor();
    }

    /**
     * Export a single log entry asynchronously.
     * The entry is queued and will be batched with others.
     */
    public void export(AppEventLog log) {
        if (!config.isEnabled()) {
            return;
        }

        if (circuitBreakerOpen) {
            System.out.println(LOG_TAG + " Circuit breaker is open, skipping export");
            return;
        }

        try {
            batchQueue.offer(log);
            checkAndFlushBatch();
        } catch (Exception e) {
            System.out.println(LOG_TAG + " Error queueing log: " + e.getMessage());
        }
    }

    /**
     * Export multiple logs at once.
     */
    public void exportBatch(List<AppEventLog> logs) {
        if (!config.isEnabled() || logs == null || logs.isEmpty()) {
            return;
        }

        logs.forEach(this::export);
    }

    /**
     * Check if batch should be flushed based on size or timeout.
     */
    private void checkAndFlushBatch() {
        synchronized (batchLock) {
            long now = System.currentTimeMillis();
            boolean shouldFlush = false;

            // Flush if batch size reached
            if (batchQueue.size() >= config.getBatchSize()) {
                shouldFlush = true;
            }

            // Flush if timeout exceeded
            if (now - lastBatchTime > config.getBatchTimeoutMs() && !batchQueue.isEmpty()) {
                shouldFlush = true;
            }

            if (shouldFlush) {
                flushBatchAsync();
            }
        }
    }

    /**
     * Start the background batch processor thread.
     */
    private void startBatchProcessor() {
        executorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(config.getBatchTimeoutMs());
                    checkAndFlushBatch();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.out.println(LOG_TAG + " Error in batch processor: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Flush queued logs asynchronously.
     */
    private void flushBatchAsync() {
        executorService.submit(() -> {
            synchronized (batchLock) {
                if (batchQueue.isEmpty()) {
                    return;
                }

                List<AppEventLog> batch = new ArrayList<>();
                AppEventLog log;
                while ((log = batchQueue.poll()) != null && batch.size() < config.getBatchSize()) {
                    batch.add(log);
                }

                lastBatchTime = System.currentTimeMillis();

                if (!batch.isEmpty()) {
                    sendWithRetry(batch, 0);
                }
            }
        });
    }

    /**
     * Send batch to OpenObserve with retry logic.
     */
    private void sendWithRetry(List<AppEventLog> batch, int attempt) {
        try {
            String payload = buildPayload(batch);
            Request request = buildRequest(payload);
            Response response = httpClient.newCall(request).execute();

            if (response.isSuccessful()) {
                consecutiveErrors = 0;
                System.out.println(LOG_TAG + " Exported " + batch.size() + " logs to OpenObserve");
                response.close();
            } else {
                handleError(batch, attempt, "HTTP " + response.code() + ": " + response.message(), response.body() != null ? response.body().string() : "");
                response.close();
            }
        } catch (IOException e) {
            handleError(batch, attempt, e.getMessage(), "");
        }
    }

    /**
     * Handle export errors with retry or circuit breaker.
     */
    private void handleError(List<AppEventLog> batch, int attempt, String error, String responseBody) {
        consecutiveErrors++;

        if (consecutiveErrors >= CIRCUIT_BREAKER_THRESHOLD) {
            circuitBreakerOpen = true;
            System.out.println(LOG_TAG + " CIRCUIT BREAKER OPEN after " + consecutiveErrors + " errors. Stopping exports.");
            // Reset after cool-down
            executorService.schedule(this::resetCircuitBreaker, 60, TimeUnit.SECONDS);
            return;
        }

        if (attempt < MAX_RETRIES) {
            int backoffMs = Math.min(INITIAL_BACKOFF_MS * (int) Math.pow(2, attempt), MAX_BACKOFF_MS);
            System.out.println(LOG_TAG + " Retrying in " + backoffMs + "ms (attempt " + (attempt + 1) + ") after: " + error);
            executorService.schedule(() -> sendWithRetry(batch, attempt + 1), backoffMs, TimeUnit.MILLISECONDS);
        } else {
            System.out.println(LOG_TAG + " Max retries exceeded. Dropping batch. Error: " + error);
            if (!responseBody.isEmpty()) {
                System.out.println(LOG_TAG + " Response: " + responseBody);
            }
        }
    }

    /**
     * Reset circuit breaker (called after cool-down).
     */
    private void resetCircuitBreaker() {
        circuitBreakerOpen = false;
        consecutiveErrors = 0;
        System.out.println(LOG_TAG + " Circuit breaker reset, resuming exports");
    }

    /**
     * Build OpenObserve HTTP request.
     */
    private Request buildRequest(String payload) {
        String url = config.getUrl().replaceAll("/$", "") + "/api/default/_json_post";

        RequestBody body = RequestBody.create(payload, MediaType.parse("application/json"));
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "SyndicatiObservability/1.0");

        // Add auth header if configured
        if (config.getUsername() != null && config.getPassword() != null) {
            String credentials = Base64.getEncoder().encodeToString(
                    (config.getUsername() + ":" + config.getPassword()).getBytes());
            builder.addHeader("Authorization", "Basic " + credentials);
        }

        return builder.build();
    }

    /**
     * Build JSON payload for OpenObserve batch ingestion.
     * Format: [{...event1...}, {...event2...}] as JSON array
     */
    private String buildPayload(List<AppEventLog> logs) {
        JsonArray array = new JsonArray();

        for (AppEventLog log : logs) {
            JsonObject obj = logToJson(log);
            array.add(obj);
        }

        return gson.toJson(array);
    }

    /**
     * Convert AppEventLog to JSON for OpenObserve ingestion.
     */
    private JsonObject logToJson(AppEventLog log) {
        JsonObject obj = new JsonObject();

        // Timestamp (required by OpenObserve, in milliseconds)
        long timestamp = log.getEventTimestamp() != null
                ? log.getEventTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                : System.currentTimeMillis();
        obj.addProperty("_timestamp", timestamp);

        // Main fields
        obj.addProperty("eventId", log.getEventId());
        obj.addProperty("eventType", log.getEventType());
        obj.addProperty("category", log.getCategory());
        obj.addProperty("action", log.getAction());
        obj.addProperty("outcome", log.getOutcome());
        obj.addProperty("level", log.getLevel());
        obj.addProperty("message", log.getMessage());

        // Correlation fields
        obj.addProperty("traceId", log.getTraceId());
        obj.addProperty("spanId", log.getSpanId());
        obj.addProperty("requestId", log.getRequestId());
        obj.addProperty("sessionId", log.getSessionId());

        // User/Source
        if (log.getUser() != null && log.getUser().getIdUser() != null) {
            obj.addProperty("userId", log.getUser().getIdUser());
            obj.addProperty("userEmail", log.getUser().getEmailUser());
        }
        obj.addProperty("ipAddress", log.getIpAddress());
        obj.addProperty("userAgent", log.getUserAgent());

        // Context
        obj.addProperty("entityType", log.getEntityType());
        obj.addProperty("entityId", log.getEntityId());
        obj.addProperty("serviceName", log.getServiceName());
        obj.addProperty("environment", log.getEnvironment());
        obj.addProperty("applicationVersion", log.getApplicationVersion());

        // Performance & Scoring
        if (log.getDurationMs() != null) {
            obj.addProperty("durationMs", log.getDurationMs());
        }
        if (log.getRiskScore() != null) {
            obj.addProperty("riskScore", log.getRiskScore());
        }
        if (log.getAnomalyScore() != null) {
            obj.addProperty("anomalyScore", log.getAnomalyScore());
        }

        // Metadata (parse if valid JSON, otherwise as string)
        if (log.getMetadataJson() != null && !log.getMetadataJson().isEmpty() && !log.getMetadataJson().equals("{}")) {
            try {
                obj.add("metadata", gson.fromJson(log.getMetadataJson(), com.google.gson.JsonElement.class));
            } catch (Exception e) {
                obj.addProperty("metadata", log.getMetadataJson());
            }
        }

        return obj;
    }

    /**
     * Force flush all queued logs immediately.
     */
    public void flush() {
        checkAndFlushBatch();
    }

    /**
     * Shutdown the exporter gracefully.
     */
    public void shutdown() {
        flush();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        httpClient.dispatcher().executorService().shutdown();
    }

    public boolean isCircuitBreakerOpen() {
        return circuitBreakerOpen;
    }

    public int getQueueSize() {
        return batchQueue.size();
    }
}
