package com.syndicati.models.user.data;

import com.syndicati.models.user.Profile;
import com.syndicati.services.DatabaseService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC profile repository aligned with web ProfileRepository behavior.
 */
public class ProfileRepository {

    private final DatabaseService databaseService;

    public ProfileRepository() {
        this.databaseService = DatabaseService.getInstance();
    }

    public List<Profile> findAllByIdDesc() {
        return findAllByIdDescWithLimit(50);
    }

    public List<Profile> findAllByIdDescWithLimit(int limit) {
        String sql = "SELECT id_profile, user_id, avatar, theme, locale, timezone, description_profile, settings FROM profile ORDER BY id_profile DESC LIMIT ?";
        List<Profile> profiles = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return profiles;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, Math.max(1, limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        profiles.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ProfileRepository.findAllByIdDesc error: " + e.getMessage());
        }

        return profiles;
    }

    public Optional<Profile> findById(int idProfile) {
        String sql = "SELECT id_profile, user_id, avatar, theme, locale, timezone, description_profile, settings FROM profile WHERE id_profile = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return Optional.empty();
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idProfile);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ProfileRepository.findById error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<Profile> findOneByUserId(int userId) {
        String sql = "SELECT * FROM profile WHERE user_id = ? LIMIT 1";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return Optional.empty();
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ProfileRepository.findOneByUserId error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<Integer> insert(Profile profile) {
        String sql = "INSERT INTO profile (user_id, avatar, theme, locale, timezone, description_profile, settings) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return Optional.empty();
            }

            try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, profile.getUserId());
                ps.setString(2, profile.getAvatar());
                if (profile.getTheme() == null) {
                    ps.setNull(3, java.sql.Types.SMALLINT);
                } else {
                    ps.setInt(3, profile.getTheme());
                }
                ps.setString(4, profile.getLocale());
                if (profile.getTimezone() == null) {
                    ps.setNull(5, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(5, profile.getTimezone());
                }
                ps.setString(6, profile.getDescriptionProfile());
                ps.setString(7, profile.getSettingsJson());
                
                if (ps.executeUpdate() > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            return Optional.of(rs.getInt(1));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ProfileRepository.insert error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public boolean update(Profile profile) {
        String sql = "UPDATE profile SET user_id = ?, avatar = ?, theme = ?, locale = ?, timezone = ?, description_profile = ?, settings = ? WHERE id_profile = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, profile.getUserId());
                ps.setString(2, profile.getAvatar());
                if (profile.getTheme() == null) {
                    ps.setNull(3, java.sql.Types.SMALLINT);
                } else {
                    ps.setInt(3, profile.getTheme());
                }
                ps.setString(4, profile.getLocale());
                if (profile.getTimezone() == null) {
                    ps.setNull(5, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(5, profile.getTimezone());
                }
                ps.setString(6, profile.getDescriptionProfile());
                ps.setString(7, profile.getSettingsJson());
                ps.setInt(8, profile.getIdProfile());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("ProfileRepository.update error: " + e.getMessage());
        }

        return false;
    }

    public boolean delete(int idProfile) {
        String sql = "DELETE FROM profile WHERE id_profile = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idProfile);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("ProfileRepository.delete error: " + e.getMessage());
        }

        return false;
    }

    private Profile mapRow(ResultSet rs) throws SQLException {
        Profile profile = new Profile();
        profile.setIdProfile(rs.getInt("id_profile"));
        profile.setUserId(rs.getInt("user_id"));
        profile.setAvatar(rs.getString("avatar"));
        int theme = rs.getInt("theme");
        profile.setTheme(rs.wasNull() ? null : theme);
        profile.setLocale(rs.getString("locale"));
        int timezone = rs.getInt("timezone");
        profile.setTimezone(rs.wasNull() ? null : timezone);
        profile.setDescriptionProfile(rs.getString("description_profile"));
        profile.setSettingsJson(rs.getString("settings"));
        return profile;
    }
}


