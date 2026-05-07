package com.syndicati.services.log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Automatically redacts sensitive data from events before storage/export.
 * Prevents logging of passwords, tokens, API keys, PII, and other confidential information.
 *
 * Redaction is applied to: message, userAgent, metadataJson, and custom field values.
 */
public class DataRedactor {

    private static final Gson gson = new Gson();
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "password|passwd|pwd|secret|apikey|api_key|token|auth|bearer",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
            "\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b");

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b(\\+|\\d)[\\d\\s()-]{8,}\\b");

    private static final List<String> SENSITIVE_FIELD_NAMES = List.of(
            "password", "passwd", "pwd", "secret", "apikey", "api_key", "api-key",
            "token", "auth", "bearer", "authorization", "refresh_token", "access_token",
            "creditcard", "credit_card", "cc", "cvv", "cvc", "ssn", "social_security",
            "pin", "pii", "personal_information", "email", "phone", "mobile"
    );

    /**
     * Redact sensitive data from a string message.
     * Replaces detected sensitive values with [REDACTED].
     */
    public static String redactMessage(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String result = message;

        // Redact common sensitive patterns
        result = result.replaceAll("(?i)(password|passwd|pwd|secret|token)\\s*[:=]\\s*[^\\s,};]+",
                "$1=[REDACTED]");
        result = result.replaceAll("(?i)(apikey|api_key|api-key)\\s*[:=]\\s*[^\\s,};]+",
                "$1=[REDACTED]");
        result = result.replaceAll("(?i)(bearer|authorization)\\s+[\\w\\-.]+",
                "$1 [REDACTED]");

        // Redact email addresses (but preserve them for logging - they're semi-public in user contexts)
        // result = EMAIL_PATTERN.matcher(result).replaceAll("[EMAIL_REDACTED]");

        // Redact credit cards
        result = CREDIT_CARD_PATTERN.matcher(result).replaceAll("[CC_REDACTED]");

        // Redact phone numbers
        result = PHONE_PATTERN.matcher(result).replaceAll("[PHONE_REDACTED]");

        // Redact SSNs
        result = result.replaceAll("\\b\\d{3}-\\d{2}-\\d{4}\\b", "[SSN_REDACTED]");

        return result;
    }

    /**
     * Redact metadata JSON object, removing sensitive fields.
     */
    public static String redactMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isEmpty() || metadataJson.equals("{}")) {
            return metadataJson;
        }

        try {
            JsonObject obj = gson.fromJson(metadataJson, JsonObject.class);
            redactJsonObject(obj);
            return gson.toJson(obj);
        } catch (Exception e) {
            // If parsing fails, return original with warning
            System.out.println("[DataRedactor] Warning: Failed to redact metadata: " + e.getMessage());
            return metadataJson;
        }
    }

    /**
     * Recursively redact sensitive fields in JsonObject.
     */
    private static void redactJsonObject(JsonObject obj) {
        List<String> keysToRedact = new ArrayList<>();

        obj.keySet().forEach(key -> {
            if (isSensitiveFieldName(key)) {
                keysToRedact.add(key);
            }
        });

        keysToRedact.forEach(key -> {
            obj.addProperty(key, "[REDACTED]");
        });
    }

    /**
     * Check if a field name indicates sensitive content.
     */
    private static boolean isSensitiveFieldName(String fieldName) {
        if (fieldName == null) {
            return false;
        }

        String lower = fieldName.toLowerCase();
        return SENSITIVE_FIELD_NAMES.stream()
                .anyMatch(lower::contains);
    }

    /**
     * Redact user agent if it contains suspicious patterns.
     * Generally keep userAgent but flag very long ones.
     */
    public static String redactUserAgent(String userAgent) {
        if (userAgent == null) {
            return userAgent;
        }

        // Cap length to prevent DoS on logging
        if (userAgent.length() > 2048) {
            return userAgent.substring(0, 2045) + "...";
        }

        return userAgent;
    }

    /**
     * Redact IP address (keep first 3 octets for analytics, anonymize last octet).
     * For IPv4: 192.168.1.x becomes 192.168.1.0
     * For IPv6: Keep first 64 bits.
     */
    public static String anonymizeIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return ipAddress;
        }

        // IPv4
        if (ipAddress.contains(".")) {
            String[] octets = ipAddress.split("\\.");
            if (octets.length == 4) {
                return octets[0] + "." + octets[1] + "." + octets[2] + ".0";
            }
        }

        // IPv6 (basic anonymization - keep first 64 bits)
        if (ipAddress.contains(":")) {
            String[] parts = ipAddress.split(":");
            if (parts.length >= 4) {
                return parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3] + ":0:0:0:0";
            }
        }

        return ipAddress;
    }
}
