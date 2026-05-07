package com.syndicati.services.forum;

import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Service to interact with the Python sentiment analysis microservice.
 */
public class SentimentAnalysisService {
    private static final String API_URL = "http://127.0.0.1:5000/api/full-analysis";
    private static Process pythonProcess;
    private final HttpClient httpClient;

    public SentimentAnalysisService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static void startMicroservice() {
        new Thread(() -> {
            try {
                System.out.println("🚀 Starting Sentiment AI Microservice...");
                ProcessBuilder pb = new ProcessBuilder("python", "sentiment_analysis_api/app.py");
                pb.directory(new File(System.getProperty("user.dir")));
                pb.inheritIO();
                pythonProcess = pb.start();
                
                Runtime.getRuntime().addShutdownHook(new Thread(SentimentAnalysisService::stopMicroservice));
            } catch (Exception e) {
                System.err.println("⚠️ Could not start Sentiment AI Microservice automatically: " + e.getMessage());
                System.err.println("Make sure 'python' is in your PATH and dependencies are installed.");
            }
        }).start();
    }

    public static void stopMicroservice() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            System.out.println("🛑 Stopping Sentiment AI Microservice...");
            pythonProcess.destroyForcibly();
            try {
                pythonProcess.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("✅ Sentiment AI Microservice stopped.");
        }
    }

    public CompletableFuture<SentimentResult> analyzeContent(String text) {
        if (text == null || text.trim().isEmpty()) {
            return CompletableFuture.completedFuture(new SentimentResult("Neutral", "100", "No content to analyze", "None", false));
        }

        JSONObject json = new JSONObject();
        json.put("text", text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .timeout(Duration.ofSeconds(60))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        JSONObject body = new JSONObject(response.body());
                        String sentimentRaw = body.getString("sentiment");
                        String emotionsRaw = body.getString("emotions");
                        
                        return parseResults(sentimentRaw, emotionsRaw);
                    } else {
                        return new SentimentResult("Error", "0", "API returned status: " + response.statusCode(), "Unknown", false);
                    }
                })
                .exceptionally(ex -> new SentimentResult("Offline", "0", "Sentiment API Error: " + ex.getMessage(), "Unknown", false));
    }

    private SentimentResult parseResults(String sentimentRaw, String emotionsRaw) {
        String sentiment = "Neutral";
        String confidence = "0";
        String explanation = "";
        String primaryEmotion = "Neutral";

        try {
            String[] lines = sentimentRaw.split("\n");
            for (String line : lines) {
                if (line.startsWith("Sentiment:")) sentiment = line.substring(10).trim();
                if (line.startsWith("Confidence:")) confidence = line.substring(11).trim().replace("%", "");
                if (line.startsWith("Explanation:")) explanation = line.substring(12).trim();
            }

            String[] eLines = emotionsRaw.split("\n");
            for (String line : eLines) {
                if (line.startsWith("Primary Emotion:")) primaryEmotion = line.substring(16).trim();
            }
        } catch (Exception e) {
            explanation = "Error parsing AI response";
        }

        return new SentimentResult(sentiment, confidence, explanation, primaryEmotion, true);
    }

    public static class SentimentResult {
        public final String sentiment;
        public final String confidence;
        public final String explanation;
        public final String primaryEmotion;
        public final boolean success;

        public SentimentResult(String sentiment, String confidence, String explanation, String primaryEmotion, boolean success) {
            this.sentiment = sentiment;
            this.confidence = confidence;
            this.explanation = explanation;
            this.primaryEmotion = primaryEmotion;
            this.success = success;
        }
    }
}
