package com.syndicati.models.log.analytics;

import java.time.LocalDateTime;
import java.util.List;

public class SuspiciousActivity {

    private final Integer userId;
    private final String userEmail;
    private final int failureCount;
    private final LocalDateTime lastSeen;
    private final List<String> flags;
    private final RiskScore riskScore;

    public SuspiciousActivity(Integer userId, String userEmail, int failureCount, LocalDateTime lastSeen, List<String> flags, RiskScore riskScore) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.failureCount = failureCount;
        this.lastSeen = lastSeen;
        this.flags = flags;
        this.riskScore = riskScore;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public List<String> getFlags() {
        return flags;
    }

    public RiskScore getRiskScore() {
        return riskScore;
    }
}
