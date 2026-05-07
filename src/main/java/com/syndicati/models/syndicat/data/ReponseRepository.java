package com.syndicati.models.syndicat.data;

import com.syndicati.models.syndicat.Reponse;
import com.syndicati.models.syndicat.Reclamation;
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
 * JDBC repository for Reponse CRUD operations.
 * Aligned with web database schema using direct queries.
 */
public class ReponseRepository {

    private final DatabaseService databaseService;
    private final UserRepository userRepository;
    private final ReclamationRepository reclamationRepository;

    public ReponseRepository() {
        this.databaseService = DatabaseService.getInstance();
        this.userRepository = new UserRepository();
        this.reclamationRepository = new ReclamationRepository();
    }

    public List<Reponse> findAll() {
        String sql = "SELECT * FROM reponses ORDER BY created_at ASC";
        List<Reponse> reponses = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return reponses;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        reponses.add(mapRow(rs));
                    } catch (SQLException rowError) {
                        System.out.println("ReponseRepository.findAll skipped row due to mapping error: " + rowError.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ReponseRepository.findAll error: " + e.getMessage());
        }

        return reponses;
    }

    public Optional<Reponse> findById(Integer id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }

        String sql = "SELECT * FROM reponses WHERE idreponses = ?";

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
            System.out.println("ReponseRepository.findById error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public List<Reponse> findByReclamationId(Integer reclamationId) {
        if (reclamationId == null || reclamationId <= 0) {
            return new ArrayList<>();
        }

        String sql = "SELECT * FROM reponses WHERE reclamation_id = ? ORDER BY created_at ASC";
        List<Reponse> reponses = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return reponses;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, reclamationId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        reponses.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ReponseRepository.findByReclamationId error: " + e.getMessage());
        }

        return reponses;
    }

    public List<Reponse> findByUserId(Integer userId) {
        if (userId == null || userId <= 0) {
            return new ArrayList<>();
        }

        String sql = "SELECT * FROM reponses WHERE id_user = ? ORDER BY created_at DESC";
        List<Reponse> reponses = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return reponses;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        reponses.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ReponseRepository.findByUserId error: " + e.getMessage());
        }

        return reponses;
    }

    public Integer create(Reponse reponse) {
        if (reponse == null || reponse.getReclamation() == null || reponse.getUser() == null) {
            return -1;
        }

        String sql = "INSERT INTO reponses (titrereponse, messagereponse, imagereponse, reclamation_id, id_user, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return -1;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, reponse.getTitreReponse());
                ps.setString(2, reponse.getMessageReponse());
                ps.setString(3, reponse.getImageReponse());
                ps.setInt(4, reponse.getReclamation().getIdReclamations());
                ps.setInt(5, reponse.getUser().getIdUser());
                LocalDateTime now = LocalDateTime.now();
                ps.setTimestamp(6, Timestamp.valueOf(now));
                ps.setTimestamp(7, Timestamp.valueOf(now));

                ps.executeUpdate();

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        reponse.setIdReponses(id);
                        reponse.setCreatedAt(now);
                        reponse.setUpdatedAt(now);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ReponseRepository.create error: " + e.getMessage());
        }

        return -1;
    }

    public boolean update(Reponse reponse) {
        if (reponse == null || reponse.getIdReponses() == null || reponse.getIdReponses() <= 0) {
            return false;
        }

        String sql = "UPDATE reponses SET titrereponse = ?, messagereponse = ?, imagereponse = ?, updated_at = ? WHERE idreponses = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, reponse.getTitreReponse());
                ps.setString(2, reponse.getMessageReponse());
                ps.setString(3, reponse.getImageReponse());
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(5, reponse.getIdReponses());

                int affectedRows = ps.executeUpdate();
                return affectedRows > 0;
            }
        } catch (SQLException e) {
            System.out.println("ReponseRepository.update error: " + e.getMessage());
        }

        return false;
    }

    public boolean delete(Integer id) {
        if (id == null || id <= 0) {
            return false;
        }

        String sql = "DELETE FROM reponses WHERE idreponses = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                int affectedRows = ps.executeUpdate();
                return affectedRows > 0;
            }
        } catch (SQLException e) {
            System.out.println("ReponseRepository.delete error: " + e.getMessage());
        }

        return false;
    }

    private Reponse mapRow(ResultSet rs) throws SQLException {
        Reponse rep = new Reponse();
        rep.setIdReponses(rs.getInt("idreponses"));
        rep.setTitreReponse(rs.getString("titrereponse"));
        rep.setMessageReponse(rs.getString("messagereponse"));
        rep.setImageReponse(rs.getString("imagereponse"));

        int reclamationId = rs.getInt("reclamation_id");
        if (!rs.wasNull()) {
            Optional<Reclamation> reclamation = reclamationRepository.findById(reclamationId);
            reclamation.ifPresent(rep::setReclamation);
        }

        int userId = rs.getInt("id_user");
        if (!rs.wasNull()) {
            Optional<User> user = userRepository.findById(userId);
            user.ifPresent(rep::setUser);
        }

        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            rep.setCreatedAt(createdTs.toLocalDateTime());
        }

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        if (updatedTs != null) {
            rep.setUpdatedAt(updatedTs.toLocalDateTime());
        }

        return rep;
    }
}
