package com.syndicati.services.user.auth;

import com.syndicati.models.user.User;
import com.syndicati.services.user.user.UserService;
import com.syndicati.services.mail.AsyncMailerService;
import com.syndicati.services.mail.MailerService;
import com.syndicati.services.mail.SyndicatiEmailComposer;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

/**
 * Forgot-password auth code flow modeled after the web implementation.
 * Uses async email delivery to prevent UI blocking and improve user experience.
 */
public class AuthCodeService {

    private final UserService userService;
    private final MailerService mailerService;
    private final AsyncMailerService asyncMailerService;
    private final Random random;

    public AuthCodeService() {
        this.userService = new UserService();
        this.mailerService = new MailerService();
        this.asyncMailerService = AsyncMailerService.getInstance();
        this.random = new Random();
    }

    public AuthFlowResult requestReset(String recovery) {
        if (recovery == null || recovery.isBlank()) {
            return AuthFlowResult.failure("Please enter your email address.");
        }

        Optional<User> userOpt = userService.findByRecovery(recovery.trim());
        if (userOpt.isEmpty()) {
            return AuthFlowResult.success(null, "If this email is registered, you will receive a 2FA code shortly.");
        }

        User user = userOpt.get();
        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        boolean updated = userService.updateAuthCode(user.getIdUser(), code, expiresAt);
        if (!updated) {
            return AuthFlowResult.failure("Failed to create security code. Please try again.");
        }

        // Email sending is now asynchronous and won't block the user.
        // It will retry automatically if it fails.
        sendOtpEmail(user, code);
        return AuthFlowResult.success(user, "A 6-digit verification code has been sent to your email.");
    }

    public AuthFlowResult requestLoginCode(String email) {
        if (email == null || email.isBlank()) {
            return AuthFlowResult.failure("Please enter your email address.");
        }

        Optional<User> userOpt = userService.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            return AuthFlowResult.failure("No account found for this email.");
        }

        User user = userOpt.get();
        if (user.isDisabled()) {
            return AuthFlowResult.failure("This account is disabled.");
        }

        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        boolean updated = userService.updateAuthCode(user.getIdUser(), code, expiresAt);
        if (!updated) {
            return AuthFlowResult.failure("Failed to create login code. Please try again.");
        }

        // Email sending is now asynchronous and won't block the user.
        // It will retry automatically if it fails.
        sendLoginOtpEmail(user, code);
        return AuthFlowResult.success(user, "A 6-digit login code has been sent to your email.");
    }

    public AuthFlowResult verifyLoginCode(String email, String code) {
        if (email == null || email.isBlank() || code == null || code.isBlank()) {
            return AuthFlowResult.failure("Email and code are required.");
        }

        Optional<User> userOpt = userService.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            return AuthFlowResult.failure("No account found for this email.");
        }

        User user = userOpt.get();
        if (!isValidCode(user, code.trim())) {
            return AuthFlowResult.failure("Invalid or expired code.");
        }

        boolean cleared = userService.clearAuthCode(user.getIdUser());
        if (!cleared) {
            return AuthFlowResult.failure("Failed to finalize OTP login.");
        }

        return AuthFlowResult.success(user, "OTP verified. Login successful.");
    }

    public AuthFlowResult verifyReset(String recovery, String code) {
        if (recovery == null || recovery.isBlank() || code == null || code.isBlank()) {
            return AuthFlowResult.failure("Email and code are required.");
        }

        Optional<User> userOpt = userService.findByRecovery(recovery.trim());
        if (userOpt.isEmpty()) {
            return AuthFlowResult.failure("Invalid session.");
        }

        User user = userOpt.get();
        if (!isValidCode(user, code.trim())) {
            return AuthFlowResult.failure("Invalid or expired code.");
        }

        String newPassword = generateRandomPassword(12);
        String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt(13));
        boolean updated = userService.updatePasswordAndClearAuth(user.getIdUser(), hashed);
        if (!updated) {
            return AuthFlowResult.failure("An error occurred while resetting your password.");
        }

        // Email sending is now asynchronous and won't block the user.
        // It will retry automatically if it fails.
        sendPasswordResetEmail(user, newPassword);
        return AuthFlowResult.success(user, "Success! Your password has been reset. Please check your email for your new credentials.");
    }

    private boolean isValidCode(User user, String code) {
        if (user.getAuthCode() == null || user.getAuthCodeExpiresAt() == null) {
            return false;
        }
        if (!code.equals(user.getAuthCode())) {
            return false;
        }
        return user.getAuthCodeExpiresAt().isAfter(LocalDateTime.now());
    }

    private String generateCode() {
        int value = 100000 + random.nextInt(900000);
        return String.valueOf(value);
    }

    private String generateRandomPassword(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = random.nextInt(chars.length());
            sb.append(chars.charAt(idx));
        }
        return sb.toString();
    }

    private void sendOtpEmail(User user, String code) {
        String html = SyndicatiEmailComposer.twoFactorCode(user.getFirstName(), code);
        String email = user.getEmailUser();

        // Send asynchronously to prevent UI blocking. Email will retry automatically if it fails.
        asyncMailerService.sendHtmlAsync(email, "Your Two-Factor Authentication Code", html,
            result -> {
                if (!result.isSuccess()) {
                    System.err.println("[AuthCode] Failed to send 2FA email to " + email + ": " + result.getError());
                    // Note: We still return success to the user because the email is queued for retry.
                    // The user will eventually receive the OTP email after retry_logic kicks in.
                }
            }
        );
    }

    private void sendLoginOtpEmail(User user, String code) {
        String html = SyndicatiEmailComposer.twoFactorCode(user.getFirstName(), code);
        String email = user.getEmailUser();

        // Send asynchronously to prevent UI blocking. Email will retry automatically if it fails.
        asyncMailerService.sendHtmlAsync(email, "Your Login Verification Code", html,
            result -> {
                if (!result.isSuccess()) {
                    System.err.println("[AuthCode] Failed to send login OTP email to " + email + ": " + result.getError());
                    // Note: We still return success to the user because the email is queued for retry.
                    // The user will eventually receive the OTP email after retry_logic kicks in.
                }
            }
        );
    }

    private void sendPasswordResetEmail(User user, String newPassword) {
        String html = SyndicatiEmailComposer.passwordReset(user.getFirstName(), newPassword);
        String email = user.getEmailUser();

        // Send asynchronously to prevent UI blocking. Email will retry automatically if it fails.
        asyncMailerService.sendHtmlAsync(email, "Horizon Protocol: Your New Credentials", html,
            result -> {
                if (!result.isSuccess()) {
                    System.err.println("[AuthCode] Failed to send password reset email to " + email + ": " + result.getError());
                    // Note: We still return success to the user because the email is queued for retry.
                    // The user will eventually receive the credentials email after retry_logic kicks in.
                }
            }
        );
    }

    public static class AuthFlowResult {
        private final boolean success;
        private final User user;
        private final String message;

        private AuthFlowResult(boolean success, User user, String message) {
            this.success = success;
            this.user = user;
            this.message = message;
        }

        public static AuthFlowResult success(User user, String message) {
            return new AuthFlowResult(true, user, message);
        }

        public static AuthFlowResult failure(String message) {
            return new AuthFlowResult(false, null, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public User getUser() {
            return user;
        }

        public String getMessage() {
            return message;
        }
    }
}


