package com.syndicati.components.shared;

import com.syndicati.MainApplication;
import com.syndicati.models.user.Profile;
import com.syndicati.models.user.User;
import com.syndicati.utils.navigation.NavigationManager;
import com.syndicati.utils.theme.ThemeManager;
import com.syndicati.utils.session.SessionManager;
import com.syndicati.utils.security.AccessControlService;
import com.syndicati.utils.image.ImageLoaderUtil;
import com.syndicati.services.user.profile.ProfileService;
import javafx.animation.PauseTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DynamicHeader {

    private final StackPane root;
    private final ThemeManager themeManager;

    private Runnable backgroundUpdateCallback;

    private HBox navbar;
    private HBox leftSection;
    private HBox rightSection;
    private HBox tabsPill;

    private final Map<String, Button> tabButtons = new LinkedHashMap<>();
    private final Map<String, Popup> tabDropdownPopups = new HashMap<>();

    private VBox notificationDropdown;
    private VBox profileDropdown;
    private Node profileAnchor;
    private Node notificationAnchor;

    private PauseTransition closeTabDelay;
    private PauseTransition closeProfileDelay;
    private PauseTransition closeNotificationDelay;
    private String activeTab = "home";

    private Circle profileTriggerAvatarCircle;
    private Label profileTriggerInitialLabel;

    public DynamicHeader() {
        this.root = new StackPane();
        this.themeManager = ThemeManager.getInstance();
        buildLayout();
        applyThemeStyling();
    }

    private void buildLayout() {
        root.setPadding(new Insets(10, 30, 0, 30));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: transparent;");
        root.setPickOnBounds(false);
        root.setMinHeight(Region.USE_PREF_SIZE);
        root.setPrefHeight(Region.USE_COMPUTED_SIZE);
        root.setMaxHeight(Region.USE_PREF_SIZE);

        navbar = new HBox();
        navbar.setAlignment(Pos.CENTER_LEFT);
        navbar.setPadding(new Insets(12, 22, 12, 22));
        navbar.setSpacing(16);
        navbar.setMinHeight(80);
        navbar.setPrefHeight(80);
        navbar.setPickOnBounds(true);

        leftSection = buildLeftSection();
        HBox centerSection = buildCenterSection();
        rightSection = buildRightSection();

        leftSection.setMinWidth(320);
        leftSection.setPrefWidth(320);
        rightSection.setMinWidth(320);
        rightSection.setPrefWidth(320);

        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        navbar.getChildren().addAll(leftSection, leftSpacer, centerSection, rightSpacer, rightSection);
        root.getChildren().add(navbar);
        if (notificationDropdown != null) {
            root.getChildren().add(notificationDropdown);
            StackPane.setAlignment(notificationDropdown, Pos.TOP_LEFT);
        }
        if (profileDropdown != null) {
            root.getChildren().add(profileDropdown);
            StackPane.setAlignment(profileDropdown, Pos.TOP_LEFT);
        }
    }

    private HBox buildLeftSection() {
        HBox left = new HBox();
        left.setAlignment(Pos.CENTER_LEFT);

        Button logo = new Button("SYNDICATI");
        logo.setMinWidth(95);
        logo.setPrefWidth(95);
        logo.setAlignment(Pos.CENTER);
        logo.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 12));
        logo.setPadding(new Insets(8, 10, 8, 10));
        styleGhostPill(logo);
        logo.setOnAction(e -> {
            activeTab = "home";
            updateTabsState();
            closeAllPopups();
            NavigationManager.getInstance().navigateTo("home");
        });

        left.getChildren().add(logo);
        return left;
    }

    private HBox buildCenterSection() {
        HBox center = new HBox();
        center.setAlignment(Pos.CENTER);

        tabsPill = new HBox();
        tabsPill.setAlignment(Pos.CENTER);
        tabsPill.setSpacing(4);
        tabsPill.setPadding(new Insets(8));

        Button home = createTabButton("home", "Home");
        home.setOnAction(e -> {
            activeTab = "home";
            updateTabsState();
            closeAllPopups();
            NavigationManager.getInstance().navigateTo("home");
        });

        Button services = createTabButton("services", "Services");
        Map<String, String> servicesItems = new LinkedHashMap<>();
        servicesItems.put("Residence", "services/residence");
        servicesItems.put("Forum", "services/forum");
        servicesItems.put("Syndicat", "services/syndicat");
        servicesItems.put("Evenement", "services/evenement");
        attachTabDropdown("services", services, servicesItems);

        Button about = createTabButton("about", "About");
        Map<String, String> aboutItems = new LinkedHashMap<>();
        aboutItems.put("Our Team", "about");
        aboutItems.put("Company", "about");
        aboutItems.put("Contact", "about");
        attachTabDropdown("about", about, aboutItems);

        tabsPill.getChildren().addAll(home, services, about);
        center.getChildren().add(tabsPill);
        updateTabsState();
        return center;
    }

    private Button createTabButton(String key, String text) {
        Button tab = new Button(text);
        tabButtons.put(key, tab);
        tab.setPadding(new Insets(12, 20, 12, 20));
        tab.setFont(Font.font(MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 13));
        tab.setOnMouseEntered(e -> {
            if (!key.equals(activeTab)) {
                tab.setStyle(
                    "-fx-background-color: " + (themeManager.isDarkMode() ? "rgba(255,255,255,0.08)" : "rgba(0,0,0,0.06)") + ";" +
                    "-fx-background-radius: 25px;" +
                    "-fx-text-fill: " + (themeManager.isDarkMode() ? "#ffffff" : "#111827") + ";" +
                    "-fx-cursor: hand;"
                );
            }
        });
        tab.setOnMouseExited(e -> updateTabsState());
        return tab;
    }

    private void attachTabDropdown(String key, Button anchorTab, Map<String, String> items) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        VBox content = new VBox();
        content.setPadding(new Insets(10));
        content.setSpacing(3);
        content.setPrefWidth(210);

        for (Map.Entry<String, String> entry : items.entrySet()) {
            Button row = new Button(entry.getKey());
            row.setMaxWidth(Double.MAX_VALUE);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 12, 10, 12));
            row.setFont(Font.font(MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 12));
            styleDropdownRow(row);
            String route = entry.getValue();
            row.setOnAction(e -> {
                closeAllPopups();
                activeTab = key;
                updateTabsState();
                NavigationManager.getInstance().navigateTo(route);
            });
            content.getChildren().add(row);
        }

        content.setOnMouseEntered(e -> cancelTabCloseDelay());
        content.setOnMouseExited(e -> scheduleCloseTabPopup(key));
        popup.getContent().add(content);

        anchorTab.setOnMouseEntered(e -> showTabPopup(key, anchorTab));
        anchorTab.setOnMouseExited(e -> scheduleCloseTabPopup(key));
        anchorTab.setOnAction(e -> {
            if (popup.isShowing()) {
                popup.hide();
            } else {
                showTabPopup(key, anchorTab);
            }
        });

        tabDropdownPopups.put(key, popup);
    }

    private void showTabPopup(String key, Button anchor) {
        cancelTabCloseDelay();
        closeNotificationPopup();
        closeProfilePopup();

        Popup popup = tabDropdownPopups.get(key);
        if (popup == null) {
            return;
        }

        for (Map.Entry<String, Popup> e : tabDropdownPopups.entrySet()) {
            if (!e.getKey().equals(key)) {
                e.getValue().hide();
            }
        }

        Node content = popup.getContent().get(0);
        content.applyCss();
        content.autosize();
        double w = content.prefWidth(-1);

        Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
        if (b == null) {
            return;
        }

        popup.show(anchor, b.getMinX() + (b.getWidth() - w) / 2.0, b.getMaxY() + 12);
    }

    private void scheduleCloseTabPopup(String key) {
        cancelTabCloseDelay();
        closeTabDelay = new PauseTransition(Duration.millis(160));
        closeTabDelay.setOnFinished(e -> {
            Popup popup = tabDropdownPopups.get(key);
            if (popup != null && popup.isShowing()) {
                Node content = popup.getContent().isEmpty() ? null : popup.getContent().get(0);
                boolean hoveringContent = content != null && content.isHover();
                boolean hoveringAnchor = tabButtons.containsKey(key) && tabButtons.get(key).isHover();
                if (!hoveringContent && !hoveringAnchor) {
                    popup.hide();
                }
            }
        });
        closeTabDelay.play();
    }

    private void cancelTabCloseDelay() {
        if (closeTabDelay != null) {
            closeTabDelay.stop();
        }
    }

    private HBox buildRightSection() {
        HBox right = new HBox();
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setSpacing(10);

        StackPane themeToggle = buildThemeToggle();
        StackPane bell = buildNotificationTrigger();
        StackPane profile = buildProfileTrigger();

        right.getChildren().addAll(themeToggle, bell, profile);
        return right;
    }

    private StackPane buildThemeToggle() {
        StackPane wrap = new StackPane();
        wrap.setAlignment(Pos.CENTER);
        wrap.setMinSize(62, 34);
        wrap.setPrefSize(62, 34);
        wrap.setMaxSize(62, 34);

        Rectangle track = new Rectangle(62, 34);
        track.setArcWidth(34);
        track.setArcHeight(34);

        Circle thumb = new Circle(13);
        Label icon = new Label(themeManager.isDarkMode() ? "\u263e" : "\u2600");
        icon.setFont(Font.font(12));

        updateThemeToggleVisual(track, thumb, icon);

        wrap.getChildren().addAll(track, thumb, icon);
        wrap.setOnMouseClicked(e -> {
            themeManager.toggleTheme();
            updateThemeToggleVisual(track, thumb, icon);
            applyThemeStyling();
            if (backgroundUpdateCallback != null) {
                backgroundUpdateCallback.run();
            }
        });

        return wrap;
    }

    private void updateThemeToggleVisual(Rectangle track, Circle thumb, Label icon) {
        boolean dark = themeManager.isDarkMode();
        track.setFill(Color.web(dark ? "rgba(255,255,255,0.08)" : "rgba(255,255,255,0.75)"));
        track.setStroke(Color.web(dark ? "rgba(255,255,255,0.20)" : "rgba(15,23,42,0.12)"));
        thumb.setFill(Color.web(dark ? "#111827" : "#ffffff"));
        thumb.setTranslateX(dark ? 13 : -13);
        icon.setText(dark ? "\u263e" : "\u2600");
        icon.setTextFill(Color.web(dark ? "#e5e7eb" : "#111827"));
        icon.setTranslateX(dark ? 13 : -13);
    }

    private StackPane buildNotificationTrigger() {
        StackPane wrap = new StackPane();
        wrap.setAlignment(Pos.CENTER);
        notificationAnchor = wrap;

        Button bellButton = new Button("\ud83d\udd14");
        bellButton.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 12));
        bellButton.setPadding(new Insets(8, 12, 8, 12));
        styleGhostPill(bellButton);

        Circle badge = new Circle(4.5, Color.web("#ff3b30"));
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(4, 5, 0, 0));

        notificationDropdown = buildNotificationDropdown();
        notificationDropdown.setVisible(false);
        notificationDropdown.setManaged(false);
        notificationDropdown.setMouseTransparent(true);
        notificationDropdown.toFront();

        notificationDropdown.setOnMouseEntered(e -> cancelNotificationCloseDelay());
        notificationDropdown.setOnMouseExited(e -> scheduleCloseNotificationPopup());

        wrap.setOnMouseEntered(e -> {
            if (profileDropdown != null && profileDropdown.isVisible()) {
                return;
            }
            cancelNotificationCloseDelay();
            showNotificationPopup();
        });
        wrap.setOnMouseExited(e -> scheduleCloseNotificationPopup());

        bellButton.setOnAction(e -> {
            if (notificationDropdown != null && notificationDropdown.isVisible()) {
                closeNotificationPopup();
                return;
            }
            showNotificationPopup();
        });

        wrap.getChildren().addAll(bellButton, badge);
        return wrap;
    }

    private VBox buildNotificationDropdown() {
        VBox box = new VBox();
        box.setPadding(new Insets(0));
        box.setSpacing(0);
        box.setPrefWidth(300);
        box.setMaxWidth(300);
        box.setMinWidth(300);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 20, 14, 20));
        Label title = new Label("Notifications");
        title.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 14));
        title.setTextFill(themeManager.isDarkMode() ? Color.web("#f3f4f6") : Color.web("#111827"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button close = new Button("\u2715");
        close.setPadding(new Insets(4, 10, 4, 10));
        styleGhostPill(close);
        close.setOnAction(e -> closeNotificationPopup());
        header.getChildren().addAll(title, spacer, close);

        VBox rows = new VBox();
        rows.setPadding(new Insets(8));
        rows.setSpacing(6);
        rows.getChildren().addAll(
            notifRow("\u2728", "Syndicati", "Your community dashboard is synced", "now"),
            notifRow("\ud83d\udcac", "Forum", "A new reply landed in your discussion", "5 min"),
            notifRow("\ud83c\udfe2", "Residence", "A maintenance update is ready", "1 h")
        );

        box.getChildren().addAll(header, rows);
        return box;
    }

    private VBox notifRow(String icon, String title, String body, String time) {
        VBox row = new VBox();
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setSpacing(2);
        Label t = new Label(icon + "  " + title);
        t.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 12));
        t.setTextFill(themeManager.isDarkMode() ? Color.web("#f3f4f6") : Color.web("#111827"));
        Label b = new Label(body);
        b.setFont(Font.font(MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 12));
        b.setTextFill(themeManager.isDarkMode() ? Color.web("#cbd5e1") : Color.web("#334155"));
        Label tm = new Label(time);
        tm.setFont(Font.font(MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 11));
        tm.setTextFill(themeManager.isDarkMode() ? Color.web("#94a3b8") : Color.web("#64748b"));
        row.getChildren().addAll(t, b, tm);
        row.setStyle("-fx-background-radius: 14px;");
        row.setOnMouseEntered(e -> row.setStyle(
            "-fx-background-color: " + (themeManager.isDarkMode() ? "rgba(255,255,255,0.06)" : "rgba(15,23,42,0.05)") + ";" +
            "-fx-background-radius: 14px;"
        ));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-background-radius: 14px;"));
        return row;
    }

    private StackPane buildProfileTrigger() {
        StackPane wrap = new StackPane();
        wrap.setAlignment(Pos.CENTER);
        wrap.setMinSize(42, 42);

        Circle avatar = new Circle(13);
        Label initial = new Label(currentInitial());
        initial.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 11));
        initial.setTextFill(Color.WHITE);

        boolean hasAvatar = applyAvatarFill(avatar);
        initial.setVisible(!hasAvatar);
        initial.setManaged(!hasAvatar);

        profileTriggerAvatarCircle = avatar;
        profileTriggerInitialLabel = initial;

        Circle online = new Circle(4, Color.web("#22c55e"));
        StackPane.setAlignment(online, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(online, new Insets(0, 2, 2, 0));

        wrap.getChildren().addAll(avatar, initial, online);
        wrap.setStyle("-fx-cursor: hand;");
        profileAnchor = wrap;

        profileDropdown = buildProfileDropdown();
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
        profileDropdown.setMouseTransparent(true);
        profileDropdown.toFront();

        profileDropdown.setOnMouseEntered(e -> cancelProfileCloseDelay());
        profileDropdown.setOnMouseExited(e -> scheduleCloseProfilePopup());

        wrap.setOnMouseEntered(e -> {
            cancelProfileCloseDelay();
            showProfilePopup();
        });
        wrap.setOnMouseExited(e -> scheduleCloseProfilePopup());

        wrap.setOnMouseClicked(e -> {
            if (profileDropdown != null && profileDropdown.isVisible()) {
                closeProfilePopup();
                e.consume();
                return;
            }
            showProfilePopup();
            e.consume();
        });

        return wrap;
    }

    private VBox buildProfileDropdown() {
        VBox box = new VBox();
        box.setPadding(new Insets(0));
        box.setSpacing(0);
        box.setPrefWidth(300);
        box.setMaxWidth(300);
        box.setMinWidth(300);
        box.setStyle(profileDropdownCardStyle());

        HBox head = new HBox();
        head.setAlignment(Pos.CENTER_LEFT);
        head.setSpacing(10);
        head.setPadding(new Insets(18, 20, 14, 20));
        Circle pic = new Circle(20);
        boolean hasAvatar = applyAvatarFill(pic);
        Label picInitial = new Label(currentInitial());
        picInitial.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 14));
        picInitial.setTextFill(Color.WHITE);
        picInitial.setVisible(!hasAvatar);
        picInitial.setManaged(!hasAvatar);
        StackPane picWrap = new StackPane(pic, picInitial);
        
        VBox info = new VBox();
        Label name = new Label(currentDisplayName());
        name.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 13));
        name.setTextFill(themeManager.isDarkMode() ? Color.web("#f3f4f6") : Color.web("#111827"));
        Label mail = new Label(currentEmail());
        mail.setFont(Font.font(MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 11));
        mail.setTextFill(themeManager.isDarkMode() ? Color.web("#94a3b8") : Color.web("#64748b"));
        info.getChildren().addAll(name, mail);
        head.getChildren().addAll(picWrap, info);

        Region sep1 = new Region();
        sep1.setPrefHeight(1);
        sep1.setStyle("-fx-background-color: " + (themeManager.isDarkMode() ? "rgba(255,255,255,0.12)" : "rgba(15,23,42,0.10)") + ";");

        Button profile = profileRow("\ud83d\udc64  Profile", "profile");
        Button settings = profileRow("\u2699  Settings", "settings");

        VBox rows = new VBox();
        rows.setPadding(new Insets(8));
        rows.setSpacing(6);
        rows.getChildren().add(profile);
        if (AccessControlService.canAccessAdminArea()) {
            rows.getChildren().add(profileRow("\ud83d\udcca  Dashboard", "dashboard"));
        }
        rows.getChildren().add(settings);

        Region sep2 = new Region();
        sep2.setPrefHeight(1);
        sep2.setStyle("-fx-background-color: " + (themeManager.isDarkMode() ? "rgba(255,255,255,0.12)" : "rgba(15,23,42,0.10)") + ";");

        Button logout = new Button("\u23fb  Sign Out");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setAlignment(Pos.CENTER_LEFT);
        logout.setPadding(new Insets(9, 10, 9, 10));
        logout.setFont(Font.font(MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 12));
        styleDropdownRow(logout);
        logout.setOnAction(e -> {
            closeProfilePopup();
            MainApplication.getInstance().logout();
        });

        VBox bottom = new VBox(8);
        bottom.setPadding(new Insets(8));
        bottom.getChildren().add(logout);

        box.getChildren().addAll(head, sep1, rows, sep2, bottom);
        return box;
    }

    private Button profileRow(String text, String route) {
        Button row = new Button(text);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(9, 10, 9, 10));
        row.setFont(Font.font(MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 12));
        styleDropdownRow(row);
        row.setOnAction(e -> {
            closeProfilePopup();
            NavigationManager.getInstance().navigateTo(route);
        });
        return row;
    }

    private void showNotificationPopup() {
        if (notificationDropdown == null || notificationAnchor == null) {
            return;
        }
        closeAllTabPopups();
        closeProfilePopup();
        positionNotificationDropdown();
        notificationDropdown.setVisible(true);
        notificationDropdown.setMouseTransparent(false);
        notificationDropdown.toFront();
    }

    private void positionProfileDropdown() {
        if (profileDropdown == null || profileAnchor == null || root.getScene() == null) {
            return;
        }

        profileDropdown.applyCss();
        profileDropdown.autosize();

        Bounds anchorScreen = profileAnchor.localToScreen(profileAnchor.getBoundsInLocal());
        Bounds rootScreen = root.localToScreen(root.getBoundsInLocal());
        if (anchorScreen == null || rootScreen == null) {
            return;
        }

        double popupW = profileDropdown.prefWidth(-1);
        double popupH = Math.max(profileDropdown.prefHeight(-1), 220);

        double xScreen = anchorScreen.getMaxX() - popupW;
        double yScreen = anchorScreen.getMaxY() + 12;

        double minX = rootScreen.getMinX() + 8;
        double maxX = rootScreen.getMaxX() - popupW - 8;
        double minY = rootScreen.getMinY() + 8;
        double maxY = rootScreen.getMaxY() - popupH - 8;

        if (maxX >= minX) {
            xScreen = Math.max(minX, Math.min(xScreen, maxX));
        }
        if (maxY >= minY) {
            yScreen = Math.max(minY, Math.min(yScreen, maxY));
        }

        Point2D local = root.screenToLocal(xScreen, yScreen);
        profileDropdown.relocate(local.getX(), local.getY());
    }

    private void positionNotificationDropdown() {
        if (notificationDropdown == null || notificationAnchor == null || root.getScene() == null) {
            return;
        }

        notificationDropdown.applyCss();
        notificationDropdown.autosize();

        Bounds anchorScreen = notificationAnchor.localToScreen(notificationAnchor.getBoundsInLocal());
        Bounds rootScreen = root.localToScreen(root.getBoundsInLocal());
        if (anchorScreen == null || rootScreen == null) {
            return;
        }

        double popupW = notificationDropdown.prefWidth(-1);
        double popupH = Math.max(notificationDropdown.prefHeight(-1), 220);

        double xScreen = anchorScreen.getMaxX() - popupW;
        double yScreen = anchorScreen.getMaxY() + 12;

        double minX = rootScreen.getMinX() + 8;
        double maxX = rootScreen.getMaxX() - popupW - 8;
        double minY = rootScreen.getMinY() + 8;
        double maxY = rootScreen.getMaxY() - popupH - 8;

        if (maxX >= minX) {
            xScreen = Math.max(minX, Math.min(xScreen, maxX));
        }
        if (maxY >= minY) {
            yScreen = Math.max(minY, Math.min(yScreen, maxY));
        }

        Point2D local = root.screenToLocal(xScreen, yScreen);
        notificationDropdown.relocate(local.getX(), local.getY());
    }

    private void scheduleCloseNotificationPopup() {
        cancelNotificationCloseDelay();
        closeNotificationDelay = new PauseTransition(Duration.millis(180));
        closeNotificationDelay.setOnFinished(e -> {
            if (notificationDropdown == null) {
                return;
            }
            boolean hoveringAnchor = notificationAnchor != null && notificationAnchor.isHover();
            boolean hoveringContent = notificationDropdown != null && notificationDropdown.isHover();
            if (!hoveringAnchor && !hoveringContent) {
                closeNotificationPopup();
            }
        });
        closeNotificationDelay.play();
    }

    private void cancelNotificationCloseDelay() {
        if (closeNotificationDelay != null) {
            closeNotificationDelay.stop();
        }
    }

    private void showProfilePopup() {
        if (profileDropdown == null || profileAnchor == null) {
            return;
        }

        refreshProfileTriggerAvatar();

        closeAllTabPopups();
        closeNotificationPopup();

        VBox previousProfileDropdown = profileDropdown;
        profileDropdown = buildProfileDropdown();
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
        profileDropdown.setMouseTransparent(true);
        profileDropdown.setOnMouseEntered(e -> cancelProfileCloseDelay());
        profileDropdown.setOnMouseExited(e -> scheduleCloseProfilePopup());

        if (previousProfileDropdown != null) {
            root.getChildren().remove(previousProfileDropdown);
        }
        if (!root.getChildren().contains(profileDropdown)) {
            root.getChildren().add(profileDropdown);
            StackPane.setAlignment(profileDropdown, Pos.TOP_LEFT);
        }

        positionProfileDropdown();
        profileDropdown.setManaged(false);
        profileDropdown.setVisible(true);
        profileDropdown.setMouseTransparent(false);
        profileDropdown.toFront();
    }

    private String profileDropdownCardStyle() {
        String dropdownBg = themeManager.isDarkMode() ? "rgba(0,0,0,0.88)" : "rgba(255,255,255,0.96)";
        String dropdownBorder = themeManager.isDarkMode() ? "rgba(255,255,255,0.10)" : "rgba(15,23,42,0.10)";
        return
            "-fx-background-color: " + dropdownBg + ";" +
            "-fx-background-radius: 30px;" +
            "-fx-border-color: " + dropdownBorder + ";" +
            "-fx-border-radius: 30px;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 28, 0.28, 0, 8);";
    }

    private void refreshProfileTriggerAvatar() {
        if (profileTriggerAvatarCircle == null || profileTriggerInitialLabel == null) {
            return;
        }
        boolean hasAvatar = applyAvatarFill(profileTriggerAvatarCircle);
        profileTriggerInitialLabel.setText(currentInitial());
        profileTriggerInitialLabel.setVisible(!hasAvatar);
        profileTriggerInitialLabel.setManaged(!hasAvatar);
    }

    private String currentDisplayName() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            return "Syndicati Member";
        }

        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (first + " " + last).trim();
        return fullName.isBlank() ? "User" : fullName;
    }

    private String currentEmail() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null || user.getEmailUser() == null || user.getEmailUser().isBlank()) {
            return "contact@syndicati.tn";
        }
        return user.getEmailUser();
    }

    private String currentInitial() {
        String name = currentDisplayName();
        if (!name.isBlank()) {
            return name.substring(0, 1).toUpperCase();
        }
        String email = currentEmail();
        return email.isBlank() ? "U" : email.substring(0, 1).toUpperCase();
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

        // Fallback to accent color
        avatarCircle.setFill(Color.web(themeManager.getAccentHex()));
        return false;
    }

    private void scheduleCloseProfilePopup() {
        cancelProfileCloseDelay();
        closeProfileDelay = new PauseTransition(Duration.millis(450));
        closeProfileDelay.setOnFinished(e -> {
            if (profileDropdown == null) {
                return;
            }
            boolean hoveringAnchor = profileAnchor != null && profileAnchor.isHover();
            boolean hoveringContent = profileDropdown.isHover();
            if (!hoveringAnchor && !hoveringContent) {
                closeProfilePopup();
            }
        });
        closeProfileDelay.play();
    }

    private void cancelProfileCloseDelay() {
        if (closeProfileDelay != null) {
            closeProfileDelay.stop();
        }
    }

    private void closeAllPopups() {
        closeAllTabPopups();
        closeNotificationPopup();
        closeProfilePopup();
    }

    private void closeAllTabPopups() {
        for (Popup popup : tabDropdownPopups.values()) {
            popup.hide();
        }
    }

    private void closeNotificationPopup() {
        cancelNotificationCloseDelay();
        if (notificationDropdown != null) {
            notificationDropdown.setVisible(false);
            notificationDropdown.setMouseTransparent(true);
        }
    }

    private void closeProfilePopup() {
        cancelProfileCloseDelay();
        if (profileDropdown != null) {
            profileDropdown.setVisible(false);
            profileDropdown.setMouseTransparent(true);
        }
    }

    private void updateTabsState() {
        for (Map.Entry<String, Button> e : tabButtons.entrySet()) {
            String key = e.getKey();
            Button tab = e.getValue();
            boolean selected = key.equals(activeTab);
            tab.setStyle(
                "-fx-background-color: " + (selected
                    ? (themeManager.isDarkMode() ? "rgba(255,255,255,0.12)" : "rgba(0,0,0,0.07)")
                    : "transparent") + ";" +
                "-fx-background-radius: 25px;" +
                "-fx-text-fill: " + (selected
                    ? (themeManager.isDarkMode() ? "#ffffff" : "#111827")
                    : (themeManager.isDarkMode() ? "#d1d5db" : "#374151")) + ";" +
                "-fx-cursor: hand;"
            );
            tab.setFont(Font.font(
                MainApplication.getInstance().getLightFontFamily(),
                selected ? FontWeight.SEMI_BOLD : FontWeight.NORMAL,
                13
            ));
        }
    }

    private void styleGhostPill(Button button) {
        button.setStyle(
            "-fx-background-color: " + (themeManager.isDarkMode() ? "rgba(255,255,255,0.06)" : "rgba(255,255,255,0.92)") + ";" +
            "-fx-background-radius: 22px;" +
            "-fx-border-color: " + (themeManager.isDarkMode() ? "rgba(255,255,255,0.18)" : "rgba(15,23,42,0.16)") + ";" +
            "-fx-border-radius: 22px;" +
            "-fx-border-width: 1;" +
            "-fx-text-fill: " + (themeManager.isDarkMode() ? "#f8fafc" : "#111827") + ";" +
            "-fx-cursor: hand;"
        );
        button.setMinHeight(42);
    }

    private void styleDropdownRow(Button row) {
        String text = themeManager.isDarkMode() ? "#e5e7eb" : "#1f2937";
        row.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-background-radius: 10px;" +
            "-fx-text-fill: " + text + ";" +
            "-fx-cursor: hand;"
        );
        row.setOnMouseEntered(e -> row.setStyle(
            "-fx-background-color: " + (themeManager.isDarkMode() ? "rgba(255,255,255,0.08)" : "rgba(15,23,42,0.10)") + ";" +
            "-fx-background-radius: 10px;" +
            "-fx-text-fill: " + text + ";" +
            "-fx-cursor: hand;"
        ));
        row.setOnMouseExited(e -> row.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-background-radius: 10px;" +
            "-fx-text-fill: " + text + ";" +
            "-fx-cursor: hand;"
        ));
    }

    private void applyThemeStyling() {
        navbar.setStyle(
            "-fx-background-color: " + (themeManager.isDarkMode() ? "rgba(0,0,0,0.55)" : "rgba(255,255,255,0.97)") + ";" +
            "-fx-background-radius: 50px;" +
            "-fx-border-color: " + (themeManager.isDarkMode() ? "rgba(255,255,255,0.10)" : "rgba(15,23,42,0.12)") + ";" +
            "-fx-border-radius: 50px;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, " + (themeManager.isDarkMode() ? "rgba(0,0,0,0.40)" : "rgba(15,23,42,0.12)") + ", 26, 0.28, 0, 6);"
        );

        tabsPill.setStyle(
            "-fx-background-color: " + (themeManager.isDarkMode() ? "rgba(255,255,255,0.05)" : "rgba(248,250,252,0.96)") + ";" +
            "-fx-background-radius: 30px;" +
            "-fx-border-color: " + (themeManager.isDarkMode() ? "rgba(255,255,255,0.14)" : "rgba(15,23,42,0.14)") + ";" +
            "-fx-border-radius: 30px;" +
            "-fx-border-width: 1;"
        );

        String dropdownBg = themeManager.isDarkMode() ? "rgba(0,0,0,0.88)" : "rgba(255,255,255,0.96)";
        String dropdownBorder = themeManager.isDarkMode() ? "rgba(255,255,255,0.10)" : "rgba(15,23,42,0.10)";

        for (Popup popup : tabDropdownPopups.values()) {
            if (!popup.getContent().isEmpty() && popup.getContent().get(0) instanceof VBox box) {
                box.setStyle(
                    "-fx-background-color: " + dropdownBg + ";" +
                    "-fx-background-radius: 20px;" +
                    "-fx-border-color: " + dropdownBorder + ";" +
                    "-fx-border-radius: 20px;" +
                    "-fx-border-width: 1;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 24, 0.25, 0, 8);"
                );
                for (Node n : box.getChildren()) {
                    if (n instanceof Button b) {
                        styleDropdownRow(b);
                    }
                }
            }
        }

        if (notificationDropdown != null) {
            notificationDropdown.setStyle(
                "-fx-background-color: " + dropdownBg + ";" +
                "-fx-background-radius: 30px;" +
                "-fx-border-color: " + dropdownBorder + ";" +
                "-fx-border-radius: 30px;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 28, 0.28, 0, 8);"
            );
            for (Node n : notificationDropdown.getChildren()) {
                if (n instanceof HBox h) {
                    for (Node child : h.getChildren()) {
                        if (child instanceof Label l) {
                            l.setTextFill(themeManager.isDarkMode() ? Color.web("#f3f4f6") : Color.web("#111827"));
                        }
                    }
                }
            }
        }

        if (profileDropdown != null) {
            profileDropdown.setStyle(
                "-fx-background-color: " + dropdownBg + ";" +
                "-fx-background-radius: 30px;" +
                "-fx-border-color: " + dropdownBorder + ";" +
                "-fx-border-radius: 30px;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 28, 0.28, 0, 8);"
            );
            for (Node n : profileDropdown.getChildren()) {
                if (n instanceof Button b) {
                    styleDropdownRow(b);
                }
            }
        }

        updateTabsState();
    }

    public void setBackgroundUpdateCallback(Runnable callback) {
        this.backgroundUpdateCallback = callback;
    }

    public void setMainContainer(javafx.scene.layout.Pane mainContainer) {
        // Compatibility method.
    }

    public void refreshTheme() {
        applyThemeStyling();
    }

    public javafx.scene.layout.Pane getRoot() {
        return root;
    }

    public void cleanup() {
        cancelTabCloseDelay();
        cancelProfileCloseDelay();
        cancelNotificationCloseDelay();
        closeAllPopups();
    }
}


