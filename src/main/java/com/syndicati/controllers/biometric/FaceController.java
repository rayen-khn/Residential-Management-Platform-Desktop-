package com.syndicati.controllers.biometric;

import com.syndicati.models.biometric.FaceCredential;
import com.syndicati.models.user.User;
import com.syndicati.models.biometric.data.FaceCredentialRepository;
import com.syndicati.services.security.FaceEncryptionService;
import com.syndicati.utils.session.SessionManager;
import com.syndicati.services.user.user.UserService;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * FaceID Authentication Controller
 * Handles face enrollment and authentication
 * Matches web version: App\Controller\FaceCred\FaceController
 */
public class FaceController {

    private static final double DISTANCE_THRESHOLD = 0.5;
    private String cachedDeviceId;
    private final FaceEncryptionService encryptionService;
    private final UserService userService;
    private final FaceCredentialRepository faceCredentialRepository;

    public FaceController() {
        this.encryptionService = new FaceEncryptionService();
        this.userService = new UserService();
        this.faceCredentialRepository = new FaceCredentialRepository();
    }

    /**
     * Enroll a user's face for a specific device
     * 
     * @param embedding Array of 384 face descriptor values
     * @param pin User's numeric PIN
     * @param deviceId Device identifier
     * @return Response map with status or error
     */
    public Map<String, Object> enrollFace(double[] embedding, String pin, String deviceId) {
        Map<String, Object> response = new HashMap<>();

        try {
            String resolvedDeviceId = normalizeDeviceId(deviceId);

            // Validate request
            if (embedding == null || pin == null) {
                response.put("error", "Missing required data: embedding, pin");
                return response;
            }

            // Get current user from session
            SessionManager sessionManager = SessionManager.getInstance();
            User currentUser = sessionManager.getCurrentUser();

            if (currentUser == null) {
                response.put("error", "User not logged in");
                return response;
            }

            if (embedding.length != 384) {
                response.put("error", "Invalid embedding: must be 384-dimensional");
                return response;
            }

            // Derive encryption key from PIN and email
            byte[] key = encryptionService.deriveKey(pin, currentUser.getEmailUser());

            // Serialize embedding to JSON and encrypt
            String embeddingJson = serializeEmbedding(embedding);
            byte[] encryptedData = encryptionService.encrypt(embeddingJson, key);

            // Check if device already has a credential
            Optional<FaceCredential> existingOpt = faceCredentialRepository.findActiveByUserAndDevice(currentUser.getIdUser(), resolvedDeviceId);
            
            FaceCredential credential;
            if (existingOpt.isPresent()) {
                credential = existingOpt.get();
            } else {
                credential = new FaceCredential();
                credential.setUserId(currentUser.getIdUser());
                credential.setDeviceId(resolvedDeviceId);
            }

            credential.setEncryptedFaceid(encryptedData);
            credential.setUpdatedAt(LocalDateTime.now());
            credential.setFlag("active");
            
            // Save to persistent database
            faceCredentialRepository.save(credential);

            response.put("status", "ok");
            response.put("message", "Face enrolled successfully");
            return response;

        } catch (Exception e) {
            response.put("error", "Enrollment failed: " + e.getMessage());
            return response;
        }
    }

