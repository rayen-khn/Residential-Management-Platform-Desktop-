package com.syndicati.models.forum.data;

import com.syndicati.models.forum.Commentaire;
import com.syndicati.models.forum.Publication;
import com.syndicati.models.forum.Reaction;
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
 * JDBC repository for forum reactions.
 */
public class ReactionRepository {

    private final DatabaseService databaseService;
    private final UserRepository userRepository;
    private final PublicationRepository publicationRepository;
    private final CommentaireRepository commentaireRepository;

    public ReactionRepository() {
        this.databaseService = DatabaseService.getInstance();
        this.userRepository = new UserRepository();
        this.publicationRepository = new PublicationRepository();
        this.commentaireRepository = new CommentaireRepository();
    }

    public Optional<Reaction> findById(Integer id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }

        String sql = "SELECT * FROM reaction WHERE id_reaction = ?";
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
            System.out.println("ReactionRepository.findById error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<Reaction> findOneByUserAndPublicationAndKind(Integer userId, Integer publicationId, String kind) {
        if (!isValidId(userId) || !isValidId(publicationId) || isBlank(kind)) {
            return Optional.empty();
        }

        String sql = "SELECT * FROM reaction WHERE user_id = ? AND publication_id = ? AND kind = ? LIMIT 1";
        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return Optional.empty();
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, publicationId);
                ps.setString(3, kind);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ReactionRepository.findOneByUserAndPublicationAndKind error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<Reaction> findOneByUserAndCommentAndKind(Integer userId, Integer commentId, String kind) {
        if (!isValidId(userId) || !isValidId(commentId) || isBlank(kind)) {
            return Optional.empty();
        }

        String sql = "SELECT * FROM reaction WHERE user_id = ? AND commentaire_id = ? AND kind = ? LIMIT 1";
        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return Optional.empty();
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, commentId);
                ps.setString(3, kind);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ReactionRepository.findOneByUserAndCommentAndKind error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public List<Reaction> findByUserAndPublication(Integer userId, Integer publicationId) {
        if (!isValidId(userId) || !isValidId(publicationId)) {
            return List.of();
        }

        String sql = "SELECT * FROM reaction WHERE user_id = ? AND publication_id = ?";
        return selectMany(sql, ps -> {
            ps.setInt(1, userId);
            ps.setInt(2, publicationId);
        });
    }

    public List<Reaction> findByUserAndComment(Integer userId, Integer commentId) {
        if (!isValidId(userId) || !isValidId(commentId)) {
            return List.of();
        }

        String sql = "SELECT * FROM reaction WHERE user_id = ? AND commentaire_id = ?";
        return selectMany(sql, ps -> {
            ps.setInt(1, userId);
            ps.setInt(2, commentId);
        });
    }

    public List<Reaction> findByComment(Integer commentId) {
        if (!isValidId(commentId)) {
            return List.of();
        }

        String sql = "SELECT * FROM reaction WHERE commentaire_id = ?";
        return selectMany(sql, ps -> {
            ps.setInt(1, commentId);
        });
    }

    public List<Reaction> findByPublication(Integer publicationId) {
        if (!isValidId(publicationId)) {
            return List.of();
        }

        String sql = "SELECT * FROM reaction WHERE publication_id = ?";
        return selectMany(sql, ps -> {
            ps.setInt(1, publicationId);
        });
    }

    public List<Reaction> findByUserAndPublicationAndKinds(Integer userId, Integer publicationId, List<String> kinds) {
        if (!isValidId(userId) || !isValidId(publicationId) || kinds == null || kinds.isEmpty()) {
            return List.of();
        }

        String in = String.join(",", java.util.Collections.nCopies(kinds.size(), "?"));
        String sql = "SELECT * FROM reaction WHERE user_id = ? AND publication_id = ? AND kind IN (" + in + ")";

        return selectMany(sql, ps -> {
            ps.setInt(1, userId);
            ps.setInt(2, publicationId);
            for (int i = 0; i < kinds.size(); i++) {
                ps.setString(i + 3, kinds.get(i));
            }
        });
    }

    public List<Reaction> findByUserAndCommentAndKinds(Integer userId, Integer commentId, List<String> kinds) {
        if (!isValidId(userId) || !isValidId(commentId) || kinds == null || kinds.isEmpty()) {
            return List.of();
        }

        String in = String.join(",", java.util.Collections.nCopies(kinds.size(), "?"));
        String sql = "SELECT * FROM reaction WHERE user_id = ? AND commentaire_id = ? AND kind IN (" + in + ")";

        return selectMany(sql, ps -> {
            ps.setInt(1, userId);
            ps.setInt(2, commentId);
            for (int i = 0; i < kinds.size(); i++) {
                ps.setString(i + 3, kinds.get(i));
            }
        });
    }

    public int countByPublicationAndKind(Integer publicationId, String kind) {
        if (!isValidId(publicationId) || isBlank(kind)) {
            return 0;
        }

        String sql = "SELECT COUNT(id_reaction) FROM reaction WHERE publication_id = ? AND kind = ?";
        return selectCount(sql, ps -> {
            ps.setInt(1, publicationId);
            ps.setString(2, kind);
        });
    }

