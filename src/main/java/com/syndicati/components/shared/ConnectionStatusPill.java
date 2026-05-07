package com.syndicati.components.shared;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.BlurType;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import com.syndicati.utils.database.ConnectionManager;
import com.syndicati.utils.theme.ThemeManager;

/**
 * Connection Status Pill - Shows database connection status
 */
public class ConnectionStatusPill {
    
    private final HBox pillContainer;
    private final Label statusLabel;
    private final Circle statusIndicator;
    private final ConnectionManager connectionManager;
    private final ThemeManager themeManager;
    
    public ConnectionStatusPill() {
        this.connectionManager = ConnectionManager.getInstance();
        this.themeManager = ThemeManager.getInstance();
        
        // Create pill container
        pillContainer = new HBox();
        pillContainer.setSpacing(6);
        pillContainer.setAlignment(Pos.CENTER);
        pillContainer.setPadding(new Insets(6, 10, 6, 10));
        
        // Set height to match theme toggle (35px)
        pillContainer.setPrefHeight(35);
        pillContainer.setMaxHeight(35);
        pillContainer.setMinHeight(35);
        
        // Set a reasonable max width to prevent overriding theme toggle
        pillContainer.setMaxWidth(120);
        
        // Create status indicator circle
        statusIndicator = new Circle(6);
        statusIndicator.setFill(Color.RED); // Default to offline
        
        // Create status label
        statusLabel = new Label("Offline");
        statusLabel.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 11));
        statusLabel.setTextFill(Color.WHITE);        // Set max width for label to prevent overflow
        statusLabel.setMaxWidth(60);
        
        // Add components to container
        pillContainer.getChildren().addAll(statusIndicator, statusLabel);
        
        // Apply initial styling (start with offline)
        updatePillStyle(false);
        
        // Listen to connection status changes
        connectionManager.isConnectedProperty().addListener((obs, oldValue, newValue) -> {
            System.out.println("Connection status changed from " + oldValue + " to " + newValue);
            updateConnectionStatus(newValue);
        });
        
        // Set initial status based on current connection state
        updateConnectionStatus(connectionManager.isConnected());
    }
    
    /**
     * Update connection status with animation
     */
    private void updateConnectionStatus(boolean isConnected) {
        // Update status indicator color
        statusIndicator.setFill(isConnected ? Color.LIMEGREEN : Color.RED);
        
        // Update status text
        statusLabel.setText(isConnected ? "Connected" : "Offline");
        
        // Update pill styling
        updatePillStyle(isConnected);
        
        // Add subtle animation
        animateStatusChange();
    }
    
    /**
     * Update pill styling based on connection status
     */
    private void updatePillStyle(boolean isConnected) {
        String backgroundColor = isConnected ? 
            "rgba(34, 197, 94, 0.2)" : "rgba(239, 68, 68, 0.2)";
        String borderColor = isConnected ? 
            "rgba(34, 197, 94, 0.4)" : "rgba(239, 68, 68, 0.4)";
        
        pillContainer.setStyle(
            "-fx-background-color: " + backgroundColor + ";" +
            "-fx-background-radius: 20px;" +
            "-fx-border-color: " + borderColor + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 20px;"
        );
        
        // Add glassmorphism shadow
        DropShadow pillShadow = new DropShadow();
        pillShadow.setBlurType(BlurType.GAUSSIAN);
        pillShadow.setColor(Color.color(0, 0, 0, 0.3));
        pillShadow.setRadius(8);
        pillShadow.setOffsetX(0);
        pillShadow.setOffsetY(3);
        pillContainer.setEffect(pillShadow);
    }
    
    /**
     * Animate status change
     */
    private void animateStatusChange() {
        // Scale animation
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), pillContainer);
        scaleTransition.setFromX(1.0);
        scaleTransition.setToX(1.1);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToY(1.1);
        scaleTransition.setAutoReverse(true);
        scaleTransition.setCycleCount(2);
        
        // Fade animation for smooth transition
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(150), pillContainer);
        fadeTransition.setFromValue(0.8);
        fadeTransition.setToValue(1.0);
        
        // Play animations
        scaleTransition.play();
        fadeTransition.play();
    }
    
    /**
     * Get the pill container for adding to layouts
     */
    public HBox getPillContainer() {
        return pillContainer;
    }
    
    /**
     * Get current connection status
     */
    public boolean isConnected() {
        return connectionManager.isConnected();
    }
    
    /**
     * Get current status text
     */
    public String getStatusText() {
        return connectionManager.getConnectionStatus();
    }
}


