package com.syndicati.models.log.enums;

/**
 * Standardized event outcome values.
 * Indicates how an event/operation concluded.
 */
public enum EventOutcome {
    SUCCESS("Operation completed successfully"),
    FAILURE("Operation failed"),
    PARTIAL("Operation partially completed"),
    RETRY("Operation will be retried"),
    TIMEOUT("Operation timed out"),
    CANCELLED("Operation cancelled by user"),
    PENDING("Operation pending"),
    REJECTED("Operation rejected (e.g., validation failure)"),
    UNAUTHORIZED("Operation unauthorized"),
    NOT_FOUND("Resource not found");

    private final String description;

    EventOutcome(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    public boolean isFailure() {
        return this == FAILURE || this == TIMEOUT || this == REJECTED || this == UNAUTHORIZED;
    }

    /**
     * Parse string to EventOutcome, defaulting to PENDING if not found.
     */
    public static EventOutcome fromString(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }
        try {
            return EventOutcome.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }
}
