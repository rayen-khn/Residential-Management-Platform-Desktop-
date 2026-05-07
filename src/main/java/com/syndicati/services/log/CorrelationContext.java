package com.syndicati.services.log;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages correlation context (MDC - Mapped Diagnostic Context) for distributed tracing.
 * Provides a thread-local store for correlation IDs that can be automatically injected into logs.
 *
 * This enables tracking of requests/operations across multiple log entries and service calls.
 */
public class CorrelationContext {

    private static final ThreadLocal<Map<String, String>> contextThreadLocal = ThreadLocal.withInitial(HashMap::new);

    private static final String TRACE_ID_KEY = "traceId";
    private static final String SPAN_ID_KEY = "spanId";
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String SESSION_ID_KEY = "sessionId";
    private static final String USER_ID_KEY = "userId";

    /**
     * Set trace ID for current thread.
     */
    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.isBlank()) {
            contextThreadLocal.get().put(TRACE_ID_KEY, traceId);
        }
    }

    /**
     * Get trace ID from current thread context.
     */
    public static String getTraceId() {
        return contextThreadLocal.get().get(TRACE_ID_KEY);
    }

    /**
     * Get or generate trace ID for current thread.
     */
    public static String getOrGenerateTraceId() {
        String traceId = getTraceId();
        if (traceId == null) {
            traceId = generateTraceId();
            setTraceId(traceId);
        }
        return traceId;
    }

    /**
     * Set span ID for current thread.
     */
    public static void setSpanId(String spanId) {
        if (spanId != null && !spanId.isBlank()) {
            contextThreadLocal.get().put(SPAN_ID_KEY, spanId);
        }
    }

    /**
     * Get span ID from current thread context.
     */
    public static String getSpanId() {
        return contextThreadLocal.get().get(SPAN_ID_KEY);
    }

    /**
     * Get or generate span ID for current thread.
     */
    public static String getOrGenerateSpanId() {
        String spanId = getSpanId();
        if (spanId == null) {
            spanId = generateSpanId();
            setSpanId(spanId);
        }
        return spanId;
    }

    /**
     * Set request ID for current thread.
     */
    public static void setRequestId(String requestId) {
        if (requestId != null && !requestId.isBlank()) {
            contextThreadLocal.get().put(REQUEST_ID_KEY, requestId);
        }
    }

    /**
     * Get request ID from current thread context.
     */
    public static String getRequestId() {
        return contextThreadLocal.get().get(REQUEST_ID_KEY);
    }

    /**
     * Set session ID for current thread.
     */
    public static void setSessionId(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            contextThreadLocal.get().put(SESSION_ID_KEY, sessionId);
        }
    }

    /**
     * Get session ID from current thread context.
     */
    public static String getSessionId() {
        return contextThreadLocal.get().get(SESSION_ID_KEY);
    }

    /**
     * Set user ID for current thread context.
     */
    public static void setUserId(Integer userId) {
        if (userId != null && userId > 0) {
            contextThreadLocal.get().put(USER_ID_KEY, userId.toString());
        }
    }

    /**
     * Get user ID from current thread context.
     */
    public static Integer getUserId() {
        String userId = contextThreadLocal.get().get(USER_ID_KEY);
        if (userId != null) {
            try {
                return Integer.parseInt(userId);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Get all correlation fields as a Map.
     */
    public static Map<String, String> getAll() {
        return new HashMap<>(contextThreadLocal.get());
    }

    /**
     * Clear all correlation context for current thread (should be called on request cleanup).
     */
    public static void clear() {
        contextThreadLocal.remove();
    }

    /**
     * Generate a new trace ID (32 hex chars).
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Generate a new span ID (16 hex chars).
     */
    public static String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Initialize correlation context for a new operation (e.g., at request start).
     * Generates new IDs if not already set.
     */
    public static void initializeIfEmpty() {
        if (getTraceId() == null) {
            setTraceId(generateTraceId());
        }
        if (getSpanId() == null) {
            setSpanId(generateSpanId());
        }
        if (getRequestId() == null) {
            setRequestId(UUID.randomUUID().toString());
        }
    }

    /**
     * Create a child span (generates new spanId but preserves traceId).
     * Returns the new spanId for the child operation.
     */
    public static String createChildSpan() {
        String traceId = getOrGenerateTraceId();
        String childSpanId = generateSpanId();
        String parentSpanId = getSpanId();

        // In a full distributed tracing system, we'd track parent-child relationship
        // For now, just generate new span and caller can track lineage
        setSpanId(childSpanId);

        return childSpanId;
    }

    /**
     * Restore parent span context (after child operation completes).
     */
    public static void restoreParentSpan(String parentSpanId) {
        if (parentSpanId != null) {
            setSpanId(parentSpanId);
        }
    }
}
