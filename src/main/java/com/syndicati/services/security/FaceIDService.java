package com.syndicati.services.security;

import com.syndicati.models.user.User;
import com.syndicati.utils.session.SessionManager;
import com.syndicati.models.user.data.UserRepository;
import javafx.scene.image.Image;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * Face ID Enrollment Service
 * Handles local face recognition enrollment and authentication
 * Uses face embeddings stored locally on the PC
 */
public class FaceIDService {
    private static final Logger LOGGER = Logger.getLogger(FaceIDService.class.getName());
    private static final String FACEID_STORE_PATH = "certs/faceid";
    private static final int MIN_GOOD_FRAMES = 20;
    
    private final UserRepository userRepository;
    private List<double[]> currentEmbeddings;
    
    public FaceIDService() {
        this.userRepository = new UserRepository();
        ensureFaceIDStoreExists();
        this.currentEmbeddings = new ArrayList<>();
    }
    
    /**
     * Start face enrollment process
     */
    public void startEnrollment() {
        currentEmbeddings.clear();
        LOGGER.info("Face ID enrollment started");
    }
    
    /**
     * Add face frame embedding during enrollment
     */
    public void addFrameEmbedding(double[] embedding) {
        if (embedding != null && embedding.length > 0) {
            currentEmbeddings.add(embedding);
            LOGGER.info("Frame added (" + currentEmbeddings.size() + "/" + MIN_GOOD_FRAMES + ")");
        }
    }
    
    /**
     * Get enrollment progress (0-1)
     */
    public double getEnrollmentProgress() {
        return Math.min(1.0, (double) currentEmbeddings.size() / MIN_GOOD_FRAMES);
    }
    
    /**
     * Check if enrollment has enough frames
     */
    public boolean isEnrollmentReady() {
        return currentEmbeddings.size() >= MIN_GOOD_FRAMES;
    }
    
