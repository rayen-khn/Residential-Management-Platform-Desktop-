package com.syndicati.models.evenement.data;

import com.syndicati.models.evenement.Participation;
import com.syndicati.models.evenement.Evenement;
import com.syndicati.models.user.User;
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
 * Participation data access layer.
 */
public class ParticipationRepository {

    private final DatabaseService databaseService;
    private final EvenementRepository evenementRepo;

    public ParticipationRepository() {
        this.databaseService = DatabaseService.getInstance();
        this.evenementRepo = new EvenementRepository();
    }

    public List<Participation> findAll() {
        String sql = "SELECT id_participation, event_id, user_id, date_participation, statut_participation, " +
                "nb_accompagnants, commentaire_participation, created_at, edited_at FROM participation ORDER BY created_at DESC";
        List<Participation> participations = new ArrayList<>();

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                participations.add(mapResultSetToParticipation(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all participations: " + e.getMessage());
        }

        return participations;
    }

    public Optional<Participation> findById(Integer id) {
        String sql = "SELECT id_participation, event_id, user_id, date_participation, statut_participation, " +
                "nb_accompagnants, commentaire_participation, created_at, edited_at FROM participation WHERE id_participation = ?";

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToParticipation(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching participation by id: " + e.getMessage());
        }

        return Optional.empty();
    }

    public List<Participation> findByEvenementId(Integer eventId) {
        String sql = "SELECT id_participation, event_id, user_id, date_participation, statut_participation, " +
                "nb_accompagnants, commentaire_participation, created_at, edited_at FROM participation WHERE event_id = ? ORDER BY created_at DESC";
        List<Participation> participations = new ArrayList<>();

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    participations.add(mapResultSetToParticipation(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching participations by event: " + e.getMessage());
        }

        return participations;
    }

    public List<Participation> findByUserId(Integer userId) {
        String sql = "SELECT id_participation, event_id, user_id, date_participation, statut_participation, " +
                "nb_accompagnants, commentaire_participation, created_at, edited_at FROM participation WHERE user_id = ? ORDER BY created_at DESC";
        List<Participation> participations = new ArrayList<>();

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    participations.add(mapResultSetToParticipation(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching participations by user: " + e.getMessage());
        }

        return participations;
    }

    public List<Participation> findByStatut(String statut) {
        String sql = "SELECT id_participation, event_id, user_id, date_participation, statut_participation, " +
                "nb_accompagnants, commentaire_participation, created_at, edited_at FROM participation WHERE statut_participation = ? ORDER BY created_at DESC";
        List<Participation> participations = new ArrayList<>();

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, statut);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    participations.add(mapResultSetToParticipation(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching participations by statut: " + e.getMessage());
        }

        return participations;
    }

    public List<Participation> findByEvenementIdAndStatut(Integer eventId, String statut) {
        String sql = "SELECT id_participation, event_id, user_id, date_participation, statut_participation, " +
                "nb_accompagnants, commentaire_participation, created_at, edited_at FROM participation " +
                "WHERE event_id = ? AND statut_participation = ? ORDER BY created_at DESC";
        List<Participation> participations = new ArrayList<>();

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, eventId);
            stmt.setString(2, statut);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    participations.add(mapResultSetToParticipation(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching participations by event and statut: " + e.getMessage());
        }

        return participations;
    }

    public Integer insert(Participation participation) {
        String sql = "INSERT INTO participation (event_id, user_id, date_participation, statut_participation, " +
                "nb_accompagnants, commentaire_participation, formulaire_data, created_at, edited_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, participation.getEvenement().getIdEvent());
            stmt.setInt(2, participation.getUser().getIdUser());
            stmt.setObject(3, LocalDateTime.now());
            stmt.setString(4, participation.getStatutParticipation() != null ? participation.getStatutParticipation() : "en_attente");
            stmt.setObject(5, participation.getNbAccompagnants() != null ? participation.getNbAccompagnants() : 0);
            stmt.setString(6, participation.getCommentaireParticipation());
            stmt.setString(7, "{}");
            stmt.setObject(8, LocalDateTime.now());
            stmt.setObject(9, LocalDateTime.now());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting participation: " + e.getMessage());
        }

        return -1;
    }

    public boolean updateStatut(Integer id, String statut) {
        String sql = "UPDATE participation SET statut_participation = ?, edited_at = ? WHERE id_participation = ?";

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, statut);
            stmt.setObject(2, LocalDateTime.now());
            stmt.setInt(3, id);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating participation statut: " + e.getMessage());
        }

        return false;
    }

    public boolean update(Integer id, Integer nbAccompagnants, String commentaire) {
        String sql = "UPDATE participation SET nb_accompagnants = ?, commentaire_participation = ?, edited_at = ? WHERE id_participation = ?";

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, nbAccompagnants);
            stmt.setString(2, commentaire);
            stmt.setObject(3, LocalDateTime.now());
            stmt.setInt(4, id);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating participation: " + e.getMessage());
        }

        return false;
    }

    public boolean delete(Integer id) {
        String sql = "DELETE FROM participation WHERE id_participation = ?";

        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting participation: " + e.getMessage());
        }

        return false;
    }

    private Participation mapResultSetToParticipation(ResultSet rs) throws SQLException {
        Participation participation = new Participation();
        participation.setIdParticipation(rs.getInt("id_participation"));
        participation.setDateParticipation(rs.getObject("date_participation", LocalDateTime.class));
        participation.setStatutParticipation(rs.getString("statut_participation"));
        participation.setNbAccompagnants(rs.getObject("nb_accompagnants", Integer.class));
        participation.setCommentaireParticipation(rs.getString("commentaire_participation"));
        participation.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        participation.setEditedAt(rs.getObject("edited_at", LocalDateTime.class));

        Integer eventId = rs.getObject("event_id", Integer.class);
        if (eventId != null) {
            var eventOpt = evenementRepo.findById(eventId);
            eventOpt.ifPresent(participation::setEvenement);
        }

        Integer userId = rs.getObject("user_id", Integer.class);
        if (userId != null) {
            User user = new User();
            user.setIdUser(userId);
            participation.setUser(user);
        }

        return participation;
    }
}
