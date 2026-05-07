package com.syndicati.services.analytics;

import com.syndicati.models.log.AppEventLog;
import com.syndicati.models.log.data.AppEventLogRepository;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for detecting suspicious user activity patterns.
 *
 * Detects:
 * - Repeated failed authentication attempts
 * - Abnormal request frequency (spikes)
 * - Unusual user paths (jumping between unrelated features)
 * - Bulk operations
 */
public class SuspiciousActivityService {

    private final AppEventLogRepository repository;

    // Configuration
    private static final int FAILED_AUTH_THRESHOLD = 3;      // 3+ failed logins = suspicious
    private static final int ABNORMAL_FREQUENCY_THRESHOLD = 10; // 10+ events in 1 min = spike
    private static final int BULK_DELETE_THRESHOLD = 5;       // 5+ deletes in 5 min = bulk op

    public SuspiciousActivityService() {
        this(new AppEventLogRepository());
    }

    public SuspiciousActivityService(AppEventLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Detect suspicious activity for a user (last 1 hour).
     */
    public SuspiciousActivityResult detectForUser(Integer userId, String sessionId) {
        SuspiciousActivityResult result = new SuspiciousActivityResult();

        if (userId == null && sessionId == null) {
            return result;
        }

        // Get recent activity (last 60 minutes)
        List<AppEventLog> recentLogs = repository.findLatest(1000);
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        List<AppEventLog> userLogs = new ArrayList<>();
        for (AppEventLog log : recentLogs) {
            if ((userId != null && log.getUser() != null && log.getUser().getIdUser().equals(userId))
                    || (sessionId != null && sessionId.equals(log.getSessionId()))) {
                if (log.getEventTimestamp() != null && log.getEventTimestamp().isAfter(oneHourAgo)) {
                    userLogs.add(log);
                }
            }
        }

        if (userLogs.isEmpty()) {
            return result;
        }

        // Check for repeated failed auth
        checkRepeatedFailedAuth(userLogs, result);

        // Check for abnormal frequency
        checkAbnormalFrequency(userLogs, result);

        // Check for bulk operations
        checkBulkOperations(userLogs, result);

        // Check for unusual paths
        checkUnusualPaths(userLogs, result);

        return result;
    }

    private void checkRepeatedFailedAuth(List<AppEventLog> logs, SuspiciousActivityResult result) {
        int failedCount = 0;

        for (AppEventLog log : logs) {
            if ("AUTH_FAILURE".equals(log.getEventType())) {
                failedCount++;
            }
        }

        if (failedCount >= FAILED_AUTH_THRESHOLD) {
            result.flags.add(new ActivityFlag(
                    "REPEATED_FAILED_AUTH",
                    "High",
                    failedCount + " failed authentication attempts in last hour",
                    failedCount / (double) FAILED_AUTH_THRESHOLD
            ));
        }
    }

    private void checkAbnormalFrequency(List<AppEventLog> logs, SuspiciousActivityResult result) {
        // Count events per minute (find the busiest minute)
        Map<String, Integer> eventsPerMinute = new LinkedHashMap<>();

        for (AppEventLog log : logs) {
            if (log.getEventTimestamp() == null) {
                continue;
            }

            // Bucket by minute
            String minute = log.getEventTimestamp().toString().substring(0, 16);
            eventsPerMinute.merge(minute, 1, Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : eventsPerMinute.entrySet()) {
            if (entry.getValue() >= ABNORMAL_FREQUENCY_THRESHOLD) {
                result.flags.add(new ActivityFlag(
                        "ABNORMAL_REQUEST_FREQUENCY",
                        "Medium",
                        entry.getValue() + " events in 1 minute (threshold: " + ABNORMAL_FREQUENCY_THRESHOLD + ")",
                        entry.getValue() / (double) ABNORMAL_FREQUENCY_THRESHOLD
                ));
            }
        }
    }

    private void checkBulkOperations(List<AppEventLog> logs, SuspiciousActivityResult result) {
        int deleteCount = 0;
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        for (AppEventLog log : logs) {
            if ("DELETE".equals(log.getEventType()) && log.getEventTimestamp() != null &&
                    log.getEventTimestamp().isAfter(fiveMinutesAgo)) {
                deleteCount++;
            }
        }

        if (deleteCount >= BULK_DELETE_THRESHOLD) {
            result.flags.add(new ActivityFlag(
                    "BULK_DELETE_OPERATION",
                    "High",
                    deleteCount + " delete operations in 5 minutes",
                    deleteCount / (double) BULK_DELETE_THRESHOLD
            ));
        }
    }

    private void checkUnusualPaths(List<AppEventLog> logs, SuspiciousActivityResult result) {
        // Detect jumping between unrelated entity types (e.g., USER → FORUM → SYNDICAT → EVENT)
        Set<String> entityTypes = new LinkedHashSet<>();

        for (AppEventLog log : logs) {
            if (log.getEntityType() != null && !log.getEntityType().isEmpty()) {
                entityTypes.add(log.getEntityType());
            }
        }

        // If user touches 5+ different entity types in short time, that's unusual
        if (entityTypes.size() >= 5) {
            result.flags.add(new ActivityFlag(
                    "UNUSUAL_USER_PATH",
                    "Low",
                    "User interacting with " + entityTypes.size() + " different entity types",
                    entityTypes.size() / 5.0
            ));
        }
    }

    /**
     * Result of suspicious activity detection.
     */
    public static class SuspiciousActivityResult {
        public List<ActivityFlag> flags = new ArrayList<>();
        public boolean isSuspicious() {
            return !flags.isEmpty();
        }
        public double totalRiskScore() {
            return flags.stream().mapToDouble(f -> f.anomalyScore).average().orElse(0.0);
        }
    }

    /**
     * Individual suspicious activity flag.
     */
    public static class ActivityFlag {
        public String ruleId;
        public String severity;
        public String description;
        public double anomalyScore;

        public ActivityFlag(String ruleId, String severity, String description, double anomalyScore) {
            this.ruleId = ruleId;
            this.severity = severity;
            this.description = description;
            this.anomalyScore = Math.min(anomalyScore, 1.0); // Cap at 1.0
        }
    }
}