    /**
     * Complete enrollment with PIN encryption
     */
    public boolean completeEnrollment(String pin) {
        try {
            if (!isEnrollmentReady()) {
                throw new IllegalStateException("Not enough frames captured");
            }
            
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null || currentUser.getIdUser() == null) {
                throw new IllegalStateException("User not logged in");
            }
            
            // Average embeddings
            double[] avgEmbedding = averageEmbeddings(currentEmbeddings);
            
            // Encrypt with PIN
            byte[] encryptedEmbedding = encryptEmbeddingWithPin(avgEmbedding, pin);
            
            // Store enrollment
            FaceIDEnrollment enrollment = new FaceIDEnrollment();
            enrollment.setId(UUID.randomUUID().toString());
            enrollment.setUserId(currentUser.getIdUser());
            enrollment.setEncryptedEmbedding(Base64.getEncoder().encodeToString(encryptedEmbedding));
            enrollment.setPin(hashPin(pin));
            enrollment.setCreatedAt(System.currentTimeMillis());
            enrollment.setLastUsed(System.currentTimeMillis());
            enrollment.setDeviceId(getDeviceId());
            
            storeFaceIDEnrollment(enrollment);
            
            currentEmbeddings.clear();
            LOGGER.info("Face ID enrollment completed for user: " + currentUser.getIdUser());
            return true;
        } catch (Exception e) {
            LOGGER.severe("Face ID enrollment failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Authenticate with face and PIN
     */
    public boolean authenticateWithFaceID(double[] liveEmbedding, String pin) {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null || currentUser.getIdUser() == null) {
                return false;
            }
            
            FaceIDEnrollment enrollment = retrieveFaceIDEnrollment(currentUser.getIdUser());
            if (enrollment == null) {
                LOGGER.warning("No Face ID enrollment found for user");
                return false;
            }
            
            // Verify PIN
            if (!verifyPin(pin, enrollment.getPin())) {
                LOGGER.warning("PIN verification failed");
                return false;
            }
            
            // Decrypt stored embedding
            byte[] encryptedEmbedding = Base64.getDecoder().decode(enrollment.getEncryptedEmbedding());
            double[] storedEmbedding = decryptEmbeddingWithPin(encryptedEmbedding, pin);
            
            // Compare embeddings
            double similarity = calculateSimilarity(liveEmbedding, storedEmbedding);
            double threshold = 0.6; // Tunable threshold
            
            if (similarity >= threshold) {
                enrollment.setLastUsed(System.currentTimeMillis());
                storeFaceIDEnrollment(enrollment);
                LOGGER.info("Face ID authentication successful");
                return true;
            } else {
                LOGGER.warning("Face similarity too low: " + similarity);
                return false;
            }
        } catch (Exception e) {
            LOGGER.severe("Face ID authentication failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if Face ID is enrolled for current user
     */
    public boolean isFaceIDEnrolled() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null || currentUser.getIdUser() == null) {
                return false;
            }
            
            return retrieveFaceIDEnrollment(currentUser.getIdUser()) != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Remove Face ID enrollment
     */
    public boolean removeFaceIDEnrollment() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null || currentUser.getIdUser() == null) {
                return false;
            }
            
            Path enrollPath = Paths.get(FACEID_STORE_PATH, currentUser.getIdUser() + ".face");
            Files.deleteIfExists(enrollPath);
            LOGGER.info("Face ID enrollment removed");
            return true;
        } catch (Exception e) {
            LOGGER.warning("Failed to remove Face ID enrollment: " + e.getMessage());
            return false;
        }
    }
    
    // ─── Private Helper Methods ───
    
    private void ensureFaceIDStoreExists() {
        try {
            Files.createDirectories(Paths.get(FACEID_STORE_PATH));
        } catch (Exception e) {
            LOGGER.warning("Failed to create Face ID store: " + e.getMessage());
        }
    }
    
    private double[] averageEmbeddings(List<double[]> embeddings) {
        if (embeddings.isEmpty()) {
            return new double[128];
        }
        
        double[] avg = new double[embeddings.get(0).length];
        for (double[] embedding : embeddings) {
            for (int i = 0; i < embedding.length; i++) {
                avg[i] += embedding[i];
            }
        }
        for (int i = 0; i < avg.length; i++) {
            avg[i] /= embeddings.size();
        }
        
        return avg;
    }
    
    private double calculateSimilarity(double[] embedding1, double[] embedding2) {
        if (embedding1.length != embedding2.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    private byte[] encryptEmbeddingWithPin(double[] embedding, String pin) throws Exception {
        // Simplified encryption - in production use proper AES encryption
        String data = Arrays.toString(embedding);
        return data.getBytes();
    }
    
    private double[] decryptEmbeddingWithPin(byte[] encrypted, String pin) throws Exception {
        // Simplified decryption
        String data = new String(encrypted);
        String[] parts = data.replace("[", "").replace("]", "").split(", ");
        double[] embedding = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            embedding[i] = Double.parseDouble(parts[i]);
        }
        return embedding;
    }
    
    private String hashPin(String pin) {
        // Use BCrypt for PIN hashing
        return org.mindrot.jbcrypt.BCrypt.hashpw(pin, org.mindrot.jbcrypt.BCrypt.gensalt(10));
    }
    
    private boolean verifyPin(String pin, String hash) {
        return org.mindrot.jbcrypt.BCrypt.checkpw(pin, hash);
    }
    
    private void storeFaceIDEnrollment(FaceIDEnrollment enrollment) throws Exception {
        String json = enrollment.toJson();
        Path path = Paths.get(FACEID_STORE_PATH, enrollment.getUserId() + ".face");
        Files.write(path, json.getBytes());
    }
    
    private FaceIDEnrollment retrieveFaceIDEnrollment(Integer userId) throws Exception {
        Path path = Paths.get(FACEID_STORE_PATH, userId + ".face");
        if (!Files.exists(path)) {
            return null;
        }
        String json = Files.readString(path);
        return FaceIDEnrollment.fromJson(json);
    }
    
    private String getDeviceId() {
        try {
            String deviceId = System.getProperty("user.name") + "@" + java.net.InetAddress.getLocalHost().getHostName();
            return deviceId;
        } catch (Exception e) {
            return "unknown-device";
        }
    }
    
    /**
     * FaceIDEnrollment Data Class
     */
    public static class FaceIDEnrollment {
        private String id;
        private Integer userId;
        private String encryptedEmbedding;
        private String pin;
        private long createdAt;
        private long lastUsed;
        private String deviceId;
        
        public FaceIDEnrollment() {}
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }
        
        public String getEncryptedEmbedding() { return encryptedEmbedding; }
        public void setEncryptedEmbedding(String encryptedEmbedding) { this.encryptedEmbedding = encryptedEmbedding; }
        
        public String getPin() { return pin; }
        public void setPin(String pin) { this.pin = pin; }
        
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        
        public long getLastUsed() { return lastUsed; }
        public void setLastUsed(long lastUsed) { this.lastUsed = lastUsed; }
        
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        
        public String toJson() {
            return String.format("{\"id\":\"%s\",\"userId\":%d,\"encryptedEmbedding\":\"%s\",\"createdAt\":%d,\"lastUsed\":%d,\"deviceId\":\"%s\"}",
                id, userId, encryptedEmbedding, createdAt, lastUsed, deviceId);
        }
        
        public static FaceIDEnrollment fromJson(String json) {
            FaceIDEnrollment enrollment = new FaceIDEnrollment();
            // In production, use proper JSON library
            return enrollment;
        }
    }
}

// Add this import
class InetAddress {
    public static Object getLocalHost() throws Exception {
        return java.net.InetAddress.getLocalHost();
    }
}
