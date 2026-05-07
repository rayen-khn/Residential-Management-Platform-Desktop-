package com.syndicati.services.log;

import com.syndicati.models.log.AppEventLog;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventValidatorTest {

    @Test
    void validateAcceptsWellFormedEvent() {
        EventValidator validator = new EventValidator();
        AppEventLog log = new AppEventLog();
        log.setEventType("USER_LOGIN");
        log.setEventTimestamp(LocalDateTime.now());
        log.setTraceId("trace-1");
        log.setLevel("INFO");
        log.setOutcome("SUCCESS");
        log.setRiskScore(BigDecimal.valueOf(0.3));
        log.setAnomalyScore(BigDecimal.valueOf(0.2));

        assertTrue(validator.validate(log));
        assertTrue(validator.getErrors().isEmpty());
    }

    @Test
    void validateRejectsOutOfRangeScoresAndNegativeDuration() {
        EventValidator validator = new EventValidator();
        AppEventLog log = new AppEventLog();
        log.setEventType("AUTH_FAILURE");
        log.setEventTimestamp(LocalDateTime.now());
        log.setSessionId("session-1");
        log.setRiskScore(BigDecimal.valueOf(2.0));
        log.setAnomalyScore(BigDecimal.valueOf(-0.1));
        log.setDurationMs(-50);

        assertFalse(validator.validate(log));
        assertTrue(validator.getErrorsAsString().contains("riskScore must be between 0 and 1"));
        assertTrue(validator.getErrorsAsString().contains("anomalyScore must be between 0 and 1"));
        assertTrue(validator.getErrorsAsString().contains("durationMs cannot be negative"));
    }
}
