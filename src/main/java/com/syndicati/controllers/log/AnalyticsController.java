package com.syndicati.controllers.log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.syndicati.models.log.AppEventLog;
import com.syndicati.models.log.analytics.AnomalyResult;
import com.syndicati.models.log.analytics.RiskScore;
import com.syndicati.models.log.analytics.SuspiciousActivity;
import com.syndicati.models.log.data.AppEventLogRepository;
import com.syndicati.services.analytics.RiskScoringService;
import com.syndicati.services.analytics.SuspiciousActivityService;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Admin analytics facade for observability and security workflows.
 *
 * Endpoint-equivalent methods:
 * - getRecentAnomalies       -> GET /api/admin/anomalies/recent
 * - getSuspiciousUsers       -> GET /api/admin/suspicious-users
 * - getUserTimeline          -> GET /api/admin/user/{id}/timeline
 * - getFeatureUsage          -> GET /api/admin/analytics/feature-usage
 */
public class AnalyticsController {

    private static final Gson GSON = new Gson();

    private final AppEventLogRepository repository;
    private final SuspiciousActivityService suspiciousActivityService;
    private final RiskScoringService riskScoringService;

    public AnalyticsController() {
        this.repository = new AppEventLogRepository();
        this.suspiciousActivityService = new SuspiciousActivityService(repository);
        this.riskScoringService = new RiskScoringService();
    }

    public List<AnomalyResult> getRecentAnomalies(int limit) {
        List<AppEventLog> logs = repository.findRecentAnomalies(0.65, Math.max(1, limit));
        List<AnomalyResult> output = new ArrayList<>();

        for (AppEventLog log : logs) {
            JsonObject metadata = parseMetadata(log.getMetadataJson());
            String label = metadata.has("anomalyLabel") ? metadata.get("anomalyLabel").getAsString() : "ANOMALY";
            String reason = metadata.has("anomalyReason") ? metadata.get("anomalyReason").getAsString() : "High anomaly score";
            LocalDateTime detectedAt = metadata.has("detectedAt")
                    ? safeParseDateTime(metadata.get("detectedAt").getAsString(), log.getEventTimestamp())
                    : log.getEventTimestamp();

            String userDisplayName = "anonymous";
            if (log.getUser() != null) {
                userDisplayName = (log.getUser().getFirstName() + " " + log.getUser().getLastName()).trim();
                if (userDisplayName.isEmpty()) userDisplayName = log.getUser().getEmailUser();
            } else if (metadata.has("username")) {
                userDisplayName = metadata.get("username").getAsString();
            } else if (metadata.has("email")) {
                userDisplayName = metadata.get("email").getAsString();
            }

            output.add(new AnomalyResult(
                    log.getId() == null ? -1L : log.getId(),
                    log.getEventType(),
                    log.getUser() == null ? null : log.getUser().getIdUser(),
                    userDisplayName,
                    log.getAnomalyScore() == null ? 0.0 : log.getAnomalyScore().doubleValue(),
                    label,
                    reason,
                    detectedAt
            ));
        }

        return output;
    }

    public List<SuspiciousActivity> getSuspiciousUsers(int limit) {
        List<String[]> rows = repository.fetchSuspiciousUsers(LocalDateTime.now().minusDays(7), 3, Math.max(1, limit));
        List<SuspiciousActivity> result = new ArrayList<>();

        for (String[] row : rows) {
            Integer userId = safeParseInt(row[0]);
            String email = row[1];
            int failureCount = safeParseInt(row[2]) == null ? 0 : safeParseInt(row[2]);
            LocalDateTime lastSeen = parseSqlTimestamp(row[3]);

            SuspiciousActivityService.SuspiciousActivityResult suspicious = suspiciousActivityService.detectForUser(userId, null);
            RiskScoringService.RiskResult risk = riskScoringService.calculateRiskScore(
                    BigDecimal.valueOf(suspicious.totalRiskScore()),
                    BigDecimal.ZERO,
                    suspicious,
                    Math.min(1.0, failureCount / 10.0)
            );

            List<String> flags = suspicious.flags.stream()
                    .map(f -> f.ruleId + ": " + f.description)
                    .toList();

            RiskScore riskScore = new RiskScore(
                    userId,
                    risk.overallRiskScore,
                    risk.severity,
                    risk.recommendation,
                    risk.anomalyComponent,
                    risk.riskComponent,
                    risk.suspiciousComponent,
                    risk.behaviorComponent
            );

            result.add(new SuspiciousActivity(userId, email, failureCount, lastSeen, flags, riskScore));
        }

        result.sort(Comparator.comparing((SuspiciousActivity s) -> s.getRiskScore().getOverallRiskScore()).reversed());
        return result;
    }

    public List<AppEventLog> getUserTimeline(int userId, int limit) {
        return repository.findLatestByUser(userId, Math.max(1, limit));
    }

    public List<Map<String, Object>> getFeatureUsage(int limit) {
        List<String[]> rows = repository.fetchFeatureUsage(Math.max(1, limit));
        List<Map<String, Object>> result = new ArrayList<>();
        for (String[] row : rows) {
            result.add(Map.of(
                    "feature", row[0],
                    "count", Integer.parseInt(row[1])
            ));
        }
        return result;
    }

    private JsonObject parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return new JsonObject();
        }
        try {
            JsonObject obj = GSON.fromJson(metadataJson, JsonObject.class);
            return obj == null ? new JsonObject() : obj;
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    private LocalDateTime safeParseDateTime(String text, LocalDateTime fallback) {
        try {
            return LocalDateTime.parse(text);
        } catch (Exception e) {
            return fallback;
        }
    }

    private LocalDateTime parseSqlTimestamp(String value) {
        try {
            return Timestamp.valueOf(value).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    private Integer safeParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }
}
