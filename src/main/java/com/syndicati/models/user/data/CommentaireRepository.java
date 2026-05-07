package com.syndicati.models.repositories;

import com.syndicati.models.entities.Commentaire;
import com.syndicati.services.DatabaseService;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC repository for comments.
 */
public class CommentaireRepository {

    private final DatabaseService databaseService;

    public CommentaireRepository() {
        this.databaseService = DatabaseService.getInstance();
    }

    public List<Commentaire> findByPublicationId(int pubId) {
        String sql = "SELECT c.*, " +
                     "u.first_name AS author_fname, " +
                     "u.last_name AS author_lname, " +
                     "pr.avatar AS author_av " +
                     "FROM commentaire c " +
                     "LEFT JOIN user u ON c.id_user = u.id_user " +
                     "LEFT JOIN profile pr ON u.id_user = pr.user_id " +
                     "WHERE c.id_pub = ? " +
                     "ORDER BY c.created_at ASC";
        
        List<Commentaire> comments = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) return comments;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, pubId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        comments.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("CommentaireRepository.findByPublicationId error: " + e.getMessage());
        }

        return comments;
    }

    public Commentaire findById(int id) {
        String sql = "SELECT c.*, " +
                     "u.first_name AS author_fname, " +
                     "u.last_name AS author_lname, " +
                     "pr.avatar AS author_av " +
                     "FROM commentaire c " +
                     "LEFT JOIN user u ON c.id_user = u.id_user " +
                     "LEFT JOIN profile pr ON u.id_user = pr.user_id " +
                     "WHERE c.id_commentaire = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) return null;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("CommentaireRepository.findById error: " + e.getMessage());
        }

        return null;
    }

    public int create(Commentaire comment) {
        String sql = "INSERT INTO `commentaire` (`description_commentaire`, `image_commentaire`, `created_at`, `updated_at`, `visibility`, `id_pub`, `id_user`) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = databaseService.getConnection();
            if (conn == null) {
                System.err.println("DB_DIAG: Connection failed");
                return -1;
            }
            
            // Force immediate persistence
            conn.setAutoCommit(true);
            System.out.println("DB_DIAG: Attempting insert into `commentaire` for pub " + comment.getIdPub() + " user " + comment.getIdUser());

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, comment.getDescriptionCommentaire());
                ps.setString(2, comment.getImageCommentaire());
                
                LocalDateTime now = LocalDateTime.now();
                ps.setTimestamp(3, Timestamp.valueOf(comment.getCreatedAt() != null ? comment.getCreatedAt() : now));
                ps.setTimestamp(4, Timestamp.valueOf(comment.getUpdatedAt() != null ? comment.getUpdatedAt() : now));
                ps.setInt(5, comment.getVisibility() != null ? comment.getVisibility() : 1);
                ps.setInt(6, comment.getIdPub());
                ps.setInt(7, comment.getIdUser());

                int affected = ps.executeUpdate();
                System.out.println("DB_DIAG: Affected rows: " + affected);
                
                if (affected > 0) {
                    int generatedId = -1;
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            generatedId = keys.getInt(1);
                        }
                    }
                    
                    // VERIFICATION: Can we find it immediately?
                    try (PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM `commentaire` WHERE `description_commentaire` = ? AND `id_pub` = ?")) {
                        check.setString(1, comment.getDescriptionCommentaire());
                        check.setInt(2, comment.getIdPub());
                        try (ResultSet rs = check.executeQuery()) {
                            if (rs.next()) {
                                System.out.println("DB_DIAG: Verification count for this comment: " + rs.getInt(1));
                            }
                        }
                    }
                    
                    return generatedId > 0 ? generatedId : 1;
                }
            }
        } catch (SQLException e) {
            System.err.println("DB_DIAG_ERROR: " + e.getMessage());
            System.err.println("DB_DIAG_SQLSTATE: " + e.getSQLState());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
        return -1;
    }

    public boolean update(Commentaire comment) {
        String sql = "UPDATE `commentaire` SET `description_commentaire` = ?, `image_commentaire` = ?, `visibility` = ?, `updated_at` = ? " +
                     "WHERE `id_commentaire` = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) return false;
            conn.setAutoCommit(true);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, comment.getDescriptionCommentaire());
                ps.setString(2, comment.getImageCommentaire());
                ps.setInt(3, comment.getVisibility());
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(5, comment.getIdCommentaire());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("SQL ERROR in CommentaireRepository.update: " + e.getMessage());
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM `commentaire` WHERE `id_commentaire` = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) return false;
            conn.setAutoCommit(true);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("SQL ERROR in CommentaireRepository.delete: " + e.getMessage());
        }
        return false;
    }

    private Commentaire mapRow(ResultSet rs) throws SQLException {
        Commentaire c = new Commentaire();
        c.setIdCommentaire(rs.getInt("id_commentaire"));
        c.setDescriptionCommentaire(rs.getString("description_commentaire"));
        c.setImageCommentaire(rs.getString("image_commentaire"));
        c.setCreatedAt(readLocalDateTime(rs, "created_at"));
        c.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        c.setVisibility(rs.getInt("visibility"));
        c.setIdPub(rs.getInt("id_pub"));
        c.setIdUser(rs.getInt("id_user"));

        // Author meta
        try {
            c.setAuthorFirstName(rs.getString("author_fname"));
            c.setAuthorLastName(rs.getString("author_lname"));
            c.setAuthorAvatar(rs.getString("author_av"));
        } catch (SQLException ignored) {}

        return c;
    }

    private LocalDateTime readLocalDateTime(ResultSet rs, String col) throws SQLException {
        Timestamp ts = rs.getTimestamp(col);
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
