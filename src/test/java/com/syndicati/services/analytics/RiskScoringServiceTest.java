package com.syndicati.services.analytics;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RiskScoringServiceTest {

    @Test
    void calculateRiskScoreReturnsCriticalForHighSignals() {
        RiskScoringService service = new RiskScoringService();
        SuspiciousActivityService.SuspiciousActivityResult suspicious = new SuspiciousActivityService.SuspiciousActivityResult();
        suspicious.flags.add(new SuspiciousActivityService.ActivityFlag("RULE", "High", "reason", 1.0));

        RiskScoringService.RiskResult result = service.calculateRiskScore(
                BigDecimal.ONE,
                BigDecimal.ONE,
                suspicious,
                1.0
        );

        assertTrue(result.overallRiskScore >= 0.8);
        assertEquals("CRITICAL", result.severity);
    }

    @Test
    void calculateRiskScoreHandlesNullSuspiciousInput() {
        RiskScoringService service = new RiskScoringService();

        RiskScoringService.RiskResult result = service.calculateRiskScore(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                0.0
        );

        assertEquals("SAFE", result.severity);
        assertEquals(0.0, result.overallRiskScore, 0.0001);
    }
}
