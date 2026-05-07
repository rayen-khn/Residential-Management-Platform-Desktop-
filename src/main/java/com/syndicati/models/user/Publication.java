package com.syndicati.models.entities;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Publication entity aligned with the web project's publication table naming.
 */
public class Publication {

    public static final List<String> CATEGORIES = Arrays.asList(
        "Announcement", "Suggestion", "Jeux Video", "Informatique", "Nouveauté", "Discussion General", "Culture", "Sport"
    );

    private Integer id;
    private String titrePub;
    private String descriptionPub;
    private LocalDateTime dateCreationPub;
    private String categoriePub;
    private String imagePub;
    private Integer userId;
    private String authorFirstName;
    private String authorLastName;
    private String authorAvatar;

    public Publication() {
        this.dateCreationPub = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getAuthorFirstName() {
        return authorFirstName;
    }

    public void setAuthorFirstName(String authorFirstName) {
        this.authorFirstName = authorFirstName;
    }

    public String getAuthorLastName() {
        return authorLastName;
    }

    public void setAuthorLastName(String authorLastName) {
        this.authorLastName = authorLastName;
    }

    public String getAuthorAvatar() {
        return authorAvatar;
    }

    public void setAuthorAvatar(String authorAvatar) {
        this.authorAvatar = authorAvatar;
    }

    public String getAuthorFullName() {
        if (authorFirstName == null && authorLastName == null) return "User #" + userId;
        return (authorFirstName != null ? authorFirstName : "").trim() + " " + (authorLastName != null ? authorLastName : "").trim();
    }

    public String getAuthorInitials() {
        StringBuilder sb = new StringBuilder();
        if (authorFirstName != null && !authorFirstName.trim().isEmpty()) {
            sb.append(authorFirstName.trim().substring(0, 1).toUpperCase());
        }
        if (authorLastName != null && !authorLastName.trim().isEmpty()) {
            sb.append(authorLastName.trim().substring(0, 1).toUpperCase());
        }
        return sb.length() > 0 ? sb.toString() : "?";
    }

    // Basic validation logic
    public boolean isValid() {
        return titrePub != null && !titrePub.trim().isEmpty() &&
               Character.isLetter(titrePub.trim().charAt(0)) &&
               descriptionPub != null && descriptionPub.length() >= 10 &&
               categoriePub != null && CATEGORIES.contains(categoriePub);
    }
}
