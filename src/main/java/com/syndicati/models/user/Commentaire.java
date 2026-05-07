package com.syndicati.models.entities;

import java.time.LocalDateTime;

/**
 * Entity for Publication Comments based on the DB schema.
 */
public class Commentaire {
    private Integer idCommentaire;
    private String descriptionCommentaire;
    private String imageCommentaire;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer visibility;
    private Integer idPub;
    private Integer idUser;

    // Author metadata for easy UI rendering
    private String authorFirstName;
    private String authorLastName;
    private String authorAvatar;

    public Commentaire() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.visibility = 1; // Default visible
    }

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

    public Integer getVisibility() {
        return visibility;
    }

    public void setVisibility(Integer visibility) {
        this.visibility = visibility;
    }

    public Integer getIdPub() {
        return idPub;
    }

    public void setIdPub(Integer idPub) {
        this.idPub = idPub;
    }

    public Integer getIdUser() {
        return idUser;
    }

    public void setIdUser(Integer idUser) {
        this.idUser = idUser;
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
        if (authorFirstName == null && authorLastName == null) return "User #" + idUser;
        return (authorFirstName != null ? authorFirstName : "").trim() + " " + (authorLastName != null ? authorLastName : "").trim();
    }

    public String getAuthorInitials() {
        String fn = (authorFirstName != null && !authorFirstName.isEmpty()) ? authorFirstName.substring(0, 1).toUpperCase() : "";
        String ln = (authorLastName != null && !authorLastName.isEmpty()) ? authorLastName.substring(0, 1).toUpperCase() : "";
        String initials = fn + ln;
        return initials.isEmpty() ? "?" : initials;
    }

    /**
     * Input validation (Contrôle de saisie)
     * Description should not be empty and should not start with symbols like . , ? ; etc.
     */
    public boolean isValid() {
        if (descriptionCommentaire == null || descriptionCommentaire.trim().isEmpty()) {
            return false;
        }
        char firstChar = descriptionCommentaire.trim().charAt(0);
        // Ensure it starts with a letter or digit
        return Character.isLetterOrDigit(firstChar);
    }
}
