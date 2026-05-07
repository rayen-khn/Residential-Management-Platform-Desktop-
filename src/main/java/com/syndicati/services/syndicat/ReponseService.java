package com.syndicati.services.syndicat;

import com.syndicati.models.syndicat.Reponse;
import com.syndicati.models.syndicat.Reclamation;
import com.syndicati.models.syndicat.data.ReponseRepository;
import com.syndicati.models.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for Reponse business logic.
 * Handles CRUD operations and validation for responses aligned with Horizon.
 */
public class ReponseService {

    private final ReponseRepository repository;

    public ReponseService() {
        this.repository = new ReponseRepository();
    }

    public List<Reponse> listAll() {
        return repository.findAll();
    }

    public List<Reponse> listByReclamation(Reclamation reclamation) {
        if (reclamation == null || reclamation.getIdReclamations() == null) {
            return List.of();
        }
        return repository.findByReclamationId(reclamation.getIdReclamations());
    }

    public List<Reponse> listByUser(User user) {
        if (user == null || user.getIdUser() == null) {
            return List.of();
        }
        return repository.findByUserId(user.getIdUser());
    }

    public Optional<Reponse> findById(Integer id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return repository.findById(id);
    }

    public Integer create(String titre, String message, String image, Reclamation reclamation, User user) {
        ValidationResult validation = validateCreate(titre, message, reclamation, user);
        if (!validation.valid) {
            System.out.println("ReponseService.create validation failed: " + validation.message);
            return -1;
        }

        Reponse reponse = new Reponse();
        reponse.setTitreReponse(titre);
        reponse.setMessageReponse(message);
        reponse.setImageReponse(image);
        reponse.setReclamation(reclamation);
        reponse.setUser(user);

        return repository.create(reponse);
    }

    public boolean update(Integer id, String titre, String message, String image) {
        if (id == null || id <= 0) {
            return false;
        }
    
        Optional<Reponse> existing = repository.findById(id);
        if (existing.isEmpty()) {
            return false;
        }
    
        Reponse rep = existing.get();
    
        if (titre != null && !titre.isBlank()) {
            rep.setTitreReponse(titre);
        }
    
        if (message != null && !message.isBlank()) {
            rep.setMessageReponse(message);
        }
    
        if (image != null && !image.isBlank()) {
            rep.setImageReponse(image);
        }
    
        rep.setUpdatedAt(LocalDateTime.now());
        return repository.update(rep);
    }

    public boolean delete(Integer id) {
        if (id == null || id <= 0) {
            return false;
        }
        return repository.delete(id);
    }

    private ValidationResult validateCreate(String titre, String message, Reclamation reclamation, User user) {
        if (message == null || message.isBlank()) {
            return ValidationResult.invalid("messagereponse is required");
        }
        if (message.length() > 255) {
            return ValidationResult.invalid("messagereponse must not exceed 255 characters");
        }

        if (reclamation == null || reclamation.getIdReclamations() == null || reclamation.getIdReclamations() <= 0) {
            return ValidationResult.invalid("reclamation is required");
        }

        if (user == null || user.getIdUser() == null || user.getIdUser() <= 0) {
            return ValidationResult.invalid("user is required");
        }

        return ValidationResult.valid();
    }

    public ReponseRepository getRepository() {
        return repository;
    }

    private static class ValidationResult {
        final boolean valid;
        final String message;

        ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        static ValidationResult valid() {
            return new ValidationResult(true, "");
        }

        static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }
}
