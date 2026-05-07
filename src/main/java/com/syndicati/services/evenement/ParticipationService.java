package com.syndicati.services.evenement;

import com.syndicati.models.evenement.Participation;
import com.syndicati.models.evenement.Evenement;
import com.syndicati.models.evenement.data.ParticipationRepository;
import com.syndicati.models.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Participation business logic.
 */
public class ParticipationService {

    private final ParticipationRepository repository;

    public ParticipationService() {
        this.repository = new ParticipationRepository();
    }

    public List<Participation> listAll() {
        return repository.findAll();
    }

    public List<Participation> listByEvenement(Evenement event) {
        if (event == null || event.getIdEvent() == null) {
            return List.of();
        }
        return repository.findByEvenementId(event.getIdEvent());
    }

    public List<Participation> listByUser(User user) {
        if (user == null || user.getIdUser() == null) {
            return List.of();
        }
        return repository.findByUserId(user.getIdUser());
    }

    public List<Participation> listByStatut(String statut) {
        if (statut == null || statut.isBlank()) {
            return List.of();
        }
        return repository.findByStatut(statut);
    }

    public List<Participation> listByEvenementAndStatut(Evenement event, String statut) {
        if (event == null || event.getIdEvent() == null || statut == null || statut.isBlank()) {
            return List.of();
        }
        return repository.findByEvenementIdAndStatut(event.getIdEvent(), statut);
    }

    public Optional<Participation> findById(Integer id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return repository.findById(id);
    }

    public Integer create(Evenement event, User user, Integer nbAccompagnants, String commentaire) {
        ValidationResult validation = validateCreate(event, user);
        if (!validation.valid) {
            System.out.println("ParticipationService.create validation failed: " + validation.message);
            return -1;
        }

        Participation participation = new Participation();
        participation.setEvenement(event);
        participation.setUser(user);
        participation.setNbAccompagnants(nbAccompagnants != null ? nbAccompagnants : 0);
        participation.setCommentaireParticipation(commentaire);
        participation.setStatutParticipation("en_attente");
        participation.setDateParticipation(LocalDateTime.now());
        participation.setCreatedAt(LocalDateTime.now());
        participation.setEditedAt(LocalDateTime.now());

        return repository.insert(participation);
    }

    public boolean updateStatut(Integer id, String statut) {
        if (id == null || id <= 0) {
            return false;
        }

        if (statut == null || !Participation.STATUTS.contains(statut)) {
            System.out.println("Invalid statut: " + statut);
            return false;
        }

        return repository.updateStatut(id, statut);
    }

    public boolean update(Integer id, Integer nbAccompagnants, String commentaire) {
        if (id == null || id <= 0) {
            return false;
        }

        if (nbAccompagnants != null && nbAccompagnants < 0) {
            System.out.println("Invalid number of accompaniers");
            return false;
        }

        return repository.update(id, nbAccompagnants, commentaire);
    }

    public boolean delete(Integer id) {
        if (id == null || id <= 0) {
            return false;
        }

        return repository.delete(id);
    }

    private ValidationResult validateCreate(Evenement event, User user) {
        if (event == null || event.getIdEvent() == null) {
            return new ValidationResult(false, "Evenement is required");
        }

        if (user == null || user.getIdUser() == null) {
            return new ValidationResult(false, "User is required");
        }

        if (event.getNbRestants() == null || event.getNbRestants() <= 0) {
            return new ValidationResult(false, "No spots remaining for this event");
        }

        return new ValidationResult(true, "Valid");
    }

    private static class ValidationResult {
        boolean valid;
        String message;

        ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
    }
}
