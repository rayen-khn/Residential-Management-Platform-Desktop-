package com.syndicati.controllers.forum;

import com.syndicati.models.forum.Commentaire;
import com.syndicati.models.forum.Publication;
import com.syndicati.models.user.User;
import com.syndicati.services.forum.CommentaireService;
import com.syndicati.services.mail.AsyncMailerService;
import com.syndicati.services.mail.SyndicatiEmailComposer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Controller facade for forum comments.
 */
public class CommentaireController {

    private final CommentaireService commentaireService;
    private final AsyncMailerService asyncMailerService;

    public CommentaireController() {
        this.commentaireService = new CommentaireService();
        this.asyncMailerService = AsyncMailerService.getInstance();
    }

    public List<Commentaire> commentaires() {
        return commentaireService.listAll();
    }

    public Optional<Commentaire> commentaireById(Integer id) {
        return commentaireService.findById(id);
    }

    public List<Commentaire> commentairesByPublication(Publication publication) {
        return commentaireService.listByPublication(publication);
    }

    public List<Commentaire> commentairesByUser(User user) {
        return commentaireService.listByUser(user);
    }

    public Integer commentaireCreate(String description, String image, boolean visibility, Publication publication, User user) {
        Integer id = commentaireService.create(description, image, visibility, publication, user);
        if (id != null && id > 0) {
            notifyAuthor(description, publication, user, visibility);
        }
        return id;
    }

    public Integer commentaireCreate(String description, String image, boolean visibility, LocalDateTime createdAt, Publication publication, User user) {
        Integer id = commentaireService.create(description, image, visibility, createdAt, publication, user);
        if (id != null && id > 0) {
            notifyAuthor(description, publication, user, visibility);
        }
        return id;
    }

    public boolean commentaireUpdate(Integer id, String description, String image, Boolean visibility) {
        boolean success = commentaireService.update(id, description, image, visibility);
        if (success) {
            commentaireService.findById(id).ifPresent(c -> notifyAuthor(description, c.getPublication(), c.getUser(), c.isVisibility()));
        }
        return success;
    }

    public boolean commentaireUpdate(Integer id, String description, String image, Boolean visibility, LocalDateTime createdAt) {
        boolean success = commentaireService.update(id, description, image, visibility, createdAt);
        if (success) {
            commentaireService.findById(id).ifPresent(c -> notifyAuthor(description, c.getPublication(), c.getUser(), c.isVisibility()));
        }
        return success;
    }

    private void notifyAuthor(String content, Publication publication, User commenter, boolean isPublic) {
        if (publication == null) {
            System.out.println("[Comment-Notification] SKIPPED: Publication is null");
            return;
        }
        
        // Ensure we have the publication's author
        User author = publication.getUser();
        if (author == null || author.getEmailUser() == null || author.getEmailUser().isBlank()) {
            System.out.println("[Comment-Notification] INFO: Author missing or incomplete, fetching from database...");
            // Use PublicationService to get a fresh copy with the user populated
            com.syndicati.services.forum.PublicationService pubService = new com.syndicati.services.forum.PublicationService();
            publication = pubService.findById(publication.getIdPublication()).orElse(publication);
            author = publication.getUser();
        }

        if (author == null) {
            System.out.println("[Comment-Notification] SKIPPED: Publication author still null after fetch (id=" + publication.getIdPublication() + ")");
            return;
        }
        
        if (commenter == null) {
            System.out.println("[Comment-Notification] SKIPPED: Commenter is null");
            return;
        }

        // Don't notify the author if they are the one commenting
        if (author.getIdUser().equals(commenter.getIdUser())) {
            System.out.println("[Comment-Notification] SKIPPED: Commenter is the author (id=" + author.getIdUser() + ")");
            return;
        }

        if (author.getEmailUser() == null || author.getEmailUser().isBlank()) {
            System.out.println("[Comment-Notification] SKIPPED: Author has no email (id=" + author.getIdUser() + ")");
            return;
        }

        String commenterName = isPublic ? (commenter.getFirstName() + " " + commenter.getLastName()).trim() : "Anonymous User";
        String snippet = content.length() > 120 ? content.substring(0, 117) + "..." : content;
        
        System.out.println("[Comment-Notification] Attempting to notify author: " + author.getEmailUser() + " about " + (isPublic ? "" : "ANONYMOUS ") + "comment by " + commenterName);

        String html = SyndicatiEmailComposer.commentNotification(
            author.getFirstName(),
            commenterName,
            publication.getTitrePub(),
            snippet
        );

        asyncMailerService.sendHtmlAsync(author.getEmailUser(), "New comment on your post: " + publication.getTitrePub(), html);
    }
    public boolean commentaireDelete(Integer id) {
        return commentaireService.delete(id);
    }

    public CommentaireService getCommentaireService() {
        return commentaireService;
    }
}