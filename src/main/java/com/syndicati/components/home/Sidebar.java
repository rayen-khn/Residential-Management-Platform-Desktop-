package com.syndicati.components.home;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import com.syndicati.utils.navigation.NavigationManager;
import com.syndicati.utils.theme.ThemeManager;

/**
 * Sidebar (rebuilt): simple, robust, push-layout friendly.
 * - Collapsed width: 64px, icons only
 * - Expanded width: 220px, icons + labels
 * - Submenus appear directly beneath parent
 * - Always reserves space in HBox via pref/min/max width
 */
public class Sidebar {

    private static final double COLLAPSED_WIDTH = 64;
    private static final double EXPANDED_WIDTH = 220;
    private static final double LEFT_INSET_COLLAPSED = 10; // requested 10px from left
    private static final double RIGHT_INSET = 12;

    private final VBox root;
    private final VBox nav;
    private boolean expanded = false;

    public Sidebar() {
        ThemeManager theme = ThemeManager.getInstance();

        root = new VBox(6);
        root.setAlignment(Pos.TOP_LEFT);
        root.setPadding(new Insets(12, RIGHT_INSET, 12, LEFT_INSET_COLLAPSED));
        root.setPrefWidth(COLLAPSED_WIDTH);
        root.setMinWidth(COLLAPSED_WIDTH);
        root.setMaxWidth(COLLAPSED_WIDTH);
        root.setFillWidth(true);
        root.setStyle(
            "-fx-background-color: " + theme.getDynamicIslandBackground() + ";" +
            "-fx-background-radius: 24px;" +
            "-fx-border-color: transparent;" +
            "-fx-border-width: 0;" +
            "-fx-border-radius: 24px;"
        );

        // Toggle
        Button toggle = makeButton("=", "");
        toggle.setOnAction(e -> toggleExpanded());
        root.getChildren().add(toggle);

        // Nav
        nav = new VBox(4);
        nav.setAlignment(Pos.TOP_LEFT);
        nav.setFillWidth(true);
        root.getChildren().add(nav);

        addTopItem("Home", "H", () -> NavigationManager.getInstance().navigateTo("home"));
        addParentWithSubmenu(
            "Services", "S",
            new String[]{"Web Development", "Mobile Applications", "Technical Consulting"},
            new Runnable[]{
                () -> NavigationManager.getInstance().navigateTo("services"),
                () -> NavigationManager.getInstance().navigateTo("services"),
                () -> NavigationManager.getInstance().navigateTo("services")
            }
        );
        addParentWithSubmenu(
            "About", "i",
            new String[]{"Our Company Story", "Meet Our Team", "Contact Information"},
            new Runnable[]{
                () -> NavigationManager.getInstance().navigateTo("about"),
                () -> NavigationManager.getInstance().navigateTo("about"),
                () -> NavigationManager.getInstance().navigateTo("about")
            }
        );
        addTopItem("Dashboard", "D", () -> NavigationManager.getInstance().navigateTo("dashboard"));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        root.getChildren().add(spacer);

        // Start collapsed (icons only)
        setLabelsVisible(false);
    }

    public VBox getRoot() { return root; }

    private void addTopItem(String label, String icon, Runnable action) {
        VBox item = new VBox(2);
        Button btn = makeButton(icon, label);
        btn.setOnAction(e -> action.run());
        item.getChildren().add(btn);
        nav.getChildren().add(item);
    }

    private void addParentWithSubmenu(String label, String icon, String[] subs, Runnable[] actions) {
        VBox item = new VBox(2);
        Button parent = makeButton(icon, label);
        VBox submenu = new VBox(2);
        submenu.setAlignment(Pos.TOP_LEFT);
        submenu.setFillWidth(true);
        submenu.setManaged(false);
        submenu.setVisible(false);
        submenu.setPadding(new Insets(4, 0, 0, 18));

        for (int i = 0; i < subs.length; i++) {
            Button sub = makeButton(">", subs[i]);
            int idx = i;
            sub.setOnAction(e -> actions[idx].run());
            submenu.getChildren().add(sub);
        }

        parent.setOnAction(e -> toggleSubmenu(submenu));
        item.getChildren().addAll(parent, submenu);
        nav.getChildren().add(item);
    }

