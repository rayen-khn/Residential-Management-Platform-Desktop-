package com.syndicati.models.forum.data;

import com.syndicati.models.forum.Commentaire;
import com.syndicati.models.forum.Publication;
import com.syndicati.models.user.User;
import com.syndicati.models.user.data.UserRepository;
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
 * JDBC repository for forum comments.
 */
public class CommentaireRepository {

    private final DatabaseService databaseService;
    private final UserRepository userRepository;
    private final PublicationRepository publicationRepository;

    public CommentaireRepository() {
        this.databaseService = DatabaseService.getInstance();
        this.userRepository = new UserRepository();
        this.publicationRepository = new PublicationRepository();
    }

    public List<Commentaire> findAll() {
        String sql = "SELECT * FROM commentaire ORDER BY created_at ASC";
        List<Commentaire> commentaires = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return commentaires;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    commentaires.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("CommentaireRepository.findAll error: " + e.getMessage());
        }

        return commentaires;
    }

    public Optional<Commentaire> findById(Integer id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }

        String sql = "SELECT * FROM commentaire WHERE id_commentaire = ?";

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
            System.out.println("CommentaireRepository.findById error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public List<Commentaire> findByPublicationId(Integer publicationId) {
        if (publicationId == null || publicationId <= 0) {
            return new ArrayList<>();
        }

        String sql = "SELECT * FROM commentaire WHERE id_pub = ? ORDER BY created_at ASC";
        List<Commentaire> commentaires = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return commentaires;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, publicationId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        commentaires.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("CommentaireRepository.findByPublicationId error: " + e.getMessage());
        }

        return commentaires;
    }

    public List<Commentaire> findByUserId(Integer userId) {
        if (userId == null || userId <= 0) {
            return new ArrayList<>();
        }

        String sql = "SELECT * FROM commentaire WHERE id_user = ? ORDER BY created_at DESC";
        List<Commentaire> commentaires = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return commentaires;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        commentaires.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("CommentaireRepository.findByUserId error: " + e.getMessage());
        }

        return commentaires;
    }

    public Integer create(Commentaire commentaire) {
        if (commentaire == null || commentaire.getPublication() == null || commentaire.getUser() == null) {
            return -1;
        }

        String sql = "INSERT INTO commentaire (description_commentaire, image_commentaire, created_at, updated_at, visibility, id_pub, id_user) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return -1;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime createdAt = commentaire.getCreatedAt() != null ? commentaire.getCreatedAt() : now;
                ps.setString(1, commentaire.getDescriptionCommentaire());
                ps.setString(2, commentaire.getImageCommentaire());
                ps.setTimestamp(3, Timestamp.valueOf(createdAt));
                ps.setTimestamp(4, Timestamp.valueOf(now));
                ps.setBoolean(5, commentaire.isVisibility());
                ps.setInt(6, commentaire.getPublication().getIdPublication());
                ps.setInt(7, commentaire.getUser().getIdUser());

                ps.executeUpdate();

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        commentaire.setIdCommentaire(id);
                        commentaire.setCreatedAt(createdAt);
                        commentaire.setUpdatedAt(now);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("CommentaireRepository.create error: " + e.getMessage());
        }

        return -1;
    }

    public boolean update(Commentaire commentaire) {
        if (commentaire == null || commentaire.getIdCommentaire() == null || commentaire.getIdCommentaire() <= 0) {
            return false;
        }

        String sql = "UPDATE commentaire SET description_commentaire = ?, image_commentaire = ?, created_at = ?, updated_at = ?, visibility = ? WHERE id_commentaire = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                LocalDateTime createdAt = commentaire.getCreatedAt() != null ? commentaire.getCreatedAt() : LocalDateTime.now();
                ps.setString(1, commentaire.getDescriptionCommentaire());
                ps.setString(2, commentaire.getImageCommentaire());
                ps.setTimestamp(3, Timestamp.valueOf(createdAt));
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                ps.setBoolean(5, commentaire.isVisibility());
                ps.setInt(6, commentaire.getIdCommentaire());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("CommentaireRepository.update error: " + e.getMessage());
        }

        return false;
    }

    public boolean delete(Integer id) {
        if (id == null || id <= 0) {
            return false;
        }

        String sql = "DELETE FROM commentaire WHERE id_commentaire = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("CommentaireRepository.delete error: " + e.getMessage());
        }

        return false;
    }

    private Commentaire mapRow(ResultSet rs) throws SQLException {
        Commentaire commentaire = new Commentaire();
        commentaire.setIdCommentaire(rs.getInt("id_commentaire"));
        commentaire.setDescriptionCommentaire(rs.getString("description_commentaire"));
        commentaire.setImageCommentaire(rs.getString("image_commentaire"));
        commentaire.setVisibility(rs.getBoolean("visibility"));

        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            commentaire.setCreatedAt(createdTs.toLocalDateTime());
        }

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        if (updatedTs != null) {
            commentaire.setUpdatedAt(updatedTs.toLocalDateTime());
        }

        int publicationId = rs.getInt("id_pub");
        if (!rs.wasNull()) {
            Optional<Publication> publication = publicationRepository.findById(publicationId);
            publication.ifPresent(commentaire::setPublication);
        }

        int userId = rs.getInt("id_user");
        if (!rs.wasNull()) {
            Optional<User> user = userRepository.findById(userId);
            user.ifPresent(commentaire::setUser);
        }

        return commentaire;
    }
}