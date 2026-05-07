package com.syndicati.components.home;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.BlurType;
import com.syndicati.utils.theme.ThemeManager;

public class CardGrid {
    private final GridPane root;

    public CardGrid() {
        this.root = new GridPane();
        setup();
    }

    private void setup() {
        root.setHgap(24);
        root.setVgap(24);
        root.setPadding(new Insets(16));

        // 2 rows x 4 cols placeholder cards
        for (int i = 0; i < 8; i++) {
            root.add(createCard("Card " + (i + 1)), i % 4, i / 4);
        }
    }

    private StackPane createCard(String title) {
        ThemeManager theme = ThemeManager.getInstance();
        StackPane card = new StackPane();
        card.setPrefSize(240, 180);
        card.setStyle(
            "-fx-background-color: " + theme.getLiquidGlassBackground() + ";" +
            "-fx-background-radius: 28px;" +
            "-fx-border-color: " + theme.getLiquidGlassBorder() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 28px;"
        );
        DropShadow glow = new DropShadow();
        glow.setBlurType(BlurType.GAUSSIAN);
        glow.setColor(theme.getNeonGlowColor().deriveColor(0, 1, 1, 0.2));
        glow.setRadius(16);
        glow.setOffsetX(0);
        glow.setOffsetY(4);
        card.setEffect(glow);

        Text t = new Text(title);
        t.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 16));
        t.setFill(Color.web(theme.getTextColor()));
        card.getChildren().add(t);
        StackPane.setAlignment(t, Pos.TOP_LEFT);
        StackPane.setMargin(t, new Insets(16));        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: " + theme.getTabHoverColor() + ";" +
            "-fx-background-radius: 28px;" +
            "-fx-border-color: " + theme.getLiquidGlassBorder() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 28px;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: " + theme.getLiquidGlassBackground() + ";" +
            "-fx-background-radius: 28px;" +
            "-fx-border-color: " + theme.getLiquidGlassBorder() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 28px;"
        ));

        return card;
    }

    public GridPane getRoot() { return root; }
}









