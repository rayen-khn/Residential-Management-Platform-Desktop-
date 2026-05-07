package com.syndicati.models.repositories;

import com.syndicati.models.entities.Publication;
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
 * JDBC repository for publication CRUD using web-aligned table/column names.
 */
public class PublicationRepository {

    private final DatabaseService databaseService;

    public PublicationRepository() {
        this.databaseService = DatabaseService.getInstance();
    }

    public List<Publication> findAllByDateDesc() {
        String sql = "SELECT p.*, p.user_id AS pub_user_id, " +
                     "u.first_name AS author_fname, " +
                     "u.last_name AS author_lname, " +
                     "pr.avatar AS author_av " +
                     "FROM publication p " +
                     "LEFT JOIN user u ON p.user_id = u.id_user " +
                     "LEFT JOIN profile pr ON u.id_user = pr.user_id " +
                     "ORDER BY p.date_creation_pub DESC";
        List<Publication> publications = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return publications;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        publications.add(mapRow(rs));
                    } catch (SQLException rowError) {
                        System.out.println("PublicationRepository.findAllByDateDesc skipped row due to mapping error: " + rowError.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("PublicationRepository.findAllByDateDesc error: " + e.getMessage());
        }

        return publications;
    }

    public Optional<Publication> findById(int id) {
        String sql = "SELECT p.*, " +
                     "u.first_name AS author_fname, " +
                     "u.last_name AS author_lname, " +
                     "pr.avatar AS author_av " +
                     "FROM publication p " +
                     "LEFT JOIN user u ON p.user_id = u.id_user " +
                     "LEFT JOIN profile pr ON u.id_user = pr.user_id " +
                     "WHERE p.id = ?";

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

    public int create(Publication publication) {
        String sql = "INSERT INTO publication (titre_pub, description_pub, date_creation_pub, categorie_pub, image_pub, user_id) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return -1;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, publication.getTitrePub());
                ps.setString(2, publication.getDescriptionPub());
                ps.setTimestamp(3, Timestamp.valueOf(publication.getDateCreationPub()));
                ps.setString(4, publication.getCategoriePub());
                ps.setString(5, publication.getImagePub());
                ps.setInt(6, publication.getUserId());

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
            System.out.println("PublicationRepository.create error: " + e.getMessage());
        }

        return -1;
    }

    public boolean update(Publication publication) {
        String sql = "UPDATE publication SET titre_pub = ?, description_pub = ?, categorie_pub = ?, image_pub = ? WHERE id = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, publication.getTitrePub());
                ps.setString(2, publication.getDescriptionPub());
                ps.setString(3, publication.getCategoriePub());
                ps.setString(4, publication.getImagePub());
                ps.setInt(5, publication.getId());

                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("PublicationRepository.update error: " + e.getMessage());
        }

        return false;
    }

    public List<Publication> findAllBookmarkedByUserId(int userId) {
        String sql = "SELECT p.*, " +
                     "u.first_name AS author_fname, " +
                     "u.last_name AS author_lname, " +
                     "pr.avatar AS author_av " +
                     "FROM publication p " +
                     "JOIN reaction r ON p.id = r.publication_id " +
                     "LEFT JOIN user u ON p.user_id = u.id_user " +
                     "LEFT JOIN profile pr ON u.id_user = pr.user_id " +
                     "WHERE r.user_id = ? AND r.kind = 'Bookmark' " +
                     "ORDER BY p.date_creation_pub DESC";
        List<Publication> publications = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) return publications;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        publications.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("PublicationRepository.findAllBookmarkedByUserId error: " + e.getMessage());
        }
        return publications;
    }

    private Publication mapRow(ResultSet rs) throws SQLException {
        Publication pub = new Publication();
        pub.setId(rs.getInt("id"));
        pub.setTitrePub(rs.getString("titre_pub"));
        pub.setDescriptionPub(rs.getString("description_pub"));
        pub.setDateCreationPub(readLocalDateTime(rs, "date_creation_pub"));
        pub.setCategoriePub(rs.getString("categorie_pub"));
        pub.setImagePub(rs.getString("image_pub"));
        
        int userId = rs.getInt("pub_user_id");
        if (!rs.wasNull()) {
            pub.setUserId(userId);
        }

        // Fallback for null dates to prevent UI crashes
        if (pub.getDateCreationPub() == null) {
            pub.setDateCreationPub(LocalDateTime.now());
        }

        // Author identity fields from JOIN
        try {
            pub.setAuthorFirstName(rs.getString("author_fname"));
            pub.setAuthorLastName(rs.getString("author_lname"));
            pub.setAuthorAvatar(rs.getString("author_av"));
        } catch (SQLException e) {
            // Silently fail if columns are missing
        }
        
        return pub;
    }

    private LocalDateTime readLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }

    public boolean delete(int id) {
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
}