    public int countByCommentAndKind(Integer commentId, String kind) {
        if (!isValidId(commentId) || isBlank(kind)) {
            return 0;
        }

        String sql = "SELECT COUNT(id_reaction) FROM reaction WHERE commentaire_id = ? AND kind = ?";
        return selectCount(sql, ps -> {
            ps.setInt(1, commentId);
            ps.setString(2, kind);
        });
    }

    public List<Reaction> findAll() {
        String sql = "SELECT * FROM reaction ORDER BY updated_at DESC";
        return selectMany(sql, ps -> {});
    }

    public Integer create(Reaction reaction) {
        if (reaction == null || reaction.getUser() == null || reaction.getUser().getIdUser() == null) {
            return -1;
        }

        String sql = "INSERT INTO reaction (user_id, publication_id, commentaire_id, kind, emoji, report_reason, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return -1;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, reaction.getUser().getIdUser());
                if (reaction.getPublication() != null && reaction.getPublication().getIdPublication() != null) {
                    ps.setInt(2, reaction.getPublication().getIdPublication());
                } else {
                    ps.setNull(2, java.sql.Types.INTEGER);
                }
                if (reaction.getCommentaire() != null && reaction.getCommentaire().getIdCommentaire() != null) {
                    ps.setInt(3, reaction.getCommentaire().getIdCommentaire());
                } else {
                    ps.setNull(3, java.sql.Types.INTEGER);
                }
                ps.setString(4, reaction.getKind());
                ps.setString(5, reaction.getEmoji());
                ps.setString(6, reaction.getReportReason());
                ps.setTimestamp(7, Timestamp.valueOf(reaction.getCreatedAt() != null ? reaction.getCreatedAt() : now));
                ps.setTimestamp(8, Timestamp.valueOf(reaction.getUpdatedAt() != null ? reaction.getUpdatedAt() : now));

                ps.executeUpdate();
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        reaction.setIdReaction(id);
                        if (reaction.getCreatedAt() == null) {
                            reaction.setCreatedAt(now);
                        }
                        if (reaction.getUpdatedAt() == null) {
                            reaction.setUpdatedAt(now);
                        }
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ReactionRepository.create error: " + e.getMessage());
        }

        return -1;
    }

    public boolean update(Reaction reaction) {
        if (reaction == null || !isValidId(reaction.getIdReaction())) {
            return false;
        }

        String sql = "UPDATE reaction SET kind = ?, emoji = ?, report_reason = ?, updated_at = ? WHERE id_reaction = ?";
        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, reaction.getKind());
                ps.setString(2, reaction.getEmoji());
                ps.setString(3, reaction.getReportReason());
                ps.setTimestamp(4, Timestamp.valueOf(reaction.getUpdatedAt() != null ? reaction.getUpdatedAt() : LocalDateTime.now()));
                ps.setInt(5, reaction.getIdReaction());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("ReactionRepository.update error: " + e.getMessage());
        }

        return false;
    }

    public boolean delete(Integer id) {
        if (!isValidId(id)) {
            return false;
        }

        String sql = "DELETE FROM reaction WHERE id_reaction = ?";
        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("ReactionRepository.delete error: " + e.getMessage());
        }

        return false;
    }

    public int deleteByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        String in = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        String sql = "DELETE FROM reaction WHERE id_reaction IN (" + in + ")";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return 0;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < ids.size(); i++) {
                    ps.setInt(i + 1, ids.get(i));
                }
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("ReactionRepository.deleteByIds error: " + e.getMessage());
        }

        return 0;
    }

    private List<Reaction> selectMany(String sql, SqlConsumer binder) {
        List<Reaction> list = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return list;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                binder.accept(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ReactionRepository.selectMany error: " + e.getMessage());
        }

        return list;
    }

    private int selectCount(String sql, SqlConsumer binder) {
        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return 0;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                binder.accept(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ReactionRepository.selectCount error: " + e.getMessage());
        }

        return 0;
    }

    private Reaction mapRow(ResultSet rs) throws SQLException {
        Reaction reaction = new Reaction();
        reaction.setIdReaction(rs.getInt("id_reaction"));
        reaction.setKind(rs.getString("kind"));
        reaction.setEmoji(rs.getString("emoji"));
        reaction.setReportReason(rs.getString("report_reason"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            reaction.setCreatedAt(created.toLocalDateTime());
        }

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            reaction.setUpdatedAt(updated.toLocalDateTime());
        }

        int userId = rs.getInt("user_id");
        if (!rs.wasNull()) {
            Optional<User> user = userRepository.findById(userId);
            user.ifPresent(reaction::setUser);
        }

        int publicationId = rs.getInt("publication_id");
        if (!rs.wasNull()) {
            Optional<Publication> publication = publicationRepository.findById(publicationId);
            publication.ifPresent(reaction::setPublication);
        }

        int commentId = rs.getInt("commentaire_id");
        if (!rs.wasNull()) {
            Optional<Commentaire> commentaire = commentaireRepository.findById(commentId);
            commentaire.ifPresent(reaction::setCommentaire);
        }

        return reaction;
    }

    private boolean isValidId(Integer id) {
        return id != null && id > 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @FunctionalInterface
    private interface SqlConsumer {
        void accept(PreparedStatement statement) throws SQLException;
    }
}
