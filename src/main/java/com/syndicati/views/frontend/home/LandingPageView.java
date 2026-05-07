package com.syndicati.views.frontend.home;

import javafx.scene.layout.*;
import javafx.beans.binding.Bindings;
import javafx.scene.control.ScrollPane;
import javafx.event.ActionEvent;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import com.syndicati.components.shared.DynamicHeader;
import com.syndicati.components.shared.DynamicFooter;
import com.syndicati.components.home.HomeContent;
 
import com.syndicati.interfaces.ViewInterface;
import com.syndicati.utils.theme.ThemeManager;
import com.syndicati.utils.navigation.NavigationManager;
import com.syndicati.views.backend.dashboard.DashboardView;

/**
 * Landing Page View - Main container with dynamic island header and footer
 */
public class LandingPageView implements ViewInterface {
    
    private final StackPane root;
    private final DynamicHeader header;
    private final DynamicFooter footer;
    private final Runnable accentRefreshListener;
    private VBox mainContent; // Added to allow content replacement
    private VBox darkPanel; // Store reference for theme updates
    private HomeContent homeContent;
    private VBox contentRow; // Stored so we can swap it out for dashboard mode
    private String currentPageName = "home";
    
    
    
    public LandingPageView() {
        this.root = new StackPane();
        this.header = new DynamicHeader();
        this.footer = new DynamicFooter();
        this.accentRefreshListener = () -> {
            applyThemeStyling();
            refreshVisiblePageForAccent();
            header.refreshTheme();
            footer.refreshTheme();
        };
        
        setupLayout();
        ThemeManager.getInstance().addAccentChangeListener(accentRefreshListener);
        
        // Pass the main container reference to header for sub-menus
        header.setMainContainer(root);
        
        // Set up background update callback for theme changes
        header.setBackgroundUpdateCallback(() -> {
            applyThemeStyling();
            footer.refreshTheme();
        });
    }
    
    private void setupLayout() {
        // Set root to completely transparent - no background at all
        root.setStyle(rootBaseStyle());
        root.setBackground(null); // Force remove any background

        // Main dark rounded panel - use VBox for vertical stacking with proper rounded corners
        darkPanel = new VBox();
        darkPanel.setSpacing(0);
        darkPanel.setPadding(new Insets(0));
        darkPanel.setAlignment(Pos.TOP_LEFT);
        
        // Use ThemeManager for dynamic background color based on theme
        ThemeManager themeManager = ThemeManager.getInstance();
        String backgroundColor = themeManager.isDarkMode()
            ? "radial-gradient(focus-angle 28deg, focus-distance 24%, center 14% 8%, radius 135%, " + themeManager.toRgba(themeManager.getAccentHex(), 0.12) + " 0%, rgba(6,6,10,0.97) 62%, rgba(3,3,5,0.99) 100%), linear-gradient(to bottom right, rgba(18,18,26,0.92), rgba(10,10,14,0.94) 52%, rgba(4,4,6,0.97) 100%)"
            : "linear-gradient(to bottom right, #f8fbff, #eef2f8 55%, #edf6ff 100%)";
        darkPanel.setStyle(
            "-fx-background-color: " + backgroundColor + ";" +
            "-fx-border-color: " + (themeManager.isDarkMode() ? themeManager.toRgba(themeManager.getAccentHex(), 0.26) : "rgba(15,23,42,0.14)") + ";" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 0;" // Let the scene clip handle corners
        );

        // Fill entire window - the clip will handle rounded corners
        StackPane.setAlignment(darkPanel, Pos.TOP_LEFT);
        StackPane.setMargin(darkPanel, new Insets(0));
        darkPanel.maxWidthProperty().bind(root.widthProperty());
        darkPanel.prefWidthProperty().bind(darkPanel.maxWidthProperty());
        darkPanel.maxHeightProperty().bind(root.heightProperty());
        darkPanel.prefHeightProperty().bind(darkPanel.maxHeightProperty());

        // No clip on darkPanel - let the scene root handle clipping
        // This ensures true rounded corners without rectangular artifacts

        // Create main content area inside panel
        mainContent = new VBox();
        mainContent.setSpacing(16);
        mainContent.setAlignment(Pos.TOP_LEFT);
        // Reserve space for the floating header so content starts below it without a dedicated header strip.
        mainContent.setPadding(new Insets(102, 10, 10, 10));

        // Build and show home content sections
        homeContent = new HomeContent();
        mainContent.getChildren().add(homeContent.getRoot());

        // Wrap mainContent + footer together so the footer scrolls with the page content
        // (footer appears at the end of the content, not pinned to the window bottom)
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox pageWrapper = new VBox();
        pageWrapper.setFillWidth(true);
        // Bind max width to the scroll pane viewport so nothing overflows horizontally
        pageWrapper.maxWidthProperty().bind(scrollPane.widthProperty());
        pageWrapper.getChildren().add(mainContent);

        // Footer container centred within the scroll area with breathing room
        VBox footerContainer = new VBox();
        footerContainer.setAlignment(Pos.CENTER);
        footerContainer.setPadding(new Insets(20, 0, 24, 0));
        footerContainer.getChildren().add(footer.getRoot());
        pageWrapper.getChildren().add(footerContainer);

        // Attach wrapped content to the already-configured scroll pane
        scrollPane.setContent(pageWrapper);

        // Create window bar that spans full width
        HBox windowBar = createWindowBar();
        // Ensure window bar is on top and not blocked by shadows from content below
        windowBar.setPickOnBounds(true);
        windowBar.setMouseTransparent(false);
        
        // Content row: main column with header + scrollable content (footer now lives inside scroll)
        contentRow = new VBox();
        contentRow.setSpacing(6);
        contentRow.setAlignment(Pos.TOP_LEFT);
        contentRow.setPadding(new Insets(48, 12, 12, 12)); // Add 48px top padding for window bar space
        contentRow.setFillWidth(true);
        StackPane floatingHeaderLayer = new StackPane(scrollPane, header.getRoot());
        floatingHeaderLayer.setAlignment(Pos.TOP_CENTER);
        StackPane.setAlignment(header.getRoot(), Pos.TOP_CENTER);
        StackPane.setMargin(header.getRoot(), new Insets(0, 0, 0, 0));

        contentRow.getChildren().add(floatingHeaderLayer);
        VBox.setVgrow(floatingHeaderLayer, Priority.ALWAYS);
        VBox.setVgrow(contentRow, Priority.ALWAYS);
        
        // Add contentRow to darkPanel (no window bar here)
        darkPanel.getChildren().add(contentRow);
        
        // Add darkPanel to root first
        root.getChildren().add(darkPanel);
        
        // Add window bar as a separate layer on top - this ensures it's above all shadows
        root.getChildren().add(windowBar);
        StackPane.setAlignment(windowBar, Pos.TOP_LEFT);

        root.addEventFilter(ActionEvent.ACTION, e -> NavigationManager.getInstance().awardInteractionXp(1));
    }
    
