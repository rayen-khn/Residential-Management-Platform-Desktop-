package com.syndicati.services.log;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataRedactorTest {

    @Test
    void redactMessageMasksSensitiveTokens() {
        String input = "login failed password=SuperSecret token=abc123 4111111111111111";
        String redacted = DataRedactor.redactMessage(input);

        assertTrue(redacted.contains("password=[REDACTED]"));
        assertTrue(redacted.contains("token=[REDACTED]"));
        assertTrue(redacted.contains("[CC_REDACTED]"));
    }

    @Test
    void redactMetadataMasksSensitiveFields() {
        String input = "{\"password\":\"hello\",\"safe\":\"value\",\"api_key\":\"xyz\"}";
        String redacted = DataRedactor.redactMetadata(input);

        assertTrue(redacted.contains("\"password\":\"[REDACTED]\""));
        assertTrue(redacted.contains("\"api_key\":\"[REDACTED]\""));
        assertTrue(redacted.contains("\"safe\":\"value\""));
    }

    @Test
    void anonymizeIpAddressMasksHostPart() {
        assertEquals("192.168.10.0", DataRedactor.anonymizeIpAddress("192.168.10.34"));
    }
}
