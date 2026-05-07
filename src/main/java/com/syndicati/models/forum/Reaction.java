package com.syndicati.models.forum;

import com.syndicati.models.user.User;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Forum reaction entity aligned with Horizon web schema.
 */
public class Reaction {

    public static final Set<String> KINDS = Set.of("Like", "Dislike", "Emoji", "Bookmark", "Report");

    private Integer idReaction;
    private User user;
    private Publication publication;
    private Commentaire commentaire;
    private String kind;
    private String emoji;
    private String reportReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Integer getIdReaction() {
        return idReaction;
    }

    public void setIdReaction(Integer idReaction) {
        this.idReaction = idReaction;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    public Commentaire getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(Commentaire commentaire) {
        this.commentaire = commentaire;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getReportReason() {
        return reportReason;
    }

    public void setReportReason(String reportReason) {
        this.reportReason = reportReason;
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
}
