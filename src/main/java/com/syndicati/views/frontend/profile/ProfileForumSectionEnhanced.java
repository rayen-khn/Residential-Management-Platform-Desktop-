package com.syndicati.views.frontend.profile;

import com.syndicati.controllers.forum.CommentaireController;
import com.syndicati.controllers.forum.PublicationController;
import com.syndicati.controllers.forum.ReactionController;
import com.syndicati.controllers.user.profile.ProfileController;
import com.syndicati.models.forum.Commentaire;
import com.syndicati.models.forum.Publication;
import com.syndicati.models.forum.Reaction;
import com.syndicati.services.forum.ReactionService;
import com.syndicati.models.user.Profile;
import com.syndicati.models.user.User;
import com.syndicati.utils.image.ImageLoaderUtil;
import com.syndicati.utils.theme.ThemeManager;
import com.syndicati.utils.session.SessionManager;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;

/**
 * Enhanced Forum section with working filters and details panel.
 */
public class ProfileForumSectionEnhanced {

    private final VBox root = new VBox(16);
    private final PublicationController publicationController;
    private final CommentaireController commentaireController;
    private final ReactionController reactionController;
    private final ProfileController profileController;
    private final User currentUser;
    private final ThemeManager tm = ThemeManager.getInstance();

    private final StackPane switcherContainer = new StackPane();
    private final VBox faceListView = new VBox(16);
    private final VBox faceDetailsView = new VBox(16);

    private final VBox publicationList = new VBox(10);
    private final VBox commentList = new VBox(10);
    private final VBox reactionList = new VBox(10);
    private final VBox bookmarkList = new VBox(10);

    private final Label detailsTitle = new Label("Details");
    private final VBox detailsContent = new VBox(12);

    private String currentTab = "publications";
    private String currentCategory = "General";
    private String currentFilter = "my";

    private Button publicationsBtn;
    private Button commentsBtn;
    private Button reactionsBtn;
    private Button bookmarksBtn;

    private final List<Publication> allPublications = new ArrayList<>();
    private final List<Commentaire> allComments = new ArrayList<>();
    private final List<Reaction> allReactions = new ArrayList<>();
    private final Map<Integer, Publication> publicationById = new HashMap<>();
    private final Map<Integer, Profile> profileByUserIdCache = new HashMap<>();

    private List<Publication> filteredPublications = new ArrayList<>();
    private List<Commentaire> filteredComments = new ArrayList<>();
    private List<Reaction> filteredReactions = new ArrayList<>();
    private List<Reaction> filteredBookmarks = new ArrayList<>();

    private String selectedCommentImagePath = null;
    private boolean commentVisibility = true;

    public ProfileForumSectionEnhanced() {
        this.publicationController = new PublicationController();
        this.commentaireController = new CommentaireController();
        this.reactionController = new ReactionController();
        this.profileController = new ProfileController();
        this.currentUser = SessionManager.getInstance().getCurrentUser();

        if (isAdminUser(currentUser)) {
            currentFilter = "all";
        }

        buildLayout();
        refreshDataAndRender();
    }

    private void buildLayout() {
        root.setPadding(new Insets(16, 0, 0, 0));

        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 1; -fx-background-radius: 20; -fx-border-radius: 20;");
        card.setPrefHeight(600);

        switcherContainer.setPrefHeight(500);
        VBox.setVgrow(switcherContainer, Priority.ALWAYS);

        buildListView();
        buildDetailsView();

        switcherContainer.getChildren().addAll(faceListView, faceDetailsView);
        faceDetailsView.setVisible(false);
        faceDetailsView.setManaged(false);

        card.getChildren().add(switcherContainer);
        root.getChildren().add(card);
    }

    private void buildListView() {
        faceListView.setStyle("-fx-background-color: transparent;");
        faceListView.setPadding(new Insets(20));

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("📧 Your Forum Activity");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);

        HBox adminFilter = createAdminFilter();
        HBox.setHgrow(title, Priority.ALWAYS);
        header.getChildren().addAll(title, adminFilter);

        faceListView.getChildren().add(header);
        faceListView.getChildren().add(createCategoryFilters());
        faceListView.getChildren().add(createSubTabs());

        ScrollPane contentScroll = new ScrollPane();
        contentScroll.setStyle("-fx-control-inner-background: transparent; -fx-padding: 0;");
        contentScroll.setFitToWidth(true);
        contentScroll.setPrefHeight(420);

        VBox contentContainer = new VBox(10);
        contentContainer.getChildren().addAll(publicationList, commentList, reactionList, bookmarkList);
        contentScroll.setContent(contentContainer);
        VBox.setVgrow(contentScroll, Priority.ALWAYS);

