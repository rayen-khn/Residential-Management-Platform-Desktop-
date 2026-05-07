package com.syndicati.models.forum;

import com.syndicati.models.user.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Forum publication entity aligned with Horizon web schema.
 */
public class Publication {

    private static final int TITLE_MIN_LENGTH = 5;
    private static final int TITLE_MAX_LENGTH = 255;
    private static final int DESCRIPTION_MIN_LENGTH = 10;
    private static final int DESCRIPTION_MAX_LENGTH = 255;

    public static final Set<String> CATEGORIES = new HashSet<>(Arrays.asList(
        "Announcement",
        "Suggestion",
        "Jeux Video",
        "Informatique",
        "Nouveauté",
        "Discussion General",
        "Culture",
        "Sport"
    ));

    private Integer idPublication;
    private String titrePub;
    private String descriptionPub;
    private LocalDateTime dateCreationPub;
    private String categoriePub;
    private String imagePub;
    private User user;

    public Integer getIdPublication() {
        return idPublication;
    }

    public void setIdPublication(Integer idPublication) {
        this.idPublication = idPublication;
    }

    public String getTitrePub() {
        return titrePub;
    }

    public void setTitrePub(String titrePub) {
        this.titrePub = titrePub;
    }

    public String getDescriptionPub() {
        return descriptionPub;
    }

    public void setDescriptionPub(String descriptionPub) {
        this.descriptionPub = descriptionPub;
    }

    public LocalDateTime getDateCreationPub() {
        return dateCreationPub;
    }

    public void setDateCreationPub(LocalDateTime dateCreationPub) {
        this.dateCreationPub = dateCreationPub;
    }

    public String getCategoriePub() {
        return categoriePub;
    }

    public void setCategoriePub(String categoriePub) {
        this.categoriePub = categoriePub;
    }

    public String getImagePub() {
        return imagePub;
    }

    public void setImagePub(String imagePub) {
        this.imagePub = imagePub;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<String> validateForCreate() {
        List<String> errors = new ArrayList<>();

        if (!isTitleValid(titrePub)) {
            errors.add("Title must start with a letter, cannot be only numbers, and must be between " + TITLE_MIN_LENGTH + " and " + TITLE_MAX_LENGTH + " characters.");
        }

        if (descriptionPub == null || descriptionPub.trim().length() < DESCRIPTION_MIN_LENGTH || descriptionPub.trim().length() > DESCRIPTION_MAX_LENGTH) {
            errors.add("Description must be between " + DESCRIPTION_MIN_LENGTH + " and " + DESCRIPTION_MAX_LENGTH + " characters.");
        }

        if (categoriePub == null || !CATEGORIES.contains(categoriePub)) {
            errors.add("Category is invalid.");
        }

        if (user == null || user.getIdUser() == null || user.getIdUser() <= 0) {
            errors.add("User is required.");
        }

        if (dateCreationPub == null) {
            errors.add("Creation date is required.");
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