package com.syndicati.services.openai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.syndicati.models.syndicat.Reclamation;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service to filter physical problems using OpenAI GPT-3.5.
 */
public class OpenAiReclamationService {
    private static final String API_KEY = "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";
    private static final String API_URL = "https://api.openai.com/v1/responses";
    
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
            
    private static final Gson gson = new Gson();

    /**
     * Filters a list of reclamations to return only those identified as physical maintenance problems.
     */
    public static List<Reclamation> filterPhysicalProblems(List<Reclamation> allReclamations) {
        if (allReclamations == null || allReclamations.isEmpty()) return new ArrayList<>();

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Analyze these building reclamations. Identify physical maintenance issues (plumbing, electrical, structural, leaks, broken items). ");
        promptBuilder.append("Return ONLY a JSON array of IDs for physical problems. Format: [1, 2, 5]. If none, return []. \n\n");
        
        for (Reclamation rec : allReclamations) {
            promptBuilder.append("ID: ").append(rec.getIdReclamations())
                    .append(" | ").append(rec.getTitreReclamations())
                    .append(": ").append(rec.getDescReclamation())
                    .append("\n");
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("model", "gpt-5.4-mini");
        payload.addProperty("input", promptBuilder.toString());
        payload.addProperty("store", true);

        RequestBody body = RequestBody.create(
                payload.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            String content = "";
            if (jsonResponse.has("output")) {
                content = jsonResponse.get("output").getAsString();
            } else if (jsonResponse.has("choices")) {
                content = jsonResponse.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
            } else if (jsonResponse.has("text")) {
                content = jsonResponse.get("text").getAsString();
            }

            // Sanitize AI response to get the array
            content = content.trim();
            if (content.contains("[") && content.contains("]")) {
                content = content.substring(content.indexOf("["), content.lastIndexOf("]") + 1);
            }
            
            JsonArray idArray = gson.fromJson(content, JsonArray.class);
            List<Integer> physicalIds = new ArrayList<>();
            for (int i = 0; i < idArray.size(); i++) {
                physicalIds.add(idArray.get(i).getAsInt());
            }

            List<Reclamation> filtered = new ArrayList<>();
            for (Reclamation rec : allReclamations) {
                if (physicalIds.contains(rec.getIdReclamations())) {
                    filtered.add(rec);
                }
            }
            return filtered;

        } catch (Exception e) {
            System.err.println("[OpenAI] API Error - Falling back to keyword analysis: " + e.getMessage());
            return applyKeywordFallback(allReclamations);
        }
    }

    /**
     * Fallback method that identifies physical problems using keyword matching.
     * Used when the AI service is unavailable.
     */
    private static List<Reclamation> applyKeywordFallback(List<Reclamation> allReclamations) {
        List<Reclamation> filtered = new ArrayList<>();
        // Comprehensive list of maintenance/physical problem keywords in French and English
        String[] keywords = {
            "fuite", "eau", "plomberie", "robinet", "tuyau", "inondation", "leak", "water", "plumbing",
            "électricité", "courant", "panne", "lampe", "ampoule", "prise", "compteur", "fil", "electric", "light", "bulb", "power",
            "cassé", "brisé", "vitre", "porte", "fenêtre", "serrure", "poignée", "broken", "glass", "door", "window", "lock",
            "ascenseur", "elevator", "lift",
            "chauffage", "clim", "climatisation", "heating", "ac",
            "mur", "fissure", "plafond", "peinture", "wall", "crack", "ceiling",
            "nettoyage", "sale", "poubelle", "odeur", "cleaning", "dirty", "trash", "smell"
        };

        for (Reclamation rec : allReclamations) {
            String content = (rec.getTitreReclamations() + " " + rec.getDescReclamation()).toLowerCase();
            for (String kw : keywords) {
                if (content.contains(kw)) {
                    filtered.add(rec);
                    break;
                }
            }
        }
        return filtered;
    }
}
