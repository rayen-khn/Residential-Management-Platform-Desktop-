package com.syndicati.models.entities;

import java.time.LocalDateTime;

/**
 * Entity for Reactions (Like, Dislike, Bookmark, Report, Emoji)
 * Works for both Publications and Comments.
 */
public class PubCommentReaction {
    private Integer idReaction;
    private Integer userId;
    private Integer publicationId;
    private Integer commentaireId;
    private String kind; // LIKE, DISLIKE, BOOKMARK, REPORT, EMOJI
    private String emoji;
    private String reportReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PubCommentReaction() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constants for Kind
    public static final String KIND_LIKE = "Like";
    public static final String KIND_DISLIKE = "Dislike";
    public static final String KIND_BOOKMARK = "Bookmark";
    public static final String KIND_REPORT = "Report";
    public static final String KIND_EMOJI = "Emoji";

    public Integer getIdReaction() { return idReaction; }
    public void setIdReaction(Integer idReaction) { this.idReaction = idReaction; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getPublicationId() { return publicationId; }
    public void setPublicationId(Integer publicationId) { this.publicationId = publicationId; }

    public Integer getCommentaireId() { return commentaireId; }
    public void setCommentaireId(Integer commentaireId) { this.commentaireId = commentaireId; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public String getReportReason() { return reportReason; }
    public void setReportReason(String reportReason) { this.reportReason = reportReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