        faceListView.getChildren().add(contentScroll);
    }

    private void buildDetailsView() {
        faceDetailsView.setStyle("-fx-background-color: rgba(20, 20, 25, 0.98);");
        faceDetailsView.setPadding(new Insets(20));

        HBox backHeader = new HBox(12);
        backHeader.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("←");
        backBtn.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-background-radius: 50%; -fx-border-radius: 50%;");
        backBtn.setOnAction(e -> showListView());

        detailsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        detailsTitle.setTextFill(Color.WHITE);
        detailsTitle.setText("Details");

        backHeader.getChildren().addAll(backBtn, detailsTitle);
        faceDetailsView.getChildren().add(backHeader);

        ScrollPane detailScroll = new ScrollPane();
        detailScroll.setStyle("-fx-control-inner-background: transparent; -fx-padding: 0;");
        detailScroll.setFitToWidth(true);

        detailsContent.setPadding(new Insets(16, 0, 0, 0));
        Label placeholder = new Label("Select an item to view details");
        placeholder.setTextFill(Color.color(1, 1, 1, 0.5));
        detailsContent.getChildren().add(placeholder);

        detailScroll.setContent(detailsContent);
        VBox.setVgrow(detailScroll, Priority.ALWAYS);
        faceDetailsView.getChildren().add(detailScroll);
    }

    private HBox createAdminFilter() {
        HBox filter = new HBox(8);
        filter.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 1; -fx-padding: 4; -fx-background-radius: 999; -fx-border-radius: 999;");

        if (isAdminUser(currentUser)) {
            Button myOwnBtn = createFilterButton("My Own", "my".equals(currentFilter));
            Button everyoneBtn = createFilterButton("Everyone", "all".equals(currentFilter));
            myOwnBtn.setOnAction(e -> switchFilter("my", myOwnBtn, everyoneBtn));
            everyoneBtn.setOnAction(e -> switchFilter("all", myOwnBtn, everyoneBtn));
            filter.getChildren().addAll(myOwnBtn, everyoneBtn);
        }

        return filter;
    }

    private Button createFilterButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-padding: 6 12 6 12; " +
            "-fx-background-color: " + (active ? "rgba(99, 102, 241, 0.8);" : "transparent;") +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 999; " +
            "-fx-font-size: 11;"
        );
        return btn;
    }

    private void switchFilter(String filterType, Button myOwnBtn, Button everyoneBtn) {
        currentFilter = filterType;
        boolean isMyOwn = "my".equals(filterType);

        myOwnBtn.setStyle(
            "-fx-padding: 6 12 6 12; " +
            "-fx-background-color: " + (isMyOwn ? "rgba(99, 102, 241, 0.8);" : "transparent;") +
            "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 999; -fx-font-size: 11;"
        );
        everyoneBtn.setStyle(
            "-fx-padding: 6 12 6 12; " +
            "-fx-background-color: " + (!isMyOwn ? "rgba(99, 102, 241, 0.8);" : "transparent;") +
            "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 999; -fx-font-size: 11;"
        );

        refreshDataAndRender();
    }

    private HBox createCategoryFilters() {
        HBox filters = new HBox(8);
        filters.setAlignment(Pos.CENTER_LEFT);

        Button announcementsBtn = new Button("📢 Announcements");
        styleCategoryButton(announcementsBtn, "Announcement".equals(currentCategory));
        announcementsBtn.setOnAction(e -> {
            currentCategory = "Announcement";
            styleCategoryButton(announcementsBtn, true);
            styleCategoryButton((Button) filters.getChildren().get(1), false);
            refreshDataAndRender();
        });

        Button generalBtn = new Button("💬 General");
        styleCategoryButton(generalBtn, !"Announcement".equals(currentCategory));
        generalBtn.setOnAction(e -> {
            currentCategory = "General";
            styleCategoryButton(announcementsBtn, false);
            styleCategoryButton(generalBtn, true);
            refreshDataAndRender();
        });

        filters.getChildren().addAll(announcementsBtn, generalBtn);
        return filters;
    }

    private void styleCategoryButton(Button btn, boolean active) {
        btn.setStyle(
            "-fx-padding: 6 12 6 12; " +
            "-fx-background-color: " + (active ? "rgba(99, 102, 241, 0.2);" : "rgba(255,255,255,0.05);") +
            "-fx-text-fill: " + (active ? "white;" : "rgba(255,255,255,0.7);") +
            "-fx-border-color: " + (active ? "rgba(99, 102, 241, 0.3);" : "rgba(255,255,255,0.1);") +
            "-fx-border-width: 1; -fx-background-radius: 999; -fx-border-radius: 999; -fx-font-size: 11;" +
            (active ? "-fx-font-weight: bold;" : "")
        );
    }

    private HBox createSubTabs() {
        HBox tabs = new HBox(8);
        tabs.setPadding(new Insets(6));
        tabs.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 1; -fx-background-radius: 999; -fx-border-radius: 999;");
        tabs.setAlignment(Pos.CENTER);

        publicationsBtn = createSubTabButton("📝 Publications", true);
        commentsBtn = createSubTabButton("💬 Comments", false);
        reactionsBtn = createSubTabButton("👍 Reactions", false);
        bookmarksBtn = createSubTabButton("🔖 Bookmarks", false);

        publicationsBtn.setOnAction(e -> switchTab("publications"));
        commentsBtn.setOnAction(e -> switchTab("comments"));
        reactionsBtn.setOnAction(e -> switchTab("reactions"));
        bookmarksBtn.setOnAction(e -> switchTab("bookmarks"));

        tabs.getChildren().addAll(publicationsBtn, commentsBtn, reactionsBtn, bookmarksBtn);
        return tabs;
    }

    private Button createSubTabButton(String text, boolean active) {
        Button btn = new Button(text);
        styleSubTabButton(btn, active);
        return btn;
    }

    private void styleSubTabButton(Button btn, boolean active) {
        btn.setStyle(
            "-fx-padding: 8 14 8 14; " +
            "-fx-background-color: " + (active ? "rgba(99, 102, 241, 0.2);" : "transparent;") +
            "-fx-text-fill: " + (active ? "white;" : "rgba(255, 255, 255, 0.6);") +
            "-fx-border-color: " + (active ? "rgba(99, 102, 241, 0.3);" : "transparent;") +
            "-fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8; " +
            (active ? "-fx-font-weight: bold;" : "") +
            "-fx-font-size: 11;"
        );
    }

    private void switchTab(String tabName) {
        currentTab = tabName;

        styleSubTabButton(publicationsBtn, "publications".equals(tabName));
        styleSubTabButton(commentsBtn, "comments".equals(tabName));
        styleSubTabButton(reactionsBtn, "reactions".equals(tabName));
        styleSubTabButton(bookmarksBtn, "bookmarks".equals(tabName));

        publicationList.setVisible("publications".equals(tabName));
        publicationList.setManaged("publications".equals(tabName));

        commentList.setVisible("comments".equals(tabName));
        commentList.setManaged("comments".equals(tabName));

        reactionList.setVisible("reactions".equals(tabName));
        reactionList.setManaged("reactions".equals(tabName));

        bookmarkList.setVisible("bookmarks".equals(tabName));
        bookmarkList.setManaged("bookmarks".equals(tabName));
    }

    private void refreshDataAndRender() {
        // 1. Data Fetching (safe on background thread)
        List<Publication> pubs = publicationController.publications();
        List<Commentaire> comments = commentaireController.commentaires();
        List<Reaction> reactions = reactionController.reactions();

        // 2. UI Updates (MUST be on FX thread)
        javafx.application.Platform.runLater(() -> {
            allPublications.clear();
            publicationById.clear();
            if (pubs != null) {
                allPublications.addAll(pubs);
                for (Publication pub : allPublications) {
                    if (pub.getIdPublication() != null) {
                        publicationById.put(pub.getIdPublication(), pub);
                    }
                }
            }

            allComments.clear();
            if (comments != null) allComments.addAll(comments);

            allReactions.clear();
            if (reactions != null) allReactions.addAll(reactions);

            filteredPublications = filterPublications(allPublications);
            filteredComments = filterComments(allComments);
            filteredReactions = filterReactions(allReactions, false);
            filteredBookmarks = filterReactions(allReactions, true);

            renderPublications();
            renderComments();
            renderReactions();
            renderBookmarks();
            updateTabLabels();
            switchTab(currentTab);
        });
    }

    private List<Publication> filterPublications(List<Publication> source) {
        List<Publication> out = new ArrayList<>();
        for (Publication pub : source) {
            if (pub == null) {
                continue;
            }
            if (!matchesUserFilter(pub.getUser())) {
                continue;
            }
            if (!matchesCategory(pub.getCategoriePub())) {
                continue;
            }
            out.add(pub);
        }
        return out;
    }

    private List<Commentaire> filterComments(List<Commentaire> source) {
        List<Commentaire> out = new ArrayList<>();
        for (Commentaire comment : source) {
            if (comment == null) {
                continue;
            }
            if (!matchesUserFilter(comment.getUser())) {
                continue;
            }
            Publication publication = resolvePublication(comment.getPublication());
            if (!matchesCategory(publication == null ? null : publication.getCategoriePub())) {
                continue;
            }
            out.add(comment);
        }
        return out;
    }

    private List<Reaction> filterReactions(List<Reaction> source, boolean bookmarksOnly) {
        List<Reaction> out = new ArrayList<>();
        for (Reaction reaction : source) {
            if (reaction == null) {
                continue;
            }
            if (bookmarksOnly && !"Bookmark".equalsIgnoreCase(reaction.getKind())) {
                continue;
            }
            if (!bookmarksOnly && "Bookmark".equalsIgnoreCase(reaction.getKind())) {
                continue;
            }

            if (!matchesUserFilter(reaction.getUser())) {
                continue;
            }

            Publication publication = resolvePublication(reaction.getPublication());
            if (publication == null && reaction.getCommentaire() != null) {
                publication = resolvePublication(reaction.getCommentaire().getPublication());
            }

            if (!matchesCategory(publication == null ? null : publication.getCategoriePub())) {
                continue;
            }
            out.add(reaction);
        }
        return out;
    }

    private boolean matchesUserFilter(User owner) {
        if (!"my".equals(currentFilter)) {
            return true;
        }
        if (currentUser == null || currentUser.getIdUser() == null || owner == null || owner.getIdUser() == null) {
            return false;
        }
        return currentUser.getIdUser().equals(owner.getIdUser());
    }

    private boolean matchesCategory(String category) {
        boolean announcement = "Announcement".equalsIgnoreCase(category);
        if ("Announcement".equals(currentCategory)) {
            return announcement;
        }
        return !announcement;
    }

    private Publication resolvePublication(Publication publication) {
        if (publication == null) {
            return null;
        }
        if (publication.getIdPublication() == null) {
            return publication;
        }
        return publicationById.getOrDefault(publication.getIdPublication(), publication);
    }

    private void renderPublications() {
        publicationList.getChildren().clear();
        if (filteredPublications.isEmpty()) {
            publicationList.getChildren().add(emptyText("No publications found for this filter."));
            return;
        }

        for (Publication pub : filteredPublications) {
            publicationList.getChildren().add(createPublicationItem(pub));
        }
    }

    private void renderComments() {
        commentList.getChildren().clear();
        if (filteredComments.isEmpty()) {
            commentList.getChildren().add(emptyText("No comments found for this filter."));
            return;
        }

        for (Commentaire comment : filteredComments) {
            commentList.getChildren().add(createCommentItem(comment));
        }
    }

    private void renderReactions() {
        reactionList.getChildren().clear();
        if (filteredReactions.isEmpty()) {
            reactionList.getChildren().add(emptyText("No reactions found for this filter."));
            return;
        }

        for (Reaction reaction : filteredReactions) {
            reactionList.getChildren().add(createReactionItem(reaction));
        }
    }

    private void renderBookmarks() {
        bookmarkList.getChildren().clear();
        if (filteredBookmarks.isEmpty()) {
            bookmarkList.getChildren().add(emptyText("No bookmarks found for this filter."));
            return;
        }

        for (Reaction bookmark : filteredBookmarks) {
            bookmarkList.getChildren().add(createBookmarkItem(bookmark));
        }
    }

    private void updateTabLabels() {
        publicationsBtn.setText("📝 Publications (" + filteredPublications.size() + ")");
        commentsBtn.setText("💬 Comments (" + filteredComments.size() + ")");
        reactionsBtn.setText("👍 Reactions (" + filteredReactions.size() + ")");
        bookmarksBtn.setText("🔖 Bookmarks (" + filteredBookmarks.size() + ")");
    }

    private Label emptyText(String message) {
        Label noData = new Label(message);
        noData.setTextFill(Color.color(1, 1, 1, 0.5));
        return noData;
    }

    private VBox createPublicationItem(Publication publication) {
        VBox item = clickableRow(() -> showPublicationDetails(publication));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("📝");
        icon.setFont(Font.font(16));

        VBox titleSection = new VBox(4);
        Label title = new Label(safe(publication.getTitrePub(), "Untitled publication"));
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        title.setTextFill(Color.WHITE);

        Label date = new Label("📅 " + formatDate(publication.getDateCreationPub()));
        date.setFont(Font.font("Segoe UI", 10));
        date.setTextFill(Color.color(1, 1, 1, 0.5));

        titleSection.getChildren().addAll(title, date);
        HBox.setHgrow(titleSection, Priority.ALWAYS);

        Label category = new Label(safe(publication.getCategoriePub(), "General"));
        category.setFont(Font.font("Segoe UI", 9));
        category.setStyle("-fx-text-fill: rgba(99, 102, 241, 0.9); -fx-background-color: rgba(99, 102, 241, 0.1); -fx-padding: 4 8 4 8; -fx-background-radius: 6;");

        Button quickBookmark = new Button(reactionController.publicationStatus(publication, currentUser).isBookmarked() ? "★" : "☆");
        quickBookmark.setStyle("-fx-background-color: transparent; -fx-text-fill: #6366f1; -fx-font-size: 14; -fx-padding: 0 8 0 8; -fx-cursor: hand;");
        quickBookmark.setOnAction(e -> {
            e.consume(); // Don't trigger row click
            reactionController.publicationToggle(publication, currentUser, "Bookmark");
            asyncRefresh();
        });
        
        Label chevron = new Label("→");
        chevron.setFont(Font.font(14));
        chevron.setTextFill(Color.color(1, 1, 1, 0.5));

        header.getChildren().addAll(icon, titleSection, category, quickBookmark, chevron);
        item.getChildren().add(header);

        return item;
    }

    private VBox createCommentItem(Commentaire comment) {
        VBox item = clickableRow(() -> showCommentDetails(comment));

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("💬");
        Label title = new Label(trimTo(safe(comment.getDescriptionCommentaire(), "No comment text"), 90));
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        HBox.setHgrow(title, Priority.ALWAYS);

        Label date = new Label(formatDate(comment.getCreatedAt()));
        date.setTextFill(Color.color(1, 1, 1, 0.5));
        date.setFont(Font.font("Segoe UI", 10));

        row.getChildren().addAll(icon, title, date);
        item.getChildren().add(row);

        return item;
    }

    private VBox createReactionItem(Reaction reaction) {
        VBox item = clickableRow(() -> showReactionDetails(reaction));

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("👍");
        String kind = safe(reaction.getKind(), "Reaction");
        String target = reaction.getPublication() != null ? "Publication" : "Comment";

        Label title = new Label(kind + " on " + target);
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        HBox.setHgrow(title, Priority.ALWAYS);

        Label date = new Label(formatDate(reaction.getUpdatedAt()));
        date.setTextFill(Color.color(1, 1, 1, 0.5));
        date.setFont(Font.font("Segoe UI", 10));

        row.getChildren().addAll(icon, title, date);
        item.getChildren().add(row);

        return item;
    }

    private VBox createBookmarkItem(Reaction bookmark) {
        Publication publication = resolvePublication(bookmark.getPublication());
        if (publication == null && bookmark.getCommentaire() != null) {
            publication = resolvePublication(bookmark.getCommentaire().getPublication());
        }

        if (publication != null) {
            // Render full publication card for bookmarks
            VBox card = createPublicationPreviewCard(publication);
            card.setStyle(card.getStyle() + "; -fx-border-color: rgba(147, 197, 253, 0.2); -fx-border-width: 1;");
            return card;
        }

        // Fallback for non-publication bookmarks
        VBox item = clickableRow(() -> showBookmarkDetails(bookmark));
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("🔖");
        String titleText = "Bookmarked item";
        Label title = new Label(trimTo(titleText, 90));
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        HBox.setHgrow(title, Priority.ALWAYS);
        Label date = new Label(formatDate(bookmark.getUpdatedAt()));
        date.setTextFill(Color.color(1, 1, 1, 0.5));
        date.setFont(Font.font("Segoe UI", 10));
        row.getChildren().addAll(icon, title, date);
        item.getChildren().add(row);
        return item;
    }

    private VBox clickableRow(Runnable onClick) {
        VBox item = new VBox(8);
        item.setStyle(
            "-fx-background-color: rgba(99, 102, 241, 0.05); " +
            "-fx-border-color: rgba(99, 102, 241, 0.1); " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 12; " +
            "-fx-border-radius: 12; " +
            "-fx-padding: 12; " +
            "-fx-cursor: hand;"
        );
        item.setOnMouseClicked(e -> onClick.run());
        item.setOnMouseEntered(e -> item.setStyle(
            "-fx-background-color: rgba(99, 102, 241, 0.1); " +
            "-fx-border-color: rgba(99, 102, 241, 0.2); " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 12; " +
            "-fx-border-radius: 12; " +
            "-fx-padding: 12; " +
            "-fx-cursor: hand;"
        ));
        item.setOnMouseExited(e -> item.setStyle(
            "-fx-background-color: rgba(99, 102, 241, 0.05); " +
            "-fx-border-color: rgba(99, 102, 241, 0.1); " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 12; " +
            "-fx-border-radius: 12; " +
            "-fx-padding: 12; " +
            "-fx-cursor: hand;"
        ));
        return item;
    }

    private void showPublicationDetails(Publication publication) {
        detailsTitle.setText("Publication Details");
        detailsContent.getChildren().clear();

        VBox previewCard = createPublicationPreviewCard(publication);
        VBox commentsSection = createPublicationCommentsSection(publication);

        detailsContent.getChildren().addAll(previewCard, commentsSection);
        animateSwitcherFace(false);
    }

    private VBox createPublicationPreviewCard(Publication publication) {
        VBox preview = detailCard();
        preview.setSpacing(15);

        // Header with Title and Bookmark
        HBox header = new HBox(15);
        header.setAlignment(Pos.TOP_LEFT);

        VBox titleArea = new VBox(4);
        Label title = new Label(safe(publication.getTitrePub(), "Untitled publication"));
        title.setTextFill(Color.WHITE);
        title.setWrapText(true);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        
        HBox meta = new HBox(10);
        Label category = new Label("🏷️ " + safe(publication.getCategoriePub(), "General"));
        category.setTextFill(Color.web("#6366f1"));
        category.setFont(Font.font("Segoe UI Emoji", FontWeight.BOLD, 11));
        
        Label date = new Label("📅 " + formatDate(publication.getDateCreationPub()));
        date.setTextFill(Color.color(1, 1, 1, 0.5));
        date.setFont(Font.font("Segoe UI Emoji", 11));
        
        meta.getChildren().addAll(category, date);
        titleArea.getChildren().addAll(title, meta);
        HBox.setHgrow(titleArea, Priority.ALWAYS);

        Button bookmarkBtn = new Button("🔖");
        styleIconButton(bookmarkBtn, reactionController.publicationStatus(publication, currentUser).isBookmarked());
        bookmarkBtn.setOnAction(e -> {
            reactionController.publicationToggle(publication, currentUser, "Bookmark");
            asyncRefresh();
        });

        header.getChildren().addAll(titleArea, bookmarkBtn);

        // Description
        Label description = new Label(safe(publication.getDescriptionPub(), "No description"));
        description.setTextFill(Color.color(1, 1, 1, 0.9));
        description.setWrapText(true);
        description.setFont(Font.font("Segoe UI", 13));
        description.setPadding(new Insets(5, 0, 5, 0));

        // Image
        Node imageNode;
        Image image = resolveImage(publication.getImagePub(), "forum_images");
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(550);
            imageView.setFitHeight(350);
            imageView.setSmooth(true);
            
            StackPane imageFrame = new StackPane(imageView);
            imageFrame.setAlignment(Pos.CENTER_LEFT);
            imageFrame.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 12; -fx-padding: 2; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 12;");
            
            // Add a subtle overlay or shadow if needed
            imageNode = imageFrame;
        } else {
            imageNode = new VBox(); // Empty placeholder
        }

        // Action Bar (Edit/Delete for author)
        HBox authorActions = new HBox(10);
        authorActions.setAlignment(Pos.CENTER_RIGHT);
        if (currentUser != null && publication.getUser() != null && currentUser.getIdUser().equals(publication.getUser().getIdUser())) {
            Button editBtn = new Button("Edit ✏️");
            editBtn.setStyle("-fx-background-color: rgba(99, 102, 241, 0.2); -fx-text-fill: #818cf8; -fx-font-size: 11; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
            editBtn.setOnAction(e -> showEditPublicationModal(publication));
            
            Button deleteBtn = new Button("Delete 🗑️");
            deleteBtn.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-text-fill: #fca5a5; -fx-font-size: 11; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
            deleteBtn.setOnAction(e -> {
                publicationController.publicationDelete(publication.getIdPublication());
                showListView();
                asyncRefresh();
            });
            
            authorActions.getChildren().addAll(editBtn, deleteBtn);
        }

        // Reaction Bar
        HBox reactionBar = createPublicationReactionBar(publication);

        preview.getChildren().addAll(header, description, imageNode, authorActions, reactionBar);
        return preview;
    }

    private void styleIconButton(Button btn, boolean active) {
        btn.setStyle("-fx-background-color: " + (active ? "rgba(99, 102, 241, 0.25)" : "rgba(255,255,255,0.05)") + "; " +
                     "-fx-text-fill: " + (active ? "#818cf8" : "white") + "; " +
                     "-fx-font-size: 16; -fx-background-radius: 10; -fx-padding: 8; -fx-cursor: hand; " +
                     "-fx-border-color: " + (active ? "rgba(99, 102, 241, 0.4)" : "transparent") + "; -fx-border-width: 1;");
    }

    private HBox createPublicationReactionBar(Publication publication) {
        VBox wrapper = new VBox(12);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setPadding(new Insets(15, 0, 0, 0));
        wrapper.setStyle("-fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 1 0 0 0;");

        ReactionService.ReactionStatus status = reactionController.publicationStatus(publication, currentUser);
        Map<String, Integer> counts = status.getCounts();
        Map<String, Integer> emojiCounts = status.getEmojiCounts();
        
        int likes = counts.getOrDefault("Like", 0);
        int dislikes = counts.getOrDefault("Dislike", 0);
        int total = likes + dislikes;
        double ratio = total > 0 ? (double) likes / total : 0.5;

        // Ratio Bar (Like Horizon/ForumPageView)
        VBox ratioContainer = new VBox(5);
        HBox ratioBar = new HBox(0);
        ratioBar.setPrefHeight(6);
        ratioBar.setMaxWidth(200);
        ratioBar.setStyle("-fx-background-radius: 3; -fx-overflow: hidden;");

        javafx.scene.layout.Region likeRegion = new javafx.scene.layout.Region();
        likeRegion.setStyle("-fx-background-color: #22c55e;");
        likeRegion.setPrefWidth(200 * ratio);
        
        javafx.scene.layout.Region dislikeRegion = new javafx.scene.layout.Region();
        dislikeRegion.setStyle("-fx-background-color: #ef4444;");
        dislikeRegion.setPrefWidth(200 * (1 - ratio));

        ratioBar.getChildren().addAll(likeRegion, dislikeRegion);
        
        Label ratioText = new Label(Math.round(ratio * 100) + "% positive");
        ratioText.setTextFill(Color.color(1, 1, 1, 0.5));
        ratioText.setFont(Font.font(10));
        ratioContainer.getChildren().addAll(ratioBar, ratioText);

        HBox mainActions = new HBox(12);
        mainActions.setAlignment(Pos.CENTER_LEFT);

        boolean isLiked = false;
        boolean isDisliked = false;
        String currentEmoji = null;
        for (ReactionService.ReactionPayload p : status.getReactions()) {
            if ("Like".equals(p.getKind())) isLiked = true;
            if ("Dislike".equals(p.getKind())) isDisliked = true;
            if ("Emoji".equals(p.getKind())) currentEmoji = p.getEmoji();
        }

        Button likeBtn = createReactionToggleBtn("👍 " + likes, isLiked, "#22c55e", () -> {
            reactionController.publicationToggle(publication, currentUser, "Like");
            asyncRefresh();
            showPublicationDetails(publication);
        });

        Button dislikeBtn = createReactionToggleBtn("👎 " + dislikes, isDisliked, "#ef4444", () -> {
            reactionController.publicationToggle(publication, currentUser, "Dislike");
            asyncRefresh();
            showPublicationDetails(publication);
        });

        mainActions.getChildren().addAll(likeBtn, dislikeBtn, ratioContainer);

        // Emoji Cluster with Counts
        javafx.scene.layout.FlowPane emojiCluster = new javafx.scene.layout.FlowPane(10, 10);
        String[] emojis = {"❤️", "😂", "😮", "😢", "😡", "👍", "🔥", "✨"};
        for (String e : emojis) {
            int eCount = emojiCounts.getOrDefault(e, 0);
            boolean active = e.equals(currentEmoji);
            
            Button eBtn = new Button(e + (eCount > 0 ? " " + eCount : ""));
            eBtn.setStyle("-fx-background-color: " + (active ? "rgba(99, 102, 241, 0.5)" : "rgba(255,255,255,0.03)") + "; " +
                          "-fx-font-family: 'Segoe UI Emoji'; " +
                          "-fx-font-size: 14; -fx-padding: 6 10; -fx-background-radius: 10; -fx-cursor: hand; " +
                          "-fx-text-fill: white; -fx-opacity: " + (active ? "1.0" : "0.8") + ";");
            
            eBtn.setOnAction(evt -> {
                reactionController.publicationEmoji(publication, currentUser, e);
                asyncRefresh();
                showPublicationDetails(publication);
            });
            emojiCluster.getChildren().add(eBtn);
        }

        wrapper.getChildren().addAll(mainActions, emojiCluster);
        HBox finalBar = new HBox(wrapper);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return finalBar;
    }

    private Button createReactionToggleBtn(String text, boolean active, String activeColor, Runnable onAction) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + (active ? tm.toRgba(activeColor, 0.2) : "rgba(255,255,255,0.05)") + "; " +
                     "-fx-text-fill: " + (active ? activeColor : "white") + "; " +
                     "-fx-font-weight: bold; -fx-font-size: 12; -fx-padding: 6 12; -fx-background-radius: 8; -fx-cursor: hand; " +
                     "-fx-border-color: " + (active ? tm.toRgba(activeColor, 0.4) : "transparent") + "; -fx-border-width: 1;");
        btn.setOnAction(e -> onAction.run());
        return btn;
    }

    private VBox createPublicationCommentsSection(Publication publication) {
        VBox section = detailCard();
        section.setSpacing(12);

        Label heading = new Label("Comments (" + (commentaireController.commentairesByPublication(publication) != null ? commentaireController.commentairesByPublication(publication).size() : 0) + ")");
        heading.setTextFill(Color.WHITE);
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));

        VBox commentsBox = new VBox(12);
        List<Commentaire> publicationComments = commentaireController.commentairesByPublication(publication);
        if (publicationComments == null || publicationComments.isEmpty()) {
            commentsBox.getChildren().add(emptyText("No comments yet. Be the first to share your thoughts!"));
        } else {
            for (Commentaire comment : publicationComments) {
                commentsBox.getChildren().add(createCommentPreviewItem(comment));
            }
        }

        VBox composeBox = new VBox(10);
        composeBox.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-padding: 15; -fx-background-radius: 12; -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1;");
        
        Label composeLabel = new Label("Write a comment...");
        composeLabel.setTextFill(Color.WHITE);
        composeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));

        TextArea commentInput = new TextArea();
        commentInput.setPromptText("Type your comment here...");
        commentInput.setWrapText(true);
        commentInput.setPrefRowCount(3);
        commentInput.setStyle("-fx-background-color: transparent; -fx-control-inner-background: rgba(0,0,0,0.2); -fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.4); -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: rgba(255,255,255,0.1);");

        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.control.CheckBox visibilityBox = new javafx.scene.control.CheckBox("Show my name");
        visibilityBox.setSelected(commentVisibility);
        visibilityBox.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 11;");
        visibilityBox.setOnAction(e -> commentVisibility = visibilityBox.isSelected());

        Button uploadBtn = new Button("📷 Add Image");
        uploadBtn.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-font-size: 11; -fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
        
        Label fileLabel = new Label("No file chosen");
        fileLabel.setTextFill(Color.color(1, 1, 1, 0.4));
        fileLabel.setFont(Font.font(10));

        uploadBtn.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            java.io.File file = fc.showOpenDialog(root.getScene().getWindow());
            if (file != null) {
                selectedCommentImagePath = file.getName();
                fileLabel.setText("📎 " + file.getName());
                fileLabel.setTextFill(Color.web("#818cf8"));
            }
        });

        HBox.setHgrow(visibilityBox, Priority.ALWAYS);
        controls.getChildren().addAll(visibilityBox, uploadBtn, fileLabel);

        Label feedback = new Label();
        feedback.setTextFill(Color.color(1, 1, 1, 0.6));
        feedback.setFont(Font.font(11));

        Button postBtn = new Button("Post Comment");
        postBtn.setStyle("-fx-padding: 10 20; -fx-background-color: #6366f1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        postBtn.setOnAction(e -> postPublicationComment(publication, commentInput, feedback));

        composeBox.getChildren().addAll(composeLabel, commentInput, controls, postBtn, feedback);
        
        section.getChildren().addAll(heading, commentsBox, new Label(""), composeBox);
        return section;
    }

    private HBox createCommentReactionBar(Commentaire comment) {
        VBox wrapper = new VBox(10);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setPadding(new Insets(10, 0, 0, 0));

        ReactionService.ReactionStatus status = reactionController.commentStatus(comment, currentUser);
        Map<String, Integer> counts = status.getCounts();
        Map<String, Integer> emojiCounts = status.getEmojiCounts();
        
        int likes = counts.getOrDefault("Like", 0);
        int dislikes = counts.getOrDefault("Dislike", 0);
        int total = likes + dislikes;
        double ratio = total > 0 ? (double) likes / total : 0.5;

        // Ratio Bar (Small version for comments)
        HBox ratioBar = new HBox(0);
        ratioBar.setPrefHeight(4);
        ratioBar.setMaxWidth(100);
        ratioBar.setStyle("-fx-background-radius: 2; -fx-overflow: hidden;");

        javafx.scene.layout.Region likeRegion = new javafx.scene.layout.Region();
        likeRegion.setStyle("-fx-background-color: #22c55e;");
        likeRegion.setPrefWidth(100 * ratio);
        
        javafx.scene.layout.Region dislikeRegion = new javafx.scene.layout.Region();
        dislikeRegion.setStyle("-fx-background-color: #ef4444;");
        dislikeRegion.setPrefWidth(100 * (1 - ratio));

        ratioBar.getChildren().addAll(likeRegion, dislikeRegion);

        HBox mainActions = new HBox(10);
        mainActions.setAlignment(Pos.CENTER_LEFT);

        boolean isLiked = false;
        boolean isDisliked = false;
        String currentEmoji = null;
        for (ReactionService.ReactionPayload p : status.getReactions()) {
            if ("Like".equals(p.getKind())) isLiked = true;
            if ("Dislike".equals(p.getKind())) isDisliked = true;
            if ("Emoji".equals(p.getKind())) currentEmoji = p.getEmoji();
        }

        Button likeBtn = createCommentReactionBtn("👍 " + likes, isLiked, "#22c55e", () -> {
            reactionController.commentToggle(comment, currentUser, "Like");
            asyncRefresh();
            publicationController.publicationById(comment.getPublication().getIdPublication()).ifPresent(this::showPublicationDetails);
        });

        Button dislikeBtn = createCommentReactionBtn("👎 " + dislikes, isDisliked, "#ef4444", () -> {
            reactionController.commentToggle(comment, currentUser, "Dislike");
            asyncRefresh();
            publicationController.publicationById(comment.getPublication().getIdPublication()).ifPresent(this::showPublicationDetails);
        });

        mainActions.getChildren().addAll(likeBtn, dislikeBtn, ratioBar);

        // Emoji Cluster
        javafx.scene.layout.FlowPane emojiCluster = new javafx.scene.layout.FlowPane(6, 6);
        String[] emojis = {"❤️", "😂", "😮", "😢", "😡", "👍", "🔥", "✨"};
        for (String e : emojis) {
            int eCount = emojiCounts.getOrDefault(e, 0);
            boolean active = e.equals(currentEmoji);
            
            Button eBtn = new Button(e + (eCount > 0 ? " " + eCount : ""));
            eBtn.setStyle("-fx-background-color: " + (active ? "rgba(99, 102, 241, 0.4)" : "rgba(255,255,255,0.02)") + "; " +
                          "-fx-font-family: 'Segoe UI Emoji'; " +
                          "-fx-font-size: 11; -fx-padding: 4 8; -fx-background-radius: 8; -fx-cursor: hand; " +
                          "-fx-text-fill: white; -fx-opacity: " + (active ? "1.0" : "0.7") + ";");
            
            eBtn.setOnAction(evt -> {
                reactionController.commentEmoji(comment, currentUser, e);
                asyncRefresh();
                publicationController.publicationById(comment.getPublication().getIdPublication()).ifPresent(this::showPublicationDetails);
            });
            emojiCluster.getChildren().add(eBtn);
        }

        wrapper.getChildren().addAll(mainActions, emojiCluster);
        return new HBox(wrapper);
    }

    private Button createCommentReactionBtn(String text, boolean active, String color, Runnable onAction) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + (active ? tm.toRgba(color, 0.15) : "transparent") + "; " +
                     "-fx-text-fill: " + (active ? color : "rgba(255,255,255,0.6)") + "; " +
                     "-fx-font-size: 11; -fx-padding: 4 8; -fx-background-radius: 6; -fx-cursor: hand;");
        btn.setOnAction(e -> onAction.run());
        return btn;
    }

    private VBox createCommentPreviewItem(Commentaire comment) {
        VBox wrap = new VBox(8);
        wrap.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-padding: 12; -fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.06);");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = createCommentAvatarNode(comment);
        
        VBox authorMeta = new VBox(2);
        String authorName = comment.isVisibility() ? author(comment.getUser()) : "Anonymous User";
        Label authorLbl = new Label(authorName);
        authorLbl.setTextFill(Color.WHITE);
        authorLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        
        Label dateLbl = new Label(formatDate(comment.getCreatedAt()));
        dateLbl.setTextFill(Color.color(1, 1, 1, 0.4));
        dateLbl.setFont(Font.font(10));
        authorMeta.getChildren().addAll(authorLbl, dateLbl);
        HBox.setHgrow(authorMeta, Priority.ALWAYS);

        // Comment Actions (Edit/Delete)
        if (currentUser != null && comment.getUser() != null && currentUser.getIdUser().equals(comment.getUser().getIdUser())) {
            Button editBtn = new Button("✏️");
            editBtn.setStyle("-fx-background-color: rgba(99, 102, 241, 0.15); -fx-text-fill: #818cf8; -fx-padding: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12;");
            editBtn.setOnAction(e -> showEditCommentModal(comment));

            Button delBtn = new Button("🗑️");
            delBtn.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-text-fill: #fca5a5; -fx-padding: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12;");
            delBtn.setOnAction(e -> {
                commentaireController.commentaireDelete(comment.getIdCommentaire());
                asyncRefresh();
                publicationController.publicationById(comment.getPublication().getIdPublication())
                    .ifPresent(this::showPublicationDetails);
            });
            header.getChildren().addAll(editBtn, delBtn);
        }

        header.getChildren().add(0, avatar);
        header.getChildren().add(1, authorMeta);

        Label content = new Label(safe(comment.getDescriptionCommentaire(), "—"));
        content.setWrapText(true);
        content.setTextFill(Color.color(1, 1, 1, 0.9));
        content.setFont(Font.font("Segoe UI", 12));
        content.setPadding(new Insets(5, 0, 5, 0));

        HBox actionsBar = createCommentReactionBar(comment);
        wrap.getChildren().addAll(header, content, actionsBar);

        // Comment Image
        if (comment.getImageCommentaire() != null && !comment.getImageCommentaire().isEmpty()) {
            Image img = resolveImage(comment.getImageCommentaire(), "commentaire_images");
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setPreserveRatio(true);
                iv.setFitWidth(200);
                iv.setFitHeight(150);
                StackPane imgFrame = new StackPane(iv);
                imgFrame.setAlignment(Pos.CENTER_LEFT);
                imgFrame.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 5;");
                wrap.getChildren().add(imgFrame);
            }
        }

        return wrap;
    }

    private StackPane createCommentAvatarNode(Commentaire comment) {
        boolean visibleIdentity = comment.isVisibility();
        User commentUser = comment.getUser();

        Circle avatarCircle = new Circle(16);
        avatarCircle.setStroke(Color.color(1, 1, 1, 0.2));
        avatarCircle.setStrokeWidth(1);

        Label initialLabel = new Label(visibleIdentity ? initials(commentUser) : "A");
        initialLabel.setTextFill(Color.WHITE);
        initialLabel.setStyle("-fx-font-size: 11; -fx-font-weight: 700;");

        boolean hasImage = false;
        if (visibleIdentity) {
            hasImage = applyUserAvatarFill(commentUser, avatarCircle);
        }
        if (!hasImage) {
            avatarCircle.setFill(Color.color(1, 1, 1, 0.10));
        }

        initialLabel.setVisible(!hasImage);
        initialLabel.setManaged(!hasImage);

        StackPane avatarWrap = new StackPane(avatarCircle, initialLabel);
        avatarWrap.setMinSize(32, 32);
        avatarWrap.setPrefSize(32, 32);
        return avatarWrap;
    }

    private boolean applyUserAvatarFill(User user, Circle avatarCircle) {
        if (user == null || user.getIdUser() == null) {
            return false;
        }

        Profile profile = profileForUser(user.getIdUser());
        String avatarPath = profile == null ? null : profile.getAvatar();
        if (avatarPath == null || avatarPath.isBlank()) {
            return false;
        }

        Image img = ImageLoaderUtil.loadProfileAvatar(avatarPath, false);
        if (img != null && !img.isError()) {
            avatarCircle.setFill(new ImagePattern(img));
            return true;
        }

        return false;
    }

    private Profile profileForUser(Integer userId) {
        if (userId == null) {
            return null;
        }

        if (profileByUserIdCache.containsKey(userId)) {
            return profileByUserIdCache.get(userId);
        }

        Profile profile = profileController.profileByUserId(userId).orElse(null);
        profileByUserIdCache.put(userId, profile);
        return profile;
    }

    private void postPublicationComment(Publication publication, TextArea input, Label feedback) {
        if (publication == null || publication.getIdPublication() == null) {
            feedback.setTextFill(Color.web("#fca5a5"));
            feedback.setText("Unable to post comment: invalid publication.");
            return;
        }

        if (currentUser == null || currentUser.getIdUser() == null) {
            feedback.setTextFill(Color.web("#fca5a5"));
            feedback.setText("You must be logged in to comment.");
            return;
        }

        String content = input.getText() == null ? "" : input.getText().trim();
        if (content.isEmpty()) {
            feedback.setTextFill(Color.web("#fca5a5"));
            feedback.setText("Comment cannot be empty.");
            return;
        }

        Integer createdId = commentaireController.commentaireCreate(content, selectedCommentImagePath, commentVisibility, publication, currentUser);
        if (createdId != null && createdId > 0) {
            input.clear();
            selectedCommentImagePath = null; // Reset image
            feedback.setTextFill(Color.web("#86efac"));
            feedback.setText("Comment posted successfully!");
            refreshDataAndRender();
            showPublicationDetails(publication);
            return;
        }

        feedback.setTextFill(Color.web("#fca5a5"));
        feedback.setText("Failed to post comment.");
    }

    private void showCommentDetails(Commentaire comment) {
        detailsTitle.setText("Comment Details");
        detailsContent.getChildren().clear();

        Publication publication = resolvePublication(comment.getPublication());

        VBox card = detailCard();
        card.getChildren().addAll(
            detailLine("Comment", safe(comment.getDescriptionCommentaire(), "—")),
            detailLine("Publication", publication == null ? "—" : safe(publication.getTitrePub(), "—")),
            detailLine("Category", publication == null ? "—" : safe(publication.getCategoriePub(), "—")),
            detailLine("Date", formatDate(comment.getCreatedAt()))
        );

        detailsContent.getChildren().add(card);
        animateSwitcherFace(false);
    }

    private void showReactionDetails(Reaction reaction) {
        detailsTitle.setText("Reaction Details");
        detailsContent.getChildren().clear();

        Publication publication = resolvePublication(reaction.getPublication());
        if (publication == null && reaction.getCommentaire() != null) {
            publication = resolvePublication(reaction.getCommentaire().getPublication());
        }

        VBox card = detailCard();
        card.getChildren().addAll(
            detailLine("Type", safe(reaction.getKind(), "—")),
            detailLine("Emoji", safe(reaction.getEmoji(), "—")),
            detailLine("Target publication", publication == null ? "—" : safe(publication.getTitrePub(), "—")),
            detailLine("Date", formatDate(reaction.getUpdatedAt()))
        );

        detailsContent.getChildren().add(card);
        animateSwitcherFace(false);
    }

    private void showBookmarkDetails(Reaction bookmark) {
        detailsTitle.setText("Bookmark Details");
        detailsContent.getChildren().clear();

        Publication publication = resolvePublication(bookmark.getPublication());
        if (publication == null && bookmark.getCommentaire() != null) {
            publication = resolvePublication(bookmark.getCommentaire().getPublication());
        }

        VBox card = detailCard();
        card.getChildren().addAll(
            detailLine("Type", "Bookmark"),
            detailLine("Publication", publication == null ? "—" : safe(publication.getTitrePub(), "—")),
            detailLine("Category", publication == null ? "—" : safe(publication.getCategoriePub(), "—")),
            detailLine("Date", formatDate(bookmark.getUpdatedAt()))
        );

        detailsContent.getChildren().add(card);
        animateSwitcherFace(false);
    }

    private VBox detailCard() {
        VBox detailCard = new VBox(12);
        detailCard.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-padding: 16;");
        return detailCard;
    }

    private VBox detailLine(String key, String value) {
        VBox line = new VBox(4);
        Label k = new Label(key);
        k.setTextFill(Color.color(1, 1, 1, 0.55));
        k.setFont(Font.font("Segoe UI", 10));

        Label v = new Label(value);
        v.setTextFill(Color.WHITE);
        v.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        v.setWrapText(true);

        line.getChildren().addAll(k, v);
        return line;
    }

    private void showListView() {
        animateSwitcherFace(true);
    }

    private void animateSwitcherFace(boolean showList) {
        VBox toShow = showList ? faceListView : faceDetailsView;
        VBox toHide = showList ? faceDetailsView : faceListView;

        FadeTransition fadeOut = new FadeTransition(Duration.millis(220), toHide);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            toHide.setVisible(false);
            toHide.setManaged(false);
        });

        toShow.setVisible(true);
        toShow.setManaged(true);
        toShow.setOpacity(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(240), toShow);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        fadeOut.play();
        fadeIn.play();
    }

    private boolean isAdminUser(User user) {
        if (user == null || user.getRoleUser() == null) {
            return false;
        }
        return "ADMIN".equalsIgnoreCase(user.getRoleUser()) || "OWNER".equalsIgnoreCase(user.getRoleUser());
    }

    private String formatDate(LocalDateTime value) {
        if (value == null) {
            return "—";
        }
        return value.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private String trimTo(String value, int max) {
        if (value == null) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }

    private String author(User user) {
        if (user == null) {
            return "Unknown";
        }

        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getLastName() == null ? "" : user.getLastName();
        String full = (first + " " + last).trim();

        if (!full.isBlank()) {
            return full;
        }
        return user.getEmailUser() == null ? "Unknown" : user.getEmailUser();
    }

    private String initials(User user) {
        if (user == null) {
            return "U";
        }

        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();

        StringBuilder result = new StringBuilder();
        if (!first.isEmpty()) {
            result.append(Character.toUpperCase(first.charAt(0)));
        }
        if (!last.isEmpty()) {
            result.append(Character.toUpperCase(last.charAt(0)));
        }

        if (result.length() == 0) {
            return "U";
        }
        return result.toString();
    }

    private Image resolveImage(String dbValue, String fallbackFolder) {
        if (dbValue == null || dbValue.isBlank()) {
            return null;
        }

        String path = dbValue.trim();
        
        // Try exact path first (already includes uploads/ or is a URL)
        Image img = ImageLoaderUtil.loadImage(path);
        if (img != null) return img;

        // Try with uploads/ prefix
        if (!path.startsWith("uploads/") && !path.startsWith("uploads\\")) {
            img = ImageLoaderUtil.loadImage("uploads/" + path);
            if (img != null) return img;
        }

        // Try with folder specific paths
        if (fallbackFolder != null) {
            img = ImageLoaderUtil.loadImage("uploads/" + fallbackFolder + "/" + path);
            if (img != null) return img;
        }

        // Common forum locations fallback
        img = ImageLoaderUtil.loadImage("uploads/forum_images/" + path);
        if (img != null) return img;
        
        img = ImageLoaderUtil.loadImage("uploads/commentaire_images/" + path);
        if (img != null) return img;

        return null;
    }

    private void asyncRefresh() {
        new Thread(this::refreshDataAndRender).start();
    }

    private void showEditPublicationModal(Publication pub) {
        javafx.stage.Stage stage = new javafx.stage.Stage(javafx.stage.StageStyle.TRANSPARENT);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        
        VBox layout = new VBox(20);
        layout.setStyle("-fx-background-color: #1e1e24; -fx-padding: 30; -fx-background-radius: 20; -fx-border-color: #3f3f46; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 20, 0, 0, 10);");
        layout.setPrefWidth(500);
        layout.setAlignment(Pos.CENTER);

        Label titleLbl = new Label("Update Your Story");
        titleLbl.setTextFill(Color.WHITE);
        titleLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        VBox form = new VBox(15);
        
        TextField titleField = new TextField(pub.getTitrePub());
        titleField.setPromptText("Post Title");
        titleField.setStyle("-fx-background-color: #09090b; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8;");

        ComboBox<String> catBox = new ComboBox<>();
        catBox.getItems().addAll("Announcement", "Suggestion", "Jeux Video", "Informatique", "Nouveauté", "Discussion General", "Culture", "Sport");
        catBox.setValue(pub.getCategoriePub() == null ? "Discussion General" : pub.getCategoriePub());
        catBox.setMaxWidth(Double.MAX_VALUE);
        catBox.setStyle("-fx-background-color: #09090b; -fx-text-fill: white; -fx-background-radius: 8;");

        TextArea descArea = new TextArea(pub.getDescriptionPub());
        descArea.setPromptText("Description...");
        descArea.setWrapText(true);
        descArea.setPrefRowCount(6);
        descArea.setStyle("-fx-control-inner-background: #09090b; -fx-text-fill: white; -fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8;");

        // Image Selection Zone
        HBox imageZone = new HBox(15);
        imageZone.setAlignment(Pos.CENTER_LEFT);
        
        String[] currentImgPath = { pub.getImagePub() };
        ImageView preview = new ImageView();
        preview.setFitWidth(80);
        preview.setFitHeight(80);
        preview.setPreserveRatio(true);
        if (pub.getImagePub() != null) {
            Image existing = resolveImage(pub.getImagePub(), "forum_images");
            if (existing != null) preview.setImage(existing);
        }

        Button selectImgBtn = new Button("📷 Change Image");
        selectImgBtn.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 15;");
        selectImgBtn.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            java.io.File file = fc.showOpenDialog(stage);
            if (file != null) {
                try {
                    // Actual "upload" - copy to forum_images
                    File uploadsDir = new File(System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "forum_images");
                    if (!uploadsDir.exists()) uploadsDir.mkdirs();
                    
                    File dest = new File(uploadsDir, file.getName());
                    java.nio.file.Files.copy(file.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    
                    currentImgPath[0] = file.getName();
                    preview.setImage(new Image(dest.toURI().toString()));
                } catch (Exception ex) {
                    System.err.println("Upload failed: " + ex.getMessage());
                }
            }
        });

        imageZone.getChildren().addAll(preview, selectImgBtn);
        form.getChildren().addAll(titleField, catBox, descArea, imageZone);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button saveBtn = new Button("Save Changes");
        saveBtn.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 10; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            publicationController.publicationUpdate(pub.getIdPublication(), titleField.getText(), descArea.getText(), catBox.getValue(), currentImgPath[0]);
            stage.close();
            asyncRefresh();
            // Small delay to let DB update
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
            pause.setOnFinished(evt -> publicationController.publicationById(pub.getIdPublication()).ifPresent(this::showPublicationDetails));
            pause.play();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a1a1aa; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> stage.close());

        actions.getChildren().addAll(cancelBtn, saveBtn);

        layout.getChildren().addAll(titleLbl, form, actions);
        
        javafx.scene.Scene scene = new javafx.scene.Scene(layout);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.show();
    }

    private void showEditCommentModal(Commentaire comment) {
        javafx.stage.Stage stage = new javafx.stage.Stage(javafx.stage.StageStyle.TRANSPARENT);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        
        VBox layout = new VBox(20);
        layout.setStyle("-fx-background-color: #1e1e24; -fx-padding: 30; -fx-background-radius: 20; -fx-border-color: #3f3f46; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 20, 0, 0, 10);");
        layout.setPrefWidth(450);
        layout.setAlignment(Pos.CENTER);

        Label titleLbl = new Label("Update Comment");
        titleLbl.setTextFill(Color.WHITE);
        titleLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));

        VBox form = new VBox(15);
        
        TextArea descArea = new TextArea(comment.getDescriptionCommentaire());
        descArea.setPromptText("Write your thoughts...");
        descArea.setWrapText(true);
        descArea.setPrefRowCount(4);
        descArea.setStyle("-fx-control-inner-background: #09090b; -fx-text-fill: white; -fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 8;");

        // Visibility Toggle
        javafx.scene.control.CheckBox visCheck = new javafx.scene.control.CheckBox("Visible Identity (Show my name)");
        visCheck.setSelected(comment.isVisibility());
        visCheck.setStyle("-fx-text-fill: #a1a1aa; -fx-font-size: 12;");

        // Image Selection Zone
        HBox imageZone = new HBox(15);
        imageZone.setAlignment(Pos.CENTER_LEFT);
        
        String[] currentImgPath = { comment.getImageCommentaire() };
        ImageView preview = new ImageView();
        preview.setFitWidth(60);
        preview.setFitHeight(60);
        preview.setPreserveRatio(true);
        if (comment.getImageCommentaire() != null) {
            Image existing = resolveImage(comment.getImageCommentaire(), "commentaire_images");
            if (existing != null) preview.setImage(existing);
        }

        Button selectImgBtn = new Button("📷 Change Photo");
        selectImgBtn.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 6 12; -fx-font-size: 11;");
        selectImgBtn.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            java.io.File file = fc.showOpenDialog(stage);
            if (file != null) {
                try {
                    File uploadsDir = new File(System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "commentaire_images");
                    if (!uploadsDir.exists()) uploadsDir.mkdirs();
                    File dest = new File(uploadsDir, file.getName());
                    java.nio.file.Files.copy(file.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    currentImgPath[0] = file.getName();
                    preview.setImage(new Image(dest.toURI().toString()));
                } catch (Exception ex) {
                    System.err.println("Comment Image Upload failed: " + ex.getMessage());
                }
            }
        });

        imageZone.getChildren().addAll(preview, selectImgBtn);
        form.getChildren().addAll(descArea, visCheck, imageZone);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button saveBtn = new Button("Update");
        saveBtn.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 8; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            commentaireController.commentaireUpdate(comment.getIdCommentaire(), descArea.getText(), currentImgPath[0], visCheck.isSelected());
            stage.close();
            asyncRefresh();
            publicationController.publicationById(comment.getPublication().getIdPublication()).ifPresent(this::showPublicationDetails);
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a1a1aa; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> stage.close());

        actions.getChildren().addAll(cancelBtn, saveBtn);

        layout.getChildren().addAll(titleLbl, form, actions);
        
        javafx.scene.Scene scene = new javafx.scene.Scene(layout);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.show();
    }

    public VBox getRoot() {
        return root;
    }

    public void cleanup() {
        // Cleanup resources if needed.
    }
}
