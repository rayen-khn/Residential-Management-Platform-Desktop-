package com.syndicati.services.log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationContextTest {

    @AfterEach
    void tearDown() {
        CorrelationContext.clear();
    }

    @Test
    void initializeGeneratesTraceAndSpanIds() {
        CorrelationContext.initializeIfEmpty();

        assertNotNull(CorrelationContext.getTraceId());
        assertNotNull(CorrelationContext.getSpanId());
        assertNotNull(CorrelationContext.getRequestId());
    }

    @Test
    void childSpanCanBeRestoredToParent() {
        CorrelationContext.initializeIfEmpty();
        String parentSpan = CorrelationContext.getSpanId();

        String childSpan = CorrelationContext.createChildSpan();
        assertNotNull(childSpan);
        assertNotEquals(parentSpan, CorrelationContext.getSpanId());

        CorrelationContext.restoreParentSpan(parentSpan);
        assertEquals(parentSpan, CorrelationContext.getSpanId());
    }
}
