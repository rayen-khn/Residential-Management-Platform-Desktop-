package com.syndicati.views.frontend.about;

import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.BlurType;
import com.syndicati.interfaces.ViewInterface;
import com.syndicati.utils.theme.ThemeManager;
import com.syndicati.utils.navigation.NavigationManager;

/**
 * About View - Main about page with sub-menu options
 */
public class AboutView implements ViewInterface {
    
    private final VBox root;
    
    public AboutView() {
        this.root = new VBox();
        setupLayout();
    }
    
    private void setupLayout() {
        root.setSpacing(40);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(80, 40, 80, 40));
        
        // Apply theme styling
        applyThemeStyling();
        
        // Title
        Text title = new Text("About Us");
        title.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 48));
        title.setFill(Color.web(ThemeManager.getInstance().getTextColor()));
        
        // Subtitle
        Text subtitle = new Text("Learn more about our company and team");
        subtitle.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 18));
        subtitle.setFill(Color.web(ThemeManager.getInstance().getSecondaryTextColor()));        // About options grid
        HBox aboutGrid = new HBox();
        aboutGrid.setSpacing(30);
        aboutGrid.setAlignment(Pos.CENTER);
        
        // Option 1: Our Story
        VBox option1 = createAboutCard("ST", "Our Story", "Learn about our journey and mission");
        option1.setOnMouseClicked(e -> {
            NavigationManager.getInstance().awardInteractionXp(1);
            NavigationManager.getInstance().navigateTo("about-detail");
        });
        
        // Option 2: Our Team
        VBox option2 = createAboutCard("TM", "Our Team", "Meet the people behind our success");
        option2.setOnMouseClicked(e -> {
            NavigationManager.getInstance().awardInteractionXp(1);
            NavigationManager.getInstance().navigateTo("about-detail");
        });
        
        // Option 3: Contact Us
        VBox option3 = createAboutCard("CT", "Contact Us", "Get in touch with our team");
        option3.setOnMouseClicked(e -> {
            NavigationManager.getInstance().awardInteractionXp(1);
            NavigationManager.getInstance().navigateTo("about-detail");
        });
        
        aboutGrid.getChildren().addAll(option1, option2, option3);
        
        root.getChildren().addAll(title, subtitle, aboutGrid);
    }
    
    private VBox createAboutCard(String icon, String title, String description) {
        VBox card = new VBox();
        card.setSpacing(15);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(30, 25, 30, 25));
        card.setMinWidth(200);
        card.setMaxWidth(250);
        
        // Apply card styling
        ThemeManager themeManager = ThemeManager.getInstance();
        card.setStyle(cardStyle(themeManager, false));
        
        // Add shadow effect
        DropShadow cardShadow = new DropShadow();
        cardShadow.setBlurType(BlurType.GAUSSIAN);
        if (themeManager.isDarkMode()) {
            cardShadow.setColor(Color.color(0, 0, 0, 0.3));
        } else {
            cardShadow.setColor(Color.color(0, 0, 0, 0.1));
        }
        cardShadow.setRadius(10);
        cardShadow.setOffsetX(0);
        cardShadow.setOffsetY(4);
        card.setEffect(cardShadow);
        
        // Icon
        Text iconText = new Text(icon);
        iconText.setFont(Font.font(48));
        
        // Title
        Text titleText = new Text(title);
        titleText.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 20));
        titleText.setFill(Color.web(themeManager.getTextColor()));
        
        // Description
        Text descText = new Text(description);
        descText.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        descText.setFill(Color.web(themeManager.getSecondaryTextColor()));
        descText.setWrappingWidth(200);        card.getChildren().addAll(iconText, titleText, descText);
        
        // Add hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(cardStyle(themeManager, true));
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle(cardStyle(themeManager, false));
        });
        
        return card;
    }

    private String cardStyle(ThemeManager tm, boolean hover) {
        if (tm.isDarkMode()) {
            String accentSoft = tm.toRgba(tm.getAccentHex(), hover ? 0.22 : 0.16);
            String accentGlow = tm.toRgba(tm.getAccentHex(), hover ? 0.10 : 0.06);
            String border = tm.toRgba(tm.getAccentHex(), hover ? 0.52 : 0.34);
            return "-fx-background-color: radial-gradient(focus-angle 32deg, focus-distance 25%, center 16% 12%, radius 125%, " + accentSoft + " 0%, rgba(0,0,0,0.88) 62%, rgba(0,0,0,0.96) 100%), " +
                   "linear-gradient(to bottom right, rgba(255,255,255,0.06), rgba(255,255,255,0.015) 46%, " + accentGlow + " 100%);" +
                   "-fx-background-radius: 20px;" +
                   "-fx-border-color: " + border + ";" +
                   "-fx-border-width: 1px;" +
                   "-fx-border-radius: 20px;";
        }
        String border = tm.toRgba(tm.getAccentHex(), hover ? 0.34 : 0.24);
        return "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.96), rgba(255,255,255,0.86) 54%, rgba(243,247,255,0.92) 100%);" +
               "-fx-background-radius: 20px;" +
               "-fx-border-color: " + border + ";" +
               "-fx-border-width: 1px;" +
               "-fx-border-radius: 20px;";
    }
    
    private void applyThemeStyling() {
        // Don't apply background color - let ImageBackground handle it
        root.setStyle("-fx-background-color: transparent;");
    }
    
    @Override
    public Pane getRoot() {
        return root;
    }
    
    public void cleanup() {
        // Cleanup resources if needed
    }
}


