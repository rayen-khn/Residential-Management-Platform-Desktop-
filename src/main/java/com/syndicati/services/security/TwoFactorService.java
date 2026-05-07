package com.syndicati.services.security;

import com.syndicati.models.user.User;
import com.syndicati.models.user.data.UserRepository;
import com.syndicati.utils.session.SessionManager;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Two-Factor Authentication Service
 * Handles TOTP (Time-based One-Time Password) setup and verification
 */
public class TwoFactorService {
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int DIGITS = 6;
    private static final int WINDOW = 1; // Allow ±30 seconds
    
    private final UserRepository userRepository;
    
    public TwoFactorService() {
        this.userRepository = new UserRepository();
    }
    
    /**
     * Generate a new TOTP secret for the user
     */
    public String generateTotpSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
    
    /**
     * Generate TOTP QR code data URL
     */
    public String generateQRCodeDataUrl(String secret, String email, String issuer) {
        try {
            String label = issuer + ":" + email;
            String otpAuthUrl = String.format("otpauth://totp/%s?secret=%s&issuer=%s", 
                label, secret, issuer);
            
            // Use a simple QR code generation approach (base64 encoded SVG or use external service)
            return generateSimpleQRCode(otpAuthUrl);
        } catch (Exception e) {
            System.err.println("Error generating QR code: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Generate a simple QR code using data URL (in production, use a library like zxing)
     */
    private String generateSimpleQRCode(String data) {
        // For desktop app, we can use a simple approach:
        // Return a placeholder or use a local QR generation library
        // This is a simplified version - in production use zxing or similar
        return "data:text/plain;base64," + Base64.getEncoder().encodeToString(data.getBytes());
    }
    
    /**
     * Verify a TOTP code
     */
    public boolean verifyTotpCode(String secret, String code) {
        try {
            long timeWindow = System.currentTimeMillis() / 1000 / 30;
            
            for (int i = -WINDOW; i <= WINDOW; i++) {
                String generatedCode = generateTotpCode(secret, timeWindow + i);
                if (generatedCode.equals(code)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error verifying TOTP: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Generate TOTP code for a specific time window
     */
    private String generateTotpCode(String secret, long timeWindow) throws Exception {
        byte[] decodedSecret = Base64.getDecoder().decode(secret);
        byte[] timeBytes = new byte[8];
        
        for (int i = 7; i >= 0; i--) {
            timeBytes[i] = (byte) (timeWindow & 0xff);
            timeWindow >>= 8;
        }
        
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(decodedSecret, 0, decodedSecret.length, HMAC_ALGORITHM));
        byte[] hash = mac.doFinal(timeBytes);
        
        int offset = hash[hash.length - 1] & 0xf;
        int truncated = 0;
        
        for (int i = 0; i < 4; i++) {
            truncated = (truncated << 8) | (hash[offset + i] & 0xff);
        }
        
        truncated = (truncated & 0x7fffffff) % (int) Math.pow(10, DIGITS);
        return String.format("%0" + DIGITS + "d", truncated);
    }
    
    /**
     * Enable TOTP for current user
     */
    public boolean enableTotpForUser(String totpSecret) {
        try {
            User user = SessionManager.getInstance().getCurrentUser();
            if (user == null || user.getIdUser() == null) {
                return false;
            }
            
            user.setTotpSecret(totpSecret);
            user.setTwoFactorEnabled(true);
            userRepository.update(user);
            
            // Update session
            SessionManager.getInstance().setCurrentUser(user);
            return true;
        } catch (Exception e) {
            System.err.println("Error enabling TOTP: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Disable TOTP for current user
     */
    public boolean disableTotpForUser() {
        try {
            User user = SessionManager.getInstance().getCurrentUser();
            if (user == null || user.getIdUser() == null) {
                return false;
            }
            
            user.setTotpSecret(null);
            user.setTwoFactorEnabled(false);
            userRepository.update(user);
            
            // Update session
            SessionManager.getInstance().setCurrentUser(user);
            return true;
        } catch (Exception e) {
            System.err.println("Error disabling TOTP: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if TOTP is configured for current user
     */
    public boolean isTotpConfigured() {
        User user = SessionManager.getInstance().getCurrentUser();
        return user != null && user.getTotpSecret() != null && !user.getTotpSecret().isBlank();
    }
    
    /**
     * Get current TOTP status
     */
    public String getTotpStatus() {
        return isTotpConfigured() ? "Configured" : "Not Configured";
    }
}
