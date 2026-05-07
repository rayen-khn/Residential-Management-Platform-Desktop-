package com.syndicati.models.evenement;

import com.syndicati.models.user.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Evenement (Event) entity aligned with Horizon web schema.
 */
public class Evenement {

    private static final int TITLE_MIN_LENGTH = 5;
    private static final int TITLE_MAX_LENGTH = 255;
    private static final int DESCRIPTION_MIN_LENGTH = 10;
    private static final int DESCRIPTION_MAX_LENGTH = 255;
    private static final int LOCATION_MIN_LENGTH = 3;
    private static final int LOCATION_MAX_LENGTH = 255;

    public static final Set<String> STATUTS = new HashSet<>(Arrays.asList(
        "planifie", "en_cours", "termine", "annule"
    ));

    public static final Set<String> TYPES = new HashSet<>(Arrays.asList(
        "reunion", "social", "formation", "maintenance", "culturel", "sportif"
    ));

    private Integer idEvent;
    private String titreEvent;
    private String descriptionEvent;
    private LocalDateTime dateEvent;
    private String lieuEvent;
    private Integer nbPlaces;
    private Integer nbRestants;
    private String statutEvent;
    private String imageEvent;
    private String typeEvent;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;
    private User user;

    public Integer getIdEvent() {
        return idEvent;
    }

    public void setIdEvent(Integer idEvent) {
        this.idEvent = idEvent;
    }

    public String getTitreEvent() {
        return titreEvent;
    }

    public void setTitreEvent(String titreEvent) {
        this.titreEvent = titreEvent;
    }

    public String getDescriptionEvent() {
        return descriptionEvent;
    }

    public void setDescriptionEvent(String descriptionEvent) {
        this.descriptionEvent = descriptionEvent;
    }

    public LocalDateTime getDateEvent() {
        return dateEvent;
    }

    public void setDateEvent(LocalDateTime dateEvent) {
        this.dateEvent = dateEvent;
    }

    public String getLieuEvent() {
        return lieuEvent;
    }

    public void setLieuEvent(String lieuEvent) {
        this.lieuEvent = lieuEvent;
    }

    public Integer getNbPlaces() {
        return nbPlaces;
    }

    public void setNbPlaces(Integer nbPlaces) {
        this.nbPlaces = nbPlaces;
    }

    public Integer getNbRestants() {
        return nbRestants;
    }

    public void setNbRestants(Integer nbRestants) {
        this.nbRestants = nbRestants;
    }

    public String getStatutEvent() {
        return statutEvent;
    }

    public void setStatutEvent(String statutEvent) {
        this.statutEvent = statutEvent;
    }

    public String getImageEvent() {
        return imageEvent;
    }

    public void setImageEvent(String imageEvent) {
        this.imageEvent = imageEvent;
    }

    public String getTypeEvent() {
        return typeEvent;
    }

    public void setTypeEvent(String typeEvent) {
        this.typeEvent = typeEvent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(LocalDateTime editedAt) {
        this.editedAt = editedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<String> validateForCreate() {
        List<String> errors = new ArrayList<>();

        if (!isTitleValid(titreEvent)) {
            errors.add("Title must start with a letter, cannot be only numbers, and must be between " + TITLE_MIN_LENGTH + " and " + TITLE_MAX_LENGTH + " characters.");
        }

        if (descriptionEvent == null || descriptionEvent.trim().length() < DESCRIPTION_MIN_LENGTH || descriptionEvent.trim().length() > DESCRIPTION_MAX_LENGTH) {
            errors.add("Description must be between " + DESCRIPTION_MIN_LENGTH + " and " + DESCRIPTION_MAX_LENGTH + " characters.");
        }

        if (lieuEvent == null || lieuEvent.trim().length() < LOCATION_MIN_LENGTH || lieuEvent.trim().length() > LOCATION_MAX_LENGTH) {
            errors.add("Location must be between " + LOCATION_MIN_LENGTH + " and " + LOCATION_MAX_LENGTH + " characters.");
        }

        if (dateEvent == null) {
            errors.add("Event date is required.");
        }

        if (nbPlaces == null || nbPlaces <= 0) {
            errors.add("Number of places must be greater than 0.");
        }

        if (nbRestants != null && nbRestants < 0) {
            errors.add("Remaining places cannot be negative.");
        }

        if (nbPlaces != null && nbRestants != null && nbRestants > nbPlaces) {
            errors.add("Remaining places cannot exceed total places.");
        }

        if (typeEvent == null || !TYPES.contains(typeEvent)) {
            errors.add("Event type is invalid.");
        }

        if (statutEvent != null && !statutEvent.isBlank() && !STATUTS.contains(statutEvent)) {
            errors.add("Event status is invalid.");
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

    @Override
    public String toString() {
        return "Evenement{" +
                "idEvent=" + idEvent +
                ", titreEvent='" + titreEvent + '\'' +
                ", typeEvent='" + typeEvent + '\'' +
                ", dateEvent=" + dateEvent +
                ", lieuEvent='" + lieuEvent + '\'' +
                '}';
    }
}
