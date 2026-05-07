package com.syndicati.services.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Locale;

/**
 * TOTP (Time-based One-Time Password) Service
 * Compatible with Google Authenticator, Authy, Microsoft Authenticator
 * Matches web version TOTP implementation
 */
public class TOTPService {
    private static final String ALGORITHM = "HmacSHA1";
    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP = 30;  // 30 seconds
    private static final String ISSUER = "Syndicati";
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    
    /**
     * Generate a new TOTP secret (Base32 encoded)
     * @return 32-character Base32 encoded secret
     */
    public static String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[20];  // 160 bits
        random.nextBytes(randomBytes);
        return base32Encode(randomBytes);
    }
    
    /**
     * Generate OTPAuth URI for QR code
     * @param email User email
     * @param secret TOTP secret
     * @return OTPAuth URI string
     */
    public static String generateOTPAuthURI(String email, String secret) {
        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s",
            ISSUER, email, secret, ISSUER
        );
    }
    
    /**
     * Verify TOTP code
     * @param secret Base32 encoded secret
     * @param code 6-digit code from authenticator app
    * @param leeway Time window in steps (1 = +/-30 seconds)
     * @return true if code is valid
     */
    public static boolean verifyCode(String secret, String code, int leeway) {
        if (secret == null || code == null || code.length() != CODE_DIGITS) {
            return false;
        }
        
        try {
            int codeInt = Integer.parseInt(code);
            long currentTime = System.currentTimeMillis() / 1000;
            long timeStep = currentTime / TIME_STEP;
            
            // Check current and surrounding time windows
            for (int i = -leeway; i <= leeway; i++) {
                long expectedCode = generateCode(secret, timeStep + i);
                if (codeInt == expectedCode) {
                    return true;
                }
            }
            
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
    * Verify TOTP code with default leeway (1 step = +/-30 seconds)
     * @param secret Base32 encoded secret
     * @param code 6-digit code from authenticator app
     * @return true if code is valid
     */
    public static boolean verifyCode(String secret, String code) {
        return verifyCode(secret, code, 1);
    }
    
    /**
     * Generate TOTP code for a specific time step
     * @param secret Base32 encoded secret
     * @param timeStep Time step (currentTime / 30)
     * @return 6-digit TOTP code
     */
    private static long generateCode(String secret, long timeStep) {
        try {
            byte[] secretBytes = base32Decode(secret);
            byte[] timeBytes = new byte[8];
            
            for (int i = 7; i >= 0; i--) {
                timeBytes[i] = (byte) (timeStep & 0xff);
                timeStep >>= 8;
            }
            
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, 0, secretBytes.length, ALGORITHM));
            byte[] hash = mac.doFinal(timeBytes);
            
            int offset = hash[hash.length - 1] & 0x0F;
            long truncatedHash = 0;
            for (int i = 0; i < 4; ++i) {
                truncatedHash <<= 8;
                truncatedHash |= (hash[offset + i] & 0xff);
            }
            
            truncatedHash &= 0x7FFFFFFF;
            truncatedHash %= 1000000;
            
            return truncatedHash;
        } catch (Exception e) {
            throw new RuntimeException("TOTP verification failed", e);
        }
    }
    
    /**
     * Encode bytes to Base32 (RFC 4648)
     * @param data Bytes to encode
     * @return Base32 string
     */
    private static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int bitIndex = 0;
        int currentByte = 0;
        
        for (byte b : data) {
            int eightBits = b & 0xFF;
            
            for (int i = 7; i >= 0; i--) {
                currentByte = (currentByte << 1) | ((eightBits >> i) & 1);
                bitIndex++;
                
                if (bitIndex == 5) {
                    sb.append(BASE32_ALPHABET.charAt(currentByte));
                    currentByte = 0;
                    bitIndex = 0;
                }
            }
        }
        
        // Padding
        if (bitIndex > 0) {
            currentByte <<= (5 - bitIndex);
            sb.append(BASE32_ALPHABET.charAt(currentByte));
        }
        
        while (sb.length() % 8 != 0) {
            sb.append('=');
        }
        
        return sb.toString();
    }
    
    /**
     * Decode Base32 string to bytes
     * @param encoded Base32 string
     * @return Decoded bytes
     */
    private static byte[] base32Decode(String encoded) {
        encoded = encoded.replaceAll("=", "").toUpperCase(Locale.US);
        
        byte[] data = new byte[encoded.length() * 5 / 8];
        int bitIndex = 0;
        int byteIndex = 0;
        int currentByte = 0;
        
        for (char c : encoded.toCharArray()) {
            int index = BASE32_ALPHABET.indexOf(c);
            if (index == -1) {
                throw new IllegalArgumentException("Invalid Base32 character: " + c);
            }
            
            for (int i = 4; i >= 0; i--) {
                currentByte = (currentByte << 1) | ((index >> i) & 1);
                bitIndex++;
                
                if (bitIndex == 8) {
                    data[byteIndex++] = (byte) currentByte;
                    currentByte = 0;
                    bitIndex = 0;
                }
            }
        }
        
        if (byteIndex < data.length) {
            return Arrays.copyOf(data, byteIndex);
        }
        
        return data;
    }
}


