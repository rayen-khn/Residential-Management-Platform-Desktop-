package com.syndicati.views.frontend.services;

import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import com.syndicati.interfaces.ViewInterface;
import com.syndicati.utils.theme.ThemeManager;

/**
 * Service Detail View - Detailed service information page
 */
public class ServiceDetailView implements ViewInterface {
    
    private final VBox root;
    
    public ServiceDetailView() {
        this.root = new VBox();
        setupLayout();
    }
    
    private void setupLayout() {
        root.setSpacing(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(80, 40, 80, 40));
        
        // Apply theme styling
        applyThemeStyling();
        
        // Back button
        Button backButton = new Button("< Back to Services");
        backButton.setStyle(
            "-fx-background-color: " + ThemeManager.getInstance().getModernAccentColor() + ";" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 12px;" +
            "-fx-padding: 8 16 8 16;" +
            "-fx-font-size: 14px;"
        );
        backButton.setOnAction(e -> {
            // This will be handled by the navigation manager
            System.out.println("Back to Services clicked");
        });
        
        // Title
        Text title = new Text("Service Details");
        title.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 36));
        title.setFill(Color.web(ThemeManager.getInstance().getTextColor()));
        
        // Content
        VBox content = new VBox();
        content.setSpacing(20);
        content.setAlignment(Pos.TOP_LEFT);
        content.setMaxWidth(600);
        ThemeManager tm = ThemeManager.getInstance();
        String cardBg = tm.isDarkMode()
            ? "radial-gradient(focus-angle 28deg, focus-distance 22%, center 14% 12%, radius 130%, " + tm.toRgba(tm.getAccentHex(), 0.18) + " 0%, rgba(0,0,0,0.88) 64%, rgba(0,0,0,0.97) 100%), linear-gradient(to bottom right, rgba(255,255,255,0.06), rgba(255,255,255,0.015) 48%, " + tm.toRgba(tm.getAccentHex(), 0.08) + " 100%)"
            : "linear-gradient(to bottom right, rgba(255,255,255,0.96), rgba(255,255,255,0.88) 54%, rgba(243,247,255,0.94) 100%)";
        content.setStyle(
            "-fx-background-color: " + cardBg + ";" +
            "-fx-background-radius: 24px;" +
            "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), tm.isDarkMode() ? 0.34 : 0.22) + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 24px;" +
            "-fx-padding: 26px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.30), 30, 0.16, 0, 10);"
        );
        
        Text description = new Text("This is a detailed view of our service offerings. Here you would find comprehensive information about our development services, pricing, and process.");
        description.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 16));
        description.setFill(Color.web(ThemeManager.getInstance().getSecondaryTextColor()));
        description.setWrappingWidth(600);
        
        Text features = new Text("* Modern web applications\n* Responsive design\n* Cross-platform compatibility\n* 24/7 support");
        features.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        features.setFill(Color.web(ThemeManager.getInstance().getTextColor()));
        content.getChildren().addAll(description, features);
        
        root.getChildren().addAll(backButton, title, content);
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


