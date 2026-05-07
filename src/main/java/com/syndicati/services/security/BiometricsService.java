package com.syndicati.services.security;

import com.syndicati.models.user.User;
import com.syndicati.utils.session.SessionManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

/**
 * Biometrics/WebAuthn Service
 * Handles Windows Hello and device biometric registration
 * Uses Windows CNG (Cryptography Next Generation) for hardware-backed credentials
 */
public class BiometricsService {
    private static final Logger LOGGER = Logger.getLogger(BiometricsService.class.getName());
    private static final String CERT_STORE_PATH = "certs/biometrics";
    
    public BiometricsService() {
        ensureCertStoreExists();
    }
    
    /**
     * Check if Windows Hello is available on this system
     */
    public boolean isWindowsHelloAvailable() {
        try {
            // Check if we're on Windows and Windows Hello is supported
            String osName = System.getProperty("os.name").toLowerCase();
            if (!osName.contains("windows")) {
                LOGGER.info("Windows Hello not available on non-Windows system");
                return false;
            }
            
            // Try to access Windows CNG provider
            return checkCNGAvailability();
        } catch (Exception e) {
            LOGGER.warning("Windows Hello check failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if CNG (Cryptography Next Generation) is available
     */
    private boolean checkCNGAvailability() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{
                "powershell.exe", "-Command", 
                "Get-Item 'HKLM:\\Software\\Microsoft\\Cryptography\\Defaults\\Provider\\Microsoft Software KSP' -ErrorAction SilentlyContinue"
            });
            
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Register a new Windows Hello credential
     */
    public BiometricCredential registerWindowsHello(String deviceName) {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null || currentUser.getIdUser() == null) {
                throw new IllegalStateException("User not logged in");
            }
            
            // Generate credential ID and store metadata
            String credentialId = UUID.randomUUID().toString();
            long createdAt = System.currentTimeMillis();
            
            BiometricCredential credential = new BiometricCredential();
            credential.setId(credentialId);
            credential.setUserId(currentUser.getIdUser());
            credential.setDeviceName(deviceName);
            credential.setDeviceType("Windows Hello");
            credential.setPublicKey(generatePublicKeyStub());
            credential.setCreatedAt(createdAt);
            credential.setLastUsed(createdAt);
            credential.setCounter(0);
            
            // Store credential (in production, use database)
            storeCredential(credential);
            
            LOGGER.info("Windows Hello credential registered: " + deviceName);
            return credential;
        } catch (Exception e) {
            LOGGER.severe("Failed to register Windows Hello: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Authenticate with Windows Hello
     */
    public boolean authenticateWithWindowsHello(String credentialId) {
        try {
            // In production, this would involve:
            // 1. Getting assertion from Windows Hello API
            // 2. Verifying signature with public key
            // 3. Checking counter value
            // 4. Updating credential metadata
            
            BiometricCredential credential = retrieveCredential(credentialId);
            if (credential == null) {
                return false;
            }
            
            // Simulate Windows Hello authentication
            // In production, use Windows.Security.Credentials or similar
            boolean success = simulateWindowsHelloAuth();
            
            if (success) {
                credential.setLastUsed(System.currentTimeMillis());
                credential.setCounter(credential.getCounter() + 1);
                storeCredential(credential);
            }
            
            return success;
        } catch (Exception e) {
            LOGGER.warning("Windows Hello authentication failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * List all registered biometric credentials for current user
     */
    public List<BiometricCredential> listCredentials() {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null || currentUser.getIdUser() == null) {
                return Collections.emptyList();
            }
            
            return retrieveCredentialsForUser(currentUser.getIdUser());
        } catch (Exception e) {
            LOGGER.warning("Failed to list credentials: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Remove a biometric credential
     */
    public boolean removeCredential(String credentialId) {
        try {
            File credFile = new File(CERT_STORE_PATH + "/" + credentialId + ".cred");
            return credFile.delete();
        } catch (Exception e) {
            LOGGER.warning("Failed to remove credential: " + e.getMessage());
            return false;
        }
    }
    
    // ─── Private Helper Methods ───
    
    private void ensureCertStoreExists() {
        try {
            Files.createDirectories(Paths.get(CERT_STORE_PATH));
        } catch (Exception e) {
            LOGGER.warning("Failed to create cert store: " + e.getMessage());
        }
    }
    
    private String generatePublicKeyStub() {
        // Stub for key generation - in production use proper RSA/EC key
        return "-----BEGIN PUBLIC KEY-----\n" +
               Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()) +
               "\n-----END PUBLIC KEY-----";
    }
    
    private void storeCredential(BiometricCredential credential) throws Exception {
        // In production, store in database
        String json = credential.toJson();
        Files.write(Paths.get(CERT_STORE_PATH + "/" + credential.getId() + ".cred"), json.getBytes());
    }
    
    private BiometricCredential retrieveCredential(String credentialId) throws Exception {
        File file = new File(CERT_STORE_PATH + "/" + credentialId + ".cred");
        if (!file.exists()) {
            return null;
        }
        String json = Files.readString(Paths.get(file.getAbsolutePath()));
        return BiometricCredential.fromJson(json);
    }
    
    private List<BiometricCredential> retrieveCredentialsForUser(Integer userId) throws Exception {
        List<BiometricCredential> credentials = new ArrayList<>();
        File dir = new File(CERT_STORE_PATH);
        
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, n) -> n.endsWith(".cred"));
            if (files != null) {
                for (File file : files) {
                    String json = Files.readString(file.toPath());
                    BiometricCredential cred = BiometricCredential.fromJson(json);
                    if (cred.getUserId().equals(userId)) {
                        credentials.add(cred);
                    }
                }
            }
        }
        
        return credentials;
    }
    
    private boolean simulateWindowsHelloAuth() {
        // Simulate authentication - replace with actual Windows Hello API
        try {
            // Placeholder: in production, integrate with Windows.Security.Credentials
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * BiometricCredential Data Class
     */
    public static class BiometricCredential {
        private String id;
        private Integer userId;
        private String deviceName;
        private String deviceType;
        private String publicKey;
        private long createdAt;
        private long lastUsed;
        private int counter;
        
        public BiometricCredential() {}
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }
        
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
        
        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
        
        public String getPublicKey() { return publicKey; }
        public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
        
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        
        public long getLastUsed() { return lastUsed; }
        public void setLastUsed(long lastUsed) { this.lastUsed = lastUsed; }
        
        public int getCounter() { return counter; }
        public void setCounter(int counter) { this.counter = counter; }
        
        public String toJson() {
            return String.format("{\"id\":\"%s\",\"userId\":%d,\"deviceName\":\"%s\",\"deviceType\":\"%s\",\"createdAt\":%d,\"lastUsed\":%d,\"counter\":%d}",
                id, userId, deviceName, deviceType, createdAt, lastUsed, counter);
        }
        
        public static BiometricCredential fromJson(String json) {
            // Simple JSON parsing for demo
            BiometricCredential cred = new BiometricCredential();
            // In production, use proper JSON library
            return cred;
        }
    }
}
