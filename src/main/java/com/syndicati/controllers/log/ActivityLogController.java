package com.syndicati.controllers.log;

import com.syndicati.models.log.AppEventLog;
import com.syndicati.services.log.UserActivityLogger;
import java.util.List;
import java.util.Map;

public class ActivityLogController {

    private final UserActivityLogger logger;

    public ActivityLogController() {
        this.logger = new UserActivityLogger();
    }

    public void logPageView(String route, String screenName) {
        logger.logPageView(route, screenName, Map.of());
    }

    public void logPageView(String route, String screenName, Map<String, Object> metadata) {
        logger.logPageView(route, screenName, metadata);
    }

    public void logUiClick(String target, String text) {
        logger.logUiClick(target, text, Map.of());
    }

    public void logUiClick(String target, String text, Map<String, Object> metadata) {
        java.util.Map<String, Object> data = metadata == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(metadata);
        
        // Check for spamming
        boolean isSpam = com.syndicati.services.log.SpamDetectionService.getInstance().isSpamming(target);
        if (isSpam) {
            data.put("spam", true);
            data.put("level", "WARNING");
            data.put("message", "Rapid-fire clicking detected on: " + target);
        }
        
        logger.logUiClick(target, text, data);
    }

    public void logCrudAction(String action, String entityType, Integer entityId, Map<String, Object> metadata) {
        logger.logCrudAction(action, entityType, entityId, metadata);
    }

    public void logAuthAction(String action, String outcome, String message, Map<String, Object> metadata) {
        logger.logAuthAction(action, outcome, message, metadata);
    }

    public void logSecurityAlert(String alertType, String severity, String message, Map<String, Object> metadata) {
        logger.logSecurityAlert(alertType, severity, message, metadata);
    }

    public void logDataExport(String entityType, String format, int count, Map<String, Object> metadata) {
        logger.logDataExport(entityType, format, count, metadata);
    }

    public void logHoneypotClick(String target, Map<String, Object> metadata) {
        java.util.Map<String, Object> data = metadata == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(metadata);
        data.put("honeypot", true);
        data.put("severity", "CRITICAL");
        logSecurityAlert("HONEYPOT_TRIGGER", "CRITICAL", "Honeypot element interacted with: " + target, data);
    }

    public List<AppEventLog> recentActivity(int limit) {
        return logger.recentActivity(limit);
    }
}