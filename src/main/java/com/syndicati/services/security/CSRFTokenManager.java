package com.syndicati.services.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * CSRF Token Manager - prevents cross-site request forgery attacks
 * Stateless CSRF protection for frontend requests
 */
public class CSRFTokenManager {
    private static final SecureRandom random = new SecureRandom();
    private static final Map<String, String> sessionTokens = new HashMap<>();
    private static final int TOKEN_LENGTH = 32;
    
    /**
     * Generate CSRF token for a session
     * @param sessionId Browser session ID
     * @return CSRF token (base64 encoded)
     */
    public static String generateToken(String sessionId) {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        random.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        sessionTokens.put(sessionId, token);
        return token;
    }
    
    /**
     * Validate CSRF token
     * @param sessionId Browser session ID
     * @param token Token from request header (X-CSRF-Token)
     * @return true if token is valid, false otherwise
     */
    public static boolean validateToken(String sessionId, String token) {
        if (sessionId == null || token == null) {
            return false;
        }
        
        String storedToken = sessionTokens.get(sessionId);
        if (storedToken == null) {
            return false;
        }
        
        // Constant-time comparison to prevent timing attacks
        return constantTimeEquals(storedToken, token);
    }
    
    /**
     * Constant-time string comparison to prevent timing attacks
     * @param a First string
     * @param b Second string
     * @return true if equal, false otherwise
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();
        
        if (aBytes.length != bBytes.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        
        return result == 0;
    }
    
    /**
     * Clear token (on logout or session expiry)
     * @param sessionId Browser session ID
     */
    public static void clearToken(String sessionId) {
        sessionTokens.remove(sessionId);
    }
}

