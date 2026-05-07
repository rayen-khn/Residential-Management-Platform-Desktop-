package com.syndicati.services.user.user;

import com.syndicati.models.user.User;
import com.syndicati.models.user.data.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * User business service aligned with the web UserController/UserRepository flow.
 */
public class UserService {

    private final UserRepository userRepository;

    public UserService() {
        this.userRepository = new UserRepository();
    }

    public List<User> listUsers() {
        return userRepository.findAllByCreatedAtDesc();
    }

    public Optional<User> findById(int idUser) {
        return userRepository.findById(idUser);
    }

    public Optional<User> findByEmail(String emailUser) {
        if (emailUser == null || emailUser.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findOneByEmailUser(emailUser.trim());
    }

    public Optional<User> findByRecovery(String recovery) {
        if (recovery == null || recovery.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findOneByEmailOrPhone(recovery.trim());
    }

    public Optional<User> findByAuthCode(String authCode, boolean onlyValid) {
        if (authCode == null || authCode.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findOneByAuthCode(authCode.trim(), onlyValid);
    }

    public List<User> searchByName(String query, int excludeUserId, int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        int safeLimit = Math.max(1, Math.min(limit, 100));
        return userRepository.searchByName(query.trim(), excludeUserId, safeLimit);
    }

    public int createUser(User user) {
        ValidationResult validation = validate(user, true);
        if (!validation.valid) {
            System.out.println("UserService.createUser validation failed: " + validation.message);
            return -1;
        }

        if (userRepository.findOneByEmailUser(user.getEmailUser()).isPresent()) {
            System.out.println("UserService.createUser validation failed: email already exists");
            return -1;
        }

        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        // Store passwords as bcrypt hashes so they match web-side security behavior.
        if (!isBcryptHash(user.getPasswordUser())) {
            user.setPasswordUser(BCrypt.hashpw(user.getPasswordUser(), BCrypt.gensalt(13)));
        }

        return userRepository.create(user);
    }

    public boolean updateUser(User user) {
        ValidationResult validation = validate(user, false);
        if (!validation.valid) {
            System.out.println("UserService.updateUser validation failed: " + validation.message);
            return false;
        }

        Optional<User> existing = userRepository.findById(user.getIdUser());
        if (existing.isEmpty()) {
            System.out.println("UserService.updateUser failed: user not found");
            return false;
        }

        Optional<User> byEmail = userRepository.findOneByEmailUser(user.getEmailUser());
        if (byEmail.isPresent() && !byEmail.get().getIdUser().equals(user.getIdUser())) {
            System.out.println("UserService.updateUser validation failed: email already exists");
            return false;
        }

        user.setUpdatedAt(LocalDateTime.now());
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(existing.get().getCreatedAt());
        }

        return userRepository.update(user);
    }

    public boolean deleteUser(int idUser) {
        if (idUser <= 0) {
            return false;
        }
        return userRepository.deleteById(idUser);
    }

    public boolean updateAuthCode(int idUser, String authCode, LocalDateTime expiresAt) {
        if (idUser <= 0) {
            return false;
        }
        return userRepository.updateAuthCode(idUser, authCode, expiresAt);
    }

    public boolean clearAuthCode(int idUser) {
        if (idUser <= 0) {
            return false;
        }
        return userRepository.clearAuthCode(idUser);
    }

    public boolean updatePasswordAndClearAuth(int idUser, String hashedPassword) {
        if (idUser <= 0 || hashedPassword == null || hashedPassword.isBlank()) {
            return false;
        }
        return userRepository.updatePasswordAndClearAuth(idUser, hashedPassword);
    }

    private ValidationResult validate(User user, boolean isCreate) {
        if (user == null) {
            return ValidationResult.invalid("User payload is null");
        }

        if (!isCreate && (user.getIdUser() == null || user.getIdUser() <= 0)) {
            return ValidationResult.invalid("id_user is required for update");
        }

        if (user.getFirstName() == null || user.getFirstName().isBlank()) {
            return ValidationResult.invalid("first_name is required");
        }

        if (user.getLastName() == null || user.getLastName().isBlank()) {
            return ValidationResult.invalid("last_name is required");
        }

        if (user.getEmailUser() == null || user.getEmailUser().isBlank()) {
            return ValidationResult.invalid("email_user is required");
        }

        if (isCreate && (user.getPasswordUser() == null || user.getPasswordUser().isBlank())) {
            return ValidationResult.invalid("password_user is required for create");
        }

        if (user.getRoleUser() == null || !User.ROLES.contains(user.getRoleUser())) {
            return ValidationResult.invalid("role_user must be one of " + User.ROLES);
        }

        return ValidationResult.valid();
    }

    private boolean isBcryptHash(String value) {
        if (value == null) {
            return false;
        }
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }

    private static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        private static ValidationResult valid() {
            return new ValidationResult(true, "");
        }

        private static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }
}


