package com.syndicati.models.log.enums;

/**
 * Event severity levels following standard logging conventions.
 */
public enum EventLevel {
    DEBUG(10, "Debug level - detailed diagnostic information"),
    INFO(20, "Info level - general informational message"),
    WARN(30, "Warn level - warning message for potential issues"),
    ERROR(40, "Error level - error condition"),
    CRITICAL(50, "Critical level - critical error requiring immediate action");

    private final int priority;
    private final String description;

    EventLevel(int priority, String description) {
        this.priority = priority;
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAtLeastAs(EventLevel other) {
        return this.priority >= other.priority;
    }

    /**
     * Parse string to EventLevel, defaulting to INFO if not found.
     */
    public static EventLevel fromString(String value) {
        if (value == null || value.isBlank()) {
            return INFO;
        }
        try {
            return EventLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return INFO;
        }
    }
}
