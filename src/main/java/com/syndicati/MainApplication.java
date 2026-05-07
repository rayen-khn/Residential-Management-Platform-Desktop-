package com.syndicati;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.scene.text.Font;
import com.syndicati.utils.security.AccessControlService;
import com.syndicati.controllers.log.ActivityLogController;
import com.syndicati.views.frontend.home.AdminDestinationChoiceView;
import com.syndicati.views.frontend.home.LandingPageView;
import com.syndicati.views.frontend.login.LoginView;
import com.syndicati.services.analytics.AnomalyScoringScheduler;
import com.syndicati.services.observability.LangfuseRuntimeService;
import com.syndicati.utils.theme.ThemeManager;
import com.syndicati.utils.navigation.NavigationManager;

/**
 * Main JavaFX Application - Syndicati desktop client
 */
public class MainApplication extends Application {
    private static final String GLOBAL_SCROLLBAR_CSS = "/styles/app-scrollbar.css";
    
    private static MainApplication instance;
    private Stage primaryStage;
    private LandingPageView landingPageView;
    private AdminDestinationChoiceView adminDestinationChoiceView;
    private LoginView loginView;
    private boolean isLoggedIn = false;
    private javafx.animation.Timeline loginChecker; // Keep reference to stop it later
    private String boldFontFamily = "Clash Grotesk"; // default name in case load resolves differently
    private String lightFontFamily = "Clash Grotesk"; // default name in case load resolves differently
    private boolean windowChromeListenerInstalled = false;
    private final ActivityLogController activityLogController = new ActivityLogController();
    private final AnomalyScoringScheduler anomalyScoringScheduler = new AnomalyScoringScheduler();
    private final LangfuseRuntimeService langfuseRuntimeService = LangfuseRuntimeService.getInstance();
    
