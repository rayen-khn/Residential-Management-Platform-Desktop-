package com.syndicati.models.log.enums;

/**
 * Event categories for high-level classification and filtering.
 */
public enum EventCategory {
    USER_ACTIVITY("General user activity like page views, clicks, form submissions"),
    SECURITY("Security-related events: auth, permissions, MFA, suspicious activity"),
    BUSINESS("Business domain events: forum posts, events, reclamations"),
    PERFORMANCE("Performance metrics: slow queries, timeouts, resource usage"),
    SYSTEM("System-level events: app start/stop, config changes, errors"),
    DATA_ACCESS("Data read/write operations"),
    THIRD_PARTY("Interactions with external services/APIs"),
    UNKNOWN("Uncategorized");

    private final String description;

    EventCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Parse string to EventCategory, defaulting to UNKNOWN if not found.
     */
    public static EventCategory fromString(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        try {
            return EventCategory.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
