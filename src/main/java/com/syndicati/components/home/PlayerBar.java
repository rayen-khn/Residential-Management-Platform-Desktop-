package com.syndicati.components.home;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.BlurType;
import com.syndicati.utils.theme.ThemeManager;

public class PlayerBar {
    private final StackPane root;

    public PlayerBar() {
        this.root = new StackPane();
        setup();
    }

    private void setup() {
        ThemeManager theme = ThemeManager.getInstance();
        root.setPadding(new Insets(10, 16, 10, 16));
        root.setStyle(
            "-fx-background-color: " + theme.getDynamicIslandBackground() + ";" +
            "-fx-background-radius: 20px;" +
            "-fx-border-color: " + theme.getDynamicIslandBorder() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 20px;"
        );
        DropShadow glow = new DropShadow();
        glow.setBlurType(BlurType.GAUSSIAN);
        glow.setColor(theme.getNeonGlowColor().deriveColor(0, 1, 1, 0.25));
        glow.setRadius(18);
        glow.setOffsetX(0);
        glow.setOffsetY(4);
        root.setEffect(glow);

        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);
        Text title = new Text("Now Playing - Demo Track");
        title.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 12));
        title.setFill(Color.web(theme.getIslandTextColor()));
        content.getChildren().addAll(new Text(">"), title, new Text("||"));
        content.getChildren().forEach(n -> {
            if (n instanceof Text) {
                ((Text)n).setFill(Color.web(theme.getIslandTextColor()));
            }
        });
        root.getChildren().add(content);
    }

    public StackPane getRoot() { return root; }
}









