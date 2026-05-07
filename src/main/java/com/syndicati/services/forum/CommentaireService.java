package com.syndicati.services.forum;

import com.syndicati.models.forum.Commentaire;
import com.syndicati.models.forum.Publication;
import com.syndicati.models.forum.data.CommentaireRepository;
import com.syndicati.models.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Forum comment business logic.
 */
public class CommentaireService {

    private final CommentaireRepository repository;

    public CommentaireService() {
        this.repository = new CommentaireRepository();
    }

    public List<Commentaire> listAll() {
        return repository.findAll();
    }

    public List<Commentaire> listByPublication(Publication publication) {
        if (publication == null || publication.getIdPublication() == null) {
            return List.of();
        }
        return repository.findByPublicationId(publication.getIdPublication());
    }

    public List<Commentaire> listByUser(User user) {
        if (user == null || user.getIdUser() == null) {
            return List.of();
        }
        return repository.findByUserId(user.getIdUser());
    }

    public Optional<Commentaire> findById(Integer id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return repository.findById(id);
    }

    public Integer create(String description, String image, boolean visibility, Publication publication, User user) {
        return create(description, image, visibility, null, publication, user);
    }

    public Integer create(String description, String image, boolean visibility, LocalDateTime createdAt, Publication publication, User user) {
        ValidationResult validation = validateCreate(description, publication, user);
        if (!validation.valid) {
            System.out.println("CommentaireService.create validation failed: " + validation.message);
            return -1;
        }

        Commentaire commentaire = new Commentaire();
        commentaire.setDescriptionCommentaire(description);
        commentaire.setImageCommentaire(image);
        commentaire.setVisibility(visibility);
        commentaire.setPublication(publication);
        commentaire.setUser(user);
        LocalDateTime now = LocalDateTime.now();
        commentaire.setCreatedAt(createdAt != null ? createdAt : now);
        commentaire.setUpdatedAt(now);

        return repository.create(commentaire);
    }

    public boolean update(Integer id, String description, String image, Boolean visibility) {
        return update(id, description, image, visibility, null);
    }

    public boolean update(Integer id, String description, String image, Boolean visibility, LocalDateTime createdAt) {
        if (id == null || id <= 0) {
            return false;
        }

        Optional<Commentaire> existing = repository.findById(id);
        if (existing.isEmpty()) {
            return false;
        }

        Commentaire commentaire = existing.get();

        if (description != null && !description.isBlank()) {
            commentaire.setDescriptionCommentaire(description);
        }

        if (image != null) {
            commentaire.setImageCommentaire(image);
        }

        if (visibility != null) {
            commentaire.setVisibility(visibility);
        }

        if (createdAt != null) {
            commentaire.setCreatedAt(createdAt);
        }

        commentaire.setUpdatedAt(LocalDateTime.now());
        return repository.update(commentaire);
    }

    public boolean delete(Integer id) {
        if (id == null || id <= 0) {
            return false;
        }
        return repository.delete(id);
    }

    private ValidationResult validateCreate(String description, Publication publication, User user) {
        if (description == null || description.isBlank()) {
            return ValidationResult.invalid("description_commentaire is required");
        }
        if (description.length() > 255) {
            return ValidationResult.invalid("description_commentaire must not exceed 255 characters");
        }

        if (publication == null || publication.getIdPublication() == null || publication.getIdPublication() <= 0) {
            return ValidationResult.invalid("publication is required");
        }

        if (user == null || user.getIdUser() == null || user.getIdUser() <= 0) {
            return ValidationResult.invalid("user is required");
        }

        return ValidationResult.valid();
    }

    public CommentaireRepository getRepository() {
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