package com.syndicati.services.observability;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Langfuse tracing service for LLM observability.
 *
 * This service provides hooks for instrumenting LLM calls and AI features.
 * Currently prepared as a foundation; can be extended when AI features are added.
 *
 * Features:
 * - OpenTelemetry-style trace/span tracking
 * - Thread-local trace context
 * - Future-proof for Langfuse OTEL export
 *
 * Usage example:
 *   LangfuseTracer tracer = new LangfuseTracer(config);
 *   Span span = tracer.startSpan("llm_call", "gpt-4");
 *   try {
 *       // Call LLM
 *       String response = callLLM(...);
 *       span.addEvent("llm_response", Map.of("tokens", 150));
 *       tracer.endSpan(span, "success");
 *   } catch (Exception e) {
 *       tracer.endSpan(span, "error");
 *   }
 */
public class LangfuseTracer {

    private static final String LOG_TAG = "[LangfuseTracer]";
    private final LangfuseConfig config;
    private final ThreadLocal<Stack<Span>> spanStack = ThreadLocal.withInitial(Stack::new);
    private final Map<String, Trace> traces = new ConcurrentHashMap<>();

    /**
     * Represents a single trace (end-to-end operation).
     */
    public static class Trace {
        public final String traceId;
        public final String name;
        public final long startTimeMs;
        public String status = "active";
        public long endTimeMs;
        public Map<String, Object> metadata = new HashMap<>();

        Trace(String traceId, String name) {
            this.traceId = traceId;
            this.name = name;
            this.startTimeMs = System.currentTimeMillis();
        }

        public long duration() {
            return endTimeMs - startTimeMs;
        }
    }

    /**
     * Represents a single span within a trace.
     */
    public static class Span {
        public final String spanId;
        public final String traceId;
        public final String name;
        public final String operation;
        public final long startTimeMs;
        public String status = "active";
        public long endTimeMs;
        public List<SpanEvent> events = new ArrayList<>();
        public Map<String, Object> metadata = new HashMap<>();

        Span(String spanId, String traceId, String name, String operation) {
            this.spanId = spanId;
            this.traceId = traceId;
            this.name = name;
            this.operation = operation;
            this.startTimeMs = System.currentTimeMillis();
        }

        public void addEvent(String eventName, Map<String, Object> attributes) {
            events.add(new SpanEvent(eventName, System.currentTimeMillis(), attributes != null ? new HashMap<>(attributes) : new HashMap<>()));
        }

        public long duration() {
            return endTimeMs - startTimeMs;
        }
    }

    /**
     * Represents an event within a span.
     */
    public static class SpanEvent {
        public String name;
        public long timestampMs;
        public Map<String, Object> attributes;

        SpanEvent(String name, long timestampMs, Map<String, Object> attributes) {
            this.name = name;
            this.timestampMs = timestampMs;
            this.attributes = attributes;
        }
    }

    public LangfuseTracer(LangfuseConfig config) {
        this.config = config;
        if (config.isEnabled()) {
            System.out.println(LOG_TAG + " Langfuse tracing enabled. Base URL: " + config.getBaseUrl());
        }
    }

    /**
     * Start a new trace (top-level operation).
     */
    public Trace startTrace(String traceName) {
        if (!config.isEnabled()) {
            return new Trace(UUID.randomUUID().toString(), traceName);
        }

        String traceId = UUID.randomUUID().toString().replace("-", "");
        Trace trace = new Trace(traceId, traceName);
        traces.put(traceId, trace);
        System.out.println(LOG_TAG + " Started trace: " + traceName + " (" + traceId + ")");
        return trace;
    }

    /**
     * Start a new span within current trace.
     * Operation typically specifies the type of work (e.g., "llm_call", "database_query").
     */
    public Span startSpan(String spanName, String operation) {
        if (!config.isEnabled()) {
            return new Span(UUID.randomUUID().toString(), UUID.randomUUID().toString(), spanName, operation);
        }

        String spanId = UUID.randomUUID().toString().replace("-", "");
        String traceId = getOrCreateTraceId();
        Span span = new Span(spanId, traceId, spanName, operation);
        spanStack.get().push(span);
        System.out.println(LOG_TAG + " Started span: " + spanName + " [" + operation + "]");
        return span;
    }

    /**
     * End a span and record it.
     */
    public void endSpan(Span span, String status) {
        if (span == null) {
            return;
        }

        span.endTimeMs = System.currentTimeMillis();
        span.status = status;

        if (!spanStack.get().isEmpty() && spanStack.get().peek() == span) {
            spanStack.get().pop();
        }

        if (config.isEnabled()) {
            System.out.println(LOG_TAG + " Ended span: " + span.name + " [" + status + "] (" + span.duration() + "ms)");
            // TODO: Export to Langfuse when integrated
        }
    }

    /**
     * End current trace.
     */
    public void endTrace(Trace trace, String status) {
        if (trace == null) {
            return;
        }

        trace.endTimeMs = System.currentTimeMillis();
        trace.status = status;

        if (config.isEnabled()) {
            System.out.println(LOG_TAG + " Ended trace: " + trace.name + " [" + status + "] (" + trace.duration() + "ms)");
            // TODO: Export to Langfuse when integrated
        }
    }

    /**
     * Add event to current span.
     */
    public void addEvent(String eventName, Map<String, Object> attributes) {
        if (spanStack.get().isEmpty()) {
            return;
        }

        Span span = spanStack.get().peek();
        span.addEvent(eventName, attributes);
    }

    /**
     * Record an LLM call (convenience method for AI features).
     * Returns a Span that should be ended when the call completes.
     */
    public Span recordLLMCall(String modelName, String prompt, int maxTokens) {
        Span span = startSpan("llm_call_" + modelName, "llm");
        span.metadata.put("model", modelName);
        span.metadata.put("prompt_length", prompt != null ? prompt.length() : 0);
        span.metadata.put("max_tokens", maxTokens);
        return span;
    }

    /**
     * Record LLM response metrics.
     */
    public void recordLLMResponse(Span span, String response, int tokensUsed, double cost) {
        if (span == null) {
            return;
        }

        span.metadata.put("response_length", response != null ? response.length() : 0);
        span.metadata.put("tokens_used", tokensUsed);
        span.metadata.put("cost_usd", cost);
        addEvent("llm_response", Map.of(
                "tokens", tokensUsed,
                "cost", cost
        ));
    }

    /**
     * Get current trace ID from span context, or generate one.
     */
    private String getOrCreateTraceId() {
        if (!spanStack.get().isEmpty()) {
            return spanStack.get().peek().traceId;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Get all active traces (for debugging/admin).
     */
    public Collection<Trace> getActiveTraces() {
        return traces.values();
    }

    /**
     * Clear thread-local context (call on request cleanup).
     */
    public void clearContext() {
        spanStack.remove();
    }

    /**
     * Check if Langfuse is enabled.
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }
}
