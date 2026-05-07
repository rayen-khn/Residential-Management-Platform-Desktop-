package com.syndicati.services.analytics;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.syndicati.models.log.AppEventLog;
import com.syndicati.models.log.data.AppEventLogRepository;
import com.syndicati.services.observability.LogAIConfig;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.math.BigDecimal;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Integrates with a LogAI worker to score anomaly risk for recent events.
 * Falls back to deterministic heuristics when worker is unavailable.
 */
public class AnomalyScoreService {

    private static final Gson GSON = new Gson();
    private static final String LOG_TAG = "[AnomalyScoreService]";

    private final AppEventLogRepository repository;
    private final LogAIConfig config;
    private final OkHttpClient httpClient;

    public AnomalyScoreService() {
        this(new AppEventLogRepository(), new LogAIConfig(), null);
    }

    public AnomalyScoreService(AppEventLogRepository repository, LogAIConfig config, OkHttpClient httpClient) {
        this.repository = repository;
        this.config = config;
        this.httpClient = httpClient == null
                ? new OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofSeconds(config.getTimeoutSeconds()))
                .readTimeout(java.time.Duration.ofSeconds(config.getTimeoutSeconds()))
                .writeTimeout(java.time.Duration.ofSeconds(config.getTimeoutSeconds()))
                .build()
                : httpClient;
    }

    /**
     * Score default batch size from configuration.
     */
    public ScoreSummary scoreRecentEvents() {
        return scoreRecentEvents(config.getBatchSize());
    }

    /**
     * Score recent unscored events and persist anomaly score + metadata hints.
     */
    public ScoreSummary scoreRecentEvents(int limit) {
        int batchLimit = Math.max(1, Math.min(limit, config.getBatchSize()));
        List<AppEventLog> events = repository.findUnscoredEvents(batchLimit);

        ScoreSummary summary = new ScoreSummary();
        summary.processed = events.size();

        if (events.isEmpty()) {
            return summary;
        }

        List<AnomalyResult> results;
        boolean usedFallback = false;

        if (config.isEnabled()) {
            try {
                results = scoreWithWorker(events);
            } catch (Exception e) {
                usedFallback = true;
                System.out.println(LOG_TAG + " Worker scoring failed, using fallback heuristics: " + e.getMessage());
                results = scoreWithHeuristics(events);
            }
        } else {
            usedFallback = true;
            results = scoreWithHeuristics(events);
        }

        summary.usedFallback = usedFallback;
        summary.anomaliesDetected = (int) results.stream().filter(r -> r.anomalyScore >= config.getAnomalyThreshold()).count();

        Map<Long, AppEventLog> byId = events.stream()
                .filter(e -> e.getId() != null)
                .collect(Collectors.toMap(AppEventLog::getId, e -> e, (left, right) -> left, LinkedHashMap::new));

        for (AnomalyResult result : results) {
            AppEventLog original = byId.get(result.eventId);
            if (original == null) {
                summary.failed++;
                continue;
            }

            String mergedMetadata = mergeAnomalyMetadata(original.getMetadataJson(), result);
            boolean updated = repository.updateAnomalyData(
                    result.eventId,
                    BigDecimal.valueOf(result.anomalyScore),
                    mergedMetadata
            );

            if (updated) {
                summary.updated++;
            } else {
                summary.failed++;
            }
        }

        return summary;
    }

    /**
     * Simple heuristic-based anomaly scoring used as fallback.
     * Returns score 0.0 (normal) to 1.0 (anomalous)
     */
    public double computeHeuristicAnomalyScore(AppEventLog log) {
        double score = 0.0;
        LocalDateTime now = log.getEventTimestamp() != null ? log.getEventTimestamp() : LocalDateTime.now();

        // 1. Authentication & Security Alerts (Highest Risk)
        if ("AUTH_FAILURE".equals(log.getEventType())) {
            score += 0.85;  // Brute force / Auth failures are high risk
        }
        if ("SECURITY_ALERT".equals(log.getEventType())) {
            score += 0.90;  // Explicit security alerts are critical
        }
        
        // 1b. Honeypot Triggers (Highest possible risk)
        if (log.getMetadataJson() != null && log.getMetadataJson().contains("\"honeypot\":true")) {
            return 1.0; // Immediate disqualification/red flag
        }

        // 2. Sensitive Entity Access (Medium-High Risk)
        if ("USER".equals(log.getEntityType()) || "FINANCE".equals(log.getEntityType()) || "SYNDICAT".equals(log.getEntityType())) {
            if ("DELETE".equals(log.getEventType()) || "UPDATE".equals(log.getEventType())) {
                score += 0.45;
            } else if ("DATA_EXPORT".equals(log.getEventType())) {
                score += 0.60; // Exporting sensitive data is risky
            }
        }

        // 3. Time-based Anomaly (The "Night Owl" rule)
        // If an admin/user does sensitive work between 1 AM and 5 AM
        int hour = now.getHour();
        if (hour >= 1 && hour <= 5) {
            if ("AUTH".equals(log.getCategory()) || "CRUD".equals(log.getCategory())) {
                score += 0.35;
            }
        }

        // 4. Interaction Spams or Rapid Clicks
        if ("UI_CLICK".equals(log.getEventType()) && log.getMessage() != null && log.getMessage().contains("spam")) {
            score += 0.5;
        }

        // 5. Bulk Operations
        if ("DELETE".equals(log.getEventType()) && (log.getMessage() != null && (log.getMessage().contains("bulk") || log.getMessage().contains("all")))) {
            score += 0.7;
        }

        // 6. Failure outcomes on sensitive actions
        if ("FAILURE".equals(log.getOutcome())) {
            if ("DELETE".equals(log.getEventType()) || "UPDATE".equals(log.getEventType()) || "AUTH".equals(log.getCategory())) {
                score += 0.4;
            } else {
                score += 0.2;
            }
        }

        // 7. Metadata-based risks
        if (log.getMetadataJson() != null) {
            String meta = log.getMetadataJson().toLowerCase();
            if (meta.contains("\"failure_count\"")) score += 0.3;
            if (meta.contains("sql injection") || meta.contains("xss")) score += 0.95; // Critical threats
            if (meta.contains("impossible_travel")) score += 0.80;
        }

        return Math.min(score, 1.0);  // Cap at 1.0
    }

    private List<AnomalyResult> scoreWithHeuristics(List<AppEventLog> events) {
        List<AnomalyResult> results = new ArrayList<>();

        for (AppEventLog log : events) {
            if (log.getId() == null) {
                continue;
            }

            double score = computeHeuristicAnomalyScore(log);
            String label = score >= config.getAnomalyThreshold() ? "ANOMALY" : "NORMAL";
            String reason = score >= config.getAnomalyThreshold()
                    ? "Heuristic threshold exceeded"
                    : "Heuristic score below threshold";

            results.add(new AnomalyResult(log.getId(), score, label, reason));
        }

        return results;
    }

    private List<AnomalyResult> scoreWithWorker(List<AppEventLog> events) throws IOException {
        String endpoint = config.getWorkerUrl().replaceAll("/$", "") + "/score";
        JsonObject payload = new JsonObject();
        JsonArray rows = new JsonArray();

        for (AppEventLog log : events) {
            if (log.getId() == null) {
                continue;
            }

            JsonObject row = new JsonObject();
            row.addProperty("id", log.getId());
            row.addProperty("eventId", log.getEventId());
            row.addProperty("eventType", log.getEventType());
            row.addProperty("category", log.getCategory());
            row.addProperty("action", log.getAction());
            row.addProperty("outcome", log.getOutcome());
            row.addProperty("level", log.getLevel());
            row.addProperty("entityType", log.getEntityType());
            row.addProperty("entityId", log.getEntityId());
            row.addProperty("sessionId", log.getSessionId());
            row.addProperty("traceId", log.getTraceId());
            row.addProperty("durationMs", log.getDurationMs());
            row.addProperty("riskScore", log.getRiskScore() == null ? 0.0 : log.getRiskScore().doubleValue());
            row.addProperty("message", log.getMessage());
            row.addProperty("eventTimestamp", log.getEventTimestamp() == null ? null : log.getEventTimestamp().toString());

            try {
                if (log.getMetadataJson() != null && !log.getMetadataJson().isBlank()) {
                    row.add("metadata", GSON.fromJson(log.getMetadataJson(), JsonElement.class));
                }
            } catch (Exception ignored) {
                row.addProperty("metadata", log.getMetadataJson());
            }

            rows.add(row);
        }

        payload.add("events", rows);

        Request request = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(payload.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Worker returned HTTP " + response.code());
            }

            String body = response.body() == null ? "" : response.body().string();
            JsonObject root = GSON.fromJson(body, JsonObject.class);
            JsonArray resultArray = root == null || !root.has("results") ? new JsonArray() : root.getAsJsonArray("results");
            List<AnomalyResult> results = new ArrayList<>();

            for (JsonElement element : resultArray) {
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject obj = element.getAsJsonObject();
                if (!obj.has("eventId")) {
                    continue;
                }

                long eventId = obj.get("eventId").getAsLong();
                double score = obj.has("anomalyScore") ? obj.get("anomalyScore").getAsDouble() : 0.0;
                String label = obj.has("anomalyLabel") ? obj.get("anomalyLabel").getAsString() : "UNKNOWN";
                String reason = obj.has("anomalyReason") ? obj.get("anomalyReason").getAsString() : "No reason";

                AnomalyResult result = new AnomalyResult(eventId, score, label, reason);
                if (obj.has("detectedAt") && !obj.get("detectedAt").isJsonNull()) {
                    try {
                        result.detectedAt = LocalDateTime.parse(obj.get("detectedAt").getAsString());
                    } catch (Exception ignored) {
                    }
                }
                results.add(result);
            }

            if (results.isEmpty()) {
                throw new IOException("Worker returned empty results");
            }

            return results;
        }
    }

    private String mergeAnomalyMetadata(String metadataJson, AnomalyResult result) {
        JsonObject obj;

        try {
            obj = metadataJson == null || metadataJson.isBlank()
                    ? new JsonObject()
                    : GSON.fromJson(metadataJson, JsonObject.class);
            if (obj == null) {
                obj = new JsonObject();
            }
        } catch (Exception e) {
            obj = new JsonObject();
        }

        obj.addProperty("anomalyLabel", result.anomalyLabel);
        obj.addProperty("anomalyReason", result.anomalyReason);
        obj.addProperty("detectedAt", result.detectedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        obj.addProperty("anomalySourceWindow", result.sourceWindow == null ? "recent_batch" : result.sourceWindow);

        return GSON.toJson(obj);
    }

    /**
     * Structure for anomaly result from LogAI worker (Phase 5).
     */
    public static class AnomalyResult {
        public Long eventId;
        public double anomalyScore;
        public String anomalyLabel;
        public String anomalyReason;
        public LocalDateTime detectedAt;
        public String sourceWindow;  // e.g., "last 1 hour"

        public AnomalyResult(Long eventId, double anomalyScore, String label, String reason) {
            this.eventId = eventId;
            this.anomalyScore = anomalyScore;
            this.anomalyLabel = label;
            this.anomalyReason = reason;
            this.detectedAt = LocalDateTime.now();
        }
    }

    public static class ScoreSummary {
        public int processed;
        public int updated;
        public int failed;
        public int anomaliesDetected;
        public boolean usedFallback;
    }
}
