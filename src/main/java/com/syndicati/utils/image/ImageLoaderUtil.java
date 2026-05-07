package com.syndicati.utils.image;

import javafx.scene.image.Image;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for loading images from various sources including the uploads folder.
 * Centralizes image loading logic with async loading and in-memory cache.
 */
public class ImageLoaderUtil {

    private static final Map<String, Image> imageCache = new LinkedHashMap<String, Image>(16, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 64; // Keep max 64 images in memory
        }
    };

    /**
     * Load an image from a relative or absolute path, URL, or resource.
     * Supports:
     * - Relative paths (resolved from project root): uploads/profile_images/avatar.jpg
     * - Absolute paths: C:\path\to\image.jpg
     * - URLs: http://example.com/image.jpg, https://example.com/image.jpg
     * - Resources: /images/default.jpg
     *
     * Uses in-memory cache to avoid reloading the same image multiple times.
     *
     * @param imagePath Path to the image
     * @param async Whether to load asynchronously (recommended: true for UI thread safety)
     * @return Image object or null if image cannot be loaded
     */
    public static Image loadImage(String imagePath, boolean async) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        
        // Check cache first
        if (imageCache.containsKey(imagePath)) {
            return imageCache.get(imagePath);
        }

        try {
            String candidate = imagePath.trim();
            Image image = null;

            // Check if it's a URL
            if (isUrl(candidate)) {
                image = new Image(candidate, async);
            } else {
                // Try loading from file system (relative or absolute path)
                Path path = Paths.get(candidate);
                if (!path.isAbsolute()) {
                    path = Paths.get(System.getProperty("user.dir")).resolve(candidate);
                }

                if (Files.exists(path) && Files.isRegularFile(path)) {
                    image = new Image(path.toUri().toString(), async);
                } else {
                    // Try loading as resource
                    String resourcePath = candidate.startsWith("/") ? candidate : "/" + candidate;
                    java.net.URL resource = ImageLoaderUtil.class.getResource(resourcePath);
                    if (resource != null) {
                        image = new Image(resource.toExternalForm(), async);
                    }
                }
            }
            
            if (image != null && !image.isError()) {
                imageCache.put(imagePath, image);
                return image;
            }

        } catch (Exception e) {
            System.err.println("ImageLoaderUtil: Failed to load image from " + imagePath + ": " + e.getMessage());
        }

        return null;
    }

    /**
     * Load an image asynchronously (non-blocking, recommended for UI).
     */
    public static Image loadImage(String imagePath) {
        return loadImage(imagePath, true);
    }

    /**
     * Load a profile avatar image from the uploads/profile_images folder.
     * Handles both existing paths and new uploads folder structure.
     *
     * @param imagePath Path stored in database (e.g., "uploads/profile_images/avatar_123.jpg" or "profile_images/avatar_123.jpg")
     * @param async Whether to load asynchronously
     * @return Image object or null if image cannot be loaded
     */
    public static Image loadProfileAvatar(String imagePath, boolean async) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        String path = imagePath.trim();

        // Handle old paths that don't have 'uploads/' prefix
        if (!path.startsWith("uploads/") && !path.startsWith("uploads\\") && !isUrl(path)) {
            path = "uploads/" + path;
        }

        return loadImage(path, async);
    }

    /**
     * Load a profile avatar image asynchronously (non-blocking).
     */
    public static Image loadProfileAvatar(String imagePath) {
        return loadProfileAvatar(imagePath, true);
    }
    
    /**
     * Clear the image cache (call sparingly, e.g., on logout).
     */
    public static void clearCache() {
        imageCache.clear();
    }

    /**
     * Check if a path is a valid URL.
     */
    private static boolean isUrl(String value) {
        if (value == null) {
            return false;
        }
        String v = value.toLowerCase();
        return v.startsWith("http://")
            || v.startsWith("https://")
            || v.startsWith("file:")
            || v.startsWith("data:")
            || v.startsWith("jar:");
    }
}

