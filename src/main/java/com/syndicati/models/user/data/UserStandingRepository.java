package com.syndicati.models.user.data;

import com.syndicati.models.user.UserStanding;
import com.syndicati.services.DatabaseService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * JDBC repository for the native resident standing row.
 */
public class UserStandingRepository {

    private final DatabaseService databaseService;

    public UserStandingRepository() {
        this.databaseService = DatabaseService.getInstance();
    }

    public Optional<UserStanding> findByUserId(int userId) {
        String sql = "SELECT * FROM user_standing WHERE user_id = ? LIMIT 1";

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
            System.out.println("UserStandingRepository.findByUserId error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public boolean save(UserStanding standing) {
        if (standing == null || standing.getUserId() == null || standing.getUserId() <= 0) {
            return false;
        }

        String sql = "INSERT INTO user_standing (user_id, level, points, standing_label, updated_at) VALUES (?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE level = VALUES(level), points = VALUES(points), standing_label = VALUES(standing_label), updated_at = VALUES(updated_at)";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, standing.getUserId());
                ps.setInt(2, Math.max(0, standing.getLevel()));
                ps.setInt(3, Math.max(0, standing.getPoints()));
                ps.setString(4, normalizeLabel(standing.getStandingLabel()));
                ps.setTimestamp(5, toTimestamp(standing.getUpdatedAt() == null ? LocalDateTime.now() : standing.getUpdatedAt()));
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("UserStandingRepository.save error: " + e.getMessage());
        }

        return false;
    }

    public UserStanding createDefault(int userId) {
        UserStanding standing = new UserStanding();
        standing.setUserId(userId);
        standing.setLevel(1);
        standing.setPoints(0);
        standing.setStandingLabel("NORMAL");
        standing.setUpdatedAt(LocalDateTime.now());
        return standing;
    }

    private UserStanding mapRow(ResultSet rs) throws SQLException {
        UserStanding standing = new UserStanding();
        standing.setUserId(rs.getInt("user_id"));
        standing.setLevel(rs.getInt("level"));
        standing.setPoints(rs.getInt("points"));
        standing.setStandingLabel(normalizeLabel(rs.getString("standing_label")));
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        standing.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        return standing;
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private String normalizeLabel(String label) {
        if (label == null || label.isBlank()) {
            return "NORMAL";
        }
        String upper = label.trim().toUpperCase();
        return switch (upper) {
            case "WARNED", "SUSPENDED", "BANNED" -> upper;
            default -> "NORMAL";
        };
    }
}

