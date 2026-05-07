package com.syndicati.utils.database;

import com.syndicati.controllers.ConnectionController;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

/**
 * Database Connection Manager - Handles database connectivity status
 * Now uses ConnectionController for proper service-based approach
 */
public class ConnectionManager {
    
    private static ConnectionManager instance;
    private final ConnectionController connectionController;
    
    private ConnectionManager() {
        this.connectionController = ConnectionController.getInstance();
    }
    
    public static ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }
    
    /**
     * Get connection status property
     */
    public BooleanProperty isConnectedProperty() {
        return connectionController.isConnectedProperty();
    }
    
    /**
     * Get connection status text property
     */
    public StringProperty connectionStatusProperty() {
        return connectionController.connectionStatusProperty();
    }
    
    /**
     * Check if currently connected
     */
    public boolean isConnected() {
        return connectionController.isConnected();
    }
    
    /**
     * Get current status text
     */
    public String getConnectionStatus() {
        return connectionController.getConnectionStatus();
    }
    
    /**
     * Force connection check
     */
    public void forceConnectionCheck() {
        connectionController.forceConnectionCheck();
    }
    
    /**
     * Start connection monitoring (called when application launches)
     */
    public void startMonitoring() {
        connectionController.startMonitoring();
    }
    
    /**
     * Get connection controller
     */
    public ConnectionController getConnectionController() {
        return connectionController;
    }
    
    /**
     * Shutdown the connection manager
     */
    public void shutdown() {
        connectionController.shutdown();
    }
}


