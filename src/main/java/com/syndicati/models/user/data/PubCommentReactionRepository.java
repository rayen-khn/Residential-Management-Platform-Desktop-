package com.syndicati.models.repositories;

import com.syndicati.models.entities.PubCommentReaction;
import com.syndicati.services.DatabaseService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PubCommentReactionRepository {
    private final DatabaseService databaseService;

    public PubCommentReactionRepository() {
        this.databaseService = DatabaseService.getInstance();
    }

    public int create(PubCommentReaction reaction) {
        String sql = "INSERT INTO reaction (user_id, publication_id, commentaire_id, kind, emoji, report_reason, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) return -1;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, reaction.getUserId());
                if (reaction.getPublicationId() != null) ps.setInt(2, reaction.getPublicationId()); else ps.setNull(2, Types.INTEGER);
                if (reaction.getCommentaireId() != null) ps.setInt(3, reaction.getCommentaireId()); else ps.setNull(3, Types.INTEGER);
                ps.setString(4, reaction.getKind());
                ps.setString(5, reaction.getEmoji());
                ps.setString(6, reaction.getReportReason());
                ps.setTimestamp(7, Timestamp.valueOf(reaction.getCreatedAt()));
                ps.setTimestamp(8, Timestamp.valueOf(reaction.getUpdatedAt()));

                int result = ps.executeUpdate();
                if (result > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating reaction: " + e.getMessage());
        }
        return -1;
    }

    public boolean delete(int userId, Integer pubId, Integer commId, String kind) {
        StringBuilder sql = new StringBuilder("DELETE FROM reaction WHERE user_id = ? AND kind = ?");
        if (pubId != null) sql.append(" AND publication_id = ?"); else sql.append(" AND publication_id IS NULL");
        if (commId != null) sql.append(" AND commentaire_id = ?"); else sql.append(" AND commentaire_id IS NULL");

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                ps.setInt(1, userId);
                ps.setString(2, kind);
                int idx = 3;
                if (pubId != null) ps.setInt(idx++, pubId);
                if (commId != null) ps.setInt(idx++, commId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting reaction: " + e.getMessage());
        }
        return false;
    }

    public Optional<PubCommentReaction> findOne(int userId, Integer pubId, Integer commId, String kind) {
        StringBuilder sql = new StringBuilder("SELECT * FROM reaction WHERE user_id = ? AND kind = ?");
        if (pubId != null) sql.append(" AND publication_id = ?"); else sql.append(" AND publication_id IS NULL");
        if (commId != null) sql.append(" AND commentaire_id = ?"); else sql.append(" AND commentaire_id IS NULL");

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) return Optional.empty();
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                ps.setInt(1, userId);
                ps.setString(2, kind);
                int idx = 3;
                if (pubId != null) ps.setInt(idx++, pubId);
                if (commId != null) ps.setInt(idx++, commId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding reaction: " + e.getMessage());
        }
        return Optional.empty();
    }

    public int countByTarget(Integer pubId, Integer commId, String kind) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM reaction WHERE kind = ?");
        if (pubId != null) sql.append(" AND publication_id = ?"); else sql.append(" AND publication_id IS NULL");
        if (commId != null) sql.append(" AND commentaire_id = ?"); else sql.append(" AND commentaire_id IS NULL");

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) return 0;
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                ps.setString(1, kind);
                int idx = 2;
                if (pubId != null) ps.setInt(idx++, pubId);
                if (commId != null) ps.setInt(idx++, commId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error counting reactions: " + e.getMessage());
        }
        return 0;
    }

    private PubCommentReaction mapRow(ResultSet rs) throws SQLException {
        PubCommentReaction r = new PubCommentReaction();
        r.setIdReaction(rs.getInt("id_reaction"));
        r.setUserId(rs.getInt("user_id"));
        int pId = rs.getInt("publication_id");
        r.setPublicationId(rs.wasNull() ? null : pId);
        int cId = rs.getInt("commentaire_id");
        r.setCommentaireId(rs.wasNull() ? null : cId);
        r.setKind(rs.getString("kind"));
        r.setEmoji(rs.getString("emoji"));
        r.setReportReason(rs.getString("report_reason"));
        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        r.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return r;
    }
}
