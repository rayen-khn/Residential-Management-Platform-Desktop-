package com.syndicati.services.evenement;

import com.syndicati.models.evenement.Evenement;
import com.syndicati.models.evenement.data.EvenementRepository;
import com.syndicati.models.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Evenement business logic.
 */
public class EvenementService {

    private final EvenementRepository repository;

    public EvenementService() {
        this.repository = new EvenementRepository();
    }

    public List<Evenement> listAll() {
        return repository.findAll();
    }

    public List<Evenement> listByUser(User user) {
        if (user == null || user.getIdUser() == null) {
            return List.of();
        }
        return repository.findByUserId(user.getIdUser());
    }

    public List<Evenement> listByType(String type) {
        if (type == null || type.isBlank()) {
            return List.of();
        }
        return repository.findByType(type);
    }

    public List<Evenement> listByStatut(String statut) {
        if (statut == null || statut.isBlank()) {
            return List.of();
        }
        return repository.findByStatut(statut);
    }

    public Optional<Evenement> findById(Integer id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return repository.findById(id);
    }

    public Integer create(String titre, String description, LocalDateTime dateEvent, String lieu, Integer nbPlaces, String type, String image, User user) {
        ValidationResult validation = validateCreate(titre, description, dateEvent, lieu, nbPlaces, type, user);
        if (!validation.valid) {
            System.out.println("EvenementService.create validation failed: " + validation.message);
            return -1;
        }

        Evenement event = new Evenement();
        event.setTitreEvent(titre);
        event.setDescriptionEvent(description);
        event.setDateEvent(dateEvent);
        event.setLieuEvent(lieu);
        event.setNbPlaces(nbPlaces);
        event.setNbRestants(nbPlaces);
        event.setTypeEvent(type);
        event.setImageEvent(image);
        event.setStatutEvent("planifie");
        event.setUser(user);
        event.setCreatedAt(LocalDateTime.now());
        event.setEditedAt(LocalDateTime.now());

        return repository.insert(event);
    }

    public boolean update(Integer id, String titre, String description, LocalDateTime dateEvent, String lieu, Integer nbPlaces, String type) {
        if (id == null || id <= 0) {
            return false;
        }

        Optional<Evenement> existingOpt = repository.findById(id);
        if (existingOpt.isEmpty()) {
            return false;
        }

        ValidationResult validation = validateUpdate(titre, description, dateEvent, lieu, nbPlaces, type);
        if (!validation.valid) {
            System.out.println("EvenementService.update validation failed: " + validation.message);
            return false;
        }

        return repository.update(id, titre, description, dateEvent, lieu, nbPlaces, type);
    }

    public boolean updateForDashboard(Integer id, String titre, String description, LocalDateTime dateEvent, String lieu, Integer nbPlaces, Integer nbRestants, String type, String image) {
        if (id == null || id <= 0) {
            return false;
        }

        ValidationResult validation = validateUpdate(titre, description, dateEvent, lieu, nbPlaces, type);
        if (!validation.valid) {
            System.out.println("EvenementService.updateForDashboard validation failed: " + validation.message);
            return false;
        }

        if (nbRestants == null || nbRestants < 0) {
            System.out.println("EvenementService.updateForDashboard validation failed: available places must be >= 0");
            return false;
        }

        if (nbPlaces != null && nbRestants > nbPlaces) {
            System.out.println("EvenementService.updateForDashboard validation failed: available places cannot exceed total places");
            return false;
        }

        return repository.updateForDashboard(id, titre, description, dateEvent, lieu, nbPlaces, nbRestants, type, image);
    }

    public boolean updateStatut(Integer id, String statut) {
        if (id == null || id <= 0) {
            return false;
        }

        if (statut == null || !Evenement.STATUTS.contains(statut)) {
            System.out.println("Invalid statut: " + statut);
            return false;
        }

        return repository.updateStatut(id, statut);
    }

    public boolean delete(Integer id) {
        if (id == null || id <= 0) {
            return false;
        }

        return repository.delete(id);
    }

    private ValidationResult validateCreate(String titre, String description, LocalDateTime dateEvent, String lieu, Integer nbPlaces, String type, User user) {
        if (titre == null || titre.trim().isEmpty() || titre.length() > 255) {
            return new ValidationResult(false, "Invalid titre");
        }

        if (description == null || description.trim().isEmpty() || description.length() < 10 || description.length() > 500) {
            return new ValidationResult(false, "Description must be 10-500 characters");
        }

        if (dateEvent == null) {
            return new ValidationResult(false, "Date event is required");
        }

        if (dateEvent.isBefore(LocalDateTime.now())) {
            return new ValidationResult(false, "Date event must be in the future");
        }

        if (lieu == null || lieu.trim().isEmpty() || lieu.length() < 3 || lieu.length() > 255) {
            return new ValidationResult(false, "Lieu must be 3-255 characters");
        }

        if (nbPlaces == null || nbPlaces < 0) {
            return new ValidationResult(false, "Number of places must be >= 0");
        }

        if (type == null || !Evenement.TYPES.contains(type)) {
            return new ValidationResult(false, "Invalid event type");
        }

        if (user == null || user.getIdUser() == null) {
            return new ValidationResult(false, "User is required");
        }

        return new ValidationResult(true, "Valid");
    }

    private ValidationResult validateUpdate(String titre, String description, LocalDateTime dateEvent, String lieu, Integer nbPlaces, String type) {
        if (titre == null || titre.trim().isEmpty() || titre.length() > 255) {
            return new ValidationResult(false, "Invalid titre");
        }

        if (description == null || description.trim().isEmpty() || description.length() < 10 || description.length() > 500) {
            return new ValidationResult(false, "Description must be 10-500 characters");
        }

        if (dateEvent == null) {
            return new ValidationResult(false, "Date event is required");
        }

        if (lieu == null || lieu.trim().isEmpty() || lieu.length() < 3 || lieu.length() > 255) {
            return new ValidationResult(false, "Lieu must be 3-255 characters");
        }

        if (nbPlaces == null || nbPlaces < 0) {
            return new ValidationResult(false, "Number of places must be >= 0");
        }

        if (type == null || !Evenement.TYPES.contains(type)) {
            return new ValidationResult(false, "Invalid event type");
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
