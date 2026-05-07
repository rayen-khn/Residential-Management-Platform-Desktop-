package com.syndicati.services.forum;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * Service to interact with the OpenAI Moderation API.
 */
public class OpenAIModerationService {

    private static final String API_URL = "https://api.openai.com/v1/moderations";
    // Key provided by the user
    private static final String API_KEY = "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";
    
    private final HttpClient httpClient;

    public OpenAIModerationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Checks the provided text for bad words/inappropriate content using OpenAI Moderation API.
     * @param text The text to check (e.g. title + " " + description)
     * @return A list of flagged categories. If empty, the content is safe.
     */
    public List<String> checkContent(String text) {
        List<String> flaggedCategories = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return flaggedCategories;
        }

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("input", text);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONArray results = jsonResponse.optJSONArray("results");

                if (results != null && results.length() > 0) {
                    JSONObject result = results.getJSONObject(0);
                    boolean flagged = result.optBoolean("flagged", false);

                    if (flagged) {
                        JSONObject categories = result.optJSONObject("categories");
                        if (categories != null) {
                            Iterator<String> keys = categories.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                if (categories.getBoolean(key)) {
                                    // Capitalize and format the category name nicely
                                    String niceName = key.replace("/", " / ").replace("-", " ");
                                    niceName = niceName.substring(0, 1).toUpperCase() + niceName.substring(1);
                                    flaggedCategories.add(niceName);
                                }
                            }
                        }
                    }
                }
            } else {
                // If rate limited or error, fallback to local filter silently
                fallbackLocalCheck(text, flaggedCategories);
            }

        } catch (Exception e) {
            // If network fails, fallback to local filter silently
            fallbackLocalCheck(text, flaggedCategories);
        }

        return flaggedCategories;
    }

    /**
     * Local fallback filter when OpenAI is rate-limited or unavailable.
     * Uses a predefined list of unacceptable words to ensure moderation never stops working.
     */
    private void fallbackLocalCheck(String text, List<String> flaggedCategories) {
        String[] badWords = {
            "worthless", "useless", "stupid", "idiot", "hate", "kill", "die",
            "merde", "putain", "connard", "salope", "bitch", "fuck", "shit"
        };
        
        String lowerText = text.toLowerCase();
        for (String word : badWords) {
            // Basic word boundary check for English & French test words
            if (lowerText.matches(".*\\b" + word + "\\b.*")) {
                flaggedCategories.add("Contenu Offensant (Filtre de secours local activé)");
                return; // Stop at first match
            }
        }
    }
}
