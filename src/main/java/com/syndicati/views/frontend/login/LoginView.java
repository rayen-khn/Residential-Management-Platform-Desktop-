package com.syndicati.views.frontend.login;

import com.syndicati.controllers.user.auth.AuthController;
import com.syndicati.controllers.user.profile.ProfileController;
import com.syndicati.controllers.biometric.CameraController;
import com.syndicati.utils.session.SessionManager;
import com.syndicati.models.user.User;
import com.syndicati.services.biometric.RealCameraService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.AnimationTimer;
import com.syndicati.interfaces.ViewInterface;
import com.syndicati.utils.theme.ThemeManager;
import com.syndicati.components.shared.ImageBackground;
import com.syndicati.components.shared.ConnectionStatusPill;
import java.util.Optional;

/**
 * Login View - Beautiful login page with liquid glass design and background video
 */
public class LoginView implements ViewInterface {
    
    private final StackPane root;
    private TextField usernameField;
    private PasswordField passwordField;
    private Button loginButton;
    private Button signUpButton;
    private Button otpLoginButton;
    private MediaPlayer mediaPlayer;
    private ImageBackground imageBackground;
    private VBox loginContainer;
    private VBox signUpContainer;
    private VBox forgotPasswordContainer;
    private VBox loginFormColumn;
    private VBox faceIdPanel;
    private HBox loginAuthFlexContainer;
    private boolean faceIdOpen = false;
    private Runnable onLoginSuccess;
    private boolean loginSuccessFired = false;
    private final AuthController authController;
    private final ProfileController profileController;
    private final CameraController cameraController;
    private TextField signUpFirstNameField;
    private RealCameraService cameraService;
    private ImageView faceIdVideoView;
    private AnimationTimer cameraUpdateTimer;
    
    // FaceID panel controls (for authentication)
    private PasswordField faceIdPinInput;
    private Label faceIdStatusLabel;
    private Button faceIdVerifyButton;
    private TextField signUpLastNameField;
    private TextField signUpEmailField;
    private TextField signUpUsernameField;
    private PasswordField signUpPasswordField;
    private PasswordField signUpConfirmPasswordField;
    private TextField forgotRecoveryField;
    
    public LoginView() {
        this.root = new StackPane();
        this.authController = new AuthController();
        this.profileController = new ProfileController();
        this.cameraController = new CameraController();
        setupLayout();
    }