    public StackPane getRoot() {
        return root;
    }
    
    public VBox getMainContent() {
        return mainContent;
    }
    
    public DynamicHeader getHeader() {
        return header;
    }

    public String getCurrentPageName() {
        return currentPageName;
    }
    
    public void navigateToProfile() {
        currentPageName = "profile";
        // Clear existing content and show profile page
        mainContent.getChildren().clear();
        mainContent.getChildren().add(NavigationManager.getInstance().getPage("profile"));
    }
    
    public void navigateToHome() {
        currentPageName = "home";
        // Clear existing content and restore original landing page content
        mainContent.getChildren().clear();
        homeContent = new HomeContent();
        mainContent.getChildren().add(homeContent.getRoot());
    }
    
    public void navigateToDashboard() {
        enterDashboardMode();
    }

    /** Swap out the normal header+content with the full admin dashboard layout. */
    public void enterDashboardMode() {
        currentPageName = "dashboard";
        DashboardView dv = NavigationManager.getInstance().getDashboardView();
        dv.setExitCallback(this::exitDashboardMode);
        HBox adminRoot = dv.getRoot();
        adminRoot.setMaxWidth(Double.MAX_VALUE);
        adminRoot.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(adminRoot, Priority.ALWAYS);
        darkPanel.getChildren().clear();
        darkPanel.getChildren().add(adminRoot);
    }

    /** Restore the normal island header + content area, go back to home. */
    public void exitDashboardMode() {
        darkPanel.getChildren().clear();
        darkPanel.getChildren().add(contentRow);
        navigateToHome();
    }
    
    public void navigateToSettings() {
        currentPageName = "settings";
        // Clear existing content and show settings page
        mainContent.getChildren().clear();
        mainContent.getChildren().add(NavigationManager.getInstance().getPage("settings"));
    }

    public void navigateToPage(String pageName) {
        String normalizedPage = pageName == null ? "home" : pageName.toLowerCase();
        currentPageName = normalizedPage;
        if ("home".equalsIgnoreCase(pageName)) {
            navigateToHome();
            return;
        }
        if ("dashboard".equalsIgnoreCase(pageName)) {
            enterDashboardMode();
            return;
        }
        mainContent.getChildren().clear();
        mainContent.getChildren().add(NavigationManager.getInstance().getPage(pageName));
    }
    
