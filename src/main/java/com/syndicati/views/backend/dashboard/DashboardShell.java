package com.syndicati.views.backend.dashboard;

import com.syndicati.utils.navigation.NavigationManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

final class DashboardShell {

    private DashboardShell() {
    }

    static VBox buildSidebar(DashboardView view) {
        VBox sb = new VBox(10);
        sb.setAlignment(Pos.TOP_CENTER);
        sb.setFillWidth(true);
        VBox.setVgrow(sb, Priority.ALWAYS);
        sb.setStyle("-fx-background-color:transparent;");
        sb.setPadding(new Insets(12, 10, 12, 10));

        VBox topPill = view.glassPill(Pos.CENTER);
        topPill.setPadding(new Insets(12, 10, 12, 10));
        topPill.setSpacing(8);

        StackPane logoMark = new StackPane();
        logoMark.setPrefSize(44, 44);
        logoMark.setStyle(
            "-fx-background-color:" + view.accentGradient() + ";" +
            "-fx-background-radius:14px;" +
            "-fx-effect:dropshadow(gaussian," + view.accentRgba(0.45) + ",18,0.4,0,4);"
        );
        Text sLetter = view.t("S", view.boldFont(), FontWeight.BOLD, 26);
        sLetter.setFill(Color.WHITE);
        logoMark.getChildren().add(sLetter);

        VBox logoText = new VBox(1);
        logoText.setAlignment(Pos.CENTER_LEFT);
        Text logoT = view.t("SYNDICATI", view.boldFont(), FontWeight.BOLD, 15);
        Text adminT = view.t("Admin Panel", view.lightFont(), FontWeight.NORMAL, 12);
        adminT.setFill(view.isDark() ? Color.web("rgba(255,255,255,0.4)") : Color.web("rgba(15,23,42,0.55)"));
        logoT.setFill(view.isDark() ? Color.WHITE : Color.web("#111827"));
        logoText.getChildren().addAll(logoT, adminT);

        HBox logoRow = new HBox(10);
        logoRow.setAlignment(Pos.CENTER_LEFT);
        if (view.sidebarExpanded) {
            logoRow.getChildren().addAll(logoMark, logoText);
        } else {
            logoRow.setAlignment(Pos.CENTER);
            logoRow.getChildren().add(logoMark);
        }

        Button toggle = new Button(view.sidebarExpanded ? "<" : ">");
        toggle.setFont(Font.font(view.boldFont(), FontWeight.BOLD, 12));
        toggle.setPadding(new Insets(7, 10, 7, 10));
        toggle.setMaxWidth(view.sidebarExpanded ? Double.MAX_VALUE : Region.USE_PREF_SIZE);
        toggle.setStyle(
            "-fx-background-color:" + (view.isDark() ? "rgba(255,255,255,0.06)" : "rgba(15,23,42,0.06)") + ";" +
            "-fx-border-color:" + (view.isDark() ? "rgba(255,255,255,0.12)" : "rgba(15,23,42,0.14)") + ";" +
            "-fx-border-width:1;" +
            "-fx-border-radius:10px;" +
            "-fx-background-radius:10px;" +
            "-fx-text-fill:" + (view.isDark() ? "rgba(255,255,255,0.8)" : "rgba(15,23,42,0.84)") + ";" +
            "-fx-cursor:hand;"
        );
        toggle.setOnAction(e -> view.toggleSidebar());
        Tooltip.install(toggle, new Tooltip(view.sidebarExpanded ? "Collapse sidebar" : "Expand sidebar"));
        topPill.getChildren().addAll(logoRow, toggle);

        VBox navPill = view.glassPill(Pos.TOP_CENTER);
        navPill.setPadding(new Insets(14, view.sidebarExpanded ? 10 : 6, 14, view.sidebarExpanded ? 10 : 6));
        navPill.setSpacing(4);
        VBox.setVgrow(navPill, Priority.ALWAYS);
        navPill.getChildren().addAll(
            view.sidebarItem("\uD83D\uDCCA", "Dashboard", "general"),
            view.pillSep(),
            view.sidebarItem("\uD83D\uDC65", "Users", "users"),
            view.sidebarItem("\uD83D\uDCAC", "Forum", "forum"),
            view.sidebarItem("\uD83C\uDFDB\uFE0F", "Syndicat", "syndicat"),
            view.sidebarItem("\uD83C\uDFE2", "Residence", "residence"),
            view.sidebarItem("\uD83C\uDF89", "Evenement", "evenement")
        );

        VBox bottomPill = view.glassPill(Pos.CENTER);
        bottomPill.setPadding(new Insets(10, view.sidebarExpanded ? 10 : 6, 10, view.sidebarExpanded ? 10 : 6));
        bottomPill.setSpacing(8);

        Button back = new Button(view.sidebarExpanded ? "\u2302  Back to App" : "\u2302");
        back.setFont(Font.font(view.lightFont(), FontWeight.NORMAL, 12));
        back.setMaxWidth(Double.MAX_VALUE);
        back.setPadding(new Insets(10, 14, 10, 14));
        back.setAlignment(view.sidebarExpanded ? Pos.CENTER_LEFT : Pos.CENTER);
        view.styleBackButton(back, false);
        back.setOnMouseEntered(e -> view.styleBackButton(back, true));
        back.setOnMouseExited(e -> view.styleBackButton(back, false));
        back.setOnAction(e -> runExitCallback(view));
        if (!view.sidebarExpanded) {
            Tooltip.install(back, new Tooltip("Back to App"));
        }

        Button signOut = new Button(view.sidebarExpanded ? "\u23FB  Sign out" : "\u23FB");
        signOut.setFont(Font.font(view.lightFont(), FontWeight.NORMAL, 12));
        signOut.setMaxWidth(Double.MAX_VALUE);
        signOut.setPadding(new Insets(10, 14, 10, 14));
        signOut.setAlignment(view.sidebarExpanded ? Pos.CENTER_LEFT : Pos.CENTER);
        view.styleSignOutButton(signOut, false);
        signOut.setOnMouseEntered(e -> view.styleSignOutButton(signOut, true));
        signOut.setOnMouseExited(e -> view.styleSignOutButton(signOut, false));
        signOut.setOnAction(e -> com.syndicati.MainApplication.getInstance().logout());
        if (!view.sidebarExpanded) {
            Tooltip.install(signOut, new Tooltip("Sign out"));
        }

        bottomPill.getChildren().addAll(back, signOut);
        sb.getChildren().addAll(topPill, navPill, bottomPill);
        return sb;
    }

