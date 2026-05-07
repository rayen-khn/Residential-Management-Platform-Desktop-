package com.syndicati.services.forum;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.UUID;

/**
 * Service to send announcements to Discord via Webhooks with image attachments.
 * Standardized for reliable multipart delivery.
 */
public class DiscordWebhookService {
    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1498849548381061131/AMVuSesWRYxw3_dOG7g1Gflrr-9HUEyvZ6JYvqsaN30t3duastKAjmcR6ubpxBUi9KPR"; 
    
    private final HttpClient httpClient;

    public DiscordWebhookService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void sendAnnouncement(String title, String description, String authorName, String imageFileName, boolean isUpdate) {
        if (WEBHOOK_URL == null || WEBHOOK_URL.isEmpty() || WEBHOOK_URL.contains("YOUR_DISCORD_WEBHOOK_URL")) {
            return;
        }

        try {
            String boundary = "SyndicatiBoundary" + System.currentTimeMillis();
            
            // Build the JSON payload
            JSONObject json = new JSONObject();
            String prefix = isUpdate ? "🔄 **Mise à jour d'une**" : "🎮 **Nouvelle**";
            json.put("content", prefix + " **Publication dans Jeux Vidéo !**");
            
            JSONObject embed = new JSONObject();
            embed.put("title", "📍 Visit the platform Syndicati");
            
            // Layout: Title on one line, Author on another
            String embedDescription = "**Titre:** " + title + "\n" +
                                     "**Posté par:** " + authorName + "\n\n" +
                                     (description.length() > 1500 ? description.substring(0, 1497) + "..." : description);
            
            embed.put("description", embedDescription);
            embed.put("color", 5814783); // Discord Blurple
            
            JSONObject footer = new JSONObject();
            footer.put("text", "Syndicati Forum • " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            embed.put("footer", footer);

            // Thumbnail icon
            JSONObject thumbnail = new JSONObject();
            thumbnail.put("url", "https://cdn-icons-png.flaticon.com/512/686/686588.png");
            embed.put("thumbnail", thumbnail);

            File imageFile = null;
            if (imageFileName != null && !imageFileName.isEmpty()) {
                imageFile = new File(System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "forum_images" + File.separator + imageFileName);
                if (imageFile.exists()) {
                    JSONObject imageObj = new JSONObject();
                    imageObj.put("url", "attachment://image.png"); 
                    embed.put("image", imageObj);
                }
            }

            JSONArray embeds = new JSONArray();
            embeds.put(embed);
            json.put("embeds", embeds);

            // Create Multipart Body
            byte[] body = createMultipartBody(boundary, json.toString(), imageFile);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(WEBHOOK_URL))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            
        } catch (Exception e) {
            System.err.println("Discord Error: " + e.getMessage());
        }
    }

    private byte[] createMultipartBody(String boundary, String jsonPayload, File file) throws IOException {
        String separator = "--" + boundary + "\r\n";
        String end = "--" + boundary + "--\r\n";
        
        // Part 1: payload_json
        StringBuilder sb = new StringBuilder();
        sb.append(separator);
        sb.append("Content-Disposition: form-data; name=\"payload_json\"\r\n");
        sb.append("Content-Type: application/json\r\n\r\n");
        sb.append(jsonPayload).append("\r\n");
        
        byte[] jsonPart = sb.toString().getBytes(StandardCharsets.UTF_8);
        
        if (file != null && file.exists()) {
            // Part 2: file
            StringBuilder fileHeader = new StringBuilder();
            fileHeader.append(separator);
            fileHeader.append("Content-Disposition: form-data; name=\"file\"; filename=\"image.png\"\r\n");
            fileHeader.append("Content-Type: image/png\r\n\r\n");
            
            byte[] head = fileHeader.toString().getBytes(StandardCharsets.UTF_8);
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            byte[] foot = ("\r\n" + end).getBytes(StandardCharsets.UTF_8);
            
            byte[] total = new byte[jsonPart.length + head.length + fileBytes.length + foot.length];
            System.arraycopy(jsonPart, 0, total, 0, jsonPart.length);
            System.arraycopy(head, 0, total, jsonPart.length, head.length);
            System.arraycopy(fileBytes, 0, total, jsonPart.length + head.length, fileBytes.length);
            System.arraycopy(foot, 0, total, jsonPart.length + head.length + fileBytes.length, foot.length);
            return total;
        }
        
        byte[] endPart = end.getBytes(StandardCharsets.UTF_8);
        byte[] total = new byte[jsonPart.length + endPart.length];
        System.arraycopy(jsonPart, 0, total, 0, jsonPart.length);
        System.arraycopy(endPart, 0, total, jsonPart.length, endPart.length);
        return total;
    }
}
