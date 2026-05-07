package com.syndicati.views.frontend.home;

import com.syndicati.MainApplication;
import com.syndicati.components.shared.DynamicFooter;
import com.syndicati.components.shared.DynamicHeader;
import com.syndicati.interfaces.ViewInterface;
import com.syndicati.models.user.User;
import com.syndicati.utils.session.SessionManager;
import com.syndicati.utils.theme.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Pre-destination page for admin-area roles shown right after login.
 */
public class AdminDestinationChoiceView implements ViewInterface {

    private final StackPane root;
    private final ThemeManager tm;
    private final DynamicHeader header;
    private final DynamicFooter footer;
    private Runnable onChooseHome;
    private Runnable onChooseDashboard;

    public AdminDestinationChoiceView() {
        this.root = new StackPane();
        this.tm = ThemeManager.getInstance();
        this.header = new DynamicHeader();
        this.footer = new DynamicFooter();
        setupLayout();
    }

    public void setOnChooseHome(Runnable onChooseHome) {
        this.onChooseHome = onChooseHome;
    }

    public void setOnChooseDashboard(Runnable onChooseDashboard) {
        this.onChooseDashboard = onChooseDashboard;
    }

    @Override
    public StackPane getRoot() {
        return root;
    }

    private void setupLayout() {
        root.setStyle("-fx-background-color: " + (tm.isDarkMode() ? "#000000" : "#f4f7fb") + ";" + tm.getScrollbarVariableStyle());
        root.setBackground(new Background(new BackgroundFill(
            Color.web(tm.isDarkMode() ? "#000000" : "#f4f7fb"),
            CornerRadii.EMPTY,
            Insets.EMPTY
        )));

        VBox darkPanel = new VBox();
        darkPanel.setAlignment(Pos.TOP_LEFT);
        darkPanel.setPadding(new Insets(0));
        darkPanel.setSpacing(0);
        darkPanel.setStyle(
            "-fx-background-color: " + (tm.isDarkMode() ? "#000000" : "linear-gradient(to bottom right, #fbfdff 0%, #f3f7fc 52%, #eef4fb 100%)") + ";" +
            "-fx-border-color: " + (tm.isDarkMode() ? tm.toRgba(tm.getAccentHex(), 0.26) : "rgba(15,23,42,0.12)") + ";" +
            "-fx-border-width: 1px;"
        );
        darkPanel.maxWidthProperty().bind(root.widthProperty());
        darkPanel.prefWidthProperty().bind(root.widthProperty());
        darkPanel.maxHeightProperty().bind(root.heightProperty());
        darkPanel.prefHeightProperty().bind(root.heightProperty());

        HBox windowBar = createWindowBar();

        StackPane centerStage = new StackPane();
        centerStage.setPadding(new Insets(160, 20, 90, 20));
        centerStage.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(centerStage, Priority.ALWAYS);

        VBox heroCard = new VBox(16);
        heroCard.setAlignment(Pos.CENTER);
        heroCard.setPadding(new Insets(28, 30, 28, 30));
        heroCard.setMaxWidth(700);
        heroCard.setStyle(
            "-fx-background-color: " + (tm.isDarkMode()
                ? "linear-gradient(to bottom right, rgba(22,22,22,0.96), rgba(10,10,10,0.98))"
                : "linear-gradient(to bottom right, rgba(255,255,255,0.98), rgba(247,248,250,0.96))") + ";" +
            "-fx-background-radius: 26;" +
            "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.35) + ";" +
            "-fx-border-width: 1.2;" +
            "-fx-border-radius: 26;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.38), 36, 0.25, 0, 16);"
        );

        String name = currentDisplayName();

        Text welcome = new Text("Welcome " + name);
        welcome.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.EXTRA_BOLD, 42));
        welcome.setFill(tm.isDarkMode() ? Color.web("#f9fafb") : Color.web("#0f172a"));

        Text brand = new Text("to Syndicati.");
        brand.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.EXTRA_BOLD, 40));
        brand.setFill(Color.web(tm.getAccentHex()));

        Text subtitle = new Text("Choose your destination to continue.");
        subtitle.setFont(Font.font(MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 15));
        subtitle.setFill(tm.isDarkMode() ? Color.web("rgba(255,255,255,0.72)") : Color.web("rgba(15,23,42,0.72)"));

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);

        javafx.scene.control.Button homeBtn = new javafx.scene.control.Button("Go Home");
        homeBtn.setStyle(
            "-fx-background-color: " + (tm.isDarkMode() ? "rgba(255,255,255,0.08)" : "rgba(15,23,42,0.08)") + ";" +
            "-fx-text-fill: " + (tm.isDarkMode() ? "#f8fafc" : "#111827") + ";" +
            "-fx-font-weight: 800;" +
            "-fx-font-size: 14;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 12 22 12 22;" +
            "-fx-cursor: hand;"
        );
        homeBtn.setOnAction(e -> {
            if (onChooseHome != null) {
                onChooseHome.run();
            }
        });

        javafx.scene.control.Button dashboardBtn = new javafx.scene.control.Button("Go Dashboard");
        dashboardBtn.setStyle(
            "-fx-background-color: " + tm.getEffectiveAccentGradient() + ";" +
            "-fx-text-fill: #ffffff;" +
            "-fx-font-weight: 900;" +
            "-fx-font-size: 14;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 12 24 12 24;" +
            "-fx-cursor: hand;"
        );
        dashboardBtn.setOnAction(e -> {
            if (onChooseDashboard != null) {
                onChooseDashboard.run();
            }
        });

        actions.getChildren().addAll(homeBtn, dashboardBtn);
        heroCard.getChildren().addAll(welcome, brand, subtitle, actions);
        centerStage.getChildren().add(heroCard);

        VBox footerWrap = new VBox(footer.getRoot());
        footerWrap.setAlignment(Pos.CENTER);
        footerWrap.setPadding(new Insets(0, 0, 22, 0));

        StackPane headerLayer = new StackPane(header.getRoot());
        headerLayer.setAlignment(Pos.TOP_CENTER);
        headerLayer.setPadding(new Insets(0, 12, 0, 12));
        // Do not let the full header layer consume clicks outside the island itself.
        headerLayer.setPickOnBounds(false);
        headerLayer.prefWidthProperty().bind(root.widthProperty());
        headerLayer.maxWidthProperty().bind(root.widthProperty());

        darkPanel.getChildren().addAll(centerStage, footerWrap);

        root.getChildren().addAll(darkPanel, headerLayer, windowBar);
        StackPane.setAlignment(headerLayer, Pos.TOP_CENTER);
        StackPane.setMargin(headerLayer, new Insets(48, 0, 0, 0));
        StackPane.setAlignment(windowBar, Pos.TOP_LEFT);

        header.setMainContainer(root);
        header.setBackgroundUpdateCallback(() -> {
            root.setStyle("-fx-background-color: " + (tm.isDarkMode() ? "#000000" : "#f4f7fb") + ";" + tm.getScrollbarVariableStyle());
        });
    }

    private String currentDisplayName() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            return "Admin";
        }
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? "Admin" : full;
    }

    private HBox createWindowBar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setSpacing(8);
        bar.setPadding(new Insets(4, 16, 4, 16));
        bar.setPrefHeight(40);
        bar.setMinHeight(40);
        bar.setMaxHeight(40);
        bar.setStyle("-fx-background-color: transparent; -fx-background-radius: 0;");
        bar.setPickOnBounds(false);
        bar.setMouseTransparent(false);

        Region dragRegion = new Region();
        HBox.setHgrow(dragRegion, Priority.ALWAYS);
        dragRegion.setMinHeight(40);
        dragRegion.setPrefHeight(40);
        dragRegion.setStyle("-fx-cursor: move;");

        javafx.scene.control.Button btnMin = new javafx.scene.control.Button("");
        javafx.scene.control.Button btnMax = new javafx.scene.control.Button("");
        javafx.scene.control.Button btnClose = new javafx.scene.control.Button("");
        styleMacOSButton(btnMin, "#febc2e", "-");
        styleMacOSButton(btnMax, "#28c840", "+");
        styleMacOSButton(btnClose, "#ff5f57", "x");

        HBox buttonContainer = new HBox(8);
        buttonContainer.setAlignment(Pos.CENTER_LEFT);
        buttonContainer.getChildren().addAll(btnMin, btnMax, btnClose);
        buttonContainer.setPickOnBounds(false);

        btnMin.setOnAction(e -> ((javafx.stage.Stage) root.getScene().getWindow()).setIconified(true));
        btnMax.setOnAction(e -> {
            javafx.stage.Stage stage = (javafx.stage.Stage) root.getScene().getWindow();
            stage.setMaximized(!stage.isMaximized());
        });
        btnClose.setOnAction(e -> ((javafx.stage.Stage) root.getScene().getWindow()).close());

        final double[] dragOffset = new double[2];
        dragRegion.setOnMousePressed(e -> {
            javafx.stage.Stage stage = (javafx.stage.Stage) root.getScene().getWindow();
            dragOffset[0] = e.getScreenX() - stage.getX();
            dragOffset[1] = e.getScreenY() - stage.getY();
        });
        dragRegion.setOnMouseDragged(e -> {
            javafx.stage.Stage stage = (javafx.stage.Stage) root.getScene().getWindow();
            if (!stage.isMaximized()) {
                stage.setX(e.getScreenX() - dragOffset[0]);
                stage.setY(e.getScreenY() - dragOffset[1]);
            }
        });

        bar.getChildren().addAll(dragRegion, buttonContainer);
        return bar;
    }

    private void styleMacOSButton(javafx.scene.control.Button btn, String color, String symbol) {
        btn.setMinSize(12, 12);
        btn.setPrefSize(12, 12);
        btn.setMaxSize(12, 12);
        btn.setFocusTraversable(false);
        btn.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-background-radius: 6px;" +
            "-fx-border-color: rgba(0,0,0,0.2);" +
            "-fx-border-width: 0.5px;" +
            "-fx-border-radius: 6px;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 8px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: transparent;" +
            "-fx-padding: 0;"
        );

        btn.setOnMouseEntered(e -> {
            btn.setText(symbol);
            btn.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-background-radius: 6px;" +
                "-fx-border-color: rgba(0,0,0,0.3);" +
                "-fx-border-width: 0.5px;" +
                "-fx-border-radius: 6px;" +
                "-fx-cursor: hand;" +
                "-fx-font-size: 8px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: rgba(0,0,0,0.7);" +
                "-fx-padding: 0;"
            );
        });

        btn.setOnMouseExited(e -> {
            btn.setText("");
            btn.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-background-radius: 6px;" +
                "-fx-border-color: rgba(0,0,0,0.2);" +
                "-fx-border-width: 0.5px;" +
                "-fx-border-radius: 6px;" +
                "-fx-cursor: hand;" +
                "-fx-font-size: 8px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: transparent;" +
                "-fx-padding: 0;"
            );
        });
    }

    public void cleanup() {
        header.cleanup();
        footer.cleanup();
    }
}
