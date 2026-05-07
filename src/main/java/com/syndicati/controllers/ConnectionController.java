package com.syndicati.controllers;

import com.syndicati.services.DatabaseService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.application.Platform;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Connection Controller - Manages database connection status and monitoring
 */
public class ConnectionController {
    
    private static ConnectionController instance;
    private final DatabaseService databaseService;
    private final BooleanProperty isConnected = new SimpleBooleanProperty(false);
    private final StringProperty connectionStatus = new SimpleStringProperty("Offline");
    private final ExecutorService connectionCheckExecutor;
    private ScheduledExecutorService scheduler;
    private boolean monitoringStarted = false;
    private boolean isCurrentlyConnected = false;
    
    private ConnectionController() {
        this.databaseService = DatabaseService.getInstance();
        this.connectionCheckExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        // Force initial offline state
        isConnected.set(false);
        connectionStatus.set("Offline");
        
        // Don't start monitoring automatically - wait for application to start
        // startConnectionMonitoring();
    }
    
    public static ConnectionController getInstance() {
        if (instance == null) {
            instance = new ConnectionController();
        }
        return instance;
    }
    
    /**
     * Start monitoring database connection status
     */
    private void startConnectionMonitoring() {
        // Test connection immediately
        checkConnectionStatus();
        
        // Start smart monitoring - only retry when offline
        scheduleNextCheck();
    }
    
    /**
     * Schedule the next connection check based on current status
     */
    private void scheduleNextCheck() {
        if (isCurrentlyConnected) {
            // If connected, stop checking completely
            System.out.println("Database connected - stopping connection checks");
        } else {
            // If offline, check again in 3 seconds
            scheduler.schedule(this::checkConnectionStatus, 3, TimeUnit.SECONDS);
            System.out.println("Database offline - retrying in 3 seconds");
        }
    }
    
    /**
     * Start connection monitoring (called when application launches)
     */
    public void startMonitoring() {
        if (monitoringStarted) {
            System.out.println("Database connection monitoring already started");
            return;
        }
        
        System.out.println("Starting database connection monitoring...");
        // Create a daemon thread pool so it won't prevent JVM shutdown
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            return Thread.ofPlatform()
                .daemon(true)
                .name("DB-Connection-Monitor")
                .unstarted(r);
        });
        this.monitoringStarted = true;
        startConnectionMonitoring();
    }
    
    /**
     * Check connection status asynchronously
     */
    private void checkConnectionStatus() {
        System.out.println("Checking database connection status...");
        CompletableFuture.supplyAsync(() -> {
            try {
                boolean result = databaseService.isDatabaseAvailable();
                System.out.println("Database availability check result: " + result);
                return result;
            } catch (Exception e) {
                System.out.println("Error checking database connection: " + e.getMessage());
                return false;
            }
        }, connectionCheckExecutor).thenAccept(connected -> {
            Platform.runLater(() -> {
                updateConnectionStatus(connected);
                // Schedule next check based on new status
                scheduleNextCheck();
            });
        });
    }
    
    /**
     * Update connection status
     */
    private void updateConnectionStatus(boolean connected) {
        boolean statusChanged = (isCurrentlyConnected != connected);
        isCurrentlyConnected = connected;
        
        isConnected.set(connected);
        connectionStatus.set(connected ? "Connected" : "Offline");
        
        // Log status change
        if (statusChanged) {
            System.out.println("[INFO] Database connection status changed: " + (connected ? "Connected" : "Offline"));
        } else {
            System.out.println("Database connection status: " + (connected ? "Connected" : "Offline"));
        }
    }
    
    /**
     * Force check connection status (for manual testing)
     */
    public void forceConnectionCheck() {
        if (monitoringStarted) {
            checkConnectionStatus();
        } else {
            System.out.println("Connection monitoring not started yet");
        }
    }
    
    /**
     * Force offline status (for testing)
     */
    public void forceOffline() {
        Platform.runLater(() -> {
            updateConnectionStatus(false);
        });
    }
    
    /**
     * Get connection status property
     */
    public BooleanProperty isConnectedProperty() {
        return isConnected;
    }
    
    /**
     * Get connection status text property
     */
    public StringProperty connectionStatusProperty() {
        return connectionStatus;
    }
    
    /**
     * Check if currently connected
     */
    public boolean isConnected() {
        return isConnected.get();
    }
    
    /**
     * Get current status text
     */
    public String getConnectionStatus() {
        return connectionStatus.get();
    }
    
    /**
     * Get database service instance
     */
    public DatabaseService getDatabaseService() {
        return databaseService;
    }
    
    /**
     * Shutdown the connection controller
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            System.out.println("Stopping database connection monitoring...");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                    System.out.println("Forcing shutdown of database monitoring threads...");
                    scheduler.shutdownNow();
                }
                System.out.println("Database monitoring threads stopped successfully");
            } catch (InterruptedException e) {
                System.out.println("Interrupted while shutting down database monitoring");
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            monitoringStarted = false;
        }

        if (!connectionCheckExecutor.isShutdown()) {
            connectionCheckExecutor.shutdownNow();
        }
    }
}


