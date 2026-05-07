package com.syndicati.models.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User entity aligned with the web project's user table naming.
 */
public class User {

    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 50;
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PHONE_MIN_LENGTH = 8;
    private static final int PHONE_MAX_LENGTH = 20;

    public static final Set<String> ROLES = new HashSet<>(Arrays.asList(
        "RESIDENT", "SYNDIC", "OWNER", "ADMIN", "SUPERADMIN"
    ));

    private Integer idUser;
    private String firstName;
    private String lastName;
    private String emailUser;
    private String passwordUser;
    private String roleUser = "RESIDENT";
    private boolean isVerified;
    private boolean isDisabled;
    private LocalDateTime disabledAt;
    private String disabledReason;
    private String authCode;
    private LocalDateTime authCodeExpiresAt;
    private boolean twoFactorEnabled;
    private String totpSecret;
    private String googleId;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Integer getIdUser() {
        return idUser;
    }

    public void setIdUser(Integer idUser) {
        this.idUser = idUser;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmailUser() {
        return emailUser;
    }

    public void setEmailUser(String emailUser) {
        this.emailUser = emailUser;
    }

    public String getPasswordUser() {
        return passwordUser;
    }

    public void setPasswordUser(String passwordUser) {
        this.passwordUser = passwordUser;
    }

    public String getRoleUser() {
        return roleUser;
    }

    public void setRoleUser(String roleUser) {
        this.roleUser = roleUser;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    public void setDisabled(boolean disabled) {
        isDisabled = disabled;
    }

    public LocalDateTime getDisabledAt() {
        return disabledAt;
    }

    public void setDisabledAt(LocalDateTime disabledAt) {
        this.disabledAt = disabledAt;
    }

    public String getDisabledReason() {
        return disabledReason;
    }

    public void setDisabledReason(String disabledReason) {
        this.disabledReason = disabledReason;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public LocalDateTime getAuthCodeExpiresAt() {
        return authCodeExpiresAt;
    }

    public void setAuthCodeExpiresAt(LocalDateTime authCodeExpiresAt) {
        this.authCodeExpiresAt = authCodeExpiresAt;
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<String> validateForCreate() {
        List<String> errors = validateCommon();

        if (passwordUser == null || passwordUser.isBlank()) {
            errors.add("Password is required.");
        } else if (passwordUser.length() < PASSWORD_MIN_LENGTH) {
            errors.add("Password must be at least " + PASSWORD_MIN_LENGTH + " characters.");
        } else {
            if (!passwordUser.matches(".*[A-Z].*")) {
                errors.add("Password must contain at least one uppercase letter.");
            }
            if (!passwordUser.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
                errors.add("Password must contain at least one special character.");
            }
        }

        return errors;
    }

    public List<String> validateForUpdate() {
        List<String> errors = validateCommon();
        if (passwordUser != null && !passwordUser.isBlank() && passwordUser.length() < PASSWORD_MIN_LENGTH) {
            errors.add("Password must be at least " + PASSWORD_MIN_LENGTH + " characters.");
        }
        if (passwordUser != null && !passwordUser.isBlank() && !passwordUser.matches(".*[A-Z].*")) {
            errors.add("Password must contain at least one uppercase letter.");
        }
        if (passwordUser != null && !passwordUser.isBlank() && !passwordUser.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            errors.add("Password must contain at least one special character.");
        }
        return errors;
    }

    private List<String> validateCommon() {
        List<String> errors = new ArrayList<>();

        if (!isNameValid(firstName)) {
            errors.add("First name must start with a letter and cannot be only numbers.");
        }
        if (!isNameValid(lastName)) {
            errors.add("Last name must start with a letter and cannot be only numbers.");
        }

        if (emailUser == null || emailUser.isBlank()) {
            errors.add("Email is required.");
        } else if (!emailUser.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            errors.add("Email format is invalid.");
        }

        if (roleUser == null || !ROLES.contains(roleUser)) {
            errors.add("Role is invalid.");
        }

        if (phone != null && !phone.isBlank()) {
            String cleaned = phone.trim();
            if (!cleaned.matches("^\\+?[0-9 ]+$")) {
                errors.add("Phone format is invalid.");
            } else {
                String digits = cleaned.replace(" ", "").replace("+", "");
                if (digits.length() < PHONE_MIN_LENGTH || digits.length() > PHONE_MAX_LENGTH) {
                    errors.add("Phone must be between " + PHONE_MIN_LENGTH + " and " + PHONE_MAX_LENGTH + " digits.");
                }
            }
        }

        return errors;
    }

    private boolean isNameValid(String value) {
        if (value == null) {
            return false;
        }
        String cleaned = value.trim();
        if (cleaned.length() < NAME_MIN_LENGTH || cleaned.length() > NAME_MAX_LENGTH) {
            return false;
        }
        return Character.isLetter(cleaned.charAt(0)) && !cleaned.matches("\\d+");
    }
}


