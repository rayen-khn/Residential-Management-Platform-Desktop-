package com.syndicati.models.evenement;

import com.syndicati.models.user.User;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Participation entity aligned with Horizon web schema.
 */
public class Participation {

    public static final Set<String> STATUTS = new HashSet<>(Arrays.asList(
        "confirme", "en_attente", "refuse", "annule"
    ));

    private Integer idParticipation;
    private Evenement evenement;
    private User user;
    private LocalDateTime dateParticipation;
    private String statutParticipation;
    private Integer nbAccompagnants;
    private String commentaireParticipation;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;

    public Integer getIdParticipation() {
        return idParticipation;
    }

    public void setIdParticipation(Integer idParticipation) {
        this.idParticipation = idParticipation;
    }

    public Evenement getEvenement() {
        return evenement;
    }

    public void setEvenement(Evenement evenement) {
        this.evenement = evenement;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getDateParticipation() {
        return dateParticipation;
    }

    public void setDateParticipation(LocalDateTime dateParticipation) {
        this.dateParticipation = dateParticipation;
    }

    public String getStatutParticipation() {
        return statutParticipation;
    }

    public void setStatutParticipation(String statutParticipation) {
        this.statutParticipation = statutParticipation;
    }

    public Integer getNbAccompagnants() {
        return nbAccompagnants;
    }

    public void setNbAccompagnants(Integer nbAccompagnants) {
        this.nbAccompagnants = nbAccompagnants;
    }

    public String getCommentaireParticipation() {
        return commentaireParticipation;
    }

    public void setCommentaireParticipation(String commentaireParticipation) {
        this.commentaireParticipation = commentaireParticipation;
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

    @Override
    public String toString() {
        return "Participation{" +
                "idParticipation=" + idParticipation +
                ", statut='" + statutParticipation + '\'' +
                ", nbAccompagnants=" + nbAccompagnants +
                '}';
    }
}
