package com.syndicati.components.shared;

import javafx.beans.value.ChangeListener;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.BlurType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.animation.*;
import javafx.util.Duration;
import com.syndicati.utils.theme.ThemeManager;

/**
 * Dynamic Footer Component - Dynamic island design footer
 */
public class DynamicFooter {
    
    private final HBox root;
    private final StackPane wrapper;
    private ChangeListener<Number> gradientPhaseListener;
    
    public DynamicFooter() {
        this.wrapper = new StackPane();
        this.root = new HBox();
        setupLayout();
        startAnimations();
        startBorderAnimation();
    }

    private void startBorderAnimation() {
        gradientPhaseListener = (obs, oldVal, newVal) -> applyThemeStyling();
        ThemeManager.getInstance().gradientPhaseProperty().addListener(gradientPhaseListener);
    }
    
    private void setupLayout() {
        root.getChildren().clear();

        // Main footer container with dynamic island styling
        root.setSpacing(0);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(6, 16, 6, 16));
        
        // Apply dynamic island background with theme-aware colors
        applyThemeStyling();
        
        // Add root to wrapper (guard against duplicate on refreshTheme calls)
        if (!wrapper.getChildren().contains(root)) {
            wrapper.getChildren().add(root);
        }
        
        // Footer content container
        HBox footerContent = new HBox();
        footerContent.setSpacing(0);
        footerContent.setAlignment(Pos.CENTER);
        footerContent.setMaxWidth(Double.MAX_VALUE);
        
        // Left side - App branding
        VBox leftSection = new VBox();
        leftSection.setAlignment(Pos.CENTER_LEFT);
        leftSection.setSpacing(2);
        leftSection.setPadding(new Insets(0, 0, 0, 0)); // No left padding to push to edge
        
