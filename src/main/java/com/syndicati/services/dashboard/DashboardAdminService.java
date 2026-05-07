package com.syndicati.services.dashboard;

import com.syndicati.controllers.syndicat.ReclamationController;
import com.syndicati.models.syndicat.Reclamation;
import com.syndicati.models.syndicat.Reponse;
import com.syndicati.controllers.user.onboarding.OnboardingController;
import com.syndicati.controllers.user.profile.ProfileController;
import com.syndicati.controllers.user.user.UserController;
import com.syndicati.controllers.forum.PublicationController;
import com.syndicati.controllers.forum.CommentaireController;
import com.syndicati.controllers.forum.ReactionController;
import com.syndicati.controllers.evenement.EvenementController;
import com.syndicati.controllers.evenement.ParticipationController;
import com.syndicati.models.user.Onboarding;
import com.syndicati.models.user.Profile;
import com.syndicati.models.user.User;
import com.syndicati.models.forum.Publication;
import com.syndicati.models.forum.Commentaire;
import com.syndicati.models.forum.Reaction;
import com.syndicati.models.evenement.Evenement;
import com.syndicati.models.evenement.Participation;
import com.syndicati.utils.session.SessionManager;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DashboardAdminService {

    private final UserController userController;
    private final ProfileController profileController;
    private final OnboardingController onboardingController;
    private final ReclamationController reclamationController;
    private final PublicationController publicationController;
    private final CommentaireController commentaireController;
    private final ReactionController reactionController;
    private final EvenementController evenementController;
    private final ParticipationController participationController;

    public DashboardAdminService() {
        this.userController = new UserController();
        this.profileController = new ProfileController();
        this.onboardingController = new OnboardingController();
        this.reclamationController = new ReclamationController();
        this.publicationController = new PublicationController();
        this.commentaireController = new CommentaireController();
        this.reactionController = new ReactionController();
        this.evenementController = new EvenementController();
        this.participationController = new ParticipationController();
    }

    public List<User> users() {
        return userController.users();
    }

    public List<Profile> profiles() {
        return profileController.profiles();
    }

    public List<Onboarding> onboardings() {
        return onboardingController.onboardings();
    }

    public List<Reclamation> reclamations() {
        return reclamationController.reclamations();
    }

    public List<Reponse> reponses() {
        return reclamationController.reponses();
    }

    public List<Publication> publications() {
        return publicationController.publications();
    }

    public List<Commentaire> commentaires() {
        return commentaireController.commentaires();
    }

    public List<com.syndicati.models.forum.Reaction> reactions() {
        return reactionController.reactions();
    }

    public List<Evenement> evenements() {
        return evenementController.evenements();
    }

    public List<Participation> participations() {
        return participationController.participations();
    }

    public boolean saveEntity(String entityLabel, String mode, String[] originalRowData, VBox fields) {
        if ("User".equalsIgnoreCase(entityLabel)) {
            return saveUser(mode, originalRowData, fields);
        }
        if ("Profile".equalsIgnoreCase(entityLabel)) {
            return saveProfile(mode, originalRowData, fields);
        }
        if ("Onboarding".equalsIgnoreCase(entityLabel)) {
            return saveOnboarding(mode, originalRowData, fields);
        }
        if ("Reclamation".equalsIgnoreCase(entityLabel)) {
            return saveReclamation(mode, originalRowData, fields);
        }
        if ("Reponse".equalsIgnoreCase(entityLabel)) {
            return saveReponse(mode, originalRowData, fields);
        }
        if ("Publication".equalsIgnoreCase(entityLabel)) {
            return savePublication(mode, originalRowData, fields);
        }
        if ("Comment".equalsIgnoreCase(entityLabel)) {
            return saveCommentaire(mode, originalRowData, fields);
        }
        if ("Event".equalsIgnoreCase(entityLabel)) {
            return saveEvenement(mode, originalRowData, fields);
        }
        return false;
    }

    public boolean deleteEntity(String entityLabel, String[] rowData) {
        if ("User".equalsIgnoreCase(entityLabel)) {
            return deleteUser(rowData);
        }
        if ("Profile".equalsIgnoreCase(entityLabel)) {
            return deleteProfile(rowData);
        }
        if ("Reclamation".equalsIgnoreCase(entityLabel)) {
            return deleteReclamation(rowData);
        }
        if ("Reponse".equalsIgnoreCase(entityLabel)) {
            return deleteReponse(rowData);
        }
        if ("Publication".equalsIgnoreCase(entityLabel)) {
            return deletePublication(rowData);
        }
        if ("Comment".equalsIgnoreCase(entityLabel)) {
            return deleteCommentaire(rowData);
        }
        if ("Event".equalsIgnoreCase(entityLabel)) {
            return deleteEvenement(rowData);
        }
        return false;
    }

    private boolean saveUser(String mode, String[] originalRowData, VBox fields) {
        Map<String, String> values = readEditableFieldValues(fields);
        String name = safe(values.get("Name"));
        String email = safe(values.get("Email"));
        String role = normalizeUserRoleForPersistence(safe(values.get("Role")));
        String verifiedText = safe(values.get("Verified"));
        String statusText = safe(values.get("Status"));

        if ("-".equals(name) || "-".equals(email) || "-".equals(role)) {
            return false;
        }

        String[] splitName = splitName(name);
        boolean verified = isTruthy(verifiedText);
        boolean disabled = "DISABLED".equalsIgnoreCase(statusText);

        if ("add".equals(mode)) {
            User user = new User();
            user.setFirstName(splitName[0]);
            user.setLastName(splitName[1]);
            user.setEmailUser(email);
            user.setRoleUser(role);
            user.setVerified(verified);
            user.setDisabled(disabled);
            user.setPasswordUser("ChangeMe#2026");
            return userController.userAdd(user) > 0;
        }

        if ("edit".equals(mode)) {
            String oldEmail = (originalRowData != null && originalRowData.length > 1) ? originalRowData[1] : email;
            Optional<User> existingOpt = userController.userByEmail(oldEmail);
            if (existingOpt.isEmpty()) {
                existingOpt = userController.userByEmail(email);
            }
            if (existingOpt.isEmpty()) {
                return false;
            }

            User existing = existingOpt.get();
            existing.setFirstName(splitName[0]);
            existing.setLastName(splitName[1]);
            existing.setEmailUser(email);
            existing.setRoleUser(role);
            existing.setVerified(verified);
            existing.setDisabled(disabled);
            if (existing.getPasswordUser() == null || existing.getPasswordUser().isBlank()) {
                existing.setPasswordUser("ChangeMe#2026");
            }
            return userController.userEdit(existing);
        }

        return false;
    }

    private String normalizeUserRoleForPersistence(String rawRole) {
        if (rawRole == null || rawRole.isBlank() || "-".equals(rawRole.trim())) {
            return "-";
        }

        String token = rawRole.trim().toUpperCase().replace(' ', '_');
        if (token.startsWith("ROLE_")) {
            token = token.substring(5);
        }

        return switch (token) {
            case "ADMINISTRATOR" -> "ADMIN";
            default -> token;
        };
    }

    private boolean saveProfile(String mode, String[] originalRowData, VBox fields) {
        Map<String, String> values = readEditableFieldValues(fields);

        if ("add".equals(mode)) {
            String userIdStr = safe(values.get("User ID"));
            if ("-".equals(userIdStr)) {
                return false;
            }

            try {
                int userId = Integer.parseInt(userIdStr);
                Profile profile = new Profile();
                profile.setUserId(userId);
                profile.setLocale(safe(values.get("Locale")));
                String themeStr = safe(values.get("Theme"));
                profile.setTheme("-".equals(themeStr) ? null : Integer.parseInt(themeStr));
                String tzStr = safe(values.get("Timezone"));
                profile.setTimezone("-".equals(tzStr) ? null : Integer.parseInt(tzStr));
                profile.setAvatar(null);
                profile.setDescriptionProfile(safe(values.get("Bio")));

                Optional<Integer> createdIdOpt = profileController.profileCreate(profile);
                return createdIdOpt.isPresent();
            } catch (NumberFormatException e) {
                return false;
            }
        }

        if ("edit".equals(mode)) {
            String profileIdStr = (originalRowData != null && originalRowData.length > 0) ? originalRowData[0] : "-";
            if ("-".equals(profileIdStr)) {
                return false;
            }

            try {
                int profileId = Integer.parseInt(profileIdStr);
                Optional<Profile> existingOpt = profileController.profileById(profileId);
                if (existingOpt.isEmpty()) {
                    return false;
                }

                Profile existing = existingOpt.get();
                existing.setLocale(safe(values.get("Locale")));
                String themeStr = safe(values.get("Theme"));
                existing.setTheme("-".equals(themeStr) ? null : Integer.parseInt(themeStr));
                String tzStr = safe(values.get("Timezone"));
                existing.setTimezone("-".equals(tzStr) ? null : Integer.parseInt(tzStr));
                existing.setDescriptionProfile(safe(values.get("Bio")));

                return profileController.profileUpdate(existing);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return false;
    }

    private boolean deleteUser(String[] rowData) {
        if (rowData == null || rowData.length < 2) {
            return false;
        }

        String email = rowData[1];
        Optional<User> existing = userController.userByEmail(email);
        return existing.filter(user -> user.getIdUser() != null)
            .map(user -> userController.userDelete(user.getIdUser()))
            .orElse(false);
    }

    private boolean deleteProfile(String[] rowData) {
        if (rowData == null || rowData.length < 1) {
            return false;
        }

        try {
            int profileId = Integer.parseInt(rowData[0]);
            return profileController.profileDelete(profileId);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean saveOnboarding(String mode, String[] originalRowData, VBox fields) {
        Map<String, String> values = readEditableFieldValues(fields);

        if ("add".equals(mode)) {
            String userIdStr = safe(values.get("User ID"));
            if ("-".equals(userIdStr)) {
                return false;
            }

            try {
                Onboarding onboarding = new Onboarding();
                onboarding.setUserId(Integer.parseInt(userIdStr));
                onboarding.setStep(parseIntOrDefault(values.get("Step"), 1));
                onboarding.setCompleted(isTruthy(values.get("Completed")));
                onboarding.setSelectedLocale(safe(values.get("Locale")));
                onboarding.setSelectedTheme(safe(values.get("Theme")));
                onboarding.setSuggestions(normalizeOptional(values.get("Suggestions")));
                onboarding.setSelectedPreferencesJson(normalizeOptional(values.get("Preferences")));

                return onboardingController.saveOnboarding(onboarding);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        if ("edit".equals(mode)) {
            String onboardingIdStr = (originalRowData != null && originalRowData.length > 0) ? originalRowData[0] : "-";
            if ("-".equals(onboardingIdStr)) {
                return false;
            }

            try {
                int onboardingId = Integer.parseInt(onboardingIdStr);
                Optional<Onboarding> existingOpt = onboardingController.onboardingById(onboardingId);
                if (existingOpt.isEmpty()) {
                    return false;
                }

                Onboarding existing = existingOpt.get();
                existing.setUserId(parseIntOrNull(values.get("User ID"), existing.getUserId()));
                existing.setStep(parseIntOrDefault(values.get("Step"), existing.getStep()));
                existing.setCompleted(isTruthy(values.get("Completed")));
                existing.setSelectedLocale(safe(values.get("Locale")));
                existing.setSelectedTheme(safe(values.get("Theme")));
                existing.setSuggestions(normalizeOptional(values.get("Suggestions")));
                existing.setSelectedPreferencesJson(normalizeOptional(values.get("Preferences")));

                return onboardingController.saveOnboarding(existing);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return false;
    }

    private boolean saveReclamation(String mode, String[] originalRowData, VBox fields) {
        Map<String, String> values = readEditableFieldValues(fields);

        if ("add".equals(mode)) {
            String title = safe(values.get("Title"));
            String userDisplayName = safe(values.get("User"));
            String statut = normalizeReclamationStatus(values.get("Status"));
            LocalDateTime date = parseDate(values.get("Date"));

            if ("-".equals(title) || "-".equals(userDisplayName)) {
                return false;
            }

            Optional<User> userOpt = findUserByDisplayName(userDisplayName);
            if (userOpt.isEmpty()) {
                return false;
            }

            String generatedDescription = "Created from dashboard: " + title;
            Integer createdId = reclamationController.reclamationCreate(
                title,
                generatedDescription,
                date,
                null,
                userOpt.get()
            );

            if (createdId == null || createdId <= 0) {
                return false;
            }

            if (!"-".equals(statut) && !"en_attente".equals(statut)) {
                return reclamationController.reclamationUpdateStatut(createdId, statut);
            }
            return true;
        }

        if ("edit".equals(mode)) {
            String statut = normalizeReclamationStatus(values.get("Status"));
            if ("-".equals(statut)) {
                return false;
            }

            Optional<Reclamation> targetOpt = findReclamationByRow(originalRowData);
            if (targetOpt.isEmpty()) {
                return false;
            }

            Reclamation target = targetOpt.get();
            if (target.getIdReclamations() == null || target.getIdReclamations() <= 0) {
                return false;
            }

            return reclamationController.reclamationUpdateStatut(target.getIdReclamations(), statut);
        }

        return false;
    }

    private boolean saveReponse(String mode, String[] originalRowData, VBox fields) {
        Map<String, String> values = readEditableFieldValues(fields);

        if ("add".equals(mode)) {
            String message = safe(values.get("Message"));
            String userDisplayName = safe(values.get("User"));
            String reclamationTitle = safe(values.get("Reclamation"));

            if ("-".equals(message) || "-".equals(userDisplayName) || "-".equals(reclamationTitle)) {
                return false;
            }

            Optional<User> userOpt = findUserByDisplayName(userDisplayName);
            Optional<Reclamation> reclamationOpt = findReclamationByTitle(reclamationTitle);
            if (userOpt.isEmpty() || reclamationOpt.isEmpty()) {
                return false;
            }

            String title = safe(values.get("Title"));
            String image = safe(values.get("Image"));
            String imageValue = "-".equals(image) ? null : image;

            Integer createdId = reclamationController.reponseCreate(
                "-".equals(title) ? null : title,
                message,
                imageValue,
                reclamationOpt.get(),
                userOpt.get()
            );
            return createdId != null && createdId > 0;
        }

        if ("edit".equals(mode)) {
            Optional<Reponse> targetOpt = findReponseByRow(originalRowData);
            if (targetOpt.isEmpty()) {
                return false;
            }

            Reponse target = targetOpt.get();
            if (target.getIdReponses() == null || target.getIdReponses() <= 0) {
                return false;
            }

            String updatedMessage = safe(values.get("Message"));
            String updatedTitle = safe(values.get("Title"));
            String updatedImage = safe(values.get("Image"));
            
            if ("-".equals(updatedMessage)) {
                return false;
            }

            return reclamationController.reponseUpdate(
                target.getIdReponses(),
                "-".equals(updatedTitle) ? target.getTitreReponse() : updatedTitle,
                updatedMessage,
                "-".equals(updatedImage) ? target.getImageReponse() : updatedImage
            );
        }

        return false;
    }

    private boolean deleteReclamation(String[] rowData) {
        Optional<Reclamation> targetOpt = findReclamationByRow(rowData);
        if (targetOpt.isEmpty() || targetOpt.get().getIdReclamations() == null) {
            return false;
        }
        Reclamation rec = targetOpt.get();
        // Delete all responses first to avoid foreign key constraint error
        List<Reponse> responses = reclamationController.reponsesByReclamation(rec);
        if (responses != null) {
            for (Reponse rep : responses) {
                reclamationController.reponseDelete(rep.getIdReponses());
            }
        }
        return reclamationController.reclamationDelete(rec.getIdReclamations());
    }

    private boolean deleteReponse(String[] rowData) {
        Optional<Reponse> targetOpt = findReponseByRow(rowData);
        if (targetOpt.isEmpty() || targetOpt.get().getIdReponses() == null) {
            return false;
        }
        return reclamationController.reponseDelete(targetOpt.get().getIdReponses());
    }

    public Optional<Reponse> findReponseByRow(String[] originalRowData) {
        if (originalRowData == null || originalRowData.length < 5) {
            return Optional.empty();
        }

        String rowTitle = safe(originalRowData[0]);
        String rowMessage = safe(originalRowData[1]);
        String rowUser = safe(originalRowData[2]);
        String rowReclamation = safe(originalRowData[3]);
        String rowDate = safe(originalRowData[4]);

        for (Reponse rep : reclamationController.reponses()) {
            String repTitle = safe(rep.getTitreReponse());
            String repMessage = toReponseTableMessage(rep.getMessageReponse());
            String repUser = safe(reclamationUserName(rep.getUser()));
            String repReclamation = rep.getReclamation() != null ? safe(rep.getReclamation().getTitreReclamations()) : "-";
            String repDate = rep.getCreatedAt() != null ? rep.getCreatedAt().toString().substring(0, 10) : "-";

            if (repTitle.equals(rowTitle)
                && repMessage.equals(rowMessage)
                && repUser.equals(rowUser)
                && repReclamation.equals(rowReclamation)
                && repDate.equals(rowDate)) {
                return Optional.of(rep);
            }
        }

        return Optional.empty();
    }

    private boolean savePublication(String mode, String[] originalRowData, VBox fields) {
        Map<String, String> values = readEditableFieldValues(fields);
        String title = safe(values.get("Title"));
        String category = safe(values.get("Category"));
        String description = safe(values.get("Description"));
        String image = safe(values.get("Image"));
        String dateText = safe(values.get("Date"));

        if ("-".equals(title) || "-".equals(category) || "-".equals(description)) {
            return false;
        }
        String imageValue = "-".equals(image) ? null : image;

        if ("add".equals(mode)) {
            User currentUser = com.syndicati.utils.session.SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                return false;
            }

            LocalDateTime publicationDate = "-".equals(dateText)
                ? LocalDateTime.now()
                : parseDateStrict(dateText).orElse(null);
            if (publicationDate == null) {
                return false;
            }

            Integer createdId = publicationController.publicationCreate(
                title,
                description,
                category,
                imageValue,
                publicationDate,
                currentUser
            );
            return createdId != null && createdId > 0;
        }

        if ("edit".equals(mode)) {
            Optional<Publication> targetOpt = findPublicationByRow(originalRowData);
            if (targetOpt.isEmpty()) {
                return false;
            }

            Publication target = targetOpt.get();
            if (target.getIdPublication() == null) {
                return false;
            }

            LocalDateTime publicationDate;
            if ("-".equals(dateText)) {
                publicationDate = target.getDateCreationPub() != null ? target.getDateCreationPub() : LocalDateTime.now();
            } else {
                publicationDate = parseDateStrict(dateText).orElse(null);
            }
            if (publicationDate == null) {
                return false;
            }

            return publicationController.publicationUpdate(
                target.getIdPublication(),
                title,
                description,
                category,
                imageValue,
                publicationDate
            );
        }

        return false;
    }

    private boolean saveCommentaire(String mode, String[] originalRowData, VBox fields) {
        Map<String, String> values = readEditableFieldValues(fields);
        String description = safe(values.get("Description"));
        String image = safe(values.get("Image"));
        String dateText = safe(values.get("Date"));
        String publicationTitle = safe(values.get("Publication"));
        String authorDisplayName = safe(values.get("Author"));

        if ("-".equals(description) || "-".equals(publicationTitle)) {
            return false;
        }
        String imageValue = "-".equals(image) ? null : image;

        if ("add".equals(mode)) {
            Optional<Publication> pubOpt = findPublicationByTitle(publicationTitle);
            Optional<User> authorOpt = findUserByDisplayName(authorDisplayName);
            
            if (pubOpt.isEmpty() || authorOpt.isEmpty()) {
                return false;
            }

            LocalDateTime createdAt = "-".equals(dateText)
                ? LocalDateTime.now()
                : parseDateStrict(dateText).orElse(null);
            if (createdAt == null) {
                return false;
            }

            Integer createdId = commentaireController.commentaireCreate(
                description,
                imageValue,
                true,
                createdAt,
                pubOpt.get(),
                authorOpt.get()
            );
            return createdId != null && createdId > 0;
        }

        if ("edit".equals(mode)) {
            Optional<Commentaire> targetOpt = findCommentaireByRow(originalRowData);
            if (targetOpt.isEmpty()) {
                return false;
            }

            Commentaire target = targetOpt.get();
            if (target.getIdCommentaire() == null) {
                return false;
            }

            LocalDateTime createdAt;
            if ("-".equals(dateText)) {
                createdAt = target.getCreatedAt() != null ? target.getCreatedAt() : LocalDateTime.now();
            } else {
                createdAt = parseDateStrict(dateText).orElse(null);
            }
            if (createdAt == null) {
                return false;
            }

            return commentaireController.commentaireUpdate(
                target.getIdCommentaire(),
                description,
                imageValue,
                target.isVisibility(),
                createdAt
            );
        }

        return false;
    }

    private boolean deletePublication(String[] rowData) {
        Optional<Publication> targetOpt = findPublicationByRow(rowData);
        if (targetOpt.isEmpty() || targetOpt.get().getIdPublication() == null) {
            return false;
        }
        return publicationController.publicationDelete(targetOpt.get().getIdPublication());
    }

    private boolean deleteCommentaire(String[] rowData) {
        Optional<Commentaire> targetOpt = findCommentaireByRow(rowData);
        if (targetOpt.isEmpty() || targetOpt.get().getIdCommentaire() == null) {
            return false;
        }
        return commentaireController.commentaireDelete(targetOpt.get().getIdCommentaire());
    }

    private boolean saveEvenement(String mode, String[] originalRowData, VBox fields) {
        Map<String, String> values = readEditableFieldValues(fields);
        String title = safe(values.get("Title"));
        String type = safe(values.get("Type")).toLowerCase();
        String description = safe(values.get("Description"));
        String dateText = safe(values.get("Date"));
        String location = safe(values.get("Location"));
        String totalPlacesText = safe(values.get("Total Places"));
        String availablePlacesText = safe(values.get("Available Places"));
        String image = safe(values.get("Image"));

        if ("-".equals(title) || "-".equals(type) || "-".equals(description) || "-".equals(dateText) || "-".equals(location)) {
            return false;
        }

        Optional<LocalDateTime> dateOpt = parseDateStrict(dateText);
        if (dateOpt.isEmpty()) {
            return false;
        }

        Integer totalPlaces = parseIntOrNull(totalPlacesText, null);
        Integer availablePlaces = parseIntOrNull(availablePlacesText, null);
        if (totalPlaces == null || availablePlaces == null) {
            return false;
        }
        if (totalPlaces < 0 || availablePlaces < 0 || availablePlaces > totalPlaces) {
            return false;
        }

        String imageValue = "-".equals(image) ? null : image;

        if ("add".equals(mode)) {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                return false;
            }

            Integer createdId = evenementController.evenementCreate(
                title,
                description,
                dateOpt.get(),
                location,
                totalPlaces,
                type,
                imageValue,
                currentUser
            );

            if (createdId == null || createdId <= 0) {
                return false;
            }

            return evenementController.evenementUpdateForDashboard(
                createdId,
                title,
                description,
                dateOpt.get(),
                location,
                totalPlaces,
                availablePlaces,
                type,
                imageValue
            );
        }

        if ("edit".equals(mode)) {
            Optional<Evenement> targetOpt = findEvenementByRow(originalRowData);
            if (targetOpt.isEmpty() || targetOpt.get().getIdEvent() == null) {
                return false;
            }

            return evenementController.evenementUpdateForDashboard(
                targetOpt.get().getIdEvent(),
                title,
                description,
                dateOpt.get(),
                location,
                totalPlaces,
                availablePlaces,
                type,
                imageValue
            );
        }

        return false;
    }

    private boolean deleteEvenement(String[] rowData) {
        Optional<Evenement> targetOpt = findEvenementByRow(rowData);
        if (targetOpt.isEmpty() || targetOpt.get().getIdEvent() == null) {
            return false;
        }
        return evenementController.evenementDelete(targetOpt.get().getIdEvent());
    }

    private Optional<Evenement> findEvenementByRow(String[] originalRowData) {
        if (originalRowData == null || originalRowData.length < 5) {
            return Optional.empty();
        }

        String rowTitle = safe(originalRowData[0]);
        String rowType = safe(originalRowData[1]);
        String rowDate = safe(originalRowData[3]);
        String rowLocation = safe(originalRowData[4]);

        for (Evenement event : evenementController.evenements()) {
            String eventTitle = safe(event.getTitreEvent());
            String eventType = safe(event.getTypeEvent());
            String eventDate = event.getDateEvent() != null ? event.getDateEvent().toLocalDate().toString() : "-";
            String eventLocation = safe(event.getLieuEvent());

            if (eventTitle.equals(rowTitle)
                && eventType.equals(rowType)
                && eventDate.equals(rowDate)
                && eventLocation.equals(rowLocation)) {
                return Optional.of(event);
            }
        }

        return Optional.empty();
    }

    private Optional<Publication> findPublicationByRow(String[] originalRowData) {
        if (originalRowData == null || originalRowData.length < 5) {
            return Optional.empty();
        }

        String rowTitle = safe(originalRowData[0]);
        String rowCategory = safe(originalRowData[1]);
        String rowDescription = safe(originalRowData[2]);
        String rowDate = safe(originalRowData[4]);

        for (Publication pub : publicationController.publications()) {
            String pubTitle = safe(pub.getTitrePub());
            String pubCategory = safe(pub.getCategoriePub());
            String pubDescription = safe(pub.getDescriptionPub());
            String pubDate = pub.getDateCreationPub() != null ? pub.getDateCreationPub().toLocalDate().toString() : "-";
            if (pubTitle.equals(rowTitle)
                && pubCategory.equals(rowCategory)
                && pubDescription.equals(rowDescription)
                && pubDate.equals(rowDate)) {
                return Optional.of(pub);
            }
        }

        return Optional.empty();
    }

    private Optional<Publication> findPublicationByTitle(String title) {
        String normalized = safe(title);
        if ("-".equals(normalized)) {
            return Optional.empty();
        }

        for (Publication pub : publicationController.publications()) {
            if (safe(pub.getTitrePub()).equals(normalized)) {
                return Optional.of(pub);
            }
        }

        return Optional.empty();
    }

    private Optional<Commentaire> findCommentaireByRow(String[] originalRowData) {
        if (originalRowData == null || originalRowData.length < 5) {
            return Optional.empty();
        }

        String rowPublication = safe(originalRowData[0]);
        String rowAuthor = safe(originalRowData[1]);
        String rowDescription = safe(originalRowData[2]);
        String rowImage = safe(originalRowData[3]);
        String rowDate = safe(originalRowData[4]);

        for (Commentaire comment : commentaireController.commentaires()) {
            String commentPub = comment.getPublication() != null ? safe(comment.getPublication().getTitrePub()) : "Unknown";
            String commentAuthor = comment.getUser() != null ? safe(comment.getUser().getFirstName()) + " " + safe(comment.getUser().getLastName()) : "Unknown";
            String commentDescription = safe(comment.getDescriptionCommentaire());
            String commentImage = safe(comment.getImageCommentaire());
            String commentDate = comment.getCreatedAt() != null ? comment.getCreatedAt().toLocalDate().toString() : "-";
            commentAuthor = commentAuthor.trim();

            if (commentPub.equals(rowPublication)
                && commentAuthor.equals(rowAuthor)
                && commentDescription.equals(rowDescription)
                && commentImage.equals(rowImage)
                && commentDate.equals(rowDate)) {
                return Optional.of(comment);
            }
        }

        return Optional.empty();
    }

    private String toReponseTableMessage(String message) {
        String normalized = safe(message);
        if (normalized.length() > 50) {
            return normalized.substring(0, 47) + "...";
        }
        return normalized;
    }

    private Optional<Reclamation> findReclamationByTitle(String title) {
        String normalized = safe(title);
        if ("-".equals(normalized)) {
            return Optional.empty();
        }

        for (Reclamation rec : reclamationController.reclamations()) {
            if (safe(rec.getTitreReclamations()).equals(normalized)) {
                return Optional.of(rec);
            }
        }

        return Optional.empty();
    }

    private Optional<User> findUserByDisplayName(String displayName) {
        String normalized = safe(displayName);
        if ("-".equals(normalized)) {
            return Optional.empty();
        }

        for (User user : userController.users()) {
            String full = reclamationUserName(user);
            if (full.equals(normalized)) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    private LocalDateTime parseDate(String dateValue) {
        String normalized = safe(dateValue);
        if ("-".equals(normalized)) {
            return LocalDateTime.now();
        }

        try {
            return LocalDate.parse(normalized).atStartOfDay();
        } catch (DateTimeParseException e) {
            return LocalDateTime.now();
        }
    }

    private Optional<LocalDateTime> parseDateStrict(String dateValue) {
        String normalized = safe(dateValue);
        if ("-".equals(normalized)) {
            return Optional.empty();
        }

        try {
            return Optional.of(LocalDate.parse(normalized).atStartOfDay());
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private String reclamationUserName(User user) {
        if (user == null) {
            return "Unknown";
        }
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? "Unknown" : full;
    }

    public Optional<Reclamation> findReclamationByRow(String[] originalRowData) {
        if (originalRowData == null || originalRowData.length < 5) {
            return Optional.empty();
        }

        String rowTitle = safe(originalRowData[0]);
        String rowUser = safe(originalRowData[1]);
        String rowDate = safe(originalRowData[3]);
        String rowReplies = safe(originalRowData[4]);

        for (Reclamation rec : reclamationController.reclamations()) {
            String recTitle = safe(rec.getTitreReclamations());
            String recUser = safe(reclamationUserName(rec));
            String recDate = rec.getCreatedAt() != null ? rec.getCreatedAt().toString().substring(0, 10) : "-";
            List<Reponse> reps = reclamationController.reponsesByReclamation(rec);
            String recReplies = String.valueOf(reps != null ? reps.size() : 0);

            if (recTitle.equals(rowTitle)
                && recUser.equals(rowUser)
                && recDate.equals(rowDate)
                && recReplies.equals(rowReplies)) {
                return Optional.of(rec);
            }
        }

        return Optional.empty();
    }

    private String reclamationUserName(Reclamation rec) {
        if (rec == null || rec.getUser() == null) {
            return "Unknown";
        }
        String first = rec.getUser().getFirstName() == null ? "" : rec.getUser().getFirstName().trim();
        String last = rec.getUser().getLastName() == null ? "" : rec.getUser().getLastName().trim();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? "Unknown" : full;
    }

    private String normalizeReclamationStatus(String value) {
        String normalized = safe(value);
        if ("-".equals(normalized)) {
            return "-";
        }

        String token = normalized.trim().toLowerCase().replace(' ', '_');
        return switch (token) {
            case "active", "en_attente", "refuse", "termine" -> token;
            case "pending" -> "en_attente";
            case "rejected" -> "refuse";
            case "completed" -> "termine";
            default -> "-";
        };
    }

    private Map<String, String> readEditableFieldValues(VBox fields) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Node node : fields.getChildren()) {
            if (!(node instanceof VBox)) {
                continue;
            }
            VBox row = (VBox) node;
            if (row.getChildren().size() < 2) {
                continue;
            }
            Node labelNode = row.getChildren().get(0);
            Node inputNode = row.getChildren().get(1);
            if (labelNode instanceof Text && inputNode instanceof TextField) {
                String label = ((Text) labelNode).getText();
                String value = ((TextField) inputNode).getText();
                values.put(label, value);
                continue;
            }
            if (labelNode instanceof Text && inputNode instanceof ComboBox) {
                String label = ((Text) labelNode).getText();
                Object selected = ((ComboBox<?>) inputNode).getValue();
                values.put(label, selected == null ? "" : selected.toString());
                continue;
            }
            if (labelNode instanceof Text && inputNode instanceof DatePicker) {
                String label = ((Text) labelNode).getText();
                DatePicker picker = (DatePicker) inputNode;
                values.put(label, picker.getValue() == null ? picker.getEditor().getText() : picker.getValue().toString());
                continue;
            }
            if (labelNode instanceof Text && inputNode instanceof HBox) {
                String label = ((Text) labelNode).getText();
                HBox rowInput = (HBox) inputNode;
                for (Node child : rowInput.getChildren()) {
                    if (child instanceof TextField) {
                        values.put(label, ((TextField) child).getText());
                        break;
                    }
                    if (child instanceof DatePicker) {
                        DatePicker picker = (DatePicker) child;
                        values.put(label, picker.getValue() == null ? picker.getEditor().getText() : picker.getValue().toString());
                        break;
                    }
                }
            } else if (labelNode instanceof Text && inputNode instanceof VBox) {
                String label = ((Text) labelNode).getText();
                VBox rowInput = (VBox) inputNode;
                // Search deep for a TextField (handles the Image preview structure)
                String value = findTextFieldValue(rowInput);
                if (value != null) {
                    values.put(label, value);
                }
            }
        }
        return values;
    }

    private String findTextFieldValue(Pane container) {
        for (Node child : container.getChildren()) {
            if (child instanceof TextField) {
                return ((TextField) child).getText();
            }
            if (child instanceof HBox) {
                String val = findTextFieldValue((HBox) child);
                if (val != null) return val;
            }
            if (child instanceof VBox) {
                String val = findTextFieldValue((VBox) child);
                if (val != null) return val;
            }
        }
        return null;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String[] splitName(String fullName) {
        String normalized = fullName == null ? "" : fullName.trim();
        if (normalized.isEmpty()) {
            return new String[]{"Unknown", "User"};
        }

        String[] parts = normalized.split("\\s+", 2);
        if (parts.length == 1) {
            return new String[]{parts[0], parts[0]};
        }
        return new String[]{parts[0], parts[1]};
    }

    private boolean isTruthy(String value) {
        String normalized = value == null ? "" : value.trim();
        return "yes".equalsIgnoreCase(normalized)
            || "true".equalsIgnoreCase(normalized)
            || "1".equalsIgnoreCase(normalized)
            || "verified".equalsIgnoreCase(normalized);
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            String normalized = safe(value);
            return "-".equals(normalized) ? defaultValue : Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Integer parseIntOrNull(String value, Integer fallback) {
        try {
            String normalized = safe(value);
            return "-".equals(normalized) ? fallback : Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String normalizeOptional(String value) {
        String normalized = safe(value);
        return "-".equals(normalized) ? null : normalized;
    }

    public List<com.syndicati.models.log.AppEventLog> recentActivityLogs(int limit) {
        return new java.util.ArrayList<>();
    }
    public List<String[]> topPages() {
        return new java.util.ArrayList<>();
    }
    public List<String[]> topClicks() {
        return new java.util.ArrayList<>();
    }
    public List<String[]> deviceBreakdown() {
        return new java.util.ArrayList<>();
    }
    public List<String[]> outcomeBreakdown() {
        return new java.util.ArrayList<>();
    }
    public List<String[]> levelBreakdown() {
        return new java.util.ArrayList<>();
    }
    public List<String[]> riskSignals(int limit) {
        return new java.util.ArrayList<>();
    }
    public List<String[]> interactionTrends() {
        return new java.util.ArrayList<>();
    }
    public List<String[]> topUsers() {
        return new java.util.ArrayList<>();
    }
    public Map<String, Integer> activityHeartbeat() {
        return new java.util.HashMap<>();
    }
}