    private void applyThemeStyling() {
        // Keep root completely transparent for rounded corners
        root.setStyle(rootBaseStyle());
        root.setBackground(null); // Force remove any background
        
        // Update darkPanel background color based on current theme
        if (darkPanel != null) {
            ThemeManager themeManager = ThemeManager.getInstance();
            String backgroundColor = themeManager.isDarkMode()
                ? "radial-gradient(focus-angle 28deg, focus-distance 24%, center 14% 8%, radius 135%, " + themeManager.toRgba(themeManager.getAccentHex(), 0.12) + " 0%, rgba(6,6,10,0.97) 62%, rgba(3,3,5,0.99) 100%), linear-gradient(to bottom right, rgba(18,18,26,0.92), rgba(10,10,14,0.94) 52%, rgba(4,4,6,0.97) 100%)"
                : "linear-gradient(to bottom right, #f8fbff, #eef2f8 55%, #edf6ff 100%)";
            darkPanel.setStyle(
                "-fx-background-color: " + backgroundColor + ";" +
                "-fx-border-color: " + (themeManager.isDarkMode() ? themeManager.toRgba(themeManager.getAccentHex(), 0.26) : "rgba(15,23,42,0.14)") + ";" +
                "-fx-border-width: 1px;" +
                "-fx-background-radius: 0;"
            );
        }
    }

    private String rootBaseStyle() {
        ThemeManager tm = ThemeManager.getInstance();
        return "-fx-background-color: transparent; -fx-background: transparent;" + tm.getScrollbarVariableStyle();
    }

    private void refreshHomeContentIfVisible() {
        if (mainContent == null || homeContent == null) {
            return;
        }
        if (mainContent.getChildren().contains(homeContent.getRoot())) {
            homeContent = new HomeContent();
            mainContent.getChildren().setAll(homeContent.getRoot());
        }
    }

    private void refreshVisiblePageForAccent() {
        NavigationManager navigation = NavigationManager.getInstance();
        navigation.rebuildThemeSensitiveViews();

        if ("home".equals(currentPageName)) {
            refreshHomeContentIfVisible();
            return;
        }

        if ("dashboard".equals(currentPageName)) {
            DashboardView dv = navigation.getDashboardView();
            dv.setExitCallback(this::exitDashboardMode);
            HBox adminRoot = dv.getRoot();
            adminRoot.setMaxWidth(Double.MAX_VALUE);
            adminRoot.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(adminRoot, Priority.ALWAYS);
            darkPanel.getChildren().setAll(adminRoot);
            return;
        }

        if (mainContent != null && darkPanel.getChildren().contains(contentRow)) {
            mainContent.getChildren().setAll(navigation.getPage(currentPageName));
        }
    }
    
    public void cleanup() {
        // Cleanup resources if needed
        ThemeManager.getInstance().removeAccentChangeListener(accentRefreshListener);
        if (header != null) header.cleanup();
        if (footer != null) footer.cleanup();
        
    }

    // Custom window bar with drag/min/restore/close - MacOS style
    private HBox createWindowBar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setSpacing(8);
        bar.setPadding(new Insets(4, 16, 4, 16)); // Reduced vertical padding from 12,8 to 4,4 for better button centering
        bar.setPrefHeight(40);
        bar.setMinHeight(40);
        bar.setMaxHeight(40);
        bar.setStyle("-fx-background-color: transparent; -fx-background-radius: 0;");
        // Ensure bar can receive all mouse events without clipping
        bar.setPickOnBounds(false); // Changed from true - allows clicks to pass through empty space
        bar.setMouseTransparent(false);

        Region dragRegion = new Region();
        HBox.setHgrow(dragRegion, Priority.ALWAYS);
        dragRegion.setMinHeight(40);
        dragRegion.setPrefHeight(40);
        dragRegion.setStyle("-fx-cursor: move;");

        // macOS-style circular window control buttons
        javafx.scene.control.Button btnMin = new javafx.scene.control.Button("");
        javafx.scene.control.Button btnMax = new javafx.scene.control.Button("");
        javafx.scene.control.Button btnClose = new javafx.scene.control.Button("");

        styleMacOSButton(btnMin, "#febc2e", "-");
        styleMacOSButton(btnMax, "#28c840", "+");
        styleMacOSButton(btnClose, "#ff5f57", "x");

        HBox buttonContainer = new HBox(8);
        buttonContainer.setAlignment(Pos.CENTER_LEFT);
        buttonContainer.setPadding(new Insets(0));
        buttonContainer.getChildren().addAll(btnMin, btnMax, btnClose);
        buttonContainer.setPickOnBounds(false);

        btnMin.setOnAction(e -> {
            javafx.stage.Stage stage = (javafx.stage.Stage) root.getScene().getWindow();
            stage.setIconified(true);
        });
        btnMax.setOnAction(e -> {
            javafx.stage.Stage stage = (javafx.stage.Stage) root.getScene().getWindow();
            stage.setMaximized(!stage.isMaximized());
        });
        btnClose.setOnAction(e -> {
            javafx.stage.Stage stage = (javafx.stage.Stage) root.getScene().getWindow();
            stage.close();
        });

