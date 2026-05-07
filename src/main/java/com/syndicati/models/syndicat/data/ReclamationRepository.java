package com.syndicati.models.syndicat.data;

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
 * JDBC repository for Reclamation CRUD operations.
 * Aligned with web database schema using direct queries.
 */
public class ReclamationRepository {

    private final DatabaseService databaseService;
    private final UserRepository userRepository;

    public ReclamationRepository() {
        this.databaseService = DatabaseService.getInstance();
        this.userRepository = new UserRepository();
    }

    public List<Reclamation> findAll() {
        String sql = "SELECT * FROM reclamations ORDER BY created_at DESC";
        List<Reclamation> reclamations = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return reclamations;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        reclamations.add(mapRow(rs));
                    } catch (SQLException rowError) {
                        System.out.println("ReclamationRepository.findAll skipped row due to mapping error: " + rowError.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ReclamationRepository.findAll error: " + e.getMessage());
        }

        return reclamations;
    }

    public Optional<Reclamation> findById(Integer id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }

        String sql = "SELECT * FROM reclamations WHERE idreclamations = ?";

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
            System.out.println("ReclamationRepository.findById error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public List<Reclamation> findByUserId(Integer userId) {
        if (userId == null || userId <= 0) {
            return new ArrayList<>();
        }

        String sql = "SELECT * FROM reclamations WHERE id_user = ? ORDER BY created_at DESC";
        List<Reclamation> reclamations = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return reclamations;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        reclamations.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ReclamationRepository.findByUserId error: " + e.getMessage());
        }

        return reclamations;
    }

    public List<Reclamation> findByStatut(String statut) {
        if (statut == null || statut.isBlank()) {
            return new ArrayList<>();
        }

        String sql = "SELECT * FROM reclamations WHERE statutreclamation = ? ORDER BY created_at DESC";
        List<Reclamation> reclamations = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return reclamations;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, statut);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        reclamations.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ReclamationRepository.findByStatut error: " + e.getMessage());
        }

        return reclamations;
    }

    public Integer create(Reclamation reclamation) {
        if (reclamation == null || reclamation.getUser() == null) {
            return -1;
        }

        String sql = "INSERT INTO reclamations (titrereclamations, descreclamation, datereclamation, statutreclamation, imagereclamation, id_user, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return -1;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, reclamation.getTitreReclamations());
                ps.setString(2, reclamation.getDescReclamation());
                ps.setTimestamp(3, reclamation.getDateReclamation() != null ? Timestamp.valueOf(reclamation.getDateReclamation()) : Timestamp.valueOf(LocalDateTime.now()));
                ps.setString(4, reclamation.getStatutReclamation() != null ? reclamation.getStatutReclamation() : "en_attente");
                ps.setString(5, reclamation.getImageReclamation());
                ps.setInt(6, reclamation.getUser().getIdUser());
                LocalDateTime now = LocalDateTime.now();
                ps.setTimestamp(7, Timestamp.valueOf(now));
                ps.setTimestamp(8, Timestamp.valueOf(now));

                ps.executeUpdate();

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        reclamation.setIdReclamations(id);
                        reclamation.setCreatedAt(now);
                        reclamation.setUpdatedAt(now);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ReclamationRepository.create error: " + e.getMessage());
        }

        return -1;
    }

    public boolean update(Reclamation reclamation) {
        if (reclamation == null || reclamation.getIdReclamations() == null || reclamation.getIdReclamations() <= 0) {
            return false;
        }

        String sql = "UPDATE reclamations SET titrereclamations = ?, descreclamation = ?, datereclamation = ?, statutreclamation = ?, imagereclamation = ?, updated_at = ? WHERE idreclamations = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, reclamation.getTitreReclamations());
                ps.setString(2, reclamation.getDescReclamation());
                ps.setTimestamp(3, Timestamp.valueOf(reclamation.getDateReclamation()));
                ps.setString(4, reclamation.getStatutReclamation());
                ps.setString(5, reclamation.getImageReclamation());
                ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(7, reclamation.getIdReclamations());

                int affectedRows = ps.executeUpdate();
                return affectedRows > 0;
            }
        } catch (SQLException e) {
            System.out.println("ReclamationRepository.update error: " + e.getMessage());
        }

        return false;
    }

    public boolean delete(Integer id) {
        if (id == null || id <= 0) {
            return false;
        }

        String sql = "DELETE FROM reclamations WHERE idreclamations = ?";

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
            System.out.println("ReclamationRepository.delete error: " + e.getMessage());
        }

        return false;
    }

    private Reclamation mapRow(ResultSet rs) throws SQLException {
        Reclamation rec = new Reclamation();
        rec.setIdReclamations(rs.getInt("idreclamations"));
        rec.setTitreReclamations(rs.getString("titrereclamations"));
        rec.setDescReclamation(rs.getString("descreclamation"));

        Timestamp dateRecTs = rs.getTimestamp("datereclamation");
        if (dateRecTs != null) {
            rec.setDateReclamation(dateRecTs.toLocalDateTime());
        }

        rec.setStatutReclamation(rs.getString("statutreclamation"));
        rec.setImageReclamation(rs.getString("imagereclamation"));

        int userId = rs.getInt("id_user");
        if (!rs.wasNull()) {
            Optional<User> user = userRepository.findById(userId);
            user.ifPresent(rec::setUser);
        }

        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            rec.setCreatedAt(createdTs.toLocalDateTime());
        }

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        if (updatedTs != null) {
            rec.setUpdatedAt(updatedTs.toLocalDateTime());
        }

        return rec;
    }
}
