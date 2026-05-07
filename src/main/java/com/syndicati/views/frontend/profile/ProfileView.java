package com.syndicati.views.frontend.profile;

import com.syndicati.MainApplication;
import com.syndicati.controllers.user.profile.ProfileController;
import com.syndicati.controllers.user.relationship.UserRelationshipController;
import com.syndicati.controllers.user.standing.UserStandingController;
import com.syndicati.interfaces.ViewInterface;
import com.syndicati.controllers.user.profile.ProfileAvatarController;
import com.syndicati.controllers.user.user.UserController;
import com.syndicati.models.user.Profile;
import com.syndicati.models.user.User;
import com.syndicati.models.user.UserRelationship;
import com.syndicati.models.user.UserStanding;
import com.syndicati.models.user.data.ProfileRepository;
import com.syndicati.models.user.data.UserStandingRepository;
import com.syndicati.models.user.data.UserRelationshipRepository;
import com.syndicati.models.user.data.UserRepository;
import com.syndicati.utils.navigation.NavigationManager;
import com.syndicati.services.user.profile.ProfileService;
import com.syndicati.utils.image.ImageLoaderUtil;
import com.syndicati.utils.session.SessionManager;
import com.syndicati.utils.theme.ThemeManager;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Profile page replica based on templates/frontend/profile/profile.html.twig.
 * This is a UI-only duplication without backend behavior.
 */
public class ProfileView implements ViewInterface {

    private final VBox root;
    private final ThemeManager tm;

    private final Map<String, VBox> mainPages = new LinkedHashMap<>();
    private final Map<String, Button> mainNavButtons = new LinkedHashMap<>();

    private final Map<String, VBox> detailTabs = new LinkedHashMap<>();
    private final Map<String, Button> detailTabButtons = new LinkedHashMap<>();
    private long lastXpInteractionAt = 0L;

    public ProfileView() {
        this.tm = ThemeManager.getInstance();
        this.root = new VBox();
        build();
    }

    private void build() {
        VBox content = new VBox(26);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(24, 0, 42, 0));
        content.setFillWidth(true);

        content.getChildren().add(createMainNavigation());

        VBox page1 = createOverviewPage();
        VBox page2 = createActivityPage();
        mainPages.put("overview", page1);
        mainPages.put("activity", page2);
        content.getChildren().addAll(page1, page2);