    @Override
    public void start(Stage primaryStage) {
        instance = this;
        this.primaryStage = primaryStage;
        
        // Load custom fonts once (will be cached by JavaFX)
        loadCustomFonts();

        // Start the Sentiment AI Microservice in the background
        com.syndicati.services.forum.SentimentAnalysisService.startMicroservice();
        
        // Create the login view first
        loginView = new LoginView();
        loginView.setOnLoginSuccess(this::navigateToLandingPage);
        
        // Set up the scene with login view - dynamic sizing with min constraints
        Scene scene = new Scene(loginView.getRoot(), 1500, 800);
        applyGlobalStyles(scene);
        // Apply global font family to entire scene (use Light as default body font)
        if (scene.getRoot() != null) {
            appendRootStyle(scene.getRoot(), "-fx-font-family: '" + lightFontFamily + "';");
        }
        
        // Set up theme manager
        ThemeManager themeManager = ThemeManager.getInstance();
        themeManager.setScene(scene);

        installWindowChromeListener();
        applyRoundedShape(scene);
        
        // Start database connection monitoring
        com.syndicati.utils.database.ConnectionManager connectionManager = com.syndicati.utils.database.ConnectionManager.getInstance();
        connectionManager.startMonitoring();
        langfuseRuntimeService.start();
        anomalyScoringScheduler.start();

        // Log application startup event - first record anchors the Langfuse session.
        activityLogController.logPageView("app_startup", "Application Startup", java.util.Map.of(
            "source",  "main_application",
            "langfuse_enabled", String.valueOf(langfuseRuntimeService.isEnabled()),
            "diagnostics", langfuseRuntimeService.diagnosticSummary()
        ));
        
        // Add JVM shutdown hook as backup
        Runtime.getRuntime().addShutdownHook(
            Thread.ofPlatform()
                .name("Syndicati-ShutdownHook")
                .unstarted(() -> {
                    System.out.println("[SHUTDOWN] JVM Shutdown - Stopping all services...");
                    com.syndicati.services.mail.AsyncMailerService.shutdown();
                    com.syndicati.services.forum.SentimentAnalysisService.stopMicroservice();
                    connectionManager.shutdown();
                    langfuseRuntimeService.stop();
                    anomalyScoringScheduler.stop();
                    System.out.println("[SHUTDOWN] All services stopped in shutdown hook");
                })
        );
        
        // Configure the stage. Use solid black scene fill to avoid desktop bleed-through.
        primaryStage.setTitle("Syndicati - Login");

        // Ensure the application exits completely when the window is closed
        primaryStage.setOnCloseRequest(event -> {
            com.syndicati.services.forum.SentimentAnalysisService.stopMicroservice();
            javafx.application.Platform.exit();
            System.exit(0);
        });
        scene.setFill(Color.BLACK);
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        
        // Set minimum window size
        primaryStage.setMinWidth(1500);
        primaryStage.setMinHeight(800);
        
        // Show the stage first (needed for width/height to be set)
        primaryStage.show();
        
        // Center the window on screen after showing
        centerStageOnScreen(primaryStage);
        
        // Add corner resize functionality
        addResizeHandlers(primaryStage, scene);
        
        // Force rounded corners by applying shape to scene root after showing
        applyRoundedShape(scene);
        
        // Add shutdown hook to properly close database monitoring and async email service
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("[SHUTDOWN] Shutting down application...");
            // Log shutdown before stopping services so tracer is still live.
            activityLogController.logPageView("app_shutdown", "Application Shutdown", java.util.Map.of(
                "source", "close_request"
            ));
            // Shutdown email service thread pool first
            com.syndicati.services.mail.AsyncMailerService.shutdown();
            // Then shutdown database monitoring
            connectionManager.shutdown();
            langfuseRuntimeService.stop();
            anomalyScoringScheduler.stop();
            System.out.println("[SUCCESS] All services stopped");
            
            // Force exit JVM after a short delay to ensure cleanup
            Thread.ofVirtual().name("Syndicati-DelayedExit").start(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("[SHUTDOWN] Forcing JVM exit...");
                System.exit(0);
            });
        });
        
        System.out.println("[SUCCESS] Syndicati started!");
        System.out.println("[INFO] Login page loaded - use admin/admin to login");
        
        // Login success is now event-driven from LoginView for immediate navigation.
    }
    
    private void setupLoginMonitoring() {
        // Stop any existing login checker first
        if (loginChecker != null) {
            loginChecker.stop();
        }
        
        // Create a timeline to check for successful login
        loginChecker = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(100), e -> {
                if (loginView != null && loginView.isLoginSuccessful()) {
                    navigateToLandingPage();
                }
            })
        );
        loginChecker.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        loginChecker.play();
    }
    
    private void navigateToLandingPage() {
        if (isLoggedIn) return; // Prevent multiple navigations

        isLoggedIn = true;
        System.out.println("[INFO] Login successful. Navigating to landing page...");

        // Store current window size and position before switching
        double currentWidth = primaryStage.getWidth();
        double currentHeight = primaryStage.getHeight();
        double currentX = primaryStage.getX();
        double currentY = primaryStage.getY();
        boolean wasMaximized = primaryStage.isMaximized();

        // STOP the login monitoring animation FIRST
        if (loginChecker != null) {
            loginChecker.stop();
            loginChecker = null;
        }

        if (AccessControlService.canAccessAdminArea()) {
            showAdminDestinationChoice(currentWidth, currentHeight, currentX, currentY, wasMaximized);
            return;
        }

        activityLogController.logPageView("landing_page", "Landing Page", java.util.Map.of(
            "source", "login_success"
        ));
        showLandingPage(currentWidth, currentHeight, currentX, currentY, wasMaximized, false);
        System.out.println("[OK] Successfully navigated to landing page.");
    }

    private void showAdminDestinationChoice(
        double currentWidth,
        double currentHeight,
        double currentX,
        double currentY,
        boolean wasMaximized
    ) {
        if (loginView != null) {
            loginView.cleanup();
            loginView = null;
        }

        if (landingPageView != null) {
            landingPageView.cleanup();
            landingPageView = null;
        }

        adminDestinationChoiceView = new AdminDestinationChoiceView();
        adminDestinationChoiceView.setOnChooseHome(() -> {
            double w = primaryStage.getWidth();
            double h = primaryStage.getHeight();
            double x = primaryStage.getX();
            double y = primaryStage.getY();
            boolean max = primaryStage.isMaximized();
            showLandingPage(w, h, x, y, max, false);
        });
        adminDestinationChoiceView.setOnChooseDashboard(() -> {
            double w = primaryStage.getWidth();
            double h = primaryStage.getHeight();
            double x = primaryStage.getX();
            double y = primaryStage.getY();
            boolean max = primaryStage.isMaximized();
            showLandingPage(w, h, x, y, max, true);
        });

        Scene scene = new Scene(adminDestinationChoiceView.getRoot());
        scene.setFill(Color.BLACK);
        scene.getStylesheets().clear();
        applyGlobalStyles(scene);
        if (scene.getRoot() != null) {
            appendRootStyle(scene.getRoot(), "-fx-font-family: '" + lightFontFamily + "';");
        }

        primaryStage.setMinWidth(1500);
        primaryStage.setMinHeight(800);
        ThemeManager.getInstance().setScene(scene);
        primaryStage.setScene(scene);

        if (wasMaximized) {
            primaryStage.setMaximized(true);
        } else {
            primaryStage.setWidth(currentWidth);
            primaryStage.setHeight(currentHeight);
            primaryStage.setX(currentX);
            primaryStage.setY(currentY);
        }

        primaryStage.setTitle("Syndicati - Choose Destination");
        addResizeHandlers(primaryStage, scene);
        applyRoundedShape(scene);
        primaryStage.show();

        activityLogController.logPageView("admin_destination_choice", "Admin Destination Choice", java.util.Map.of(
            "source", "login_success"
        ));
    }

    private void showLandingPage(
        double currentWidth,
        double currentHeight,
        double currentX,
        double currentY,
        boolean wasMaximized,
        boolean goToDashboard
    ) {
        if (loginView != null) {
            loginView.cleanup();
            loginView = null;
        }

        if (adminDestinationChoiceView != null) {
            adminDestinationChoiceView.cleanup();
            adminDestinationChoiceView = null;
        }

        if (landingPageView != null) {
            landingPageView.cleanup();
            landingPageView = null;
        }

        landingPageView = new LandingPageView();

        NavigationManager navigationManager = NavigationManager.getInstance();
        navigationManager.setViews(landingPageView);

        Scene scene = new Scene(landingPageView.getRoot());
        scene.setFill(Color.BLACK); // Keep non-transparent app background
        scene.getStylesheets().clear(); // Clear any inherited styles
        applyGlobalStyles(scene);
        if (scene.getRoot() != null) {
            appendRootStyle(scene.getRoot(), "-fx-font-family: '" + lightFontFamily + "';");
        }

        primaryStage.setMinWidth(1500);
        primaryStage.setMinHeight(800);

        ThemeManager.getInstance().setScene(scene);
        primaryStage.setScene(scene);

        if (wasMaximized) {
            primaryStage.setMaximized(true);
        } else {
            primaryStage.setWidth(currentWidth);
            primaryStage.setHeight(currentHeight);
            primaryStage.setX(currentX);
            primaryStage.setY(currentY);
        }

        primaryStage.setTitle("Syndicati - Dashboard");

        addResizeHandlers(primaryStage, scene);
        applyRoundedShape(scene);
        primaryStage.show();

        activityLogController.logPageView(goToDashboard ? "admin_dashboard" : "landing_dashboard", "Dashboard", java.util.Map.of(
            "source", "scene_switch",
            "dashboard_mode", goToDashboard ? "admin" : "community"
        ));

        javafx.application.Platform.runLater(() -> {
            landingPageView.getRoot().layout();
            primaryStage.sizeToScene();
            javafx.application.Platform.runLater(() -> {
                landingPageView.getRoot().layout();
                primaryStage.sizeToScene();
                if (goToDashboard) {
                    landingPageView.enterDashboardMode();
                } else {
                    landingPageView.navigateToHome();
                }
            });
        });
    }

    private void loadCustomFonts() {
        try {
            // Load ClashGrotesk-Bold for titles
            java.io.InputStream boldStream = MainApplication.class.getResourceAsStream("/ClashGrotesk-Bold.otf");
            if (boldStream != null) {
                Font boldFont = Font.loadFont(boldStream, 14);
                if (boldFont != null) {
                    boldFontFamily = boldFont.getFamily();
                    System.out.println("[INFO] Loaded bold font: " + boldFont.getName() + " (family: " + boldFontFamily + ")");
                } else {
                    System.out.println("[WARN] Failed to load ClashGrotesk-Bold.otf font - using default family name.");
                }
                boldStream.close();
            } else {
                System.out.println("[WARN] ClashGrotesk-Bold.otf not found on classpath.");
            }
            
            // Load Archivo-Regular for body text (primary)
            java.io.InputStream regularPrimaryStream = MainApplication.class.getResourceAsStream("/Archivo-Regular.ttf");
            if (regularPrimaryStream != null) {
                Font regularPrimaryFont = Font.loadFont(regularPrimaryStream, 14);
                if (regularPrimaryFont != null) {
                    lightFontFamily = regularPrimaryFont.getFamily();
                    System.out.println("[INFO] Loaded body font (regular): " + regularPrimaryFont.getName() + " (family: " + lightFontFamily + ")");
                } else {
                    System.out.println("[WARN] Failed to load Archivo-Regular.ttf font - trying Archivo-Light.");
                }
                regularPrimaryStream.close();
            } else {
                System.out.println("[WARN] Archivo-Regular.ttf not found on classpath - trying Archivo-Light.");
            }

            // Fallback: Archivo-Regular
            if ("Clash Grotesk".equals(lightFontFamily)) {
                java.io.InputStream archivoRegularStream = MainApplication.class.getResourceAsStream("/Archivo-Regular.ttf");
                if (archivoRegularStream != null) {
                    Font archivoRegular = Font.loadFont(archivoRegularStream, 14);
                    if (archivoRegular != null) {
                        lightFontFamily = archivoRegular.getFamily();
                        System.out.println("[INFO] Loaded body font (regular): " + archivoRegular.getName() + " (family: " + lightFontFamily + ")");
                    } else {
                        System.out.println("[WARN] Failed to load Archivo-Regular.ttf font - trying Archivo-Light.");
                    }
                    archivoRegularStream.close();
                } else {
                    System.out.println("[WARN] Archivo-Regular.ttf not found on classpath - trying Archivo-Light.");
                }
            }

            // Fallback: Archivo-Light
            if ("Clash Grotesk".equals(lightFontFamily)) {
                java.io.InputStream archivoLightStream = MainApplication.class.getResourceAsStream("/Archivo-Light.ttf");
                if (archivoLightStream != null) {
                    Font archivoLight = Font.loadFont(archivoLightStream, 14);
                    if (archivoLight != null) {
                        lightFontFamily = archivoLight.getFamily();
                        System.out.println("[INFO] Loaded body font (light): " + archivoLight.getName() + " (family: " + lightFontFamily + ")");
                    } else {
                        System.out.println("[WARN] Failed to load Archivo-Light.ttf font - trying Clash fallback.");
                    }
                    archivoLightStream.close();
                } else {
                    System.out.println("[WARN] Archivo-Light.ttf not found on classpath - trying Clash fallback.");
                }
            }

            // Fallback: ClashGrotesk-Regular
            if ("Clash Grotesk".equals(lightFontFamily)) {
                java.io.InputStream regularStream = MainApplication.class.getResourceAsStream("/ClashGrotesk-Regular.ttf");
                if (regularStream != null) {
                    Font regularFont = Font.loadFont(regularStream, 14);
                    if (regularFont != null) {
                        lightFontFamily = regularFont.getFamily();
                        System.out.println("[INFO] Loaded regular font: " + regularFont.getName() + " (family: " + lightFontFamily + ")");
                    } else {
                        System.out.println("[WARN] Failed to load ClashGrotesk-Regular.ttf font - trying light font.");
                    }
                    regularStream.close();
                } else {
                    System.out.println("[WARN] ClashGrotesk-Regular.ttf not found on classpath - trying light font.");
                }
            }

            // Final fallback: old ClashGrotesk-Light
            if ("Clash Grotesk".equals(lightFontFamily)) {
                java.io.InputStream lightStream = MainApplication.class.getResourceAsStream("/ClashGrotesk-Light.otf");
                if (lightStream != null) {
                    Font lightFont = Font.loadFont(lightStream, 14);
                    if (lightFont != null) {
                        lightFontFamily = lightFont.getFamily();
                        System.out.println("[INFO] Loaded light fallback font: " + lightFont.getName() + " (family: " + lightFontFamily + ")");
                    } else {
                        System.out.println("[WARN] Failed to load ClashGrotesk-Light.otf font - using default family name.");
                    }
                    lightStream.close();
                } else {
                    System.out.println("[WARN] ClashGrotesk-Light.otf not found on classpath.");
                }
            }
        } catch (Exception ex) {
            System.out.println("[WARN] Error loading custom fonts: " + ex.getMessage());
        }
    }
    
    public String getBoldFontFamily() {
        return boldFontFamily;
    }
    
    public String getLightFontFamily() {
        return lightFontFamily;
    }
    
    public void logout() {
        System.out.println("[INFO] Logging out - returning to login page...");
                // Clear user session
        com.syndicati.utils.session.SessionManager.getInstance().clear();
                // Store current window size and position before switching
        double currentWidth = primaryStage.getWidth();
        double currentHeight = primaryStage.getHeight();
        double currentX = primaryStage.getX();
        double currentY = primaryStage.getY();
        boolean wasMaximized = primaryStage.isMaximized();
        
        // Reset login state
        isLoggedIn = false;
        
        // Create new login view
        loginView = new LoginView();
        loginView.setOnLoginSuccess(this::navigateToLandingPage);
        
        // Update the scene with solid black fill - don't set initial size
        Scene scene = new Scene(loginView.getRoot());
        scene.setFill(Color.BLACK); // Keep non-transparent app background
        scene.getStylesheets().clear(); // Avoid inherited styles that could reintroduce backgrounds
        applyGlobalStyles(scene);
        
        // Set minimum window size
        primaryStage.setMinWidth(1500);
        primaryStage.setMinHeight(800);
        
        ThemeManager.getInstance().setScene(scene);
        
        // Set scene first, THEN restore size
        primaryStage.setScene(scene);
        
        // Restore window size and position before showing
        if (wasMaximized) {
            primaryStage.setMaximized(true);
        } else {
            primaryStage.setWidth(currentWidth);
            primaryStage.setHeight(currentHeight);
            primaryStage.setX(currentX);
            primaryStage.setY(currentY);
        }
        
        primaryStage.setTitle("Syndicati - Login");
        
        // Add corner resize functionality for login page
        addResizeHandlers(primaryStage, scene);
        
        // Reapply rounded window clip for the login scene after logout
        applyRoundedShape(scene);
        
        // Show stage
        primaryStage.show();

        activityLogController.logPageView("login_page", "Login Page", java.util.Map.of(
            "source", "logout"
        ));
        
        // Avoid forcing additional size/layout passes here; media-backed backgrounds
        // are initialized asynchronously and can be disrupted by immediate re-scaling.
        
        // Clean up landing page view
        if (landingPageView != null) {
            landingPageView.cleanup();
            landingPageView = null;
        }
        
        System.out.println("[OK] Successfully returned to login page.");
    }
    
    public static MainApplication getInstance() {
        return instance;
    }

    private void appendRootStyle(javafx.scene.Parent root, String styleChunk) {
        String existing = root.getStyle();
        if (existing == null) {
            existing = "";
        }
        root.setStyle(existing + styleChunk);
    }

    private void applyGlobalStyles(Scene scene) {
        java.net.URL cssUrl = MainApplication.class.getResource(GLOBAL_SCROLLBAR_CSS);
        if (cssUrl != null) {
            String css = cssUrl.toExternalForm();
            if (!scene.getStylesheets().contains(css)) {
                scene.getStylesheets().add(css);
            }
        } else {
            System.out.println("[WARN] Global stylesheet not found: " + GLOBAL_SCROLLBAR_CSS);
        }
    }
    
    private void centerStageOnScreen(Stage stage) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
        stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
    }
    
    private void applyRoundedShape(Scene scene) {
        updateWindowClip(scene, primaryStage != null && primaryStage.isMaximized());
    }

    private void installWindowChromeListener() {
        if (windowChromeListenerInstalled || primaryStage == null) {
            return;
        }

        primaryStage.maximizedProperty().addListener((observable, oldValue, maximized) -> {
            updateWindowClip(primaryStage.getScene(), maximized);
        });
        windowChromeListenerInstalled = true;
    }

    private void updateWindowClip(Scene scene, boolean maximized) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }

        if (maximized) {
            scene.getRoot().setClip(null);
            return;
        }

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        clip.widthProperty().bind(scene.widthProperty());
        clip.heightProperty().bind(scene.heightProperty());
        scene.getRoot().setClip(clip);
    }
    
    private void addResizeHandlers(Stage stage, Scene scene) {
        final double RESIZE_MARGIN = 8; // Pixel margin for resize detection
        final double WINDOW_BAR_HEIGHT = 40; // Height of custom window bar to exclude from resize
        final double[] resizeStartX = {0};
        final double[] resizeStartY = {0};
        final double[] resizeStartWidth = {0};
        final double[] resizeStartHeight = {0};
        final String[] resizeDirection = {""};
        
        scene.setOnMouseMoved(e -> {
            if (stage.isMaximized()) return;
            
            double mouseX = e.getSceneX();
            double mouseY = e.getSceneY();
            double width = scene.getWidth();
            double height = scene.getHeight();
            
            // Skip if mouse is over window bar (top 40px center area) to allow dragging
            if (mouseY < WINDOW_BAR_HEIGHT && mouseX > RESIZE_MARGIN && mouseX < width - RESIZE_MARGIN) {
                scene.setCursor(javafx.scene.Cursor.DEFAULT);
                return;
            }
            
            // Detect which edge/corner
            boolean left = mouseX < RESIZE_MARGIN;
            boolean right = mouseX > width - RESIZE_MARGIN;
            boolean top = mouseY < RESIZE_MARGIN;
            boolean bottom = mouseY > height - RESIZE_MARGIN;
            
            // Set cursor based on position
            if (top && left) {
                scene.setCursor(javafx.scene.Cursor.NW_RESIZE);
            } else if (top && right) {
                scene.setCursor(javafx.scene.Cursor.NE_RESIZE);
            } else if (bottom && left) {
                scene.setCursor(javafx.scene.Cursor.SW_RESIZE);
            } else if (bottom && right) {
                scene.setCursor(javafx.scene.Cursor.SE_RESIZE);
            } else if (top) {
                scene.setCursor(javafx.scene.Cursor.N_RESIZE);
            } else if (bottom) {
                scene.setCursor(javafx.scene.Cursor.S_RESIZE);
            } else if (left) {
                scene.setCursor(javafx.scene.Cursor.W_RESIZE);
            } else if (right) {
                scene.setCursor(javafx.scene.Cursor.E_RESIZE);
            } else {
                scene.setCursor(javafx.scene.Cursor.DEFAULT);
            }
        });
        
        scene.setOnMousePressed(e -> {
            if (stage.isMaximized()) return;
            
            double mouseX = e.getSceneX();
            double mouseY = e.getSceneY();
            double width = scene.getWidth();
            double height = scene.getHeight();
            
            // Skip if mouse is over window bar (top 40px center area)
            if (mouseY < WINDOW_BAR_HEIGHT && mouseX > RESIZE_MARGIN && mouseX < width - RESIZE_MARGIN) {
                resizeDirection[0] = "";
                return;
            }
            
            resizeStartX[0] = e.getScreenX();
            resizeStartY[0] = e.getScreenY();
            resizeStartWidth[0] = stage.getWidth();
            resizeStartHeight[0] = stage.getHeight();
            
            // Detect which edge/corner
            boolean left = mouseX < RESIZE_MARGIN;
            boolean right = mouseX > width - RESIZE_MARGIN;
            boolean top = mouseY < RESIZE_MARGIN;
            boolean bottom = mouseY > height - RESIZE_MARGIN;
            
            if (top && left) {
                resizeDirection[0] = "NW";
            } else if (top && right) {
                resizeDirection[0] = "NE";
            } else if (bottom && left) {
                resizeDirection[0] = "SW";
            } else if (bottom && right) {
                resizeDirection[0] = "SE";
            } else if (top) {
                resizeDirection[0] = "N";
            } else if (bottom) {
                resizeDirection[0] = "S";
            } else if (left) {
                resizeDirection[0] = "W";
            } else if (right) {
                resizeDirection[0] = "E";
            } else {
                resizeDirection[0] = "";
            }
        });
        
        scene.setOnMouseDragged(e -> {
            if (stage.isMaximized() || resizeDirection[0].isEmpty()) return;
            
            double deltaX = e.getScreenX() - resizeStartX[0];
            double deltaY = e.getScreenY() - resizeStartY[0];
            
            String dir = resizeDirection[0];
            
            // Handle horizontal resizing
            if (dir.contains("W")) {
                double newWidth = resizeStartWidth[0] - deltaX;
                if (newWidth >= stage.getMinWidth()) {
                    stage.setX(e.getScreenX());
                    stage.setWidth(newWidth);
                }
            } else if (dir.contains("E")) {
                double newWidth = resizeStartWidth[0] + deltaX;
                if (newWidth >= stage.getMinWidth()) {
                    stage.setWidth(newWidth);
                }
            }
            
            // Handle vertical resizing
            if (dir.contains("N")) {
                double newHeight = resizeStartHeight[0] - deltaY;
                if (newHeight >= stage.getMinHeight()) {
                    stage.setY(e.getScreenY());
                    stage.setHeight(newHeight);
                }
            } else if (dir.contains("S")) {
                double newHeight = resizeStartHeight[0] + deltaY;
                if (newHeight >= stage.getMinHeight()) {
                    stage.setHeight(newHeight);
                }
            }
        });
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}


