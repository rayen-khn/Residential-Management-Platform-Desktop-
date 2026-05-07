package com.syndicati.services.log;

import com.syndicati.models.log.AppEventLog;
import com.syndicati.models.log.data.AppEventLogRepository;
import com.syndicati.models.log.enums.EventCategory;
import com.syndicati.models.log.enums.EventLevel;
import com.syndicati.models.log.enums.EventOutcome;
import com.syndicati.models.log.enums.EventType;
import com.syndicati.models.user.User;
import com.syndicati.services.observability.LangfuseRuntimeService;
import com.syndicati.services.observability.LangfuseTracer;
import com.syndicati.utils.session.SessionManager;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced UserActivityLogger with standardized event handling.
 * 
 * Features:
 * - Event validation via EventValidator
 * - Automatic data redaction via DataRedactor
 * - Correlation context management for distributed tracing
 * - Enum-based event type/level/outcome enforcement
 * - Thread-safe correlation propagation
 */
public class UserActivityLogger {

    private final AppEventLogRepository repository;
    private final LogBuffer logBuffer;
    private final String runtimeSessionId;
    private final EventValidator validator;

    public UserActivityLogger() {
        this(new AppEventLogRepository(), new LogBuffer());
    }

    public UserActivityLogger(AppEventLogRepository repository, LogBuffer logBuffer) {
        this.repository = repository;
        this.logBuffer = logBuffer;
        this.validator = new EventValidator();
        this.runtimeSessionId = UUID.randomUUID().toString();
        
        // Initialize correlation context for this session
        CorrelationContext.setSessionId(this.runtimeSessionId);
        CorrelationContext.initializeIfEmpty();
    }

