package com.syndicati.services.log;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility to detect rapid-fire interactions (spamming) on UI elements.
 * Helps identify potential bots or frustrated users.
 */
public class SpamDetectionService {

    private static SpamDetectionService instance;
    
    // Tracks clicks: elementId -> lastClickTime
    private final Map<String, Instant> lastInteractions = new ConcurrentHashMap<>();
    // Tracks click counts: elementId -> count in window
    private final Map<String, Integer> interactionCounts = new ConcurrentHashMap<>();
    
    private static final int SPAM_WINDOW_MS = 2000; // 2 seconds
    private static final int SPAM_THRESHOLD = 5;    // 5 clicks in window

    private SpamDetectionService() {}

    public static synchronized SpamDetectionService getInstance() {
        if (instance == null) {
            instance = new SpamDetectionService();
        }
        return instance;
    }

    /**
     * Records an interaction and returns true if it is considered spam.
     */
    public boolean isSpamming(String elementId) {
        Instant now = Instant.now();
        Instant last = lastInteractions.get(elementId);
        
        lastInteractions.put(elementId, now);
        
        if (last != null && now.toEpochMilli() - last.toEpochMilli() < SPAM_WINDOW_MS) {
            int count = interactionCounts.getOrDefault(elementId, 0) + 1;
            interactionCounts.put(elementId, count);
            return count >= SPAM_THRESHOLD;
        } else {
            interactionCounts.put(elementId, 1);
            return false;
        }
    }

    /**
     * Checks for rapid navigation between different views.
     */
    public boolean isNavigationSpamming(String sessionId) {
        return isSpamming("nav_" + sessionId);
    }
}
