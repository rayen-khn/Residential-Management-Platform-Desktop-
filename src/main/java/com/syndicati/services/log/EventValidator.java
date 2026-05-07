package com.syndicati.services.log;

import com.syndicati.models.log.AppEventLog;
import com.syndicati.models.log.enums.EventLevel;
import com.syndicati.models.log.enums.EventOutcome;
import com.syndicati.models.log.enums.EventType;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates AppEventLog objects for consistency and schema compliance.
 * Ensures events meet minimum requirements before storage/export.
 */
public class EventValidator {

    private final List<String> validationErrors = new ArrayList<>();

    /**
     * Validate an event log entry. Returns true if valid, false otherwise.
     * Errors can be retrieved via getErrors().
     */
    public boolean validate(AppEventLog event) {
        validationErrors.clear();

        if (event == null) {
            validationErrors.add("Event cannot be null");
            return false;
        }

        // Required fields
        if (event.getEventType() == null || event.getEventType().isBlank()) {
            validationErrors.add("eventType is required");
        }

        if (event.getEventTimestamp() == null) {
            validationErrors.add("eventTimestamp is required");
        }

        // Validate enum values if present
        if (event.getEventType() != null && !event.getEventType().isBlank()) {
            try {
                EventType.fromString(event.getEventType());
            } catch (Exception e) {
                // EventType.fromString returns UNKNOWN on error, so this is just defensive
                validationErrors.add("Invalid eventType: " + event.getEventType());
            }
        }

        if (event.getLevel() != null && !event.getLevel().isBlank()) {
            try {
                EventLevel.fromString(event.getLevel());
            } catch (Exception e) {
                validationErrors.add("Invalid level: " + event.getLevel());
            }
        }

        if (event.getOutcome() != null && !event.getOutcome().isBlank()) {
            try {
                EventOutcome.fromString(event.getOutcome());
            } catch (Exception e) {
                validationErrors.add("Invalid outcome: " + event.getOutcome());
            }
        }

        // Validate correlation fields (should have at least one)
        boolean hasCorrelation = (event.getTraceId() != null && !event.getTraceId().isBlank())
                || (event.getRequestId() != null && !event.getRequestId().isBlank())
                || (event.getSessionId() != null && !event.getSessionId().isBlank());

        if (!hasCorrelation) {
            validationErrors.add("At least one correlation field (traceId, requestId, sessionId) is recommended");
        }

        // Validate score fields are in valid range if present
        if (event.getRiskScore() != null) {
            if (event.getRiskScore().doubleValue() < 0 || event.getRiskScore().doubleValue() > 1) {
                validationErrors.add("riskScore must be between 0 and 1");
            }
        }

        if (event.getAnomalyScore() != null) {
            if (event.getAnomalyScore().doubleValue() < 0 || event.getAnomalyScore().doubleValue() > 1) {
                validationErrors.add("anomalyScore must be between 0 and 1");
            }
        }

        // Validate duration if present
        if (event.getDurationMs() != null && event.getDurationMs() < 0) {
            validationErrors.add("durationMs cannot be negative");
        }

        return validationErrors.isEmpty();
    }

    /**
     * Get list of validation errors from last validate() call.
     */
    public List<String> getErrors() {
        return new ArrayList<>(validationErrors);
    }

    /**
     * Get validation errors as a single string.
     */
    public String getErrorsAsString() {
        return String.join("; ", validationErrors);
    }

    /**
     * Check if event has all critical fields (can proceed even with warnings).
     */
    public boolean hasCriticalFields(AppEventLog event) {
        return event != null
                && event.getEventType() != null && !event.getEventType().isBlank()
                && event.getEventTimestamp() != null;
    }
}
