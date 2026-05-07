package com.syndicati.models.log.enums;

/**
 * Standardized event types for the observability system.
 * These enums provide type-safe event categorization and prevent typos in event logging.
 */
public enum EventType {
    // User activity
    USER_LOGIN("User login attempt"),
    USER_LOGOUT("User logout"),
    USER_SIGNUP("User registration"),
    USER_UPDATE("User profile update"),
    
    // Page/view navigation
    PAGE_VIEW("Page view/navigation"),
    VIEW_CHANGE("View navigation"),
    ROUTE_CHANGE("Route change"),
    
    // UI interactions
    UI_CLICK("UI element click"),
    FORM_SUBMIT("Form submission"),
    DIALOG_OPEN("Dialog opened"),
    DIALOG_CLOSE("Dialog closed"),
    
    // CRUD operations
    CREATE("Create entity"),
    READ("Read entity"),
    UPDATE("Update entity"),
    DELETE("Delete entity"),
    
    // Business domain
    FORUM_POST("Forum post created/modified"),
    FORUM_COMMENT("Forum comment created/modified"),
    FORUM_REACTION("Forum reaction added/removed"),
    EVENT_CREATE("Event created"),
    EVENT_JOIN("User joined event"),
    EVENT_CANCEL("Event cancelled"),
    RECLAMATION_SUBMIT("Reclamation submitted"),
    RECLAMATION_RESOLVE("Reclamation resolved"),
    
    // Security & Auth
    AUTH_SUCCESS("Authentication succeeded"),
    AUTH_FAILURE("Authentication failed"),
    PERMISSION_DENIED("Permission denied"),
    ROLE_CHANGE("User role changed"),
    MFA_ENABLED("Multi-factor auth enabled"),
    MFA_DISABLED("Multi-factor auth disabled"),
    MFA_VERIFY("MFA verification attempt"),
    
    // File operations
    FILE_UPLOAD("File uploaded"),
    FILE_DELETE("File deleted"),
    FILE_DOWNLOAD("File downloaded"),
    
    // System events
    APP_START("Application started"),
    APP_STOP("Application stopped"),
    APP_ERROR("Application error"),
    APP_WARNING("Application warning"),
    CONFIG_CHANGE("Configuration changed"),
    
    // Performance
    SLOW_QUERY("Slow database query"),
    SLOW_API_CALL("Slow API call"),
    TIMEOUT("Operation timeout"),
    
    // Anomaly/Risk
    SUSPICIOUS_ACTIVITY("Suspicious activity detected"),
    FRAUD_ALERT("Potential fraud detected"),
    RATE_LIMIT_HIT("Rate limit exceeded"),
    
    // Other
    UNKNOWN("Unknown event type");

    private final String description;

    EventType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Parse string to EventType, defaulting to UNKNOWN if not found.
     */
    public static EventType fromString(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        try {
            return EventType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
