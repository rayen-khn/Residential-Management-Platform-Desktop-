package com.syndicati.controllers.biometric;

import com.syndicati.models.biometric.WebAuthnCredential;
import com.syndicati.models.user.User;
import com.syndicati.utils.session.SessionManager;
import com.syndicati.services.user.user.UserService;
import java.time.LocalDateTime;
import java.util.*;

/**
 * WebAuthn/Passkey Authentication Controller
 * Handles FIDO2/WebAuthn credential registration and authentication
 * Matches web version: App\Controller\WebAuthn\WebAuthnController
 */
public class WebAuthnController {

    private final UserService userService;
    private final List<WebAuthnCredential> credentialStorage; // In-memory storage (replace with DB)
    private final Map<String, WebAuthnChallenge> activeChallenges; // Store registration/login challenges

    public static class WebAuthnChallenge {
        public String challenge;
        public Integer userId;
        public String email;
        public Long timestamp;

        public WebAuthnChallenge(String challenge, Integer userId, String email) {
            this.challenge = challenge;
            this.userId = userId;
            this.email = email;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired(long timeoutMs) {
            return System.currentTimeMillis() - timestamp > timeoutMs;
        }
    }

    public WebAuthnController() {
        this.userService = new UserService();
        this.credentialStorage = new ArrayList<>();
        this.activeChallenges = new HashMap<>();
    }

    /**
     * Generate options for WebAuthn registration
     * MUST be called by authenticated user
     * 
     * POST /webauthn/register/options
     */
    public Map<String, Object> getRegistrationOptions() {
        Map<String, Object> response = new HashMap<>();

        try {
            SessionManager sessionManager = SessionManager.getInstance();
            User currentUser = sessionManager.getCurrentUser();

            if (currentUser == null) {
                response.put("error", "User not logged in");
                return response;
            }

            // Generate random 32-byte challenge
            String challenge = generateChallenge();

            // Store challenge in session for verification later
            WebAuthnChallenge webChallenge = new WebAuthnChallenge(challenge, currentUser.getIdUser(), currentUser.getEmailUser());
            activeChallenges.put(challenge, webChallenge);

            // Create options object matching web version
            Map<String, Object> options = new HashMap<>();
            options.put("challenge", challenge);
            options.put("rp", Map.of(
                "name", "Syndicati",
                "id", "localhost"
            ));
            options.put("user", Map.of(
                "id", currentUser.getIdUser().toString(),
                "name", currentUser.getEmailUser(),
                "displayName", currentUser.getFirstName() + " " + currentUser.getLastName()
            ));
            options.put("pubKeyCredParams", new Object[]{
                Map.of("type", "public-key", "alg", -7),    // ES256
                Map.of("type", "public-key", "alg", -257)   // RS256
            });
            options.put("timeout", 60000);
            options.put("attestation", "direct");
            options.put("authenticatorSelection", Map.of(
                "authenticatorAttachment", "platform",
                "requireResidentKey", true,
                "userVerification", "required"
            ));

            response.put("options", options);
            return response;

        } catch (Exception e) {
            response.put("error", "Failed to generate registration options: " + e.getMessage());
            return response;
        }
    }

    /**
     * Verify and store WebAuthn credential
     * 
     * POST /webauthn/register/verify
     */
    public Map<String, Object> verifyRegistration(String credentialId, String publicKey, List<String> transports) {
        Map<String, Object> response = new HashMap<>();

        try {
            SessionManager sessionManager = SessionManager.getInstance();
            User currentUser = sessionManager.getCurrentUser();

            if (currentUser == null) {
                response.put("error", "User not logged in");
                return response;
            }

            if (credentialId == null || credentialId.isEmpty() || publicKey == null || publicKey.isEmpty()) {
                response.put("error", "credentialId and publicKey are required");
                return response;
            }

            // In production: Verify attestation response against challenge
            // For now: Accept all valid requests

            if (transports == null) {
                transports = new ArrayList<>();
            }

            // Check if credential already exists
            if (credentialExists(credentialId)) {
                response.put("error", "Credential already registered");
                return response;
            }

            // Create credential
            WebAuthnCredential credential = new WebAuthnCredential();
            credential.setUserId(currentUser.getIdUser());
            credential.setCredentialId(credentialId);
            credential.setPublicKey(publicKey);
            credential.setSignCount(0);
            credential.setTransports(transports);

            credentialStorage.add(credential);

            response.put("status", "ok");
            response.put("message", "Credential registered successfully");
            return response;

        } catch (Exception e) {
            response.put("error", "Registration verification failed: " + e.getMessage());
            return response;
        }
    }

    /**
     * Generate options for WebAuthn login
     * 
     * POST /webauthn/login/options
     */
    public Map<String, Object> getLoginOptions(String email) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (email == null || email.isEmpty()) {
                response.put("error", "Email is required");
                return response;
            }

            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                response.put("error", "User not found");
                return response;
            }
            User user = userOpt.get();

            // Find all credentials for this user
            List<WebAuthnCredential> userCredentials = findCredentialsForUser(user.getIdUser());
            if (userCredentials.isEmpty()) {
                response.put("error", "No WebAuthn credentials enrolled for this user");
                return response;
            }

            // Generate challenge
            String challenge = generateChallenge();
            WebAuthnChallenge webChallenge = new WebAuthnChallenge(challenge, user.getIdUser(), email);
            activeChallenges.put(challenge, webChallenge);