    static VBox buildMainArea(DashboardView view) {
        return view.createMainArea();
    }

    static HBox buildHeader(DashboardView view) {
        HBox h = new HBox(14);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(12, 24, 12, 24));
        h.setMinHeight(58);
        h.setMaxHeight(58);
        h.setStyle("-fx-background-color:" + (view.isDark() ? "rgba(0,0,0,0.98)" : "rgba(255,255,255,0.98)") + ";-fx-border-color:" + (view.isDark() ? "rgba(255,255,255,0.14)" : "rgba(15,23,42,0.12)") + ";-fx-border-width:0 0 1 0;");

        HBox searchBar = new HBox(8);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(7, 14, 7, 14));
        searchBar.setPrefWidth(280);
        searchBar.setStyle("-fx-background-color:" + (view.isDark() ? "rgba(255,255,255,0.08)" : "rgba(15,23,42,0.04)") + ";-fx-background-radius:10px;-fx-border-color:" + (view.isDark() ? "rgba(255,255,255,0.16)" : "rgba(15,23,42,0.14)") + ";-fx-border-width:1;-fx-border-radius:10px;");
        Text sch = new Text(" Search admin...");
        sch.setFont(Font.font(view.lightFont(), FontWeight.NORMAL, 13));
        sch.setFill(view.isDark() ? Color.web("rgba(255,255,255,0.60)") : Color.web("rgba(15,23,42,0.62)"));
        searchBar.getChildren().add(sch);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        StackPane bell = buildNotificationTrigger(view);

        HBox up = new HBox(8);
        up.setAlignment(Pos.CENTER);
        up.setPadding(new Insets(5, 12, 5, 6));
        up.setStyle("-fx-background-color:" + (view.isDark() ? "rgba(255,255,255,0.05)" : "rgba(15,23,42,0.05)") + ";-fx-background-radius:20px;-fx-border-color:" + (view.isDark() ? "rgba(255,255,255,0.08)" : "rgba(15,23,42,0.12)") + ";-fx-border-width:1;-fx-border-radius:20px;-fx-cursor:hand;");
        Circle av = new Circle(14);
        boolean hasAvatarImage = view.applyAvatarFill(av);
        Text ini = view.t(view.currentInitial(), view.boldFont(), FontWeight.BOLD, 12);
        ini.setFill(Color.WHITE);
        ini.setVisible(!hasAvatarImage);
        ini.setManaged(!hasAvatarImage);
        StackPane avStack = new StackPane(av, ini);
        VBox ui = new VBox(1);
        Text nm = view.t(view.currentDisplayName(), view.boldFont(), FontWeight.BOLD, 12);
        nm.setFill(view.isDark() ? Color.WHITE : Color.web("#111827"));
        Text rl = view.t(view.currentRoleLabel(), view.lightFont(), FontWeight.NORMAL, 12);
        rl.setFill(view.isDark() ? Color.web("rgba(255,255,255,0.4)") : Color.web("rgba(15,23,42,0.52)"));
        ui.getChildren().addAll(nm, rl);
        Text chevron = view.t("v", view.boldFont(), FontWeight.BOLD, 13);
        chevron.setFill(view.isDark() ? Color.web("rgba(255,255,255,0.65)") : Color.web("rgba(15,23,42,0.65)"));
        up.getChildren().addAll(avStack, ui, chevron);

        attachProfileDropdown(view, up);

        h.getChildren().addAll(searchBar, sp, bell, up);
        return h;
    }

    static StackPane buildNotificationTrigger(DashboardView view) {
        StackPane bell = new StackPane();
        bell.setPrefSize(38, 38);
        bell.setMaxSize(38, 38);
        bell.setStyle(
            "-fx-background-color:" + (view.isDark() ? "rgba(255,255,255,0.05)" : "rgba(15,23,42,0.05)") + ";" +
            "-fx-background-radius:19;" +
            "-fx-border-color:" + (view.isDark() ? "rgba(255,255,255,0.10)" : "rgba(15,23,42,0.12)") + ";" +
            "-fx-border-width:1;" +
            "-fx-border-radius:19;" +
            "-fx-cursor:hand;"
        );

        Text bellIc = view.t("\uD83D\uDD14", view.boldFont(), FontWeight.BOLD, 13);
        bellIc.setFill(view.isDark() ? Color.web("rgba(255,255,255,0.88)") : Color.web("rgba(15,23,42,0.84)"));

        Circle badgeDot = new Circle(4.5, Color.web("#ff3b30"));
        StackPane.setAlignment(badgeDot, Pos.TOP_RIGHT);
        StackPane.setMargin(badgeDot, new Insets(6, 6, 0, 0));

        VBox dropdownCard = buildNotificationDropdownCard(view);

        Runnable showPopup = () -> {
            cancelNotificationCloseDelay(view);
            if (view.profilePopup != null && view.profilePopup.isShowing()) {
                view.profilePopup.hide();
            }
            showNotificationPopupInsideStage(view, bell, dropdownCard);
        };

        bell.setOnMouseEntered(e -> showPopup.run());
        bell.setOnMouseExited(e -> scheduleCloseNotificationPopup(view, bell, dropdownCard));

        dropdownCard.setOnMouseEntered(e -> cancelNotificationCloseDelay(view));
        dropdownCard.setOnMouseExited(e -> {
            if (!bell.isHover()) {
                scheduleCloseNotificationPopup(view, bell, dropdownCard);
            }
        });

        bell.setOnMouseClicked(e -> {
            if (view.notificationPopup != null && view.notificationPopup.isShowing()) {
                view.notificationPopup.hide();
            } else {
                showPopup.run();
            }
        });

        bell.getChildren().addAll(bellIc, badgeDot);
        return bell;
    }

    static VBox buildNotificationDropdownCard(DashboardView view) {
        VBox box = new VBox();
        box.setPadding(new Insets(0));
        box.setSpacing(0);
        box.setPrefWidth(300);
        box.setMinWidth(300);
        box.setMaxWidth(300);
        box.setStyle(
            "-fx-background-color:" + (view.isDark() ? "rgba(12,12,18,0.94)" : "rgba(255,255,255,0.98)") + ";" +
            "-fx-border-color:" + (view.isDark() ? "rgba(255,255,255,0.12)" : "rgba(15,23,42,0.12)") + ";" +
            "-fx-border-width:1;" +
            "-fx-background-radius:16px;" +
            "-fx-border-radius:16px;" +
            "-fx-effect:dropshadow(gaussian," + (view.isDark() ? "rgba(0,0,0,0.45)" : "rgba(15,23,42,0.14)") + ",30,0,0,10);"
        );

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 20, 14, 20));

        Text title = view.t("Notifications", view.boldFont(), FontWeight.BOLD, 14);
        title.setFill(view.isDark() ? Color.web("#f3f4f6") : Color.web("#111827"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button close = new Button("x");
        close.setPadding(new Insets(4, 10, 4, 10));
        close.setFont(Font.font(view.lightFont(), FontWeight.NORMAL, 12));
        close.setStyle(
            "-fx-background-color:" + (view.isDark() ? "rgba(255,255,255,0.06)" : "rgba(15,23,42,0.06)") + ";" +
            "-fx-background-radius:999;" +
            "-fx-border-color:" + (view.isDark() ? "rgba(255,255,255,0.12)" : "rgba(15,23,42,0.14)") + ";" +
            "-fx-border-width:1;" +
            "-fx-border-radius:999;" +
            "-fx-text-fill:" + (view.isDark() ? "rgba(255,255,255,0.88)" : "rgba(15,23,42,0.84)") + ";" +
            "-fx-cursor:hand;"
        );
        close.setOnAction(e -> {
            cancelNotificationCloseDelay(view);
            if (view.notificationPopup != null) {
                view.notificationPopup.hide();
            }
        });

        VBox rows = new VBox();
        rows.setPadding(new Insets(8));
        rows.setSpacing(6);
        rows.getChildren().addAll(
            notificationRow(view, "Syndicati", "Your community dashboard is synced", "now"),
            notificationRow(view, "Forum", "A new reply landed in your discussion", "5 min"),
            notificationRow(view, "Residence", "A maintenance update is ready", "1 h")
        );

        header.getChildren().addAll(title, spacer, close);
        box.getChildren().addAll(header, rows);
        return box;
    }

    static VBox notificationRow(DashboardView view, String title, String body, String time) {
        VBox row = new VBox();
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setSpacing(2);

        Text tTitle = view.t(title, view.boldFont(), FontWeight.BOLD, 12);
        tTitle.setFill(view.isDark() ? Color.web("#f3f4f6") : Color.web("#111827"));
        Text tBody = view.t(body, view.lightFont(), FontWeight.NORMAL, 12);
        tBody.setFill(view.isDark() ? Color.web("#d4d4d8") : Color.web("#334155"));
        Text tTime = view.t(time, view.lightFont(), FontWeight.NORMAL, 11);
        tTime.setFill(view.isDark() ? Color.web("#a1a1aa") : Color.web("#64748b"));

        row.getChildren().addAll(tTitle, tBody, tTime);
        row.setStyle("-fx-background-color:transparent;-fx-background-radius:14px;");
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:" + (view.isDark() ? "rgba(255,255,255,0.06)" : "rgba(15,23,42,0.06)") + ";-fx-background-radius:14px;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color:transparent;-fx-background-radius:14px;"));
        return row;
    }

    static void scheduleCloseNotificationPopup(DashboardView view, StackPane anchor, VBox card) {
        cancelNotificationCloseDelay(view);
        view.notificationHideDelay = new PauseTransition(Duration.millis(180));
        view.notificationHideDelay.setOnFinished(e -> {
            if (view.notificationPopup != null && view.notificationPopup.isShowing() && !anchor.isHover() && !card.isHover()) {
                view.notificationPopup.hide();
            }
        });
        view.notificationHideDelay.playFromStart();
    }

    static void cancelNotificationCloseDelay(DashboardView view) {
        if (view.notificationHideDelay != null) {
            view.notificationHideDelay.stop();
        }
    }

    static void showNotificationPopupInsideStage(DashboardView view, StackPane bell, VBox card) {
        if (view.notificationPopup == null) {
            view.notificationPopup = new Popup();
            view.notificationPopup.setAutoHide(true);
            view.notificationPopup.setHideOnEscape(true);
            view.notificationPopup.setAutoFix(false);
            view.notificationPopup.getContent().setAll(card);
        }

        var bellBounds = bell.localToScreen(bell.getBoundsInLocal());
        if (bellBounds == null) {
            return;
        }

        Window window = bell.getScene() != null ? bell.getScene().getWindow() : null;
        if (window == null) {
            return;
        }

        double width = 300;
        double margin = 8;
        double desiredX = bellBounds.getMaxX() - width;
        double desiredY = bellBounds.getMaxY() + 12;

        if (!view.notificationPopup.isShowing()) {
            view.notificationPopup.show(bell, desiredX, desiredY);
        }

        double popupW = view.notificationPopup.getWidth() > 0 ? view.notificationPopup.getWidth() : width;
        double popupH = view.notificationPopup.getHeight() > 0 ? view.notificationPopup.getHeight() : Math.max(220, card.prefHeight(width));

        double minX = window.getX() + margin;
        double maxX = window.getX() + window.getWidth() - popupW - margin;
        double clampedX = Math.max(minX, Math.min(desiredX, maxX));

        double minY = window.getY() + margin;
        double maxY = window.getY() + window.getHeight() - popupH - margin;

        double y = desiredY;
        if (y > maxY) {
            y = bellBounds.getMinY() - popupH - 10;
        }
        double clampedY = Math.max(minY, Math.min(y, maxY));

        view.notificationPopup.setX(clampedX);
        view.notificationPopup.setY(clampedY);
    }

    static void attachProfileDropdown(DashboardView view, HBox profilePill) {
        VBox card = new VBox(8);
        card.setPrefWidth(280);
        card.setPadding(new Insets(14, 12, 10, 12));
        card.setStyle(
            "-fx-background-color:" + (view.isDark() ? "rgba(12,12,18,0.90)" : "rgba(255,255,255,0.98)") + ";" +
            "-fx-border-color:" + (view.isDark() ? "rgba(255,255,255,0.12)" : "rgba(15,23,42,0.12)") + ";" +
            "-fx-border-width:1;" +
            "-fx-background-radius:16px;" +
            "-fx-border-radius:16px;" +
            "-fx-effect:dropshadow(gaussian," + (view.isDark() ? "rgba(0,0,0,0.45)" : "rgba(15,23,42,0.14)") + ",30,0,0,10);"
        );

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 2, 8, 2));

        Circle av = new Circle(20);
        boolean hasAvatarImage = view.applyAvatarFill(av);
        Text init = view.t(view.currentInitial(), view.boldFont(), FontWeight.BOLD, 16);
        init.setFill(Color.WHITE);
        init.setVisible(!hasAvatarImage);
        init.setManaged(!hasAvatarImage);
        StackPane avatar = new StackPane(av, init);

        VBox info = new VBox(2);
        Text name = view.t(view.currentDisplayName(), view.boldFont(), FontWeight.BOLD, 15);
        name.setFill(view.isDark() ? Color.WHITE : Color.web("#111827"));
        Text mail = view.t(view.currentEmail(), view.lightFont(), FontWeight.NORMAL, 13);
        mail.setFill(view.isDark() ? Color.web("rgba(255,255,255,0.55)") : Color.web("rgba(15,23,42,0.55)"));
        info.getChildren().addAll(name, mail);
        header.getChildren().addAll(avatar, info);

        Region sep1 = new Region();
        sep1.setPrefHeight(1);
        sep1.setStyle("-fx-background-color:" + (view.isDark() ? "rgba(255,255,255,0.1)" : "rgba(15,23,42,0.10)") + ";");

        VBox items = new VBox(4);
        items.getChildren().addAll(
            dropdownItem(view, "\uD83D\uDC64", "My Profile", () -> openFrontendPage(view, "profile")),
            dropdownItem(view, "\u2302", "Main Home", () -> runExitCallback(view)),
            dropdownItem(view, "\u2699", "Settings", () -> openFrontendPage(view, "settings")),
            dropdownItemWithBadge(view, "\uD83D\uDCB3", "Billing", "4", () -> openFrontendPage(view, "profile"))
        );

        Region sep2 = new Region();
        sep2.setPrefHeight(1);
        sep2.setStyle("-fx-background-color:" + (view.isDark() ? "rgba(255,255,255,0.1)" : "rgba(15,23,42,0.10)") + ";");

        Button logout = dropdownItem(view, "\u23FB", "Log Out", () -> com.syndicati.MainApplication.getInstance().logout());
        logout.setStyle(
            "-fx-background-color:transparent;" +
            "-fx-background-radius:8px;" +
            "-fx-text-fill:#ef4444;" +
            "-fx-padding:9 10 9 10;" +
            "-fx-alignment:CENTER_LEFT;" +
            "-fx-cursor:hand;"
        );
        logout.setOnMouseEntered(e -> logout.setStyle(
            "-fx-background-color:rgba(239,68,68,0.12);" +
            "-fx-background-radius:8px;" +
            "-fx-text-fill:#ef4444;" +
            "-fx-padding:9 10 9 10;" +
            "-fx-alignment:CENTER_LEFT;" +
            "-fx-cursor:hand;"
        ));
        logout.setOnMouseExited(e -> logout.setStyle(
            "-fx-background-color:transparent;" +
            "-fx-background-radius:8px;" +
            "-fx-text-fill:#ef4444;" +
            "-fx-padding:9 10 9 10;" +
            "-fx-alignment:CENTER_LEFT;" +
            "-fx-cursor:hand;"
        ));

        card.getChildren().addAll(header, sep1, items, sep2, logout);

        view.profilePopup = new Popup();
        view.profilePopup.setAutoHide(true);
        view.profilePopup.setHideOnEscape(true);
        view.profilePopup.setAutoFix(false);
        view.profilePopup.getContent().setAll(card);

        PauseTransition hideDelay = new PauseTransition(Duration.millis(170));
        hideDelay.setOnFinished(e -> {
            if (view.profilePopup != null && view.profilePopup.isShowing() && !profilePill.isHover() && !card.isHover()) {
                view.profilePopup.hide();
            }
        });

        Runnable showPopup = () -> {
            hideDelay.stop();
            if (view.notificationPopup != null && view.notificationPopup.isShowing()) {
                view.notificationPopup.hide();
            }
            showProfilePopupInsideStage(view, profilePill, card);
        };

        profilePill.setOnMouseEntered(e -> showPopup.run());
        profilePill.setOnMouseExited(e -> hideDelay.playFromStart());
        card.setOnMouseEntered(e -> hideDelay.stop());
        card.setOnMouseExited(e -> {
            if (!profilePill.isHover()) {
                hideDelay.playFromStart();
            }
        });
        profilePill.setOnMouseClicked(e -> {
            if (view.profilePopup != null && view.profilePopup.isShowing()) {
                view.profilePopup.hide();
            } else {
                showPopup.run();
            }
        });
    }

    static void showProfilePopupInsideStage(DashboardView view, HBox profilePill, VBox card) {
        if (view.profilePopup == null) {
            return;
        }

        var pillBounds = profilePill.localToScreen(profilePill.getBoundsInLocal());
        if (pillBounds == null) {
            return;
        }

        Window window = profilePill.getScene() != null ? profilePill.getScene().getWindow() : null;
        if (window == null) {
            return;
        }

        double margin = 8;
        double desiredX = pillBounds.getMaxX() - 280;
        double desiredY = pillBounds.getMaxY() + 12;

        if (!view.profilePopup.isShowing()) {
            view.profilePopup.show(profilePill, desiredX, desiredY);
        }

        double popupW = view.profilePopup.getWidth() > 0 ? view.profilePopup.getWidth() : 280;
        double popupH = view.profilePopup.getHeight() > 0 ? view.profilePopup.getHeight() : Math.max(260, card.prefHeight(280));

        double minX = window.getX() + margin;
        double maxX = window.getX() + window.getWidth() - popupW - margin;
        double clampedX = Math.max(minX, Math.min(desiredX, maxX));

        double minY = window.getY() + margin;
        double maxY = window.getY() + window.getHeight() - popupH - margin;

        double y = desiredY;
        if (y > maxY) {
            y = pillBounds.getMinY() - popupH - 10;
        }
        double clampedY = Math.max(minY, Math.min(y, maxY));

        view.profilePopup.setX(clampedX);
        view.profilePopup.setY(clampedY);
    }

    static Button dropdownItem(DashboardView view, String icon, String label, Runnable action) {
        Button b = new Button(icon + "   " + label);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setFont(Font.font(view.lightFont(), FontWeight.NORMAL, 12));
        b.setStyle(
            "-fx-background-color:transparent;" +
            "-fx-background-radius:8px;" +
            "-fx-text-fill:" + (view.isDark() ? "rgba(255,255,255,0.9)" : "rgba(15,23,42,0.88)") + ";" +
            "-fx-padding:9 10 9 10;" +
            "-fx-alignment:CENTER_LEFT;" +
            "-fx-cursor:hand;"
        );
        b.setOnMouseEntered(e -> b.setStyle(
            "-fx-background-color:" + (view.isDark() ? "rgba(255,255,255,0.07)" : "rgba(15,23,42,0.07)") + ";" +
            "-fx-background-radius:8px;" +
            "-fx-text-fill:" + (view.isDark() ? "rgba(255,255,255,1)" : "rgba(15,23,42,1)") + ";" +
            "-fx-padding:9 10 9 10;" +
            "-fx-alignment:CENTER_LEFT;" +
            "-fx-cursor:hand;"
        ));
        b.setOnMouseExited(e -> b.setStyle(
            "-fx-background-color:transparent;" +
            "-fx-background-radius:8px;" +
            "-fx-text-fill:" + (view.isDark() ? "rgba(255,255,255,0.9)" : "rgba(15,23,42,0.88)") + ";" +
            "-fx-padding:9 10 9 10;" +
            "-fx-alignment:CENTER_LEFT;" +
            "-fx-cursor:hand;"
        ));
        b.setOnAction(e -> {
            if (view.profilePopup != null) {
                view.profilePopup.hide();
            }
            if (view.notificationPopup != null) {
                view.notificationPopup.hide();
            }
            action.run();
        });
        return b;
    }

    static Button dropdownItemWithBadge(DashboardView view, String icon, String label, String badge, Runnable action) {
        Button b = dropdownItem(view, icon, label, action);
        b.setText(icon + "   " + label + "          " + badge);
        return b;
    }

    static void openFrontendPage(DashboardView view, String page) {
        runExitCallback(view);
        Platform.runLater(() -> NavigationManager.getInstance().navigateTo(page));
    }

    static void runExitCallback(DashboardView view) {
        if (view.exitCallback != null) {
            view.exitCallback.run();
        }
    }
}
