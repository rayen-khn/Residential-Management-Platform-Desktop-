package com.syndicati.models.user.data;

import com.syndicati.models.user.User;
import com.syndicati.services.DatabaseService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for user CRUD using web-aligned table/column names.
 */
public class UserRepository {

    private final DatabaseService databaseService;

    public UserRepository() {
        this.databaseService = DatabaseService.getInstance();
    }

    public List<User> findAllByCreatedAtDesc() {
        return findAllByCreatedAtDescWithLimit(100);
    }

    public List<User> findAllByCreatedAtDescWithLimit(int limit) {
        String sql = "SELECT id_user, first_name, last_name, email_user, password_user, role_user, is_verified, is_disabled, disabled_at, disabled_reason, authCode, authCode_expires_at, two_factor_enabled, totp_secret, google_id, phone, created_at, updated_at FROM user ORDER BY created_at DESC LIMIT ?";
        List<User> users = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return users;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, Math.max(1, limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            users.add(mapRow(rs));
                        } catch (SQLException rowError) {
                            int rowId = 0;
                            try {
                                rowId = rs.getInt("id_user");
                            } catch (SQLException ignored) {
                                // Best-effort logging only.
                            }
                            System.out.println("UserRepository.findAllByCreatedAtDesc skipped row id_user=" + rowId + " due to mapping error: " + rowError.getMessage());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("UserRepository.findAllByCreatedAtDesc error: " + e.getMessage());
        }

        return users;
    }

    public Optional<User> findById(int idUser) {
        String sql = "SELECT id_user, first_name, last_name, email_user, password_user, role_user, is_verified, is_disabled, disabled_at, disabled_reason, authCode, authCode_expires_at, two_factor_enabled, totp_secret, google_id, phone, created_at, updated_at FROM user WHERE id_user = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return Optional.empty();
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idUser);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("UserRepository.findById error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<User> findOneByEmailUser(String emailUser) {
        String sql = "SELECT * FROM user WHERE LOWER(email_user) = LOWER(?) LIMIT 1";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return Optional.empty();
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, emailUser);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("UserRepository.findOneByEmailUser error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<User> findOneByEmailOrPhone(String recovery) {
        String sql = "SELECT * FROM user WHERE LOWER(email_user) = LOWER(?) OR phone = ? LIMIT 1";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return Optional.empty();
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, recovery);
                ps.setString(2, recovery);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("UserRepository.findOneByEmailOrPhone error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<User> findOneByAuthCode(String authCode, boolean onlyValid) {
        String sql = onlyValid
            ? "SELECT * FROM user WHERE authCode = ? AND authCode_expires_at > NOW() LIMIT 1"
            : "SELECT * FROM user WHERE authCode = ? LIMIT 1";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return Optional.empty();
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, authCode);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("UserRepository.findOneByAuthCode error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public List<User> searchByName(String query, int excludeUserId, int limit) {
        String sql = "SELECT * FROM user WHERE id_user != ? AND (LOWER(first_name) LIKE LOWER(?) OR LOWER(last_name) LIKE LOWER(?)) LIMIT ?";
        List<User> users = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return users;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                String like = "%" + query + "%";
                ps.setInt(1, excludeUserId);
                ps.setString(2, like);
                ps.setString(3, like);
                ps.setInt(4, limit);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        users.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("UserRepository.searchByName error: " + e.getMessage());
        }