            // Build allowCredentials list
            List<Map<String, Object>> allowCredentials = new ArrayList<>();
            for (WebAuthnCredential cred : userCredentials) {
                Map<String, Object> credDesc = new HashMap<>();
                credDesc.put("type", "public-key");
                credDesc.put("id", cred.getCredentialId());
                List<String> transports = cred.getTransports();
                if (transports == null || transports.isEmpty()) {
                    transports = Arrays.asList("internal", "hybrid", "usb", "nfc", "ble");
                }
                credDesc.put("transports", transports);
                allowCredentials.add(credDesc);
            }

            // Create options
            Map<String, Object> options = new HashMap<>();
            options.put("challenge", challenge);
            options.put("rp", Map.of(
                "name", "Syndicati",
                "id", "localhost"
            ));
            options.put("timeout", 60000);
            options.put("userVerification", "required");
            options.put("allowCredentials", allowCredentials);

            response.put("options", options);
            return response;

        } catch (Exception e) {
            response.put("error", "Failed to generate login options: " + e.getMessage());
            return response;
        }
    }

    /**
     * Verify WebAuthn assertion and create user session
     * 
     * POST /webauthn/login/verify
     */
    public Map<String, Object> verifyAssertion(String credentialId, Integer signCount) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (credentialId == null || credentialId.isEmpty()) {
                response.put("error", "credentialId is required");
                return response;
            }

            // Find credential
            WebAuthnCredential credential = findCredentialByCredentialId(credentialId);
            if (credential == null) {
                response.put("error", "Credential not found");
                return response;
            }

            Optional<User> userOpt = userService.findById(credential.getUserId());
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

            // In production: Verify assertion signature and clientDataJSON
            // For now: Accept if credential exists

            // Update sign count for cloning detection
            Integer newSignCount = signCount != null ? signCount : credential.getSignCount() + 1;
            if (newSignCount <= credential.getSignCount()) {
                response.put("error", "Possible credential cloning detected");
                return response;
            }

            credential.setSignCount(newSignCount);
            credential.setLastUsedAt(LocalDateTime.now());

            // Create user session
            SessionManager sessionManager = SessionManager.getInstance();
            sessionManager.setCurrentUser(user);

            response.put("status", "ok");
            response.put("message", "Authentication successful");
            response.put("user", user.getFirstName() + " " + user.getLastName());
            return response;

        } catch (Exception e) {
            response.put("error", "Assertion verification failed: " + e.getMessage());
            return response;
        }
    }

    /**
     * List all WebAuthn credentials for logged-in user
     */
    public Map<String, Object> listUserCredentials() {
        Map<String, Object> response = new HashMap<>();

        try {
            SessionManager sessionManager = SessionManager.getInstance();
            User currentUser = sessionManager.getCurrentUser();

            if (currentUser == null) {
                response.put("error", "User not logged in");
                return response;
            }

            List<Map<String, Object>> credentials = new ArrayList<>();
            List<WebAuthnCredential> userCreds = findCredentialsForUser(currentUser.getIdUser());

            for (WebAuthnCredential cred : userCreds) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", cred.getIdWebauthn());
                item.put("credentialId", cred.getCredentialId());
                item.put("transports", cred.getTransports());
                item.put("createdAt", cred.getCreatedAt());
                item.put("lastUsedAt", cred.getLastUsedAt());
                credentials.add(item);
            }

            response.put("credentials", credentials);
            return response;

        } catch (Exception e) {
            response.put("error", "Failed to list credentials: " + e.getMessage());
            return response;
        }
    }

    /**
     * Delete a WebAuthn credential
     */
    public Map<String, Object> deleteCredential(Integer credentialId) {
        Map<String, Object> response = new HashMap<>();

        SessionManager sessionManager = SessionManager.getInstance();
        User currentUser = sessionManager.getCurrentUser();

        if (currentUser == null) {
            response.put("error", "User not logged in");
            return response;
        }

        WebAuthnCredential credential = credentialStorage.stream()
            .filter(c -> c.getIdWebauthn().equals(credentialId) && c.getUserId().equals(currentUser.getIdUser()))
            .findFirst()
            .orElse(null);

        if (credential == null) {
            response.put("error", "Credential not found");
            return response;
        }

        credentialStorage.remove(credential);
        response.put("status", "ok");
        response.put("message", "Credential deleted successfully");
        return response;
    }

    // Helper methods

    private String generateChallenge() {
        byte[] challenge = new byte[32];
        new java.security.SecureRandom().nextBytes(challenge);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(challenge);
    }

    private boolean credentialExists(String credentialId) {
        return credentialStorage.stream()
            .anyMatch(c -> c.getCredentialId().equals(credentialId));
    }

    private List<WebAuthnCredential> findCredentialsForUser(Integer userId) {
        List<WebAuthnCredential> result = new ArrayList<>();
        for (WebAuthnCredential cred : credentialStorage) {
            if (cred.getUserId().equals(userId)) {
                result.add(cred);
            }
        }
        return result;
    }

    private WebAuthnCredential findCredentialByCredentialId(String credentialId) {
        return credentialStorage.stream()
            .filter(c -> c.getCredentialId().equals(credentialId))
            .findFirst()
            .orElse(null);
    }
}

