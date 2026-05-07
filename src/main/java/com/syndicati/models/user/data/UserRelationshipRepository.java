package com.syndicati.models.user.data;

import com.syndicati.models.user.UserRelationship;
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
 * JDBC repository for the native resident relationship graph.
 */
public class UserRelationshipRepository {

    private final DatabaseService databaseService;

    public UserRelationshipRepository() {
        this.databaseService = DatabaseService.getInstance();
    }

    public Optional<UserRelationship> findById(int id) {
        String sql = "SELECT * FROM user_relationship WHERE id = ? LIMIT 1";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return Optional.empty();
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("UserRelationshipRepository.findById error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<UserRelationship> findRelationship(int userIdOne, int userIdTwo) {
        String sql = "SELECT * FROM user_relationship WHERE (user_first_id = ? AND user_second_id = ?) OR (user_first_id = ? AND user_second_id = ?) LIMIT 1";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return Optional.empty();
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userIdOne);
                ps.setInt(2, userIdTwo);
                ps.setInt(3, userIdTwo);
                ps.setInt(4, userIdOne);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("UserRelationshipRepository.findRelationship error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public List<UserRelationship> findFriendRelationships(int userId, Integer limit) {
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM user_relationship WHERE status = 'FRIENDS' AND (user_first_id = ? OR user_second_id = ?) ORDER BY updated_at DESC"
        );
        if (limit != null && limit > 0) {
            sql.append(" LIMIT ?");
        }

        List<UserRelationship> relationships = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return relationships;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                ps.setInt(1, userId);
                ps.setInt(2, userId);
                if (limit != null && limit > 0) {
                    ps.setInt(3, limit);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        relationships.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("UserRelationshipRepository.findFriendRelationships error: " + e.getMessage());
        }

        return relationships;
    }

    public List<UserRelationship> findPendingRelationshipsFor(int userId) {
        String sql = "SELECT * FROM user_relationship WHERE (user_second_id = ? AND status = 'PENDING_FIRST_SECOND') OR (user_first_id = ? AND status = 'PENDING_SECOND_FIRST') ORDER BY created_at DESC";
        List<UserRelationship> relationships = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return relationships;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        relationships.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("UserRelationshipRepository.findPendingRelationshipsFor error: " + e.getMessage());
        }

        return relationships;
    }

    public int countFriends(int userId) {
        String sql = "SELECT COUNT(*) AS total FROM user_relationship WHERE status = 'FRIENDS' AND (user_first_id = ? OR user_second_id = ?)";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return 0;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("total");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("UserRelationshipRepository.countFriends error: " + e.getMessage());
        }

        return 0;
    }

    public int countPendingRequests(int userId) {
        String sql = "SELECT COUNT(*) AS total FROM user_relationship WHERE (user_second_id = ? AND status = 'PENDING_FIRST_SECOND') OR (user_first_id = ? AND status = 'PENDING_SECOND_FIRST')";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return 0;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("total");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("UserRelationshipRepository.countPendingRequests error: " + e.getMessage());
        }

        return 0;
    }

    public int save(UserRelationship relationship) {
        if (relationship == null || relationship.getUserFirstId() == null || relationship.getUserSecondId() == null) {
            return -1;
        }

        String sql = relationship.getId() == null
            ? "INSERT INTO user_relationship (user_first_id, user_second_id, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?)"
            : "UPDATE user_relationship SET user_first_id = ?, user_second_id = ?, status = ?, updated_at = ? WHERE id = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return -1;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                if (relationship.getId() == null) {
                    ps.setInt(1, relationship.getUserFirstId());
                    ps.setInt(2, relationship.getUserSecondId());
                    ps.setString(3, normalizeStatus(relationship.getStatus()));
                    ps.setTimestamp(4, toTimestamp(relationship.getCreatedAt() == null ? LocalDateTime.now() : relationship.getCreatedAt()));
                    ps.setTimestamp(5, toTimestamp(relationship.getUpdatedAt() == null ? LocalDateTime.now() : relationship.getUpdatedAt()));
                } else {
                    ps.setInt(1, relationship.getUserFirstId());
                    ps.setInt(2, relationship.getUserSecondId());
                    ps.setString(3, normalizeStatus(relationship.getStatus()));
                    ps.setTimestamp(4, toTimestamp(relationship.getUpdatedAt() == null ? LocalDateTime.now() : relationship.getUpdatedAt()));
                    ps.setInt(5, relationship.getId());
                }

                int affected = ps.executeUpdate();
                if (affected == 0) {
                    return -1;
                }

                if (relationship.getId() == null) {
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            return keys.getInt(1);
                        }
                    }
                }

                return relationship.getId() == null ? 1 : relationship.getId();
            }
        } catch (SQLException e) {
            System.out.println("UserRelationshipRepository.save error: " + e.getMessage());
        }

        return -1;
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM user_relationship WHERE id = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("UserRelationshipRepository.deleteById error: " + e.getMessage());
        }

        return false;
    }

    private UserRelationship mapRow(ResultSet rs) throws SQLException {
        UserRelationship relationship = new UserRelationship();
        relationship.setId(rs.getInt("id"));
        relationship.setUserFirstId(rs.getInt("user_first_id"));
        relationship.setUserSecondId(rs.getInt("user_second_id"));
        relationship.setStatus(normalizeStatus(rs.getString("status")));

        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        relationship.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        relationship.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
        return relationship;
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "PENDING_FIRST_SECOND";
        }

        String upper = status.trim().toUpperCase();
        return switch (upper) {
            case "PENDING_SECOND_FIRST", "FRIENDS", "BLOCKED_FIRST_SECOND", "BLOCKED_SECOND_FIRST" -> upper;
            default -> "PENDING_FIRST_SECOND";
        };
    }
}

