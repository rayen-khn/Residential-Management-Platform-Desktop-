package com.syndicati.views.backend.dashboard;

import com.syndicati.models.syndicat.Reclamation;
import com.syndicati.models.user.Profile;
import com.syndicati.models.user.User;
import javafx.animation.PauseTransition;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.scene.Scene;
import com.syndicati.interfaces.ViewInterface;
import com.syndicati.utils.session.SessionManager;
import com.syndicati.utils.theme.ThemeManager;
import com.syndicati.utils.image.ImageLoaderUtil;
import com.syndicati.services.dashboard.DashboardAdminService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.scene.text.TextAlignment;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;

/**
 * Admin Dashboard View - full replica of the Horizon admin panel.
 * Sidebar (220px) + scrollable main area.
 * Call setExitCallback() so "Back to App" can exit dashboard mode.
 */
@SuppressWarnings({"SpellCheckingInspection", "CssInvalidPropertyValue"})
public class DashboardView implements ViewInterface {

    private final HBox root;
    String activeSection = "general";
    private final Map<String, Button> sectionButtons = new HashMap<>();
    VBox contentArea;
    Runnable exitCallback;
    private final Runnable accentRefreshListener;
    boolean sidebarExpanded = true;
    Popup profilePopup;
    Popup notificationPopup;
    PauseTransition notificationHideDelay;
    private final DashboardAdminService dashboardAdminService;
    private static final DateTimeFormatter DASHBOARD_DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public DashboardView() {
        this.root = new HBox();
        this.accentRefreshListener = this::refreshAccentStyling;
        this.dashboardAdminService = new DashboardAdminService();
        setupLayout();
        ThemeManager.getInstance().addAccentChangeListener(accentRefreshListener);
    }

    DashboardAdminService dashboardAdminService() {
        return dashboardAdminService;
    }

    public void setExitCallback(Runnable cb) { this.exitCallback = cb; }

    @Override public HBox getRoot()  { return root; }
    @Override public void cleanup()  {
        exitCallback = null;
        ThemeManager.getInstance().removeAccentChangeListener(accentRefreshListener);
    }

    private void refreshAccentStyling() {
        String section = activeSection;
        sectionButtons.clear();

        root.getChildren().clear();
        VBox sidebar  = DashboardShell.buildSidebar(this);
        sidebar.setPrefWidth(sidebarWidth()); sidebar.setMinWidth(sidebarWidth()); sidebar.setMaxWidth(sidebarWidth());
        VBox mainArea = DashboardShell.buildMainArea(this);
        HBox.setHgrow(mainArea, Priority.ALWAYS);
        root.getChildren().addAll(sidebar, mainArea);

        activeSection = section;
        contentArea.getChildren().setAll(buildSection(section));
        sectionButtons.forEach((k, b) -> styleSidebarItem(b, k.equals(section)));
    }

    private ThemeManager theme() { return ThemeManager.getInstance(); }
    private String accentHex() { return theme().getAccentHex(); }
    String accentGradient() { return theme().getEffectiveAccentGradient(); }
    String accentRgba(double alpha) { return theme().toRgba(accentHex(), alpha); }
    boolean isDark() { return theme().isDarkMode(); }
    Color textPrimaryColor() { return isDark() ? Color.web("#f8fafc") : Color.web("#111827"); }
    Color textSecondaryColor() { return isDark() ? Color.web("rgba(255,255,255,0.78)") : Color.web("rgba(17,24,39,0.82)"); }
    Color textMutedColor() { return isDark() ? Color.web("rgba(255,255,255,0.55)") : Color.web("rgba(30,41,59,0.64)"); }

