package com.syndicati.controllers.forum;

import com.syndicati.models.forum.Commentaire;
import com.syndicati.models.forum.Publication;
import com.syndicati.models.forum.Reaction;
import com.syndicati.models.user.User;
import com.syndicati.services.forum.ReactionService;
import com.syndicati.services.forum.ReactionService.ReactionActionResult;
import com.syndicati.services.forum.ReactionService.ReactionStatus;
import java.util.List;

/**
 * Controller facade for forum reactions.
 */
public class ReactionController {

    private final ReactionService reactionService;

    public ReactionController() {
        this.reactionService = new ReactionService();
    }

    public ReactionActionResult publicationToggle(Publication publication, User user, String kind) {
        Integer publicationId = publication == null ? null : publication.getIdPublication();
        return reactionService.togglePublicationReaction(publicationId, user, kind);
    }

    public ReactionActionResult publicationEmoji(Publication publication, User user, String emoji) {
        Integer publicationId = publication == null ? null : publication.getIdPublication();
        return reactionService.reactPublicationEmoji(publicationId, user, emoji);
    }

    public ReactionActionResult publicationReport(Publication publication, User user, String reason) {
        Integer publicationId = publication == null ? null : publication.getIdPublication();
        return reactionService.reportPublication(publicationId, user, reason);
    }

    public ReactionStatus publicationStatus(Publication publication, User user) {
        Integer publicationId = publication == null ? null : publication.getIdPublication();
        return reactionService.publicationStatus(publicationId, user);
    }

    public ReactionActionResult commentToggle(Commentaire commentaire, User user, String kind) {
        Integer commentId = commentaire == null ? null : commentaire.getIdCommentaire();
        return reactionService.toggleCommentReaction(commentId, user, kind);
    }

    public ReactionActionResult commentEmoji(Commentaire commentaire, User user, String emoji) {
        Integer commentId = commentaire == null ? null : commentaire.getIdCommentaire();
        return reactionService.reactCommentEmoji(commentId, user, emoji);
    }

    public ReactionActionResult commentReport(Commentaire commentaire, User user, String reason) {
        Integer commentId = commentaire == null ? null : commentaire.getIdCommentaire();
        return reactionService.reportComment(commentId, user, reason);
    }

    public ReactionStatus commentStatus(Commentaire commentaire, User user) {
        Integer commentId = commentaire == null ? null : commentaire.getIdCommentaire();
        return reactionService.commentStatus(commentId, user);
    }

    public List<Reaction> reactions() {
        return reactionService.reactions();
    }

    public ReactionService getReactionService() {
        return reactionService;
    }
}
