package com.syndicati.services.security;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Login rate limiter to prevent brute force attacks
 * Matches web version: max 5 attempts per 15 minutes
 */
public class LoginRateLimiter {
    private static class LoginAttempt {
        int count;
        LocalDateTime firstAttempt;
        LocalDateTime lastAttempt;
        
        LoginAttempt(LocalDateTime now) {
            this.count = 1;
            this.firstAttempt = now;
            this.lastAttempt = now;
        }
    }
    
    private static final Map<String, LoginAttempt> attempts = new HashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;
    
    /**
     * Check if user is rate-limited
     * @param email User email
     * @return true if rate-limited, false if allowed
     */
    public static boolean isRateLimited(String email) {
        String key = email.toLowerCase().trim();
        LocalDateTime now = LocalDateTime.now();
        
        if (!attempts.containsKey(key)) {
            return false;
        }
        
        LoginAttempt attempt = attempts.get(key);
        long minutesSinceFirst = ChronoUnit.MINUTES.between(attempt.firstAttempt, now);
        
        // Reset if outside lockout window
        if (minutesSinceFirst > LOCKOUT_MINUTES) {
            attempts.remove(key);
            return false;
        }
        
        // Check if max attempts exceeded
        return attempt.count >= MAX_ATTEMPTS;
    }
    
    /**
     * Record failed login attempt
     * @param email User email
     */
    public static void recordFailedAttempt(String email) {
        String key = email.toLowerCase().trim();
        LocalDateTime now = LocalDateTime.now();
        
        if (!attempts.containsKey(key)) {
            attempts.put(key, new LoginAttempt(now));
        } else {
            LoginAttempt attempt = attempts.get(key);
            long minutesSinceFirst = ChronoUnit.MINUTES.between(attempt.firstAttempt, now);
            
            // Reset if outside lockout window
            if (minutesSinceFirst > LOCKOUT_MINUTES) {
                attempts.put(key, new LoginAttempt(now));
            } else {
                attempt.count++;
                attempt.lastAttempt = now;
            }
        }
    }
    
    /**
     * Clear rate limit for email (on successful login)
     * @param email User email
     */
    public static void clearAttempts(String email) {
        attempts.remove(email.toLowerCase().trim());
    }
    
    /**
     * Get remaining attempts before lockout
     * @param email User email
     * @return Remaining attempts, -1 if locked out
     */
    public static int getRemainingAttempts(String email) {
        String key = email.toLowerCase().trim();
        
        if (!attempts.containsKey(key)) {
            return MAX_ATTEMPTS;
        }
        
        if (isRateLimited(key)) {
            return -1;
        }
        
        LoginAttempt attempt = attempts.get(key);
        return MAX_ATTEMPTS - attempt.count;
    }
    
    /**
     * Get lockout time remaining in seconds
     * @param email User email
     * @return Seconds remaining, 0 if not locked out
     */
    public static long getLockoutTimeRemaining(String email) {
        String key = email.toLowerCase().trim();
        
        if (!attempts.containsKey(key) || !isRateLimited(key)) {
            return 0;
        }
        
        LoginAttempt attempt = attempts.get(key);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime unlockTime = attempt.firstAttempt.plusMinutes(LOCKOUT_MINUTES);
        
        return ChronoUnit.SECONDS.between(now, unlockTime);
    }
}

