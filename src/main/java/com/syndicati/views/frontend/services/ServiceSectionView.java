package com.syndicati.views.frontend.services;

import com.syndicati.MainApplication;
import com.syndicati.interfaces.ViewInterface;
import com.syndicati.utils.theme.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Generic UI-only page used by Services submenu sections.
 */
public class ServiceSectionView implements ViewInterface {

    private final VBox root;

    public ServiceSectionView(String title, String subtitle, String[][] cards) {
        this.root = new VBox(24);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(34, 22, 40, 22));
        root.setMaxWidth(Double.MAX_VALUE);
        root.setStyle("-fx-background-color: transparent;");

        ThemeManager tm = ThemeManager.getInstance();

        Text pageTitle = new Text(title + ".");
        pageTitle.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 42));
        pageTitle.setFill(Color.web(tm.getTextColor()));

        Text pageSubtitle = new Text(subtitle);
        pageSubtitle.setFont(Font.font(MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 15));
        pageSubtitle.setFill(Color.web(tm.getSecondaryTextColor()));

        VBox header = new VBox(8, pageTitle, pageSubtitle);
        header.setAlignment(Pos.CENTER);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setMaxWidth(1200);

        int colCount = 2;
        for (int i = 0; i < cards.length; i++) {
            VBox card = buildCard(cards[i][0], cards[i][1]);
            card.setMaxWidth(Double.MAX_VALUE);
            GridPane.setFillWidth(card, true);
            grid.add(card, i % colCount, i / colCount);
        }

        for (int i = 0; i < colCount; i++) {
            javafx.scene.layout.ColumnConstraints col = new javafx.scene.layout.ColumnConstraints();
            col.setMinWidth(0);
            col.setPercentWidth(50);
            col.setHgrow(Priority.ALWAYS);
            col.setFillWidth(true);
            grid.getColumnConstraints().add(col);
        }

        Text status = new Text("UI only for now - interactions and backend wiring come later.");
        status.setFont(Font.font(MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 12));
        status.setFill(Color.web(tm.getSecondaryTextColor()));

        root.getChildren().addAll(header, grid, status);
    }

    private VBox buildCard(String title, String description) {
        ThemeManager tm = ThemeManager.getInstance();

        VBox card = new VBox(10);
        card.setPadding(new Insets(22));
        card.setAlignment(Pos.TOP_LEFT);
        String surface = tm.isDarkMode()
            ? "radial-gradient(focus-angle 26deg, focus-distance 22%, center 14% 12%, radius 128%, " + tm.toRgba(tm.getAccentHex(), 0.20) + " 0%, rgba(0,0,0,0.88) 64%, rgba(0,0,0,0.97) 100%), " +
              "linear-gradient(to bottom right, rgba(255,255,255,0.06), rgba(255,255,255,0.015) 48%, " + tm.toRgba(tm.getAccentHex(), 0.08) + " 100%)"
            : "linear-gradient(to bottom right, rgba(255,255,255,0.96), rgba(255,255,255,0.88) 54%, rgba(243,247,255,0.94) 100%)";
        card.setStyle(
            "-fx-background-color: " + surface + ";" +
            "-fx-background-radius: 22px;" +
            "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), tm.isDarkMode() ? 0.36 : 0.24) + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 22px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 28, 0.16, 0, 9);"
        );

        StackPane accentBar = new StackPane();
        accentBar.setPrefHeight(4);
        accentBar.setMaxWidth(Double.MAX_VALUE);
        accentBar.setStyle(
            "-fx-background-color: " + tm.getEffectiveAccentGradient() + ";" +
            "-fx-background-radius: 999px;"
        );

        Text titleText = new Text(title);
        titleText.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 22));
        titleText.setFill(Color.web(tm.getTextColor()));

        Text descText = new Text(description);
        descText.setWrappingWidth(500);
        descText.setFont(Font.font(MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 13));
        descText.setFill(Color.web(tm.getSecondaryTextColor()));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(accentBar, titleText, descText, spacer);
        return card;
    }

    @Override
    public Pane getRoot() {
        return root;
    }

    @Override
    public void cleanup() {
        // No listeners or resources to release for this static placeholder view.
    }
}


