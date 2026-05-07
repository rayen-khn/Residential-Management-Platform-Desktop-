package com.syndicati.components.shared;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.geometry.Pos;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

/**
 * Video Background Component - Login video only with image fallback
 */
public class ImageBackground {
    private static final double LOGIN_VIDEO_WARMUP_SECONDS = 2.5;
    
    private final StackPane root;
    private MediaView backgroundMediaView;
    private ImageView backgroundImageView;
    private MediaPlayer loginPlayer;
    private Image fallbackImage;
    private boolean isVideoMode = true;
    private boolean loginVideoAttached = false;
    private boolean loginVideoWarmupComplete = false;
    
    public ImageBackground() {
        this.root = new StackPane();
        setupLayout();
        loadMedia();
        updateBackground();
    }
    
    private void setupLayout() {
        backgroundMediaView = new MediaView();
        backgroundMediaView.setPreserveRatio(false);
        backgroundMediaView.setSmooth(true);
        
        backgroundImageView = new ImageView();
        backgroundImageView.setPreserveRatio(false);
        backgroundImageView.setSmooth(true);
        
        root.setMouseTransparent(true);
        root.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        root.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        
        StackPane.setAlignment(backgroundMediaView, Pos.CENTER);
        StackPane.setAlignment(backgroundImageView, Pos.CENTER);
        
        backgroundMediaView.fitWidthProperty().bind(root.widthProperty());
        backgroundMediaView.fitHeightProperty().bind(root.heightProperty());
        backgroundImageView.fitWidthProperty().bind(root.widthProperty());
        backgroundImageView.fitHeightProperty().bind(root.heightProperty());
        
        root.getChildren().addAll(backgroundImageView, backgroundMediaView);
    }
    
    private void loadMedia() {
        try {
            System.out.println("Loading login video...");
            loadLoginVideo();
            System.out.println("Loading fallback image...");
            loadFallbackImage();
            System.out.println("Media loading completed!");
        } catch (Exception e) {
            System.err.println("Error loading media: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadLoginVideo() {
        try {
            System.out.println("Loading login.mp4...");
            Media loginMedia = new Media(getClass().getResource("/login.mp4").toString());
            loginPlayer = new MediaPlayer(loginMedia);
            loginPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            loginPlayer.setMute(true);
            loginPlayer.setAutoPlay(false);
            loginPlayer.setOnError(() -> {
                System.err.println("Media error: " + loginPlayer.getError());
                updateImageBackground();
            });
            loginPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (newTime != null && newTime.toSeconds() >= LOGIN_VIDEO_WARMUP_SECONDS) {
                    revealWarmedVideo();
                }
            });
            backgroundMediaView.setMediaPlayer(loginPlayer);
            loginVideoAttached = true;
            System.out.println("Login video loaded successfully");
        } catch (Exception e) {
            System.err.println("Failed to load login.mp4: " + e.getMessage());
            loginPlayer = null;
            isVideoMode = false;
        }
    }
    
    private void loadFallbackImage() {
        try {
            System.out.println("Loading fallback image...");
            fallbackImage = new Image(getClass().getResourceAsStream("/darkmode.jpg"));
            if (fallbackImage != null) {
                System.out.println("Fallback image loaded successfully");
            } else {
                System.out.println("Fallback image failed to load");
            }
        } catch (Exception e) {
            System.err.println("Error loading fallback image: " + e.getMessage());
        }
    }
    
    private void updateBackground() {
        if (backgroundMediaView == null && backgroundImageView == null) {
            System.out.println("Both media and image views are null");
            return;
        }
        updateImageBackground();
    }
    
    private void updateImageBackground() {
        if (fallbackImage != null) {
            System.out.println("Setting fallback image background");
            backgroundImageView.setImage(fallbackImage);
            backgroundImageView.setVisible(true);
            backgroundMediaView.setVisible(false);
        } else {
            System.out.println("Fallback image is null");
            backgroundImageView.setVisible(true);
            backgroundMediaView.setVisible(false);
        }
    }
    
    public void updateTheme() {
        System.out.println("updateTheme() called - login video only");
    }
    
    public void setLoginMode() {
        System.out.println("Setting login mode");
        if (isVideoMode && loginPlayer != null) {
            try {
                if (!loginVideoAttached) {
                    backgroundMediaView.setMediaPlayer(loginPlayer);
                    loginVideoAttached = true;
                }
                System.out.println("Preparing login video");
                playWhenReady(loginPlayer);
            } catch (Exception e) {
                System.err.println("Error playing login video: " + e.getMessage());
                updateImageBackground();
            }
        } else {
            System.out.println("Login video not available, using fallback");
            updateImageBackground();
        }
    }

    private void playWhenReady(MediaPlayer player) {
        if (player == null) {
            updateImageBackground();
            return;
        }
        try {
            if (backgroundMediaView.getMediaPlayer() != player) {
                backgroundMediaView.setMediaPlayer(player);
            }

            MediaPlayer.Status status = player.getStatus();
            if (status != MediaPlayer.Status.PLAYING) {
                player.play();
            }

            if (loginVideoWarmupComplete) {
                backgroundMediaView.setVisible(true);
                backgroundImageView.setVisible(false);
                System.out.println("Video playback started successfully");
                return;
            }

            // Keep the fallback image visible while the media decoder warms up.
            backgroundMediaView.setVisible(false);
            backgroundImageView.setVisible(true);

            // Fast machines may already be past warmup; reveal immediately in that case.
            if (player.getCurrentTime() != null && player.getCurrentTime().toSeconds() >= LOGIN_VIDEO_WARMUP_SECONDS) {
                revealWarmedVideo();
            }
        } catch (Exception ex) {
            System.err.println("Failed to start playback: " + ex.getMessage());
            updateImageBackground();
        }
    }

    private void revealWarmedVideo() {
        if (loginVideoWarmupComplete) {
            return;
        }

        try {
            backgroundMediaView.setVisible(true);
            backgroundImageView.setVisible(false);
            loginVideoWarmupComplete = true;
            System.out.println("Video playback warmed up and revealed successfully");
        } catch (Exception ex) {
            System.err.println("Failed to reveal warmed video: " + ex.getMessage());
            updateImageBackground();
        }
    }
    
    public StackPane getRoot() {
        return root;
    }
    
    public void cleanup() {
        System.out.println("Cleaning up video resources");
        try {
            if (loginPlayer != null) {
                loginPlayer.stop();
                loginPlayer.dispose();
                loginPlayer = null;
            }
            if (backgroundMediaView != null) {
                backgroundMediaView.setMediaPlayer(null);
                backgroundMediaView.setVisible(false);
            }
            if (backgroundImageView != null) {
                backgroundImageView.setImage(null);
                backgroundImageView.setVisible(false);
            }
            loginVideoAttached = false;
            loginVideoWarmupComplete = false;
            System.out.println("Video resources cleaned up successfully");
        } catch (Exception e) {
            System.err.println("Error during media cleanup: " + e.getMessage());
        }
    }
}
