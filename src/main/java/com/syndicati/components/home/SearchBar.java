package com.syndicati.components.home;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.BlurType;
import com.syndicati.utils.theme.ThemeManager;

public class SearchBar {
    private final HBox root;
    private final TextField input;

    public SearchBar() {
        this.root = new HBox();
        this.input = new TextField();
        setup();
    }

    private void setup() {
        ThemeManager theme = ThemeManager.getInstance();
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(16));
        root.setSpacing(12);
        root.setStyle(
            "-fx-background-color: " + theme.getDynamicIslandBackground() + ";" +
            "-fx-background-radius: 24px;" +
            "-fx-border-color: " + theme.getDynamicIslandBorder() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 24px;"
        );
        DropShadow glow = new DropShadow();
        glow.setBlurType(BlurType.GAUSSIAN);
        glow.setColor(theme.getNeonGlowColor().deriveColor(0, 1, 1, 0.25));
        glow.setRadius(18);
        glow.setOffsetX(0);
        glow.setOffsetY(4);
        root.setEffect(glow);

        Text lens = new Text("S");
        lens.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 16));
        lens.setFill(Color.web(theme.getIslandTextColor()));

        input.setPromptText("Search...");
        input.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), 14));
        input.setPrefHeight(36);
        input.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: " + theme.getIslandTextColor() + ";" +
            "-fx-prompt-text-fill: rgba(255,255,255,0.55);" +
            "-fx-border-color: transparent;"
        );

        root.getChildren().addAll(lens, input);
    }

    public HBox getRoot() { return root; }
}









