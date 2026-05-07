package com.syndicati.models.syndicat;

import com.syndicati.models.user.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reponse entity aligned exactly with Horizon database schema.
 * Maps to reponses table.
 */
public class Reponse {

    private static final int TITLE_MIN_LENGTH = 5;
    private static final int TITLE_MAX_LENGTH = 255;
    private static final int MESSAGE_MIN_LENGTH = 2;
    private static final int MESSAGE_MAX_LENGTH = 255;

    private Integer idReponses;
    private String titreReponse;
    private String messageReponse;
    private String imageReponse;
    private Reclamation reclamation;
    private User user;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Reponse() {
    }

    public Reponse(String titre, String message, Reclamation reclamation, User user) {
        this.titreReponse = titre;
        this.messageReponse = message;
        this.reclamation = reclamation;
        this.user = user;
    }

    // Getters and Setters
    public Integer getIdReponses() {
        return idReponses;
    }

    public void setIdReponses(Integer idReponses) {
        this.idReponses = idReponses;
    }

    public String getTitreReponse() {
        return titreReponse;
    }

    public void setTitreReponse(String titreReponse) {
        this.titreReponse = titreReponse;
    }

    public String getMessageReponse() {
        return messageReponse;
    }

    public void setMessageReponse(String messageReponse) {
        this.messageReponse = messageReponse;
    }

    public String getImageReponse() {
        return imageReponse;
    }

    public void setImageReponse(String imageReponse) {
        this.imageReponse = imageReponse;
    }

    public Reclamation getReclamation() {
        return reclamation;
    }

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public List<String> validateForCreate() {
        List<String> errors = new ArrayList<>();

        if (!isTitleValid(titreReponse)) {
            errors.add("Title must start with a letter and be between " + TITLE_MIN_LENGTH + " and " + TITLE_MAX_LENGTH + " characters.");
        }

        if (!isMessageValid(messageReponse)) {
            errors.add("Message must start with a letter and be between " + MESSAGE_MIN_LENGTH + " and " + MESSAGE_MAX_LENGTH + " characters.");
        }

        if (reclamation == null || reclamation.getIdReclamations() == null || reclamation.getIdReclamations() <= 0) {
            errors.add("Reclamation is required.");
        }

        if (user == null || user.getIdUser() == null || user.getIdUser() <= 0) {
            errors.add("User is required.");
        }

        return errors;
    }

    public List<String> validateForUpdate() {
        return validateForCreate();
    }

    private boolean isTitleValid(String value) {
        if (value == null) {
            return false;
        }
        String cleaned = value.trim();
        if (cleaned.length() < TITLE_MIN_LENGTH || cleaned.length() > TITLE_MAX_LENGTH) {
            return false;
        }
        return !cleaned.isEmpty() && Character.isLetter(cleaned.charAt(0)) && !cleaned.matches("\\d+");
    }

    private boolean isMessageValid(String value) {
        if (value == null) {
            return false;
        }
        String cleaned = value.trim();
        if (cleaned.length() < MESSAGE_MIN_LENGTH || cleaned.length() > MESSAGE_MAX_LENGTH) {
            return false;
        }
        return !cleaned.isEmpty() && Character.isLetter(cleaned.charAt(0));
    }
}