    String currentDisplayName() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            return "Admin User";
        }

        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? "User" : fullName;
    }

    String currentEmail() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null || user.getEmailUser() == null || user.getEmailUser().isBlank()) {
            return "contact@syndicati.tn";
        }
        return user.getEmailUser();
    }

    String currentRoleLabel() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null || user.getRoleUser() == null || user.getRoleUser().isBlank()) {
            return "Administrateur";
        }
        String role = user.getRoleUser().trim();
        if ("ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            return "Administrator";
        }
        return role;
    }

    String currentInitial() {
        String name = currentDisplayName();
        if (!name.isBlank()) {
            return name.substring(0, 1).toUpperCase();
        }
        String email = currentEmail();
        return email.isBlank() ? "U" : email.substring(0, 1).toUpperCase();
    }

    boolean applyAvatarFill(Circle avatarCircle) {
        Profile profile = SessionManager.getInstance().getCurrentProfile();
        String avatarPath = profile == null ? null : profile.getAvatar();

        if (avatarPath != null && !avatarPath.isBlank()) {
            Image img = ImageLoaderUtil.loadProfileAvatar(avatarPath, false);
            if (img != null && !img.isError()) {
                avatarCircle.setFill(new ImagePattern(img));
                return true;
            }
        }

        // Fallback to accent color
        avatarCircle.setFill(Color.web(accentHex()));
        return false;
    }


    private String sidebarPillStyle() {
        String glassFill = isDark()
            ? "linear-gradient(to bottom right, rgba(14,14,14,0.94), rgba(10,10,10,0.96) 54%, rgba(6,6,6,0.98) 100%)"
            : "linear-gradient(to bottom right, rgba(255,255,255,0.98), rgba(248,248,248,0.97) 54%, rgba(242,242,242,0.96) 100%)";
        return
            "-fx-background-color:" + glassFill + ";" +
            "-fx-border-color:" + (isDark() ? "rgba(255,255,255,0.14)" : "rgba(15,23,42,0.12)") + ";" +
            "-fx-border-width:1;" +
            "-fx-border-radius:24px;" +
            "-fx-background-radius:24px;" +
            "-fx-effect:dropshadow(gaussian," + (isDark() ? "rgba(0,0,0,0.28)" : "rgba(15,23,42,0.12)") + ",26,0,0,8);";
    }

    private void setupLayout() {
        root.setSpacing(0);
        root.setAlignment(Pos.TOP_LEFT);
        root.setFillHeight(true);
        root.setStyle(
            "-fx-background-color: " + (
                isDark()
                    ? "linear-gradient(to bottom right, #070707, #030303 55%, #000000 100%)"
                    : "linear-gradient(to bottom right, #f7f7f7, #f2f2f2 55%, #ececec 100%)"
            ) + ";"
        );
        root.setPadding(new Insets(40, 0, 0, 0));   // 40px = window-bar height

        VBox sidebar  = DashboardShell.buildSidebar(this);
        sidebar.setPrefWidth(sidebarWidth()); sidebar.setMinWidth(sidebarWidth()); sidebar.setMaxWidth(sidebarWidth());

        VBox mainArea = DashboardShell.buildMainArea(this);
        HBox.setHgrow(mainArea, Priority.ALWAYS);
        root.getChildren().addAll(sidebar, mainArea);
    }

    private double sidebarWidth() {
        return sidebarExpanded ? 250 : 96;
    }

    void toggleSidebar() {
        sidebarExpanded = !sidebarExpanded;
        refreshAccentStyling();
    }

    // SIDEBAR - three floating glass pills (matches web admin CSS)
    //   .admin-sidebar-top | .admin-sidebar-nav | .admin-sidebar-bottom
    //   each: backdrop-blur glass, border-radius 24px, transparent gap

    /** Shared glass pill container - three of these make up the sidebar. */
    VBox glassPill(Pos alignment) {
        VBox pill = new VBox(0);
        pill.setAlignment(alignment);
        pill.setFillWidth(true);
        pill.setStyle(sidebarPillStyle());
        return pill;
    }

    void styleBackButton(Button b, boolean h) {
        String background = h ? (isDark() ? accentRgba(0.22) : "rgba(15,23,42,0.12)") : "transparent";
        String textColor = h ? (isDark() ? "#e5e7eb" : "#111827") : (isDark() ? "rgba(255,255,255,0.5)" : "rgba(15,23,42,0.55)");
        b.setStyle(
            "-fx-background-color:" + background + ";" +
            "-fx-background-radius:12px;" +
            "-fx-text-fill:" + textColor + ";" +
            "-fx-cursor:hand;"
        );
    }

    void styleSignOutButton(Button b, boolean h) {
        String background = h ? "#800020" : "rgba(128,0,32,0.85)";
        String textColor = h ? "#ffffff" : "#ffe4ea";
        b.setStyle(
            "-fx-background-color:" + background + ";" +
            "-fx-background-radius:12px;" +
            "-fx-text-fill:" + textColor + ";" +
            "-fx-cursor:hand;"
        );
    }

    Region pillSep() {
        Region r = new Region(); r.setPrefHeight(1); r.setMaxHeight(1);
        r.setStyle("-fx-background-color:" + (isDark() ? "rgba(255,255,255,0.08)" : "rgba(15,23,42,0.10)") + ";");
        VBox.setMargin(r, new Insets(6, 0, 6, 0));
        return r;
    }

    Button sidebarItem(String icon, String label, String section) {
        HBox inner = new HBox(10);
        inner.setAlignment(sidebarExpanded ? Pos.CENTER_LEFT : Pos.CENTER);
        inner.setMouseTransparent(true);
        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(32, 32); iconBox.setMinSize(32, 32); iconBox.setMaxSize(32, 32);
        iconBox.setStyle("-fx-background-color:" + (isDark() ? "rgba(255,255,255,0.05)" : "rgba(15,23,42,0.06)") + ";-fx-background-radius:10px;");
        Text ic = new Text(icon); ic.setFont(Font.font(15));
        iconBox.getChildren().add(ic);

        inner.getChildren().add(iconBox);
        if (sidebarExpanded) {
            Text lbl = t(label, lightFont(), FontWeight.NORMAL, 15);
            lbl.setFill(isDark() ? Color.web("rgba(229,231,235,0.75)") : Color.web("rgba(15,23,42,0.72)"));
            inner.getChildren().add(lbl);
        }

        Button btn = new Button();
        btn.setGraphic(inner);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(sidebarExpanded ? Pos.CENTER_LEFT : Pos.CENTER);
        btn.setPadding(sidebarExpanded ? new Insets(6, 10, 6, 10) : new Insets(6, 6, 6, 6));
        btn.setPrefHeight(44);
        if (!sidebarExpanded) {
            btn.setPrefWidth(44);
            btn.setMinWidth(44);
            btn.setMaxWidth(44);
            Tooltip.install(btn, new Tooltip(label));
        }
        sectionButtons.put(section, btn);
        styleSidebarItem(btn, section.equals(activeSection));

        btn.setOnMouseEntered(_ -> {
            if (!section.equals(activeSection)) {
                btn.setStyle("-fx-background-color:" + (isDark() ? accentRgba(0.15) : "rgba(15,23,42,0.08)") + ";-fx-background-radius:12px;-fx-cursor:hand;-fx-border-color:" + (isDark() ? accentRgba(0.22) : "rgba(15,23,42,0.14)") + ";-fx-border-width:1;-fx-border-radius:12px;-fx-effect:dropshadow(gaussian," + (isDark() ? "rgba(0,0,0,0.3)" : "rgba(15,23,42,0.12)") + ",16,0,0,4);");
                iconBox.setStyle("-fx-background-color:" + (isDark() ? "rgba(255,255,255,0.1)" : "rgba(15,23,42,0.10)") + ";-fx-background-radius:10px;");
             }
        });
        btn.setOnMouseExited(_ -> {
            styleSidebarItem(btn, section.equals(activeSection));
            if (!section.equals(activeSection)) iconBox.setStyle("-fx-background-color:" + (isDark() ? "rgba(255,255,255,0.05)" : "rgba(15,23,42,0.06)") + ";-fx-background-radius:10px;");
        });
        btn.setOnAction(_ -> switchSection(section));
        return btn;
    }

    private void styleSidebarItem(Button b, boolean active) {
        if (active) {
            b.setStyle(
                "-fx-background-color:" + accentGradient() + ";" +
                "-fx-background-radius:12px;" +
                "-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian," + accentRgba(0.5) + ",20,0.3,0,6);"
            );
            // Re-style icon box inside active button to match web active item
            if (b.getGraphic() instanceof HBox inner) {
                if (!inner.getChildren().isEmpty() && inner.getChildren().getFirst() instanceof StackPane ib) {
                    ib.setStyle("-fx-background-color:rgba(255,255,255,0.15);-fx-background-radius:10px;");
                }
                if (inner.getChildren().size() > 1 && inner.getChildren().get(1) instanceof Text text) {
                    text.setFill(Color.WHITE);
                }
            }
        } else {
            b.setStyle("-fx-background-color:transparent;-fx-background-radius:12px;-fx-cursor:hand;");
            if (b.getGraphic() instanceof HBox inner) {
                if (!inner.getChildren().isEmpty() && inner.getChildren().getFirst() instanceof StackPane ib) {
                    ib.setStyle("-fx-background-color:" + (isDark() ? "rgba(255,255,255,0.05)" : "rgba(15,23,42,0.06)") + ";-fx-background-radius:10px;");
                }
                if (inner.getChildren().size() > 1 && inner.getChildren().get(1) instanceof Text text) {
                    text.setFill(isDark() ? Color.web("rgba(229,231,235,0.75)") : Color.web("rgba(15,23,42,0.72)"));
                }
            }
        }
    }

    private void switchSection(String section) {
        activeSection = section;
        sectionButtons.forEach((k, b) -> styleSidebarItem(b, k.equals(section)));
        contentArea.getChildren().setAll(buildSection(section));
    }

    // MAIN AREA
    VBox createMainArea() {
        VBox area = new VBox(0);
        area.setFillWidth(true);
        area.setStyle("-fx-background-color:" + (isDark() ? "#050505" : "#f3f4f6") + ";");

        contentArea = new VBox(0);
        contentArea.setFillWidth(true);
        contentArea.getChildren().add(buildSection(activeSection));

        ScrollPane scroll = new ScrollPane(contentArea);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        area.getChildren().addAll(DashboardShell.buildHeader(this), scroll);
        return area;
    }

    // SECTION DISPATCHER
    VBox buildSection(String section) {
        return switch (section) {
            case "users"     -> buildUsersSection();
            case "forum"     -> buildForumSection();
            case "syndicat"  -> buildSyndicatSection();
            case "residence" -> buildResidenceSection();
            case "evenement" -> buildEvenementSection();
            default          -> buildGeneralSection();
        };
    }

    // GENERAL section
    VBox buildGeneralSection() {
        VBox s = new VBox(20); s.setFillWidth(true); s.setPadding(new Insets(24));
        VBox subContent = new VBox(20); subContent.setFillWidth(true);
        HBox subBar = subTabBar(new String[]{"Overview", "Engagement", "System"}, key -> {
            subContent.getChildren().setAll(
                "Engagement".equals(key) ? buildEngagementContent() :
                "System".equals(key)     ? buildSystemContent()     :
                buildGeneralOverview()
            );
        });
        subContent.getChildren().add(buildGeneralOverview());
        s.getChildren().addAll(mainSwitcher(), subBar, subContent);
        return s;
    }

    private VBox buildGeneralOverview() {
        VBox v = new VBox(20); v.setFillWidth(true);
        HBox stats = new HBox(16); stats.setFillHeight(true);
        addStatCards(stats,
            new String[]{"USR","OK","ACT","PLS"},
            new String[]{"Total Residents","Active Today","Interactions Today","Community Pulse"},
            new String[]{"1,247","84","502","38"},
            new String[]{"#a78bfa","#34d399","#60a5fa","#fbbf24"}
        );
        HBox grid = new HBox(16); grid.setFillHeight(true);
        VBox chart = buildActivityChart(); HBox.setHgrow(chart, Priority.ALWAYS);
        VBox topU  = buildTopUsers(); topU.setPrefWidth(290); topU.setMinWidth(290); topU.setMaxWidth(290);
        grid.getChildren().addAll(chart, topU);
        v.getChildren().addAll(stats, grid);
        return v;
    }

    private VBox buildEngagementContent() {
        VBox rows = new VBox(10);
        String[][] pages = {
            {"1","#a78bfa","/frontend/home",      "284 views"},
            {"2","#60a5fa","/frontend/forum",     "211 views"},
            {"3","#34d399","/frontend/profile",   "183 views"},
            {"4","#fbbf24","/frontend/evenement", "124 views"},
            {"5","#f87171","/admin/dashboard",    "98 views"}
        };
        for (String[] p : pages) {
            rows.getChildren().add(buildRowCard(p[1], p[2], p[3], p[0], 10));
        }
        VBox card = sectionCard();
        card.getChildren().addAll(t("Top Pages - Most Visited Routes", boldFont(), FontWeight.BOLD, 18), rows);
        return new VBox(card);
    }

    private VBox buildSystemContent() {
        HBox stats = new HBox(16); stats.setFillHeight(true);
        addStatCards(stats,
            new String[]{"SRV","DB","RT","PRC"},
            new String[]{"Server Status","DB Size","Avg Response","Active Processes"},
            new String[]{"Online","248 MB","124 ms","7"},
            new String[]{"#34d399","#60a5fa","#a78bfa","#fbbf24"}
        );
        VBox logs = new VBox(8);
        for (String[] ev : new String[][]{
            {"#34d399","Mar 12 09:14","User admin logged in"},
            {"#fbbf24","Mar 12 08:52","Scheduled email batch: 58 sent"},
            {"#34d399","Mar 12 07:30","DB backup completed (248 MB)"},
            {"#34d399","Mar 11 22:00","Cache cleared successfully"},
            {"#fbbf24","Mar 11 20:18","New user registration: Karim S."}
        }) {
            logs.getChildren().add(buildRowCard(ev[0], ev[2], ev[1], null, 8));
        }
        VBox logCard = sectionCard();
        logCard.getChildren().addAll(t("Recent System Events", boldFont(), FontWeight.BOLD, 18), logs);
        return new VBox(20, stats, logCard);
    }

    private HBox buildRowCard(String color, String title, String value, String index, int radius) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(radius == 10 ? 10 : 7, 14, radius == 10 ? 10 : 7, 14));
        row.setStyle("-fx-background-color:rgba(255,255,255,0.02);-fx-background-radius:" + radius + "px;-fx-border-color:rgba(255,255,255,0.05);-fx-border-width:1;-fx-border-radius:" + radius + "px;");
        
        if (index != null) {
            StackPane rd = new StackPane(t(index, boldFont(), FontWeight.BOLD, 11));
            rd.setPrefSize(28, 28);
            rd.setStyle("-fx-background-color:" + color + "33;-fx-background-radius:14;");
            ((Text)rd.getChildren().getFirst()).setFill(Color.web(color));
            row.getChildren().add(rd);
        } else {
            Circle dot = new Circle(4, Color.web(color));
            row.getChildren().add(dot);
        }

        Text rt = t(title, boldFont(), FontWeight.NORMAL, index != null ? 13 : 14);
        rt.setFill(textSecondaryColor());
        Region spr = new Region(); HBox.setHgrow(spr, Priority.ALWAYS);
        Text cnt = t(value, boldFont(), index != null ? FontWeight.BOLD : FontWeight.NORMAL, 13);
        cnt.setFill(index != null ? Color.web(color) : textMutedColor());
        
        row.getChildren().addAll(rt, spr, cnt);
        return row;
    }

    private VBox buildActivityChart() {
        VBox card = sectionCard();
        Text title = t("Activity Pulse", boldFont(), FontWeight.BOLD, 15); title.setFill(textPrimaryColor());
        Text sub   = t("Page Views â–   UI Clicks â–   â€” Last 7 Days", lightFont(), FontWeight.NORMAL, 13);
        sub.setFill(textMutedColor());

        HBox cw = new HBox(8); cw.setAlignment(Pos.BOTTOM_LEFT);
        cw.setPrefHeight(140); cw.setPadding(new Insets(8,0,0,0));
        String[] days  = {"MON","TUE","WED","THU","FRI","SAT","SUN"};
        int[] views    = {65,82,58,90,74,45,60};
        int[] clicks   = {42,55,38,70,52,30,44};
        for (int i = 0; i < days.length; i++) {
            VBox col = new VBox(4); col.setAlignment(Pos.BOTTOM_CENTER);
            HBox.setHgrow(col, Priority.ALWAYS);
            HBox pair = new HBox(3); pair.setAlignment(Pos.BOTTOM_CENTER);
            pair.getChildren().addAll(barR(Math.max(8, views[i]*120/100), "#a78bfa"), barR(Math.max(8, clicks[i]*120/100), "#34d399"));
            Text d = t(days[i], lightFont(), FontWeight.NORMAL, 11); d.setFill(textMutedColor());
            col.getChildren().addAll(pair, d);
            cw.getChildren().add(col);
        }
        HBox footer = new HBox(20); footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10,0,0,0));
        footer.setStyle("-fx-border-color:rgba(255,255,255,0.06);-fx-border-width:1 0 0 0;");
        footer.getChildren().addAll(
            metric("P","New Posts","+12","#a78bfa"),
            metric("E","New Events","+5","#34d399"),
            metric("G","Engagement","High","#60a5fa")
        );
        card.getChildren().addAll(title, sub, cw, footer);
        return card;
    }

    private VBox buildTopUsers() {
        VBox card = sectionCard();
        Text title = t("â­  Top Active Citizens", boldFont(), FontWeight.BOLD, 14); title.setFill(textPrimaryColor());
        VBox list = new VBox(6);
        for (String[] u : new String[][]{
            {"Ahmed B.","SYNDIC","142"}, {"Leila M.","RESIDENT","118"},
            {"Karim S.","ADMIN","95"},   {"Sara A.","RESIDENT","87"},
            {"Omar Z.","SYNDIC","76"}
        }) {
            HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6,8,6,8));
            row.setStyle("-fx-background-color:rgba(255,255,255,0.025);-fx-background-radius:8;");
            Text ini = t(u[0].substring(0,1), boldFont(), FontWeight.BOLD, 14); ini.setFill(Color.web("#fbbf24"));
            StackPane av = new StackPane(ini); av.setPrefSize(32,32);
            av.setStyle("-fx-background-color:rgba(251,191,36,0.15);-fx-background-radius:16;");
            VBox info = new VBox(1); HBox.setHgrow(info, Priority.ALWAYS);
            Text nm = t(u[0], lightFont(), FontWeight.NORMAL, 14); nm.setFill(textSecondaryColor());
            Text rl = t(u[1], lightFont(), FontWeight.NORMAL, 12); rl.setFill(Color.web("#fbbf24"));
            info.getChildren().addAll(nm, rl);
            Text pts = t(u[2], boldFont(), FontWeight.BOLD, 13); pts.setFill(textPrimaryColor());
            row.getChildren().addAll(av, info, pts);
            list.getChildren().add(row);
        }
        card.getChildren().addAll(title, list);
        return card;
    }

    // USERS section (Twig: switcher Users/Profile/Onboarding)
    VBox buildUsersSection() { return DashboardUsersSection.build(this); }

    // FORUM section (Twig: Publications/Commentaires/Reactions)
    VBox buildForumSection() { return DashboardForumSection.build(this); }

    // SYNDICAT section (Twig: Reclamations/Responses)
    VBox buildSyndicatSection() { return DashboardSyndicatSection.build(this); }

    // RESIDENCE section (Twig: Residences/Appartements/Maintenance)
    VBox buildResidenceSection() { return DashboardResidenceSection.build(this); }

    // EVENEMENT section (Twig: Evenements/Participations)
    VBox buildEvenementSection() { return DashboardEvenementSection.build(this); }

    String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DASHBOARD_DATE_TIME_FMT);
    }

    // SHARED HELPERS
    VBox moduleShell(String name, String icon) {
        VBox s = new VBox(20); s.setFillWidth(true); s.setPadding(new Insets(24));
        HBox heading = new HBox(10); heading.setAlignment(Pos.CENTER_LEFT);
        Text ic = new Text(icon); ic.setFont(Font.font(20));
        Text nm = t(name+" Overview", boldFont(), FontWeight.BOLD, 21); nm.setFill(textPrimaryColor());
        heading.getChildren().addAll(ic, nm);
        s.getChildren().add(heading);
        return s;
    }

    void addStatCards(HBox row, String[] icons, String[] labels, String[] values, String[] colors) {
        for (int i = 0; i < labels.length; i++) {
            VBox card = statCard(icons[i], labels[i], values[i], colors[i]);
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
    }

    private VBox statCard(String icon, String label, String value, String color) {
        VBox card = new VBox(10); card.setPadding(new Insets(20)); card.setAlignment(Pos.TOP_LEFT);
        String normal = "-fx-background-color:rgba(255,255,255,0.03);-fx-background-radius:16px;-fx-border-color:rgba(255,255,255,0.07);-fx-border-width:1;-fx-border-radius:16px;";
        String hover  = "-fx-background-color:rgba(255,255,255,0.055);-fx-background-radius:16px;-fx-border-color:" + color + "55;-fx-border-width:1;-fx-border-radius:16px;";
        card.setStyle(normal);
        card.setOnMouseEntered(_ -> card.setStyle(hover));
        card.setOnMouseExited(_  -> card.setStyle(normal));
        StackPane ic = new StackPane(); ic.setPrefSize(40,40);
        ic.setStyle("-fx-background-color:" + color + "25;-fx-background-radius:12px;");
        ic.getChildren().add(new Text(icon));
        Text lbl = t(label, lightFont(), FontWeight.NORMAL, 14); lbl.setFill(textMutedColor());
        Text val = t(value, boldFont(),  FontWeight.BOLD,   28); val.setFill(textPrimaryColor());
        Region gl = new Region(); gl.setPrefHeight(2);
        gl.setStyle("-fx-background-color:linear-gradient(to right,"+color+",transparent);-fx-background-radius:2;");
        card.getChildren().addAll(ic, lbl, val, gl);
        return card;
    }

    private VBox glassCard() {
        VBox c = new VBox(14); c.setPadding(new Insets(20)); c.setFillWidth(true);
        c.setStyle("-fx-background-color:rgba(255,255,255,0.03);-fx-background-radius:16px;-fx-border-color:rgba(255,255,255,0.07);-fx-border-width:1;-fx-border-radius:16px;");
        return c;
    }

    VBox dataTableWithCrud(String title, String entityLabel, String[] cols, String[][] rows, boolean allowAdd) {
        return dataTableWithCrud(title, entityLabel, cols, rows, allowAdd, null);
    }

    VBox dataTableWithCrud(String title, String entityLabel, String[] cols, String[][] rows, boolean allowAdd, Node headerControls) {
        CrudSpec spec = crudSpec(title, entityLabel);
        
        StackPane faceContainer = new StackPane();
        faceContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
        faceContainer.setMaxHeight(Double.MAX_VALUE);
        faceContainer.setStyle("-fx-background-color:transparent;");
        faceContainer.setMaxWidth(Double.MAX_VALUE);
        
        VBox tableCard = new VBox(12);
        tableCard.setPadding(new Insets(12));
        tableCard.setStyle("-fx-background-color:transparent;");
        tableCard.setMaxWidth(Double.MAX_VALUE);
        
        VBox modalFace = new VBox(12);
        modalFace.setPadding(new Insets(12));
        modalFace.setStyle("-fx-background-color:transparent;");
        modalFace.setMaxWidth(Double.MAX_VALUE);
        modalFace.setVisible(false);
        
        VBox card = glassCard();
        card.setPadding(new Insets(16, 16, 14, 16));

        final int pageSize = 10;
        final String[][] sourceRows = rows == null ? new String[0][] : rows;

        final class PagerState {
            int page = 1;
        }
        final PagerState pagerState = new PagerState();

        final Runnable[] refreshTable = new Runnable[1];

        HBox head = new HBox(10);
        head.setAlignment(Pos.CENTER_LEFT);
        Text tt = t(title, boldFont(), FontWeight.BOLD, 18); tt.setFill(textPrimaryColor());
        Text sub = t("Live module data", lightFont(), FontWeight.NORMAL, 13);
        sub.setFill(textMutedColor());
        VBox titleWrap = new VBox(2, tt, sub);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        head.getChildren().addAll(titleWrap, spacer);

        if (headerControls != null) {
            if (headerControls instanceof Region) {
                ((Region) headerControls).setMinWidth(Region.USE_PREF_SIZE);
            }
            head.getChildren().add(headerControls);
        }

        if (allowAdd) {
            Button addBtn = pillAction(spec.addButtonLabel, true);
            addBtn.setOnAction(ignored -> switchToModalFace(faceContainer, spec, entityLabel, "add", cols, null));
            head.getChildren().add(addBtn);
        }

        GridPane tbl = new GridPane();
        tbl.setHgap(0);
        tbl.setVgap(0);
        tbl.setMaxWidth(Double.MAX_VALUE);
        VBox tableWrap = new VBox(tbl);
        tableWrap.setFillWidth(true);

        int displayCols = cols.length + 1;
        for (int c = 0; c < displayCols; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            tbl.getColumnConstraints().add(cc);
        }
        HBox pagerInfo = new HBox(8);
        pagerInfo.setAlignment(Pos.CENTER_LEFT);
        Text pageLabel = t("", lightFont(), FontWeight.NORMAL, 12);
        pageLabel.setFill(textMutedColor());
        Button prevBtn = pillAction("<", false);
        Button nextBtn = pillAction(">", false);
        HBox pageNumberBox = new HBox(6);
        pageNumberBox.setAlignment(Pos.CENTER);
        HBox pager = new HBox(8, prevBtn, pageNumberBox, nextBtn, pageLabel);
        pager.setAlignment(Pos.CENTER);
        pager.setPadding(new Insets(8, 0, 0, 0));

        for (int c = 0; c < cols.length; c++) {
            if (cols[c].startsWith("[H]")) continue;
            Text col = t(cols[c].toUpperCase(), boldFont(), FontWeight.BOLD, 12);
            col.setFill(textMutedColor());
            HBox cell = new HBox(col);
            cell.setPadding(new Insets(9,12,9,12));
            cell.setStyle("-fx-border-color:rgba(255,255,255,0.06);-fx-border-width:0 0 1 0;");
            tbl.add(cell, c, 0);
        }
        Text actionCol = t("ACTIONS", boldFont(), FontWeight.BOLD, 12);
        actionCol.setFill(textMutedColor());
        HBox actionHeader = new HBox(actionCol);
        actionHeader.setPadding(new Insets(9,12,9,12));
        actionHeader.setStyle("-fx-border-color:rgba(255,255,255,0.06);-fx-border-width:0 0 1 0;");
        tbl.add(actionHeader, cols.length, 0);

        refreshTable[0] = () -> {
            tbl.getChildren().removeIf(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) > 0);

            List<String[]> workingRows = new ArrayList<>();
            for (String[] row : sourceRows) {
                if (row == null) {
                    continue;
                }
                workingRows.add(row);
            }

            int totalRows = workingRows.size();
            int totalPages = Math.max(1, (int) Math.ceil(totalRows / (double) pageSize));

            if (totalRows == 0) {
                HBox emptyCell = new HBox(t("No records found", lightFont(), FontWeight.NORMAL, 14));
                emptyCell.setPadding(new Insets(12, 12, 12, 12));
                emptyCell.setStyle("-fx-border-color:rgba(255,255,255,0.06);-fx-border-width:0 0 1 0;");
                tbl.add(emptyCell, 0, 1, cols.length + 1, 1);
                pageLabel.setText("Page 1 / 1 | 0 records");
                prevBtn.setDisable(true);
                nextBtn.setDisable(true);
                prevBtn.setOpacity(0.55);
                nextBtn.setOpacity(0.55);
                return;
            }

            int safePage = Math.clamp(pagerState.page, 1, totalPages);
            pagerState.page = safePage;
            int fromIndex = (safePage - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, totalRows);

            for (int r = fromIndex; r < toIndex; r++) {
                String bg = (((r - fromIndex) & 1) == 0) ? "transparent" : "rgba(255,255,255,0.01)";
                String[] rowData = workingRows.get(r);
                // Only show the primary columns in the main table to avoid clutter
                for (int c = 0; c < cols.length; c++) {
                    if (cols[c].startsWith("[H]")) continue;
                    String displayValue = rowData[c];
                    // If it's an image column, show a clean icon in the table instead of the long filename
                    if (isImageFieldLabel(cols[c]) && displayValue != null && !displayValue.equals("-")) {
                        displayValue = "🖼️ View Image";
                    }
                    
                    Text tx = t(displayValue, lightFont(), FontWeight.NORMAL, 14);
                    tx.setFill(c == 0 ? textSecondaryColor() : textMutedColor());
                    HBox cb = new HBox(tx);
                    cb.setPadding(new Insets(10,12,10,12));
                    cb.setStyle("-fx-background-color:" + bg + ";");
                    cb.setOnMouseEntered(_ -> cb.setStyle("-fx-background-color:" + accentRgba(0.07) + ";"));
                    cb.setOnMouseExited(_ -> cb.setStyle("-fx-background-color:" + bg + ";"));
                    tbl.add(cb, c, (r - fromIndex) + 1);
                }

                HBox rowActions = new HBox(6);
                rowActions.setAlignment(Pos.CENTER_LEFT);
                rowActions.setPadding(new Insets(8, 12, 8, 12));
                rowActions.setStyle("-fx-background-color:" + bg + ";");

                Button viewBtn = pillAction("View", false);
                viewBtn.setOnAction(ignored -> switchToModalFace(faceContainer, spec, entityLabel, "view", cols, rowData));
                rowActions.getChildren().add(viewBtn);

                tbl.add(rowActions, cols.length, (r - fromIndex) + 1);
            }

            pageLabel.setText("Page " + safePage + " / " + totalPages + " | " + totalRows + " records");
            prevBtn.setDisable(safePage <= 1);
            nextBtn.setDisable(safePage >= totalPages);
            prevBtn.setOpacity(prevBtn.isDisable() ? 0.55 : 1.0);
            nextBtn.setOpacity(nextBtn.isDisable() ? 0.55 : 1.0);

            pageNumberBox.getChildren().clear();
            int maxButtons = 3;
            int start = Math.max(1, safePage - 1);
            int end = Math.min(totalPages, start + maxButtons - 1);
            start = Math.max(1, end - maxButtons + 1);

            for (int pageNum = start; pageNum <= end; pageNum++) {
                final int targetPage = pageNum;
                Button pageBtn = pagerBtn(String.valueOf(pageNum), pageNum == safePage);
                pageBtn.setDisable(pageNum == safePage);
                pageBtn.setOpacity(pageNum == safePage ? 1.0 : 0.95);
                pageBtn.setOnAction(ignored -> {
                    pagerState.page = targetPage;
                    refreshTable[0].run();
                });
                pageNumberBox.getChildren().add(pageBtn);
            }
        };

        prevBtn.setOnAction(ignored -> {
            pagerState.page--;
            refreshTable[0].run();
        });
        nextBtn.setOnAction(ignored -> {
            pagerState.page++;
            refreshTable[0].run();
        });

        refreshTable[0].run();

        card.getChildren().addAll(head, tableWrap, pager);
        tableCard.getChildren().add(card);
        
        StackPane.setAlignment(tableCard, Pos.TOP_CENTER);
        StackPane.setAlignment(modalFace, Pos.TOP_CENTER);
        faceContainer.getChildren().addAll(tableCard, modalFace);
        faceContainer.setUserData(modalFace);
        
        VBox wrap = new VBox(faceContainer);
        wrap.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(faceContainer, Priority.ALWAYS);
        return wrap;
    }

    private CrudSpec crudSpec(String title, String entityLabel) {
        CrudSpec s = CrudSpec.defaults(entityLabel);

        switch (title) {
            case "Users Table":
                s.viewTitle = "User Details";
                s.viewSubtitle = "View complete user information";
                s.editTitle = "Edit User";
                s.editSubtitle = "Current data is pre-filled. Edit and save.";
                s.addTitle = "Add User";
                s.addSubtitle = "Create a new user. Same fields as sign up, plus role and verified.";
                s.cancelLabel = "Discard";
                s.saveEditLabel = "Update Account";
                s.saveAddLabel = "Create Account";
                s.addButtonLabel = "Add User";
                break;
            case "User Profile Data":
                s.viewTitle = "Profile Details";
                s.viewSubtitle = "Comprehensive profile information with user relationship";
                s.editTitle = "Edit Profile";
                s.editSubtitle = "Modify profile fields (locale, theme, timezone, avatar, bio).";
                s.addTitle = "Add Profile";
                s.addSubtitle = "Create a new profile for a user.";
                s.cancelLabel = "Cancel";
                s.saveEditLabel = "Save Changes";
                s.saveAddLabel = "Create Profile";
                s.addButtonLabel = "Add Profile";
                break;
            case "Onboarding Data":
                s.viewTitle = "Onboarding Details";
                s.viewSubtitle = "Comprehensive onboarding information";
                s.editTitle = "Modify onboarding choices";
                s.editSubtitle = "Update language, theme, preferences and suggestions.";
                s.cancelLabel = "Close Wizard";
                s.saveEditLabel = "Save changes";
                s.viewDeleteLabel = null;
                break;
            case "Forum Publications":
                s.viewTitle = "Publication Details";
                s.viewSubtitle = "View complete publication information";
                s.editTitle = "Edit Publication";
                s.editSubtitle = "Modify the publication and save changes.";
                s.addTitle = "Add Publication";
                s.addSubtitle = "Create a new forum publication.";
                s.saveEditLabel = "Update Publication";
                s.saveAddLabel = "Post Publication";
                s.addButtonLabel = "Add Publication";
                break;
            case "Forum Comments":
                s.viewTitle = "Comment Details";
                s.viewSubtitle = "View comment content and metadata.";
                s.editTitle = "Edit Comment";
                s.editSubtitle = "Modify the comment text and save changes.";
                s.saveEditLabel = "Update Comment";
                break;
            case "Forum Reactions":
                s.viewTitle = "Reaction Details";
                s.viewSubtitle = "View reaction content and metadata.";
                s.editTitle = "Edit Reaction";
                s.editSubtitle = "Modify reaction kind and details.";
                s.saveEditLabel = "Update Reaction";
                break;
            case "Reclamations":
                s.viewTitle = "Reclamation Details";
                s.viewSubtitle = "View complete reclamation information";
                s.editTitle = "Edit Reclamation";
                s.editSubtitle = "Modify reclamation status and details.";
                s.saveEditLabel = "Update Status";
                break;
            case "Responses":
                s.viewTitle = "Response Details";
                s.viewSubtitle = "View response content and metadata.";
                break;
            case "Residences":
                s.viewTitle = "Residence Details";
                s.viewSubtitle = "Full information overview";
                s.editTitle = "Edit Residence";
                s.editSubtitle = "Update residence properties";
                s.addTitle = "Add Residence";
                s.addSubtitle = "Create a new entry";
                s.saveEditLabel = "Update Residence";
                s.saveAddLabel = "Create Residence";
                s.addButtonLabel = "Add Residence";
                break;
            case "Appartements":
                s.viewTitle = "Appartement Details";
                s.viewSubtitle = "Information overview";
                s.editTitle = "Edit Appartement";
                s.editSubtitle = "Update details";
                s.addTitle = "Add Appartement";
                s.addSubtitle = "Add to inventory";
                s.saveEditLabel = "Update Appartement";
                s.saveAddLabel = "Create Appartement";
                s.addButtonLabel = "Add Appartement";
                break;
            case "Maintenance":
                s.viewTitle = "Maintenance Details";
                s.viewSubtitle = "Full analysis & conditions";
                s.cancelLabel = "Back to List";
                s.viewDeleteLabel = null;
                break;
            case "Evenements":
                s.viewTitle = "Event Details";
                s.viewSubtitle = "Full information overview";
                s.editTitle = "Edit Event";
                s.editSubtitle = "Update event properties";
                s.addTitle = "Add Event";
                s.addSubtitle = "Create a new entry";
                s.saveEditLabel = "Update Event";
                s.saveAddLabel = "Post Event";
                s.addButtonLabel = "Add Event";
                break;
            case "Participations":
                s.viewTitle = "Participation Details";
                s.viewSubtitle = "Information overview";
                s.editTitle = "Edit Participation";
                s.editSubtitle = "Update record";
                s.saveEditLabel = "Update Participation";
                break;
            default:
                break;
        }

        return s;
    }

    Button pillAction(String text, boolean primary) {
        Button b = new Button(text);
        b.setFont(Font.font(lightFont(), FontWeight.NORMAL, 10));
        b.setPadding(new Insets(4, 10, 4, 10));
        String background = primary ? accentRgba(0.24) : "transparent";
        String border = primary ? accentRgba(0.34) : (isDark() ? "rgba(255,255,255,0.16)" : "rgba(15,23,42,0.20)");
        String textColor = primary ? "white" : (isDark() ? "rgba(255,255,255,0.80)" : "rgba(15,23,42,0.86)");
        b.setStyle(
            "-fx-background-color:" + background + ";" +
            "-fx-border-color:" + border + ";" +
            "-fx-text-fill:" + textColor + ";" +
            "-fx-border-width:1;" +
            "-fx-background-radius:100px;" +
            "-fx-border-radius:100px;" +
            "-fx-cursor:hand;"
        );
        return b;
    }

    private void switchToModalFace(StackPane container, CrudSpec spec, String entityLabel, String mode, String[] cols, String[] rowData) {
        VBox modalFace = (VBox) container.getUserData();
        String title = "view".equals(mode) ? spec.viewTitle : ("edit".equals(mode) ? spec.editTitle : spec.addTitle);
        String subtitle = "view".equals(mode) ? spec.viewSubtitle : ("edit".equals(mode) ? spec.editSubtitle : spec.addSubtitle);

        modalFace.getChildren().clear();
        
        VBox modalCard = glassCard();
        modalCard.setPadding(new Insets(16, 16, 14, 16));

        HBox head = new HBox(10);
        head.setAlignment(Pos.CENTER_LEFT);
        VBox tWrap = new VBox(3);
        Text hTitle = t(title, boldFont(), FontWeight.BOLD, 19); hTitle.setFill(textPrimaryColor());
        Text hSub = t(subtitle, lightFont(), FontWeight.NORMAL, 13); hSub.setFill(textMutedColor());
        tWrap.getChildren().addAll(hTitle, hSub);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button close = new Button("x");
        close.setPadding(new Insets(6, 10, 6, 10));
        close.setStyle("-fx-background-color:" + (isDark() ? "rgba(255,255,255,0.08)" : "rgba(15,23,42,0.08)") + ";-fx-background-radius:999;-fx-text-fill:" + (isDark() ? "rgba(255,255,255,0.85)" : "rgba(15,23,42,0.85)") + ";-fx-cursor:hand;");
        close.setOnAction(_ -> switchToTableFace(container));
        head.getChildren().addAll(tWrap, spacer, close);

        VBox fields = buildModalFields(entityLabel, mode, cols, rowData);

        ScrollPane formScroll = new ScrollPane(fields);
        formScroll.setFitToWidth(true);
        formScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        formScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        formScroll.setPrefViewportHeight(300);
        formScroll.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        VBox.setVgrow(formScroll, Priority.ALWAYS);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button cancel = pillAction(spec.cancelLabel, false);
        cancel.setOnAction(_ -> switchToTableFace(container));
        actions.getChildren().add(cancel);
        
        if ("view".equals(mode)) {
            if (spec.viewDeleteLabel != null && !"Reponse".equalsIgnoreCase(entityLabel)) {
                Button del = dangerAction(spec.viewDeleteLabel);
                del.setOnAction(ignored -> {
                    showConfirmationAlert(
                        "Confirm Deletion",
                        "Are you sure you want to delete this " + entityLabel.toLowerCase() + "? This action cannot be undone.",
                        () -> {
                            if (dashboardAdminService.deleteEntity(entityLabel, rowData)) {
                                showPremiumAlert("Deleted", "The record has been removed successfully.", true);
                                switchSection(activeSection);
                            } else {
                                showPremiumAlert("Error", "Could not delete this record. It might be linked to other data.", false);
                                switchToTableFace(container);
                            }
                        }
                    );
                });
                actions.getChildren().add(del);
            }
            if (!"Reponse".equalsIgnoreCase(entityLabel)) {
                Button edit = pillAction("Edit", true);
                edit.setOnAction(ignored -> switchToModalFace(container, spec, entityLabel, "edit", cols, rowData));
                actions.getChildren().add(edit);
            }
        } else {
            String saveLabel = "add".equals(mode) ? spec.saveAddLabel : spec.saveEditLabel;
            Button save = pillAction(saveLabel, true);
            save.setOnAction(ignored -> {
                boolean success = dashboardAdminService.saveEntity(entityLabel, mode, rowData, fields);
                if (success) {
                    showPremiumAlert("Success", entityLabel + " saved successfully!", true);
                    refreshAccentStyling();
                    switchSection(activeSection);
                } else {
                    showPremiumAlert("Error", "Unable to save " + entityLabel.toLowerCase() + ". Please check your inputs.", false);
                    switchToTableFace(container);
                }
            });
            actions.getChildren().add(save);
        }

        modalCard.getChildren().addAll(head, formScroll, actions);
        modalFace.getChildren().add(modalCard);

        ObservableList<Node> children = container.getChildren();
        VBox tableCard = (VBox) children.getFirst();
        tableCard.setVisible(false);
        modalFace.setVisible(true);
    }

    public void showPremiumAlert(String title, String message, boolean success) {
        Stage alertStage = new Stage();
        alertStage.initStyle(StageStyle.TRANSPARENT);
        alertStage.initModality(Modality.APPLICATION_MODAL);
        
        Window owner = root.getScene() != null ? root.getScene().getWindow() : null;
        if (owner != null) {
            alertStage.initOwner(owner);
        }

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));
        content.setMinWidth(420);
        content.setMaxWidth(420);
        
        String colorTheme = success ? "#2ecc71" : "#ff3b30";
        content.setStyle(
            "-fx-background-color: rgba(20, 20, 25, 0.96);" +
            "-fx-background-radius: 32px;" +
            "-fx-border-color: " + colorTheme + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 32px;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 40, 0, 0, 20);"
        );

        Text icon = new Text(success ? "\u2713" : "\u26A0");
        icon.setFont(Font.font(boldFont(), FontWeight.BOLD, 54));
        icon.setFill(Color.web(colorTheme));
        
        StackPane iconCircle = new StackPane(icon);
        iconCircle.setPrefSize(90, 90);
        iconCircle.setMaxSize(90, 90);
        iconCircle.setStyle(
            "-fx-background-color: " + accentRgba(0.1) + ";" +
            "-fx-background-radius: 999px;" +
            "-fx-border-color: " + accentRgba(0.2) + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 999px;"
        );

        Text tTitle = new Text(title);
        tTitle.setFont(Font.font(boldFont(), FontWeight.BOLD, 22));
        tTitle.setFill(Color.WHITE);

        Text tMsg = new Text(message);
        tMsg.setFont(Font.font(lightFont(), 14));
        tMsg.setFill(Color.web("#94a3b8"));
        tMsg.setWrappingWidth(340);
        tMsg.setTextAlignment(TextAlignment.CENTER);

        Button ok = new Button("Continue");
        ok.setCursor(javafx.scene.Cursor.HAND);
        ok.setMaxWidth(Double.MAX_VALUE);
        ok.setPrefHeight(44);
        String btnGradient = success 
            ? "linear-gradient(to right, #2ecc71, #27ae60)" 
            : "linear-gradient(to right, #ff3b30, #ff7b30)";
            
        ok.setStyle(
            "-fx-background-color: " + btnGradient + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: 800;" +
            "-fx-background-radius: 14px;" +
            "-fx-padding: 0 30 0 30;"
        );
        ok.setOnAction(e -> alertStage.close());

        content.getChildren().addAll(iconCircle, tTitle, tMsg, ok);

        content.setOpacity(0);
        content.setScaleX(0.9);
        content.setScaleY(0.9);
        
        FadeTransition ft = new FadeTransition(Duration.millis(300), content);
        ft.setToValue(1.0);
        
        ScaleTransition st = new ScaleTransition(Duration.millis(350), content);
        st.setToX(1.0);
        st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);

        StackPane sceneRoot = new StackPane(content);
        sceneRoot.setStyle("-fx-background-color: transparent;");
        sceneRoot.setPadding(new Insets(50));
        
        Scene alertScene = new Scene(sceneRoot);
        alertScene.setFill(Color.TRANSPARENT);
        alertStage.setScene(alertScene);
        
        alertStage.show();
        ft.play();
        st.play();
    }

    private void showConfirmationAlert(String title, String message, Runnable onConfirm) {
        Stage alertStage = new Stage();
        alertStage.initStyle(StageStyle.TRANSPARENT);
        alertStage.initModality(Modality.APPLICATION_MODAL);
        
        Window owner = root.getScene() != null ? root.getScene().getWindow() : null;
        if (owner != null) {
            alertStage.initOwner(owner);
        }

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));
        content.setMinWidth(420);
        content.setMaxWidth(420);
        
        String colorTheme = "#f59e0b"; // Warning yellow
        content.setStyle(
            "-fx-background-color: rgba(20, 20, 25, 0.98);" +
            "-fx-background-radius: 32px;" +
            "-fx-border-color: " + colorTheme + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 32px;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 40, 0, 0, 20);"
        );

        Text icon = new Text("?"); 
        icon.setFont(Font.font(boldFont(), FontWeight.BOLD, 54));
        icon.setFill(Color.web(colorTheme));
        
        StackPane iconCircle = new StackPane(icon);
        iconCircle.setPrefSize(90, 90);
        iconCircle.setMaxSize(90, 90);
        iconCircle.setStyle(
            "-fx-background-color: rgba(245, 158, 11, 0.1);" +
            "-fx-background-radius: 999px;" +
            "-fx-border-color: rgba(245, 158, 11, 0.2);" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 999px;"
        );

        Text tTitle = new Text(title);
        tTitle.setFont(Font.font(boldFont(), FontWeight.BOLD, 22));
        tTitle.setFill(Color.WHITE);

        Text tMsg = new Text(message);
        tMsg.setFont(Font.font(lightFont(), 14));
        tMsg.setFill(Color.web("#94a3b8"));
        tMsg.setWrappingWidth(340);
        tMsg.setTextAlignment(TextAlignment.CENTER);

        HBox btns = new HBox(16);
        btns.setAlignment(Pos.CENTER);
        btns.setPadding(new Insets(10, 0, 0, 0));
        
        Button cancel = new Button("Cancel");
        cancel.setCursor(javafx.scene.Cursor.HAND);
        cancel.setMinWidth(130);
        cancel.setPrefHeight(44);
        cancel.setStyle(
            "-fx-background-color: rgba(255,255,255,0.05);" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: 700;" +
            "-fx-background-radius: 14px;" +
            "-fx-border-color: rgba(255,255,255,0.1);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 14px;"
        );
        cancel.setOnAction(e -> alertStage.close());

        Button confirm = new Button("Confirm Delete");
        confirm.setCursor(javafx.scene.Cursor.HAND);
        confirm.setMinWidth(130);
        confirm.setPrefHeight(44);
        confirm.setStyle(
            "-fx-background-color: linear-gradient(to right, #ff3b30, #ff7b30);" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: 800;" +
            "-fx-background-radius: 14px;"
        );
        confirm.setOnAction(e -> {
            alertStage.close();
            onConfirm.run();
        });

        btns.getChildren().addAll(cancel, confirm);
        content.getChildren().addAll(iconCircle, tTitle, tMsg, btns);

        content.setOpacity(0);
        content.setScaleX(0.9);
        content.setScaleY(0.9);
        
        FadeTransition ft = new FadeTransition(Duration.millis(300), content);
        ft.setToValue(1.0);
        ScaleTransition st = new ScaleTransition(Duration.millis(350), content);
        st.setToX(1.0); st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);

        StackPane sceneRoot = new StackPane(content);
        sceneRoot.setStyle("-fx-background-color: transparent;");
        sceneRoot.setPadding(new Insets(50));
        
        Scene alertScene = new Scene(sceneRoot);
        alertScene.setFill(Color.TRANSPARENT);
        alertStage.setScene(alertScene);
        
        alertStage.show();
        ft.play();
        st.play();
    }

    private void switchToTableFace(StackPane container) {
        ObservableList<Node> children = container.getChildren();
        VBox tableCard = (VBox) children.getFirst();
        VBox modalFace = (VBox) children.get(1);
        
        tableCard.setVisible(true);
        modalFace.setVisible(false);
    }

    private VBox buildModalFields(String entityLabel, String mode, String[] cols, String[] rowData) {
        VBox fields = new VBox(10);

        fields.getChildren().add(sectionTitle(
            "view".equals(mode) ? "Overview" :
            "edit".equals(mode) ? "Editable Fields" :
            "Create New Record"
        ));
        if ("view".equals(mode)) {
            // Meta strip (ID, Ref, Audit) removed for a cleaner UI
            // fields.getChildren().add(metaStrip(rowData));
        }

        // Show all available data in the detail view, even if hidden from the table
        int dataCount = (rowData != null) ? rowData.length : cols.length;
        for (int i = 0; i < dataCount; i++) {
            String label = i < cols.length ? cols[i] : "Additional Data " + (i - cols.length + 1);
            if (label.startsWith("[H]")) label = label.substring(3);
            
            String val = (rowData != null && i < rowData.length) ? rowData[i] : "";
            boolean editable = ("edit".equals(mode) || "add".equals(mode)) && i < cols.length;
            fields.getChildren().add(fieldRow(entityLabel, mode, label, val, editable));
        }

        if ("edit".equals(mode) || "add".equals(mode)) {
            fields.getChildren().add(sectionTitle("Flags & Metadata"));
            fields.getChildren().add(infoChipRow());
            fields.getChildren().add(notesBox());
        }

        return fields;
    }

    private Text sectionTitle(String text) {
        Text t = t(text, boldFont(), FontWeight.BOLD, 16);
        t.setFill(textSecondaryColor());
        return t;
    }

    private HBox metaStrip(String[] rowData) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 0, 8, 0));
        String a = rowData != null && rowData.length > 0 ? rowData[0] : "Item";
        String b = rowData != null && rowData.length > 1 ? rowData[1] : "Primary";
        row.getChildren().addAll(
            chip("ID * " + Math.abs(a.hashCode() % 10000), false),
            chip("Ref * " + b, true),
            chip("Audit * Enabled", false)
        );
        return row;
    }

    private HBox infoChipRow() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        String[] labels = {"Active", "Verified", "Synced", "Tracked"};
        for (int i = 0; i < labels.length; i++) {
            row.getChildren().add(chip(labels[i], (i & 1) != 0));
        }
        return row;
    }

    private Region chip(String label, boolean accent) {
        Text tx = t(label, lightFont(), FontWeight.NORMAL, 12);
        tx.setFill(accent ? Color.WHITE : textSecondaryColor());
        HBox box = new HBox(tx);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(5, 10, 5, 10));
        String common = ";-fx-border-width:1;-fx-background-radius:999;-fx-border-radius:999;";
        String chipStyle = accent
            ? "-fx-background-color:" + accentRgba(0.26) + ";-fx-border-color:" + accentRgba(0.36) + common
            : "-fx-background-color:rgba(255,255,255,0.05);-fx-border-color:rgba(255,255,255,0.12)" + common;
        box.setStyle(chipStyle);
        return box;
    }

    private VBox notesBox() {
        VBox wrap = new VBox(4);
        Text lbl = t("Internal notes", lightFont(), FontWeight.NORMAL, 13);
        lbl.setFill(textMutedColor());
        Text val = t("Add context for admins (reason, follow-up, priority).", lightFont(), FontWeight.NORMAL, 13);
        val.setFill(textSecondaryColor());
        VBox box = new VBox(val);
        box.setPadding(new Insets(10, 12, 10, 12));
        box.setStyle(
            "-fx-background-color:rgba(255,255,255,0.03);" +
            "-fx-border-color:rgba(255,255,255,0.08);" +
            "-fx-border-width:1;" +
            "-fx-background-radius:10px;" +
            "-fx-border-radius:10px;"
        );
        wrap.getChildren().addAll(lbl, box);
        return wrap;
    }

    private Node fieldRow(String entityLabel, String mode, String label, String value, boolean editable) {
        VBox row = new VBox(6);
        row.setPadding(new Insets(4, 0, 4, 0));
        Text lbl = t(label, lightFont(), FontWeight.NORMAL, 11);
        lbl.setFill(textMutedColor());
        Label liveHint = modalLiveHintLabel();

        boolean canEditThisField = editable;
        if ("Reclamation".equalsIgnoreCase(entityLabel) && "edit".equals(mode) && !"Status".equalsIgnoreCase(label)) {
            canEditThisField = false;
        }
        if ("Reponse".equalsIgnoreCase(entityLabel) && "edit".equals(mode) && !"Message".equalsIgnoreCase(label) && !"Title".equalsIgnoreCase(label) && !"Image".equalsIgnoreCase(label)) {
            canEditThisField = false;
        }
        if ("Reponse".equalsIgnoreCase(entityLabel) && "add".equals(mode) && "Date".equalsIgnoreCase(label)) {
            canEditThisField = false;
        }

        if (canEditThisField) {
            if ("User".equalsIgnoreCase(entityLabel) && "Role".equalsIgnoreCase(label)) {
                ComboBox<String> roleSelect = new ComboBox<>();
                roleSelect.getItems().addAll(userRoleOptions(value));
                String selectedRole = normalizeRoleValue(value);
                if (selectedRole.equals("-") && "add".equals(mode)) {
                    selectedRole = "ROLE_RESIDENT";
                }
                if (!selectedRole.equals("-") && !roleSelect.getItems().contains(selectedRole)) {
                    roleSelect.getItems().add(selectedRole);
                }
                if (!selectedRole.equals("-")) {
                    roleSelect.setValue(selectedRole);
                }
                styleSelect(roleSelect, "Select Role", this::formatRoleDisplay);
                installLiveValidation(roleSelect, liveHint, entityLabel, label);
                row.getChildren().addAll(lbl, roleSelect, liveHint);
                return row;
            }

            if ("User".equalsIgnoreCase(entityLabel) && "Verified".equalsIgnoreCase(label)) {
                ComboBox<String> verifiedSelect = new ComboBox<>();
                verifiedSelect.getItems().addAll("Yes", "No");
                verifiedSelect.setValue(isTruthyText(value) ? "Yes" : "No");
                styleSelect(verifiedSelect, "Select Value", Function.identity());
                installLiveValidation(verifiedSelect, liveHint, entityLabel, label);
                row.getChildren().addAll(lbl, verifiedSelect, liveHint);
                return row;
            }

            if ("Reclamation".equalsIgnoreCase(entityLabel) && "Status".equalsIgnoreCase(label)) {
                ComboBox<String> statusSelect = new ComboBox<>();
                statusSelect.getItems().addAll(reclamationStatusOptions(value));
                String selectedStatus = normalizeReclamationStatusValue(value);
                if (!selectedStatus.equals("-") && !statusSelect.getItems().contains(selectedStatus)) {
                    statusSelect.getItems().add(selectedStatus);
                }
                if (!selectedStatus.equals("-")) {
                    statusSelect.setValue(selectedStatus);
                }
                styleSelect(statusSelect, "Select Status", this::formatReclamationStatusDisplay);
                installLiveValidation(statusSelect, liveHint, entityLabel, label);
                row.getChildren().addAll(lbl, statusSelect, liveHint);
                return row;
            }

            if ("Reponse".equalsIgnoreCase(entityLabel) && "User".equalsIgnoreCase(label)) {
                ComboBox<String> userSelect = new ComboBox<>();
                userSelect.getItems().addAll(reponseUserOptions(value));
                String selectedUser = normalizeUserDisplayName(value);
                if (!selectedUser.equals("-") && !userSelect.getItems().contains(selectedUser)) {
                    userSelect.getItems().add(selectedUser);
                }
                if (!selectedUser.equals("-")) {
                    userSelect.setValue(selectedUser);
                }
                styleSelect(userSelect, "Select User", Function.identity());
                
                // --- Dynamic Filtering Logic ---
                userSelect.valueProperty().addListener((obs, oldUser, newUser) -> {
                    // Find the Reclamation ComboBox in the same parent container
                    VBox parent = (VBox) row.getParent();
                    if (parent != null) {
                        for (Node node : parent.getChildren()) {
                            if (node instanceof VBox fieldBox) {
                                if (fieldBox.getChildren().size() >= 2 && fieldBox.getChildren().get(0) instanceof Text t) {
                                    if ("Reclamation".equalsIgnoreCase(t.getText()) && fieldBox.getChildren().get(1) instanceof ComboBox<?> cb) {
                                        @SuppressWarnings("unchecked")
                                        ComboBox<String> reclamationSelect = (ComboBox<String>) cb;
                                        boolean hasUser = newUser != null && !"-".equals(newUser);
                                        reclamationSelect.setDisable(!hasUser);
                                        reclamationSelect.getItems().setAll(hasUser ? reponseReclamationOptionsFiltered(newUser) : new ArrayList<>());
                                        reclamationSelect.setValue(null);
                                        
                                        if (hasUser) {
                                            if (!reclamationSelect.getItems().isEmpty()) {
                                                reclamationSelect.setPromptText("Select a reclamation from " + newUser);
                                            } else {
                                                reclamationSelect.setPromptText("No reclamations found for " + newUser);
                                            }
                                        } else {
                                            reclamationSelect.setPromptText("Select User first");
                                        }
                                    }
                                }
                            }
                        }
                    }
                });

                installLiveValidation(userSelect, liveHint, entityLabel, label);
                row.getChildren().addAll(lbl, userSelect, liveHint);
                return row;
            }

            if ("Reponse".equalsIgnoreCase(entityLabel) && "Reclamation".equalsIgnoreCase(label)) {
                ComboBox<String> reclamationSelect = new ComboBox<>();
                String userVal = "-";
                // Try to find if a user is already selected in the fields
                VBox parent = (VBox) row.getParent();
                if (parent != null) {
                    for (Node node : parent.getChildren()) {
                        if (node instanceof VBox fieldBox) {
                            if (fieldBox.getChildren().size() >= 2 && fieldBox.getChildren().get(0) instanceof Text t && "User".equalsIgnoreCase(t.getText())) {
                                if (fieldBox.getChildren().get(1) instanceof ComboBox<?> cb) {
                                    Object val = cb.getValue();
                                    if (val != null) userVal = val.toString();
                                }
                            }
                        }
                    }
                }

                boolean hasUser = !"-".equals(userVal);
                reclamationSelect.getItems().addAll(hasUser ? reponseReclamationOptionsFiltered(userVal) : new ArrayList<>());
                String current = normalizeReclamationTitle(value);
                if (!current.equals("-") && !reclamationSelect.getItems().contains(current)) {
                    reclamationSelect.getItems().add(current);
                }
                if (!current.equals("-")) {
                    reclamationSelect.setValue(current);
                }
                
                reclamationSelect.setDisable(!hasUser && current.equals("-"));
                styleSelect(reclamationSelect, hasUser ? "Select Reclamation" : "Select User first", Function.identity());
                installLiveValidation(reclamationSelect, liveHint, entityLabel, label);
                row.getChildren().addAll(lbl, reclamationSelect, liveHint);
                return row;
            }

            if ("Reponse".equalsIgnoreCase(entityLabel) && "Date".equalsIgnoreCase(label)) {
                canEditThisField = false;
            }

            if ("Publication".equalsIgnoreCase(entityLabel) && "Category".equalsIgnoreCase(label)) {
                ComboBox<String> categorySelect = new ComboBox<>();
                java.util.List<String> categories = java.util.List.of(
                    "Announcement", "Suggestion", "Jeux Video", "Informatique", "NouveautÃ©", "Discussion General", "Culture", "Sport"
                );
                categorySelect.getItems().addAll(categories);
                if (value != null && !value.isEmpty() && !value.equals("-")) {
                    categorySelect.setValue(value);
                } else {
                    categorySelect.setValue("Discussion General");
                }
                styleSelect(categorySelect, "Select Category", Function.identity());
                installLiveValidation(categorySelect, liveHint, entityLabel, label);
                row.getChildren().addAll(lbl, categorySelect, liveHint);
                return row;
            }

            if ("Event".equalsIgnoreCase(entityLabel) && "Type".equalsIgnoreCase(label)) {
                ComboBox<String> typeSelect = new ComboBox<>();
                typeSelect.getItems().addAll(eventTypeOptions(value));
                String selectedType = normalizeEventTypeValue(value);
                if (!selectedType.equals("-") && !typeSelect.getItems().contains(selectedType)) {
                    typeSelect.getItems().add(selectedType);
                }
                if (!selectedType.equals("-")) {
                    typeSelect.setValue(selectedType);
                } else if (!typeSelect.getItems().isEmpty()) {
                    typeSelect.setValue(typeSelect.getItems().getFirst());
                }
                styleSelect(typeSelect, "Select Type", this::formatEventTypeDisplay);
                installLiveValidation(typeSelect, liveHint, entityLabel, label);
                row.getChildren().addAll(lbl, typeSelect, liveHint);
                return row;
            }

            if ("Event".equalsIgnoreCase(entityLabel) && "Status".equalsIgnoreCase(label)) {
                ComboBox<String> eventStatusSelect = new ComboBox<>();
                eventStatusSelect.getItems().addAll(eventStatusOptions(value));
                String selectedStatus = normalizeEventStatusValue(value);
                if (!selectedStatus.equals("-") && !eventStatusSelect.getItems().contains(selectedStatus)) {
                    eventStatusSelect.getItems().add(selectedStatus);
                }
                if (!selectedStatus.equals("-")) {
                    eventStatusSelect.setValue(selectedStatus);
                }
                styleSelect(eventStatusSelect, "Select Status", this::formatEventStatusDisplay);
                installLiveValidation(eventStatusSelect, liveHint, entityLabel, label);
                row.getChildren().addAll(lbl, eventStatusSelect, liveHint);
                return row;
            }

            if (isDateFieldLabel(label)) {
                DatePicker datePicker = new DatePicker(parseDateForPicker(value));
                styleDatePicker(datePicker);
                installLiveValidation(datePicker, liveHint, entityLabel, label);
                row.getChildren().addAll(lbl, datePicker, liveHint);
                return row;
            }

            if (isImageFieldLabel(label)) {
                VBox imageGroup = new VBox(8);
                HBox previewContainer = new HBox(12);
                previewContainer.setAlignment(Pos.CENTER_LEFT);

                // --- Current Image Preview ---
                if ("edit".equals(mode) && value != null && !value.equals("-") && !value.isEmpty()) {
                    VBox currentBox = new VBox(4);
                    currentBox.setAlignment(Pos.CENTER);
                    Text currentLbl = t("Current", lightFont(), FontWeight.BOLD, 10);
                    currentLbl.setFill(textMutedColor());
                    
                    String fullPath = value.trim();
                    if (!fullPath.contains(":\\") && !fullPath.contains(":/") && !fullPath.startsWith("uploads/")) {
                        fullPath = "uploads/" + fullPath;
                    }
                    Image currentImg = ImageLoaderUtil.loadImage(fullPath);
                    if (currentImg != null) {
                        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(currentImg);
                        iv.setFitWidth(80);
                        iv.setFitHeight(80);
                        iv.setPreserveRatio(true);
                        StackPane frame = new StackPane(iv);
                        frame.setStyle("-fx-border-color:rgba(255,255,255,0.1);-fx-border-width:1;-fx-border-radius:8px;-fx-background-color:rgba(255,255,255,0.03);-fx-background-radius:8px;");
                        currentBox.getChildren().addAll(currentLbl, frame);
                        previewContainer.getChildren().add(currentBox);
                    }
                }

                // --- New Image Live Preview ---
                VBox nextBox = new VBox(4);
                nextBox.setAlignment(Pos.CENTER);
                Text nextLbl = t("New Selection", lightFont(), FontWeight.BOLD, 10);
                nextLbl.setFill(Color.web(accentHex()));
                StackPane nextFrame = new StackPane();
                nextFrame.setPrefSize(80, 80);
                nextFrame.setStyle("-fx-border-color:" + accentRgba(0.3) + ";-fx-border-width:1;-fx-border-radius:8px;-fx-background-color:rgba(255,255,255,0.03);-fx-background-radius:8px;");
                nextBox.getChildren().addAll(nextLbl, nextFrame);
                nextBox.setVisible(false); // Hidden until a selection is made
                nextBox.setManaged(false);
                previewContainer.getChildren().add(nextBox);

                TextField imageInput = new TextField(value);
                imageInput.setFont(Font.font(lightFont(), FontWeight.NORMAL, 12));
                imageInput.setPrefHeight(36);
                imageInput.setStyle(inputStyle(false, false));
                
                // Live preview listener
                imageInput.textProperty().addListener((obs, oldV, newV) -> {
                    if (newV != null && !newV.isEmpty() && !newV.equals("-") && !newV.equals(value)) {
                        String path = newV.trim();
                        if (!path.contains(":\\") && !path.contains(":/") && !path.startsWith("uploads/")) {
                            path = "uploads/" + path;
                        }
                        Image newImg = ImageLoaderUtil.loadImage(path);
                        if (newImg != null) {
                            javafx.scene.image.ImageView niv = new javafx.scene.image.ImageView(newImg);
                            niv.setFitWidth(80);
                            niv.setFitHeight(80);
                            niv.setPreserveRatio(true);
                            nextFrame.getChildren().setAll(niv);
                            nextBox.setVisible(true);
                            nextBox.setManaged(true);
                        }
                    }
                });

                Button browse = new Button("Browse");
                browse.setFont(Font.font(lightFont(), FontWeight.SEMI_BOLD, 11));
                browse.setPrefHeight(36);
                browse.setStyle("-fx-background-color:" + accentGradient() + ";-fx-text-fill:white;-fx-background-radius:10px;-fx-cursor:hand;");
                
                browse.setOnAction(_ -> {
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle("Select Image");
                    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
                    Window owner = root.getScene() != null ? root.getScene().getWindow() : null;
                    java.io.File file = chooser.showOpenDialog(owner);
                    if (file != null) {
                        try {
                            String fileName = System.currentTimeMillis() + "_" + file.getName();
                            String uploadsPath = System.getProperty("user.dir") + java.io.File.separator + "uploads" + java.io.File.separator + "reponse_images";
                            java.io.File uploadsDir = new java.io.File(uploadsPath);
                            if (!uploadsDir.exists()) uploadsDir.mkdirs();
                            java.io.File destFile = new java.io.File(uploadsDir, fileName);
                            java.nio.file.Files.copy(file.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            imageInput.setText("reponse_images/" + fileName);
                        } catch (java.io.IOException ex) {
                            imageInput.setText(file.getAbsolutePath());
                        }
                    }
                });

                HBox inputRow = new HBox(8, imageInput, browse);
                HBox.setHgrow(imageInput, Priority.ALWAYS);
                
                imageGroup.getChildren().addAll(previewContainer, inputRow);
                installLiveValidation(imageInput, liveHint, entityLabel, label);
                row.getChildren().addAll(lbl, imageGroup, liveHint);
                return row;
            }

            TextField input = new TextField(value);
            input.setFont(Font.font(lightFont(), FontWeight.NORMAL, 12));
            input.setPrefHeight(36);
            input.setMinHeight(36);
            input.setStyle(inputStyle(false, false));
            input.setOnMouseEntered(_ -> input.setStyle(inputStyle(true, input.isFocused())));
            input.setOnMouseExited(_ -> input.setStyle(inputStyle(false, input.isFocused())));
            input.focusedProperty().addListener((ignoredObservable, ignoredOldValue, newVal) -> input.setStyle(inputStyle(false, newVal)));
            installLiveValidation(input, liveHint, entityLabel, label);
            row.getChildren().addAll(lbl, input, liveHint);
        } else {
            if (isImageFieldLabel(label) && value != null && !value.equals("-")) {
                VBox imageDisplayBox = new VBox(8);
                imageDisplayBox.setPadding(new Insets(10, 12, 10, 12));
                imageDisplayBox.setStyle("-fx-background-color:rgba(255,255,255,0.03);-fx-border-color:rgba(255,255,255,0.08);-fx-border-width:1;-fx-background-radius:10px;-fx-border-radius:10px;");
                
                String fullPath = value.trim();
                // Fix: Don't prefix absolute paths (e.g. E:\) with 'uploads/'
                if (!fullPath.startsWith("uploads/") && !fullPath.startsWith("uploads\\") && !fullPath.contains(":\\") && !fullPath.contains(":/")) {
                    fullPath = "uploads/" + fullPath;
                }
                
                Image img = ImageLoaderUtil.loadImage(fullPath);
                if (img != null) {
                    javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                    iv.setFitWidth(300);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(true);
                    
                    // Add a nice border/shadow to the image
                    StackPane imgContainer = new StackPane(iv);
                    imgContainer.setStyle("-fx-border-color:rgba(255,255,255,0.1);-fx-border-width:1;-fx-border-radius:8px;-fx-background-radius:8px;");
                    
                    Text pathText = t(value, lightFont(), FontWeight.NORMAL, 10);
                    pathText.setFill(textMutedColor());
                    
                    imageDisplayBox.getChildren().addAll(imgContainer, pathText);
                } else {
                    Text errorText = t("⚠️ Image not found: " + value, lightFont(), FontWeight.NORMAL, 13);
                    errorText.setFill(Color.web("#ff3b30"));
                    imageDisplayBox.getChildren().add(errorText);
                }
                row.getChildren().addAll(lbl, imageDisplayBox);
                return row;
            }

            VBox contentBox = new VBox(4);
            if (value != null && value.contains("\n")) {
                String[] lines = value.split("\n");
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    Text lineText = t("• " + line.trim(), lightFont(), FontWeight.NORMAL, 14);
                    lineText.setFill(textSecondaryColor());
                    contentBox.getChildren().add(lineText);
                }
            } else {
                Text val = t(value, lightFont(), FontWeight.NORMAL, 14);
                val.setFill(textSecondaryColor());
                contentBox.getChildren().add(val);
            }

            VBox box = new VBox(contentBox);
            box.setPadding(new Insets(8, 10, 8, 10));
            box.setStyle(
                "-fx-background-color:rgba(255,255,255,0.03);" +
                "-fx-border-color:rgba(255,255,255,0.08);" +
                "-fx-border-width:1;" +
                "-fx-background-radius:10px;" +
                "-fx-border-radius:10px;"
            );
            row.getChildren().addAll(lbl, box);
        }
        return row;
    }

    private Label modalLiveHintLabel() {
        Label hint = new Label("Start typing...");
        hint.setTextFill(textMutedColor());
        hint.setFont(Font.font(lightFont(), FontWeight.SEMI_BOLD, 10));
        return hint;
    }

    private void installLiveValidation(TextField input, Label hint, String entityLabel, String fieldLabel) {
        Runnable refresh = () -> updateLiveHint(hint, dashboardFieldValidationError(entityLabel, fieldLabel, input.getText()));
        input.textProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        refresh.run();
    }

    private void installLiveValidation(ComboBox<String> input, Label hint, String entityLabel, String fieldLabel) {
        Runnable refresh = () -> updateLiveHint(hint, dashboardFieldValidationError(entityLabel, fieldLabel, input.getValue()));
        input.valueProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        refresh.run();
    }

    private void installLiveValidation(DatePicker input, Label hint, String entityLabel, String fieldLabel) {
        Runnable refresh = () -> {
            String value = input.getValue() != null ? input.getValue().toString() : input.getEditor().getText();
            updateLiveHint(hint, dashboardFieldValidationError(entityLabel, fieldLabel, value));
        };
        input.valueProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        input.getEditor().textProperty().addListener((obs, oldValue, newValue) -> refresh.run());
        refresh.run();
    }

    private void updateLiveHint(Label hint, String error) {
        if (error == null || error.isBlank()) {
            hint.setText("âœ“ Looks good");
            hint.setTextFill(Color.web(accentHex()));
            return;
        }
        hint.setText(error);
        hint.setTextFill(Color.web("#ff3b30"));
    }

    private String dashboardFieldValidationError(String entityLabel, String fieldLabel, String value) {
        String normalizedField = fieldLabel == null ? "" : fieldLabel.trim().toLowerCase();
        String normalizedEntity = entityLabel == null ? "" : entityLabel.trim().toLowerCase();
        String cleaned = value == null ? "" : value.trim();

        if (cleaned.isEmpty()) {
            return normalizedField.contains("image") || normalizedField.contains("avatar") ? null : "This field is required";
        }

        if (normalizedField.contains("email")) {
            return cleaned.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$") ? null : "Invalid email format";
        }

        if (normalizedField.contains("date")) {
            return cleaned.length() >= 8 ? null : "Date value looks incomplete";
        }

        if (normalizedField.contains("place") || normalizedField.contains("number") || normalizedField.contains("accompagnant")) {
            try {
                int parsed = Integer.parseInt(cleaned);
                return parsed >= 0 ? null : "Value must be 0 or higher";
            } catch (NumberFormatException ignored) {
                return "Must be a numeric value";
            }
        }

        if (normalizedField.contains("title")
            || normalizedField.contains("name")
            || normalizedField.contains("subject")
            || normalizedField.contains("message")
            || normalizedField.contains("description")) {
            if (cleaned.length() < 3) return "At least 3 characters required";
            if (!Character.isLetter(cleaned.charAt(0))) return "Must start with a letter";
            return null;
        }

        if ("user".equals(normalizedEntity) && normalizedField.contains("phone")) {
            return cleaned.length() >= 8 ? null : "Phone looks too short";
        }

        return cleaned.length() >= 2 ? null : "At least 2 characters required";
    }

    private boolean isImageFieldLabel(String label) {
        String normalized = label == null ? "" : label.trim().toLowerCase();
        return normalized.contains("image") || normalized.contains("avatar");
    }

    private boolean isDateFieldLabel(String label) {
        String normalized = label == null ? "" : label.trim().toLowerCase();
        return normalized.contains("date") || normalized.contains("created") || normalized.contains("updated");
    }

    private LocalDate parseDateForPicker(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(trimmed + ", " + LocalDate.now().getYear(), DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        } catch (Exception ignored) {
        }
        return null;
    }

    private void styleDatePicker(DatePicker picker) {
        picker.setEditable(true);
        picker.setPrefHeight(36);
        picker.setMinHeight(36);
        picker.setMaxWidth(Double.MAX_VALUE);
        picker.setStyle(inputStyle(false, false));
        picker.setOnMouseEntered(_ -> picker.setStyle(inputStyle(true, picker.isFocused())));
        picker.setOnMouseExited(_ -> picker.setStyle(inputStyle(false, picker.isFocused())));
        picker.focusedProperty().addListener((ignoredObservable, ignoredOldValue, newVal) -> picker.setStyle(inputStyle(false, newVal)));
    }

    private List<String> eventStatusOptions(String currentValue) {
        LinkedHashSet<String> statuses = new LinkedHashSet<>();
        statuses.add("planifie");
        statuses.add("en_cours");
        statuses.add("termine");
        statuses.add("annule");

        String current = normalizeEventStatusValue(currentValue);
        if (!current.equals("-")) {
            statuses.add(current);
        }
        return new ArrayList<>(statuses);
    }

    private List<String> eventTypeOptions(String currentValue) {
        LinkedHashSet<String> types = new LinkedHashSet<>();
        types.add("reunion");
        types.add("social");
        types.add("formation");
        types.add("maintenance");
        types.add("culturel");
        types.add("sportif");

        String current = normalizeEventTypeValue(currentValue);
        if (!current.equals("-")) {
            types.add(current);
        }
        return new ArrayList<>(types);
    }

    private String normalizeEventTypeValue(String typeValue) {
        if (typeValue == null || typeValue.isBlank()) {
            return "-";
        }
        String token = typeValue.trim().toLowerCase().replace(' ', '_');
        return switch (token) {
            case "reunion", "social", "formation", "maintenance", "culturel", "sportif" -> token;
            default -> "-";
        };
    }

    private String formatEventTypeDisplay(String typeValue) {
        String type = normalizeEventTypeValue(typeValue);
        return switch (type) {
            case "reunion" -> "Reunion";
            case "social" -> "Social";
            case "formation" -> "Formation";
            case "maintenance" -> "Maintenance";
            case "culturel" -> "Culturel";
            case "sportif" -> "Sportif";
            default -> "Select Type";
        };
    }

    private String normalizeEventStatusValue(String statusValue) {
        if (statusValue == null || statusValue.isBlank()) {
            return "-";
        }
        String token = statusValue.trim().toLowerCase().replace(' ', '_');
        return switch (token) {
            case "planifie", "en_cours", "termine", "annule" -> token;
            case "planned" -> "planifie";
            case "ongoing", "in_progress" -> "en_cours";
            case "completed" -> "termine";
            case "cancelled" -> "annule";
            default -> "-";
        };
    }

    private String formatEventStatusDisplay(String statusValue) {
        String status = normalizeEventStatusValue(statusValue);
        return switch (status) {
            case "planifie" -> "Planned";
            case "en_cours" -> "In Progress";
            case "termine" -> "Completed";
            case "annule" -> "Cancelled";
            default -> "Select Status";
        };
    }

    private List<String> reponseUserOptions(String currentValue) {
        LinkedHashSet<String> users = new LinkedHashSet<>();
        for (User user : dashboardAdminService.users()) {
            String displayName = userDisplayName(user);
            if (!displayName.equals("Unknown")) {
                users.add(displayName);
            }
        }

        String current = normalizeUserDisplayName(currentValue);
        if (!current.equals("-")) {
            users.add(current);
        }
        return new ArrayList<>(users);
    }

    private List<String> reponseReclamationOptions(String currentValue) {
        return reponseReclamationOptionsFiltered(null);
    }

    private List<String> reponseReclamationOptionsFiltered(String userDisplayName) {
        LinkedHashSet<String> reclamations = new LinkedHashSet<>();
        
        // If we have a user name, we filter. If null, we show all (default behavior)
        for (Reclamation reclamation : dashboardAdminService.reclamations()) {
            if (userDisplayName != null && !"-".equals(userDisplayName)) {
                String repUser = userDisplayName(reclamation.getUser());
                if (!repUser.equals(userDisplayName)) {
                    continue;
                }
            }
            String title = normalizeReclamationTitle(reclamation != null ? reclamation.getTitreReclamations() : null);
            if (!title.equals("-")) {
                reclamations.add(title);
            }
        }
        return new ArrayList<>(reclamations);
    }

    private String normalizeUserDisplayName(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? "-" : normalized;
    }

    private String normalizeReclamationTitle(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? "-" : normalized;
    }

    private String userDisplayName(User user) {
        if (user == null) {
            return "Unknown";
        }
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? "Unknown" : fullName;
    }

    private List<String> reclamationStatusOptions(String currentValue) {
        LinkedHashSet<String> statuses = new LinkedHashSet<>();
        statuses.add("active");
        statuses.add("en_attente");
        statuses.add("refuse");
        statuses.add("termine");

        String current = normalizeReclamationStatusValue(currentValue);
        if (!current.equals("-")) {
            statuses.add(current);
        }
        return new ArrayList<>(statuses);
    }

    private String normalizeReclamationStatusValue(String statusValue) {
        if (statusValue == null || statusValue.isBlank()) {
            return "-";
        }
        String token = statusValue.trim().toLowerCase().replace(' ', '_');
        return switch (token) {
            case "active", "en_attente", "refuse", "termine" -> token;
            case "pending" -> "en_attente";
            case "rejected" -> "refuse";
            case "completed" -> "termine";
            default -> "-";
        };
    }

    private String formatReclamationStatusDisplay(String statusValue) {
        String status = normalizeReclamationStatusValue(statusValue);
        return switch (status) {
            case "active" -> "Active";
            case "en_attente" -> "Pending";
            case "refuse" -> "Rejected";
            case "termine" -> "Completed";
            default -> "Select Status";
        };
    }

    private void styleSelect(ComboBox<String> select, String placeholder, Function<String, String> displayFormatter) {
        select.setMaxWidth(Double.MAX_VALUE);
        select.setPrefHeight(36);
        select.setMinHeight(36);
        select.setPromptText(placeholder);
        
        // Enhanced combobox styling with glass morphism
        String baseStyle = "-fx-background-color:rgba(255,255,255,0.06);" +
            "-fx-border-color:rgba(255,255,255,0.15);" +
            "-fx-border-width:1;" +
            "-fx-background-radius:10px;" +
            "-fx-border-radius:10px;" +
            "-fx-text-fill:" + (isDark() ? "#e5e7eb" : "#111827") + ";" +
            "-fx-font-size:12;" +
            "-fx-font-family:'" + lightFont() + "';" +
            "-fx-padding:0 12 0 12;" +
            "-fx-focus-color:transparent;" +
            "-fx-faint-focus-color:transparent;";
        
        select.setStyle(baseStyle);
        
        // Custom button cell for displaying formatted text
        select.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(placeholder);
                } else {
                    setText(displayFormatter.apply(item));
                }
                setStyle("-fx-text-fill:" + (isDark() ? "#e5e7eb" : "#111827") + ";");
            }
        });
        
        // Custom list cell factory for dropdown items
        select.setCellFactory(ignored -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(displayFormatter.apply(item));
                    String cellStyle = "-fx-padding:8 12 8 12;" +
                        "-fx-text-fill:" + (isDark() ? "#e5e7eb" : "#111827") + ";" +
                        "-fx-font-size:12;" +
                        "-fx-font-family:'" + lightFont() + "';";
                    if (isSelected()) {
                        setStyle(cellStyle + "-fx-background-color:" + accentRgba(0.28) + ";");
                    } else {
                        setStyle(cellStyle + "-fx-background-color:transparent;");
                    }
                }
            }
        });
        
        // Custom popup styling
        select.setOnShown(ignored -> {
            if (select.getSkin() != null) {
                try {
                    var popup = (javafx.scene.control.skin.ComboBoxListViewSkin<?>) select.getSkin();
                    var listView = (javafx.scene.control.ListView<?>) popup.getPopupContent();
                    if (listView != null) {
                        listView.setStyle(
                            "-fx-background-color:rgba(20,20,20,0.95);" +
                            "-fx-control-inner-background:rgba(20,20,20,0.95);" +
                            "-fx-padding:0;" +
                            "-fx-border-color:rgba(255,255,255,0.12);" +
                            "-fx-border-width:1;" +
                            "-fx-border-radius:8;" +
                            "-fx-background-radius:8;"
                        );
                    }
                } catch (Exception ex) {
                    // Fallback if skin casting fails
                }
            }
        });
        
        // Hover and focus effects
        select.setOnMouseEntered(_ -> select.setStyle(inputStyle(true, select.isFocused())));
        select.setOnMouseExited(_ -> select.setStyle(inputStyle(false, select.isFocused())));
        select.focusedProperty().addListener((ignoredObservable, ignoredOldValue, newVal) -> select.setStyle(inputStyle(false, newVal)));
    }

    private String inputStyle(boolean hover, boolean focus) {
        String base = "-fx-background-color:rgba(255,255,255," + (focus ? "0.09" : (hover ? "0.08" : "0.06")) + ");" +
            "-fx-border-color:" + (focus ? accentRgba(0.45) : (hover ? accentRgba(0.35) : "rgba(255,255,255,0.15)")) + ";" +
            "-fx-border-width:1;" +
            "-fx-background-radius:10px;" +
            "-fx-border-radius:10px;" +
            "-fx-text-fill:" + (isDark() ? "#e5e7eb" : "#111827") + ";" +
            "-fx-font-size:12;" +
            "-fx-font-family:'" + lightFont() + "';" +
            "-fx-padding:0 12 0 12;" +
            "-fx-focus-color:transparent;" +
            "-fx-faint-focus-color:transparent;";
        if (hover || focus) {
            base += "-fx-effect:dropshadow(gaussian," + (focus ? accentRgba(0.25) : "rgba(0,0,0,0.15)") + "," + (focus ? "12" : "8") + ",0,0," + (focus ? "6" : "4") + ");";
        }
        return base;
    }

    private List<String> userRoleOptions(String currentValue) {
        LinkedHashSet<String> roles = new LinkedHashSet<>();
        roles.add("ROLE_ADMIN");
        roles.add("ROLE_SYNDIC");
        roles.add("ROLE_RESIDENT");

        for (User user : dashboardAdminService.users()) {
            String normalized = normalizeRoleValue(user.getRoleUser());
            if (!normalized.equals("-")) {
                roles.add(normalized);
            }
        }

        String current = normalizeRoleValue(currentValue);
        if (!current.equals("-")) {
            roles.add(current);
        }
        return new ArrayList<>(roles);
    }

    private String normalizeRoleValue(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            return "-";
        }
        String normalized = roleValue.trim().toUpperCase();
        if (normalized.equals("-")) {
            return "-";
        }
        if (normalized.startsWith("ROLE_")) {
            return normalized;
        }
        return "ROLE_" + normalized;
    }

    private boolean isTruthyText(String value) {
        String normalized = value == null ? "" : value.trim();
        return "yes".equalsIgnoreCase(normalized)
            || "true".equalsIgnoreCase(normalized)
            || "1".equalsIgnoreCase(normalized)
            || "verified".equalsIgnoreCase(normalized);
    }

    private String formatRoleDisplay(String roleValue) {
        if (roleValue == null || roleValue.isBlank() || "-".equals(roleValue)) {
            return "Select Role";
        }
        String normalized = roleValue.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        return switch (normalized) {
            case "ADMIN"    -> "Administrator";
            case "SYNDIC"   -> "Syndic";
            case "RESIDENT" -> "Resident";
            case "OWNER"    -> "Owner";
            default -> {
                // Format as Title Case: "CUSTOM_ROLE" -> "Custom Role"
                String[] parts = normalized.replace("_", " ").toLowerCase().split("\\s+");
                StringBuilder titleCase = new StringBuilder();
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        titleCase.append(Character.toUpperCase(part.charAt(0)))
                            .append(part.substring(1))
                            .append(" ");
                    }
                }
                yield titleCase.toString().trim();
            }
        };
    }

    private Button dangerAction(String text) {
        Button b = new Button(text);
        b.setFont(Font.font(lightFont(), FontWeight.NORMAL, 10));
        b.setPadding(new Insets(5, 12, 5, 12));
        b.setStyle(
            "-fx-background-color:rgba(239,68,68,0.18);" +
            "-fx-border-color:rgba(239,68,68,0.45);" +
            "-fx-border-width:1;" +
            "-fx-background-radius:999;" +
            "-fx-border-radius:999;" +
            "-fx-text-fill:#fecaca;" +
            "-fx-cursor:hand;"
        );
        return b;
    }

    private static class CrudSpec {
        String viewTitle;
        String viewSubtitle;
        String editTitle;
        String editSubtitle;
        String addTitle;
        String addSubtitle;
        String cancelLabel;
        String saveEditLabel;
        String saveAddLabel;
        String addButtonLabel;
        String viewDeleteLabel;

        static CrudSpec defaults(String entityLabel) {
            CrudSpec s = new CrudSpec();
            s.viewTitle = entityLabel + " Details";
            s.viewSubtitle = "View complete " + entityLabel.toLowerCase() + " information.";
            s.editTitle = "Edit " + entityLabel;
            s.editSubtitle = "Modify fields and save changes.";
            s.addTitle = "Add " + entityLabel;
            s.addSubtitle = "Create a new entry.";
            s.cancelLabel = "Discard";
            s.saveEditLabel = "Save changes";
            s.saveAddLabel = "Create";
            s.addButtonLabel = "Add " + entityLabel;
            s.viewDeleteLabel = "Delete " + entityLabel;
            return s;
        }
    }

    private Button pagerBtn(String text, boolean active) {
        Button b = new Button(text);
        b.setFont(Font.font(lightFont(), active ? FontWeight.BOLD : FontWeight.NORMAL, 11));
        b.setPadding(new Insets(5, 10, 5, 10));
        b.setMinWidth(30);
        String background = active ? accentGradient() : (isDark() ? "rgba(255,255,255,0.05)" : "rgba(15,23,42,0.05)");
        String border = active ? "transparent" : (isDark() ? "rgba(255,255,255,0.10)" : "rgba(15,23,42,0.14)");
        String borderWidth = active ? "0" : "1";
        String textColor = active ? "white" : (isDark() ? "rgba(255,255,255,0.72)" : "rgba(15,23,42,0.78)");
        b.setStyle(
            "-fx-background-color:" + background + ";" +
            "-fx-background-radius:8px;" +
            "-fx-border-color:" + border + ";" +
            "-fx-border-width:" + borderWidth + ";" +
            "-fx-border-radius:8px;" +
            "-fx-text-fill:" + textColor + ";" +
            "-fx-cursor:hand;"
        );
        return b;
    }

    private HBox mainSwitcher() {
        HBox c = new HBox(); c.setAlignment(Pos.CENTER);
        HBox pill = createPill(
            0,
            new Insets(4),
            isDark() ? "rgba(10,10,10,0.65)" : "rgba(248,250,252,0.96)",
            isDark() ? "rgba(255,255,255,0.1)" : "rgba(15,23,42,0.14)",
            100
        );
        String[] labels   = {"General","Users","Forum","Syndicat","Residence","Evenement"};
        String[] sections = {"general","users","forum","syndicat","residence","evenement"};
        for (int i = 0; i < labels.length; i++) {
            final String sec = sections[i];
            Button tab = new Button(labels[i]);
            tab.setFont(Font.font(boldFont(), FontWeight.BOLD, 12));
            tab.setPadding(new Insets(8,18,8,18));
            boolean active = sec.equals(activeSection);
            String background = active ? accentGradient() : "transparent";
            String textColor = active ? "white" : (isDark() ? "rgba(255,255,255,0.45)" : "rgba(15,23,42,0.74)");
            tab.setStyle(
                "-fx-background-color:" + background + ";" +
                "-fx-background-radius:100px;" +
                "-fx-text-fill:" + textColor + ";" +
                "-fx-cursor:hand;"
            );
            tab.setOnAction(_ -> switchSection(sec));
            pill.getChildren().add(tab);
        }
        c.getChildren().add(pill);
        return c;
    }

    /** Switcher used inside module pages (Users/Profile/Onboarding, etc.). */
    HBox moduleModeSwitcher(String[] labels, Consumer<String> onSelect) {
        HBox wrap = new HBox();
        wrap.setAlignment(Pos.CENTER);

        HBox pill = createPill(
            6,
            new Insets(6),
            isDark() ? "rgba(10,10,10,0.45)" : "rgba(248,250,252,0.95)",
            isDark() ? "rgba(255,255,255,0.1)" : "rgba(15,23,42,0.14)",
            100
        );
        DropShadow pillShadow = new DropShadow();
        pillShadow.setBlurType(BlurType.GAUSSIAN);
        pillShadow.setColor(Color.web(isDark() ? "rgba(0,0,0,0.35)" : "rgba(15,23,42,0.10)"));
        pillShadow.setRadius(20);
        pillShadow.setOffsetX(0);
        pillShadow.setOffsetY(6);
        pill.setEffect(pillShadow);

        List<Button> tabButtons = new ArrayList<>();

        String activeLabel = labels[0];
        for (String label : labels) {
            Button tab = new Button(label);
            tab.setFont(Font.font(boldFont(), FontWeight.BOLD, 12));
            tab.setPadding(new Insets(8, 18, 8, 18));
            tab.setAlignment(Pos.CENTER);
            if (label.equals(activeLabel)) {
                tab.setStyle(
                    "-fx-background-color:" + accentGradient() + ";" +
                    "-fx-background-radius:100px;" +
                    "-fx-text-fill:white;" +
                    "-fx-border-color:" + accentRgba(0.32) + ";" +
                    "-fx-border-width:1;" +
                    "-fx-border-radius:100px;" +
                    "-fx-cursor:hand;"
                );
            } else {
                tab.setStyle(
                    "-fx-background-color:transparent;" +
                    "-fx-background-radius:100px;" +
                    "-fx-text-fill:" + (isDark() ? "rgba(255,255,255,0.55)" : "rgba(15,23,42,0.78)") + ";" +
                    "-fx-cursor:hand;"
                );
            }

            tab.setOnAction(_ -> {
                for (Button b : tabButtons) {
                    b.setStyle(
                        "-fx-background-color:transparent;" +
                        "-fx-background-radius:100px;" +
                        "-fx-text-fill:" + (isDark() ? "rgba(255,255,255,0.55)" : "rgba(15,23,42,0.78)") + ";" +
                        "-fx-cursor:hand;"
                    );
                }
                tab.setStyle(
                    "-fx-background-color:" + accentGradient() + ";" +
                    "-fx-background-radius:100px;" +
                    "-fx-text-fill:white;" +
                    "-fx-border-color:" + accentRgba(0.32) + ";" +
                    "-fx-border-width:1;" +
                    "-fx-border-radius:100px;" +
                    "-fx-cursor:hand;"
                );
                onSelect.accept(label);
            });

            tabButtons.add(tab);
            pill.getChildren().add(tab);
        }

        wrap.getChildren().add(pill);
        return wrap;
    }

    private HBox subTabBar(String[] labels, Consumer<String> onSelect) {
        HBox c = new HBox(); c.setAlignment(Pos.CENTER);
        HBox pill = createPill(
            4,
            new Insets(6),
            isDark() ? "rgba(15,15,17,0.65)" : "rgba(248,250,252,0.95)",
            isDark() ? "rgba(255,255,255,0.08)" : "rgba(15,23,42,0.14)",
            16
        );
        String activeLabel = labels[0];
        for (String label : labels) {
            Button btn = new Button(label.toUpperCase());
            btn.setFont(Font.font(boldFont(), FontWeight.BOLD, 11));
            btn.setPadding(new Insets(5,16,5,16));
            styleSubTab(btn, label.equals(activeLabel));
            btn.setOnAction(_ -> {
                pill.getChildren().forEach(this::resetSubTabIfButton);
                styleSubTab(btn, true);
                onSelect.accept(label);
            });
            pill.getChildren().add(btn);
        }
        c.getChildren().add(pill);
        return c;
    }

    private void styleSubTab(Button b, boolean active) {
        String background = active ? accentRgba(0.2) : "transparent";
        String textColor = active ? "white" : (isDark() ? "rgba(255,255,255,0.4)" : "rgba(15,23,42,0.7)");
        String border = active ? accentRgba(0.3) : "transparent";
        b.setStyle(
            "-fx-background-color:" + background + ";" +
            "-fx-background-radius:12px;" +
            "-fx-text-fill:" + textColor + ";" +
            "-fx-border-color:" + border + ";" +
            "-fx-border-width:1;" +
            "-fx-border-radius:12px;" +
            "-fx-cursor:hand;"
        );
    }

    VBox sectionCard() {
        return glassCard();
    }

    private HBox createPill(double spacing, Insets padding, String background, String border, double radius) {
        HBox pill = new HBox(spacing);
        pill.setAlignment(Pos.CENTER);
        pill.setPadding(padding);
        pill.setStyle(
            "-fx-background-color:" + background + ";" +
            "-fx-background-radius:" + radius + "px;" +
            "-fx-border-color:" + border + ";" +
            "-fx-border-width:1;" +
            "-fx-border-radius:" + radius + "px;"
        );
        return pill;
    }

    private void resetSubTabIfButton(Node node) {
        if (node instanceof Button b) {
            styleSubTab(b, false);
        }
    }

    private HBox metric(String icon, String label, String val, String color) {
        HBox row = new HBox(6); row.setAlignment(Pos.CENTER_LEFT);
        Text ic = new Text(icon); ic.setFont(Font.font(13));
        Text lb = t(label, lightFont(), FontWeight.NORMAL, 13); lb.setFill(textMutedColor());
        Text vl = t(val,   boldFont(),  FontWeight.BOLD,   11); vl.setFill(Color.web(color));
        row.getChildren().addAll(ic, lb, vl);
        return row;
    }

    Region barR(int h, String color) {
        Region r = new Region(); r.setPrefWidth(12); r.setPrefHeight(h);
        r.setStyle("-fx-background-color:"+color+";-fx-background-radius:4 4 0 0;");
        return r;
    }

    VBox buildEmptyState(String message) {
        VBox empty = new VBox(10);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(30, 0, 30, 0));
        Text msg = t(message, lightFont(), FontWeight.NORMAL, 14);
        msg.setFill(textMutedColor());
        empty.getChildren().add(msg);
        return empty;
    }

    Text t(String s, String family, FontWeight w, double size) {
        Text tx = new Text(s);
        tx.setFont(Font.font(family, w, size));
        tx.setFill(textPrimaryColor());
        return tx;
    }
    String boldFont()  { return com.syndicati.MainApplication.getInstance().getBoldFontFamily();  }
    String lightFont() { return com.syndicati.MainApplication.getInstance().getLightFontFamily(); }

    /** Multi-mode module view template - used to reduce duplication across sections. */
    VBox moduleModeView(String name, String icon, String[] labels, java.util.function.Function<String, Node> contentMapper) {
        VBox shell = moduleShell(name, icon);
        VBox body = new VBox(16);
        body.setFillWidth(true);

        HBox sub = moduleModeSwitcher(labels, key -> {
            body.getChildren().setAll(contentMapper.apply(key));
        });

        body.getChildren().add(contentMapper.apply(labels[0]));
        shell.getChildren().addAll(sub, body);
        return shell;
    }




}