        return users;
    }

    public int create(User user) {
        String sql = "INSERT INTO user (first_name, last_name, email_user, password_user, role_user, is_verified, is_disabled, disabled_at, disabled_reason, authCode, authCode_expires_at, two_factor_enabled, totp_secret, google_id, phone, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return -1;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                applyPersistParams(ps, user, false);
                int affected = ps.executeUpdate();
                if (affected == 0) {
                    return -1;
                }

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("UserRepository.create error: " + e.getMessage());
        }

        return -1;
    }

    public boolean update(User user) {
        String sql = "UPDATE user SET first_name = ?, last_name = ?, email_user = ?, password_user = ?, role_user = ?, is_verified = ?, is_disabled = ?, disabled_at = ?, disabled_reason = ?, authCode = ?, authCode_expires_at = ?, two_factor_enabled = ?, totp_secret = ?, google_id = ?, phone = ?, updated_at = ? WHERE id_user = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                applyPersistParams(ps, user, true);
                ps.setInt(17, user.getIdUser());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("UserRepository.update error: " + e.getMessage());
        }

        return false;
    }

    public boolean deleteById(int idUser) {
        String sql = "DELETE FROM user WHERE id_user = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idUser);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("UserRepository.deleteById error: " + e.getMessage());
        }

        return false;
    }

    public boolean updateAuthCode(int idUser, String authCode, LocalDateTime expiresAt) {
        String sql = "UPDATE user SET authCode = ?, authCode_expires_at = ?, updated_at = ? WHERE id_user = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, authCode);
                ps.setTimestamp(2, toTimestamp(expiresAt));
                ps.setTimestamp(3, toTimestamp(LocalDateTime.now()));
                ps.setInt(4, idUser);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("UserRepository.updateAuthCode error: " + e.getMessage());
        }

        return false;
    }

    public boolean clearAuthCode(int idUser) {
        return updateAuthCode(idUser, null, null);
    }

    public boolean updatePasswordAndClearAuth(int idUser, String hashedPassword) {
        String sql = "UPDATE user SET password_user = ?, authCode = NULL, authCode_expires_at = NULL, updated_at = ? WHERE id_user = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, hashedPassword);
                ps.setTimestamp(2, toTimestamp(LocalDateTime.now()));
                ps.setInt(3, idUser);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("UserRepository.updatePasswordAndClearAuth error: " + e.getMessage());
        }

        return false;
    }

    private void applyPersistParams(PreparedStatement ps, User user, boolean forUpdate) throws SQLException {
        ps.setString(1, user.getFirstName());
        ps.setString(2, user.getLastName());
        ps.setString(3, user.getEmailUser());
        ps.setString(4, user.getPasswordUser());
        ps.setString(5, user.getRoleUser());
        ps.setBoolean(6, user.isVerified());
        ps.setBoolean(7, user.isDisabled());
        ps.setTimestamp(8, toTimestamp(user.getDisabledAt()));
        ps.setString(9, user.getDisabledReason());
        ps.setString(10, user.getAuthCode());
        ps.setTimestamp(11, toTimestamp(user.getAuthCodeExpiresAt()));
        ps.setBoolean(12, user.isTwoFactorEnabled());
        ps.setString(13, user.getTotpSecret());
        ps.setString(14, user.getGoogleId());
        ps.setString(15, user.getPhone());

        if (forUpdate) {
            ps.setTimestamp(16, toTimestamp(user.getUpdatedAt()));
        } else {
            ps.setTimestamp(16, toTimestamp(user.getCreatedAt()));
            ps.setTimestamp(17, toTimestamp(user.getUpdatedAt()));
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setIdUser(rs.getInt("id_user"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmailUser(rs.getString("email_user"));
        user.setPasswordUser(rs.getString("password_user"));
        user.setRoleUser(rs.getString("role_user"));
        user.setVerified(rs.getBoolean("is_verified"));
        user.setDisabled(rs.getBoolean("is_disabled"));
        user.setDisabledAt(readLocalDateTime(rs, "disabled_at"));
        user.setDisabledReason(rs.getString("disabled_reason"));
        user.setAuthCode(rs.getString("authCode"));
        user.setAuthCodeExpiresAt(readLocalDateTime(rs, "authCode_expires_at"));
        user.setTwoFactorEnabled(rs.getBoolean("two_factor_enabled"));
        user.setTotpSecret(rs.getString("totp_secret"));
        user.setGoogleId(rs.getString("google_id"));
        user.setPhone(rs.getString("phone"));
        user.setCreatedAt(readLocalDateTime(rs, "created_at"));
        user.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return user;
    }

    private LocalDateTime readLocalDateTime(ResultSet rs, String column) throws SQLException {
        try {
            return toLocalDateTime(rs.getTimestamp(column));
        } catch (SQLException ex) {
            // Handles invalid MySQL zero-datetime values by treating them as null.
            String msg = ex.getMessage();
            if (msg != null && msg.toLowerCase().contains("zero date")) {
                return null;
            }
            throw ex;
        }
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}


