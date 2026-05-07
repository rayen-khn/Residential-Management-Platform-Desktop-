package com.syndicati.models.syndicat;

import com.syndicati.models.user.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reclamation entity aligned exactly with Horizon database schema.
 * Maps to reclamations table.
 */
public class Reclamation {

    private static final int TITLE_MIN_LENGTH = 5;
    private static final int TITLE_MAX_LENGTH = 255;
    private static final int DESCRIPTION_MIN_LENGTH = 10;
    private static final int DESCRIPTION_MAX_LENGTH = 255;

    public static final Set<String> STATUTS = new HashSet<>(Arrays.asList(
        "active", "en_attente", "refuse", "termine"
    ));

    private Integer idReclamations;
    private String titreReclamations;
    private String descReclamation;
    private LocalDateTime dateReclamation;
    private String statutReclamation = "en_attente";
    private String imageReclamation;
    private User user;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Reponse> reponses = new ArrayList<>();

    // Constructors
    public Reclamation() {
    }

    public Reclamation(String titre, String desc, LocalDateTime date, User user) {
        this.titreReclamations = titre;
        this.descReclamation = desc;
        this.dateReclamation = date;
        this.user = user;
        this.statutReclamation = "en_attente";
    }

    // Getters and Setters
    public Integer getIdReclamations() {
        return idReclamations;
    }

    public void setIdReclamations(Integer idReclamations) {
        this.idReclamations = idReclamations;
    }

    public String getTitreReclamations() {
        return titreReclamations;
    }

    public void setTitreReclamations(String titreReclamations) {
        this.titreReclamations = titreReclamations;
    }

    public String getDescReclamation() {
        return descReclamation;
    }

    public void setDescReclamation(String descReclamation) {
        this.descReclamation = descReclamation;
    }

    public LocalDateTime getDateReclamation() {
        return dateReclamation;
    }

    public void setDateReclamation(LocalDateTime dateReclamation) {
        this.dateReclamation = dateReclamation;
    }

    public String getStatutReclamation() {
        return statutReclamation;
    }

    public void setStatutReclamation(String statutReclamation) {
        this.statutReclamation = statutReclamation;
    }

    public String getImageReclamation() {
        return imageReclamation;
    }

    public void setImageReclamation(String imageReclamation) {
        this.imageReclamation = imageReclamation;
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

    public List<Reponse> getReponses() {
        if (reponses == null) {
            reponses = new ArrayList<>();
        }
        return reponses;
    }

    public void setReponses(List<Reponse> reponses) {
        this.reponses = reponses;
    }

    public void addReponse(Reponse reponse) {
        if (reponses == null) {
            reponses = new ArrayList<>();
        }
        reponses.add(reponse);
        reponse.setReclamation(this);
    }

    public List<String> validateForCreate() {
        List<String> errors = new ArrayList<>();

        if (!isTitleValid(titreReclamations)) {
            errors.add("Title must start with a letter, cannot be only numbers, and must be between " + TITLE_MIN_LENGTH + " and " + TITLE_MAX_LENGTH + " characters.");
        }

        if (descReclamation == null || descReclamation.trim().length() < DESCRIPTION_MIN_LENGTH || descReclamation.trim().length() > DESCRIPTION_MAX_LENGTH) {
            errors.add("Description must be between " + DESCRIPTION_MIN_LENGTH + " and " + DESCRIPTION_MAX_LENGTH + " characters.");
        }

        if (dateReclamation == null) {
            errors.add("Reclamation date is required.");
        }

        if (statutReclamation != null && !statutReclamation.isBlank() && !STATUTS.contains(statutReclamation)) {
            errors.add("Status is invalid.");
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
        return Character.isLetter(cleaned.charAt(0)) && !cleaned.matches("\\d+");
    }
}