        setMainPage("overview");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        root.getChildren().add(scroll);
        root.setStyle("-fx-background-color: transparent;");
    }

    private HBox createMainNavigation() {
        HBox nav = new HBox(12);
        nav.setAlignment(Pos.CENTER);
        nav.setPadding(new Insets(8));
        nav.setMaxWidth(560);
        nav.setStyle(
            "-fx-background-color: " + surfaceSoft() + ";" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 999px;" +
            "-fx-border-radius: 999px;"
        );

        Button overview = createMainNavButton("Overview", "overview");
        Button activity = createMainNavButton("Activity & Details", "activity");
        mainNavButtons.put("overview", overview);
        mainNavButtons.put("activity", activity);
        nav.getChildren().addAll(overview, activity);
        return nav;
    }

    private Button createMainNavButton(String label, String key) {
        Button btn = new Button(label);
        btn.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 13));
        btn.setOnAction(e -> setMainPage(key));
        styleMainNavButton(btn, false);
        return btn;
    }

    private void setMainPage(String key) {
        mainPages.forEach((name, pane) -> {
            boolean active = name.equals(key);
            pane.setVisible(active);
            pane.setManaged(active);
        });
        mainNavButtons.forEach((name, btn) -> styleMainNavButton(btn, name.equals(key)));
    }

    private void styleMainNavButton(Button btn, boolean active) {
        if (active) {
            btn.setStyle(
                "-fx-background-color: " + tm.getEffectiveAccentGradient() + ";" +
                "-fx-text-fill: white; -fx-font-weight: 800;" +
                "-fx-background-radius: 999px; -fx-padding: 12 24 12 24;"
            );
        } else {
            btn.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: " + textMuted() + ";" +
                "-fx-font-weight: 800;" +
                "-fx-background-radius: 999px; -fx-padding: 12 24 12 24;"
            );
        }
    }

    private VBox createOverviewPage() {
        VBox page = new VBox(24);
        page.setAlignment(Pos.TOP_CENTER);

        page.getChildren().add(createHeroCard());

        HBox row = new HBox(22);
        row.setAlignment(Pos.TOP_CENTER);
        row.setMaxWidth(1800);
        row.prefWidthProperty().bind(root.widthProperty().multiply(0.95));
        VBox standing = createStandingCard();
        VBox circle = createCircleCard();
        HBox.setHgrow(standing, Priority.ALWAYS);
        HBox.setHgrow(circle, Priority.ALWAYS);
        standing.setMaxWidth(Double.MAX_VALUE);
        circle.setMaxWidth(Double.MAX_VALUE);
        row.getChildren().addAll(standing, circle);

        page.getChildren().add(row);
        return page;
    }

    private VBox createHeroCard() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        Profile currentProfile = SessionManager.getInstance().getCurrentProfile();
        if (currentProfile == null && currentUser != null && currentUser.getIdUser() != null) {
            currentProfile = new ProfileController().findOneByUserId(currentUser.getIdUser()).orElse(null);
            if (currentProfile != null) {
                SessionManager.getInstance().setCurrentProfile(currentProfile);
            }
        }
        
        VBox card = new VBox(0);
        card.setMaxWidth(1800);
        card.prefWidthProperty().bind(root.widthProperty().multiply(0.95));
        card.setStyle(shell(34, surfaceCard(), 0.16));

        StackPane banner = new StackPane();
        banner.setMinHeight(180);
        banner.setStyle(
            "-fx-background-color: linear-gradient(to right, rgba(43,43,58,0.95), rgba(20,20,28,0.9));" +
            "-fx-background-radius: 34px 34px 0 0;"
        );
        banner.getChildren().add(text("PROFILE", 40, true, "rgba(255,255,255,0.20)"));

        HBox body = new HBox(26);
        body.setPadding(new Insets(24));
        body.setAlignment(Pos.TOP_LEFT);

        StackPane avatarWrap = new StackPane();
        avatarWrap.setCursor(javafx.scene.Cursor.HAND);
        Circle avatar = new Circle(56);
        avatar.setFill(Color.web(tm.getAccentHex()));
        avatar.setStroke(Color.web(tm.toRgba(tm.getAccentHex(), 0.35)));
        avatar.setStrokeWidth(2);

        String userInitial = currentInitial(currentUser);
        Text avatarText = text(userInitial, 38, true, "#ffffff");
        boolean hasAvatar = applyAvatarFill(avatar);
        avatarText.setVisible(!hasAvatar);
        avatarText.setManaged(!hasAvatar);
        avatarWrap.getChildren().addAll(avatar, avatarText);
        avatarWrap.setOnMouseClicked(e -> handleProfileAvatarUpload(currentUser, avatar, avatarText));

        VBox identity = new VBox(10);
        identity.setAlignment(Pos.TOP_LEFT);
        String displayName = currentUser != null ? currentUser.getFirstName() + " " + currentUser.getLastName() : "Syndicati Member";
        String displayRole = currentUser != null ? currentUser.getRoleUser() : "Resident";
        String displayEmail = currentUser != null ? currentUser.getEmailUser() : "contact@syndicati.tn";
        String displayBio = currentProfile != null && currentProfile.getDescriptionProfile() != null
            ? currentProfile.getDescriptionProfile().trim()
            : "";
        String createdDate = currentUser != null && currentUser.getCreatedAt() != null 
            ? currentUser.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            : "12/03/2026";
        String verifiedStatus = currentUser != null && currentUser.isVerified() ? "Verified" : "Pending";
        
        Text name = text(displayName, 34, true, "#ffffff");
        Text role = text(displayRole, 16, false, textSoft());
        Text email = text(displayEmail, 14, false, textMuted());
        Text bio = null;
        if (!displayBio.isBlank()) {
            bio = text(displayBio, 13, false, "rgba(255,255,255,0.72)");
            bio.setWrappingWidth(520);
            bio.setLineSpacing(3);
        }

        HBox stats = new HBox(12,
            statPill("Account created", createdDate),
            statPill("Status", verifiedStatus)
        );

        VBox defaultInfo = bio == null
            ? new VBox(10, name, role, email, stats)
            : new VBox(10, name, role, email, bio, stats);

        HBox quickBtnWrap = new HBox();
        quickBtnWrap.setAlignment(Pos.CENTER_RIGHT);
        quickBtnWrap.setPadding(new Insets(4, 0, 0, 0));

        Button showActionsBtn = new Button("▦  QUICK ACTIONS  ›");
        styleQuickActionsButton(showActionsBtn, false);
        quickBtnWrap.getChildren().add(showActionsBtn);
        defaultInfo.getChildren().add(quickBtnWrap);

        StackPane actionsSwitcher = new StackPane();
        actionsSwitcher.setStyle("-fx-background-color: transparent;");
        actionsSwitcher.setPrefHeight(260);
        actionsSwitcher.setMinHeight(260);
        actionsSwitcher.setMaxHeight(260);

        VBox switcherView = new VBox(12);
        switcherView.setId("actions-switcher-view");
        switcherView.setStyle("-fx-background-color: transparent;");
        switcherView.setVisible(false);
        switcherView.setManaged(false);

        HBox switcherTopBar = new HBox(10);
        switcherTopBar.setAlignment(Pos.CENTER_LEFT);

        Button switcherBackBtn = new Button("←");
        switcherBackBtn.setStyle(
            "-fx-min-width: 30px; -fx-min-height: 30px;" +
            "-fx-max-width: 30px; -fx-max-height: 30px;" +
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-border-color: rgba(255,255,255,0.15);" +
            "-fx-border-width: 1px; -fx-border-radius: 999px; -fx-background-radius: 999px;" +
            "-fx-text-fill: rgba(255,255,255,0.75); -fx-font-weight: 700; -fx-cursor: hand;"
        );
        Text switcherTitle = text("QUICK ACTIONS", 12, true, "rgba(255,255,255,0.55)");
        switcherTopBar.getChildren().addAll(switcherBackBtn, switcherTitle);

        GridPane actions = new GridPane();
        actions.setId("quick-actions-grid");
        actions.setHgap(10);
        actions.setVgap(10);
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(33.33);
        c.setFillWidth(true);
        actions.getColumnConstraints().addAll(c, c, c);
        actions.add(createActionTile("🎥 Host Spotlight", "host", switcherView, showActionsBtn), 0, 0);
        actions.add(createActionTile("🔑 Join by Code", "join", switcherView, showActionsBtn), 1, 0);
        actions.add(createActionTile("🔐 2FA", "2fa", switcherView, showActionsBtn), 2, 0);
        actions.add(createActionTile("👆 Biometrics", "biometrics", switcherView, showActionsBtn), 0, 1);
        actions.add(createActionTile("👤 Face ID", "faceid", switcherView, showActionsBtn), 1, 1);
        actions.add(createActionTile("⚙️ Settings", "settings", switcherView, showActionsBtn), 2, 1);

        switcherView.getChildren().addAll(switcherTopBar, actions);

        VBox detailView = new VBox(12);
        detailView.setId("actions-detail-view");
        detailView.setStyle("-fx-background-color: transparent;");
        detailView.setVisible(false);
        detailView.setManaged(false);

        showActionsBtn.setOnAction(e -> showActionsTiles(defaultInfo, switcherView, detailView, showActionsBtn));
        switcherBackBtn.setOnAction(e -> showDefaultHeroInfo(defaultInfo, switcherView, detailView, showActionsBtn));

        actionsSwitcher.getChildren().addAll(defaultInfo, switcherView, detailView);

        identity.getChildren().add(actionsSwitcher);
        HBox.setHgrow(identity, Priority.ALWAYS);

        body.getChildren().addAll(avatarWrap, identity);
        card.getChildren().addAll(banner, body);

        root.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            if (!switcherView.isVisible()) {
                return;
            }

            Object target = event.getTarget();
            if (target instanceof javafx.scene.Node targetNode && !isDescendantOf(targetNode, actionsSwitcher)) {
                showDefaultHeroInfo(defaultInfo, switcherView, detailView, showActionsBtn);
            }
        });

        return card;
    }
    
    private void showActionsTiles(VBox defaultView, VBox switcherView, VBox detailView, Button showBtn) {
        if (detailView.isVisible()) {
            detailView.setVisible(false);
            detailView.setManaged(false);
            detailView.getChildren().clear();
        }
        styleQuickActionsButton(showBtn, true);
        switcherView.toFront();
        switcherView.setVisible(true);
        switcherView.setManaged(true);
        animateHeroTransition(defaultView, switcherView);
    }

    private void showDefaultHeroInfo(VBox defaultView, VBox switcherView, VBox detailView, Button showBtn) {
        VBox activeFrom = detailView.isVisible() ? detailView : switcherView;
        styleQuickActionsButton(showBtn, false);
        defaultView.toFront();
        defaultView.setVisible(true);
        defaultView.setManaged(true);
        animateHeroTransition(activeFrom, defaultView);
    }

    private void styleQuickActionsButton(Button button, boolean active) {
        if (active) {
            button.setStyle(
                "-fx-background-color: " + tm.getEffectiveAccentGradient() + ";" +
                "-fx-text-fill: #ffffff; -fx-border-color: transparent; -fx-border-width: 1px;" +
                "-fx-background-radius: 999px; -fx-border-radius: 999px;" +
                "-fx-padding: 9 16 9 16; -fx-font-weight: 800; -fx-font-size: 11px;"
            );
        } else {
            button.setStyle(
                "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.15) + ";" +
                "-fx-text-fill: " + tm.getAccentHex() + ";" +
                "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.35) + "; -fx-border-width: 1px;" +
                "-fx-background-radius: 999px; -fx-border-radius: 999px;" +
                "-fx-padding: 9 16 9 16; -fx-font-weight: 800; -fx-font-size: 11px;"
            );
        }
    }

    private void animateHeroTransition(VBox from, VBox to) {
        if (from == to) {
            return;
        }

        to.setVisible(true);
        to.setManaged(true);
        to.setOpacity(0);
        to.setTranslateX(18);

        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(220), from);
        fadeOut.setFromValue(from.getOpacity());
        fadeOut.setToValue(0);

        javafx.animation.TranslateTransition slideOut = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(220), from);
        slideOut.setFromX(0);
        slideOut.setToX(-18);

        fadeOut.setOnFinished(e -> {
            from.setVisible(false);
            from.setManaged(false);
            from.setOpacity(1);
            from.setTranslateX(0);
        });

        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(260), to);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        javafx.animation.TranslateTransition slideIn = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(260), to);
        slideIn.setFromX(18);
        slideIn.setToX(0);

        fadeOut.play();
        slideOut.play();
        fadeIn.play();
        slideIn.play();
    }

    private boolean isDescendantOf(javafx.scene.Node node, javafx.scene.Node ancestor) {
        javafx.scene.Node current = node;
        while (current != null) {
            if (current == ancestor) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private VBox createStandingCard() {
        VBox card = cardShell();
        card.setPadding(new Insets(22));

        User currentUser = SessionManager.getInstance().getCurrentUser();
        UserStanding standing = SessionManager.getInstance().getCurrentStanding();
        if ((standing == null || standing.getUserId() == null) && currentUser != null && currentUser.getIdUser() != null) {
            UserStandingRepository repo = new UserStandingRepository();
            Optional<UserStanding> opt = repo.findByUserId(currentUser.getIdUser());
            standing = opt.orElse(null);
            if (standing != null) {
                SessionManager.getInstance().setCurrentStanding(standing);
            }
        }

        int levelValue = standing != null ? standing.getLevel() : 1;
        int pointsValue = standing != null ? standing.getPoints() : 0;
        String standingLabel = standing != null ? standing.getStandingLabel() : "NORMAL";

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox level = new VBox(0, text(String.valueOf(levelValue), 28, true, "#ffffff"), text("LVL", 10, true, tm.getAccentHex()));
        level.setAlignment(Pos.CENTER);
        level.setMinSize(72, 72);
        level.setStyle(shell(16, surfaceSoft(), 0.16));

        VBox title = new VBox(4, text("RESIDENT STANDING", 18, true, "#ffffff"), text("Your status within the Horizon community", 12, false, textMuted()));
        HBox.setHgrow(title, Priority.ALWAYS);

        VBox points = new VBox(2, text(String.valueOf(pointsValue), 24, true, "#ffffff"), text("SYNDIC PTS", 10, true, "rgba(255,255,255,0.50)"));
        points.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(level, title, points);

        boolean isAllGood = "NORMAL".equals(standingLabel) || "GOOD".equals(standingLabel);
        boolean isLimited = "LIMITED".equals(standingLabel);
        boolean isAtRisk = "AT_RISK".equals(standingLabel);
        boolean isSuspended = "SUSPENDED".equals(standingLabel);

        HBox standingBar = new HBox(12,
            standingPoint("All good", isAllGood),
            standingPoint("Limited", isLimited),
            standingPoint("At risk", isAtRisk),
            standingPoint("Suspended", isSuspended)
        );
        standingBar.setAlignment(Pos.CENTER_LEFT);

        int nextLevelXp = (levelValue + 1) * 100;
        int currentXp = pointsValue % 100;
        int xpRemaining = 100 - currentXp;

        VBox xp = new VBox(8);
        HBox xpTop = new HBox();
        xpTop.setAlignment(Pos.CENTER_LEFT);
        Text left = text("XP TOWARDS LEVEL " + (levelValue + 1), 11, true, textMuted());
        Text right = text(currentXp + "/100", 11, true, "#ffffff");
        HBox.setHgrow(left, Priority.ALWAYS);
        xpTop.getChildren().addAll(left, right);

        StackPane progressTrack = new StackPane();
        progressTrack.setAlignment(Pos.CENTER_LEFT);
        progressTrack.setMinHeight(8);
        progressTrack.setStyle("-fx-background-color: " + surfaceSoft() + "; -fx-background-radius: 999px;");
        StackPane fill = new StackPane();
        fill.setPrefWidth(currentXp * 1.8);
        fill.setMinHeight(8);
        fill.setStyle("-fx-background-color: " + tm.getEffectiveAccentGradient() + "; -fx-background-radius: 999px;");
        progressTrack.getChildren().add(fill);

        xp.getChildren().addAll(xpTop, progressTrack);

        card.getChildren().addAll(header, standingBar, xp);
        return card;
    }

    private VBox createCircleCard() {
        VBox card = cardShell();
        card.setPadding(new Insets(22));

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getIdUser() == null) {
            card.getChildren().addAll(
                text("CIRCLE", 18, true, "#ffffff"),
                text("Sign in to manage your circle.", 12, false, textMuted())
            );
            return card;
        }

        UserRelationshipController relationshipController = new UserRelationshipController();
        UserController userController = new UserController();

        Text friendsBadge = badge("0 FRIENDS");
        Text pendingBadge = badge("0 PENDING");
        HBox top = new HBox(10, text("CIRCLE", 18, true, "#ffffff"), friendsBadge, pendingBadge);
        top.setAlignment(Pos.CENTER_LEFT);

        Button friendsTab = tabChip("Friends", true);
        Button pendingTab = tabChip("Pending", false);
        HBox switcher = new HBox(8, friendsTab, pendingTab);

        GridPane friendsGrid = new GridPane();
        friendsGrid.setHgap(12);
        friendsGrid.setVgap(12);

        VBox pendingList = new VBox(10);

        StackPane listFace = new StackPane(friendsGrid, pendingList);
        pendingList.setVisible(false);
        pendingList.setManaged(false);

        VBox search = new VBox(10);
        search.setPadding(new Insets(14));
        search.setStyle(shell(18, surfaceSoft(), 0.16));
        Text searchTitle = text("Explore Community", 13, true, "#ffffff");
        Text searchSub = text("Search residents and send connection requests.", 12, false, textMuted());

        javafx.scene.control.TextField searchField = new javafx.scene.control.TextField();
        searchField.setPromptText("Search by name...");
        searchField.setStyle(
            "-fx-padding: 9 12 9 12;" +
            "-fx-background-color: rgba(255,255,255,0.05);" +
            "-fx-text-fill: white;" +
            "-fx-border-color: rgba(255,255,255,0.12);" +
            "-fx-border-width: 1;" +
            "-fx-background-radius: 12; -fx-border-radius: 12;"
        );

        VBox searchResults = new VBox(8);

        Runnable[] refreshCircle = new Runnable[1];
        Runnable[] refreshSearch = new Runnable[1];
        int[] previousFriendCount = new int[] { -1 };
        int[] previousPendingCount = new int[] { -1 };

        refreshCircle[0] = () -> {
            int friendCount = relationshipController.countFriends(currentUser);
            int pendingCount = relationshipController.countPendingRequests(currentUser);
            friendsBadge.setText(friendCount + " FRIENDS");
            pendingBadge.setText(pendingCount + " PENDING");
            if (previousFriendCount[0] != -1 && previousFriendCount[0] != friendCount) {
                animateBadgePulse(friendsBadge);
            }
            if (previousPendingCount[0] != -1 && previousPendingCount[0] != pendingCount) {
                animateBadgePulse(pendingBadge);
            }
            previousFriendCount[0] = friendCount;
            previousPendingCount[0] = pendingCount;

            friendsGrid.getChildren().clear();
            List<User> friends = relationshipController.findFriends(currentUser, 24);
            int col = 0;
            int row = 0;
            for (User friend : friends) {
                VBox friendItem = circleFriendCard(friend, () -> {
                    relationshipController.removeConnection(currentUser.getIdUser(), friend.getIdUser());
                    refreshCircle[0].run();
                    refreshSearch[0].run();
                });
                friendsGrid.add(friendItem, col, row);
                col++;
                if (col >= 4) {
                    col = 0;
                    row++;
                }
            }

            if (friends.isEmpty()) {
                friendsGrid.add(text("No friends yet.", 12, false, textMuted()), 0, 0);
            }

            pendingList.getChildren().clear();
            List<UserRelationship> pending = relationshipController.findPendingRequestsFor(currentUser);
            for (UserRelationship relationship : pending) {
                Integer firstId = relationship.getUserFirstId();
                Integer secondId = relationship.getUserSecondId();
                if (firstId == null || secondId == null) {
                    continue;
                }

                boolean incoming = secondId.equals(currentUser.getIdUser());
                int otherId = incoming ? firstId : secondId;
                Optional<User> otherUser = userController.findById(otherId);
                if (otherUser.isEmpty()) {
                    continue;
                }

                pendingList.getChildren().add(circlePendingRow(
                    otherUser.get(),
                    incoming,
                    () -> {
                        if (relationship.getId() != null) {
                            relationshipController.acceptRequest(relationship.getId(), currentUser.getIdUser());
                            refreshCircle[0].run();
                            refreshSearch[0].run();
                        }
                    },
                    () -> {
                        relationshipController.removeConnection(currentUser.getIdUser(), otherUser.get().getIdUser());
                        refreshCircle[0].run();
                        refreshSearch[0].run();
                    }
                ));
            }

            if (pendingList.getChildren().isEmpty()) {
                pendingList.getChildren().add(text("No pending requests.", 12, false, textMuted()));
            }
        };

        refreshSearch[0] = () -> {
            searchResults.getChildren().clear();
            String query = searchField.getText() == null ? "" : searchField.getText().trim();
            if (query.isEmpty()) {
                searchResults.getChildren().add(text("Start typing to discover residents.", 11, false, textMuted()));
                return;
            }

            List<User> users = userController.searchByName(query, currentUser.getIdUser(), 8);
            if (users.isEmpty()) {
                searchResults.getChildren().add(text("No users found.", 11, false, textMuted()));
                return;
            }

            for (User candidate : users) {
                if (candidate.getIdUser() == null || candidate.getIdUser().equals(currentUser.getIdUser())) {
                    continue;
                }

                Optional<UserRelationship> relationshipOpt = relationshipController.findRelationship(currentUser.getIdUser(), candidate.getIdUser());
                String actionLabel = relationshipController.getRelationshipLabel(currentUser.getIdUser(), candidate.getIdUser());
                Button actionBtn = new Button(actionLabel);
                actionBtn.setStyle(
                    "-fx-padding: 7 12 7 12;" +
                    "-fx-background-color: " + tm.getEffectiveAccentGradient() + ";" +
                    "-fx-text-fill: white; -fx-font-weight: 800;" +
                    "-fx-background-radius: 12; -fx-cursor: hand;"
                );

                actionBtn.setOnAction(evt -> {
                    String state = relationshipController.getRelationshipState(currentUser.getIdUser(), candidate.getIdUser());
                    boolean done;
                    switch (state) {
                        case "NONE" -> done = relationshipController.sendFriendRequest(currentUser.getIdUser(), candidate.getIdUser());
                        case "FRIENDS" -> done = relationshipController.removeConnection(currentUser.getIdUser(), candidate.getIdUser());
                        case "PENDING" -> done = relationshipController.removeConnection(currentUser.getIdUser(), candidate.getIdUser());
                        default -> done = false;
                    }

                    if (!done) {
                        showAvatarAlert(Alert.AlertType.ERROR, "Circle", "Could not update relationship.");
                        return;
                    }
                    refreshCircle[0].run();
                    refreshSearch[0].run();
                });

                if ("BLOCKED".equals(relationshipController.getRelationshipState(currentUser.getIdUser(), candidate.getIdUser()))) {
                    actionBtn.setDisable(true);
                }

                searchResults.getChildren().add(circleSearchRow(candidate, relationshipOpt, actionBtn, currentUser, () -> {
                    refreshCircle[0].run();
                    refreshSearch[0].run();
                }));
            }
        };

        javafx.animation.PauseTransition searchDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(220));
        searchDebounce.setOnFinished(e -> refreshSearch[0].run());
        searchField.textProperty().addListener((obs, oldValue, newValue) -> searchDebounce.playFromStart());

        friendsTab.setOnAction(e -> {
            styleCircleTabButton(friendsTab, true);
            styleCircleTabButton(pendingTab, false);
            animateCircleFace(pendingList, friendsGrid);
        });

        pendingTab.setOnAction(e -> {
            styleCircleTabButton(friendsTab, false);
            styleCircleTabButton(pendingTab, true);
            animateCircleFace(friendsGrid, pendingList);
        });

        refreshCircle[0].run();
        refreshSearch[0].run();

        search.getChildren().addAll(searchTitle, searchSub, searchField, searchResults);

        card.getChildren().addAll(top, switcher, listFace, search);
        return card;
    }

    private void styleCircleTabButton(Button btn, boolean active) {
        String background = active ? tm.getEffectiveAccentGradient() : surfaceSoft();
        String textFill = active ? "white" : textMuted();
        btn.setStyle(
            "-fx-background-color: " + background + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-font-weight: 800; -fx-background-radius: 12px; -fx-padding: 8 12 8 12;"
        );
    }

    private VBox circleFriendCard(User friend, Runnable onRemove) {
        String fullName = ((friend.getFirstName() == null ? "" : friend.getFirstName()) + " " + (friend.getLastName() == null ? "" : friend.getLastName())).trim();
        String display = fullName.isBlank() ? "User" : fullName;

        VBox c = new VBox(8);
        c.setPadding(new Insets(10));
        c.setAlignment(Pos.CENTER_LEFT);
        c.setStyle(shell(14, "rgba(255,255,255,0.03)", 0.10));

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        StackPane avatar = circleAvatarNode(friend, display, 38, 14);

        VBox meta = new VBox(2);
        meta.getChildren().addAll(
            text(display, 11, true, "#ffffff"),
            text(friend.getEmailUser() == null ? "" : friend.getEmailUser(), 10, false, textMuted())
        );
        row.getChildren().addAll(avatar, meta);

        Button removeBtn = new Button("Remove");
        removeBtn.setStyle(
            "-fx-padding: 6 10 6 10;" +
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-text-fill: white; -fx-font-weight: 800;" +
            "-fx-background-radius: 10; -fx-cursor: hand;"
        );
        removeBtn.setOnAction(e -> onRemove.run());

        c.getChildren().addAll(row, removeBtn);
        return c;
    }

    private HBox circlePendingRow(User otherUser, boolean incoming, Runnable onAccept, Runnable onDeclineOrCancel) {
        String fullName = ((otherUser.getFirstName() == null ? "" : otherUser.getFirstName()) + " " + (otherUser.getLastName() == null ? "" : otherUser.getLastName())).trim();
        String display = fullName.isBlank() ? "User" : fullName;

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle(shell(12, "rgba(255,255,255,0.03)", 0.10));

        StackPane avatar = circleAvatarNode(otherUser, display, 34, 12);

        VBox meta = new VBox(2);
        meta.getChildren().addAll(
            text(display, 12, true, "#ffffff"),
            text(incoming ? "Incoming request" : "Sent request", 10, false, textMuted())
        );
        HBox.setHgrow(meta, Priority.ALWAYS);

        HBox actions = new HBox(8);
        if (incoming) {
            Button accept = new Button("Accept");
            accept.setStyle(
                "-fx-padding: 6 10 6 10;" +
                "-fx-background-color: " + tm.getEffectiveAccentGradient() + ";" +
                "-fx-text-fill: white; -fx-font-weight: 800;" +
                "-fx-background-radius: 10; -fx-cursor: hand;"
            );
            accept.setOnAction(e -> onAccept.run());
            actions.getChildren().add(accept);
        }

        Button decline = new Button(incoming ? "Decline" : "Cancel");
        decline.setStyle(
            "-fx-padding: 6 10 6 10;" +
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-text-fill: white; -fx-font-weight: 800;" +
            "-fx-background-radius: 10; -fx-cursor: hand;"
        );
        decline.setOnAction(e -> onDeclineOrCancel.run());
        actions.getChildren().add(decline);

        row.getChildren().addAll(avatar, meta, actions);
        return row;
    }

    private HBox circleSearchRow(User candidate, Optional<UserRelationship> relationshipOpt, Button actionBtn, User currentUser, Runnable onRefresh) {
        String fullName = ((candidate.getFirstName() == null ? "" : candidate.getFirstName()) + " " + (candidate.getLastName() == null ? "" : candidate.getLastName())).trim();
        String display = fullName.isBlank() ? "User" : fullName;

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));
        row.setStyle(shell(10, "rgba(255,255,255,0.03)", 0.08));

        StackPane avatar = circleAvatarNode(candidate, display, 32, 12);

        VBox meta = new VBox(1);
        meta.getChildren().addAll(
            text(display, 11, true, "#ffffff"),
            text(candidate.getEmailUser() == null ? "" : candidate.getEmailUser(), 10, false, textMuted())
        );
        HBox.setHgrow(meta, Priority.ALWAYS);

        if (relationshipOpt.isPresent()) {
            UserRelationship r = relationshipOpt.get();
            boolean incoming = r.getUserSecondId() != null && r.getUserSecondId().equals(currentUser.getIdUser())
                && "PENDING_FIRST_SECOND".equalsIgnoreCase(r.getStatus());
            if (incoming && r.getId() != null) {
                Button acceptBtn = new Button("Accept");
                acceptBtn.setStyle(
                    "-fx-padding: 6 10 6 10;" +
                    "-fx-background-color: " + tm.getEffectiveAccentGradient() + ";" +
                    "-fx-text-fill: white; -fx-font-weight: 800;" +
                    "-fx-background-radius: 10; -fx-cursor: hand;"
                );
                UserRelationshipController relationshipController = new UserRelationshipController();
                acceptBtn.setOnAction(e -> {
                    relationshipController.acceptRequest(r.getId(), currentUser.getIdUser());
                    onRefresh.run();
                });
                row.getChildren().addAll(avatar, meta, acceptBtn, actionBtn);
                return row;
            }
        }

        row.getChildren().addAll(avatar, meta, actionBtn);
        return row;
    }

    private VBox createActivityPage() {
        VBox page = new VBox(20);
        page.setAlignment(Pos.TOP_CENTER);

        HBox tabNav = new HBox(8);
        tabNav.setAlignment(Pos.CENTER);
        tabNav.setPadding(new Insets(8));
        tabNav.setMaxWidth(1120);
        tabNav.setStyle(shell(999, "rgba(255,255,255,0.04)", 0.1));

        addDetailTab(tabNav, "Account", "account", new ProfileAccountSection().getRoot());
        addDetailTab(tabNav, "Forum", "forum", new ProfileForumSectionEnhanced().getRoot());
        addDetailTab(tabNav, "Events", "events", new ProfileEventsSectionEnhanced().getRoot());
        addDetailTab(tabNav, "Reclamation", "reclamation", new ProfileReclamationSectionEnhanced().getRoot());
        addDetailTab(tabNav, "Residence", "residence", createResidenceTab());

        VBox wrapper = new VBox(0);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setMaxWidth(1800);
        wrapper.prefWidthProperty().bind(root.widthProperty().multiply(0.95));
        wrapper.getChildren().addAll(tabNav);
        wrapper.getChildren().addAll(detailTabs.values());

        setDetailTab("account");
        page.getChildren().add(wrapper);
        return page;
    }

    private void addDetailTab(HBox nav, String label, String key, VBox page) {
        Button btn = new Button(label);
        btn.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 12));
        btn.setOnAction(e -> setDetailTab(key));
        styleDetailTabButton(btn, false);

        detailTabButtons.put(key, btn);
        detailTabs.put(key, page);
        nav.getChildren().add(btn);
    }

    private void setDetailTab(String key) {
        detailTabs.forEach((name, pane) -> {
            boolean active = name.equals(key);
            pane.setVisible(active);
            pane.setManaged(active);
        });
        detailTabButtons.forEach((name, btn) -> styleDetailTabButton(btn, name.equals(key)));
    }

    private void styleDetailTabButton(Button btn, boolean active) {
        String background = active ? tm.getEffectiveAccentGradient() : "transparent";
        String textFill = active ? "white" : textMuted();
        btn.setStyle(
            "-fx-background-color: " + background + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-font-weight: 800; -fx-background-radius: 999px; -fx-padding: 11 18 11 18;"
        );
    }

    private VBox createResidenceTab() {
        VBox tab = new VBox(16);
        tab.setPadding(new Insets(16, 0, 0, 0));

        VBox card = cardShell();
        card.setPadding(new Insets(22));
        card.getChildren().add(text("Your Residences", 24, true, "#ffffff"));
        card.getChildren().add(residenceItem("Horizon Gardens", "Bloc A - Apt 302", "Owner", "Available", "Parking: Yes"));
        card.getChildren().add(residenceItem("Horizon Park", "Bloc C - Apt 104", "Tenant", "Occupied", "Parking: No"));

        tab.getChildren().add(card);
        return tab;
    }

    private HBox infoLine(String k, String v) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Text key = text(k, 13, true, textMuted());
        Text val = text(v, 13, false, "#ffffff");
        HBox.setHgrow(key, Priority.NEVER);
        HBox.setHgrow(val, Priority.ALWAYS);
        row.getChildren().addAll(key, spacer(26), val);
        return row;
    }

    private VBox listRow(String type, String title, String status) {
        VBox row = new VBox(4);
        row.setPadding(new Insets(14));
        row.setStyle(shell(14, tm.toRgba(tm.getAccentHex(), 0.06), 0.13));
        row.getChildren().addAll(
            text(type + " - " + status, 11, true, tm.getAccentHex()),
            text(title, 14, true, "#ffffff")
        );
        return row;
    }

    private VBox forumItem(String type, String title, String category, String date) {
        VBox row = new VBox(8);
        row.setPadding(new Insets(14));
        row.setStyle(shell(14, tm.toRgba(tm.getAccentHex(), 0.05), 0.10));

        HBox top = new HBox();
        Text left = text(type + " - " + category, 11, true, tm.getAccentHex());
        Text right = text(date, 11, false, textMuted());
        HBox.setHgrow(left, Priority.ALWAYS);
        top.getChildren().addAll(left, right);

        row.getChildren().addAll(top, text(title, 14, true, "#ffffff"));
        return row;
    }

    private VBox eventItem(String title, String date, String location, String status, String type) {
        VBox row = new VBox(10);
        row.setPadding(new Insets(16));
        row.setStyle(shell(16, "rgba(255,255,255,0.03)", 0.08));

        HBox top = new HBox(10);
        VBox iconWrap = new VBox(text("EV", 11, true, tm.getAccentHex()));
        iconWrap.setAlignment(Pos.CENTER);
        iconWrap.setMinSize(42, 42);
        iconWrap.setStyle(shell(10, tm.toRgba(tm.getAccentHex(), 0.10), 0.18));

        VBox titleMeta = new VBox(5,
            text(title, 15, true, "#ffffff"),
            text(date + " | " + location, 12, false, textMuted())
        );
        HBox.setHgrow(titleMeta, Priority.ALWAYS);

        Text statusTag = text(status, 11, true, "#ffffff");
        VBox statusWrap = new VBox(statusTag);
        statusWrap.setPadding(new Insets(6, 10, 6, 10));
        statusWrap.setStyle(shell(999, tm.toRgba(tm.getAccentHex(), 0.18), 0.25));

        top.getChildren().addAll(iconWrap, titleMeta, statusWrap);
        row.getChildren().addAll(top, text("Type: " + type + " | Enrolled: 12/40", 12, false, textMuted()));
        return row;
    }

    private VBox reclamationItem(String title, String status, String submitted) {
        VBox row = new VBox(8);
        row.setPadding(new Insets(16));
        row.setStyle(shell(16, "rgba(255,255,255,0.03)", 0.08));

        HBox top = new HBox();
        Text titleText = text(title, 14, true, "#ffffff");
        Text statusText = text(status, 11, true, tm.getAccentHex());
        HBox.setHgrow(titleText, Priority.ALWAYS);
        top.getChildren().addAll(titleText, statusText);

        row.getChildren().addAll(top, text(submitted, 12, false, textMuted()));
        return row;
    }

    private VBox residenceItem(String residence, String unit, String relation, String availability, String parking) {
        VBox row = new VBox(8);
        row.setPadding(new Insets(16));
        row.setStyle(shell(16, "rgba(255,255,255,0.03)", 0.08));

        HBox top = new HBox();
        VBox info = new VBox(4,
            text(residence, 15, true, "#ffffff"),
            text(unit, 12, false, textMuted())
        );
        HBox.setHgrow(info, Priority.ALWAYS);
        Text relationTag = text(relation, 11, true, tm.getAccentHex());
        top.getChildren().addAll(info, relationTag);

        row.getChildren().addAll(top, text(availability + " | " + parking, 12, false, textMuted()));
        return row;
    }

    private VBox pillCard(String label, String value) {
        VBox pill = new VBox(4,
            text(label, 11, true, textMuted()),
            text(value, 13, true, "#ffffff")
        );
        pill.setPadding(new Insets(10, 12, 10, 12));
        pill.setStyle(shell(12, "rgba(255,255,255,0.04)", 0.09));
        return pill;
    }

    private Button smallPill(String text, boolean active) {
        Button b = new Button(text);
        String background = active ? "rgba(255,255,255,0.15)" : "transparent";
        String textFill = active ? "#ffffff" : textMuted();
        b.setStyle(
            "-fx-background-color: " + background + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 999px; -fx-padding: 7 12 7 12;"
        );
        return b;
    }

    private Button forumTabPill(String text, boolean active) {
        Button b = new Button(text);
        String background = active ? "rgba(255,255,255,0.10)" : "transparent";
        String textFill = active ? "#ffffff" : textMuted();
        b.setStyle(
            "-fx-background-color: " + background + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 999px; -fx-padding: 9 12 9 12;"
        );
        return b;
    }

    private Button pagePill(String text, boolean active) {
        Button b = new Button(text);
        b.setMinSize(34, 34);
        String background = active ? tm.getEffectiveAccentGradient() : "transparent";
        String textFill = active ? "#ffffff" : textMuted();
        b.setStyle(
            "-fx-background-color: " + background + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 999px;"
        );
        return b;
    }

    private VBox createActionTile(String label, String actionId, VBox switcherView, Button showActionsBtn) {
        VBox tile = new VBox(6);
        tile.setAlignment(Pos.CENTER);
        tile.setPadding(new Insets(13, 8, 13, 8));
        tile.setStyle(
            "-fx-background-color: rgba(255,255,255,0.04);" +
            "-fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1px;" +
            "-fx-background-radius: 16px; -fx-border-radius: 16px;"
        );
        tile.setCursor(javafx.scene.Cursor.HAND);
        tile.setMinHeight(78);
        
        javafx.scene.text.Text icon = text(label.split(" ")[0], 18, true, tm.getAccentHex());
        javafx.scene.text.Text title = text(label.substring(label.indexOf(" ")+1).toUpperCase(), 10, true, "rgba(255,255,255,0.70)");
        
        tile.getChildren().addAll(icon, title);
        
        tile.setOnMouseEntered(e -> {
            tile.setStyle(
                "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.12) + ";" +
                "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.30) + "; -fx-border-width: 1px;" +
                "-fx-background-radius: 16px; -fx-border-radius: 16px;"
            );
            tile.setTranslateY(-3);
            title.setFill(Color.WHITE);
        });
        tile.setOnMouseExited(e -> {
            tile.setStyle(
                "-fx-background-color: rgba(255,255,255,0.04);" +
                "-fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1px;" +
                "-fx-background-radius: 16px; -fx-border-radius: 16px;"
            );
            tile.setTranslateY(0);
            title.setFill(Color.web("rgba(255,255,255,0.70)"));
        });
        tile.setOnMouseClicked(e -> {
            awardInteractionXp(1);
            showActionPanel(actionId, switcherView, showActionsBtn);
        });
        
        return tile;
    }
    
    private void showActionPanel(String actionId, VBox switcherView, Button showActionsBtn) {
        StackPane parent = (StackPane) switcherView.getParent();
        VBox detailView = (VBox) parent.getChildren().get(2);
        detailView.getChildren().clear();
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 12, 0));
        header.setStyle("-fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 0 0 1 0;");
        
        javafx.scene.control.Button backBtn = new javafx.scene.control.Button("←");
        backBtn.setStyle(
            "-fx-min-width: 30px; -fx-min-height: 30px;" +
            "-fx-max-width: 30px; -fx-max-height: 30px;" +
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-border-color: rgba(255,255,255,0.15); -fx-border-width: 1px;" +
            "-fx-border-radius: 999px; -fx-background-radius: 999px;" +
            "-fx-text-fill: rgba(255,255,255,0.75); -fx-font-weight: 700; -fx-cursor: hand;"
        );
        backBtn.setOnAction(e -> hideActionPanel(switcherView, detailView, showActionsBtn));
        
        javafx.scene.text.Text title = text(getTitleForAction(actionId), 14, true, "rgba(255,255,255,0.88)");
        
        header.getChildren().addAll(backBtn, title);
        detailView.getChildren().add(header);
        
        // Add content based on action
        VBox content = new VBox(12);
        content.setPadding(new Insets(12, 0, 0, 0));
        content.setStyle("-fx-background-color: transparent;");
        
        ScrollPane scrollPane = null;
        if ("settings".equals(actionId)) {
            scrollPane = new ScrollPane(content);
            scrollPane.setFitToWidth(true);
            scrollPane.setMaxHeight(190);
            scrollPane.setPrefHeight(190);
            scrollPane.setStyle("-fx-control-inner-background: transparent; -fx-padding: 0; -fx-border-color: transparent;");
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        }
        
        switch(actionId) {
            case "2fa" -> {
                User user = SessionManager.getInstance().getCurrentUser();
                content.getChildren().add(text("Two-Factor Authentication", 13, true, textSoft()));
                content.getChildren().add(text("Status: " + (user != null && user.isVerified() ? "Enabled" : "Disabled"), 11, false, textMuted()));
                content.getChildren().add(text("Protect your account with two-factor authentication using SMS or authenticator apps.", 11, false, textMuted()));
                javafx.scene.control.Button enableBtn = new javafx.scene.control.Button("Enable 2FA");
                enableBtn.setStyle("-fx-padding: 11 16 11 16; -fx-background-color: " + tm.getEffectiveAccentGradient() + "; -fx-text-fill: white; -fx-background-radius: 14; -fx-font-weight: 800; -fx-cursor: hand;");
                content.getChildren().add(enableBtn);
            }
            case "biometrics" -> {
                content.getChildren().add(text("Biometric Authentication", 13, true, textSoft()));
                content.getChildren().add(text("Use your device biometrics (FaceID, TouchID, Windows Hello) to log in faster.", 11, false, textMuted()));
                content.getChildren().add(text("Loading credentials...", 11, false, "rgba(255,255,255,0.40)"));
                javafx.scene.control.Button setupBtn = new javafx.scene.control.Button("Register New Device");
                setupBtn.setStyle("-fx-padding: 11 16 11 16; -fx-background-color: " + tm.getEffectiveAccentGradient() + "; -fx-text-fill: white; -fx-background-radius: 14; -fx-font-weight: 800; -fx-cursor: hand;");
                content.getChildren().add(setupBtn);
            }
            case "faceid" -> {
                content.getChildren().add(text("Face Recognition", 13, true, textSoft()));
                content.getChildren().add(text("Protect your account with local Face ID. This only works on this PC.", 11, false, textMuted()));
                javafx.scene.control.TextField pinField = new javafx.scene.control.TextField();
                pinField.setPromptText("Enter a local PIN for encryption");
                pinField.setStyle("-fx-padding: 9 12 9 12; -fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12;");
                content.getChildren().add(pinField);
                content.getChildren().add(text("This PIN encrypts your Face ID locally.", 10, false, "rgba(255,255,255,0.40)"));
                javafx.scene.control.Button enrollBtn = new javafx.scene.control.Button("Start Enrollment");
                enrollBtn.setStyle("-fx-padding: 11 16 11 16; -fx-background-color: " + tm.getEffectiveAccentGradient() + "; -fx-text-fill: white; -fx-background-radius: 14; -fx-font-weight: 800; -fx-cursor: hand;");
                content.getChildren().add(enrollBtn);
            }
            case "settings" -> {
                User user = SessionManager.getInstance().getCurrentUser();
                Profile profile = currentProfile();
                content.getChildren().add(text("Profile Settings", 13, true, textSoft()));
                
                // Bio field
                content.getChildren().add(text("Bio", 11, true, "rgba(255,255,255,0.70)"));
                javafx.scene.control.TextArea bioArea = new javafx.scene.control.TextArea();
                bioArea.setPromptText("Tell us a bit about yourself...");
                bioArea.setWrapText(true);
                bioArea.setStyle("-fx-padding: 9; -fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 1; -fx-control-inner-background: rgba(255,255,255,0.05); -fx-background-radius: 12; -fx-border-radius: 12;");
                bioArea.setPrefHeight(50);
                bioArea.setText(profile != null && profile.getDescriptionProfile() != null ? profile.getDescriptionProfile() : "");
                content.getChildren().add(bioArea);
                content.getChildren().add(text(" ", 8, false, "transparent"));
                
                // Phone field
                content.getChildren().add(text("Phone", 11, true, "rgba(255,255,255,0.70)"));
                javafx.scene.control.TextField phoneField = new javafx.scene.control.TextField();
                phoneField.setPromptText("Phone number");
                phoneField.setStyle("-fx-padding: 9 12 9 12; -fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12;");
                phoneField.setText(user != null && user.getPhone() != null ? user.getPhone() : "");
                content.getChildren().add(phoneField);
                content.getChildren().add(text(" ", 8, false, "transparent"));
                
                // Theme, Language, Timezone row
                content.getChildren().add(text("Preferences", 11, true, "rgba(255,255,255,0.70)"));
                HBox prefRow = new HBox(10);
                prefRow.setStyle("-fx-fill-height: true;");
                
                javafx.scene.control.ComboBox<String> themeCombo = new javafx.scene.control.ComboBox<>();
                themeCombo.getItems().addAll("Dark", "Light", "Auto");
                themeCombo.setValue(themeFromCode(profile == null ? null : profile.getTheme()));
                themeCombo.setStyle("-fx-padding: 8; -fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.12); -fx-background-radius: 12; -fx-border-radius: 12;");
                HBox.setHgrow(themeCombo, Priority.ALWAYS);
                
                javafx.scene.control.ComboBox<String> langCombo = new javafx.scene.control.ComboBox<>();
                langCombo.getItems().addAll("English", "French", "Arabic", "Spanish");
                langCombo.setValue(languageFromLocale(profile == null ? null : profile.getLocale()));
                langCombo.setStyle("-fx-padding: 8; -fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.12); -fx-background-radius: 12; -fx-border-radius: 12;");
                HBox.setHgrow(langCombo, Priority.ALWAYS);
                
                javafx.scene.control.TextField tzField = new javafx.scene.control.TextField();
                tzField.setPromptText("UTC+1");
                tzField.setStyle("-fx-padding: 9 12 9 12; -fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12;");
                tzField.setText(timezoneText(profile == null ? null : profile.getTimezone()));
                HBox.setHgrow(tzField, Priority.ALWAYS);
                
                prefRow.getChildren().addAll(themeCombo, langCombo, tzField);
                content.getChildren().add(prefRow);
                content.getChildren().add(text(" ", 8, false, "transparent"));
                
                // Security section
                content.getChildren().add(text("SECURITY", 10, true, "rgba(255,255,255,0.50)"));
                content.getChildren().add(text("Current Password", 11, true, "rgba(255,255,255,0.70)"));
                javafx.scene.control.PasswordField currentPwdField = new javafx.scene.control.PasswordField();
                currentPwdField.setPromptText("Required to change password");
                currentPwdField.setStyle("-fx-padding: 9 12 9 12; -fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12;");
                content.getChildren().add(currentPwdField);
                content.getChildren().add(text(" ", 8, false, "transparent"));
                
                content.getChildren().add(text("New Password", 11, true, "rgba(255,255,255,0.70)"));
                javafx.scene.control.PasswordField newPwdField = new javafx.scene.control.PasswordField();
                newPwdField.setPromptText("Min 6 characters");
                newPwdField.setStyle("-fx-padding: 9 12 9 12; -fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12;");
                content.getChildren().add(newPwdField);
                content.getChildren().add(text(" ", 8, false, "transparent"));
                
                content.getChildren().add(text("Confirm Password", 11, true, "rgba(255,255,255,0.70)"));
                javafx.scene.control.PasswordField confirmPwdField = new javafx.scene.control.PasswordField();
                confirmPwdField.setPromptText("Confirm new password");
                confirmPwdField.setStyle("-fx-padding: 9 12 9 12; -fx-background-color: rgba(255,255,255,0.05); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12;");
                content.getChildren().add(confirmPwdField);
                content.getChildren().add(text(" ", 4, false, "transparent"));
                
                javafx.scene.control.Button saveBtn = new javafx.scene.control.Button("Save Changes");
                saveBtn.setStyle("-fx-padding: 11 16 11 16; -fx-background-color: " + tm.getEffectiveAccentGradient() + "; -fx-text-fill: white; -fx-background-radius: 14; -fx-font-weight: 800; -fx-cursor: hand;");
                saveBtn.setOnAction(e -> {
                    User sessionUser = SessionManager.getInstance().getCurrentUser();
                    if (sessionUser == null || sessionUser.getIdUser() == null) {
                        showAvatarAlert(Alert.AlertType.ERROR, "Settings", "No active user session.");
                        return;
                    }

                    UserController userController = new UserController();
                    ProfileController profileController = new ProfileController();

                    String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
                    String currentPwd = currentPwdField.getText() == null ? "" : currentPwdField.getText().trim();
                    String newPwd = newPwdField.getText() == null ? "" : newPwdField.getText().trim();
                    String confirmPwd = confirmPwdField.getText() == null ? "" : confirmPwdField.getText().trim();

                    boolean wantsPasswordChange = !currentPwd.isEmpty() || !newPwd.isEmpty() || !confirmPwd.isEmpty();
                    if (wantsPasswordChange) {
                        if (currentPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
                            showAvatarAlert(Alert.AlertType.ERROR, "Settings", "Fill current, new and confirm password fields.");
                            return;
                        }
                        if (!matchesPassword(currentPwd, sessionUser.getPasswordUser())) {
                            showAvatarAlert(Alert.AlertType.ERROR, "Settings", "Current password is incorrect.");
                            return;
                        }
                        if (newPwd.length() < 6) {
                            showAvatarAlert(Alert.AlertType.ERROR, "Settings", "New password must be at least 6 characters.");
                            return;
                        }
                        if (!newPwd.equals(confirmPwd)) {
                            showAvatarAlert(Alert.AlertType.ERROR, "Settings", "New password and confirmation do not match.");
                            return;
                        }
                    }

                    User userToSave = sessionUser;
                    userToSave.setPhone(phone.isEmpty() ? null : phone);
                    if (wantsPasswordChange) {
                        userToSave.setPasswordUser(BCrypt.hashpw(newPwd, BCrypt.gensalt(13)));
                    }

                    boolean userUpdated = userController.userEdit(userToSave);
                    if (!userUpdated) {
                        showAvatarAlert(Alert.AlertType.ERROR, "Settings", "Failed to update user account settings.");
                        return;
                    }

                    Profile profileToSave = getOrCreateCurrentProfile(sessionUser);
                    if (profileToSave == null) {
                        showAvatarAlert(Alert.AlertType.ERROR, "Settings", "Failed to load profile settings.");
                        return;
                    }

                    profileToSave.setDescriptionProfile(bioArea.getText() == null ? "" : bioArea.getText().trim());
                    profileToSave.setTheme(themeToCode(themeCombo.getValue()));
                    profileToSave.setLocale(languageToLocale(langCombo.getValue()));
                    profileToSave.setTimezone(parseTimezone(tzField.getText()));
                    if (profileToSave.getSettingsJson() == null || profileToSave.getSettingsJson().isBlank()) {
                        profileToSave.setSettingsJson("{}");
                    }

                    boolean profileUpdated = profileController.updateProfile(profileToSave);
                    if (!profileUpdated) {
                        showAvatarAlert(Alert.AlertType.ERROR, "Settings", "Failed to update profile preferences.");
                        return;
                    }

                    SessionManager.getInstance().setCurrentUser(userToSave);
                    SessionManager.getInstance().setCurrentProfile(profileToSave);
                    currentPwdField.clear();
                    newPwdField.clear();
                    confirmPwdField.clear();
                    showAvatarAlert(Alert.AlertType.INFORMATION, "Settings", "Settings saved successfully.");
                });
                content.getChildren().add(saveBtn);
            }
            case "host" -> {
                content.getChildren().add(text("Host Spotlight", 13, true, textSoft()));
                content.getChildren().add(text("Start a video spotlight session and invite residents to join.", 11, false, textMuted()));
                javafx.scene.control.Button startBtn = new javafx.scene.control.Button("Start Spotlight");
                startBtn.setStyle("-fx-padding: 11 16 11 16; -fx-background-color: " + tm.getEffectiveAccentGradient() + "; -fx-text-fill: white; -fx-background-radius: 14; -fx-font-weight: 800; -fx-cursor: hand;");
                content.getChildren().add(startBtn);
            }
            case "join" -> {
                content.getChildren().add(text("Join by Invitation Code", 13, true, textSoft()));
                content.getChildren().add(text("Enter a room code to join an existing spotlight session.", 11, false, textMuted()));
                javafx.scene.control.Button joinBtn = new javafx.scene.control.Button("Join Session");
                joinBtn.setStyle("-fx-padding: 11 16 11 16; -fx-background-color: " + tm.getEffectiveAccentGradient() + "; -fx-text-fill: white; -fx-background-radius: 14; -fx-font-weight: 800; -fx-cursor: hand;");
                content.getChildren().add(joinBtn);
            }
        }
        
        if (scrollPane != null) {
            detailView.getChildren().add(scrollPane);
        } else {
            detailView.getChildren().add(content);
        }
        animateHeroTransition(switcherView, detailView);
    }
    
    private void hideActionPanel(VBox switcherView, VBox detailView, Button showActionsBtn) {
        styleQuickActionsButton(showActionsBtn, true);
        animateHeroTransition(detailView, switcherView);
    }
    
    private String getTitleForAction(String actionId) {
        return switch(actionId) {
            case "2fa" -> "Two-Factor Auth";
            case "biometrics" -> "Biometrics";
            case "faceid" -> "Face ID";
            case "settings" -> "Settings";
            case "host" -> "Host Spotlight";
            case "join" -> "Join by Code";
            default -> "Action";
        };
    }
    
    private String formatRole(String role) {
        return switch(role) {
            case "ADMIN" -> "🔐 Administrator";
            case "OWNER" -> "👑 Property Owner";
            case "RESIDENT" -> "👤 Resident";
            default -> role;
        };
    }

    private void handleProfileAvatarUpload(User user, Circle avatarCircle, Text avatarText) {
        if (user == null || user.getIdUser() == null || root.getScene() == null) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Profile Picture");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File selected = chooser.showOpenDialog(root.getScene().getWindow());
        if (selected == null) {
            return;
        }

        try {
            Profile profile = getOrCreateCurrentProfile(user);
            if (profile == null || profile.getIdProfile() == null) {
                showAvatarAlert(Alert.AlertType.ERROR, "Avatar", "Could not load your profile record.");
                return;
            }

            byte[] data = Files.readAllBytes(selected.toPath());
            ProfileAvatarController controller = new ProfileAvatarController();
            ProfileAvatarController.AvatarUpdateResult result = controller.updateAvatar(profile, data, selected.getName());
            if (!result.isSuccess()) {
                showAvatarAlert(Alert.AlertType.ERROR, "Avatar", result.getMessage());
                return;
            }

            profile.setAvatar(result.getImagePath());
            SessionManager.getInstance().setCurrentProfile(profile);

            boolean hasAvatar = applyAvatarFill(avatarCircle);
            avatarText.setText(currentInitial(user));
            avatarText.setVisible(!hasAvatar);
            avatarText.setManaged(!hasAvatar);
            showAvatarAlert(Alert.AlertType.INFORMATION, "Avatar", "Profile picture updated.");
        } catch (Exception ex) {
            showAvatarAlert(Alert.AlertType.ERROR, "Avatar", "Failed to upload image.");
        }
    }

    private boolean applyAvatarFill(Circle avatarCircle) {
        Profile profile = currentProfile();
        String avatarPath = profile == null ? null : profile.getAvatar();
        if (avatarPath != null && !avatarPath.isBlank()) {
            Image img = ImageLoaderUtil.loadProfileAvatar(avatarPath, false);
            if (img != null && !img.isError()) {
                avatarCircle.setFill(new ImagePattern(img));
                return true;
            }
        }
        avatarCircle.setFill(Color.web(tm.getAccentHex()));
        return false;
    }

    private Profile currentProfile() {
        SessionManager session = SessionManager.getInstance();
        Profile profile = session.getCurrentProfile();
        if (profile != null) {
            return profile;
        }

        User user = session.getCurrentUser();
        if (user == null || user.getIdUser() == null) {
            return null;
        }

        ProfileService profileService = new ProfileService();
        profile = profileService.findOneByUserId(user.getIdUser()).orElse(null);
        if (profile != null) {
            session.setCurrentProfile(profile);
        }
        return profile;
    }

    private Profile getOrCreateCurrentProfile(User user) {
        Profile existing = currentProfile();
        if (existing != null) {
            return existing;
        }

        ProfileService profileService = new ProfileService();
        Profile created = new Profile();
        created.setUserId(user.getIdUser());
        created.setAvatar(null);
        created.setLocale("en");
        created.setTheme(0);
        created.setTimezone(1);
        created.setDescriptionProfile("");
        created.setSettingsJson("{}");

        Optional<Integer> id = profileService.createProfile(created);
        if (id.isEmpty()) {
            return null;
        }

        Profile loaded = profileService.findById(id.get()).orElse(null);
        if (loaded != null) {
            SessionManager.getInstance().setCurrentProfile(loaded);
        }
        return loaded;
    }

    private String currentInitial(User user) {
        if (user != null) {
            String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
            String last = user.getLastName() == null ? "" : user.getLastName().trim();
            String fullName = (first + " " + last).trim();
            if (!fullName.isBlank()) {
                return fullName.substring(0, 1).toUpperCase();
            }
            String email = user.getEmailUser() == null ? "" : user.getEmailUser().trim();
            if (!email.isBlank()) {
                return email.substring(0, 1).toUpperCase();
            }
        }
        return "U";
    }

    private void showAvatarAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean matchesPassword(String raw, String stored) {
        if (stored == null || stored.isBlank()) {
            return false;
        }

        try {
            if (stored.startsWith("$2y$")) {
                return BCrypt.checkpw(raw, "$2a$" + stored.substring(4));
            }
            if (stored.startsWith("$2a$") || stored.startsWith("$2b$")) {
                return BCrypt.checkpw(raw, stored);
            }
        } catch (IllegalArgumentException ignored) {
            return false;
        }

        return raw.equals(stored);
    }

    private String themeFromCode(Integer themeCode) {
        if (themeCode == null) {
            return "Dark";
        }
        return switch (themeCode) {
            case 1 -> "Dark";
            case 2 -> "Auto";
            default -> "Light";
        };
    }

    private Integer themeToCode(String themeValue) {
        if (themeValue == null) {
            return 1;
        }
        return switch (themeValue) {
            case "Light" -> 0;
            case "Auto" -> 2;
            default -> 1;
        };
    }

    private String languageFromLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return "English";
        }
        String normalized = locale.toLowerCase();
        return switch (normalized) {
            case "fr", "fr_fr" -> "French";
            case "ar", "ar_tn" -> "Arabic";
            case "es", "es_es" -> "Spanish";
            default -> "English";
        };
    }

    private String languageToLocale(String language) {
        if (language == null) {
            return "en";
        }
        return switch (language) {
            case "French" -> "fr";
            case "Arabic" -> "ar";
            case "Spanish" -> "es";
            default -> "en";
        };
    }

    private Integer parseTimezone(String input) {
        if (input == null || input.isBlank()) {
            return 1;
        }
        String normalized = input.trim().toUpperCase().replace("UTC", "").replace("GMT", "").trim();
        if (normalized.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private String timezoneText(Integer tz) {
        if (tz == null) {
            return "UTC+1";
        }
        if (tz == 0) {
            return "UTC";
        }
        return "UTC" + (tz > 0 ? "+" : "") + tz;
    }

    private void installXpActionTracking(javafx.scene.Parent parent) {
        parent.addEventFilter(ActionEvent.ACTION, event -> {
            Object source = event.getSource();
            if (source instanceof Button) {
                awardInteractionXp(1);
            }
        });
    }

    private void awardInteractionXp(int xp) {
        NavigationManager.getInstance().awardInteractionXp(Math.max(0, xp));
    }

    private void animateCircleFace(javafx.scene.Node from, javafx.scene.Node to) {
        if (from == to || to.isVisible()) {
            return;
        }

        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(150), from);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            from.setVisible(false);
            from.setManaged(false);

            to.setVisible(true);
            to.setManaged(true);
            to.setOpacity(0);

            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(180), to);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private void animateBadgePulse(Text badge) {
        javafx.animation.ScaleTransition pulse = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(180), badge);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.08);
        pulse.setToY(1.08);
        pulse.setCycleCount(2);
        pulse.setAutoReverse(true);
        pulse.play();
    }

    private StackPane circleAvatarNode(User user, String fallbackDisplay, double size, int fontSize) {
        Circle circle = new Circle(size / 2.0);
        circle.setFill(Color.web(tm.toRgba(tm.getAccentHex(), 0.20)));
        circle.setStroke(Color.web(tm.toRgba(tm.getAccentHex(), 0.34)));
        circle.setStrokeWidth(1.4);

        String initial = fallbackDisplay == null || fallbackDisplay.isBlank()
            ? "U"
            : fallbackDisplay.substring(0, 1).toUpperCase();
        Text initialText = text(initial, fontSize, true, "#ffffff");

        boolean hasAvatar = applyAvatarFillForUser(circle, user);
        initialText.setVisible(!hasAvatar);
        initialText.setManaged(!hasAvatar);

        StackPane avatar = new StackPane(circle, initialText);
        avatar.setMinSize(size, size);
        avatar.setPrefSize(size, size);
        return avatar;
    }

    private boolean applyAvatarFillForUser(Circle avatarCircle, User user) {
        if (user == null || user.getIdUser() == null) {
            return false;
        }

        ProfileService profileService = new ProfileService();
        Profile userProfile = profileService.findOneByUserId(user.getIdUser()).orElse(null);
        String avatarPath = userProfile == null ? null : userProfile.getAvatar();
        if (avatarPath == null || avatarPath.isBlank()) {
            return false;
        }

        Image img = ImageLoaderUtil.loadProfileAvatar(avatarPath, false);
        if (img == null || img.isError()) {
            return false;
        }
        avatarCircle.setFill(new ImagePattern(img));
        return true;
    }

    private VBox standingPoint(String label, boolean active) {
        VBox point = new VBox(6);
        point.setAlignment(Pos.CENTER);
        Circle dot = new Circle(10);
        dot.setFill(active ? Color.web(tm.getAccentHex()) : Color.web("#2f3136"));
        Text txt = text(label, 11, true, active ? "#ffffff" : textMuted());
        point.getChildren().addAll(dot, txt);
        return point;
    }

    private VBox friendCard(String name) {
        VBox c = new VBox(8);
        c.setAlignment(Pos.CENTER);
        StackPane avatar = new StackPane();
        avatar.setMinSize(60, 60);
        avatar.setStyle(shell(16, "rgba(255,255,255,0.05)", 0.10));
        avatar.getChildren().add(text("U", 18, true, textSoft()));
        c.getChildren().addAll(avatar, text(name, 11, true, textMuted()));
        return c;
    }

    private VBox statPill(String label, String value) {
        VBox pill = new VBox(4, text(label, 11, true, textMuted()), text(value, 13, true, "#ffffff"));
        pill.setPadding(new Insets(10, 14, 10, 14));
        pill.setStyle(shell(12, "rgba(255,255,255,0.04)", 0.09));
        return pill;
    }

    private Button tabChip(String label, boolean active) {
        Button b = new Button(label);
        String background = active ? tm.getEffectiveAccentGradient() : surfaceSoft();
        String textFill = active ? "white" : textMuted();
        b.setStyle(
            "-fx-background-color: " + background + ";" +
            "-fx-text-fill: " + textFill + ";" +
            "-fx-font-weight: 800; -fx-background-radius: 12px; -fx-padding: 8 12 8 12;"
        );
        return b;
    }

    private Text badge(String label) {
        return text(label, 10, true, "#ffffff");
    }

    private VBox cardShell() {
        VBox c = new VBox(16);
        c.setStyle(shell(28, surfaceCard(), 0.16));
        return c;
    }

    private String shell(double radius, String bg, double borderOpacity) {
        return "-fx-background-color: " + bg + ";"
            + "-fx-border-color: rgba(255,255,255," + borderOpacity + ");"
            + "-fx-border-width: 1px;"
            + "-fx-background-radius: " + radius + "px;"
            + "-fx-border-radius: " + radius + "px;";
    }

    private String surfaceCard() {
        return tm.isDarkMode()
            ? "linear-gradient(from 0% 0% to 100% 100%, rgba(10,10,10,0.94) 0%, rgba(14,14,14,0.94) 62%, " + tm.toRgba(tm.getAccentHex(), 0.10) + " 100%)"
            : "linear-gradient(from 0% 0% to 100% 100%, rgba(255,255,255,0.98) 0%, rgba(248,250,252,0.96) 100%)";
    }

    private String surfaceSoft() {
        return tm.isDarkMode() ? "rgba(255,255,255,0.09)" : "rgba(15,23,42,0.08)";
    }

    private String borderSoft() {
        return tm.isDarkMode() ? tm.toRgba(tm.getAccentHex(), 0.34) : "rgba(15,23,42,0.16)";
    }

    private String textSoft() {
        return tm.isDarkMode() ? "rgba(255,255,255,0.93)" : "rgba(15,23,42,0.90)";
    }

    private String textMuted() {
        return tm.isDarkMode() ? "rgba(255,255,255,0.79)" : "rgba(30,41,59,0.82)";
    }

    private Pane spacer(double width) {
        Pane p = new Pane();
        p.setMinWidth(width);
        return p;
    }

    private Text text(String value, int size, boolean bold, String color) {
        Text t = new Text(value);
        t.setFont(Font.font(
            bold ? MainApplication.getInstance().getBoldFontFamily() : MainApplication.getInstance().getLightFontFamily(),
            bold ? FontWeight.BOLD : FontWeight.NORMAL,
            size
        ));
        t.setFill(Color.web(color));
        return t;
    }

    @Override
    public Pane getRoot() {
        return root;
    }

    @Override
    public void cleanup() {
        // No resources to release in this static UI replica.
    }
}