    public void log(String eventType, String entityType, Integer entityId, Map<String, Object> metadata, User user) {
        try {
            // Initialize correlation context if not already set
            CorrelationContext.initializeIfEmpty();

            Map<String, Object> data = enrichMetadata(metadata);
            AppEventLog log = new AppEventLog();

            // Use correlation context values if available
            log.setEventId(asString(data.get("event_id"), UUID.randomUUID().toString()));
            log.setSessionId(asString(data.get("session_id"), CorrelationContext.getSessionId() != null ? CorrelationContext.getSessionId() : runtimeSessionId));
            log.setRequestId(asString(data.get("request_id"), CorrelationContext.getRequestId()));
            log.setTraceId(asString(data.get("trace_id"), CorrelationContext.getOrGenerateTraceId()));
            log.setSpanId(asString(data.get("span_id"), CorrelationContext.getOrGenerateSpanId()));

            // Normalize event type via enum
            String normalizedEventType = eventType;
            try {
                EventType et = EventType.fromString(eventType);
                normalizedEventType = et.name();
            } catch (Exception e) {
                // Keep original if parsing fails
            }
            log.setEventType(normalizedEventType);

            // Normalize category via enum
            String categoryStr = asString(data.get("category"), "USER_ACTIVITY");
            try {
                EventCategory ec = EventCategory.fromString(categoryStr);
                log.setCategory(ec.name());
            } catch (Exception e) {
                log.setCategory(categoryStr);
            }

            log.setAction(asString(data.get("action"), eventType));

            // Normalize outcome via enum
            String outcomeStr = asString(data.get("outcome"), "SUCCESS");
            try {
                EventOutcome eo = EventOutcome.fromString(outcomeStr);
                log.setOutcome(eo.name());
            } catch (Exception e) {
                log.setOutcome(outcomeStr);
            }

            // Redact message before logging
            String message = asString(data.get("message"), null);
            if (message != null) {
                message = DataRedactor.redactMessage(message);
            }
            log.setMessage(message);

            // Redact IP (anonymize last octet)
            String ipAddress = asString(data.get("ip_address"), hostAddress());
            if (ipAddress != null) {
                ipAddress = DataRedactor.anonymizeIpAddress(ipAddress);
            }
            log.setIpAddress(ipAddress);

            // Redact user agent
            String userAgent = asString(data.get("user_agent"), System.getProperty("http.agent"));
            if (userAgent != null) {
                userAgent = DataRedactor.redactUserAgent(userAgent);
            }
            log.setUserAgent(userAgent);

            log.setDurationMs(parseInteger(data.get("duration_ms")));
            log.setRiskScore(parseDecimal(data.get("risk_score")));
            log.setAnomalyScore(parseDecimal(data.get("anomaly_score")));
            log.setEntityType(entityType);
            log.setEntityId(entityId);

            // Redact metadata before storing
            String redactedMetadata = DataRedactor.redactMetadata(toJson(data));
            log.setMetadataJson(redactedMetadata);

            log.setEventTimestamp(LocalDateTime.now());

            // Normalize level via enum
            String levelStr = asString(data.get("level"), "INFO");
            try {
                EventLevel el = EventLevel.fromString(levelStr);
                log.setLevel(el.name());
            } catch (Exception e) {
                log.setLevel(levelStr);
            }

            log.setServiceName(asString(data.get("service_name"), "SyndicatiDesktop"));
            log.setEnvironment(asString(data.get("environment"), defaultEnvironment()));
            log.setApplicationVersion(asString(data.get("application_version"), System.getProperty("app.version", "dev")));

            User resolvedUser = user != null ? user : SessionManager.getInstance().getCurrentUser();
            if (resolvedUser != null && resolvedUser.getIdUser() != null && resolvedUser.getIdUser() > 0) {
                log.setUser(resolvedUser);
                // Also update correlation context
                CorrelationContext.setUserId(resolvedUser.getIdUser());
            }

            // Validate event before persistence
            if (!validator.validate(log)) {
                System.out.println("[UserActivityLogger] Validation warnings: " + validator.getErrorsAsString());
                // Continue anyway if critical fields are present
                if (!validator.hasCriticalFields(log)) {
                    System.out.println("[UserActivityLogger] Skipping event due to missing critical fields");
                    return;
                }
            }

            repository.create(log);
            logBuffer.push(log);

            // --- Langfuse trace/span recording ---
            recordToLangfuse(log);
        } catch (Exception e) {
            System.out.println("[UserActivityLogger] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Records a completed log event as a Langfuse trace + span so that every
     * persisted DB row has a matching observability entry. The span is closed
     * immediately since the event is already a discrete, completed action.
     *
     * IDs are taken from the event itself so Langfuse and the DB stay in sync.
     */
    private void recordToLangfuse(AppEventLog log) {
        try {
            LangfuseRuntimeService langfuse = LangfuseRuntimeService.getInstance();
            if (!langfuse.isEnabled()) {
                return;
            }

            LangfuseTracer tracer = langfuse.tracer();

            // Use the event's own traceId so all events in the same session
            // cluster under a single trace in the Langfuse UI.
            String traceId = log.getTraceId();
            LangfuseTracer.Trace trace = tracer.startTrace(
                    traceId != null ? traceId : "session_" + runtimeSessionId);

            // One span per event - represents the discrete UI/CRUD action.
            String spanName = (log.getEventType() != null ? log.getEventType() : "EVENT")
                    + (log.getEntityType() != null ? "." + log.getEntityType() : "");
            LangfuseTracer.Span span = tracer.startSpan(spanName, "activity_log");

            // Attach key fields as metadata.
            span.metadata.put("event_id",   log.getEventId());
            span.metadata.put("trace_id",   log.getTraceId());
            span.metadata.put("span_id",    log.getSpanId());
            span.metadata.put("session_id", log.getSessionId());
            span.metadata.put("action",     log.getAction());
            span.metadata.put("category",   log.getCategory());
            span.metadata.put("level",      log.getLevel());
            span.metadata.put("outcome",    log.getOutcome());
            if (log.getUser() != null) {
                span.metadata.put("user_id", log.getUser().getIdUser());
            }

            // Add a single span event summarising the activity.
            span.addEvent("activity_recorded", Map.of(
                    "event_type", log.getEventType() != null ? log.getEventType() : "",
                    "outcome",    log.getOutcome()   != null ? log.getOutcome()   : "",
                    "entity",     log.getEntityType() != null ? log.getEntityType() : ""
            ));

            tracer.endSpan(span, log.getOutcome() != null ? log.getOutcome() : "SUCCESS");
            tracer.endTrace(trace, "SUCCESS");
        } catch (Exception ex) {
            // Never let Langfuse errors surface to callers.
            System.out.println("[UserActivityLogger] Langfuse recording error: " + ex.getMessage());
        }
    }

    public void logPageView(String route, String screenName, Map<String, Object> metadata) {
        Map<String, Object> data = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        data.putIfAbsent("route", route);
        data.putIfAbsent("screen", screenName);
        log("PAGE_VIEW", "ROUTE", null, data, null);
    }

    public void logUiClick(String target, String text, Map<String, Object> metadata) {
        Map<String, Object> data = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        data.putIfAbsent("target", target);
        data.putIfAbsent("text", text);
        log("UI_CLICK", "UI_ELEMENT", null, data, null);
    }

    public void logCrudAction(String action, String entityType, Integer entityId, Map<String, Object> metadata) {
        Map<String, Object> data = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        data.putIfAbsent("action", action);
        log(action == null ? "CRUD" : action.toUpperCase(), entityType, entityId, data, null);
    }

    public void logAuthAction(String action, String outcome, String message, Map<String, Object> metadata) {
        Map<String, Object> data = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        data.putIfAbsent("action", action);
        data.putIfAbsent("outcome", outcome);
        data.putIfAbsent("message", message);
        data.putIfAbsent("category", "AUTH");
        
        String eventType = "AUTH_" + action.toUpperCase();
        if ("failure".equalsIgnoreCase(outcome)) {
            eventType = "AUTH_FAILURE";
        }
        
        log(eventType, "USER", null, data, null);
    }

    public void logSecurityAlert(String alertType, String severity, String message, Map<String, Object> metadata) {
        Map<String, Object> data = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        data.putIfAbsent("alert_type", alertType);
        data.putIfAbsent("severity", severity);
        data.putIfAbsent("message", message);
        data.putIfAbsent("category", "SECURITY");
        data.putIfAbsent("level", severity.toUpperCase());
        log("SECURITY_ALERT", "SYSTEM", null, data, null);
    }

    public void logDataExport(String entityType, String format, int count, Map<String, Object> metadata) {
        Map<String, Object> data = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        data.putIfAbsent("export_format", format);
        data.putIfAbsent("record_count", count);
        data.putIfAbsent("category", "DATA_PRIVACY");
        data.putIfAbsent("action", "EXPORT");
        log("DATA_EXPORT", entityType, null, data, null);
    }

    public List<AppEventLog> recentActivity(int limit) {
        return repository.findLatest(limit);
    }

    private Map<String, Object> enrichMetadata(Map<String, Object> metadata) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (metadata != null) {
            data.putAll(metadata);
        }

        data.putIfAbsent("host", hostName());
        data.putIfAbsent("os", System.getProperty("os.name"));
        data.putIfAbsent("os_version", System.getProperty("os.version"));
        data.putIfAbsent("java_version", System.getProperty("java.version"));
        data.putIfAbsent("user_name", System.getProperty("user.name"));

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            data.putIfAbsent("session_user", currentUser.getEmailUser());
            data.putIfAbsent("session_role", currentUser.getRoleUser());
        }

        return data;
    }

    private String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return System.getProperty("user.name", "desktop");
        }
    }

    private String hostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private String defaultEnvironment() {
        String env = System.getenv("APP_ENV");
        return env == null || env.isBlank() ? "desktop" : env;
    }

    private String toJson(Map<String, Object> data) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append('"').append(escape(entry.getKey())).append('"').append(':').append(valueToJson(entry.getValue()));
        }
        json.append('}');
        return json.toString();
    }

    private String valueToJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return '"' + escape(String.valueOf(value)) + '"';
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String asString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String randomHex(int length) {
        if (length <= 0) {
            return "";
        }
        String hex = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        return hex.substring(0, Math.min(length, hex.length()));
    }
}