package com.syndicati.models.log.analytics;

public class RiskScore {

    private final Integer userId;
    private final double overallRiskScore;
    private final String severity;
    private final String recommendation;
    private final double anomalyComponent;
    private final double riskComponent;
    private final double suspiciousComponent;
    private final double behaviorComponent;

    public RiskScore(Integer userId, double overallRiskScore, String severity, String recommendation,
                     double anomalyComponent, double riskComponent, double suspiciousComponent, double behaviorComponent) {
        this.userId = userId;
        this.overallRiskScore = overallRiskScore;
        this.severity = severity;
        this.recommendation = recommendation;
        this.anomalyComponent = anomalyComponent;
        this.riskComponent = riskComponent;
        this.suspiciousComponent = suspiciousComponent;
        this.behaviorComponent = behaviorComponent;
    }

    public Integer getUserId() {
        return userId;
    }

    public double getOverallRiskScore() {
        return overallRiskScore;
    }

    public String getSeverity() {
        return severity;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public double getAnomalyComponent() {
        return anomalyComponent;
    }

    public double getRiskComponent() {
        return riskComponent;
    }

    public double getSuspiciousComponent() {
        return suspiciousComponent;
    }

    public double getBehaviorComponent() {
        return behaviorComponent;
    }
}
