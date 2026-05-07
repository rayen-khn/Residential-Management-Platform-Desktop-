package com.syndicati.services.forum;

import com.syndicati.models.forum.Commentaire;
import com.syndicati.models.forum.Publication;
import com.syndicati.models.forum.Reaction;
import com.syndicati.models.forum.data.CommentaireRepository;
import com.syndicati.models.forum.data.PublicationRepository;
import com.syndicati.models.forum.data.ReactionRepository;
import com.syndicati.models.user.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Forum reaction business logic aligned with Horizon reaction rules.
 */
public class ReactionService {

    private static final List<String> LIKE_DISLIKE = List.of("Like", "Dislike");

    private final ReactionRepository reactionRepository;
    private final PublicationRepository publicationRepository;
    private final CommentaireRepository commentaireRepository;

    public ReactionService() {
        this.reactionRepository = new ReactionRepository();
        this.publicationRepository = new PublicationRepository();
        this.commentaireRepository = new CommentaireRepository();
    }

    public ReactionActionResult togglePublicationReaction(Integer publicationId, User user, String kind) {
        if (!isValidUser(user)) {
            return ReactionActionResult.failure("You must be logged in to react.");
        }
        if (!isValidId(publicationId)) {
            return ReactionActionResult.failure("Invalid publication.");
        }
        if (!List.of("Like", "Dislike", "Bookmark").contains(kind)) {
            return ReactionActionResult.failure("Invalid reaction kind.");
        }

        Optional<Publication> publication = publicationRepository.findById(publicationId);
        if (publication.isEmpty()) {
            return ReactionActionResult.failure("Publication not found.");
        }

        Optional<Reaction> existing = reactionRepository.findOneByUserAndPublicationAndKind(user.getIdUser(), publicationId, kind);
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get().getIdReaction());
            return publicationResult(publicationId, user.getIdUser(), "removed", null, "Reaction removed.");
        }

        if (LIKE_DISLIKE.contains(kind)) {
            String opposite = "Like".equals(kind) ? "Dislike" : "Like";
            reactionRepository.findOneByUserAndPublicationAndKind(user.getIdUser(), publicationId, opposite)
                .ifPresent(op -> reactionRepository.delete(op.getIdReaction()));

            reactionRepository.findOneByUserAndPublicationAndKind(user.getIdUser(), publicationId, "Emoji")
                .ifPresent(emoji -> reactionRepository.delete(emoji.getIdReaction()));
        }

        Reaction reaction = new Reaction();
        reaction.setUser(user);
        reaction.setPublication(publication.get());
        reaction.setKind(kind);
        reaction.setCreatedAt(LocalDateTime.now());
        reaction.setUpdatedAt(LocalDateTime.now());
        reactionRepository.create(reaction);

        return publicationResult(publicationId, user.getIdUser(), "added", null, "Reaction added.");
    }

    public ReactionActionResult reactPublicationEmoji(Integer publicationId, User user, String emoji) {
        if (!isValidUser(user)) {
            return ReactionActionResult.failure("You must be logged in to react.");
        }
        if (!isValidId(publicationId) || isBlank(emoji)) {
            return ReactionActionResult.failure("Invalid emoji reaction request.");
        }

        Optional<Publication> publication = publicationRepository.findById(publicationId);
        if (publication.isEmpty()) {
            return ReactionActionResult.failure("Publication not found.");
        }

        List<Reaction> conflicts = reactionRepository.findByUserAndPublicationAndKinds(user.getIdUser(), publicationId, LIKE_DISLIKE);
        deleteMany(conflicts);

        Optional<Reaction> existing = reactionRepository.findOneByUserAndPublicationAndKind(user.getIdUser(), publicationId, "Emoji");
        if (existing.isPresent()) {
            Reaction ex = existing.get();
            if (emoji.equals(ex.getEmoji())) {
                reactionRepository.delete(ex.getIdReaction());
                return publicationResult(publicationId, user.getIdUser(), "removed", null, "Emoji reaction removed.");
            }
            ex.setEmoji(emoji);
            ex.setUpdatedAt(LocalDateTime.now());
            reactionRepository.update(ex);
            return publicationResult(publicationId, user.getIdUser(), "added", emoji, "Emoji reaction updated.");
        }

        Reaction reaction = new Reaction();
        reaction.setUser(user);
        reaction.setPublication(publication.get());
        reaction.setKind("Emoji");
        reaction.setEmoji(emoji);
        reaction.setCreatedAt(LocalDateTime.now());
        reaction.setUpdatedAt(LocalDateTime.now());
        reactionRepository.create(reaction);

        return publicationResult(publicationId, user.getIdUser(), "added", emoji, "Emoji reaction added.");
    }

    public ReactionActionResult reportPublication(Integer publicationId, User user, String reason) {
        if (!isValidUser(user)) {
            return ReactionActionResult.failure("You must be logged in to report.");
        }
        if (!isValidId(publicationId) || isBlank(reason)) {
            return ReactionActionResult.failure("Invalid report request.");
        }

        Optional<Publication> publication = publicationRepository.findById(publicationId);
        if (publication.isEmpty()) {
            return ReactionActionResult.failure("Publication not found.");
        }

        Reaction reaction = new Reaction();
        reaction.setUser(user);
        reaction.setPublication(publication.get());
        reaction.setKind("Report");
        reaction.setReportReason(reason);
        reaction.setCreatedAt(LocalDateTime.now());
        reaction.setUpdatedAt(LocalDateTime.now());
        reactionRepository.create(reaction);

        return publicationResult(publicationId, user.getIdUser(), "added", null, "Report submitted. Thank you.");
    }

    public ReactionStatus publicationStatus(Integer publicationId, User user) {
        if (!isValidUser(user) || !isValidId(publicationId)) {
            return ReactionStatus.empty();
        }

        List<Reaction> reactions = reactionRepository.findByUserAndPublication(user.getIdUser(), publicationId);
        return buildStatus(reactions, publicationId, null);
    }

    public ReactionActionResult toggleCommentReaction(Integer commentId, User user, String kind) {
        if (!isValidUser(user)) {
            return ReactionActionResult.failure("You must be logged in to react.");
        }
        if (!isValidId(commentId)) {
            return ReactionActionResult.failure("Invalid comment.");
        }
        if (!List.of("Like", "Dislike").contains(kind)) {
            return ReactionActionResult.failure("Invalid reaction kind.");
        }

        Optional<Commentaire> commentaire = commentaireRepository.findById(commentId);
        if (commentaire.isEmpty()) {
            return ReactionActionResult.failure("Comment not found.");
        }

        Optional<Reaction> existing = reactionRepository.findOneByUserAndCommentAndKind(user.getIdUser(), commentId, kind);
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get().getIdReaction());
            return commentResult(commentId, user.getIdUser(), "removed", null, "Reaction removed.");
        }

        String opposite = "Like".equals(kind) ? "Dislike" : "Like";
        reactionRepository.findOneByUserAndCommentAndKind(user.getIdUser(), commentId, opposite)
            .ifPresent(op -> reactionRepository.delete(op.getIdReaction()));

        reactionRepository.findOneByUserAndCommentAndKind(user.getIdUser(), commentId, "Emoji")
            .ifPresent(emoji -> reactionRepository.delete(emoji.getIdReaction()));

        Reaction reaction = new Reaction();
        reaction.setUser(user);
        reaction.setCommentaire(commentaire.get());
        reaction.setKind(kind);
        reaction.setCreatedAt(LocalDateTime.now());
        reaction.setUpdatedAt(LocalDateTime.now());
        reactionRepository.create(reaction);

        return commentResult(commentId, user.getIdUser(), "added", null, "Reaction added.");
    }

    public ReactionActionResult reactCommentEmoji(Integer commentId, User user, String emoji) {
        if (!isValidUser(user)) {
            return ReactionActionResult.failure("You must be logged in to react.");
        }
        if (!isValidId(commentId) || isBlank(emoji)) {
            return ReactionActionResult.failure("Invalid emoji reaction request.");
        }

        Optional<Commentaire> commentaire = commentaireRepository.findById(commentId);
        if (commentaire.isEmpty()) {
            return ReactionActionResult.failure("Comment not found.");
        }

        List<Reaction> conflicts = reactionRepository.findByUserAndCommentAndKinds(user.getIdUser(), commentId, LIKE_DISLIKE);
        deleteMany(conflicts);

        Optional<Reaction> existing = reactionRepository.findOneByUserAndCommentAndKind(user.getIdUser(), commentId, "Emoji");
        if (existing.isPresent()) {
            Reaction ex = existing.get();
            if (emoji.equals(ex.getEmoji())) {
                reactionRepository.delete(ex.getIdReaction());
                return commentResult(commentId, user.getIdUser(), "removed", null, "Emoji reaction removed.");
            }
            ex.setEmoji(emoji);
            ex.setUpdatedAt(LocalDateTime.now());
            reactionRepository.update(ex);
            return commentResult(commentId, user.getIdUser(), "added", emoji, "Emoji reaction updated.");
        }

        Reaction reaction = new Reaction();
        reaction.setUser(user);
        reaction.setCommentaire(commentaire.get());
        reaction.setKind("Emoji");
        reaction.setEmoji(emoji);
        reaction.setCreatedAt(LocalDateTime.now());
        reaction.setUpdatedAt(LocalDateTime.now());
        reactionRepository.create(reaction);

        return commentResult(commentId, user.getIdUser(), "added", emoji, "Emoji reaction added.");
    }

    public ReactionActionResult reportComment(Integer commentId, User user, String reason) {
        if (!isValidUser(user)) {
            return ReactionActionResult.failure("You must be logged in to report.");
        }
        if (!isValidId(commentId) || isBlank(reason)) {
            return ReactionActionResult.failure("Invalid report request.");
        }

        Optional<Commentaire> commentaire = commentaireRepository.findById(commentId);
        if (commentaire.isEmpty()) {
            return ReactionActionResult.failure("Comment not found.");
        }

        Reaction reaction = new Reaction();
        reaction.setUser(user);
        reaction.setCommentaire(commentaire.get());
        reaction.setKind("Report");
        reaction.setReportReason(reason);
        reaction.setCreatedAt(LocalDateTime.now());
        reaction.setUpdatedAt(LocalDateTime.now());
        reactionRepository.create(reaction);

        return commentResult(commentId, user.getIdUser(), "added", null, "Comment reported.");
    }

    public ReactionStatus commentStatus(Integer commentId, User user) {
        if (!isValidUser(user) || !isValidId(commentId)) {
            return ReactionStatus.empty();
        }

        List<Reaction> reactions = reactionRepository.findByUserAndComment(user.getIdUser(), commentId);
        return buildStatus(reactions, null, commentId);
    }

    public List<Reaction> reactions() {
        return reactionRepository.findAll();
    }

    private ReactionActionResult publicationResult(Integer publicationId, Integer userId, String action, String emoji, String message) {
        ReactionStatus status = publicationStatus(publicationId, buildUser(userId));
        return ReactionActionResult.success(action, emoji, message, status);
    }

    private ReactionActionResult commentResult(Integer commentId, Integer userId, String action, String emoji, String message) {
        ReactionStatus status = commentStatus(commentId, buildUser(userId));
        return ReactionActionResult.success(action, emoji, message, status);
    }

    private ReactionStatus buildStatus(List<Reaction> reactions, Integer publicationId, Integer commentId) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("Like", publicationId != null
            ? reactionRepository.countByPublicationAndKind(publicationId, "Like")
            : reactionRepository.countByCommentAndKind(commentId, "Like"));
        counts.put("Dislike", publicationId != null
            ? reactionRepository.countByPublicationAndKind(publicationId, "Dislike")
            : reactionRepository.countByCommentAndKind(commentId, "Dislike"));

        // Individual Emoji Counts
        Map<String, Integer> emojiCounts = new HashMap<>();
        if (publicationId != null) {
            List<Reaction> allPubReactions = reactionRepository.findByPublication(publicationId);
            for (Reaction r : allPubReactions) {
                if ("Emoji".equals(r.getKind()) && r.getEmoji() != null) {
                    emojiCounts.put(r.getEmoji(), emojiCounts.getOrDefault(r.getEmoji(), 0) + 1);
                }
            }
        } else if (commentId != null) {
            List<Reaction> allCommentReactions = reactionRepository.findByComment(commentId);
            for (Reaction r : allCommentReactions) {
                if ("Emoji".equals(r.getKind()) && r.getEmoji() != null) {
                    emojiCounts.put(r.getEmoji(), emojiCounts.getOrDefault(r.getEmoji(), 0) + 1);
                }
            }
        }

        boolean isBookmarked = false;
        List<ReactionPayload> payloads = new ArrayList<>();

        for (Reaction reaction : reactions) {
            if ("Bookmark".equals(reaction.getKind())) {
                isBookmarked = true;
            }
            payloads.add(new ReactionPayload(reaction.getKind(), reaction.getEmoji()));
        }

        return new ReactionStatus(payloads, counts, emojiCounts, isBookmarked);
    }

    private User buildUser(Integer userId) {
        User user = new User();
        user.setIdUser(userId);
        return user;
    }

    private void deleteMany(List<Reaction> reactions) {
        if (reactions == null || reactions.isEmpty()) {
            return;
        }

        List<Integer> ids = new ArrayList<>();
        for (Reaction reaction : reactions) {
            if (reaction.getIdReaction() != null) {
                ids.add(reaction.getIdReaction());
            }
        }
        reactionRepository.deleteByIds(ids);
    }

    private boolean isValidUser(User user) {
        return user != null && isValidId(user.getIdUser());
    }

    private boolean isValidId(Integer id) {
        return id != null && id > 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static class ReactionActionResult {

        private final boolean success;
        private final String action;
        private final String emoji;
        private final String message;
        private final ReactionStatus status;

        private ReactionActionResult(boolean success, String action, String emoji, String message, ReactionStatus status) {
            this.success = success;
            this.action = action;
            this.emoji = emoji;
            this.message = message;
            this.status = status;
        }

        public static ReactionActionResult success(String action, String emoji, String message, ReactionStatus status) {
            return new ReactionActionResult(true, action, emoji, message, status);
        }

        public static ReactionActionResult failure(String message) {
            return new ReactionActionResult(false, "", null, message, ReactionStatus.empty());
        }

        public boolean isSuccess() {
            return success;
        }

        public String getAction() {
            return action;
        }

        public String getEmoji() {
            return emoji;
        }

        public String getMessage() {
            return message;
        }

        public ReactionStatus getStatus() {
            return status;
        }
    }

    public static class ReactionStatus {

        private final List<ReactionPayload> reactions;
        private final Map<String, Integer> counts;
        private final Map<String, Integer> emojiCounts;
        private final boolean bookmarked;

        public ReactionStatus(List<ReactionPayload> reactions, Map<String, Integer> counts, Map<String, Integer> emojiCounts, boolean bookmarked) {
            this.reactions = reactions;
            this.counts = counts;
            this.emojiCounts = emojiCounts;
            this.bookmarked = bookmarked;
        }

        public static ReactionStatus empty() {
            return new ReactionStatus(List.of(), Map.of("Like", 0, "Dislike", 0), new HashMap<>(), false);
        }

        public List<ReactionPayload> getReactions() {
            return reactions;
        }

        public Map<String, Integer> getCounts() {
            return counts;
        }

        public Map<String, Integer> getEmojiCounts() {
            return emojiCounts;
        }

        public boolean isBookmarked() {
            return bookmarked;
        }
    }

    public static class ReactionPayload {

        private final String kind;
        private final String emoji;

        public ReactionPayload(String kind, String emoji) {
            this.kind = kind;
            this.emoji = emoji;
        }

        public String getKind() {
            return kind;
        }

        public String getEmoji() {
            return emoji;
        }
    }
}
