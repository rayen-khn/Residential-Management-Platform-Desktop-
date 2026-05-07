package com.syndicati.services.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;
import java.util.Arrays;

/**
 * FaceID Encryption Service
 * Handles AES-256-GCM encryption for face embeddings
 * Uses PBKDF2 for key derivation with email-based salt
 * 
 * Matches web version: App\Service\Face\FaceEncryptionService
 */
public class FaceEncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH = 32;           // 256 bits
    private static final int IV_LENGTH = 12;            // 96 bits for GCM
    private static final int TAG_LENGTH = 128;          // 128 bits
    private static final int PBKDF2_ITERATIONS = 10000;
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * Derive a 32-byte key from a PIN and email using PBKDF2
     * 
     * @param pin User's PIN (numeric or alphanumeric)
     * @param userEmail Email used as static salt
     * @return 32-byte derived key
     */
    public byte[] deriveKey(String pin, String userEmail) {
        try {
            byte[] salt = userEmail.getBytes("UTF-8");
            KeySpec keySpec = new PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            return factory.generateSecret(keySpec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive key: " + e.getMessage(), e);
        }
    }

    /**
     * Encrypt data using AES-256-GCM
     * Returns binary packed data: IV (12 bytes) | TAG (16 bytes) | Ciphertext
     * 
     * @param data Plain text data to encrypt
     * @param key 32-byte AES key
     * @return Packed encrypted data
     */
    public byte[] encrypt(String data, byte[] key) {
        try {
            // Generate random 12-byte IV
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Create cipher
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            SecretKey secretKey = new SecretKeySpec(key, 0, key.length, ALGORITHM);
            GCMParameterSpec gcmParamSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParamSpec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(data.getBytes("UTF-8"));

            // Get the authentication tag (last 16 bytes of ciphertext)
            byte[] tag = new byte[16];
            System.arraycopy(ciphertext, ciphertext.length - 16, tag, 0, 16);

            // Get actual ciphertext without tag
            byte[] actualCiphertext = new byte[ciphertext.length - 16];
            System.arraycopy(ciphertext, 0, actualCiphertext, 0, actualCiphertext.length);

            // Pack: IV | TAG | Ciphertext
            byte[] result = new byte[IV_LENGTH + 16 + actualCiphertext.length];
            System.arraycopy(iv, 0, result, 0, IV_LENGTH);
            System.arraycopy(tag, 0, result, IV_LENGTH, 16);
            System.arraycopy(actualCiphertext, 0, result, IV_LENGTH + 16, actualCiphertext.length);

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt data: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt data using AES-256-GCM
     * Expects packed data: IV (12 bytes) | TAG (16 bytes) | Ciphertext
     * 
     * @param packedData Packed encrypted data
     * @param key 32-byte AES key
     * @return Decrypted plaintext or null if decryption fails
     */
    public String decrypt(byte[] packedData, byte[] key) {
        try {
            if (packedData == null || packedData.length < IV_LENGTH + 16) {
                return null;
            }

            // Extract components
            byte[] iv = new byte[IV_LENGTH];
            byte[] tag = new byte[16];
            byte[] ciphertext = new byte[packedData.length - IV_LENGTH - 16];

            System.arraycopy(packedData, 0, iv, 0, IV_LENGTH);
            System.arraycopy(packedData, IV_LENGTH, tag, 0, 16);
            System.arraycopy(packedData, IV_LENGTH + 16, ciphertext, 0, ciphertext.length);

            // Combine ciphertext and tag for decryption
            byte[] encryptedData = new byte[ciphertext.length + 16];
            System.arraycopy(ciphertext, 0, encryptedData, 0, ciphertext.length);
            System.arraycopy(tag, 0, encryptedData, ciphertext.length, 16);

            // Create cipher
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            SecretKey secretKey = new SecretKeySpec(key, 0, key.length, ALGORITHM);
            GCMParameterSpec gcmParamSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParamSpec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(encryptedData);
            return new String(plaintext, "UTF-8");
        } catch (Exception e) {
            // Return null on decryption failure (invalid PIN or corrupted data)
            return null;
        }
    }

    /**
     * Calculate Euclidean distance between two face embedding vectors
     * Used to compare enrolled face with current face detection
     * 
     * @param embedding1 First 384-dimensional face embedding
     * @param embedding2 Second 384-dimensional face embedding
     * @return Euclidean distance
     */
    public double calculateDistance(double[] embedding1, double[] embedding2) {
        if (embedding1 == null || embedding2 == null || embedding1.length != embedding2.length) {
            throw new IllegalArgumentException("Embeddings must have same length");
        }

        double sum = 0;
        for (int i = 0; i < embedding1.length; i++) {
            double diff = embedding1[i] - embedding2[i];
            sum += diff * diff;
        }

        return Math.sqrt(sum);
    }

    /**
     * Verify if face matches with default threshold
     * Default threshold from web version: 0.5
     * 
     * @param enrolledEmbedding Original enrolled face embedding
     * @param currentEmbedding Current detected face embedding
     * @return true if distance < 0.5
     */
    public boolean verifyFace(double[] enrolledEmbedding, double[] currentEmbedding) {
        return verifyFace(enrolledEmbedding, currentEmbedding, 0.5);
    }

    /**
     * Verify if face matches with custom threshold
     * 
     * @param enrolledEmbedding Original enrolled face embedding
     * @param currentEmbedding Current detected face embedding
     * @param threshold Maximum distance to consider a match
     * @return true if distance < threshold
     */
    public boolean verifyFace(double[] enrolledEmbedding, double[] currentEmbedding, double threshold) {
        double distance = calculateDistance(enrolledEmbedding, currentEmbedding);
        return distance < threshold;
    }
}