        final double[] dragOffset = new double[2];
        // Restore classic working layout: buttons on the right, drag region fills left
        dragRegion.setMouseTransparent(false);
        buttonContainer.setMouseTransparent(false);
        dragRegion.setPickOnBounds(true);
        // Critical: Allow button container to NOT clip bounds so full button area is clickable
        buttonContainer.setPickOnBounds(false);

        // Standard drag logic (no event target checks)
        dragRegion.setOnMousePressed(e -> {
            javafx.stage.Stage stage = (javafx.stage.Stage) root.getScene().getWindow();
            dragOffset[0] = e.getScreenX() - stage.getX();
            dragOffset[1] = e.getScreenY() - stage.getY();
        });
        dragRegion.setOnMouseDragged(e -> {
            javafx.stage.Stage stage = (javafx.stage.Stage) root.getScene().getWindow();
            if (!stage.isMaximized()) {
                stage.setX(e.getScreenX() - dragOffset[0]);
                stage.setY(e.getScreenY() - dragOffset[1]);
            }
        });

        // --- CLEANED UP: Only add children and set up handlers ONCE ---
        // Add dragRegion and buttonContainer only ONCE, in the correct order
        bar.getChildren().addAll(dragRegion, buttonContainer);
        HBox.setHgrow(dragRegion, Priority.ALWAYS);
        buttonContainer.setAlignment(Pos.CENTER_RIGHT);
        return bar;
    }

    private void styleMacOSButton(javafx.scene.control.Button btn, String color, String symbol) {
        // Create circular buttons like macOS
        btn.setMinSize(12, 12);
        btn.setPrefSize(12, 12);
        btn.setMaxSize(12, 12);
        btn.setFocusTraversable(false);
        btn.setPickOnBounds(true);
        btn.setMouseTransparent(false);
        
        // macOS style: circular with solid color
        btn.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-background-radius: 6px;" + // Half of 12px for perfect circle
            "-fx-border-color: rgba(0,0,0,0.2);" +
            "-fx-border-width: 0.5px;" +
            "-fx-border-radius: 6px;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 8px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: transparent;" + // Hide symbol by default
            "-fx-padding: 0;"
        );
        
        // Store the symbol for hover effect
        final String btnSymbol = symbol;
        
        // Show symbol and darken on hover
        btn.setOnMouseEntered(e -> {
            btn.setText(btnSymbol);
            String darkerColor = color;
            if (color.equals("#ff5f57")) darkerColor = "#e04b42"; // Darker red
            if (color.equals("#febc2e")) darkerColor = "#d9a21a"; // Darker yellow
            if (color.equals("#28c840")) darkerColor = "#1fa931"; // Darker green
            
            btn.setStyle(
                "-fx-background-color: " + darkerColor + ";" +
                "-fx-background-radius: 6px;" +
                "-fx-border-color: rgba(0,0,0,0.3);" +
                "-fx-border-width: 0.5px;" +
                "-fx-border-radius: 6px;" +
                "-fx-cursor: hand;" +
                "-fx-font-size: 8px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: rgba(0,0,0,0.7);" + // Show dark symbol on hover
                "-fx-padding: 0;"
            );
        });
        
        // Hide symbol and restore color on exit
        btn.setOnMouseExited(e -> {
            btn.setText("");
            btn.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-background-radius: 6px;" +
                "-fx-border-color: rgba(0,0,0,0.2);" +
                "-fx-border-width: 0.5px;" +
                "-fx-border-radius: 6px;" +
                "-fx-cursor: hand;" +
                "-fx-font-size: 8px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: transparent;" +
                "-fx-padding: 0;"
            );
        });
    }

    private void styleBoxyWindowButton(javafx.scene.control.Button btn, String buttonType) {
        btn.setMinSize(32, 32);
        btn.setPrefSize(32, 32);
        btn.setMaxSize(32, 32);
        btn.setFocusTraversable(false);
        btn.setPickOnBounds(true);
        btn.setMouseTransparent(false);
        btn.setStyle(
            "-fx-background-color: #23232b;" +
            "-fx-background-radius: 0;" +
            "-fx-border-color: #444;" +
            "-fx-border-width: 1.2px;" +
            "-fx-border-radius: 0;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #fff;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: #353545;" +
            "-fx-background-radius: 0;" +
            "-fx-border-color: #666;" +
            "-fx-border-width: 1.2px;" +
            "-fx-border-radius: 0;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #fff;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: #23232b;" +
            "-fx-background-radius: 0;" +
            "-fx-border-color: #444;" +
            "-fx-border-width: 1.2px;" +
            "-fx-border-radius: 0;" +
            "-fx-cursor: hand;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #fff;"
        ));
    }
}