    Text appName = new Text("Syndicati");
    appName.setFont(javafx.scene.text.Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), javafx.scene.text.FontWeight.BOLD, 11));
        appName.setFill(Color.web(ThemeManager.getInstance().getIslandTextColor()));
        
    Text copyrightText = new Text("(c) 2025 All rights reserved");
    copyrightText.setFont(javafx.scene.text.Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), javafx.scene.text.FontWeight.NORMAL, 8));
        copyrightText.setFill(Color.web(ThemeManager.getInstance().getIslandSecondaryTextColor()));
        
        leftSection.getChildren().addAll(appName, copyrightText);
        
        // Center - Quick links with better styling
        HBox centerSection = new HBox();
        centerSection.setSpacing(12);
        centerSection.setAlignment(Pos.CENTER);
        
        // Create footer links with improved design
        centerSection.getChildren().addAll(
            createFooterLink("\ud83d\udcd8", "Documentation"),
            createFooterLink("\ud83c\udfa7", "Support"),
            createFooterLink("\ud83d\udc19", "GitHub"),
            createFooterLink("\ud83d\udcc4", "License")
        );
        
        // Right side - Made with love (enhanced)
        VBox rightSection = new VBox();
        rightSection.setAlignment(Pos.CENTER_RIGHT);
        rightSection.setSpacing(2);
        rightSection.setPadding(new Insets(0, 0, 0, 0)); // No right padding to push to edge
        
        HBox loveText = new HBox();
        loveText.setSpacing(6);
        loveText.setAlignment(Pos.CENTER_RIGHT);
        
    Text loveText1 = new Text("Made with");
    loveText1.setFont(javafx.scene.text.Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), javafx.scene.text.FontWeight.NORMAL, 9));
        loveText1.setFill(Color.web(ThemeManager.getInstance().getIslandSecondaryTextColor()));
        
        Text heart = new Text("care");
        heart.setFont(javafx.scene.text.Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), javafx.scene.text.FontWeight.BOLD, 9));
        
    Text loveText2 = new Text("by Amine");
    loveText2.setFont(javafx.scene.text.Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), javafx.scene.text.FontWeight.BOLD, 9));
        loveText2.setFill(Color.web(ThemeManager.getInstance().getModernAccentColor()));
        
        loveText.getChildren().addAll(loveText1, heart, loveText2);
        
    Text versionText = new Text("v1.0.0");
    versionText.setFont(javafx.scene.text.Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), javafx.scene.text.FontWeight.NORMAL, 7));
        versionText.setFill(Color.web(ThemeManager.getInstance().getIslandSecondaryTextColor()));
        
        rightSection.getChildren().addAll(loveText, versionText);
        
        // Add sections to footer content
        footerContent.getChildren().addAll(leftSection, centerSection, rightSection);
        
        // Set grow priorities to push content to edges
        HBox.setHgrow(leftSection, Priority.ALWAYS);
        HBox.setHgrow(centerSection, Priority.NEVER);
        HBox.setHgrow(rightSection, Priority.ALWAYS);
        
        // Force alignment to edges
        leftSection.setAlignment(Pos.CENTER_LEFT);
        rightSection.setAlignment(Pos.CENTER_RIGHT);
        
        root.getChildren().add(footerContent);
    }
    
    private VBox createFooterLink(String icon, String text) {
        VBox link = new VBox();
        link.setSpacing(3);
        link.setAlignment(Pos.CENTER);
        link.setPadding(new Insets(4, 10, 4, 10));
        link.setCursor(javafx.scene.Cursor.HAND);
        ThemeManager themeManager = ThemeManager.getInstance();
        boolean dark = themeManager.isDarkMode();
        String baseText = dark ? themeManager.getIslandSecondaryTextColor() : "#334155";
        String baseIcon = dark ? themeManager.getIslandTextColor() : "#0f172a";
        String hoverText = dark ? themeManager.getModernAccentColor() : "#0b1220";
        String hoverBg = dark ? themeManager.getLiquidGlassHover() : "rgba(15,23,42,0.08)";
        
        // Link icon with theme-aware styling
        Text linkIcon = new Text(icon);
        linkIcon.setFont(javafx.scene.text.Font.font(18));
        linkIcon.setFill(Color.web(baseIcon));
        
        // Link text with improved typography
        Text linkText = new Text(text);
        linkText.setFont(javafx.scene.text.Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), javafx.scene.text.FontWeight.NORMAL, 9));
        linkText.setFill(Color.web(baseText));
        link.getChildren().addAll(linkIcon, linkText);
        
        // Enhanced hover effects with smooth transitions
        link.setOnMouseEntered(e -> {
            link.setStyle(
                "-fx-background-color: " + hoverBg + ";" +
                "-fx-background-radius: 12px;" +
                "-fx-scale-x: 1.05;" +
                "-fx-scale-y: 1.05;"
            );
            
            // Animate icon
            ScaleTransition iconScale = new ScaleTransition(Duration.millis(150), linkIcon);
            iconScale.setToX(1.2);
            iconScale.setToY(1.2);
            iconScale.play();
            
            // Change text and icon color on hover
            linkText.setFill(Color.web(hoverText));
            linkIcon.setFill(Color.web(hoverText));
        });
        
        link.setOnMouseExited(e -> {
            link.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-scale-x: 1.0;" +
                "-fx-scale-y: 1.0;"
            );
            
            // Reset icon
            ScaleTransition iconScale = new ScaleTransition(Duration.millis(150), linkIcon);
            iconScale.setToX(1.0);
            iconScale.setToY(1.0);
            iconScale.play();
            
            // Reset text and icon color
            linkText.setFill(Color.web(baseText));
            linkIcon.setFill(Color.web(baseIcon));
        });
        
        // Add click effect
        link.setOnMousePressed(e -> {
            link.setStyle(
                "-fx-background-color: " + hoverBg + ";" +
                "-fx-background-radius: 12px;" +
                "-fx-scale-x: 0.95;" +
                "-fx-scale-y: 0.95;"
            );
        });
        
        link.setOnMouseReleased(e -> {
            link.setStyle(
                "-fx-background-color: " + hoverBg + ";" +
                "-fx-background-radius: 12px;" +
                "-fx-scale-x: 1.05;" +
                "-fx-scale-y: 1.05;"
            );
        });
        
        return link;
    }
    
    private void applyThemeStyling() {
        ThemeManager tm = ThemeManager.getInstance();
        if (tm.isDarkMode()) {
            double pulse = 0.32 + (0.12 * Math.sin(tm.gradientPhaseProperty().get() * Math.PI));
            double radius = 22 + (6 * Math.sin(tm.gradientPhaseProperty().get() * Math.PI));

            root.setStyle(
                "-fx-background-color: " + tm.getEffectiveAccentGradient() + ", " + tm.getDynamicIslandBackground() + ";" +
                "-fx-background-insets: 0, 1.5;" +
                "-fx-background-radius: 50px, 48.5px;" +
                "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.0) + ";" +
                "-fx-border-width: 0;" +
                "-fx-border-radius: 50px;"
            );

            DropShadow footerShadow = new DropShadow();
            footerShadow.setBlurType(BlurType.GAUSSIAN);
            footerShadow.setColor(tm.getNeonGlowColor().deriveColor(0, 1, 1, pulse));
            footerShadow.setRadius(radius);
            footerShadow.setOffsetX(0);
            footerShadow.setOffsetY(0);
            root.setEffect(footerShadow);
            return;
        }

        root.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, rgba(255,255,255,0.98), rgba(247,250,255,0.97));" +
            "-fx-background-radius: 50px;" +
            "-fx-border-color: rgba(15,23,42,0.14);" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 50px;"
        );

        DropShadow footerShadow = new DropShadow();
        footerShadow.setBlurType(BlurType.GAUSSIAN);
        footerShadow.setColor(Color.web("rgba(15,23,42,0.16)"));
        footerShadow.setRadius(20);
        footerShadow.setOffsetX(0);
        footerShadow.setOffsetY(4);
        root.setEffect(footerShadow);
    }
    
    private void startAnimations() {
        // Create subtle floating animation for the dynamic island
        TranslateTransition floatAnimation = new TranslateTransition(Duration.seconds(4), root);
        floatAnimation.setFromY(0);
        floatAnimation.setToY(2);
        floatAnimation.setAutoReverse(true);
        floatAnimation.setCycleCount(Animation.INDEFINITE);
        floatAnimation.setInterpolator(Interpolator.EASE_BOTH);
        floatAnimation.play();
    }
    
    public StackPane getRoot() {
        return wrapper;
    }
    
    public void cleanup() {
        if (gradientPhaseListener != null) {
            ThemeManager.getInstance().gradientPhaseProperty().removeListener(gradientPhaseListener);
            gradientPhaseListener = null;
        }
    }
    
    // Public method to refresh theme-dependent styles
    public void refreshTheme() {
        setupLayout();
    }
}


