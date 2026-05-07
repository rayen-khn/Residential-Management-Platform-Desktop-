package com.syndicati.models.forum;

import com.syndicati.models.user.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Forum comment entity aligned with Horizon web schema.
 */
public class Commentaire {

    private static final int DESCRIPTION_MIN_LENGTH = 1;
    private static final int DESCRIPTION_MAX_LENGTH = 255;

    private Integer idCommentaire;
    private String descriptionCommentaire;
    private String imageCommentaire;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean visibility = true;
    private Publication publication;
    private User user;

    public Integer getIdCommentaire() {
        return idCommentaire;
    }

    public void setIdCommentaire(Integer idCommentaire) {
        this.idCommentaire = idCommentaire;
    }

    public String getDescriptionCommentaire() {
        return descriptionCommentaire;
    }

    public void setDescriptionCommentaire(String descriptionCommentaire) {
        this.descriptionCommentaire = descriptionCommentaire;
    }

    public String getImageCommentaire() {
        return imageCommentaire;
    }

    public void setImageCommentaire(String imageCommentaire) {
        this.imageCommentaire = imageCommentaire;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isVisibility() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }

    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<String> validateForCreate() {
        List<String> errors = new ArrayList<>();

        if (descriptionCommentaire == null || descriptionCommentaire.trim().length() < DESCRIPTION_MIN_LENGTH || descriptionCommentaire.trim().length() > DESCRIPTION_MAX_LENGTH) {
            errors.add("Comment text must be between " + DESCRIPTION_MIN_LENGTH + " and " + DESCRIPTION_MAX_LENGTH + " characters.");
        }

        if (publication == null || publication.getIdPublication() == null || publication.getIdPublication() <= 0) {
            errors.add("Publication is required.");
        }

        if (user == null || user.getIdUser() == null || user.getIdUser() <= 0) {
            errors.add("User is required.");
        }

        return errors;
    }

    public List<String> validateForUpdate() {
        return validateForCreate();
    }
}