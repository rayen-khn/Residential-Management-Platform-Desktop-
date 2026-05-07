package com.syndicati.services.analytics;

import java.math.BigDecimal;
import java.util.*;

/**
 * Risk scoring service that aggregates multiple risk signals into a single risk score.
 *
 * Factors considered:
 * - Anomaly score (from LogAI)
 * - Risk score (from event logging)
 * - Suspicious activity flags (repeated failures, unusual patterns)
 * - User behavior baseline deviation
 *
 * Result: 0.0 (safe) to 1.0 (critical)
 */
public class RiskScoringService {

    // Weights for different risk factors (must sum to 1.0)
    private static final double WEIGHT_ANOMALY_SCORE = 0.3;
    private static final double WEIGHT_RISK_SCORE = 0.2;
    private static final double WEIGHT_SUSPICIOUS_FLAGS = 0.4;
    private static final double WEIGHT_BEHAVIOR_BASELINE = 0.1;

    /**
     * Calculate overall risk score for a user/session based on multiple signals.
     */
    public RiskResult calculateRiskScore(
            BigDecimal anomalyScore,
            BigDecimal riskScore,
            SuspiciousActivityService.SuspiciousActivityResult suspiciousActivity,
            double behaviorBaselineDeviation) {

        RiskResult result = new RiskResult();

        // Normalize scores to 0-1 range
        double normalizedAnomaly = normalizeScore(anomalyScore);
        double normalizedRisk = normalizeScore(riskScore);
        double suspiciousScore = suspiciousActivity == null ? 0.0 : suspiciousActivity.totalRiskScore();
        double normalizedBehavior = Math.min(behaviorBaselineDeviation, 1.0);

        // Weighted aggregate
        double overallScore = (normalizedAnomaly * WEIGHT_ANOMALY_SCORE)
                + (normalizedRisk * WEIGHT_RISK_SCORE)
                + (suspiciousScore * WEIGHT_SUSPICIOUS_FLAGS)
                + (normalizedBehavior * WEIGHT_BEHAVIOR_BASELINE);

        result.overallRiskScore = overallScore;
        result.anomalyComponent = normalizedAnomaly;
        result.riskComponent = normalizedRisk;
        result.suspiciousComponent = suspiciousScore;
        result.behaviorComponent = normalizedBehavior;

        // Determine severity level
        if (overallScore >= 0.8) {
            result.severity = "CRITICAL";
            result.recommendation = "Block immediately, require MFA verification";
        } else if (overallScore >= 0.6) {
            result.severity = "HIGH";
            result.recommendation = "Flag for review, consider requiring re-auth";
        } else if (overallScore >= 0.4) {
            result.severity = "MEDIUM";
            result.recommendation = "Monitor closely, collect additional data";
        } else if (overallScore >= 0.2) {
            result.severity = "LOW";
            result.recommendation = "Log for analysis";
        } else {
            result.severity = "SAFE";
            result.recommendation = "No action needed";
        }

        return result;
    }

    /**
     * Normalize BigDecimal score to 0-1 range.
     */
    private double normalizeScore(BigDecimal score) {
        if (score == null) {
            return 0.0;
        }
        double value = score.doubleValue();
        return Math.min(Math.max(value, 0.0), 1.0);
    }

    /**
     * Result of risk scoring.
     */
    public static class RiskResult {
        public double overallRiskScore;
        public double anomalyComponent;
        public double riskComponent;
        public double suspiciousComponent;
        public double behaviorComponent;
        public String severity;
        public String recommendation;

        @Override
        public String toString() {
            return String.format(
                    "RiskScore{overall=%.2f, severity=%s, anomaly=%.2f, risk=%.2f, suspicious=%.2f, behavior=%.2f}",
                    overallRiskScore, severity, anomalyComponent, riskComponent, suspiciousComponent, behaviorComponent
            );
        }

        public Map<String, Object> toMap() {
            return Map.of(
                    "overallRiskScore", overallRiskScore,
                    "severity", severity,
                    "recommendation", recommendation,
                    "components", Map.of(
                            "anomaly", anomalyComponent,
                            "risk", riskComponent,
                            "suspicious", suspiciousComponent,
                            "behavior", behaviorComponent
                    )
            );
        }
    }
}
