package com.syndicati.interfaces;

import javafx.scene.Parent;

/**
 * Interface for all view components
 */
public interface ViewInterface {
    
    /**
     * Get the root node of the view
     * @return Parent node containing the view
     */
    Parent getRoot();
    
    /**
     * Cleanup resources when view is destroyed
     */
    void cleanup();
}



