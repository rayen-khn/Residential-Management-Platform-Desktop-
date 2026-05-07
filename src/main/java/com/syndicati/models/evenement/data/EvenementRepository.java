package com.syndicati.models.evenement.data;

import com.syndicati.models.evenement.Evenement;
import com.syndicati.services.DatabaseService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Evenement data access layer.
 */
public class EvenementRepository {

    private final DatabaseService databaseService;

    public EvenementRepository() {
        this.databaseService = DatabaseService.getInstance();
    }

    public List<Evenement> findAll() {
        String sql = "SELECT id_event, titre_event, description_event, date_event, lieu_event, " +
                "nb_places, nb_restants, statut_event, image_event, type_event, created_at, edited_at, user_id " +
                "FROM evenement ORDER BY date_event DESC";
        List<Evenement> events = new ArrayList<>();

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                events.add(mapResultSetToEvenement(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all evenements: " + e.getMessage());
        }

        return events;
    }

    public Optional<Evenement> findById(Integer id) {
        String sql = "SELECT id_event, titre_event, description_event, date_event, lieu_event, " +
                "nb_places, nb_restants, statut_event, image_event, type_event, created_at, edited_at, user_id " +
                "FROM evenement WHERE id_event = ?";

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEvenement(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching evenement by id: " + e.getMessage());
        }

        return Optional.empty();
    }

    public List<Evenement> findByUserId(Integer userId) {
        String sql = "SELECT id_event, titre_event, description_event, date_event, lieu_event, " +
                "nb_places, nb_restants, statut_event, image_event, type_event, created_at, edited_at, user_id " +
                "FROM evenement WHERE user_id = ? ORDER BY date_event DESC";
        List<Evenement> events = new ArrayList<>();

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapResultSetToEvenement(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching evenements by user: " + e.getMessage());
        }

        return events;
    }

    public List<Evenement> findByType(String type) {
        String sql = "SELECT id_event, titre_event, description_event, date_event, lieu_event, " +
                "nb_places, nb_restants, statut_event, image_event, type_event, created_at, edited_at, user_id " +
                "FROM evenement WHERE type_event = ? AND statut_event IN ('planifie', 'en_cours') ORDER BY date_event ASC";
        List<Evenement> events = new ArrayList<>();

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, type);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapResultSetToEvenement(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching evenements by type: " + e.getMessage());
        }

        return events;
    }

    public List<Evenement> findByStatut(String statut) {
        String sql = "SELECT id_event, titre_event, description_event, date_event, lieu_event, " +
                "nb_places, nb_restants, statut_event, image_event, type_event, created_at, edited_at, user_id " +
                "FROM evenement WHERE statut_event = ? ORDER BY date_event DESC";
        List<Evenement> events = new ArrayList<>();

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, statut);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    events.add(mapResultSetToEvenement(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching evenements by statut: " + e.getMessage());
        }

        return events;
    }

    public Integer insert(Evenement event) {
        String sql = "INSERT INTO evenement (titre_event, description_event, date_event, lieu_event, " +
                "nb_places, nb_restants, statut_event, image_event, type_event, created_at, edited_at, user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, event.getTitreEvent());
            stmt.setString(2, event.getDescriptionEvent());
            stmt.setObject(3, event.getDateEvent());
            stmt.setString(4, event.getLieuEvent());
            stmt.setObject(5, event.getNbPlaces());
            stmt.setObject(6, event.getNbRestants());
            stmt.setString(7, event.getStatutEvent() != null ? event.getStatutEvent() : "planifie");
            stmt.setString(8, event.getImageEvent());
            stmt.setString(9, event.getTypeEvent());
            stmt.setObject(10, LocalDateTime.now());
            stmt.setObject(11, LocalDateTime.now());
            stmt.setInt(12, event.getUser().getIdUser());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting evenement: " + e.getMessage());
        }

        return -1;
    }

    public boolean update(Integer id, String titre, String description, LocalDateTime date, String lieu, Integer nbPlaces, String type) {
        String sql = "UPDATE evenement SET titre_event = ?, description_event = ?, date_event = ?, " +
                "lieu_event = ?, nb_places = ?, type_event = ?, edited_at = ? WHERE id_event = ?";

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, titre);
            stmt.setString(2, description);
            stmt.setObject(3, date);
            stmt.setString(4, lieu);
            stmt.setObject(5, nbPlaces);
            stmt.setString(6, type);
            stmt.setObject(7, LocalDateTime.now());
            stmt.setInt(8, id);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating evenement: " + e.getMessage());
        }

        return false;
    }

    public boolean updateForDashboard(Integer id, String titre, String description, LocalDateTime date, String lieu, Integer nbPlaces, Integer nbRestants, String type, String image) {
        String sql = "UPDATE evenement SET titre_event = ?, description_event = ?, date_event = ?, " +
                "lieu_event = ?, nb_places = ?, nb_restants = ?, type_event = ?, image_event = ?, edited_at = ? WHERE id_event = ?";

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, titre);
            stmt.setString(2, description);
            stmt.setObject(3, date);
            stmt.setString(4, lieu);
            stmt.setObject(5, nbPlaces);
            stmt.setObject(6, nbRestants);
            stmt.setString(7, type);
            stmt.setString(8, image);
            stmt.setObject(9, LocalDateTime.now());
            stmt.setInt(10, id);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating evenement for dashboard: " + e.getMessage());
        }

        return false;
    }

    public boolean updateStatut(Integer id, String statut) {
        String sql = "UPDATE evenement SET statut_event = ?, edited_at = ? WHERE id_event = ?";

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, statut);
            stmt.setObject(2, LocalDateTime.now());
            stmt.setInt(3, id);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating evenement statut: " + e.getMessage());
        }

        return false;
    }

    public boolean delete(Integer id) {
        String sql = "DELETE FROM evenement WHERE id_event = ?";

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting evenement: " + e.getMessage());
        }

        return false;
    }

    private Evenement mapResultSetToEvenement(ResultSet rs) throws SQLException {
        Evenement event = new Evenement();
        event.setIdEvent(rs.getInt("id_event"));
        event.setTitreEvent(rs.getString("titre_event"));
        event.setDescriptionEvent(rs.getString("description_event"));
        event.setDateEvent(rs.getObject("date_event", LocalDateTime.class));
        event.setLieuEvent(rs.getString("lieu_event"));
        event.setNbPlaces(rs.getObject("nb_places", Integer.class));
        event.setNbRestants(rs.getObject("nb_restants", Integer.class));
        event.setStatutEvent(rs.getString("statut_event"));
        event.setImageEvent(rs.getString("image_event"));
        event.setTypeEvent(rs.getString("type_event"));
        event.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        event.setEditedAt(rs.getObject("edited_at", LocalDateTime.class));

        Integer userId = rs.getObject("user_id", Integer.class);
        if (userId != null) {
            com.syndicati.models.user.User user = new com.syndicati.models.user.User();
            user.setIdUser(userId);
            event.setUser(user);
        }

        return event;
    }
}