    private Button makeButton(String icon, String label) {
        ThemeManager theme = ThemeManager.getInstance();
        Text tIcon = new Text(icon);
        tIcon.setFill(Color.web(theme.getIslandTextColor()));
        tIcon.setFont(Font.font("Segoe UI Emoji", 14));

        Text tLabel = new Text(label);
        tLabel.setFill(Color.web(theme.getIslandTextColor()));
        tLabel.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 13));

        HBox content = new HBox(10, tIcon, tLabel);
        content.setAlignment(Pos.CENTER_LEFT);

        Button btn = new Button();
        btn.setGraphic(content);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.prefWidthProperty().bind(root.widthProperty().subtract(LEFT_INSET_COLLAPSED + RIGHT_INSET));
        btn.setMinHeight(36);
        btn.setTextOverrun(OverrunStyle.CLIP);
        btn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-background-radius: 12px;" +
            "-fx-text-fill: " + theme.getIslandTextColor() + ";"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: " + theme.getTabHoverColor() + ";" +
            "-fx-background-radius: 12px;" +
            "-fx-text-fill: white;"));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-background-radius: 12px;" +
            "-fx-text-fill: " + theme.getIslandTextColor() + ";"));

        // Show/hide label initially (collapsed)
        tLabel.setManaged(false);
        tLabel.setVisible(false);
        return btn;
    }

    private void toggleExpanded() {
        double target = expanded ? COLLAPSED_WIDTH : EXPANDED_WIDTH;
        expanded = !expanded;
        setLabelsVisible(false); // hide labels during tween
        applyLeftInset();
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(220),
            new KeyValue(root.prefWidthProperty(), target),
            new KeyValue(root.minWidthProperty(), target),
            new KeyValue(root.maxWidthProperty(), target)
        ));
        tl.setOnFinished(e -> setLabelsVisible(expanded));
        tl.play();
    }

    private void toggleSubmenu(VBox submenu) {
        if (submenu.isManaged()) {
            Timeline tl = new Timeline(new KeyFrame(Duration.millis(140),
                new KeyValue(submenu.opacityProperty(), 0.0),
                new KeyValue(submenu.translateYProperty(), -6)
            ));
            tl.setOnFinished(e -> { submenu.setManaged(false); submenu.setVisible(false); submenu.setTranslateY(0); });
            tl.play();
        } else {
            submenu.setManaged(true);
            submenu.setVisible(true);
            submenu.setOpacity(0);
            submenu.setTranslateY(-6);
            Timeline tl = new Timeline(new KeyFrame(Duration.millis(160),
                new KeyValue(submenu.opacityProperty(), 1.0),
                new KeyValue(submenu.translateYProperty(), 0)
            ));
            tl.play();
        }
    }

    private void setLabelsVisible(boolean visible) {
        for (javafx.scene.Node node : nav.getChildren()) {
            if (node instanceof VBox) {
                VBox item = (VBox) node;
                if (!item.getChildren().isEmpty() && item.getChildren().get(0) instanceof Button) {
                    Button btn = (Button) item.getChildren().get(0);
                    HBox content = (HBox) btn.getGraphic();
                    Text label = (Text) content.getChildren().get(1);
                    label.setManaged(visible);
                    label.setVisible(visible);
                }
                if (item.getChildren().size() > 1 && item.getChildren().get(1) instanceof VBox) {
                    VBox submenu = (VBox) item.getChildren().get(1);
                    for (javafx.scene.Node s : submenu.getChildren()) {
                        if (s instanceof Button) {
                            Button sb = (Button) s;
                            HBox sbContent = (HBox) sb.getGraphic();
                            Text sbLabel = (Text) sbContent.getChildren().get(1);
                            sbLabel.setManaged(visible);
                            sbLabel.setVisible(visible);
                        }
                    }
                }
            }
        }
        applyLeftInset();
    }

    private void applyLeftInset() {
        double left = expanded ? 10 : LEFT_INSET_COLLAPSED; // keep a small inset in both states
        root.setPadding(new Insets(12, RIGHT_INSET, 12, left));
    }
}