    public void setOnLoginSuccess(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    private void setupAutoLoginListeners() {
        if (usernameField != null) {
            usernameField.textProperty().addListener((obs, oldVal, newVal) -> tryAutoLogin());
        }
        if (passwordField != null) {
            passwordField.textProperty().addListener((obs, oldVal, newVal) -> tryAutoLogin());
        }
    }

    private void tryAutoLogin() {
        if (loginSuccessFired || usernameField == null || passwordField == null) {
            return;
        }
        if ("admin".equals(usernameField.getText()) && "admin".equals(passwordField.getText())) {
            loginSuccessFired = true;
            if (onLoginSuccess != null) {
                onLoginSuccess.run();
            } else {
                navigateToLandingPage();
            }
        }
    }
    
    private void setupLayout() {
        // Image background
        imageBackground = new ImageBackground();
        root.getChildren().add(imageBackground.getRoot());
        
        // Set login video background
        imageBackground.setLoginMode();
        
        // Create the main login container
        loginContainer = createLoginContainer();
        setupAutoLoginListeners();
        
        // Create the sign up container
        signUpContainer = createSignUpContainer();
        signUpContainer.setMaxHeight(Region.USE_PREF_SIZE);
        
        // Create the forgot password container
        forgotPasswordContainer = createForgotPasswordContainer();
        
        // Add login container to root (initially visible)
        root.getChildren().add(loginContainer);
        loginContainer.setVisible(true);
        loginContainer.setManaged(true);
        
        // Add sign up container to root (initially hidden)
        root.getChildren().add(signUpContainer);
        signUpContainer.setVisible(false);
        signUpContainer.setManaged(false);
        
        // Add forgot password container to root (initially hidden)
        root.getChildren().add(forgotPasswordContainer);
        forgotPasswordContainer.setVisible(false);
        forgotPasswordContainer.setManaged(false);
        
        // Create window bar (like landing page)
        HBox windowBar = createWindowBar();
        
        // Create horizontal container for both controls (ensure it doesn't block clicks under empty bounds)
        HBox topRightControls = new HBox();
        topRightControls.setSpacing(12);
        topRightControls.setAlignment(Pos.TOP_RIGHT);
        topRightControls.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        topRightControls.setPickOnBounds(false);
        
        // Create connection status pill
        ConnectionStatusPill connectionPill = new ConnectionStatusPill();
        
        // Create theme toggle
        StackPane themeToggle = createThemeToggle();
        
        // Add both to horizontal container
        topRightControls.getChildren().addAll(connectionPill.getPillContainer(), themeToggle);
        
        // Add window bar to root as a separate layer on top
        root.getChildren().add(windowBar);
        StackPane.setAlignment(windowBar, Pos.TOP_LEFT);
        
        // Add the horizontal container to root (positioned below window bar)
        root.getChildren().add(topRightControls);
        StackPane.setAlignment(topRightControls, Pos.TOP_RIGHT);
        StackPane.setMargin(topRightControls, new Insets(52, 20, 0, 0)); // Top margin 52px to go below window bar
        // Keep login UI on top of overlays
        loginContainer.toFront();
        
        // Apply theme styling
        applyThemeStyling();
    }
    
    
    private VBox createLoginContainer() {
        VBox container = new VBox();
        container.setSpacing(0);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(20, 18, 18, 18));
        container.setPrefWidth(560);
        container.setMaxWidth(560);
        container.setMaxHeight(520);
        
        ThemeManager themeManager = ThemeManager.getInstance();
        container.setStyle(authSurfaceStyle(themeManager));
        container.setOnMouseEntered(e -> container.setStyle(authSurfaceHoverStyle(themeManager)));
        container.setOnMouseExited(e -> container.setStyle(authSurfaceStyle(themeManager)));
        
        // Add glassmorphism shadow
        DropShadow glassShadow = new DropShadow();
        glassShadow.setBlurType(javafx.scene.effect.BlurType.GAUSSIAN);
        glassShadow.setColor(Color.color(0, 0, 0, 0.3));
        glassShadow.setRadius(30);
        glassShadow.setOffsetX(0);
        glassShadow.setOffsetY(10);
        container.setEffect(glassShadow);
        
        // App title
        Text title = new Text("Welcome Back");
        title.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 36));
        title.setFill(createAccentGradientPaint());
        title.setTextAlignment(TextAlignment.CENTER);
        
        // Subtitle
        Text subtitle = new Text("Sign in to continue");
        subtitle.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 16));
        subtitle.setFill(themeManager.isDarkMode() ? Color.web("#b3b8e0") : Color.web("#475569"));
        subtitle.setTextAlignment(TextAlignment.CENTER);
        
        VBox headerWrap = new VBox(6, title, subtitle);
        headerWrap.setAlignment(Pos.CENTER);

        // Username/email + password row (website-like split form)
        VBox usernameContainer = createInputField("", "Email");
        usernameField = (TextField) usernameContainer.getChildren().get(1);
        
        VBox passwordContainer = createInputField("", "Password");
        passwordField = (PasswordField) passwordContainer.getChildren().get(1);

        HBox row = createFormRow(usernameContainer, passwordContainer);
        
        // Create button container for side-by-side buttons
        HBox buttonContainer = createButtonContainer();
        
        // Forgot password link
        Hyperlink forgotPassword = new Hyperlink("Forgot Password?");
        forgotPassword.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        forgotPassword.setTextFill(Color.web(themeManager.getAccentHex()));
        forgotPassword.setBorder(Border.EMPTY);
        forgotPassword.setOnAction(e -> switchToForgotPassword());

        // Website-like auth methods block (visual only for now)
        HBox socialDivider = createAuthSocialDivider();
        VBox authMethodsBlock = createAuthMethodsBlock();
        
        loginFormColumn = new VBox(12,
            headerWrap,
            row,
            buttonContainer,
            forgotPassword,
            socialDivider,
            authMethodsBlock
        );
        loginFormColumn.setAlignment(Pos.TOP_CENTER);
        loginFormColumn.setPadding(new Insets(22, 16, 10, 16));
        loginFormColumn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(loginFormColumn, Priority.ALWAYS);

        faceIdPanel = createFaceIdPanel();

        loginAuthFlexContainer = new HBox(0, loginFormColumn, faceIdPanel);
        loginAuthFlexContainer.setAlignment(Pos.CENTER_LEFT);
        loginAuthFlexContainer.setMaxWidth(Double.MAX_VALUE);
        loginAuthFlexContainer.setStyle("-fx-background-radius: 24px;");

        container.getChildren().add(loginAuthFlexContainer);
        
        return container;
    }
    
    private HBox createButtonContainer() {
        HBox buttonContainer = new HBox();
        buttonContainer.setSpacing(8);
        buttonContainer.setAlignment(Pos.CENTER);
        
        // Create sign in button
        loginButton = createLoginButton();
        loginButton.setPrefWidth(146); // Half of original 300px width minus spacing
        loginButton.setMaxWidth(146);
        loginButton.setMinWidth(146);
        
        // Create sign up button
        signUpButton = createSignUpButton();
        signUpButton.setPrefWidth(112);
        signUpButton.setMaxWidth(112);
        signUpButton.setMinWidth(112);

        // Create OTP button (email-only login flow)
        otpLoginButton = createOtpLoginButton();
        otpLoginButton.setPrefWidth(112);
        otpLoginButton.setMaxWidth(112);
        otpLoginButton.setMinWidth(112);
        
        buttonContainer.getChildren().addAll(loginButton, signUpButton, otpLoginButton);
        return buttonContainer;
    }

    private HBox createAuthSocialDivider() {
        HBox divider = new HBox(10);
        divider.setAlignment(Pos.CENTER);
        divider.setMaxWidth(Double.MAX_VALUE);

        Region left = new Region();
        left.setPrefHeight(1);
        left.setMaxHeight(1);
        left.setStyle("-fx-background-color: rgba(255,255,255,0.18);");
        HBox.setHgrow(left, Priority.ALWAYS);

        Text txt = new Text("Or continue with");
        txt.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 12));
        txt.setFill(ThemeManager.getInstance().isDarkMode() ? Color.web("#a0aec0") : Color.web("#64748b"));

        Region right = new Region();
        right.setPrefHeight(1);
        right.setMaxHeight(1);
        right.setStyle("-fx-background-color: rgba(255,255,255,0.18);");
        HBox.setHgrow(right, Priority.ALWAYS);

        divider.getChildren().addAll(left, txt, right);
        return divider;
    }

    private VBox createAuthMethodsBlock() {
        VBox wrap = new VBox(8);
        wrap.setAlignment(Pos.CENTER);
        wrap.setMaxWidth(Double.MAX_VALUE);

        HBox primaryRow = new HBox(8);
        primaryRow.setAlignment(Pos.CENTER);
        primaryRow.setMaxWidth(Double.MAX_VALUE);

        Button google = createAuthMethodButton("\ud83d\udd10", "Sign in with Google", true);
        HBox.setHgrow(google, Priority.ALWAYS);
        google.setMaxWidth(Double.MAX_VALUE);
        google.setOnAction(e -> showInfoMessage("Google login UI is ready. Functionality will be connected later."));

        Button toggle = new Button("➕");
        toggle.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 12));
        toggle.setPrefWidth(42);
        toggle.setPrefHeight(42);
        toggle.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-background-radius: 12px;" +
            "-fx-border-color: rgba(255,255,255,0.18);" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 12px;" +
            "-fx-text-fill: " + ThemeManager.getInstance().getTextColor() + ";" +
            "-fx-cursor: hand;"
        );

        VBox secondary = new VBox(8);
        secondary.setAlignment(Pos.CENTER);
        secondary.setVisible(false);
        secondary.setManaged(false);
        secondary.setOpacity(0);

        Button github = createAuthMethodButton("\ud83d\ude4f", "GitHub", false);
        github.setOnAction(e -> showInfoMessage("GitHub login UI is ready. Functionality will be connected later."));

        HBox biometricRow = new HBox(8);
        biometricRow.setAlignment(Pos.CENTER);
        biometricRow.setMaxWidth(Double.MAX_VALUE);

        Button passkey = createAuthMethodButton("\ud83d\udd10", "Passkey", false);
        Button faceId = createAuthMethodButton("\ud83d\udcf7", "Face ID", false);
        HBox.setHgrow(passkey, Priority.ALWAYS);
        HBox.setHgrow(faceId, Priority.ALWAYS);
        passkey.setMaxWidth(Double.MAX_VALUE);
        faceId.setMaxWidth(Double.MAX_VALUE);
        passkey.setOnAction(e -> handlePasskeyLogin());
        faceId.setOnAction(e -> openFaceIdPanel());
        biometricRow.getChildren().addAll(passkey, faceId);

        secondary.getChildren().addAll(github, biometricRow);

        final boolean[] expanded = {false};
        toggle.setOnAction(e -> {
            expanded[0] = !expanded[0];
            if (expanded[0]) {
                secondary.setManaged(true);
                secondary.setVisible(true);

                javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(220), secondary);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);

                javafx.animation.TranslateTransition slideIn = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(220), secondary);
                slideIn.setFromY(-8);
                slideIn.setToY(0);

                javafx.animation.RotateTransition rotate = new javafx.animation.RotateTransition(javafx.util.Duration.millis(180), toggle);
                rotate.setToAngle(180);

                new javafx.animation.ParallelTransition(fadeIn, slideIn, rotate).play();
            } else {
                javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(170), secondary);
                fadeOut.setFromValue(secondary.getOpacity());
                fadeOut.setToValue(0);

                javafx.animation.TranslateTransition slideOut = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(170), secondary);
                slideOut.setFromY(0);
                slideOut.setToY(-8);

                javafx.animation.RotateTransition rotate = new javafx.animation.RotateTransition(javafx.util.Duration.millis(170), toggle);
                rotate.setToAngle(0);

                javafx.animation.ParallelTransition hideAnim = new javafx.animation.ParallelTransition(fadeOut, slideOut, rotate);
                hideAnim.setOnFinished(done -> {
                    secondary.setVisible(false);
                    secondary.setManaged(false);
                    secondary.setTranslateY(0);
                });
                hideAnim.play();
            }
        });

        primaryRow.getChildren().addAll(google, toggle);
        wrap.getChildren().addAll(primaryRow, secondary);
        return wrap;
    }

    private VBox createFaceIdPanel() {
        ThemeManager tm = ThemeManager.getInstance();
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(14, 16, 14, 16));
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setVisible(false);
        panel.setManaged(false);
        panel.setOpacity(0);
        panel.setTranslateX(24);
        panel.setPrefWidth(0);
        panel.setMaxWidth(0);
        panel.setMinWidth(0);
        panel.setStyle(
            "-fx-background-color: " + (tm.isDarkMode() ? "rgba(8,8,8,0.65)" : "rgba(245,245,245,0.78)") + ";" +
            "-fx-border-color: rgba(255,255,255,0.10);" +
            "-fx-border-width: 0 0 0 1px;" +
            "-fx-background-radius: 18px;"
        );

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Text title = new Text("Face ID");
        title.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 20));
        title.setFill(Color.web(tm.getTextColor()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button close = new Button("x");
        close.setPrefSize(30, 30);
        close.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-border-color: rgba(255,255,255,0.14);" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 10px;" +
            "-fx-background-radius: 10px;" +
            "-fx-text-fill: " + tm.getTextColor() + ";" +
            "-fx-cursor: hand;"
        );
        close.setOnAction(e -> closeFaceIdPanel());
        header.getChildren().addAll(title, spacer, close);

        StackPane videoWrap = new StackPane();
        videoWrap.setPrefHeight(210);
        videoWrap.setMinHeight(180);
        videoWrap.setMaxHeight(260);
        videoWrap.setStyle(
            "-fx-background-color: #000000;" +
            "-fx-background-radius: 14px;" +
            "-fx-border-color: rgba(255,255,255,0.12);" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 14px;"
        );

        // Create ImageView for live camera feed
        faceIdVideoView = new ImageView();
        faceIdVideoView.setPreserveRatio(true);
        faceIdVideoView.setFitWidth(300);
        faceIdVideoView.setFitHeight(210);

        Text camHint = new Text("Camera Preview");
        camHint.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        camHint.setFill(Color.web("rgba(255,255,255,0.52)"));

        Region scannerFrame = new Region();
        scannerFrame.setMouseTransparent(true);
        scannerFrame.setPrefSize(250, 160);
        scannerFrame.setStyle(
            "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.30) + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-color: transparent;"
        );

        videoWrap.getChildren().addAll(camHint, faceIdVideoView, scannerFrame);

        faceIdStatusLabel = new Label("Awaiting facial signature...");
        faceIdStatusLabel.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 12));
        faceIdStatusLabel.setStyle(
            "-fx-text-fill: " + tm.toRgba(tm.getAccentHex(), 0.96) + ";" +
            "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.12) + ";" +
            "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.22) + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 999px;" +
            "-fx-background-radius: 999px;" +
            "-fx-padding: 6px 12px;"
        );

        Label pinLabel = new Label("PIN");
        pinLabel.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.BOLD, 11));
        pinLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.60);");

        faceIdPinInput = new PasswordField();
        faceIdPinInput.setPromptText("Enter PIN");
        faceIdPinInput.setAlignment(Pos.CENTER);
        faceIdPinInput.setStyle(
            "-fx-background-color: rgba(255,255,255,0.05);" +
            "-fx-border-color: rgba(255,255,255,0.16);" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 10px;" +
            "-fx-background-radius: 10px;" +
            "-fx-text-fill: " + tm.getTextColor() + ";" +
            "-fx-prompt-text-fill: rgba(255,255,255,0.45);" +
            "-fx-padding: 10px 12px;"
        );

        faceIdVerifyButton = new Button("Verify");
        faceIdVerifyButton.setMaxWidth(Double.MAX_VALUE);
        faceIdVerifyButton.setPrefHeight(42);
        faceIdVerifyButton.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 13));
        faceIdVerifyButton.setStyle(
            "-fx-background-color: " + tm.getEffectiveAccentGradient() + ";" +
            "-fx-background-radius: 12px;" +
            "-fx-text-fill: white;" +
            "-fx-cursor: hand;" +
            "-fx-letter-spacing: 0.5px;"
        );
        faceIdVerifyButton.setOnAction(e -> handleFaceIDLogin());

        panel.getChildren().addAll(header, videoWrap, faceIdStatusLabel, pinLabel, faceIdPinInput, faceIdVerifyButton);
        VBox.setVgrow(videoWrap, Priority.ALWAYS);
        return panel;
    }

    private void openFaceIdPanel() {
        if (faceIdOpen || faceIdPanel == null || loginContainer == null || loginFormColumn == null) {
            return;
        }
        faceIdOpen = true;

        faceIdPanel.setManaged(true);
        faceIdPanel.setVisible(true);

        loginFormColumn.setStyle(
            "-fx-border-color: rgba(255,255,255,0.10);" +
            "-fx-border-width: 0 1px 0 0;" +
            "-fx-padding: 22 24 10 16;"
        );

        javafx.animation.Timeline tl = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                new javafx.animation.KeyValue(loginContainer.prefWidthProperty(), 560),
                new javafx.animation.KeyValue(loginContainer.maxWidthProperty(), 560),
                new javafx.animation.KeyValue(faceIdPanel.maxWidthProperty(), 0),
                new javafx.animation.KeyValue(faceIdPanel.prefWidthProperty(), 0),
                new javafx.animation.KeyValue(faceIdPanel.opacityProperty(), 0),
                new javafx.animation.KeyValue(faceIdPanel.translateXProperty(), 24)
            ),
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(450),
                new javafx.animation.KeyValue(loginContainer.prefWidthProperty(), 1080, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(loginContainer.maxWidthProperty(), 1080, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(faceIdPanel.maxWidthProperty(), 500, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(faceIdPanel.prefWidthProperty(), 500, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(faceIdPanel.opacityProperty(), 1, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(faceIdPanel.translateXProperty(), 0, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        tl.play();
    }

    private void closeFaceIdPanel() {
        if (!faceIdOpen || faceIdPanel == null || loginContainer == null || loginFormColumn == null) {
            return;
        }
        faceIdOpen = false;

        // Release camera resources
        if (cameraService != null) {
            cameraService.stopCapture();
            cameraService.release();
            cameraService = null;
        }

        // Stop camera update timer
        if (cameraUpdateTimer != null) {
            cameraUpdateTimer.stop();
            cameraUpdateTimer = null;
        }

        javafx.animation.Timeline tl = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                new javafx.animation.KeyValue(loginContainer.prefWidthProperty(), loginContainer.getWidth() > 560 ? loginContainer.getWidth() : 1080),
                new javafx.animation.KeyValue(loginContainer.maxWidthProperty(), loginContainer.getWidth() > 560 ? loginContainer.getWidth() : 1080),
                new javafx.animation.KeyValue(faceIdPanel.maxWidthProperty(), faceIdPanel.getWidth() > 0 ? faceIdPanel.getWidth() : 500),
                new javafx.animation.KeyValue(faceIdPanel.prefWidthProperty(), faceIdPanel.getWidth() > 0 ? faceIdPanel.getWidth() : 500),
                new javafx.animation.KeyValue(faceIdPanel.opacityProperty(), faceIdPanel.getOpacity()),
                new javafx.animation.KeyValue(faceIdPanel.translateXProperty(), faceIdPanel.getTranslateX())
            ),
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(360),
                new javafx.animation.KeyValue(loginContainer.prefWidthProperty(), 560, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(loginContainer.maxWidthProperty(), 560, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(faceIdPanel.maxWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(faceIdPanel.prefWidthProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(faceIdPanel.opacityProperty(), 0, javafx.animation.Interpolator.EASE_BOTH),
                new javafx.animation.KeyValue(faceIdPanel.translateXProperty(), 24, javafx.animation.Interpolator.EASE_BOTH)
            )
        );
        tl.setOnFinished(e -> {
            faceIdPanel.setVisible(false);
            faceIdPanel.setManaged(false);
            loginFormColumn.setStyle("-fx-padding: 22 16 10 16;");
        });
        tl.play();
    }

    private Button createAuthMethodButton(String icon, String text, boolean primary) {
        Button b = new Button(icon + "  " + text);
        b.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 13));
        b.setPrefHeight(42);
        b.setMaxWidth(Double.MAX_VALUE);
        String normal = primary ?
            "-fx-background-color: rgba(255,255,255,0.10);" +
            "-fx-background-radius: 12px;" +
            "-fx-border-color: rgba(255,255,255,0.20);" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 12px;" +
            "-fx-text-fill: " + ThemeManager.getInstance().getTextColor() + ";" +
            "-fx-cursor: hand;"
            :
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-background-radius: 12px;" +
            "-fx-border-color: rgba(255,255,255,0.14);" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 12px;" +
            "-fx-text-fill: " + ThemeManager.getInstance().getTextColor() + ";" +
            "-fx-cursor: hand;";
        String hover = primary ?
            "-fx-background-color: rgba(255,255,255,0.16);" +
            "-fx-background-radius: 12px;" +
            "-fx-border-color: " + ThemeManager.getInstance().toRgba(ThemeManager.getInstance().getAccentHex(), 0.45) + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 12px;" +
            "-fx-text-fill: " + ThemeManager.getInstance().getTextColor() + ";" +
            "-fx-cursor: hand;"
            :
            "-fx-background-color: rgba(255,255,255,0.11);" +
            "-fx-background-radius: 12px;" +
            "-fx-border-color: " + ThemeManager.getInstance().toRgba(ThemeManager.getInstance().getAccentHex(), 0.36) + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 12px;" +
            "-fx-text-fill: " + ThemeManager.getInstance().getTextColor() + ";" +
            "-fx-cursor: hand;";
        b.setStyle(normal);
        b.setOnMouseEntered(e -> {
            b.setStyle(hover);
            b.setScaleX(1.02);
            b.setScaleY(1.02);
        });
        b.setOnMouseExited(e -> {
            b.setStyle(normal);
            b.setScaleX(1.0);
            b.setScaleY(1.0);
        });
        return b;
    }
    
    private VBox createSignUpContainer() {
        VBox container = new VBox();
        container.setSpacing(10);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(24, 34, 24, 34));
        container.setMaxWidth(700);
        
        ThemeManager themeManager = ThemeManager.getInstance();
        container.setStyle(authSurfaceStyle(themeManager));
        container.setOnMouseEntered(e -> container.setStyle(authSurfaceHoverStyle(themeManager)));
        container.setOnMouseExited(e -> container.setStyle(authSurfaceStyle(themeManager)));
        
        // Add glassmorphism shadow
        DropShadow glassShadow = new DropShadow();
        glassShadow.setBlurType(javafx.scene.effect.BlurType.GAUSSIAN);
        glassShadow.setColor(Color.color(0, 0, 0, 0.3));
        glassShadow.setRadius(30);
        glassShadow.setOffsetX(0);
        glassShadow.setOffsetY(10);
        container.setEffect(glassShadow);
        
        // ===== HEADER =====
        Text title = new Text("Create Account");
        title.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 28));
        title.setFill(createAccentGradientPaint());
        title.setTextAlignment(TextAlignment.CENTER);
        
        Text subtitle = new Text("Join us today");
        subtitle.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        subtitle.setFill(themeManager.isDarkMode() ? Color.web("#b3b8e0") : Color.web("#475569"));
        subtitle.setTextAlignment(TextAlignment.CENTER);

        VBox headerWrap = new VBox(4, title, subtitle);
        headerWrap.setAlignment(Pos.CENTER);
        
        // ===== FORM FIELDS WITH VALIDATION =====
        VBox firstNameContainer = createInputFieldWithValidation("First Name", "firstName");
        VBox lastNameContainer = createInputFieldWithValidation("Last Name", "lastName");
        VBox emailContainer = createInputFieldWithValidation("Email Address", "email");
        
        signUpFirstNameField = (TextField) firstNameContainer.lookup("TextField");
        signUpLastNameField = (TextField) lastNameContainer.lookup("TextField");
        signUpEmailField = (TextField) emailContainer.lookup("TextField");
        
        // Setup validation listeners
        setupNameValidation(signUpFirstNameField, firstNameContainer, "firstName");
        setupNameValidation(signUpLastNameField, lastNameContainer, "lastName");
        setupEmailValidation(signUpEmailField, emailContainer);
        
        // ===== PASSWORD FIELD WITH VALIDATION DISPLAY =====
        VBox passwordFieldUI = createPasswordFieldWithValidation();
        signUpPasswordField = (PasswordField) getNodeByStyle(passwordFieldUI, "-fx-is-password: true;");
        if (signUpPasswordField == null) {
            // Fallback: find first PasswordField
            for (javafx.scene.Node n : passwordFieldUI.lookupAll("PasswordField")) {
                signUpPasswordField = (PasswordField)n;
                break;
            }
        }
        
        // ===== CONFIRM PASSWORD FIELD =====
        VBox confirmPasswordContainer = createPasswordInputField("Confirm Password");
        signUpConfirmPasswordField = (PasswordField) getPasswordFieldFromContainer(confirmPasswordContainer);
        
        HBox rowOne = createFormRow(firstNameContainer, lastNameContainer);
        HBox rowTwo = createFormRow(emailContainer, new VBox()); // Empty space
        rowTwo.getChildren().get(1).setStyle("-fx-min-width: 0; -fx-pref-width: 0;");
        
        // ===== SIGN UP BUTTON =====
        Button signUpSubmitButton = createSignUpSubmitButton();
        
        // ===== BACK TO LOGIN LINK =====
        Hyperlink backToLogin = new Hyperlink("Already have an account? Sign In");
        backToLogin.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        backToLogin.setTextFill(Color.web(themeManager.getAccentHex()));
        backToLogin.setBorder(Border.EMPTY);
        backToLogin.setOnAction(e -> switchToLogin());
        
        // Add all elements to container
        container.getChildren().addAll(
            headerWrap,
            rowOne,
            rowTwo,
            passwordFieldUI,
            confirmPasswordContainer,
            signUpSubmitButton,
            backToLogin
        );
        
        return container;
    }
    
    private VBox createPasswordFieldWithValidation() {
        VBox mainContainer = new VBox(10);
        mainContainer.setStyle("-fx-padding: 0;");
        
        // Label
        Text label = new Text("Password");
        label.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        label.setFill(Color.web(ThemeManager.getInstance().isDarkMode() ? "#f8fafc" : "#0f172a"));
        
        // Password field with eye toggle
        HBox passwordFieldContainer = new HBox(8);
        passwordFieldContainer.setAlignment(Pos.CENTER_LEFT);
        
        PasswordField pwField = new PasswordField();
        pwField.setPromptText("Create a strong password");
        pwField.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        pwField.setPrefHeight(46);
        pwField.setMaxWidth(Double.MAX_VALUE);
        applyAuthInputStyle(pwField, false);
        pwField.focusedProperty().addListener((obs, oldVal, newVal) -> applyAuthInputStyle(pwField, newVal));
        HBox.setHgrow(pwField, Priority.ALWAYS);
        
        // Eye toggle button
        Button eyeToggle = createPasswordToggleButton(pwField);
        eyeToggle.setPrefSize(40, 40);
        eyeToggle.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ThemeManager.getInstance().getAccentHex() + "; -fx-font-size: 16;");
        
        passwordFieldContainer.getChildren().addAll(pwField, eyeToggle);
        
        // Password requirements display  
        VBox requirementsBox = createPasswordRequirementsDisplay(pwField);
        
        mainContainer.getChildren().addAll(label, passwordFieldContainer, requirementsBox);
        
        return mainContainer;
    }
    
    private VBox createPasswordRequirementsDisplay(PasswordField pwField) {
        VBox requirementsBox = new VBox(8);
        requirementsBox.setStyle("-fx-padding: 12; -fx-background-color: rgba(255,255,255,0.03); -fx-border-radius: 8; -fx-background-radius: 8;");
        
        // Length requirement
        HBox lengthReq = createRequirementRow("\u2713", "8+ characters");
        lengthReq.setId("req-length");
        lengthReq.setStyle("-fx-text-fill: rgba(255,255,255,0.5);");
        
        // Uppercase requirement
        HBox uppercaseReq = createRequirementRow("\u2713", "Uppercase letter");
        uppercaseReq.setId("req-uppercase");
        uppercaseReq.setStyle("-fx-text-fill: rgba(255,255,255,0.5);");
        
        // Special character requirement
        HBox specialReq = createRequirementRow("\u2713", "Special character");
        specialReq.setId("req-special");
        specialReq.setStyle("-fx-text-fill: rgba(255,255,255,0.5);");
        
        requirementsBox.getChildren().addAll(lengthReq, uppercaseReq, specialReq);
        
        // Add listener to update validation display
        pwField.textProperty().addListener((obs, oldVal, newVal) -> {
            updatePasswordRequirements(requirementsBox, newVal);
        });
        
        return requirementsBox;
    }
    
    private void updatePasswordRequirements(VBox requirementsBox, String password) {
        boolean hasLength = password != null && password.length() >= 8;
        boolean hasUppercase = password != null && password.matches(".*[A-Z].*");
        boolean hasSpecial = password != null && password.matches(".*[!@#$%^&*(),.?\\\":{}|<>].*");
        
        updateRequirementStyle((HBox) requirementsBox.lookup("#req-length"), hasLength);
        updateRequirementStyle((HBox) requirementsBox.lookup("#req-uppercase"), hasUppercase);
        updateRequirementStyle((HBox) requirementsBox.lookup("#req-special"), hasSpecial);
    }
    
    private void updateRequirementStyle(HBox reqBox, boolean met) {
        if (reqBox != null) {
            ThemeManager tm = ThemeManager.getInstance();
            String color = met ? tm.getAccentHex() : "rgba(255,255,255,0.5)";
            reqBox.setStyle("-fx-text-fill: " + color + ";");
            // Update checkmark icon color
            for (javafx.scene.Node node : reqBox.getChildren()) {
                if (node instanceof Text) {
                    ((Text)node).setFill(Color.web(color));
                }
            }
        }
    }
    
    private HBox createRequirementRow(String icon, String text) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        
        Text iconText = new Text(icon);
        iconText.setFont(Font.font(10));
        iconText.setFill(Color.web("rgba(255,255,255,0.5)"));
        
        Text labelText = new Text(text);
        labelText.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 12));
        labelText.setFill(Color.web("rgba(255,255,255,0.6)"));
        
        row.getChildren().addAll(iconText, labelText);
        return row;
    }
    
    private Button createPasswordToggleButton(PasswordField pwField) {
        Button btn = new Button("\ud83d\udc41");
        btn.setOnAction(e -> {
            String currentText = pwField.getText();
            TextField tempField = new TextField(currentText);
            tempField.setPrefHeight(pwField.getPrefHeight());
            tempField.setMaxWidth(Double.MAX_VALUE);
            
            // Toggle between PasswordField and TextField
            Parent parent = pwField.getParent();
            if (parent instanceof HBox) {
                HBox container = (HBox) parent;
                int index = container.getChildren().indexOf(pwField);
                if (pwField.isVisible()) {
                    // Show as text
                    container.getChildren().set(index, tempField);
                    applyAuthInputStyle(tempField, true);
                    tempField.requestFocus();
                } else {
                    // Show as password
                    container.getChildren().set(index, pwField);
                    pwField.requestFocus();
                }
            }
        });
        return btn;
    }
    
    private VBox createPasswordInputField(String label) {
        VBox container = new VBox();
        container.setSpacing(8);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setMaxWidth(Double.MAX_VALUE);
        
        // Label
        Text labelText = new Text(label);
        labelText.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        labelText.setFill(Color.web(ThemeManager.getInstance().isDarkMode() ? "#f8fafc" : "#0f172a"));
        
        // Password field with eye toggle
        HBox passwordContainer = new HBox(8);
        passwordContainer.setAlignment(Pos.CENTER_LEFT);
        
        PasswordField pwField = new PasswordField();
        pwField.setPromptText(label);
        pwField.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        pwField.setPrefHeight(46);
        pwField.setMaxWidth(Double.MAX_VALUE);
        applyAuthInputStyle(pwField, false);
        pwField.focusedProperty().addListener((obs, oldVal, newVal) -> applyAuthInputStyle(pwField, newVal));
        HBox.setHgrow(pwField, Priority.ALWAYS);
        
        // Eye toggle
        Button eyeToggle = createPasswordToggleButton(pwField);
        eyeToggle.setPrefSize(40, 40);
        eyeToggle.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ThemeManager.getInstance().getAccentHex() + "; -fx-font-size: 16;");
        
        passwordContainer.getChildren().addAll(pwField, eyeToggle);
        
        container.getChildren().addAll(labelText, passwordContainer);
        
        return container;
    }
    
    private javafx.scene.Node getNodeByStyle(javafx.scene.Parent parent, String stylePattern) {
        for (javafx.scene.Node node : parent.lookupAll("*")) {
            if (node instanceof PasswordField) {
                return node;
            }
        }
        return null;
    }
    
    private PasswordField getPasswordFieldFromContainer(VBox container) {
        for (javafx.scene.Node node : container.lookupAll("PasswordField")) {
            return (PasswordField)node;
        }
        return null;
    }
    
    private VBox createForgotPasswordContainer() {
        VBox container = new VBox();
        container.setSpacing(16);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(34, 34, 30, 34));
        container.setMaxWidth(560);
        container.setMaxHeight(480);
        
        ThemeManager themeManager = ThemeManager.getInstance();
        container.setStyle(authSurfaceStyle(themeManager));
        container.setOnMouseEntered(e -> container.setStyle(authSurfaceHoverStyle(themeManager)));
        container.setOnMouseExited(e -> container.setStyle(authSurfaceStyle(themeManager)));
        
        // Add glassmorphism shadow
        DropShadow glassShadow = new DropShadow();
        glassShadow.setBlurType(javafx.scene.effect.BlurType.GAUSSIAN);
        glassShadow.setColor(Color.color(0, 0, 0, 0.3));
        glassShadow.setRadius(30);
        glassShadow.setOffsetX(0);
        glassShadow.setOffsetY(10);
        container.setEffect(glassShadow);
        
        // App title
        Text title = new Text("Reset Password");
        title.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 30));
        title.setFill(createAccentGradientPaint());
        title.setTextAlignment(TextAlignment.CENTER);
        
        // Subtitle
        Text subtitle = new Text("Recover your account access");
        subtitle.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        subtitle.setFill(themeManager.isDarkMode() ? Color.web("#b3b8e0") : Color.web("#475569"));
        subtitle.setTextAlignment(TextAlignment.CENTER);
        
        // Description
        Text description = new Text("Enter your username, email, or phone number and we'll send you a reset link or OTP");
        description.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 12));
        description.setFill(themeManager.isDarkMode() ? Color.web("rgba(255,255,255,0.75)") : Color.web("rgba(30,41,59,0.75)"));
        description.setTextAlignment(TextAlignment.CENTER);
        description.setWrappingWidth(430);
        
        // Recovery field
        VBox recoveryContainer = createCenteredInputField("", "Username, Email, or Phone Number");
        forgotRecoveryField = (TextField) recoveryContainer.getChildren().get(1);
        
        // Send reset button
        Button sendResetButton = createSendResetButton();
        
        // Back to login link
        Hyperlink backToLogin = new Hyperlink("Remember your password? Sign In");
        backToLogin.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        backToLogin.setTextFill(Color.web(themeManager.getAccentHex()));
        backToLogin.setBorder(Border.EMPTY);
        backToLogin.setOnAction(e -> switchToLogin());
        
        // Add all elements to container
        container.getChildren().addAll(
            title, subtitle, description,
            recoveryContainer, sendResetButton, backToLogin
        );
        
        return container;
    }
    
    private VBox createCenteredInputField(String icon, String placeholder) {
        VBox container = new VBox();
        container.setSpacing(8);
        container.setAlignment(Pos.CENTER); // Center alignment instead of CENTER_LEFT
        
        // Icon and label
        HBox labelContainer = new HBox();
        labelContainer.setSpacing(8);
        labelContainer.setAlignment(Pos.CENTER); // Center alignment
        
        Text label = new Text(placeholder);
        label.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        label.setFill(Color.web(ThemeManager.getInstance().isDarkMode() ? "#f8fafc" : "#0f172a"));

        if (icon != null && !icon.isBlank()) {
            Text iconText = new Text(icon);
            iconText.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 15));
            iconText.setFill(Color.web(ThemeManager.getInstance().isDarkMode() ? "#cbd5e1" : "#475569"));
            labelContainer.getChildren().add(iconText);
        }
        labelContainer.getChildren().add(label);
        
        // Input field
        TextField inputField = new TextField();
    inputField.setPromptText(placeholder);
    inputField.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        inputField.setPrefHeight(46);
        inputField.setMaxWidth(Double.MAX_VALUE);
        applyAuthInputStyle(inputField, false);
        inputField.focusedProperty().addListener((obs, oldVal, newVal) -> applyAuthInputStyle(inputField, newVal));
        
        container.getChildren().addAll(labelContainer, inputField);
        
        return container;
    }

    private String authSurfaceStyle(ThemeManager tm) {
        String bg = tm.isDarkMode()
            ? "rgba(12,12,12,0.86)"
            : "rgba(245,245,245,0.88)";
        return "-fx-background-color: " + bg + ";" +
               "-fx-background-radius: 28px;" +
               "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), tm.isDarkMode() ? 0.30 : 0.24) + ";" +
               "-fx-border-width: 1.5px;" +
               "-fx-border-radius: 28px;" +
               "-fx-background-insets: 0, 0, 0, 0;";
    }

    private String authSurfaceHoverStyle(ThemeManager tm) {
        String bg = tm.isDarkMode()
            ? "rgba(16,16,16,0.90)"
            : "rgba(245,245,245,0.94)";
        return "-fx-background-color: " + bg + ";" +
               "-fx-background-radius: 28px;" +
               "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), tm.isDarkMode() ? 0.36 : 0.28) + ";" +
               "-fx-border-width: 1.5px;" +
               "-fx-border-radius: 28px;" +
               "-fx-background-insets: 0, 0, 0, 0;";
    }
    
    private VBox createInputField(String icon, String placeholder) {
        VBox container = new VBox();
        container.setSpacing(8);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setMaxWidth(Double.MAX_VALUE);
        
        // Icon and label
        HBox labelContainer = new HBox();
        labelContainer.setSpacing(8);
        labelContainer.setAlignment(Pos.CENTER_LEFT);
        
        Text label = new Text(placeholder);
        label.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        label.setFill(Color.web(ThemeManager.getInstance().isDarkMode() ? "#f8fafc" : "#0f172a"));
        if (icon != null && !icon.isBlank()) {
            Text iconText = new Text(icon);
            iconText.setFont(Font.font(15));
            iconText.setFill(Color.web(ThemeManager.getInstance().isDarkMode() ? "#cbd5e1" : "#475569"));
            labelContainer.getChildren().add(iconText);
        }
        labelContainer.getChildren().add(label);
        
        // Input field
        TextField inputField;
        if (placeholder.contains("Password")) {
            inputField = new PasswordField();
        } else {
            inputField = new TextField();
        }
        
    inputField.setPromptText(placeholder);
    inputField.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        inputField.setPrefHeight(46);
        inputField.setMaxWidth(Double.MAX_VALUE);
        applyAuthInputStyle(inputField, false);
        inputField.focusedProperty().addListener((obs, oldVal, newVal) -> applyAuthInputStyle(inputField, newVal));
        
        container.getChildren().addAll(labelContainer, inputField);
        
        return container;
    }
    
    private VBox createInputFieldWithValidation(String placeholder, String fieldType) {
        VBox container = new VBox();
        container.setSpacing(4);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setMaxWidth(Double.MAX_VALUE);
        
        // Label
        Text label = new Text(placeholder);
        label.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        label.setFill(Color.web(ThemeManager.getInstance().isDarkMode() ? "#f8fafc" : "#0f172a"));
        
        // Input field
        TextField inputField = new TextField();
        inputField.setPromptText(placeholder);
        inputField.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        inputField.setPrefHeight(46);
        inputField.setMaxWidth(Double.MAX_VALUE);
        applyAuthInputStyle(inputField, false);
        inputField.focusedProperty().addListener((obs, oldVal, newVal) -> applyAuthInputStyle(inputField, newVal));
        
        // Validation message container
        Text validationMessage = new Text("");
        validationMessage.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 12));
        validationMessage.setFill(Color.web("#ff3b30")); // Error red by default
        validationMessage.setVisible(false);
        validationMessage.setManaged(false);
        validationMessage.setId("validation-" + fieldType);
        
        container.getChildren().addAll(label, inputField, validationMessage);
        
        return container;
    }
    
    private void setupNameValidation(TextField field, VBox container, String fieldType) {
        Text validationMsg = (Text) container.lookup("#validation-" + fieldType);
        
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String trimmed = newVal.trim();
            boolean valid = true;
            String message = "";
            
            if (trimmed.isEmpty()) {
                valid = false;
                message = fieldType.equals("firstName") ? "First name is required" : "Last name is required";
            } else if (trimmed.length() < 2) {
                valid = false;
                message = "Must be at least 2 characters";
            } else if (!trimmed.matches("^[\\p{L}\\s-]+$")) {
                valid = false;
                message = "Only letters, spaces and hyphens allowed";
            }
            
            // Update validation message
            if (valid && !trimmed.isEmpty()) {
                validationMsg.setVisible(false);
                validationMsg.setManaged(false);
                validationMsg.setFill(Color.web(ThemeManager.getInstance().getAccentHex()));
                validationMsg.setText("\u2713");
            } else if (!trimmed.isEmpty()) {
                validationMsg.setVisible(true);
                validationMsg.setManaged(true);
                validationMsg.setFill(Color.web("#ff3b30"));
                validationMsg.setText(message);
            } else {
                validationMsg.setVisible(false);
                validationMsg.setManaged(false);
            }
        });
    }
    
    private void setupEmailValidation(TextField field, VBox container) {
        Text validationMsg = (Text) container.lookup("#validation-email");
        
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String trimmed = newVal.trim().toLowerCase();
            boolean valid = true;
            String message = "";
            
            if (trimmed.isEmpty()) {
                valid = false;
                message = "Email is required";
            } else if (!trimmed.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                valid = false;
                message = "Invalid email format";
            }
            
            // Check if email already exists (basic check)
            if (valid && !trimmed.isEmpty()) {
                Optional<User> existing = authController.findUserByEmail(trimmed);
                if (existing.isPresent()) {
                    valid = false;
                    message = "Email already registered";
                }
            }
            
            // Update validation message
            if (valid && !trimmed.isEmpty()) {
                validationMsg.setVisible(true);
                validationMsg.setManaged(true);
                validationMsg.setFill(Color.web(ThemeManager.getInstance().getAccentHex()));
                validationMsg.setText("\u2713");
            } else if (!trimmed.isEmpty()) {
                validationMsg.setVisible(true);
                validationMsg.setManaged(true);
                validationMsg.setFill(Color.web("#ff3b30"));
                validationMsg.setText(message);
            } else {
                validationMsg.setVisible(false);
                validationMsg.setManaged(false);
            }
        });
    }
    
    private Button createLoginButton() {
        Button button = new Button("Sign In");
        button.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 16));
        button.setPrefHeight(50);
        
        // Apply liquid glass styling
        ThemeManager themeManager = ThemeManager.getInstance();
        button.setStyle(primaryButtonStyle(themeManager));
        
        // Add shadow effect
        DropShadow buttonShadow = new DropShadow();
        buttonShadow.setBlurType(javafx.scene.effect.BlurType.GAUSSIAN);
        buttonShadow.setColor(Color.color(0, 0, 0, 0.3));
        buttonShadow.setRadius(15);
        buttonShadow.setOffsetX(0);
        buttonShadow.setOffsetY(5);
        button.setEffect(buttonShadow);
        
        // Add hover effects
        button.setOnMouseEntered(e -> {
            button.setStyle(primaryButtonHoverStyle(themeManager));
            button.setScaleX(1.03);
            button.setScaleY(1.03);
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle(primaryButtonStyle(themeManager));
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });
        
        // Add click handler
        button.setOnAction(e -> handleLogin());
        
        return button;
    }
    
    private Button createSignUpButton() {
        Button button = new Button("Sign Up");
        button.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 16));
        button.setPrefHeight(50);
        
        // Apply dark red liquid glass styling
        ThemeManager themeManager = ThemeManager.getInstance();
        button.setStyle(secondaryButtonStyle(themeManager));
        
        // Add shadow effect
        DropShadow buttonShadow = new DropShadow();
        buttonShadow.setBlurType(javafx.scene.effect.BlurType.GAUSSIAN);
        buttonShadow.setColor(Color.color(0, 0, 0, 0.3));
        buttonShadow.setRadius(15);
        buttonShadow.setOffsetX(0);
        buttonShadow.setOffsetY(5);
        button.setEffect(buttonShadow);
        
        // Add hover effects
        button.setOnMouseEntered(e -> {
            button.setStyle(secondaryButtonHoverStyle(themeManager));
            button.setScaleX(1.03);
            button.setScaleY(1.03);
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle(secondaryButtonStyle(themeManager));
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });
        
        // Add click handler
        button.setOnAction(e -> switchToSignUp());
        
        return button;
    }

    private Button createOtpLoginButton() {
        Button button = new Button("OTP");
        button.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 14));
        button.setPrefHeight(50);

        ThemeManager themeManager = ThemeManager.getInstance();
        button.setStyle(secondaryButtonStyle(themeManager));

        DropShadow buttonShadow = new DropShadow();
        buttonShadow.setBlurType(javafx.scene.effect.BlurType.GAUSSIAN);
        buttonShadow.setColor(Color.color(0, 0, 0, 0.3));
        buttonShadow.setRadius(15);
        buttonShadow.setOffsetX(0);
        buttonShadow.setOffsetY(5);
        button.setEffect(buttonShadow);

        button.setOnMouseEntered(e -> {
            button.setStyle(secondaryButtonHoverStyle(themeManager));
            button.setScaleX(1.03);
            button.setScaleY(1.03);
        });

        button.setOnMouseExited(e -> {
            button.setStyle(secondaryButtonStyle(themeManager));
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });

        button.setOnAction(e -> handleOtpLogin());
        return button;
    }
    
    private Button createSignUpSubmitButton() {
        Button button = new Button("Create Account");
        button.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 16));
        button.setPrefHeight(50);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinWidth(Region.USE_PREF_SIZE);
        
        // Apply liquid glass styling
        ThemeManager themeManager = ThemeManager.getInstance();
        button.setStyle(primaryButtonStyle(themeManager));
        
        // Add shadow effect
        DropShadow buttonShadow = new DropShadow();
        buttonShadow.setBlurType(javafx.scene.effect.BlurType.GAUSSIAN);
        buttonShadow.setColor(Color.color(0, 0, 0, 0.3));
        buttonShadow.setRadius(15);
        buttonShadow.setOffsetX(0);
        buttonShadow.setOffsetY(5);
        button.setEffect(buttonShadow);
        
        // Add hover effects
        button.setOnMouseEntered(e -> {
            button.setStyle(primaryButtonHoverStyle(themeManager));
            button.setScaleX(1.03);
            button.setScaleY(1.03);
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle(primaryButtonStyle(themeManager));
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });
        
        // Add click handler
        button.setOnAction(e -> handleSignUp());
        
        return button;
    }
    
    private Button createSendResetButton() {
        Button button = new Button("Send Reset Link");
        button.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 16));
        button.setPrefHeight(50);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinWidth(Region.USE_PREF_SIZE);
        
        // Apply liquid glass styling
        ThemeManager themeManager = ThemeManager.getInstance();
        button.setStyle(primaryButtonStyle(themeManager));
        
        // Add shadow effect
        DropShadow buttonShadow = new DropShadow();
        buttonShadow.setBlurType(javafx.scene.effect.BlurType.GAUSSIAN);
        buttonShadow.setColor(Color.color(0, 0, 0, 0.3));
        buttonShadow.setRadius(15);
        buttonShadow.setOffsetX(0);
        buttonShadow.setOffsetY(5);
        button.setEffect(buttonShadow);
        
        // Add hover effects
        button.setOnMouseEntered(e -> {
            button.setStyle(primaryButtonHoverStyle(themeManager));
            button.setScaleX(1.03);
            button.setScaleY(1.03);
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle(primaryButtonStyle(themeManager));
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });
        
        // Add click handler
        button.setOnAction(e -> handleForgotPassword());
        
        return button;
    }

    private HBox createFormRow(VBox left, VBox right) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setMaxWidth(Double.MAX_VALUE);
        right.setMaxWidth(Double.MAX_VALUE);
        row.getChildren().addAll(left, right);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private void applyAuthInputStyle(TextField inputField, boolean focused) {
        ThemeManager tm = ThemeManager.getInstance();
        String inputBg = tm.isDarkMode() ? "rgba(20,20,20,0.92)" : "rgba(255,255,255,0.88)";
        String border = focused ? tm.toRgba(tm.getAccentHex(), tm.isDarkMode() ? 0.86 : 0.65) : tm.toRgba(tm.getAccentHex(), tm.isDarkMode() ? 0.26 : 0.30);
        String promptTextColor = tm.isDarkMode() ? "rgba(255,255,255,0.75)" : "rgba(51,65,85,0.68)";
        String effect = focused
            ? "-fx-effect:dropshadow(gaussian," + tm.toRgba(tm.getAccentHex(), 0.45) + ",18,0.25,0,0);"
            : "-fx-effect:dropshadow(gaussian," + tm.toRgba(tm.getAccentHex(), 0.16) + ",10,0.20,0,1);";
        inputField.setStyle(
            "-fx-background-color:" + inputBg + ";" +
            "-fx-background-radius:14px;" +
            "-fx-border-color:" + border + ";" +
            "-fx-border-width:" + (focused ? "1.5px" : "1.2px") + ";" +
            "-fx-border-radius:14px;" +
            "-fx-text-fill:" + tm.getTextColor() + ";" +
            "-fx-prompt-text-fill:" + promptTextColor + ";" +
            "-fx-padding:12px 16px;" +
            effect
        );
    }

    private LinearGradient createAccentGradientPaint() {
        ThemeManager tm = ThemeManager.getInstance();
        Color accent = Color.web(tm.getAccentHex());
        Color lighter = accent.deriveColor(0, 1.0, 1.35, 1.0);
        return new LinearGradient(
            0, 0, 1, 0,
            true,
            CycleMethod.NO_CYCLE,
            new Stop(0, lighter),
            new Stop(1, accent)
        );
    }

    private String primaryButtonStyle(ThemeManager tm) {
        return "-fx-background-color:" + tm.getEffectiveAccentGradient() + ";" +
               "-fx-background-radius:999px;" +
               "-fx-border-radius:999px;" +
               "-fx-border-color:transparent;" +
               "-fx-text-fill:#ffffff;" +
               "-fx-cursor:hand;" +
               "-fx-padding:12px 20px;";
    }

    private String primaryButtonHoverStyle(ThemeManager tm) {
        return "-fx-background-color:" + tm.getEffectiveAccentGradient() + ";" +
               "-fx-background-radius:999px;" +
               "-fx-border-radius:999px;" +
               "-fx-border-color:" + tm.toRgba(tm.getAccentHex(), 0.35) + ";" +
               "-fx-border-width:1px;" +
               "-fx-text-fill:#ffffff;" +
               "-fx-cursor:hand;" +
               "-fx-padding:12px 20px;";
    }

    private String secondaryButtonStyle(ThemeManager tm) {
        return "-fx-background-color:" + (tm.isDarkMode() ? "rgba(24,24,24,0.85)" : "rgba(255,255,255,0.90)") + ";" +
               "-fx-background-radius:999px;" +
               "-fx-border-color:" + tm.toRgba(tm.getAccentHex(), 0.30) + ";" +
               "-fx-border-width:1.2px;" +
               "-fx-border-radius:999px;" +
               "-fx-text-fill:" + tm.getTextColor() + ";" +
               "-fx-cursor:hand;" +
               "-fx-padding:12px 20px;";
    }

    private String secondaryButtonHoverStyle(ThemeManager tm) {
        return "-fx-background-color:" + (tm.isDarkMode() ? "rgba(30,30,30,0.92)" : "rgba(255,255,255,0.98)") + ";" +
               "-fx-background-radius:999px;" +
               "-fx-border-color:" + tm.toRgba(tm.getAccentHex(), 0.44) + ";" +
               "-fx-border-width:1.2px;" +
               "-fx-border-radius:999px;" +
               "-fx-text-fill:" + tm.getTextColor() + ";" +
               "-fx-cursor:hand;" +
               "-fx-padding:12px 20px;";
    }
    
    private void switchToSignUp() {
        resetFaceIdPanelState();
        loginContainer.setVisible(false);
        loginContainer.setManaged(false);
        signUpContainer.setVisible(true);
        signUpContainer.setManaged(true);
        forgotPasswordContainer.setVisible(false);
        forgotPasswordContainer.setManaged(false);
        signUpContainer.toFront();
    }
    
    private void switchToLogin() {
        signUpContainer.setVisible(false);
        signUpContainer.setManaged(false);
        forgotPasswordContainer.setVisible(false);
        forgotPasswordContainer.setManaged(false);
        loginContainer.setVisible(true);
        loginContainer.setManaged(true);
        resetFaceIdPanelState();
        loginContainer.toFront();
    }
    
    private void switchToForgotPassword() {
        resetFaceIdPanelState();
        loginContainer.setVisible(false);
        loginContainer.setManaged(false);
        signUpContainer.setVisible(false);
        signUpContainer.setManaged(false);
        forgotPasswordContainer.setVisible(true);
        forgotPasswordContainer.setManaged(true);
        forgotPasswordContainer.toFront();
    }

    private void resetFaceIdPanelState() {
        faceIdOpen = false;
        if (faceIdPanel != null) {
            faceIdPanel.setVisible(false);
            faceIdPanel.setManaged(false);
            faceIdPanel.setOpacity(0);
            faceIdPanel.setTranslateX(24);
            faceIdPanel.setPrefWidth(0);
            faceIdPanel.setMaxWidth(0);
        }
        if (loginContainer != null) {
            loginContainer.setPrefWidth(560);
            loginContainer.setMaxWidth(560);
        }
        if (loginFormColumn != null) {
            loginFormColumn.setStyle("-fx-padding: 22 16 10 16;");
        }
    }
    
    private void handleSignUp() {
        String firstName = signUpFirstNameField == null ? "" : signUpFirstNameField.getText().trim();
        String lastName = signUpLastNameField == null ? "" : signUpLastNameField.getText().trim();
        String email = signUpEmailField == null ? "" : signUpEmailField.getText().trim().toLowerCase();
        String password = signUpPasswordField == null ? "" : signUpPasswordField.getText();
        String confirmPassword = signUpConfirmPasswordField == null ? "" : signUpConfirmPasswordField.getText();

        // Perform client-side validation
        java.util.List<String> validationErrors = validateSignUpForm(firstName, lastName, email, password, confirmPassword);
        if (!validationErrors.isEmpty()) {
            showValidationErrorPopup(validationErrors);
            return;
        }

        // All validations passed, proceed with signup
        AuthController.AuthResult result = authController.signUp(firstName, lastName, email, password, confirmPassword);
        if (!result.isSuccess()) {
            showErrorMessage(result.getMessage());
            return;
        }

        showInfoMessage(result.getMessage());
        // Store new user in session if available
        if (result.getUser() != null) {
            SessionManager.getInstance().setCurrentUser(result.getUser());
            profileController.profileByUserId(result.getUser().getIdUser()).ifPresent(profile ->
                SessionManager.getInstance().setCurrentProfile(profile)
            );
        }
        usernameField.setText(email == null ? "" : email.trim());
        passwordField.setText("");
        if (signUpUsernameField != null) {
            signUpUsernameField.clear();
        }
        if (signUpFirstNameField != null) {
            signUpFirstNameField.clear();
        }
        if (signUpLastNameField != null) {
            signUpLastNameField.clear();
        }
        if (signUpEmailField != null) {
            signUpEmailField.clear();
        }
        if (signUpPasswordField != null) {
            signUpPasswordField.clear();
        }
        if (signUpConfirmPasswordField != null) {
            signUpConfirmPasswordField.clear();
        }

        switchToLogin();
    }
    
    private java.util.List<String> validateSignUpForm(String firstName, String lastName, String email, String password, String confirmPassword) {
        java.util.List<String> errors = new java.util.ArrayList<>();

        User draft = new User();
        draft.setFirstName(firstName);
        draft.setLastName(lastName);
        draft.setEmailUser(email);
        draft.setPasswordUser(password);
        draft.setRoleUser("RESIDENT");
        errors.addAll(draft.validateForCreate());

        if (email != null && !email.isBlank()) {
            Optional<User> existing = authController.findUserByEmail(email);
            if (existing.isPresent()) {
                errors.add("Email is already registered");
            }
        }
        
        // Confirm password validation
        if (confirmPassword.isEmpty()) {
            errors.add("Confirm password is required");
        } else if (!password.equals(confirmPassword)) {
            errors.add("Password confirmation does not match");
        }
        
        return errors;
    }
    
    private void showValidationErrorPopup(java.util.List<String> errors) {
        // Create popup overlay
        StackPane popupOverlay = new StackPane();
        popupOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
        popupOverlay.setPrefSize(root.getWidth(), root.getHeight());
        
        // Create popup card with glassmorphism effect
        VBox popupCard = new VBox(12);
        popupCard.setStyle(
            "-fx-background-color: rgba(10, 10, 10, 0.85);" +
            "-fx-background-radius: 16px;" +
            "-fx-border-color: " + ThemeManager.getInstance().toRgba(ThemeManager.getInstance().getAccentHex(), 0.3) + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 16px;" +
            "-fx-padding: 20px;"
        );
        popupCard.setPrefWidth(400);
        popupCard.setMaxWidth(400);
        popupCard.setMinWidth(400);
        popupCard.setMaxHeight(Region.USE_PREF_SIZE);
        popupCard.setAlignment(Pos.TOP_CENTER);
        
        // Add glassmorphism shadow
        DropShadow shadow = new DropShadow();
        shadow.setBlurType(javafx.scene.effect.BlurType.GAUSSIAN);
        shadow.setColor(Color.color(0, 0, 0, 0.4));
        shadow.setRadius(20);
        shadow.setOffsetY(8);
        popupCard.setEffect(shadow);
        
        // Title
        Text title = new Text("Missing Information");
        title.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 18));
        title.setFill(Color.web("#ff3b30"));
        
        // Errors list
        VBox errorsList = new VBox(8);
        errorsList.setStyle("-fx-padding: 4px 0;");
        errorsList.setMaxHeight(Region.USE_PREF_SIZE);
        
        for (String error : errors) {
            HBox errorItem = new HBox(10);
            errorItem.setAlignment(Pos.TOP_LEFT);
            errorItem.setStyle("-fx-padding: 0;");
            
            Text bullet = new Text("\u2716");
            bullet.setFont(Font.font(14));
            bullet.setFill(Color.web("#ff3b30"));
            
            Text errorText = new Text(error);
            errorText.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 13));
            errorText.setFill(Color.web("#cbd5e1"));
            errorText.setWrappingWidth(330);
            
            errorItem.getChildren().addAll(bullet, errorText);
            errorsList.getChildren().add(errorItem);
        }
        
        // Dismiss button
        Button dismissButton = new Button("Got it");
        dismissButton.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 14));
        dismissButton.setPrefHeight(38);
        dismissButton.setMaxWidth(Double.MAX_VALUE);
        dismissButton.setStyle(
            "-fx-background-color: " + ThemeManager.getInstance().getAccentHex() + ";" +
            "-fx-background-radius: 8px;" +
            "-fx-text-fill: white;" +
            "-fx-cursor: hand;" +
            "-fx-font-weight: bold;"
        );
        
        dismissButton.setOnMouseEntered(e -> dismissButton.setStyle(
            "-fx-background-color: " + ThemeManager.getInstance().toRgba(
                ThemeManager.getInstance().getAccentHex(), 0.85) + ";" +
            "-fx-background-radius: 8px;" +
            "-fx-text-fill: white;" +
            "-fx-cursor: hand;" +
            "-fx-font-weight: bold;"
        ));
        
        dismissButton.setOnMouseExited(e -> dismissButton.setStyle(
            "-fx-background-color: " + ThemeManager.getInstance().getAccentHex() + ";" +
            "-fx-background-radius: 8px;" +
            "-fx-text-fill: white;" +
            "-fx-cursor: hand;" +
            "-fx-font-weight: bold;"
        ));
        
        // Add all content to card
        popupCard.getChildren().addAll(
            title,
            errorsList,
            dismissButton
        );
        
        // Center the popup card in overlay
        popupOverlay.getChildren().add(popupCard);
        StackPane.setAlignment(popupCard, Pos.CENTER);
        
        // Add overlay to root
        root.getChildren().add(popupOverlay);
        
        // Fade in animation
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), popupOverlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        
        // Scale animation for popup card
        javafx.animation.ScaleTransition scaleIn = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(200), popupCard);
        scaleIn.setFromX(0.85);
        scaleIn.setFromY(0.85);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        
        // Dismiss button click handler
        dismissButton.setOnAction(e -> {
            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(150), popupOverlay);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> root.getChildren().remove(popupOverlay));
            fadeOut.play();
        });
        
        new javafx.animation.ParallelTransition(fadeIn, scaleIn).play();
    }
    
    private void handleForgotPassword() {
        String recovery = forgotRecoveryField == null ? "" : forgotRecoveryField.getText();

        AuthController.AuthResult requestResult = authController.requestPasswordReset(recovery);
        if (!requestResult.isSuccess()) {
            showErrorMessage(requestResult.getMessage());
            return;
        }

        showInfoMessage(requestResult.getMessage());

        TextInputDialog codeDialog = new TextInputDialog();
        codeDialog.setTitle("Verify Code");
        codeDialog.setHeaderText("Enter the 6-digit code sent to your email");
        codeDialog.setContentText("Auth code:");
        java.util.Optional<String> entered = codeDialog.showAndWait();
        if (entered.isEmpty() || entered.get().isBlank()) {
            return;
        }

        AuthController.AuthResult verifyResult = authController.verifyPasswordReset(recovery, entered.get().trim());
        if (!verifyResult.isSuccess()) {
            showErrorMessage(verifyResult.getMessage());
            return;
        }

        showInfoMessage(verifyResult.getMessage());
        switchToLogin();
    }
    
    private StackPane createTopRightControls() {
        // Create horizontal container for connection pill and theme toggle
        HBox controlsHBox = new HBox();
        controlsHBox.setSpacing(12);
        controlsHBox.setAlignment(Pos.CENTER_RIGHT);
        
        // Create connection status pill
        ConnectionStatusPill connectionPill = new ConnectionStatusPill();
        
        // Create theme toggle
        StackPane themeToggle = createThemeToggle();
        
        // Add both to horizontal container
        controlsHBox.getChildren().addAll(connectionPill.getPillContainer(), themeToggle);
        
        // Create container for positioning
        StackPane controlsContainer = new StackPane();
        controlsContainer.getChildren().add(controlsHBox);
        
        // Position in top right corner using StackPane alignment
        StackPane.setAlignment(controlsContainer, Pos.TOP_RIGHT);
        
        // Set margins from the edges
        StackPane.setMargin(controlsContainer, new Insets(20, 20, 0, 0));
        
        return controlsContainer;
    }
    
    private StackPane createThemeToggle() {
        StackPane toggleContainer = new StackPane();
        
        // Set the container size to exactly match the toggle button
        toggleContainer.setPrefSize(65, 35);
        toggleContainer.setMaxSize(65, 35);
        toggleContainer.setMinSize(65, 35);
        
        // Toggle track (background) - more rounded with theme-aware colors
        javafx.scene.shape.Rectangle track = new javafx.scene.shape.Rectangle(65, 35);
        track.setArcWidth(35); // Fully rounded
        track.setArcHeight(35);
        
        final ThemeManager themeManager = ThemeManager.getInstance();
        track.setFill(Color.web(themeManager.getToggleTrackBackground()));
        track.setStroke(Color.web(themeManager.getToggleTrackBorder()));
        track.setStrokeWidth(1);
        
        // Toggle thumb container (will hold the icon)
        StackPane thumbContainer = new StackPane();
        thumbContainer.setTranslateX(themeManager.isDarkMode() ? -16 : 16); // Position based on current theme
        
        // Moon icon (dark mode)
        javafx.scene.text.Text moonIcon = new javafx.scene.text.Text("D");
        moonIcon.setFont(javafx.scene.text.Font.font(16));
        moonIcon.setFill(Color.WHITE);
        moonIcon.setOpacity(themeManager.isDarkMode() ? 1.0 : 0.0);
        
        // Sun icon (light mode)
        javafx.scene.text.Text sunIcon = new javafx.scene.text.Text("L");
        sunIcon.setFont(javafx.scene.text.Font.font(16));
        sunIcon.setFill(Color.web("#f59e0b")); // Modern amber color
        sunIcon.setOpacity(themeManager.isDarkMode() ? 0.0 : 1.0);
        
        // Add shadow to thumb container
        javafx.scene.effect.DropShadow thumbShadow = new javafx.scene.effect.DropShadow();
        thumbShadow.setBlurType(javafx.scene.effect.BlurType.GAUSSIAN);
        if (themeManager.isDarkMode()) {
            thumbShadow.setColor(Color.color(0, 0, 0, 0.3));
        } else {
            thumbShadow.setColor(Color.color(0, 0, 0, 0.15));
        }
        thumbShadow.setRadius(6);
        thumbShadow.setOffsetX(0);
        thumbShadow.setOffsetY(3);
        thumbContainer.setEffect(thumbShadow);
        
        // Add icons to thumb container
        thumbContainer.getChildren().addAll(moonIcon, sunIcon);
        
        // Add all elements to container
        toggleContainer.getChildren().addAll(track, thumbContainer);
        
        // Add click handler for toggle
        toggleContainer.setOnMouseClicked(e -> toggleTheme(thumbContainer, sunIcon, moonIcon, track));
        
        // Add hover effect - only on the track
        track.setOnMouseEntered(e -> {
            track.setFill(Color.web(themeManager.getToggleTrackHover()));
        });
        track.setOnMouseExited(e -> {
            track.setFill(Color.web(themeManager.getToggleTrackBackground()));
        });
        
        return toggleContainer;
    }
    
    private void toggleTheme(StackPane thumbContainer, javafx.scene.text.Text sunIcon, 
                           javafx.scene.text.Text moonIcon, javafx.scene.shape.Rectangle track) {
        // Toggle theme in theme manager
        final ThemeManager themeManager = ThemeManager.getInstance();
        themeManager.toggleTheme();
        
        // Keep login video regardless of theme changes
        if (imageBackground != null) {
            imageBackground.setLoginMode();
        }
        
        // Animate thumb movement
        javafx.animation.TranslateTransition thumbAnimation = new javafx.animation.TranslateTransition(
            javafx.util.Duration.millis(300), thumbContainer);
        
        // Animate icon transitions
        javafx.animation.FadeTransition sunFade = new javafx.animation.FadeTransition(
            javafx.util.Duration.millis(300), sunIcon);
        javafx.animation.FadeTransition moonFade = new javafx.animation.FadeTransition(
            javafx.util.Duration.millis(300), moonIcon);
        
        if (themeManager.isDarkMode()) {
            // Move to dark mode position (left) and show moon
            thumbAnimation.setToX(-16);
            sunFade.setToValue(0.0); // Hide sun
            moonFade.setToValue(1.0); // Show moon
        } else {
            // Move to light mode position (right) and show sun
            thumbAnimation.setToX(16);
            sunFade.setToValue(1.0); // Show sun
            moonFade.setToValue(0.0); // Hide moon
        }
        
        thumbAnimation.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
        sunFade.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
        moonFade.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
        
        // Play all animations together
        javafx.animation.ParallelTransition parallelTransition = new javafx.animation.ParallelTransition();
        parallelTransition.getChildren().addAll(thumbAnimation, sunFade, moonFade);
        parallelTransition.play();
        
        // Update styling after animation
        parallelTransition.setOnFinished(e -> {
            track.setFill(Color.web(themeManager.getToggleTrackBackground()));
            track.setStroke(Color.web(themeManager.getToggleTrackBorder()));
        });
        
        System.out.println("Theme switched to: " + (themeManager.isDarkMode() ? "Dark" : "Light"));
    }
    
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        // Keep admin/admin shortcut for local dev convenience.
        if ("admin".equals(username) && "admin".equals(password)) {
            loginSuccessFired = true;
            // Store dummy admin user in session for header profile
            com.syndicati.models.user.User adminUser = new com.syndicati.models.user.User();
            adminUser.setFirstName("Admin");
            adminUser.setLastName("");
            adminUser.setEmailUser("admin@syndicati.tn");
            adminUser.setRoleUser("ADMIN");
            adminUser.setVerified(true);
            adminUser.setDisabled(false);
            SessionManager.getInstance().setCurrentUser(adminUser);
            System.out.println("Login successful! Navigating to landing page...");
            if (onLoginSuccess != null) {
                onLoginSuccess.run();
            } else {
                navigateToLandingPage();
            }
            return;
        }

        AuthController.AuthResult result = authController.login(username, password);
        if (result.isSuccess()) {
            loginSuccessFired = true;
            // Store user in session
            if (result.getUser() != null) {
                SessionManager.getInstance().setCurrentUser(result.getUser());
                // Also load user's profile if available
                profileController.profileByUserId(result.getUser().getIdUser()).ifPresent(profile ->
                    SessionManager.getInstance().setCurrentProfile(profile)
                );
                System.out.println("Login successful for user: " + result.getUser().getEmailUser());
            }
            if (onLoginSuccess != null) {
                onLoginSuccess.run();
            } else {
                navigateToLandingPage();
            }
            return;
        }

        showErrorMessage(result.getMessage());
    }

    private void handleOtpLogin() {
        String email = usernameField == null ? "" : usernameField.getText();
        AuthController.AuthResult request = authController.requestLoginOtp(email);
        if (!request.isSuccess()) {
            showErrorMessage(request.getMessage());
            return;
        }

        showInfoMessage(request.getMessage());

        showOtpLoginModal(email.trim());
    }

    private void showOtpLoginModal(String email) {
        ThemeManager tm = ThemeManager.getInstance();

        StackPane overlay = new StackPane();
        overlay.setPickOnBounds(true);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.52);");

        VBox card = new VBox(12);
        card.setAlignment(Pos.TOP_CENTER);
        card.setMaxWidth(420);
        card.setMaxHeight(280);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setStyle(
            "-fx-background-color: " + (tm.isDarkMode() ? "rgba(14,14,14,0.95)" : "rgba(245,245,245,0.97)") + ";" +
            "-fx-background-radius: 20px;" +
            "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.38) + ";" +
            "-fx-border-width: 1.2px;" +
            "-fx-border-radius: 20px;"
        );

        Text title = new Text("OTP Verification");
        title.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 20));
        title.setFill(createAccentGradientPaint());

        Text sub = new Text("Enter the 6-digit code sent to " + email);
        sub.setWrappingWidth(380);
        sub.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 12));
        sub.setFill(tm.isDarkMode() ? Color.web("rgba(255,255,255,0.72)") : Color.web("rgba(30,41,59,0.72)"));

        TextField codeField = new TextField();
        codeField.setPromptText("000000");
        codeField.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 20));
        codeField.setAlignment(Pos.CENTER);
        codeField.setPrefHeight(48);
        codeField.setMaxWidth(320);
        codeField.setStyle(
            "-fx-background-color: " + (tm.isDarkMode() ? "rgba(20,20,20,0.92)" : "rgba(255,255,255,0.92)") + ";" +
            "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.44) + ";" +
            "-fx-border-width: 1.4px;" +
            "-fx-border-radius: 14px;" +
            "-fx-background-radius: 14px;" +
            "-fx-text-fill: " + tm.getTextColor() + ";" +
            "-fx-prompt-text-fill: " + (tm.isDarkMode() ? "rgba(255,255,255,0.35)" : "rgba(30,41,59,0.35)") + ";"
        );
        codeField.textProperty().addListener((obs, oldVal, newVal) -> {
            String digits = newVal == null ? "" : newVal.replaceAll("\\D", "");
            if (digits.length() > 6) {
                digits = digits.substring(0, 6);
            }
            if (!digits.equals(newVal)) {
                codeField.setText(digits);
            }
        });

        Label hint = new Label("Code expires in 15 minutes");
        hint.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 11));
        hint.setTextFill(Color.web(tm.toRgba(tm.getAccentHex(), 0.95)));
        hint.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 12));
        cancelBtn.setStyle(secondaryButtonStyle(tm));
        cancelBtn.setPrefWidth(90);

        Button verifyBtn = new Button("Verify OTP");
        verifyBtn.setFont(Font.font(com.syndicati.MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 12));
        verifyBtn.setStyle(primaryButtonStyle(tm));
        verifyBtn.setPrefWidth(110);

        HBox actions = new HBox(10, cancelBtn, verifyBtn);
        actions.setAlignment(Pos.CENTER);

        Runnable close = () -> {
            root.getChildren().remove(overlay);
            loginContainer.toFront();
        };

        cancelBtn.setOnAction(e -> close.run());
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                close.run();
            }
        });

        Runnable verifyAction = () -> {
            String code = codeField.getText() == null ? "" : codeField.getText().trim();
            if (code.length() != 6) {
                hint.setText("Please enter a valid 6-digit code");
                hint.setTextFill(Color.web("#f87171"));
                return;
            }

            AuthController.AuthResult verify = authController.verifyLoginOtp(email, code);
            if (!verify.isSuccess()) {
                hint.setText(verify.getMessage());
                hint.setTextFill(Color.web("#f87171"));
                return;
            }

            if (verify.getUser() != null) {
                SessionManager.getInstance().setCurrentUser(verify.getUser());
                profileController.profileByUserId(verify.getUser().getIdUser()).ifPresent(profile ->
                    SessionManager.getInstance().setCurrentProfile(profile)
                );
            }

            close.run();
            loginSuccessFired = true;
            if (onLoginSuccess != null) {
                onLoginSuccess.run();
            } else {
                navigateToLandingPage();
            }
        };

        verifyBtn.setOnAction(e -> verifyAction.run());
        codeField.setOnAction(e -> verifyAction.run());

        card.getChildren().addAll(title, sub, codeField, hint, actions);
        overlay.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);

        card.setOpacity(0);
        card.setScaleX(0.96);
        card.setScaleY(0.96);
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(160), card);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        javafx.animation.ScaleTransition scaleIn = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(180), card);
        scaleIn.setFromX(0.96);
        scaleIn.setFromY(0.96);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        new javafx.animation.ParallelTransition(fadeIn, scaleIn).play();

        root.getChildren().add(overlay);
        overlay.toFront();
        javafx.application.Platform.runLater(codeField::requestFocus);
    }
    
    private void showErrorMessage(String message) {
        // Create a temporary error message
    Label errorLabel = new Label(message);
    errorLabel.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 14));
        errorLabel.setTextFill(Color.color(1, 0.3, 0.3, 1)); // Red color
        errorLabel.setStyle(
            "-fx-background-color: rgba(255, 0, 0, 0.1);" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 8px 16px;"
        );
        
        // Add to root temporarily
        root.getChildren().add(errorLabel);
        
        // Remove after 3 seconds
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3), e -> {
                root.getChildren().remove(errorLabel);
            })
        );
        timeline.play();
    }

    private void showInfoMessage(String message) {
        Label infoLabel = new Label(message);
        infoLabel.setFont(Font.font(com.syndicati.MainApplication.getInstance().getLightFontFamily(), FontWeight.NORMAL, 13));
        infoLabel.setTextFill(Color.web("#e2e8f0"));
        infoLabel.setStyle(
            "-fx-background-color: rgba(8, 8, 8, 0.72);" +
            "-fx-border-color: " + ThemeManager.getInstance().toRgba(ThemeManager.getInstance().getAccentHex(), 0.40) + ";" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 10px;" +
            "-fx-border-radius: 10px;" +
            "-fx-padding: 8px 14px;"
        );

        root.getChildren().add(infoLabel);
        StackPane.setAlignment(infoLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(infoLabel, new Insets(0, 0, 26, 0));

        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(140), infoLabel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        javafx.animation.PauseTransition hold = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2.0));

        javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(220), infoLabel);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> root.getChildren().remove(infoLabel));

        new javafx.animation.SequentialTransition(fadeIn, hold, fadeOut).play();
    }
    
    private void navigateToLandingPage() {
        // This will be handled by the main application
        System.out.println("Login successful - ready to navigate to landing page");
        // The main application will handle the navigation
    }
    
    private void applyThemeStyling() {
        // Login page has its own styling, but we can add theme-aware elements if needed
        root.setStyle("-fx-background-color: transparent;" + ThemeManager.getInstance().getScrollbarVariableStyle());
    }
    
    @Override
    public Pane getRoot() {
        return root;
    }
    
    // Custom window bar with drag/min/restore/close - macOS style (same as LandingPageView)
    private HBox createWindowBar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setSpacing(8);
        bar.setPadding(new Insets(4, 16, 4, 16));
        bar.setPrefHeight(40);
        bar.setMinHeight(40);
        bar.setMaxHeight(40);
        bar.setStyle("-fx-background-color: transparent; -fx-background-radius: 0;");
        bar.setPickOnBounds(false);
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
        buttonContainer.setMouseTransparent(false);

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
        dragRegion.setMouseTransparent(false);
        buttonContainer.setMouseTransparent(false);
        dragRegion.setPickOnBounds(true);
        buttonContainer.setPickOnBounds(false);

        // Standard drag logic
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
        
        // Store the symbol for hover effect
        final String btnSymbol = symbol;
        
        // Show symbol and darken on hover
        btn.setOnMouseEntered(e -> {
            btn.setText(btnSymbol);
            String darkerColor = color;
            if (color.equals("#ff5f57")) darkerColor = "#e04b42";
            if (color.equals("#febc2e")) darkerColor = "#d9a21a";
            if (color.equals("#28c840")) darkerColor = "#1fa931";
            
            btn.setStyle(
                "-fx-background-color: " + darkerColor + ";" +
                "-fx-background-radius: 6px;" +
                "-fx-border-color: rgba(0,0,0,0.3);" +
                "-fx-border-width: 0.5px;" +
                "-fx-border-radius: 6px;" +
                "-fx-cursor: hand;" +
                "-fx-font-size: 8px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: rgba(0,0,0,0.7);" +
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
    
    @Override
    public void cleanup() {
        // Clean up camera resources
        if (cameraService != null) {
            cameraService.stopCapture();
            cameraService.release();
            cameraService = null;
        }
        if (cameraUpdateTimer != null) {
            cameraUpdateTimer.stop();
            cameraUpdateTimer = null;
        }
        
        // Clean up media player if it exists
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        // Clean up image background
        if (imageBackground != null) {
            imageBackground.cleanup();
            // Remove image background from root
            root.getChildren().remove(imageBackground.getRoot());
            imageBackground = null;
        }
        // Clear all children from root to ensure no lingering backgrounds
        root.getChildren().clear();
        root.setStyle("-fx-background-color: transparent;" + ThemeManager.getInstance().getScrollbarVariableStyle());
        root.setBackground(null);
    }
    
    // Public method to check if login was successful
    public boolean isLoginSuccessful() {
        return loginSuccessFired;
    }

    /**
     * Handle Passkey (WebAuthn) login
     */
    private void handlePasskeyLogin() {
        try {
            if (usernameField == null || usernameField.getText().trim().isEmpty()) {
                showErrorMessage("Please enter your email first.");
                return;
            }

            String email = usernameField.getText().trim().toLowerCase();
            
            // Disable button during authentication
            Button passkeyBtn = null;
            for (javafx.scene.Node node : ((HBox) usernameField.getParent().getParent().getParent().getParent().lookup(".biometric-row")).getChildren()) {
                if (node instanceof Button && ((Button)node).getText().contains("Passkey")) {
                    passkeyBtn = (Button) node;
                    break;
                }
            }
            
            final Button finalPasskeyBtn = passkeyBtn;
            
            if (finalPasskeyBtn != null) {
                finalPasskeyBtn.setDisable(true);
                 finalPasskeyBtn.setText("\ud83d\udd10 Authenticating...");
            }

            // Simulate WebAuthn authentication flow
            Thread.ofVirtual().name("LoginView-Passkey-Auth").start(() -> {
                try {
                    // In production, would call:
                    // 1. POST /webauthn/login/options with email
                    // 2. Get challenge from server
                    // 3. Trigger platform authenticator (Windows Hello, etc.)
                    // 4. POST /webauthn/login/verify with assertion

                    Thread.sleep(1500);  // Simulate device verification

                    // Create mock user session (in production: from actual authentication)
                    User authenticatedUser = new User();
                    authenticatedUser.setEmailUser(email);
                    authenticatedUser.setFirstName("User");
                    authenticatedUser.setLastName("Authenticated");

                    javafx.application.Platform.runLater(() -> {
                        // Set session
                        SessionManager.getInstance().setCurrentUser(authenticatedUser);
                        
                        // Show success message
                        showInfoMessage("Passkey authentication successful!");
                        
                        // Navigate to dashboard
                        if (onLoginSuccess != null) {
                            onLoginSuccess.run();
                        }
                        
                        // Re-enable button
                        if (finalPasskeyBtn != null) {
                            finalPasskeyBtn.setDisable(false);
                               finalPasskeyBtn.setText("\ud83d\udd10 Passkey");
                        }
                    });
                    
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        showErrorMessage("Passkey authentication failed: " + ex.getMessage());
                        if (finalPasskeyBtn != null) {
                            finalPasskeyBtn.setDisable(false);
                                finalPasskeyBtn.setText("\ud83d\udd10 Passkey");
                        }
                    });
                }
            });
            
        } catch (Exception ex) {
            showErrorMessage("Passkey login error: " + ex.getMessage());
        }
    }

    /**
     * Handle FaceID authentication during login
     */
    private void handleFaceIDLogin() {
        try {
            if (usernameField == null || usernameField.getText().trim().isEmpty()) {
                showErrorMessage("Please enter your email first.");
                return;
            }

            String email = usernameField.getText().trim().toLowerCase();
            String pin = faceIdPinInput != null ? faceIdPinInput.getText().trim() : "";

            // Validate PIN
            if (pin.isEmpty() || pin.length() < 4 || pin.length() > 6 || !pin.matches("\\d+")) {
                showErrorMessage("PIN must be 4-6 digits.");
                if (faceIdPinInput != null) {
                    faceIdPinInput.requestFocus();
                }
                return;
            }

            // Initialize camera if not already done
            if (cameraService == null) {
                cameraService = cameraController.getOrCreate(cameraService);
                if (!cameraController.initializeDefaultCamera(cameraService)) {
                    showErrorMessage("Failed to access camera.\n\nPlease check:\n1. Camera is connected and powered on\n2. No other app is using it\n3. Check console for details");
                    return;
                }
            }

            // Disable verify button during authentication
            if (faceIdVerifyButton != null) {
                faceIdVerifyButton.setDisable(true);
                faceIdVerifyButton.setText("\ud83d\udd04 Verifying...");
            }
            if (faceIdStatusLabel != null) {
                faceIdStatusLabel.setText("Starting camera...");
            }

            // Start capturing in background thread
            Thread.ofVirtual().name("LoginView-FaceID-Capture").start(() -> {
                try {
                    System.out.println("LoginView: Starting camera capture...");
                    
                    // Start camera capture
                    cameraService.startCapture();
                    Thread.sleep(1500);  // Allow camera to warm up (increased to 1.5 seconds)
                    
                    // Verify that frames are actually being captured
                    javafx.scene.image.Image testFrame = cameraService.getCurrentFrameWithDetection();
                    if (testFrame == null) {
                        System.err.println("LoginView: No frames captured after warm-up");
                        javafx.application.Platform.runLater(() -> {
                            showErrorMessage("Camera not responding. Please check:\n1. Camera is connected and not in use by another app\n2. Camera permissions are granted\n3. Try unplugging and reconnecting the camera");
                            if (faceIdVerifyButton != null) {
                                faceIdVerifyButton.setDisable(false);
                                faceIdVerifyButton.setText("Verify");
                            }
                            cameraService.stopCapture();
                        });
                        return;
                    }
                    System.out.println("LoginView: Frames are being captured successfully");

                    // Start animation timer to display camera preview
                    javafx.application.Platform.runLater(() -> {
                        if (cameraUpdateTimer != null) {
                            cameraUpdateTimer.stop();
                        }
                        cameraUpdateTimer = new AnimationTimer() {
                            @Override
                            public void handle(long now) {
                                javafx.scene.image.Image frame = cameraService.getCurrentFrameWithDetection();
                                if (frame != null && faceIdVideoView != null) {
                                    faceIdVideoView.setImage(frame);
                                }
                            }
                        };
                        cameraUpdateTimer.start();
                    });

                    javafx.application.Platform.runLater(() -> {
                        if (faceIdStatusLabel != null) {
                            faceIdStatusLabel.setText("Position your face in the frame...");
                        }
                    });

                    // Collect frames for authentication
                    final int[] framesCapturedArray = {0};
                    final int targetFrames = 5;
                    long startTime = System.currentTimeMillis();
                    long timeout = 15000;  // 15 second timeout

                    while (framesCapturedArray[0] < targetFrames && (System.currentTimeMillis() - startTime) < timeout) {
                        // Check frame quality
                        RealCameraService.FaceQuality quality = cameraService.assessFrameQuality();

                        final int currentFrameCount = framesCapturedArray[0];
                        javafx.application.Platform.runLater(() -> {
                            if (faceIdStatusLabel != null) {
                                faceIdStatusLabel.setText(quality.message + " (" + currentFrameCount + "/" + targetFrames + ")");
                            }
                        });

                        if (quality.isGood) {
                            // Capture frame
                            RealCameraService.FaceFrameData frameData = cameraService.captureFrame();
                            if (frameData != null) {
                                framesCapturedArray[0]++;
                                final int updatedFrameCount = framesCapturedArray[0];
                                System.out.println("LoginView: Frame captured " + updatedFrameCount + "/" + targetFrames);
                                javafx.application.Platform.runLater(() -> {
                                    if (faceIdStatusLabel != null) {
                                        faceIdStatusLabel.setText("Face captured: " + updatedFrameCount + "/" + targetFrames);
                                    }
                                });
                            }
                        }

                        Thread.sleep(200);
                    }

                    if (framesCapturedArray[0] < targetFrames) {
                        javafx.application.Platform.runLater(() -> {
                            if (cameraUpdateTimer != null) {
                                cameraUpdateTimer.stop();
                            }
                            showErrorMessage("Failed to capture enough face data. Please try again.\n\nMake sure:\n1. Your face is visible in the frame\n2. The frame is well-lit\n3. Your face is large enough in the frame");
                            if (faceIdVerifyButton != null) {
                                faceIdVerifyButton.setDisable(false);
                                faceIdVerifyButton.setText("Verify");
                            }
                            if (faceIdStatusLabel != null) {
                                faceIdStatusLabel.setText("Authentication failed. Try again.");
                            }
                            cameraService.stopCapture();
                        });
                        return;
                    }

                    cameraService.stopCapture();

                    // Simulate face recognition matching
                    javafx.application.Platform.runLater(() -> {
                        if (faceIdStatusLabel != null) {
                            faceIdStatusLabel.setText("Comparing face data...");
                        }
                    });

                    Thread.sleep(1000);

                    // Fetch actual user from database
                    Optional<User> authenticatedUserOpt = authController.findUserByEmail(email);
                    
                    if (!authenticatedUserOpt.isPresent()) {
                        javafx.application.Platform.runLater(() -> {
                            if (cameraUpdateTimer != null) {
                                cameraUpdateTimer.stop();
                            }
                            showErrorMessage("User not found. Please check your email address.");
                            if (faceIdVerifyButton != null) {
                                faceIdVerifyButton.setDisable(false);
                                faceIdVerifyButton.setText("Verify");
                            }
                            if (faceIdStatusLabel != null) {
                                faceIdStatusLabel.setText("Authentication failed. Try again.");
                            }
                        });
                        return;
                    }

                    User authenticatedUser = authenticatedUserOpt.get();

                    javafx.application.Platform.runLater(() -> {
                        // Set session
                        SessionManager.getInstance().setCurrentUser(authenticatedUser);
                        
                        // Load and set user's profile
                        profileController.profileByUserId(authenticatedUser.getIdUser()).ifPresent(profile ->
                            SessionManager.getInstance().setCurrentProfile(profile)
                        );
                        
                        // Update status
                        if (faceIdStatusLabel != null) {
                            faceIdStatusLabel.setText("\u2713 Authentication successful!");
                        }
                        
                        // Show success message
                        showInfoMessage("Face ID authentication successful!");
                        
                        // Close FaceID panel
                        closeFaceIdPanel();
                        
                        // Clear PIN
                        if (faceIdPinInput != null) {
                            faceIdPinInput.clear();
                        }
                        
                        // Navigate to dashboard
                        if (onLoginSuccess != null) {
                            onLoginSuccess.run();
                        }
                        
                        // Re-enable button
                        if (faceIdVerifyButton != null) {
                            faceIdVerifyButton.setDisable(false);
                            faceIdVerifyButton.setText("Verify");
                        }
                    });

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    javafx.application.Platform.runLater(() -> {
                        if (cameraUpdateTimer != null) {
                            cameraUpdateTimer.stop();
                        }
                        showErrorMessage("Face ID authentication interrupted.");
                        if (faceIdVerifyButton != null) {
                            faceIdVerifyButton.setDisable(false);
                            faceIdVerifyButton.setText("Verify");
                        }
                    });
                } catch (Exception ex) {
                    System.err.println("LoginView Camera Error: " + ex.getMessage());
                    ex.printStackTrace();
                    
                    javafx.application.Platform.runLater(() -> {
                        if (cameraUpdateTimer != null) {
                            cameraUpdateTimer.stop();
                        }
                        showErrorMessage("Face ID authentication error: " + ex.getMessage() + "\n\nCheck console for details.");
                        if (faceIdStatusLabel != null) {
                            faceIdStatusLabel.setText("Error: " + ex.getMessage());
                        }
                        if (faceIdVerifyButton != null) {
                            faceIdVerifyButton.setDisable(false);
                            faceIdVerifyButton.setText("Verify");
                        }
                    });
                } finally {
                    if (cameraService != null) {
                        cameraService.stopCapture();
                    }
                    javafx.application.Platform.runLater(() -> {
                        if (cameraUpdateTimer != null) {
                            cameraUpdateTimer.stop();
                            cameraUpdateTimer = null;
                        }
                    });
                }
            });

        } catch (Exception ex) {
            showErrorMessage("FaceID login error: " + ex.getMessage());
        }
    }
}


