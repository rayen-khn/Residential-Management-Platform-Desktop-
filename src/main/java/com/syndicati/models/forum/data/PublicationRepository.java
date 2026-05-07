package com.syndicati.models.forum.data;

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
 * JDBC repository for forum publications.
 */
public class PublicationRepository {

    private final DatabaseService databaseService;
    private final UserRepository userRepository;

    public PublicationRepository() {
        this.databaseService = DatabaseService.getInstance();
        this.userRepository = new UserRepository();
    }

    public List<Publication> findAll() {
        String sql = "SELECT * FROM publication ORDER BY date_creation_pub DESC";
        List<Publication> publications = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return publications;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    publications.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("PublicationRepository.findAll error: " + e.getMessage());
        }

        return publications;
    }

    public Optional<Publication> findById(Integer id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }

        String sql = "SELECT * FROM publication WHERE id = ?";

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
            System.out.println("PublicationRepository.findById error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public List<Publication> findByUserId(Integer userId) {
        if (userId == null || userId <= 0) {
            return new ArrayList<>();
        }

        String sql = "SELECT * FROM publication WHERE user_id = ? ORDER BY date_creation_pub DESC";
        List<Publication> publications = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return publications;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        publications.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("PublicationRepository.findByUserId error: " + e.getMessage());
        }

        return publications;
    }

    public List<Publication> findByCategory(String category) {
        List<Publication> publications = new ArrayList<>();
        String sql;

        if (category == null || category.isBlank()) {
            return findAll();
        }

        if ("General".equals(category)) {
            sql = "SELECT * FROM publication WHERE categorie_pub <> ? ORDER BY date_creation_pub DESC";
        } else {
            sql = "SELECT * FROM publication WHERE categorie_pub = ? ORDER BY date_creation_pub DESC";
        }

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return publications;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, "General".equals(category) ? "Announcement" : category);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        publications.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("PublicationRepository.findByCategory error: " + e.getMessage());
        }

        return publications;
    }

    public Integer create(Publication publication) {
        if (publication == null || publication.getUser() == null) {
            return -1;
        }

        String sql = "INSERT INTO publication (titre_pub, description_pub, date_creation_pub, categorie_pub, image_pub, user_id) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return -1;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, publication.getTitrePub());
                ps.setString(2, publication.getDescriptionPub());
                LocalDateTime now = publication.getDateCreationPub() != null ? publication.getDateCreationPub() : LocalDateTime.now();
                ps.setTimestamp(3, Timestamp.valueOf(now));
                ps.setString(4, publication.getCategoriePub());
                ps.setString(5, publication.getImagePub());
                ps.setInt(6, publication.getUser().getIdUser());

                ps.executeUpdate();

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        publication.setIdPublication(id);
                        publication.setDateCreationPub(now);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("PublicationRepository.create error: " + e.getMessage());
        }

        return -1;
    }

    public boolean update(Publication publication) {
        if (publication == null || publication.getIdPublication() == null || publication.getIdPublication() <= 0) {
            return false;
        }

        String sql = "UPDATE publication SET titre_pub = ?, description_pub = ?, date_creation_pub = ?, categorie_pub = ?, image_pub = ?, user_id = ? WHERE id = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, publication.getTitrePub());
                ps.setString(2, publication.getDescriptionPub());
                LocalDateTime date = publication.getDateCreationPub() != null ? publication.getDateCreationPub() : LocalDateTime.now();
                ps.setTimestamp(3, Timestamp.valueOf(date));
                ps.setString(4, publication.getCategoriePub());
                ps.setString(5, publication.getImagePub());
                ps.setInt(6, publication.getUser() != null ? publication.getUser().getIdUser() : 0);
                ps.setInt(7, publication.getIdPublication());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("PublicationRepository.update error: " + e.getMessage());
        }

        return false;
    }

    public boolean delete(Integer id) {
        if (id == null || id <= 0) {
            return false;
        }

        String sql = "DELETE FROM publication WHERE id = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("PublicationRepository.delete error: " + e.getMessage());
        }

        return false;
    }

    public List<Publication> findAllLatest() {
        return findAll();
    }

    private Publication mapRow(ResultSet rs) throws SQLException {
        Publication publication = new Publication();
        publication.setIdPublication(rs.getInt("id"));
        publication.setTitrePub(rs.getString("titre_pub"));
        publication.setDescriptionPub(rs.getString("description_pub"));
        publication.setCategoriePub(rs.getString("categorie_pub"));
        publication.setImagePub(rs.getString("image_pub"));

        Timestamp creationTs = rs.getTimestamp("date_creation_pub");
        if (creationTs != null) {
            publication.setDateCreationPub(creationTs.toLocalDateTime());
        }

        int userId = rs.getInt("user_id");
        if (!rs.wasNull()) {
            Optional<User> user = userRepository.findById(userId);
            user.ifPresent(publication::setUser);
        }

        return publication;
    }
}