    /**
     * Authenticate user with face recognition
     * 
     * @param email User's email
     * @param embedding Current detected face embedding
     * @param pin User's numeric PIN
     * @param deviceId Device identifier
     * @return Response map with user data or error
     */
    public Map<String, Object> authenticateWithFace(String email, double[] embedding, String pin, String deviceId) {
        Map<String, Object> response = new HashMap<>();

        try {
            String resolvedDeviceId = normalizeDeviceId(deviceId);

            // Validate request
            if (email == null || embedding == null || pin == null) {
                response.put("error", "Missing required data: email, embedding, pin");
                return response;
            }

            // Find user by email
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                response.put("error", "User not found");
                return response;
            }

            User user = userOpt.get();
            // Check if user is verified and not disabled
            if (!user.isVerified()) {
                response.put("error", "Account not verified by administrator");
                return response;
            }

            if (user.isDisabled()) {
                response.put("error", "Account is disabled");
                return response;
            }

            // Find enrolled credential for this device
            FaceCredential credential = findActiveForUserAndDevice(user.getIdUser(), resolvedDeviceId);
            if (credential == null || credential.getEncryptedFaceid() == null) {
                response.put("error", "No FaceID enrolled for this device");
                return response;
            }

            // Derive key and decrypt stored embedding
            byte[] key = encryptionService.deriveKey(pin, user.getEmailUser());
            String decryptedEmbeddingJson = encryptionService.decrypt(credential.getEncryptedFaceid(), key);

            if (decryptedEmbeddingJson == null) {
                response.put("error", "Invalid PIN or corrupted data");
                return response;
            }

            // Parse stored embedding
            double[] storedEmbedding = parseEmbeddingFromString(decryptedEmbeddingJson);

            // Calculate distance
            double distance = encryptionService.calculateDistance(embedding, storedEmbedding);

            // Verify face match
            if (distance < DISTANCE_THRESHOLD) {
                // Update last used timestamp
                credential.setLastUsedAt(LocalDateTime.now());

                // Create session
                SessionManager sessionManager = SessionManager.getInstance();
                sessionManager.setCurrentUser(user);

                response.put("status", "ok");
                response.put("message", "Face verified successfully");
                response.put("distance", distance);
                response.put("user", user.getFirstName() + " " + user.getLastName());
                return response;
            }

            response.put("error", "Face mismatch");
            response.put("distance", distance);
            response.put("threshold", DISTANCE_THRESHOLD);
            return response;

        } catch (Exception e) {
            response.put("error", "Authentication failed: " + e.getMessage());
            return response;
        }
    }

    /**
     * Find active face credential for user and device
     * In production, this would query the database
     */
    private FaceCredential findActiveForUserAndDevice(Integer userId, String deviceId) {
        Optional<FaceCredential> credentialOpt = faceCredentialRepository.findActiveByUserAndDevice(userId, deviceId);
        return credentialOpt.orElse(null);
    }

    /**
     * Delete/disable face credential for a device
     */
    public Map<String, Object> disableFace(Integer userId, String deviceId) {
        Map<String, Object> response = new HashMap<>();

        FaceCredential credential = findActiveForUserAndDevice(userId, normalizeDeviceId(deviceId));
        if (credential == null) {
            response.put("error", "Credential not found");
            return response;
        }

        credential.setFlag("inactive");
        credential.setUpdatedAt(LocalDateTime.now());
        faceCredentialRepository.save(credential);
        
        response.put("status", "ok");
        response.put("message", "FaceID disabled successfully");
        return response;
    }

    /**
     * List all enrolled face credentials for a user
     */
    public List<Map<String, Object>> listUserFaceCredentials(Integer userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        List<FaceCredential> credentials = faceCredentialRepository.findAllActiveByUser(userId);

        for (FaceCredential cred : credentials) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", cred.getIdFacecred());
            item.put("deviceId", cred.getDeviceId());
            item.put("flag", cred.getFlag());
            item.put("createdAt", cred.getCreatedAt());
            item.put("lastUsedAt", cred.getLastUsedAt());
            list.add(item);
        }

        return list;
    }

    /**
     * Serialize embedding array to string
     */
    private String serializeEmbedding(double[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Parse embedding array from string representation
     */
    private double[] parseEmbeddingFromString(String str) {
        String cleaned = str.replace("[", "").replace("]", "").trim();
        String[] parts = cleaned.split(",");
        double[] result = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Double.parseDouble(parts[i].trim());
        }
        return result;
    }

    /**
     * Check if a user already has face enrollment for a specific device
     * @param userId User ID
     * @param deviceId Device ID
     * @return true if enrollment exists and is active
     */
    public boolean hasActiveFaceEnrollment(Integer userId, String deviceId) {
        FaceCredential credential = findActiveForUserAndDevice(userId, normalizeDeviceId(deviceId));
        return credential != null && "active".equals(credential.getFlag());
    }
    
    /**
     * Check if a user has any active face enrollment regardless of device.
     */
    public boolean hasAnyActiveFaceEnrollment(Integer userId) {
        Optional<FaceCredential> credential = faceCredentialRepository.findAnyActiveByUser(userId);
        return credential.isPresent() && "active".equalsIgnoreCase(credential.get().getFlag());
    }

    /**
     * Get enrollment details for a user and device
     * @param userId User ID
     * @param deviceId Device ID
     * @return Map with enrollment details or empty map if not found
     */
    public Map<String, Object> getFaceEnrollmentDetails(Integer userId, String deviceId) {
        Map<String, Object> details = new HashMap<>();
        FaceCredential credential = findActiveForUserAndDevice(userId, normalizeDeviceId(deviceId));
        
        if (credential != null && "active".equals(credential.getFlag())) {
            details.put("enrolled", true);
            details.put("deviceId", credential.getDeviceId());
            details.put("createdAt", credential.getCreatedAt());
            details.put("lastUsedAt", credential.getLastUsedAt());
        } else {
            details.put("enrolled", false);
        }
        
        return details;
    }

    public String resolveCurrentDeviceId() {
        if (cachedDeviceId != null && !cachedDeviceId.isBlank()) {
            return cachedDeviceId;
        }

        String host = null;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            // Fallback to environment variables below.
        }

        if (host == null || host.isBlank()) {
            host = System.getenv("COMPUTERNAME");
        }
        if (host == null || host.isBlank()) {
            host = System.getenv("HOSTNAME");
        }
        if (host == null || host.isBlank()) {
            host = "unknown-host";
        }

        String os = System.getProperty("os.name", "unknown-os");
        String user = System.getProperty("user.name", "user");

        cachedDeviceId = (os + "-" + host + "-" + user).replaceAll("[^a-zA-Z0-9._-]", "_");
        return cachedDeviceId;
    }

    private String normalizeDeviceId(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return resolveCurrentDeviceId();
        }
        return deviceId.trim();
    }
}

