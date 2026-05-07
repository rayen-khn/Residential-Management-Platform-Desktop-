package com.syndicati.views.frontend.services;

import com.syndicati.MainApplication;
import com.syndicati.controllers.forum.CommentaireController;
import com.syndicati.controllers.forum.PublicationController;
import com.syndicati.controllers.forum.ReactionController;
import com.syndicati.controllers.user.profile.ProfileController;
import com.syndicati.interfaces.ViewInterface;
import com.syndicati.models.forum.Commentaire;
import com.syndicati.models.forum.Publication;
import com.syndicati.models.user.Profile;
import com.syndicati.models.user.User;
import com.syndicati.services.forum.OpenAIModerationService;
import com.syndicati.services.forum.SentimentAnalysisService;
import com.syndicati.services.forum.SentimentAnalysisService.SentimentResult;
import com.syndicati.services.forum.DiscordWebhookService;
import com.syndicati.services.forum.ReactionService.ReactionActionResult;
import com.syndicati.services.forum.ReactionService.ReactionPayload;
import com.syndicati.services.forum.ReactionService.ReactionStatus;
import com.syndicati.utils.image.ImageLoaderUtil;
import com.syndicati.utils.session.SessionManager;
import com.syndicati.utils.theme.ThemeManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.util.Duration;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.Cursor;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class ForumPageView implements ViewInterface {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final DateTimeFormatter SHORT_DATE_FMT = DateTimeFormatter.ofPattern("MMM dd");
    private static final List<String> CATEGORIES = List.of(
            "Announcement", "Suggestion", "Jeux Video", "Informatique", "Nouveauté", "Discussion General", "Culture",
            "Sport");
    private static final List<String> MODERATOR_ROLES = List.of("OWNER", "ADMIN", "SUPERADMIN", "SYNDIC");
    private static final List<String> EMOJIS = List.of("❤️", "😂", "😮", "😢", "😡", "👍", "🔥", "✨");

    private final StackPane root = new StackPane();
    private final VBox mainLayout = new VBox(18);
    private final VBox notificationBox = new VBox(10);
    private final ThemeManager tm = ThemeManager.getInstance();
    private final SessionManager session = SessionManager.getInstance();
    private final PublicationController publications = new PublicationController();
    private final CommentaireController comments = new CommentaireController();
    private final ReactionController reactions = new ReactionController();
    private final ProfileController profiles = new ProfileController();
    private final OpenAIModerationService moderationService = new OpenAIModerationService();
    private final SentimentAnalysisService sentimentService = new SentimentAnalysisService();
    private final DiscordWebhookService discordService = new DiscordWebhookService();

    private final VBox listBox = new VBox(10);
    private final StackPane faceStack = new StackPane();
    private final Map<Integer, VBox> listItemsById = new HashMap<>();

    private final VBox readFace = new VBox();
    private final VBox createFace = new VBox();
    private final VBox editFace = new VBox();

    private final StackPane detailHero = new StackPane();
    private final ImageView detailHeroImage = new ImageView();
    private final Rectangle detailHeroClip = new Rectangle();
    private Image currentHeroImage;

    private final Text heroTitle = new Text("Welcome to the Forum");
    private final Text heroMeta = new Text("");
    private final Text heroBody = new Text("Select a publication from the right panel.");
    private final Text heroCategory = new Text("Discussion General");
    private final HBox ownerActions = new HBox(8);

    private final Button postLikeButton = new Button("Like");
    private final Button postDislikeButton = new Button("Dislike");
    private final Button postEmojiButton = new Button("Emoji");
    private final Button postBookmarkButton = new Button("Bookmark");
    private final Button postReportButton = new Button("Report");
    private Button feelingButton;
    private final Label postLikeCount = new Label("");
    private final Label postDislikeCount = new Label("");
    private final Label postRatioText = new Label("0%");
    private Region postLikesBarRegion;
    private Region postDislikesBarRegion;

    private final VBox commentsBox = new VBox(10);
    private final VBox commentFormContainer = new VBox(10);
    private final Label announcementHint = new Label("Comments are disabled for this announcement.");
    private final VBox publicationReportPanel = new VBox(8);
    private final VBox publicationEmojiPanel = new VBox(8);
    private final Map<Integer, VBox> commentEditPanels = new HashMap<>();
    private final Map<Integer, VBox> commentReportPanels = new HashMap<>();
    private final Map<Integer, VBox> commentEmojiPanels = new HashMap<>();
    private final Map<Integer, Profile> profileByUserIdCache = new HashMap<>();
    private Commentaire currentCommentForInline;

    private final TextField createTitle = new TextField();
    private final ComboBox<String> createCategory = new ComboBox<>();
    private final TextArea createDescription = new TextArea();
    private final Label createTitleValidation = new Label();
    private final Label createCategoryValidation = new Label();
    private final Label createDescriptionValidation = new Label();
    private final TextField createImageField = new TextField();
    private final ImageView createImagePreview = new ImageView();
    private File createImage;
    private boolean forceAnnouncementCreate;

    private final TextField editTitle = new TextField();
    private final ComboBox<String> editCategory = new ComboBox<>();
    private final TextArea editDescription = new TextArea();
    private final Label editTitleValidation = new Label();
    private final Label editCategoryValidation = new Label();
    private final Label editDescriptionValidation = new Label();
    private final TextField editImageField = new TextField();
    private final ImageView editCurrentImagePreview = new ImageView();
    private final ImageView editNewImagePreview = new ImageView();
    private File editImage;

    private final TextArea commentInput = new TextArea();
    private final Label commentValidation = new Label();
    private boolean isCommentPublic = true;
    private final TextField commentImageField = new TextField();
    private final List<File> commentImages = new ArrayList<>();
    private final FlowPane commentImagePreviews = new FlowPane();

    private final Button filterGeneral = new Button("General");
    private final Button filterAnnouncements = new Button("Announcements");

    private Publication current;
    private String currentFilter = "General";

    public ForumPageView() {
        mainLayout.setPadding(new Insets(24));
        mainLayout.setStyle("-fx-background-color: transparent;");
        mainLayout.setAlignment(Pos.TOP_CENTER);

        notificationBox.setPickOnBounds(false);
        notificationBox.setAlignment(Pos.TOP_RIGHT);
        notificationBox.setPadding(new Insets(20));
        notificationBox.setSpacing(10);
        notificationBox.setPrefWidth(400);
        notificationBox.setMaxWidth(Region.USE_PREF_SIZE);

        mainLayout.getChildren().addAll(buildHero(), buildSplit());
        root.getChildren().addAll(mainLayout, notificationBox);

        showFace(readFace);
        loadCategory("General");
    }

    @Override
    public StackPane getRoot() {
        return root;
    }

    @Override
    public void cleanup() {
    }

    private Node buildHero() {
        VBox hero = new VBox(12);
        hero.setPadding(new Insets(34));
        hero.setMaxWidth(1800);
        hero.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #090909 0%, #131313 100%);" +
                        "-fx-background-radius: 48px;" +
                        "-fx-border-color: rgba(255,255,255,0.08);" +
                        "-fx-border-radius: 48px;");

        Label badge = new Label("COMMUNITY HUB");
        badge.setStyle(
                "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.14) + ";" +
                        "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.45) + ";" +
                        "-fx-border-radius: 100px;" +
                        "-fx-background-radius: 100px;" +
                        "-fx-padding: 7 16 7 16;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: 800;" +
                        "-fx-text-fill: white;");

        Text title = new Text("Voices of Horizon");
        title.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BLACK, 66));
        title.setFill(Color.WHITE);

        Text subtitle = new Text(
                "Join the conversation. Connect with neighbors, share ideas, and shape your community.");
        subtitle.setWrappingWidth(840);
        subtitle.setFont(Font.font(MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 17));
        subtitle.setFill(Color.web("rgba(255,255,255,0.68)"));

        hero.getChildren().addAll(badge, title, subtitle);
        return hero;
    }

    private Node buildSplit() {
        HBox split = new HBox();
        split.setMinWidth(0);
        split.setMaxWidth(1800);
        split.setPrefHeight(860);
        split.setStyle(
                "-fx-background-color: #0a0a0c;" +
                        "-fx-background-radius: 32px;" +
                        "-fx-border-color: rgba(255,255,255,0.08);" +
                        "-fx-border-radius: 32px;");

        VBox main = buildMainPanel();
        VBox side = buildSidePanel();
        side.setPrefWidth(360);
        side.setMaxWidth(360);
        side.setMinWidth(360);

        main.setMinWidth(0);
        main.prefWidthProperty().bind(
                Bindings.max(0, split.widthProperty().subtract(side.widthProperty())));
        HBox.setHgrow(main, Priority.ALWAYS);
        HBox.setHgrow(side, Priority.NEVER);
        split.getChildren().addAll(main, side);
        return split;
    }

    private VBox buildMainPanel() {
        VBox panel = new VBox();
        panel.setMinWidth(0);
        panel.setPadding(new Insets(0));
        panel.setStyle(
                "-fx-border-color: transparent rgba(255,255,255,0.10) transparent transparent; -fx-border-width: 0 1px 0 0;");
        panel.setMaxWidth(Double.MAX_VALUE);

        Rectangle panelClip = new Rectangle();
        panelClip.widthProperty().bind(panel.widthProperty());
        panelClip.heightProperty().bind(panel.heightProperty());
        panel.setClip(panelClip);

        faceStack.getChildren().addAll(readFace, createFace, editFace);
        buildReadFace();
        buildCreateFace();
        buildEditFace();

        VBox.setVgrow(faceStack, Priority.ALWAYS);
        panel.getChildren().add(faceStack);
        return panel;
    }

    private void buildReadFace() {
        readFace.getChildren().clear();
        readFace.setMinWidth(0);
        readFace.setMaxWidth(Double.MAX_VALUE);
        readFace.setPrefWidth(Region.USE_COMPUTED_SIZE);

        detailHero.setPrefHeight(320);
        detailHero.setMinHeight(320);
        detailHero.setMaxHeight(320);
        detailHero.setMinWidth(0);
        detailHero.setMaxWidth(Double.MAX_VALUE);
        detailHero.setClip(detailHeroClip);
        detailHero.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #1a1a2e 0%, #16213e 100%); -fx-border-radius: 24px 0px 0px 24px;");

        detailHeroClip.setArcWidth(24);
        detailHeroClip.setArcHeight(24);
        detailHeroClip.widthProperty().bind(detailHero.widthProperty());
        detailHeroClip.heightProperty().bind(detailHero.heightProperty());

        detailHeroImage.setSmooth(true);
        detailHeroImage.setPreserveRatio(false);
        detailHero.widthProperty().addListener((obs, oldV, newV) -> refreshHeroViewport());
        detailHero.heightProperty().addListener((obs, oldV, newV) -> refreshHeroViewport());

        heroTitle.setFill(Color.WHITE);
        heroTitle.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 42));
        heroMeta.setFill(Color.web("rgba(255,255,255,0.72)"));
        heroCategory.setFill(Color.web("rgba(255,255,255,0.95)"));
        heroBody.setFill(Color.web("rgba(255,255,255,0.90)"));
        heroBody.wrappingWidthProperty().bind(detailHero.widthProperty().subtract(52));

        VBox heroOverlay = new VBox(10);
        heroOverlay.setPadding(new Insets(28));
        heroOverlay.setAlignment(Pos.BOTTOM_LEFT);
        heroOverlay.setStyle(
                "-fx-background-color: linear-gradient(from 0% 100% to 0% 0%, rgba(20,20,30,0.94) 3%, rgba(20,20,30,0.08) 100%);");

        Label categoryPill = new Label();
        categoryPill.textProperty().bind(heroCategory.textProperty());
        categoryPill.setStyle(categoryStyle(heroCategory.getText()));
        heroCategory.textProperty().addListener((obs, oldVal, newVal) -> categoryPill.setStyle(categoryStyle(newVal)));

        heroOverlay.getChildren().addAll(categoryPill, heroMeta, heroTitle);
        detailHero.getChildren().setAll(detailHeroImage, heroOverlay);
        StackPane.setAlignment(heroOverlay, Pos.BOTTOM_LEFT);

        FlowPane reactionCluster = buildPublicationReactionCluster();

        BorderPane actionsBar = new BorderPane();
        actionsBar.setMinWidth(0);
        actionsBar.setPadding(new Insets(12));
        actionsBar.setPrefWidth(Region.USE_COMPUTED_SIZE);
        actionsBar.setMaxWidth(Double.MAX_VALUE);
        actionsBar.setStyle(
                "-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 14px; -fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 14px;");

        Rectangle actionsBarClip = new Rectangle();
        actionsBarClip.widthProperty().bind(actionsBar.widthProperty());
        actionsBarClip.heightProperty().bind(actionsBar.heightProperty());
        actionsBar.setClip(actionsBarClip);

        Button createSwitcher = ghostButton("New Post", () -> openCreateFace(false));
        Button editSwitcher = ghostButton("Edit", this::openEditFace);
        Button deleteSwitcher = ghostButton("Delete", this::deleteCurrentPublication);
        feelingButton = ghostButton("Feeling", this::showSentimentAnalysis);

        ownerActions.getChildren().setAll(editSwitcher, deleteSwitcher);
        ownerActions.setManaged(false);
        ownerActions.setVisible(false);

        VBox reactionWrap = new VBox(reactionCluster);
        reactionWrap.setMinWidth(0);
        reactionWrap.setMaxWidth(Double.MAX_VALUE);
        BorderPane.setAlignment(reactionWrap, Pos.CENTER_LEFT);

        postReportButton.setMinWidth(Region.USE_PREF_SIZE);
        postReportButton.setMaxWidth(Region.USE_PREF_SIZE);

        HBox rightActions = new HBox(8, ownerActions, feelingButton, postReportButton);
        rightActions.setAlignment(Pos.CENTER_RIGHT);
        rightActions.setMinWidth(Region.USE_PREF_SIZE);
        rightActions.setMaxWidth(Region.USE_PREF_SIZE);
        BorderPane.setAlignment(rightActions, Pos.CENTER_RIGHT);

        reactionCluster.setMinWidth(0);
        reactionCluster.prefWrapLengthProperty().bind(
                actionsBar.widthProperty().subtract(rightActions.widthProperty()).subtract(40));

        actionsBar.setLeft(reactionWrap);
        actionsBar.setRight(rightActions);

        feelingButton.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                        "-fx-border-color: rgba(255,255,255,0.15);" +
                        "-fx-border-radius: 12px; -fx-background-radius: 12px;" +
                        "-fx-text-fill: white; -fx-padding: 8 14 8 14;");
        feelingButton.setOnAction(e -> showSentimentAnalysis());

        VBox publicationBody = new VBox(10, heroBody, actionsBar);
        publicationBody.setPadding(new Insets(22, 0, 10, 0));
        publicationBody.setMinWidth(0);
        publicationBody.setMaxWidth(Double.MAX_VALUE);

        buildPublicationReportPanel();
        buildPublicationEmojiPanel();
        VBox inlinePanels = new VBox(8, publicationReportPanel, publicationEmojiPanel);
        inlinePanels.setPadding(new Insets(0, 0, 0, 0));
        inlinePanels.setFillWidth(true);
        inlinePanels.setMaxWidth(Double.MAX_VALUE);

        VBox commentsSection = buildCommentsSection();
        VBox.setMargin(commentsSection, new Insets(20, 0, 0, 0));
        commentsSection.setMaxWidth(Double.MAX_VALUE);

        VBox content = new VBox(publicationBody, inlinePanels, commentsSection);
        content.setPadding(new Insets(0, 26, 26, 26));
        content.setMinWidth(0);
        content.setFillWidth(true);
        content.setMaxWidth(Double.MAX_VALUE);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setMinWidth(0);
        scroll.setMaxWidth(Double.MAX_VALUE);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Hard clip to prevent content from overflowing behind sidebar
        Rectangle scrollClip = new Rectangle();
        scrollClip.widthProperty().bind(scroll.widthProperty());
        scrollClip.heightProperty().bind(scroll.heightProperty());
        scroll.setClip(scrollClip);

        // Force content to fit scroll pane width
        content.prefWidthProperty().bind(scroll.widthProperty().subtract(52));
        content.setMaxWidth(Double.MAX_VALUE);

        readFace.getChildren().addAll(detailHero, scroll);
    }

    private FlowPane buildPublicationReactionCluster() {
        styleReactionButton(postLikeButton);
        styleReactionButton(postDislikeButton);
        styleReactionButton(postEmojiButton);
        styleReactionButton(postBookmarkButton);
        styleReactionButton(postReportButton);

        postLikeButton.setOnAction(e -> togglePublicationReaction("Like"));
        postDislikeButton.setOnAction(e -> togglePublicationReaction("Dislike"));
        postEmojiButton.setOnAction(e -> togglePublicationEmojiPanel());
        postBookmarkButton.setOnAction(e -> togglePublicationReaction("Bookmark"));
        postReportButton.setOnAction(e -> togglePublicationReportPanel());

        HBox likeWrap = reactionCountWrap(postLikeButton, postLikeCount);
        HBox dislikeWrap = reactionCountWrap(postDislikeButton, postDislikeCount);

        // Custom Ratio Bar Container (Pill shape)
        HBox ratioBarContainer = new HBox();
        ratioBarContainer.setPrefSize(110, 6);
        ratioBarContainer.setMaxSize(110, 6);
        ratioBarContainer.setAlignment(Pos.CENTER_LEFT);
        ratioBarContainer.setStyle("-fx-background-radius: 100px; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 100px; -fx-border-width: 1; -fx-overflow: hidden;");
        
        // Clip to ensure rounded corners work for internal children
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(110, 6);
        clip.setArcWidth(6);
        clip.setArcHeight(6);
        ratioBarContainer.setClip(clip);

        // Green Part (Likes)
        Region likesBar = new Region();
        likesBar.setStyle("-fx-background-color: #22c55e;");
        likesBar.setPrefHeight(6);
        
        // Red Part (Dislikes)
        Region dislikesBar = new Region();
        dislikesBar.setStyle("-fx-background-color: #ef4444;");
        dislikesBar.setPrefHeight(6);
        
        ratioBarContainer.getChildren().addAll(likesBar, dislikesBar);
        
        // Store references to update later
        this.postLikesBarRegion = likesBar; 
        this.postDislikesBarRegion = dislikesBar;
        
        // Store references to update later
        this.postLikesBarRegion = likesBar; 
        
        postRatioText.setTextFill(Color.web("rgba(255,255,255,0.72)"));
        postRatioText.setStyle("-fx-font-size: 11px; -fx-font-weight: 700;");

        HBox ratioWrap = new HBox(8, ratioBarContainer, postRatioText);
        ratioWrap.setAlignment(Pos.CENTER_LEFT);

        FlowPane cluster = new FlowPane();
        cluster.setHgap(8);
        cluster.setVgap(8);
        cluster.getChildren().addAll(likeWrap, dislikeWrap, ratioWrap, postEmojiButton, postBookmarkButton);
        cluster.setAlignment(Pos.CENTER_LEFT);
        return cluster;
    }

    private void buildCreateFace() {
        createFace.getChildren().clear();

        VBox card = new VBox(12);
        card.setPadding(new Insets(22));
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 16px; -fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 16px;");

        Text heading = new Text("Create Publication");
        heading.setFill(Color.WHITE);
        heading.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 24));

        createTitle.setPromptText("Title");
        createCategory.getItems().setAll(CATEGORIES);
        createCategory.setValue("Discussion General");
        createDescription.setPromptText("Description");
        createDescription.setPrefRowCount(8);
        createImageField.setPromptText("Optional image");
        createImageField.setEditable(false);

        tuneInput(createTitle);
        tuneInput(createDescription);
        tuneInput(createCategory);
        styleValidationLabel(createTitleValidation, "Title must be at least 5 characters", false);
        styleValidationLabel(createCategoryValidation, "Category is required", false);
        styleValidationLabel(createDescriptionValidation, "Description must be at least 10 characters", false);

        createTitle.textProperty().addListener((obs, oldValue, newValue) -> updateCreateValidationState());
        createCategory.valueProperty().addListener((obs, oldValue, newValue) -> updateCreateValidationState());
        createDescription.textProperty().addListener((obs, oldValue, newValue) -> updateCreateValidationState());

        preparePreview(createImagePreview, 130, 130);

        Label createFileLabel = new Label("No image selected");
        createFileLabel.setTextFill(Color.web("rgba(255,255,255,0.65)"));
        createFileLabel.setStyle("-fx-font-size: 10px;");

        HBox imageRow = buildAttachmentZone(
                () -> {
                    File file = chooseImage();
                    if (file != null) {
                        createImage = file;
                        createImageField.setText(file.getName());
                        createFileLabel.setText(file.getName());
                        createImagePreview.setImage(new Image(file.toURI().toString(), true));
                    }
                },
                () -> {
                    createImage = null;
                    createImageField.clear();
                    createFileLabel.setText("No image selected");
                    createImagePreview.setImage(null);
                },
                createFileLabel);

        HBox previewRow = new HBox(8, createImagePreview);

        HBox actions = new HBox(8,
                primaryButton("Publish", this::submitPublication),
                ghostButton("Back", () -> showFace(readFace)));

        card.getChildren().addAll(
                heading,
                createTitle,
                createTitleValidation,
                createCategory,
                createCategoryValidation,
                createDescription,
                createDescriptionValidation,
                imageRow,
                previewRow,
                actions);

        updateCreateValidationState();

        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        createFace.getChildren().add(scroll);
    }

    private void buildEditFace() {
        editFace.getChildren().clear();

        VBox card = new VBox(12);
        card.setPadding(new Insets(22));
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 16px; -fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 16px;");

        Text heading = new Text("Edit Publication");
        heading.setFill(Color.WHITE);
        heading.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 24));

        editTitle.setPromptText("Title");
        editCategory.getItems().setAll(CATEGORIES);
        editDescription.setPromptText("Description");
        editDescription.setPrefRowCount(8);
        editImageField.setPromptText("Optional new image");
        editImageField.setEditable(false);

        tuneInput(editTitle);
        tuneInput(editDescription);
        tuneInput(editCategory);
        styleValidationLabel(editTitleValidation, "Title must be at least 5 characters", false);
        styleValidationLabel(editCategoryValidation, "Category is required", false);
        styleValidationLabel(editDescriptionValidation, "Description must be at least 10 characters", false);

        editTitle.textProperty().addListener((obs, oldValue, newValue) -> updateEditValidationState());
        editCategory.valueProperty().addListener((obs, oldValue, newValue) -> updateEditValidationState());
        editDescription.textProperty().addListener((obs, oldValue, newValue) -> updateEditValidationState());

        preparePreview(editCurrentImagePreview, 120, 120);
        preparePreview(editNewImagePreview, 120, 120);

        Label editFileLabel = new Label("No new image selected");
        editFileLabel.setTextFill(Color.web("rgba(255,255,255,0.65)"));
        editFileLabel.setStyle("-fx-font-size: 10px;");

        HBox imageRow = buildAttachmentZone(
                () -> {
                    File file = chooseImage();
                    if (file != null) {
                        editImage = file;
                        editImageField.setText(file.getName());
                        editFileLabel.setText(file.getName());
                        editNewImagePreview.setImage(new Image(file.toURI().toString(), true));
                    }
                },
                () -> {
                    editImage = null;
                    editImageField.clear();
                    editFileLabel.setText("No new image selected");
                    editNewImagePreview.setImage(null);
                },
                editFileLabel);

        VBox currentWrap = new VBox(4, dimLabel("Current image"), editCurrentImagePreview);
        VBox newWrap = new VBox(4, dimLabel("New image"), editNewImagePreview);
        HBox previews = new HBox(18, currentWrap, newWrap);

        HBox actions = new HBox(8,
                primaryButton("Save", this::submitEditPublication),
                ghostButton("Back", () -> showFace(readFace)));

        card.getChildren().addAll(
                heading,
                editTitle,
                editTitleValidation,
                editCategory,
                editCategoryValidation,
                editDescription,
                editDescriptionValidation,
                imageRow,
                previews,
                actions);

        updateEditValidationState();

        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        editFace.getChildren().add(scroll);
    }

    private VBox buildCommentsSection() {
        VBox wrap = new VBox(10);
        wrap.setPadding(new Insets(18));
        wrap.setFillWidth(true);
        wrap.setMaxWidth(Double.MAX_VALUE);
        wrap.setStyle(
                "-fx-background-color: rgba(255,255,255,0.03); -fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 16px; -fx-background-radius: 16px;");

        Text heading = new Text("Discussion");
        heading.setFill(Color.WHITE);
        heading.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 18));

        commentInput.setPromptText("Share your thoughts...");
        commentInput.setPrefRowCount(3);
        commentInput.setMaxWidth(Double.MAX_VALUE);
        tuneInput(commentInput);
        commentValidation.setStyle("-fx-font-size: 11px; -fx-font-weight: 600;");
        commentInput.textProperty().addListener((obs, oldValue, newValue) -> updateCommentValidationState());
        updateCommentValidationState();

        // Public/Private toggle island
        VBox toggleIsland = new VBox(6);
        toggleIsland.setPadding(new Insets(12));
        toggleIsland.setStyle(
                "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.08) + ";" +
                        "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.25) + ";" +
                        "-fx-border-radius: 12px;" +
                        "-fx-background-radius: 12px;");

        Text toggleLabel = new Text("Comment Visibility");
        toggleLabel.setFill(Color.WHITE);
        toggleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700;");

        Text toggleDesc = new Text("Choose whether others can see your comment");
        toggleDesc.setFill(Color.web("rgba(255,255,255,0.60)"));
        toggleDesc.setStyle("-fx-font-size: 10px;");

        HBox toggleSwitch = new HBox(8);
        toggleSwitch.setAlignment(Pos.CENTER_LEFT);
        toggleSwitch.setPrefHeight(36);

        Button publicBtn = new Button("Public");
        Button privateBtn = new Button("Private");
        publicBtn.setPrefWidth(70);
        privateBtn.setPrefWidth(70);

        publicBtn.setStyle(
                "-fx-background-color: " + tm.getAccentHex() + ";" +
                        "-fx-text-fill: white; -fx-font-weight: 700;" +
                        "-fx-background-radius: 10px; -fx-padding: 6 12 6 12;");
        privateBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                        "-fx-border-color: rgba(255,255,255,0.15);" +
                        "-fx-border-radius: 10px; -fx-background-radius: 10px;" +
                        "-fx-text-fill: rgba(255,255,255,0.70); -fx-padding: 6 12 6 12;");

        publicBtn.setOnAction(e -> {
            isCommentPublic = true;
            publicBtn.setStyle(
                    "-fx-background-color: " + tm.getAccentHex() + ";" +
                            "-fx-text-fill: white; -fx-font-weight: 700;" +
                            "-fx-background-radius: 10px; -fx-padding: 6 12 6 12;");
            privateBtn.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.06);" +
                            "-fx-border-color: rgba(255,255,255,0.15);" +
                            "-fx-border-radius: 10px; -fx-background-radius: 10px;" +
                            "-fx-text-fill: rgba(255,255,255,0.70); -fx-padding: 6 12 6 12;");
        });

        privateBtn.setOnAction(e -> {
            isCommentPublic = false;
            privateBtn.setStyle(
                    "-fx-background-color: " + tm.getAccentHex() + ";" +
                            "-fx-text-fill: white; -fx-font-weight: 700;" +
                            "-fx-background-radius: 10px; -fx-padding: 6 12 6 12;");
            publicBtn.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.06);" +
                            "-fx-border-color: rgba(255,255,255,0.15);" +
                            "-fx-border-radius: 10px; -fx-background-radius: 10px;" +
                            "-fx-text-fill: rgba(255,255,255,0.70); -fx-padding: 6 12 6 12;");
        });

        toggleSwitch.getChildren().addAll(publicBtn, privateBtn);
        toggleIsland.getChildren().addAll(toggleLabel, toggleDesc, toggleSwitch);

        // Image attachments with preview
        Label commentFileLabel = new Label("No images selected");
        commentFileLabel.setTextFill(Color.web("rgba(255,255,255,0.65)"));
        commentFileLabel.setStyle("-fx-font-size: 10px;");

        commentImagePreviews.setHgap(8);
        commentImagePreviews.setVgap(8);
        commentImagePreviews.setPadding(new Insets(8, 0, 0, 0));
        commentImagePreviews.setStyle("-fx-background-color: transparent;");

        HBox imageRow = buildMultiImageAttachmentZone(
                () -> {
                    File file = chooseImage();
                    if (file != null && !commentImages.contains(file)) {
                        commentImages.add(file);
                        commentFileLabel.setText(
                                commentImages.size() + " image" + (commentImages.size() > 1 ? "s" : "") + " selected");
                        updateImagePreview(commentImages, commentImagePreviews);
                    }
                },
                commentFileLabel,
                commentImagePreviews,
                commentImages);
        imageRow.setAlignment(Pos.CENTER_LEFT);
        imageRow.setMaxWidth(Double.MAX_VALUE);

        Button send = primaryButton("Post Comment", this::submitComment);

        commentFormContainer.getChildren().setAll(commentInput, commentValidation, imageRow, commentImagePreviews,
                toggleIsland, send);
        commentFormContainer.setFillWidth(true);

        announcementHint.setTextFill(Color.web("rgba(255,255,255,0.5)"));
        announcementHint.setStyle("-fx-font-size: 13px; -fx-font-style: italic; -fx-padding: 10 0 0 0;");
        announcementHint.setManaged(false);
        announcementHint.setVisible(false);

        wrap.getChildren().addAll(heading, commentsBox, commentFormContainer, announcementHint);
        return wrap;
    }

    private VBox buildSidePanel() {
        VBox side = new VBox(10);
        side.setPadding(new Insets(14));
        side.setStyle("-fx-background-color: rgba(0,0,0,0.2);");
        side.setPrefWidth(360);
        side.setMinWidth(300);
        side.setMaxWidth(360);

        Text title = new Text("Discussions");
        title.setFill(Color.WHITE);
        title.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 20));

        filterGeneral.setOnAction(e -> loadCategory("General"));
        filterAnnouncements.setOnAction(e -> loadCategory("Announcement"));

        filterGeneral.setMaxWidth(Double.MAX_VALUE);
        filterAnnouncements.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(filterGeneral, Priority.ALWAYS);
        HBox.setHgrow(filterAnnouncements, Priority.ALWAYS);
        HBox filters = new HBox(8, filterGeneral, filterAnnouncements);

        Button newPost = primaryButton("New Post", () -> openCreateFace(false));
        newPost.setMaxWidth(Double.MAX_VALUE);

        HBox createButtons = new HBox(8, newPost);
        HBox.setHgrow(newPost, Priority.ALWAYS);

        listBox.setMaxWidth(Double.MAX_VALUE);
        listBox.setPrefWidth(330);
        listBox.setFillWidth(true);

        ScrollPane listScroll = new ScrollPane(listBox);
        listScroll.setFitToWidth(true);
        listScroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        listScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(listScroll, Priority.ALWAYS);

        side.getChildren().addAll(title, createButtons, filters, listScroll);
        return side;
    }

    private void loadCategory(String category) {
        currentFilter = category;
        updateFilterButtons();

        List<Publication> items = "Announcement".equals(category)
                ? publications.publicationsByCategory("Announcement")
                : publications.publicationsByCategory("General");
        items = new ArrayList<>(items);
        items.sort((a, b) -> compareDates(b.getDateCreationPub(), a.getDateCreationPub()));

        listBox.getChildren().clear();
        listItemsById.clear();

        if (items.isEmpty()) {
            listBox.getChildren().add(emptyLabel("No publications."));
            clearCurrent();
            return;
        }

        for (Publication pub : items) {
            listBox.getChildren().add(publicationItem(pub));
        }

        Publication target = current;
        boolean found = false;
        if (target != null) {
            for (Publication item : items) {
                if (Objects.equals(item.getIdPublication(), target.getIdPublication())) {
                    found = true;
                    break;
                }
            }
        }

        if (target == null || !found) {
            target = items.get(0);
        }
        selectPublication(target);
    }

    private Node publicationItem(Publication pub) {
        VBox item = new VBox(7);
        item.setPadding(new Insets(12));
        item.setStyle(inactiveListItemStyle());
        item.setMinWidth(0);
        item.setMaxWidth(Double.MAX_VALUE);
        item.setPrefWidth(Double.MAX_VALUE);

        HBox top = new HBox(8);
        Label pill = new Label(pub.getCategoriePub() == null ? "General" : pub.getCategoriePub());
        pill.setStyle(categoryStyle(pub.getCategoriePub()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label dateMini = dimLabel(shortDate(pub.getDateCreationPub()));
        top.getChildren().addAll(pill, spacer, dateMini);

        Text title = new Text(trim(pub.getTitrePub(), 40));
        title.setFill(Color.WHITE);
        title.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 13));
        title.wrappingWidthProperty().bind(item.widthProperty().subtract(26));

        HBox authorRow = new HBox(8);
        Label avatar = new Label(initials(pub.getUser()));
        avatar.setMinSize(32, 32);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle(
                "-fx-background-color: rgba(255,255,255,0.10);" +
                        "-fx-background-radius: 16px;" +
                        "-fx-border-color: rgba(255,255,255,0.20);" +
                        "-fx-border-radius: 16px;" +
                        "-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 700;");

        Text meta = new Text(author(pub.getUser()));
        meta.setFill(Color.web("rgba(255,255,255,0.65)"));
        meta.wrappingWidthProperty().bind(item.widthProperty().subtract(86));
        authorRow.getChildren().addAll(avatar, meta);

        item.getChildren().addAll(top, title, authorRow);
        item.setOnMouseClicked(e -> {
            selectPublication(pub);
            showFace(readFace);
        });

        if (pub.getIdPublication() != null) {
            listItemsById.put(pub.getIdPublication(), item);
        }
        return item;
    }

    private void selectPublication(Publication publication) {
        current = publication;
        if (publication == null) {
            clearCurrent();
            return;
        }

        heroTitle.setText(publication.getTitrePub() == null ? "Untitled" : publication.getTitrePub());
        heroBody.setText(publication.getDescriptionPub() == null ? "" : publication.getDescriptionPub());
        heroCategory
                .setText(publication.getCategoriePub() == null ? "Discussion General" : publication.getCategoriePub());
        heroMeta.setText(author(publication.getUser()) + " • " + format(publication.getDateCreationPub()));

        Image image = resolveImage(publication.getImagePub(), "forum_images");
        setHeroImage(image);

        updateOwnerActionsVisibility();
        updateActiveListItem();
        refreshPublicationReactionUI();
        
        boolean isAnnouncement = "Announcement".equals(publication.getCategoriePub());
        commentFormContainer.setVisible(!isAnnouncement);
        commentFormContainer.setManaged(!isAnnouncement);
        commentsBox.setVisible(!isAnnouncement);
        commentsBox.setManaged(!isAnnouncement);
        announcementHint.setVisible(isAnnouncement);
        announcementHint.setManaged(isAnnouncement);
        
        renderComments(publication);
    }

    private void renderComments(Publication publication) {
        List<Commentaire> list = comments.commentairesByPublication(publication);
        commentsBox.getChildren().clear();

        if (list.isEmpty()) {
            commentsBox.getChildren().add(emptyLabel("No comments yet."));
            return;
        }

        for (Commentaire comment : list) {
            commentsBox.getChildren().add(commentCard(comment));
        }
    }

    private Node commentCard(Commentaire comment) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 12px; -fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 12px;");

        HBox authorRow = new HBox(8);
        authorRow.setAlignment(Pos.CENTER_LEFT);

        StackPane avatarNode = commentAvatarNode(comment);
        Text authorText = new Text((comment.isVisibility() ? author(comment.getUser()) : "Anonymous") + " • "
                + format(comment.getCreatedAt()));
        authorText.setFill(Color.web("rgba(255,255,255,0.70)"));
        authorRow.getChildren().addAll(avatarNode, authorText);

        Text body = new Text(safe(comment.getDescriptionCommentaire()));
        body.setFill(Color.WHITE);
        body.wrappingWidthProperty().bind(card.widthProperty().subtract(26));

        card.getChildren().addAll(authorRow, body);

        Image img = resolveImage(comment.getImageCommentaire(), "commentaire_images");
        if (img != null) {
            ImageView preview = new ImageView(img);
            preview.setPreserveRatio(true);
            preview.setFitWidth(340);
            preview.setFitHeight(220);
            preview.setSmooth(true);
            preview.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 10px;");
            card.getChildren().add(preview);
        }

        card.getChildren().add(buildCommentActionBar(comment));

        VBox editPanelContainer = buildCommentEditPanel(comment);
        card.getChildren().add(editPanelContainer);

        VBox reportPanelContainer = buildCommentReportPanel(comment);
        card.getChildren().add(reportPanelContainer);

        VBox emojiPanelContainer = buildCommentEmojiPanel(comment);
        card.getChildren().add(emojiPanelContainer);

        return card;
    }

    private StackPane commentAvatarNode(Commentaire comment) {
        boolean visibleIdentity = comment != null && comment.isVisibility();
        User commentUser = comment == null ? null : comment.getUser();

        Circle avatarCircle = new Circle(16);
        avatarCircle.setStroke(Color.color(1, 1, 1, 0.2));
        avatarCircle.setStrokeWidth(1);

        Label initialsLabel = new Label(visibleIdentity ? initials(commentUser) : "A");
        initialsLabel.setTextFill(Color.WHITE);
        initialsLabel.setStyle("-fx-font-size: 11; -fx-font-weight: 700;");

        boolean hasImage = visibleIdentity && applyUserAvatarFill(commentUser, avatarCircle);
        if (!hasImage) {
            avatarCircle.setFill(Color.color(1, 1, 1, 0.10));
        }

        initialsLabel.setVisible(!hasImage);
        initialsLabel.setManaged(!hasImage);

        StackPane avatarWrap = new StackPane(avatarCircle, initialsLabel);
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

        Profile profile = profiles.profileByUserId(userId).orElse(null);
        profileByUserIdCache.put(userId, profile);
        return profile;
    }

    private Node buildCommentActionBar(Commentaire comment) {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 0, 0, 0));

        Button like = new Button("Like");
        Button dislike = new Button("Dislike");
        Button emoji = new Button("Emoji");
        Button report = new Button("Report");

        styleReactionButton(like);
        styleReactionButton(dislike);
        styleReactionButton(emoji);
        styleReactionButton(report);

        Label likeCount = new Label("");
        likeCount.setTextFill(Color.web("rgba(255,255,255,0.75)"));
        likeCount.setStyle("-fx-font-size: 11px;");

        Label dislikeCount = new Label("");
        dislikeCount.setTextFill(Color.web("rgba(255,255,255,0.75)"));
        dislikeCount.setStyle("-fx-font-size: 11px;");

        HBox likeWrap = reactionCountWrap(like, likeCount);
        HBox dislikeWrap = reactionCountWrap(dislike, dislikeCount);

        // Custom Ratio Bar for Comments
        HBox ratioBarContainer = new HBox();
        ratioBarContainer.setPrefSize(70, 5);
        ratioBarContainer.setMaxSize(70, 5);
        ratioBarContainer.setAlignment(Pos.CENTER_LEFT);
        ratioBarContainer.setStyle("-fx-background-radius: 80px; -fx-overflow: hidden; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 80px; -fx-border-width: 0.5;");
        
        javafx.scene.shape.Rectangle commClip = new javafx.scene.shape.Rectangle(70, 5);
        commClip.setArcWidth(5);
        commClip.setArcHeight(5);
        ratioBarContainer.setClip(commClip);

        Region likesBar = new Region();
        likesBar.setStyle("-fx-background-color: #22c55e;");
        likesBar.setPrefHeight(5);
        
        Region dislikesBar = new Region();
        dislikesBar.setStyle("-fx-background-color: #ef4444;");
        dislikesBar.setPrefHeight(5);

        ratioBarContainer.getChildren().addAll(likesBar, dislikesBar);

        Label ratioText = new Label("0%");
        ratioText.setTextFill(Color.web("rgba(255,255,255,0.72)"));
        ratioText.setStyle("-fx-font-size: 10px; -fx-font-weight: 700;");

        HBox ratioWrap = new HBox(6, ratioBarContainer, ratioText);
        ratioWrap.setAlignment(Pos.CENTER_LEFT);

        User currentUser = session.getCurrentUser();
        applyCommentReactionUI(reactions.commentStatus(comment, currentUser), like, dislike, emoji, likeCount,
                dislikeCount, likesBar, ratioText);

        like.setOnAction(e -> {
            ReactionActionResult result = reactions.commentToggle(comment, session.getCurrentUser(), "Like");
            if (!result.isSuccess()) {
                showInfo("Reaction", result.getMessage());
                return;
            }
            applyCommentReactionUI(result.getStatus(), like, dislike, emoji, likeCount, dislikeCount, likesBar,
                    ratioText);
        });

        dislike.setOnAction(e -> {
            ReactionActionResult result = reactions.commentToggle(comment, session.getCurrentUser(), "Dislike");
            if (!result.isSuccess()) {
                showInfo("Reaction", result.getMessage());
                return;
            }
            applyCommentReactionUI(result.getStatus(), like, dislike, emoji, likeCount, dislikeCount, likesBar,
                    ratioText);
        });

        emoji.setOnAction(e -> toggleCommentEmojiPanel(comment));
        report.setOnAction(e -> toggleCommentReportPanel(comment));

        Button feeling = ghostButton("Feeling", () -> showCommentSentimentAnalysis(comment));
        bar.getChildren().addAll(likeWrap, dislikeWrap, ratioWrap, emoji, report, feeling);

        if (canManageComment(comment)) {
            Button edit = ghostButton("Edit", () -> toggleCommentEditPanel(comment));
            Button delete = ghostButton("Delete", () -> deleteComment(comment));
            bar.getChildren().addAll(edit, delete);
        }

        return bar;
    }

    private HBox reactionCountWrap(Button button, Label count) {
        HBox wrap = new HBox(4, button, count);
        wrap.setAlignment(Pos.CENTER_LEFT);
        return wrap;
    }

    private void applyCommentReactionUI(
            ReactionStatus status,
            Button like,
            Button dislike,
            Button emoji,
            Label likeCount,
            Label dislikeCount,
            Region likesBar,
            Label ratioText) {
        
        // Find the dislikes bar sibling
        Region dislikesBar = null;
        if (likesBar.getParent() instanceof HBox) {
            HBox parent = (HBox) likesBar.getParent();
            if (parent.getChildren().size() >= 2) {
                dislikesBar = (Region) parent.getChildren().get(1);
            }
        }
        styleReactionButton(like);
        styleReactionButton(dislike);
        styleReactionButton(emoji);

        boolean hasLike = hasKind(status, "Like");
        boolean hasDislike = hasKind(status, "Dislike");
        String emojiValue = emojiFromStatus(status);
        boolean hasEmoji = emojiValue != null;

        if (hasLike) {
            styleReactionButtonActive(like, "#4ade80");
        } else if (hasDislike) {
            styleReactionButtonActive(dislike, "#f87171");
        } else if (hasEmoji) {
            styleReactionButtonActive(emoji, "#fbbf24");
        }

        int likes = count(status, "Like");
        int dislikes = count(status, "Dislike");
        int total = likes + dislikes;

        likeCount.setText(likes > 0 ? String.valueOf(likes) : "");
        dislikeCount.setText(dislikes > 0 ? String.valueOf(dislikes) : "");

        double ratio = total > 0 ? (double) likes / total : 0;
        likesBar.setPrefWidth(ratio * 70);
        likesBar.setMinWidth(ratio * 70);
        likesBar.setMaxWidth(ratio * 70);
        
        if (dislikesBar != null) {
            double dWidth = (1.0 - ratio) * 70;
            dislikesBar.setPrefWidth(dWidth);
            dislikesBar.setMinWidth(dWidth);
            dislikesBar.setMaxWidth(dWidth);
        }
        
        ratioText.setText(Math.round(ratio * 100) + "%");

        if (hasEmoji) {
            emoji.setText(emojiValue);
        } else {
            emoji.setText("Emoji");
        }
    }

    private void openCreateFace(boolean announcement) {
        forceAnnouncementCreate = announcement;
        createTitle.clear();
        createDescription.clear();
        createCategory.setValue(announcement ? "Announcement" : "Discussion General");
        createCategory.setDisable(announcement);
        createImageField.clear();
        createImagePreview.setImage(null);
        createImage = null;
        updateCreateValidationState();
        showFace(createFace);
    }

    private void openEditFace() {
        if (current == null) {
            showInfo("Edit", "Select a publication first.");
            return;
        }
        if (!canEditCurrent()) {
            showInfo("Permission", "You can edit only your own publication (or admin role).");
            return;
        }

        editTitle.setText(current.getTitrePub());
        editDescription.setText(current.getDescriptionPub());
        editCategory.setValue(current.getCategoriePub());
        editImageField.clear();
        editCurrentImagePreview.setImage(resolveImage(current.getImagePub(), "forum_images"));
        editNewImagePreview.setImage(null);
        editImage = null;
        updateEditValidationState();
        showFace(editFace);
    }

    private void submitPublication() {
        User user = session.getCurrentUser();
        if (user == null) {
            showInfo("Error", "You must be logged in to publish.");
            return;
        }

        String title = safe(createTitle.getText());
        String description = safe(createDescription.getText());

        List<String> flaggedCategories = moderationService.checkContent(title + " " + description);
        if (!flaggedCategories.isEmpty()) {
            showInfo("Moderation Alert", "Votre publication a été bloquée pour contenu inapproprié.\nCatégories détectées : " + String.join(", ", flaggedCategories));
            return;
        }

        String category = forceAnnouncementCreate ? "Announcement" : createCategory.getValue();

        List<String> createErrors = buildPublicationDraft(title, description, category).validateForCreate();
        if (!createErrors.isEmpty()) {
            showInfo("Validation", createErrors.getFirst());
            return;
        }

        String image = null;
        try {
            if (createImage != null) {
                image = copyUpload(createImage, "forum_images");
            }
        } catch (IOException ex) {
            showInfo("Upload Error", ex.getMessage());
            return;
        }

        Integer id = publications.publicationCreate(title, description, category, image, user);
        if (id != null && id > 0) {
            loadCategory("Announcement".equals(category) ? "Announcement" : "General");
            publications.publicationById(id).ifPresent(this::selectPublication);
            forceAnnouncementCreate = false;
            showFace(readFace);
            showNotification("Publication added successfully!", true);

            if ("Jeux Video".equals(category)) {
                discordService.sendAnnouncement(title, description, author(user), image, false);
            }
        } else {
            showInfo("Error", "Unable to create publication.");
        }
    }

    private void submitEditPublication() {
        if (current == null) {
            return;
        }

        String title = safe(editTitle.getText());
        String description = safe(editDescription.getText());

        List<String> flaggedCategories = moderationService.checkContent(title + " " + description);
        if (!flaggedCategories.isEmpty()) {
            showInfo("Moderation Alert", "Votre publication a été bloquée pour contenu inapproprié.\nCatégories détectées : " + String.join(", ", flaggedCategories));
            return;
        }

        String category = editCategory.getValue();

        List<String> editErrors = buildPublicationDraft(title, description, category).validateForCreate();
        if (!editErrors.isEmpty()) {
            showInfo("Validation", editErrors.getFirst());
            return;
        }

        String image = current.getImagePub();
        try {
            if (editImage != null) {
                image = copyUpload(editImage, "forum_images");
            }
        } catch (IOException ex) {
            showInfo("Upload Error", ex.getMessage());
            return;
        }

        boolean ok = publications.publicationUpdate(current.getIdPublication(), title, description, category, image);
        if (ok) {
            loadCategory(currentFilter);
            publications.publicationById(current.getIdPublication()).ifPresent(this::selectPublication);
            showFace(readFace);
            showNotification("Publication updated successfully!", true);

            if ("Jeux Video".equals(category)) {
                discordService.sendAnnouncement(title, description, author(session.getCurrentUser()), image, true);
            }
        } else {
            showInfo("Error", "Unable to update publication.");
        }
    }

    private void deleteCurrentPublication() {
        if (current == null) {
            return;
        }
        if (!canEditCurrent()) {
            showInfo("Permission", "You can delete only your own publication (or admin role).");
            return;
        }

        boolean confirmed = confirmFancy("Delete Publication", "Delete this publication permanently?");
        if (!confirmed) {
            return;
        }

        boolean ok = publications.publicationDelete(current.getIdPublication());
        if (ok) {
            loadCategory(currentFilter);
            showFace(readFace);
            showNotification("Publication deleted successfully!", true);
        } else {
            showInfo("Error", "Unable to delete publication.");
        }
    }

    private void submitComment() {
        if (current == null) {
            return;
        }

        User user = session.getCurrentUser();
        if (user == null) {
            showInfo("Error", "You must be logged in to comment.");
            return;
        }

        String description = safe(commentInput.getText());
        String commentError = commentDescriptionError(description);
        if (commentError != null) {
            showInfo("Validation", commentError);
            return;
        }

        List<String> flaggedCategories = moderationService.checkContent(description);
        if (!flaggedCategories.isEmpty()) {
            showInfo("Moderation Alert", "Votre commentaire a été bloqué pour contenu inapproprié.\nCatégories détectées : " + String.join(", ", flaggedCategories));
            return;
        }

        String image = null;
        try {
            if (!commentImages.isEmpty()) {
                // Upload first image as primary
                image = copyUpload(commentImages.get(0), "commentaire_images");
            }
        } catch (IOException ex) {
            showInfo("Upload Error", ex.getMessage());
            return;
        }

        Integer id = comments.commentaireCreate(description, image, isCommentPublic, current, user);
        if (id != null && id > 0) {
            commentInput.clear();
            updateCommentValidationState();
            commentImageField.clear();
            commentImages.clear();
            commentImagePreviews.getChildren().clear();
            isCommentPublic = true;
            renderComments(current);
        } else {
            showInfo("Error", "Unable to post comment.");
        }
    }

    private void editComment(Commentaire comment) {
        if (comment.getIdCommentaire() == null) {
            return;
        }

        Dialog<ButtonType> dialog = styledDialog("Edit Comment");
        TextArea area = new TextArea(safe(comment.getDescriptionCommentaire()));
        area.setPrefRowCount(4);
        tuneInput(area);

        VBox content = new VBox(10, dimLabel("Update your comment"), area);
        content.setPadding(new Insets(4, 0, 0, 0));
        dialog.getDialogPane().setContent(content);

        ButtonType save = new ButtonType("Save", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(save, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result != save) {
                return;
            }

            String text = safe(area.getText());
            String updateError = commentDescriptionError(text);
            if (updateError != null) {
                showInfo("Validation", updateError);
                return;
            }

            boolean ok = comments.commentaireUpdate(comment.getIdCommentaire(), text, comment.getImageCommentaire(),
                    comment.isVisibility());
            if (ok) {
                renderComments(current);
            } else {
                showInfo("Error", "Unable to update comment.");
            }
        });
    }

    private void deleteComment(Commentaire comment) {
        if (comment.getIdCommentaire() == null) {
            return;
        }
        if (!canManageComment(comment)) {
            showInfo("Permission", "You can delete only your own comment (or admin role).");
            return;
        }

        boolean confirmed = confirmFancy("Delete Comment", "Delete this comment permanently?");
        if (!confirmed) {
            return;
        }

        boolean ok = comments.commentaireDelete(comment.getIdCommentaire());
        if (ok) {
            renderComments(current);
        } else {
            showInfo("Error", "Unable to delete comment.");
        }
    }

    private void togglePublicationReportPanel() {
        if (publicationReportPanel.isManaged()) {
            publicationReportPanel.setVisible(false);
            publicationReportPanel.setManaged(false);
        } else {
            publicationReportPanel.setVisible(true);
            publicationReportPanel.setManaged(true);
        }
    }

    private VBox buildPublicationReportPanel() {
        publicationReportPanel.getChildren().clear();
        publicationReportPanel.setVisible(false);
        publicationReportPanel.setManaged(false);
        publicationReportPanel.setMinWidth(0);
        publicationReportPanel.setMaxWidth(Double.MAX_VALUE);
        publicationReportPanel.setPadding(new Insets(10));
        publicationReportPanel.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10px; -fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 10px;");

        TextArea reasonInput = new TextArea();
        reasonInput.setPromptText("Why are you reporting this publication?");
        reasonInput.setPrefRowCount(3);
        reasonInput.setMaxWidth(Double.MAX_VALUE);
        reasonInput.setWrapText(true);
        tuneInput(reasonInput);

        Button submitBtn = primaryButton("Send Report", () -> {
            String reason = safe(reasonInput.getText());
            if (reason.isBlank()) {
                showInfo("Report", "Please provide a reason.");
                return;
            }

            if (current != null && current.getIdPublication() != null) {
                ReactionActionResult result = reactions.publicationReport(current, session.getCurrentUser(), reason);
                if (result.isSuccess()) {
                    showInfo("Report Sent", result.getMessage());
                    reasonInput.clear();
                    togglePublicationReportPanel();
                } else {
                    showInfo("Report", result.getMessage());
                }
            }
        });

        Button closeBtn = ghostButton("Cancel", this::togglePublicationReportPanel);

        HBox actions = new HBox(8, submitBtn, closeBtn);
        actions.setMaxWidth(Double.MAX_VALUE);
        publicationReportPanel.getChildren().addAll(reasonInput, actions);
        return publicationReportPanel;
    }

    private void toggleCommentEditPanel(Commentaire comment) {
        VBox panel = commentEditPanels.get(comment.getIdCommentaire());
        if (panel != null) {
            if (Objects.equals(currentCommentForInline, comment) && panel.isManaged()) {
                panel.setVisible(false);
                panel.setManaged(false);
                currentCommentForInline = null;
            } else {
                currentCommentForInline = comment;
                panel.setVisible(true);
                panel.setManaged(true);
            }
        }
    }

    private void toggleCommentReportPanel(Commentaire comment) {
        VBox panel = commentReportPanels.get(comment.getIdCommentaire());
        if (panel != null) {
            if (Objects.equals(currentCommentForInline, comment) && panel.isManaged()) {
                panel.setVisible(false);
                panel.setManaged(false);
                currentCommentForInline = null;
            } else {
                currentCommentForInline = comment;
                panel.setVisible(true);
                panel.setManaged(true);
            }
        }
    }

    private void toggleCommentEmojiPanel(Commentaire comment) {
        VBox panel = commentEmojiPanels.get(comment.getIdCommentaire());
        if (panel != null) {
            if (Objects.equals(currentCommentForInline, comment) && panel.isManaged()) {
                panel.setVisible(false);
                panel.setManaged(false);
                currentCommentForInline = null;
            } else {
                currentCommentForInline = comment;
                panel.setVisible(true);
                panel.setManaged(true);
            }
        }
    }

    private VBox buildCommentReportPanel(Commentaire comment) {
        VBox panel = new VBox(8);
        commentReportPanels.put(comment.getIdCommentaire(), panel);

        panel.setMinWidth(0);
        panel.setMaxWidth(Double.MAX_VALUE);
        panel.setPadding(new Insets(10));
        panel.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10px; -fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 10px;");
        panel.setVisible(false);
        panel.setManaged(false);

        TextArea reasonInput = new TextArea();
        reasonInput.setPromptText("Why are you reporting this comment?");
        reasonInput.setPrefRowCount(3);
        reasonInput.setMaxWidth(Double.MAX_VALUE);
        reasonInput.setWrapText(true);
        tuneInput(reasonInput);

        Button submitBtn = primaryButton("Send Report", () -> {
            String reason = safe(reasonInput.getText());
            if (reason.isBlank()) {
                showInfo("Report", "Please provide a reason.");
                return;
            }

            ReactionActionResult result = reactions.commentReport(comment, session.getCurrentUser(), reason);
            if (result.isSuccess()) {
                showInfo("Report Sent", result.getMessage());
                reasonInput.clear();
                toggleCommentReportPanel(comment);
            } else {
                showInfo("Report", result.getMessage());
            }
        });

        Button closeBtn = ghostButton("Cancel", () -> toggleCommentReportPanel(comment));

        HBox actions = new HBox(8, submitBtn, closeBtn);
        actions.setMaxWidth(Double.MAX_VALUE);
        panel.getChildren().addAll(reasonInput, actions);
        return panel;
    }

    private void togglePublicationReaction(String kind) {
        if (current == null || current.getIdPublication() == null) {
            return;
        }

        ReactionActionResult result = reactions.publicationToggle(current, session.getCurrentUser(), kind);
        if (!result.isSuccess()) {
            showInfo("Reaction", result.getMessage());
            return;
        }

        applyPublicationReactionUI(result.getStatus());
    }

    private void togglePublicationEmojiPanel() {
        if (publicationEmojiPanel.isManaged()) {
            publicationEmojiPanel.setVisible(false);
            publicationEmojiPanel.setManaged(false);
        } else {
            publicationEmojiPanel.setVisible(true);
            publicationEmojiPanel.setManaged(true);
        }
    }

    private VBox buildPublicationEmojiPanel() {
        publicationEmojiPanel.getChildren().clear();
        publicationEmojiPanel.setVisible(false);
        publicationEmojiPanel.setManaged(false);
        publicationEmojiPanel.setPadding(new Insets(8));
        publicationEmojiPanel.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10px; -fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 10px;");
        publicationEmojiPanel.setSpacing(6);

        HBox emojiRow = new HBox(6);
        emojiRow.setAlignment(Pos.CENTER_LEFT);

        for (String emoji : EMOJIS) {
            Label emojiLabel = new Label(emoji);
            emojiLabel.setFont(new Font(28));
            emojiLabel.setCursor(Cursor.HAND);
            emojiLabel.setPadding(new Insets(4));

            String emojiValue = emoji;
            emojiLabel.setOnMouseEntered(e -> emojiLabel.setOpacity(0.7));
            emojiLabel.setOnMouseExited(e -> emojiLabel.setOpacity(1.0));
            emojiLabel.setOnMouseClicked(e -> {
                e.consume();
                if (current != null && current.getIdPublication() != null) {
                    ReactionActionResult result = reactions.publicationEmoji(current, session.getCurrentUser(),
                            emojiValue);
                    if (!result.isSuccess()) {
                        showInfo("Reaction", result.getMessage());
                        return;
                    }
                    applyPublicationReactionUI(result.getStatus());
                    togglePublicationEmojiPanel();
                }
            });
            emojiRow.getChildren().add(emojiLabel);
        }

        Button closeBtn = ghostButton("Close", this::togglePublicationEmojiPanel);
        publicationEmojiPanel.getChildren().addAll(emojiRow, closeBtn);
        return publicationEmojiPanel;
    }

    private VBox buildCommentEmojiPanel(Commentaire comment) {
        VBox panel = new VBox(8);
        commentEmojiPanels.put(comment.getIdCommentaire(), panel);

        panel.setVisible(false);
        panel.setManaged(false);
        panel.setPadding(new Insets(8));
        panel.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10px; -fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 10px;");
        panel.setSpacing(6);

        HBox emojiRow = new HBox(6);
        emojiRow.setAlignment(Pos.CENTER_LEFT);

        for (String emoji : EMOJIS) {
            Label emojiLabel = new Label(emoji);
            emojiLabel.setFont(new Font(28));
            emojiLabel.setCursor(Cursor.HAND);
            emojiLabel.setPadding(new Insets(4));

            String emojiValue = emoji;
            emojiLabel.setOnMouseEntered(e -> emojiLabel.setOpacity(0.7));
            emojiLabel.setOnMouseExited(e -> emojiLabel.setOpacity(1.0));
            emojiLabel.setOnMouseClicked(e -> {
                e.consume();
                ReactionActionResult result = reactions.commentEmoji(comment, session.getCurrentUser(), emojiValue);
                if (!result.isSuccess()) {
                    showInfo("Reaction", result.getMessage());
                    return;
                }
                toggleCommentEmojiPanel(comment);
                if (current != null) {
                    renderComments(current);
                }
            });
            emojiRow.getChildren().add(emojiLabel);
        }

        Button closeBtn = ghostButton("Close", () -> toggleCommentEmojiPanel(comment));
        panel.getChildren().addAll(emojiRow, closeBtn);
        return panel;
    }

    private VBox buildCommentEditPanel(Commentaire comment) {
        VBox panel = new VBox(8);
        commentEditPanels.put(comment.getIdCommentaire(), panel);

        panel.setMinWidth(0);
        panel.setMaxWidth(Double.MAX_VALUE);
        panel.setPadding(new Insets(10));
        panel.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10px; -fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 10px;");
        panel.setVisible(false);
        panel.setManaged(false);

        TextArea editInput = new TextArea(safe(comment.getDescriptionCommentaire()));
        editInput.setPromptText("Update your comment...");
        editInput.setPrefRowCount(3);
        editInput.setMaxWidth(Double.MAX_VALUE);
        editInput.setWrapText(true);
        tuneInput(editInput);

        // Visual identity toggle (Public/Anonymous)
        HBox visibilityBox = new HBox(10);
        visibilityBox.setAlignment(Pos.CENTER_LEFT);
        Label visLabel = new Label("Public Identity:");
        visLabel.setTextFill(Color.web("rgba(255,255,255,0.7)"));
        visLabel.setStyle("-fx-font-size: 12px;");
        
        CheckBox visCheck = new CheckBox();
        visCheck.setSelected(comment.isVisibility());
        visCheck.setStyle("-fx-mark-color: #22c55e;"); // Green check
        visibilityBox.getChildren().addAll(visLabel, visCheck);

        // Image Handling
        final String[] tempImage = {comment.getImageCommentaire()};
        ImageView preview = new ImageView();
        preview.setFitWidth(100);
        preview.setFitHeight(70);
        preview.setPreserveRatio(true);
        preview.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-background-radius: 5px;");
        
        if (tempImage[0] != null && !tempImage[0].isEmpty()) {
            Image currentImg = resolveImage(tempImage[0], "commentaire_images");
            if (currentImg != null) preview.setImage(currentImg);
        }

        Button imgBtn = ghostButton("Change Photo", () -> {
            File selected = chooseImage();
            if (selected != null) {
                try {
                    String relative = copyUpload(selected, "commentaire_images");
                    tempImage[0] = relative;
                    preview.setImage(new Image(selected.toURI().toString()));
                } catch (Exception ex) {
                    showInfo("Image", "Failed to load image: " + ex.getMessage());
                }
            }
        });

        HBox mediaRow = new HBox(12, preview, imgBtn);
        mediaRow.setAlignment(Pos.CENTER_LEFT);

        Button saveBtn = primaryButton("Save", () -> {
            String newText = safe(editInput.getText());
            if (newText.isBlank()) {
                showInfo("Edit", "Comment cannot be empty.");
                return;
            }

            List<String> flaggedCategories = moderationService.checkContent(newText);
            if (!flaggedCategories.isEmpty()) {
                showInfo("Moderation Alert", "Votre commentaire a été bloqué pour contenu inapproprié.\nCatégories détectées : " + String.join(", ", flaggedCategories));
                return;
            }

            boolean success = comments.commentaireUpdate(comment.getIdCommentaire(), newText,
                    tempImage[0], visCheck.isSelected());
            if (success) {
                comment.setDescriptionCommentaire(newText);
                comment.setImageCommentaire(tempImage[0]);
                comment.setVisibility(visCheck.isSelected());
                
                showNotification("Comment updated successfully!", true);
                toggleCommentEditPanel(comment);
                if (current != null) {
                    renderComments(current);
                }
            } else {
                showInfo("Edit", "Unable to update comment.");
            }
        });

        Button closeBtn = ghostButton("Cancel", () -> toggleCommentEditPanel(comment));

        HBox actions = new HBox(8, saveBtn, closeBtn);
        actions.setPadding(new Insets(4, 0, 0, 0));
        
        panel.getChildren().addAll(editInput, visibilityBox, mediaRow, actions);
        return panel;
    }

    private void refreshPublicationReactionUI() {
        if (current == null || current.getIdPublication() == null) {
            return;
        }

        applyPublicationReactionUI(reactions.publicationStatus(current, session.getCurrentUser()));
    }

    private void applyPublicationReactionUI(ReactionStatus status) {
        if (status == null) {
            status = ReactionStatus.empty();
        }

        styleReactionButton(postLikeButton);
        styleReactionButton(postDislikeButton);
        styleReactionButton(postEmojiButton);
        styleReactionButton(postBookmarkButton);

        boolean hasLike = hasKind(status, "Like");
        boolean hasDislike = hasKind(status, "Dislike");
        String emojiValue = emojiFromStatus(status);
        boolean hasEmoji = emojiValue != null;

        if (hasLike) {
            styleReactionButtonActive(postLikeButton, "#4ade80");
        } else if (hasDislike) {
            styleReactionButtonActive(postDislikeButton, "#f87171");
        } else if (hasEmoji) {
            styleReactionButtonActive(postEmojiButton, "#fbbf24");
        }

        if (status.isBookmarked()) {
            styleReactionButtonActive(postBookmarkButton, "#93c5fd");
            postBookmarkButton.setText("Bookmarked");
        } else {
            postBookmarkButton.setText("Bookmark");
        }

        int likes = count(status, "Like");
        int dislikes = count(status, "Dislike");
        int total = likes + dislikes;

        postLikeCount.setText(likes > 0 ? String.valueOf(likes) : "");
        postDislikeCount.setText(dislikes > 0 ? String.valueOf(dislikes) : "");

        double ratio = total > 0 ? (double) likes / total : 0;
        
        double lWidth = ratio * 110;
        postLikesBarRegion.setPrefWidth(lWidth);
        postLikesBarRegion.setMinWidth(lWidth);
        postLikesBarRegion.setMaxWidth(lWidth);
        
        double dWidth = (1.0 - ratio) * 110;
        postDislikesBarRegion.setPrefWidth(dWidth);
        postDislikesBarRegion.setMinWidth(dWidth);
        postDislikesBarRegion.setMaxWidth(dWidth);
        
        postRatioText.setText(Math.round(ratio * 100) + "%");

        if (hasEmoji) {
            postEmojiButton.setText(emojiValue);
        } else {
            postEmojiButton.setText("Emoji");
        }
    }

    private void styleReactionButton(Button button) {
        button.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                        "-fx-border-color: rgba(255,255,255,0.16);" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-text-fill: rgba(255,255,255,0.92);" +
                        "-fx-padding: 6 12 6 12;");
    }

    private boolean hasKind(ReactionStatus status, String kind) {
        if (status == null || kind == null) {
            return false;
        }
        for (ReactionPayload payload : status.getReactions()) {
            if (kind.equals(payload.getKind())) {
                return true;
            }
        }
        return false;
    }

    private String emojiFromStatus(ReactionStatus status) {
        if (status == null) {
            return null;
        }
        for (ReactionPayload payload : status.getReactions()) {
            if ("Emoji".equals(payload.getKind()) && payload.getEmoji() != null && !payload.getEmoji().isBlank()) {
                return payload.getEmoji();
            }
        }
        return null;
    }

    private int count(ReactionStatus status, String kind) {
        if (status == null || status.getCounts() == null || kind == null) {
            return 0;
        }
        Integer value = status.getCounts().get(kind);
        return value == null ? 0 : value;
    }

    private void styleReactionButtonActive(Button button, String colorHex) {
        button.setStyle(
                "-fx-background-color: " + rgba(colorHex, 0.18) + ";" +
                        "-fx-border-color: " + rgba(colorHex, 0.55) + ";" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-text-fill: " + colorHex + ";" +
                        "-fx-font-weight: 700;" +
                        "-fx-padding: 6 12 6 12;");
    }

    private String rgba(String hex, double alpha) {
        if (hex == null || !hex.startsWith("#") || (hex.length() != 7 && hex.length() != 4)) {
            return "rgba(255,255,255," + alpha + ")";
        }

        int r;
        int g;
        int b;
        if (hex.length() == 7) {
            r = Integer.parseInt(hex.substring(1, 3), 16);
            g = Integer.parseInt(hex.substring(3, 5), 16);
            b = Integer.parseInt(hex.substring(5, 7), 16);
        } else {
            r = Integer.parseInt(hex.substring(1, 2) + hex.substring(1, 2), 16);
            g = Integer.parseInt(hex.substring(2, 3) + hex.substring(2, 3), 16);
            b = Integer.parseInt(hex.substring(3, 4) + hex.substring(3, 4), 16);
        }

        return "rgba(" + r + "," + g + "," + b + "," + alpha + ")";
    }

    private void setHeroImage(Image image) {
        currentHeroImage = image;
        detailHeroImage.setImage(image);
        refreshHeroViewport();
    }

    private void refreshHeroViewport() {
        if (currentHeroImage == null) {
            detailHeroImage.setViewport(null);
            return;
        }

        double boxW = detailHero.getWidth();
        double boxH = detailHero.getHeight();
        double imgW = currentHeroImage.getWidth();
        double imgH = currentHeroImage.getHeight();

        if (boxW <= 1 || boxH <= 1 || imgW <= 1 || imgH <= 1) {
            return;
        }

        double boxRatio = boxW / boxH;
        double imgRatio = imgW / imgH;

        double vpX;
        double vpY;
        double vpW;
        double vpH;

        if (imgRatio > boxRatio) {
            vpH = imgH;
            vpW = imgH * boxRatio;
            vpX = (imgW - vpW) / 2.0;
            vpY = 0;
        } else {
            vpW = imgW;
            vpH = imgW / boxRatio;
            vpX = 0;
            vpY = (imgH - vpH) / 2.0;
        }

        detailHeroImage.setViewport(new Rectangle2D(vpX, vpY, vpW, vpH));
        detailHeroImage.setFitWidth(boxW);
        detailHeroImage.setFitHeight(boxH);
    }

    private void showFace(VBox face) {
        readFace.setVisible(false);
        readFace.setManaged(false);
        createFace.setVisible(false);
        createFace.setManaged(false);
        editFace.setVisible(false);
        editFace.setManaged(false);

        face.setVisible(true);
        face.setManaged(true);
    }

    private boolean canEditCurrent() {
        User currentUser = session.getCurrentUser();
        if (currentUser == null || current == null || current.getUser() == null) {
            return false;
        }

        if (Objects.equals(currentUser.getIdUser(), current.getUser().getIdUser())) {
            return true;
        }

        String role = currentUser.getRoleUser();
        return role != null && MODERATOR_ROLES.contains(role);
    }

    private boolean canDeleteCurrent() {
        User currentUser = session.getCurrentUser();
        if (currentUser == null || current == null || current.getUser() == null) {
            return false;
        }

        // Only allow delete if user is the owner AND has admin role
        if (Objects.equals(currentUser.getIdUser(), current.getUser().getIdUser())) {
            String role = currentUser.getRoleUser();
            return role != null && MODERATOR_ROLES.contains(role);
        }

        return false;
    }

    private boolean canManageComment(Commentaire comment) {
        User currentUser = session.getCurrentUser();
        if (currentUser == null || comment == null || comment.getUser() == null) {
            return false;
        }

        if (Objects.equals(currentUser.getIdUser(), comment.getUser().getIdUser())) {
            return true;
        }

        String role = currentUser.getRoleUser();
        return role != null && MODERATOR_ROLES.contains(role);
    }

    private void updateFilterButtons() {
        filterGeneral.setStyle("General".equals(currentFilter) ? activeFilterStyle() : inactiveFilterStyle());
        filterAnnouncements
                .setStyle("Announcement".equals(currentFilter) ? activeFilterStyle() : inactiveFilterStyle());
    }

    private String activeFilterStyle() {
        return "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.20) + "; -fx-border-color: "
                + tm.toRgba(tm.getAccentHex(), 0.70)
                + "; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-text-fill: white; -fx-font-weight: 700;";
    }

    private String inactiveFilterStyle() {
        return "-fx-background-color: rgba(255,255,255,0.06); -fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-text-fill: white;";
    }

    private Button primaryButton(String label, Runnable action) {
        Button button = new Button(label);
        button.setStyle(
                "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.80) + ";" +
                        "-fx-text-fill: white; -fx-font-weight: 700;" +
                        "-fx-background-radius: 12px; -fx-padding: 8 14 8 14;");
        button.setOnAction(e -> action.run());
        return button;
    }

    private HBox buildAttachmentZone(Runnable onBrowse, Runnable onRemove, Label fileLabel) {
        Label uploadIcon = new Label("📎");
        uploadIcon.setStyle("-fx-font-size: 18px;");

        VBox attachmentZone = new VBox(6);
        attachmentZone.setAlignment(Pos.CENTER);
        attachmentZone.setPrefHeight(70);
        attachmentZone.setCursor(Cursor.HAND);
        attachmentZone.setStyle(
                "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.06) + ";" +
                        "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.40) + ";" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-style: dashed;");

        Text uploadText = new Text("Click to upload image");
        uploadText.setFill(Color.web("rgba(255,255,255,0.60)"));
        uploadText.setStyle("-fx-font-size: 11px;");

        attachmentZone.getChildren().addAll(uploadIcon, uploadText, fileLabel);
        attachmentZone.setOnMouseClicked(e -> onBrowse.run());
        attachmentZone.setOnMouseEntered(e -> attachmentZone.setStyle(
                "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.12) + ";" +
                        "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.60) + ";" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-style: dashed;"));
        attachmentZone.setOnMouseExited(e -> attachmentZone.setStyle(
                "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.06) + ";" +
                        "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.40) + ";" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-style: dashed;"));

        Button remove = ghostButton("Remove", onRemove);

        HBox row = new HBox(8, attachmentZone, remove);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(attachmentZone, Priority.ALWAYS);
        return row;
    }

    private HBox buildMultiImageAttachmentZone(Runnable onBrowse, Label fileLabel, FlowPane previewPane,
            List<File> images) {
        Label uploadIcon = new Label("📎");
        uploadIcon.setStyle("-fx-font-size: 18px;");

        VBox attachmentZone = new VBox(6);
        attachmentZone.setAlignment(Pos.CENTER);
        attachmentZone.setPrefHeight(70);
        attachmentZone.setCursor(Cursor.HAND);
        attachmentZone.setStyle(
                "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.06) + ";" +
                        "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.40) + ";" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-style: dashed;");

        Text uploadText = new Text("Click to add images");
        uploadText.setFill(Color.web("rgba(255,255,255,0.60)"));
        uploadText.setStyle("-fx-font-size: 11px;");

        attachmentZone.getChildren().addAll(uploadIcon, uploadText, fileLabel);
        attachmentZone.setOnMouseClicked(e -> onBrowse.run());
        attachmentZone.setOnMouseEntered(e -> attachmentZone.setStyle(
                "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.12) + ";" +
                        "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.60) + ";" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-style: dashed;"));
        attachmentZone.setOnMouseExited(e -> attachmentZone.setStyle(
                "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.06) + ";" +
                        "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.40) + ";" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 12px;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-style: dashed;"));

        HBox row = new HBox(8, attachmentZone);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(attachmentZone, Priority.ALWAYS);
        return row;
    }

    private void updateImagePreview(List<File> images, FlowPane previewPane) {
        previewPane.getChildren().clear();
        for (File file : images) {
            VBox imageCard = new VBox(4);
            imageCard.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.03);" +
                            "-fx-border-color: rgba(255,255,255,0.10);" +
                            "-fx-border-radius: 8px;" +
                            "-fx-background-radius: 8px;" +
                            "-fx-padding: 4;");

            ImageView img = new ImageView(new Image(file.toURI().toString(), true));
            img.setFitWidth(80);
            img.setFitHeight(80);
            img.setPreserveRatio(true);

            Button removeBtn = ghostButton("×", () -> {
                images.remove(file);
                updateImagePreview(images, previewPane);
            });
            removeBtn.setStyle(
                    "-fx-background-color: rgba(200,0,0,0.70);" +
                            "-fx-text-fill: white; -fx-font-weight: 700;" +
                            "-fx-background-radius: 6px; -fx-padding: 2 6 2 6;" +
                            "-fx-font-size: 16px;");
            removeBtn.setMaxWidth(Double.MAX_VALUE);

            imageCard.getChildren().addAll(img, removeBtn);
            VBox.setVgrow(img, Priority.ALWAYS);
            previewPane.getChildren().add(imageCard);
        }
    }

    private Button ghostButton(String label, Runnable action) {
        Button button = new Button(label);
        button.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                        "-fx-border-color: rgba(255,255,255,0.15);" +
                        "-fx-border-radius: 12px; -fx-background-radius: 12px;" +
                        "-fx-text-fill: white; -fx-padding: 8 14 8 14;");
        button.setOnAction(e -> action.run());
        return button;
    }

    private String copyUpload(File source, String folder) throws IOException {
        File dir = new File(System.getProperty("user.dir") + File.separator + "uploads" + File.separator + folder);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Unable to create upload folder");
        }

        String name = System.currentTimeMillis() + "_" + source.getName();
        File dest = new File(dir, name);
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return folder + "/" + name;
    }

    private File chooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp", "*.webp"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        Window window = root.getScene() == null ? null : root.getScene().getWindow();
        return chooser.showOpenDialog(window);
    }

    private void clearCurrent() {
        current = null;
        heroTitle.setText("Welcome to the Forum");
        heroMeta.setText("");
        heroBody.setText("Select a publication from the right panel.");
        heroCategory.setText("Discussion General");
        setHeroImage(null);

        ownerActions.setManaged(false);
        ownerActions.setVisible(false);

        postLikeCount.setText("");
        postDislikeCount.setText("");
        if (postLikesBarRegion != null) postLikesBarRegion.setPrefWidth(0);
        if (postDislikesBarRegion != null) postDislikesBarRegion.setPrefWidth(0);
        postRatioText.setText("0%");

        updateActiveListItem();
        commentsBox.getChildren().setAll(emptyLabel("No comments yet."));
    }

    private Node emptyLabel(String value) {
        Label label = new Label(value);
        label.setTextFill(Color.web("rgba(255,255,255,0.70)"));
        return label;
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

    private String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 3) + "...";
    }

    private String format(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.format(DATE_FMT);
    }

    private String shortDate(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.format(SHORT_DATE_FMT);
    }

    private int compareDates(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String inactiveListItemStyle() {
        return "-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 16px; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 16px;";
    }

    private String activeListItemStyle() {
        return "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.17)
                + "; -fx-background-radius: 16px; -fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.90)
                + "; -fx-border-radius: 16px;";
    }

    private void updateActiveListItem() {
        for (VBox item : listItemsById.values()) {
            item.setStyle(inactiveListItemStyle());
        }

        if (current != null && current.getIdPublication() != null) {
            VBox active = listItemsById.get(current.getIdPublication());
            if (active != null) {
                active.setStyle(activeListItemStyle());
            }
        }
    }

    private String categoryStyle(String category) {
        String base = "-fx-padding: 4 8 4 8; -fx-font-size: 10px; -fx-font-weight: 800; -fx-background-radius: 8px; -fx-border-radius: 8px;";
        if (category == null) {
            return base
                    + "-fx-text-fill: white; -fx-background-color: rgba(255,255,255,0.1); -fx-border-color: rgba(255,255,255,0.2);";
        }

        switch (category) {
            case "Announcement":
                return base
                        + "-fx-text-fill: #ff8080; -fx-background-color: rgba(255,50,50,0.2); -fx-border-color: rgba(255,50,50,0.4);";
            case "Suggestion":
                return base
                        + "-fx-text-fill: #80ffaa; -fx-background-color: rgba(50,255,100,0.15); -fx-border-color: rgba(50,255,100,0.3);";
            case "Jeux Video":
                return base
                        + "-fx-text-fill: #d080ff; -fx-background-color: rgba(150,50,255,0.2); -fx-border-color: rgba(150,50,255,0.4);";
            case "Informatique":
                return base
                        + "-fx-text-fill: #80c0ff; -fx-background-color: rgba(50,150,255,0.2); -fx-border-color: rgba(50,150,255,0.4);";
            case "Nouveauté":
                return base
                        + "-fx-text-fill: #ffd680; -fx-background-color: rgba(255,200,50,0.2); -fx-border-color: rgba(255,200,50,0.4);";
            case "Culture":
                return base
                        + "-fx-text-fill: #d4a0ff; -fx-background-color: rgba(200,100,255,0.2); -fx-border-color: rgba(200,100,255,0.4);";
            case "Sport":
                return base
                        + "-fx-text-fill: #7fe0c0; -fx-background-color: rgba(50,200,150,0.2); -fx-border-color: rgba(50,200,150,0.4);";
            default:
                return base
                        + "-fx-text-fill: #9cc5ff; -fx-background-color: rgba(100,200,255,0.15); -fx-border-color: rgba(100,200,255,0.3);";
        }
    }

    private Label dimLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("rgba(255,255,255,0.62)"));
        return label;
    }

    private void preparePreview(ImageView preview, double width, double height) {
        preview.setFitWidth(width);
        preview.setFitHeight(height);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        preview.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05); -fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 10px;");
    }

    private void tuneInput(Node node) {
        node.setStyle(
                "-fx-background-color: rgba(0,0,0,0.42);" +
                        "-fx-control-inner-background: rgba(0,0,0,0.42);" +
                        "-fx-border-color: rgba(255,255,255,0.14);" +
                        "-fx-border-radius: 12px;" +
                        "-fx-background-radius: 12px;" +
                        "-fx-prompt-text-fill: rgba(255,255,255,0.55);" +
                        "-fx-text-fill: white;");
    }

    private void styleValidationLabel(Label label, String text, boolean valid) {
        label.setText(text);
        label.setTextFill(valid ? Color.web(tm.getAccentHex()) : Color.web("#ff3b30"));
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: 600;");
    }

    private boolean isPublicationFormValid(String title, String description, String category) {
        return buildPublicationDraft(title, description, category).validateForCreate().isEmpty();
    }

    private void updateCreateValidationState() {
        String title = safe(createTitle.getText());
        String description = safe(createDescription.getText());
        String category = forceAnnouncementCreate ? "Announcement" : createCategory.getValue();

        String titleError = publicationFieldError(title, description, category, "Title ");
        String categoryError = publicationFieldError(title, description, category, "Category ");
        String descriptionError = publicationFieldError(title, description, category, "Description ");

        styleValidationLabel(
                createTitleValidation,
                titleError == null ? "✓ Title looks good" : titleError,
                titleError == null);
        styleValidationLabel(
                createCategoryValidation,
                categoryError == null ? "✓ Category selected" : categoryError,
                categoryError == null);
        styleValidationLabel(
                createDescriptionValidation,
                descriptionError == null ? "✓ Description looks good" : descriptionError,
                descriptionError == null);
    }

    private void updateEditValidationState() {
        String title = safe(editTitle.getText());
        String description = safe(editDescription.getText());
        String category = editCategory.getValue();

        String titleError = publicationFieldError(title, description, category, "Title ");
        String categoryError = publicationFieldError(title, description, category, "Category ");
        String descriptionError = publicationFieldError(title, description, category, "Description ");

        styleValidationLabel(
                editTitleValidation,
                titleError == null ? "✓ Title looks good" : titleError,
                titleError == null);
        styleValidationLabel(
                editCategoryValidation,
                categoryError == null ? "✓ Category selected" : categoryError,
                categoryError == null);
        styleValidationLabel(
                editDescriptionValidation,
                descriptionError == null ? "✓ Description looks good" : descriptionError,
                descriptionError == null);
    }

    private void updateCommentValidationState() {
        String description = safe(commentInput.getText());
        if (description.isEmpty()) {
            commentValidation.setText("Type your comment (minimum 5 characters)");
            commentValidation.setTextFill(Color.web("rgba(255,255,255,0.62)"));
            return;
        }

        String error = commentDescriptionError(description);
        if (error != null) {
            commentValidation.setText(error);
            commentValidation.setTextFill(Color.web("#ff3b30"));
            return;
        }

        commentValidation.setText("✓ Comment looks good");
        commentValidation.setTextFill(Color.web(tm.getAccentHex()));
    }

    private Publication buildPublicationDraft(String title, String description, String category) {
        Publication draft = new Publication();
        draft.setTitrePub(title == null ? "" : title.trim());
        draft.setDescriptionPub(description == null ? "" : description.trim());
        draft.setCategoriePub(category);
        draft.setDateCreationPub(LocalDateTime.now());
        draft.setUser(validationUser());
        return draft;
    }

    private String publicationFieldError(String title, String description, String category, String prefix) {
        List<String> errors = buildPublicationDraft(title, description, category).validateForCreate();
        for (String error : errors) {
            if (error.startsWith(prefix)) {
                return error;
            }
        }
        return null;
    }

    private String commentDescriptionError(String description) {
        Commentaire draft = new Commentaire();
        draft.setDescriptionCommentaire(description == null ? "" : description.trim());
        Publication publication = new Publication();
        publication.setIdPublication(
                current != null && current.getIdPublication() != null ? current.getIdPublication() : 1);
        draft.setPublication(publication);
        draft.setUser(validationUser());
        for (String error : draft.validateForCreate()) {
            if (error.startsWith("Comment text ")) {
                return error;
            }
        }
        return null;
    }

    private User validationUser() {
        User user = session.getCurrentUser();
        if (user != null && user.getIdUser() != null && user.getIdUser() > 0) {
            return user;
        }
        User placeholder = new User();
        placeholder.setIdUser(1);
        return placeholder;
    }

    private void updateOwnerActionsVisibility() {
        boolean can = canDeleteCurrent();
        ownerActions.setVisible(can);
        ownerActions.setManaged(can);
    }

    private boolean isModerator(User user) {
        if (user == null) {
            return false;
        }
        String role = user.getRoleUser();
        return role != null && MODERATOR_ROLES.contains(role);
    }

    private Image resolveImage(String dbValue, String fallbackFolder) {
        if (dbValue == null || dbValue.isBlank()) {
            return null;
        }

        String raw = dbValue.trim().replace("\\", "/");
        try {
            if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("file:/")) {
                Image web = new Image(raw, true);
                return web.isError() ? null : web;
            }

            File pathAsFile = new File(raw);
            if (pathAsFile.exists()) {
                Image img = new Image(pathAsFile.toURI().toString(), true);
                return img.isError() ? null : img;
            }

            File uploadsRoot = new File(System.getProperty("user.dir") + File.separator + "uploads");
            List<File> candidates = List.of(
                    new File(uploadsRoot, raw),
                    new File(uploadsRoot, fallbackFolder + File.separator + raw),
                    new File(uploadsRoot, "forum_images" + File.separator + raw),
                    new File(uploadsRoot, "commentaire_images" + File.separator + raw));

            for (File candidate : candidates) {
                if (candidate.exists()) {
                    Image img = new Image(candidate.toURI().toString(), true);
                    if (!img.isError()) {
                        return img;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private Dialog<ButtonType> styledDialog(String title) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.initStyle(StageStyle.TRANSPARENT);

        if (root.getScene() != null && root.getScene().getWindow() != null) {
            dialog.initOwner(root.getScene().getWindow());
        }
        
        DialogPane pane = dialog.getDialogPane();
        pane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setFill(Color.TRANSPARENT);
            }
        });
        
        pane.setStyle(
                "-fx-background-color: #000000;" +
                "-fx-border-color: rgba(255,255,255,0.08);" +
                "-fx-background-radius: 24px;" +
                "-fx-border-radius: 24px;" +
                "-fx-border-width: 1;");
        
        // Safety checks for internal components
        Node header = pane.lookup(".header-panel");
        if (header != null) {
            header.setStyle("-fx-background-color: transparent;");
        }
        
        Node buttonBar = pane.lookup(".button-bar");
        if (buttonBar != null) {
            buttonBar.setStyle("-fx-background-color: transparent; -fx-padding: 15;");
        }

        return dialog;
    }

    private boolean confirmFancy(String title, String message) {
        Dialog<ButtonType> dialog = styledDialog(title);
        
        VBox container = new VBox(24);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(35, 40, 15, 40));
        container.setPrefWidth(420);
        
        // Animated-style Icon Wrapper (Red Tint)
        StackPane iconWrapper = new StackPane();
        iconWrapper.setPrefSize(80, 80);
        iconWrapper.setMaxSize(80, 80);
        iconWrapper.setStyle(
            "-fx-background-color: rgba(255, 59, 48, 0.1);" +
            "-fx-background-radius: 100px;" +
            "-fx-border-color: rgba(255, 59, 48, 0.2);" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 100px;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(255, 59, 48, 0.15), 20, 0, 0, 0);"
        );
        
        Label icon = new Label("🗑");
        icon.setStyle("-fx-font-size: 32px; -fx-text-fill: #ff3b30;");
        iconWrapper.getChildren().add(icon);
        
        VBox texts = new VBox(8);
        texts.setAlignment(Pos.CENTER);
        
        Label head = new Label("Delete Item?");
        head.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 24));
        head.setTextFill(Color.WHITE);
        head.setStyle("-fx-letter-spacing: -0.02em;");
        
        Label msg = new Label("This action cannot be undone. \nAre you sure you want to permanently delete this?");
        msg.setTextFill(Color.web("rgba(255, 255, 255, 0.6)"));
        msg.setWrapText(true);
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        msg.setFont(Font.font(MainApplication.getInstance().getLightFontFamily(), 15));
        
        texts.getChildren().addAll(head, msg);
        container.getChildren().addAll(iconWrapper, texts);
        
        dialog.getDialogPane().setContent(container);

        ButtonType confirm = new ButtonType("Confirm Delete", ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(cancel, confirm); // Reordered for Horizon style

        Node confirmBtn = dialog.getDialogPane().lookupButton(confirm);
        if (confirmBtn != null) {
            confirmBtn.setStyle(
                "-fx-background-color: rgba(255, 59, 48, 0.15);" +
                "-fx-text-fill: #ff3b30;" +
                "-fx-font-weight: 700;" +
                "-fx-background-radius: 12px;" +
                "-fx-border-color: rgba(255, 59, 48, 0.4);" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 12px;" +
                "-fx-padding: 10 25 10 25;" +
                "-fx-cursor: hand;"
            );
            
            // Hover effect can't be easily set in setStyle, but we match the static state
        }

        Node cancelBtn = dialog.getDialogPane().lookupButton(cancel);
        if (cancelBtn != null) {
            cancelBtn.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.05);" +
                "-fx-text-fill: rgba(255, 255, 255, 0.7);" +
                "-fx-border-color: rgba(255, 255, 255, 0.1);" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 12px; -fx-background-radius: 12px;" +
                "-fx-padding: 10 25 10 25;" +
                "-fx-cursor: hand;"
            );
        }

        return dialog.showAndWait().filter(result -> result == confirm).isPresent();
    }

    private String promptReason(String title, String placeholder) {
        Dialog<ButtonType> dialog = styledDialog(title);
        TextArea area = new TextArea();
        area.setPrefRowCount(4);
        area.setPromptText(placeholder);
        tuneInput(area);

        VBox content = new VBox(10, dimLabel("Provide a short reason"), area);
        dialog.getDialogPane().setContent(content);

        ButtonType submit = new ButtonType("Submit", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(submit, ButtonType.CANCEL);

        return dialog.showAndWait().filter(result -> result == submit).map(result -> safe(area.getText())).orElse(null);
    }

    private void showInfo(String title, String content) {
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        if (root.getScene() != null && root.getScene().getWindow() != null) {
            stage.initOwner(root.getScene().getWindow());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        }

        javafx.scene.layout.VBox container = new javafx.scene.layout.VBox(15);
        container.setPadding(new javafx.geometry.Insets(25));
        container.setStyle(
            "-fx-background-color: rgba(17, 24, 39, 0.95);" +
            "-fx-background-radius: 16px;" +
            "-fx-border-color: rgba(236, 72, 153, 0.4);" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 16px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 10);"
        );

        javafx.scene.text.Text titleText = new javafx.scene.text.Text(title);
        titleText.setFont(javafx.scene.text.Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), javafx.scene.text.FontWeight.BOLD, 22));
        titleText.setFill(javafx.scene.paint.Color.web("#ec4899"));

        javafx.scene.text.Text contentText = new javafx.scene.text.Text(content);
        contentText.setFont(javafx.scene.text.Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), javafx.scene.text.FontWeight.NORMAL, 15));
        contentText.setFill(javafx.scene.paint.Color.web("rgba(255, 255, 255, 0.85)"));
        contentText.setWrappingWidth(350);

        javafx.scene.control.Button btn = new javafx.scene.control.Button("OK");
        btn.setStyle(
            "-fx-background-color: linear-gradient(to right, #ec4899, #8b5cf6);" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 8px 24px;" +
            "-fx-cursor: hand;"
        );
        btn.setOnAction(e -> stage.close());

        javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(btn);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        container.getChildren().addAll(titleText, contentText, btnBox);

        javafx.scene.Scene scene = new javafx.scene.Scene(container);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);
        
        if (root.getScene() != null && root.getScene().getWindow() != null) {
            javafx.stage.Window owner = root.getScene().getWindow();
            stage.setX(owner.getX() + (owner.getWidth() - 400) / 2);
            stage.setY(owner.getY() + (owner.getHeight() - 200) / 2);
        }
        
        stage.showAndWait();
    }

    private void showNotification(String message, boolean success) {
        notificationBox.toFront();
        HBox toast = new HBox(12);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setPadding(new Insets(14, 20, 14, 20));
        toast.setMaxWidth(350);

        String bgColor = success ? "#10b981" : "#ef4444"; // Emerald or Red
        toast.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-background-radius: 12px;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 15, 0, 0, 6);" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-radius: 12px;");

        Label icon = new Label(success ? "✓" : "✕");
        icon.setTextFill(Color.WHITE);
        icon.setStyle("-fx-font-size: 18px; -fx-font-weight: 900;");

        Label text = new Label(message);
        text.setTextFill(Color.WHITE);
        text.setWrapText(true);
        text.setStyle("-fx-font-size: 13px; -fx-font-weight: 700;");

        toast.getChildren().addAll(icon, text);

        // Animations
        toast.setTranslateX(400); // Start off-screen right
        toast.setOpacity(0);

        notificationBox.getChildren().add(toast);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), toast);
        slideIn.setToX(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), toast);
        fadeIn.setToValue(1);

        PauseTransition pause = new PauseTransition(Duration.seconds(4));

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(400), toast);
        slideOut.setToX(400);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toast);
        fadeOut.setToValue(0);

        SequentialTransition seq = new SequentialTransition(
                new javafx.animation.ParallelTransition(slideIn, fadeIn),
                pause,
                new javafx.animation.ParallelTransition(slideOut, fadeOut));

        seq.setOnFinished(e -> notificationBox.getChildren().remove(toast));
        seq.play();
    }

    private void showSentimentAnalysis() {
        if (current == null) return;

        String textToAnalyze = current.getTitrePub() + ". " + current.getDescriptionPub();
        
        // Show loading state
        showNotification("Analyzing feelings... please wait", true);

        sentimentService.analyzeContent(textToAnalyze).thenAccept(result -> {
            javafx.application.Platform.runLater(() -> {
                if (!result.success) {
                    showInfo("Sentiment Analysis", result.explanation);
                    return;
                }
                showSentimentPopup(result);
            });
        });
    }

    private void showSentimentPopup(SentimentResult result) {
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        if (root.getScene() != null && root.getScene().getWindow() != null) {
            stage.initOwner(root.getScene().getWindow());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        }

        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setPrefWidth(450);
        container.setStyle(
            "-fx-background-color: rgba(17, 24, 39, 0.95);" +
            "-fx-background-radius: 20px;" +
            "-fx-border-color: rgba(236, 72, 153, 0.3);" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 20px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 30, 0, 0, 15);"
        );

        Text titleText = new Text("AI Sentiment Analysis");
        titleText.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 24));
        titleText.setFill(Color.web("#ec4899"));

        Label sentimentLabel = new Label(result.sentiment.toUpperCase());
        String color = result.sentiment.toLowerCase().contains("pos") ? "#4ade80" : 
                       (result.sentiment.toLowerCase().contains("neg") ? "#f87171" : "#fbbf24");
        sentimentLabel.setStyle(
            "-fx-background-color: " + color + "22;" +
            "-fx-text-fill: " + color + ";" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 5 12 5 12;" +
            "-fx-background-radius: 20px;" +
            "-fx-border-color: " + color + "44;" +
            "-fx-border-radius: 20px;"
        );

        HBox header = new HBox(15, titleText, sentimentLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox scoreBox = new VBox(5);
        Label scoreLabel = new Label("Confidence Score: " + result.confidence + "%");
        scoreLabel.setTextFill(Color.web("rgba(255,255,255,0.7)"));
        scoreLabel.setFont(Font.font(12));
        
        double confidenceVal = 0.5;
        try { confidenceVal = Double.parseDouble(result.confidence.replace("%", "")) / 100.0; } catch(Exception e){}
        ProgressBar pb = new ProgressBar(confidenceVal);
        pb.setPrefWidth(Double.MAX_VALUE);
        pb.setStyle("-fx-accent: " + color + ";");
        scoreBox.getChildren().addAll(scoreLabel, pb);

        VBox emotionBox = new VBox(8);
        Label emotionTitle = new Label("Primary Emotion Detected");
        emotionTitle.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 14));
        emotionTitle.setTextFill(Color.WHITE);
        
        Label emotionValue = new Label(result.primaryEmotion);
        emotionValue.setFont(Font.font(20));
        emotionValue.setTextFill(Color.web("#8b5cf6")); 
        emotionBox.getChildren().addAll(emotionTitle, emotionValue);

        VBox explanationBox = new VBox(8);
        Label expTitle = new Label("AI Detailed Explanation");
        expTitle.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 14));
        expTitle.setTextFill(Color.WHITE);
        
        Text expText = new Text(result.explanation);
        expText.setFont(Font.font(MainApplication.getInstance().getLightFontFamily(), 14));
        expText.setFill(Color.web("rgba(255,255,255,0.8)"));
        expText.setWrappingWidth(390);
        explanationBox.getChildren().addAll(expTitle, expText);

        Button closeBtn = new Button("Close Analysis");
        closeBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #ec4899, #8b5cf6);" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 10px;" +
            "-fx-padding: 10 30 10 30;" +
            "-fx-cursor: hand;"
        );
        closeBtn.setOnAction(e -> stage.close());
        
        HBox btnBox = new HBox(closeBtn);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        container.getChildren().addAll(header, scoreBox, emotionBox, explanationBox, btnBox);

        javafx.scene.Scene scene = new javafx.scene.Scene(container);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        
        if (root.getScene() != null && root.getScene().getWindow() != null) {
            javafx.stage.Window owner = root.getScene().getWindow();
            stage.setX(owner.getX() + (owner.getWidth() - 450) / 2);
            stage.setY(owner.getY() + (owner.getHeight() - 450) / 2);
        }
        
        stage.show();
    }

    private void showCommentSentimentAnalysis(Commentaire comment) {
        if (comment == null) return;

        String textToAnalyze = comment.getDescriptionCommentaire();
        
        showNotification("Analyzing comment feeling...", true);

        sentimentService.analyzeContent(textToAnalyze).thenAccept(result -> {
            javafx.application.Platform.runLater(() -> {
                if (!result.success) {
                    showInfo("Sentiment Analysis", result.explanation);
                    return;
                }
                showSentimentPopup(result);
            });
        });
    }
}
