package com.syndicati.views.frontend.home;

import com.syndicati.models.user.Onboarding;
import com.syndicati.models.user.Profile;
import com.syndicati.models.user.User;
import com.syndicati.controllers.user.onboarding.OnboardingController;
import com.syndicati.controllers.user.profile.ProfileController;
import com.syndicati.utils.session.SessionManager;
import com.syndicati.utils.theme.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Desktop onboarding overlay aligned with the website 7-step onboarding flow.
 */
public class OnboardingOverlayView {

    private final User user;
    private final OnboardingController onboardingController;
    private final ProfileController profileController;
    private final SessionManager sessionManager;
    private final Runnable onCompleted;
    private final boolean editMode;
    private final StackPane root;
    private final VBox card;
    private final HBox progressBar;
    private final Label titleLabel;
    private final Label subtitleLabel;
    private final VBox stepBody;
    private final Button prevButton;
    private final Button saveButton;
    private final Button nextButton;
    private final Button skipButton;

    private Onboarding onboarding;
    private JSONObject prefs;
    private final Map<String, ToggleGroup> activeGroups = new LinkedHashMap<>();
    private TextArea suggestionsArea;
    private boolean shouldShow;

    public OnboardingOverlayView(User user, Runnable onCompleted) {
        this(user, onCompleted, false);
    }

    public OnboardingOverlayView(User user, Runnable onCompleted, boolean editMode) {
        this.user = user;
        this.onCompleted = onCompleted;
        this.editMode = editMode;
        this.onboardingController = new OnboardingController();
        this.profileController = new ProfileController();
        this.sessionManager = SessionManager.getInstance();

        this.root = new StackPane();
        this.card = new VBox(18);
        this.progressBar = new HBox(8);
        this.titleLabel = new Label();
        this.subtitleLabel = new Label();
        this.stepBody = new VBox(14);
        this.prevButton = new Button("Previous");
        this.saveButton = new Button("Save");
        this.nextButton = new Button("Next");
        this.skipButton = new Button("Skip for now");

        initializeState();
        buildUi();
        if (shouldShow) {
            renderCurrentStep();
        }
    }

    public boolean shouldShow() {
        return shouldShow;
    }

    public StackPane getRoot() {
        return root;
    }

    private void initializeState() {
        if (user == null || user.getIdUser() == null || user.getIdUser() <= 0) {
            shouldShow = false;
            return;
        }

        Optional<Onboarding> loaded = onboardingController.findOrCreateByUserId(user.getIdUser());
        if (loaded.isEmpty()) {
            shouldShow = false;
            return;
        }

        onboarding = loaded.get();
        if (!editMode && onboarding.isCompleted()) {
            shouldShow = false;
            return;
        }

        if (editMode && onboarding.getStep() != 1) {
            onboarding.setStep(1);
        }

        String rawJson = onboarding.getSelectedPreferencesJson();
        if (rawJson == null || rawJson.isBlank()) {
            prefs = new JSONObject();
        } else {
            try {
                prefs = new JSONObject(rawJson);
            } catch (Exception ex) {
                prefs = new JSONObject();
            }
        }

        shouldShow = true;
    }

    private void buildUi() {
        root.setVisible(shouldShow);
        root.setManaged(shouldShow);
        root.setPickOnBounds(shouldShow);
        updateBackgroundStyle();

        progressBar.setAlignment(Pos.CENTER);
        progressBar.setStyle("-fx-spacing: 8;");

        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: white; -fx-wrap-text: true;");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.72); -fx-line-spacing: 4;");

        stepBody.setFillWidth(true);
        stepBody.setStyle("-fx-spacing: 12;");

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);

        styleButton(skipButton,
            "-fx-font-size: 13px; -fx-font-weight: 700; -fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: rgba(255,255,255,0.85);",
            "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: rgba(255,255,255,0.95);");
        styleButton(saveButton,
            "-fx-font-size: 13px; -fx-font-weight: 700; -fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: rgba(255,255,255,0.85);",
            "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: rgba(255,255,255,0.95);");
        styleButton(prevButton,
            "-fx-font-size: 13px; -fx-font-weight: 700; -fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: rgba(255,255,255,0.85);",
            "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: rgba(255,255,255,0.95);");
        styleButton(nextButton,
            "-fx-font-size: 13px; -fx-font-weight: 700; -fx-background-color: " + ThemeManager.getInstance().getEffectiveAccentGradient() + "; -fx-text-fill: white;",
            "-fx-background-color: " + ThemeManager.getInstance().getEffectiveAccentGradient() + "; -fx-text-fill: white;");

        skipButton.setOnAction(e -> cancelOnboarding());
        saveButton.setOnAction(e -> saveOnboardingProgress());
        prevButton.setOnAction(e -> previousStep());
        nextButton.setOnAction(e -> nextStep());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        actions.getChildren().addAll(skipButton, spacer, saveButton, prevButton, nextButton);

        card.setPadding(new Insets(22));
        card.setMaxWidth(650);
        card.setMaxHeight(520);
        card.setSpacing(16);
        updateCardStyle();

        card.getChildren().addAll(progressBar, titleLabel, subtitleLabel, stepBody, actions);

        root.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);
        StackPane.setMargin(card, new Insets(20));
    }

    private void updateBackgroundStyle() {
        ThemeManager tm = ThemeManager.getInstance();
        root.setStyle("-fx-background-color: rgba(0,0,0,0.96);");
    }

    private void updateCardStyle() {
        ThemeManager tm = ThemeManager.getInstance();
        if (tm.isDarkMode()) {
            card.setStyle(
                "-fx-background-color: #050505;"
                + "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.18) + ";"
                + "-fx-border-width: 1px;"
                + "-fx-background-radius: 28px;"
                + "-fx-border-radius: 28px;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.38), 24, 0.0, 0, 8);"
            );
        } else {
            card.setStyle(
                "-fx-background-color: #050505;"
                + "-fx-border-color: rgba(255,255,255,0.08);"
                + "-fx-border-width: 1px;"
                + "-fx-background-radius: 28px;"
                + "-fx-border-radius: 28px;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.28), 20, 0.0, 0, 6);"
            );
            titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: white; -fx-wrap-text: true;");
            subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.72); -fx-line-spacing: 4;");
        }
    }

    private void renderCurrentStep() {
        if (!shouldShow || onboarding == null) {
            return;
        }

        updateBackgroundStyle();
        updateCardStyle();

        activeGroups.clear();
        suggestionsArea = null;
        progressBar.getChildren().clear();
        stepBody.getChildren().clear();

        for (int i = 1; i <= 7; i++) {
            Circle dot = new Circle(5.5);
            if (onboarding.getStep() > i) {
                dot.setFill(Color.web(ThemeManager.getInstance().getAccentHex()));
            } else if (onboarding.getStep() == i) {
                dot.setFill(Color.web(ThemeManager.getInstance().getAccentHex()));
            } else {
                dot.setFill(Color.web("rgba(255,255,255,0.22)"));
            }
            progressBar.getChildren().add(dot);
        }

        switch (onboarding.getStep()) {
            case 1 -> renderStepOne();
            case 2 -> renderStepTwo();
            case 3 -> renderStepThree();
            case 4 -> renderStepFour();
            case 5 -> renderStepFive();
            case 6 -> renderStepSix();
            default -> renderStepSeven();
        }

        prevButton.setDisable(onboarding.getStep() <= 1);
        nextButton.setText(onboarding.getStep() >= 7 ? "Finish" : "Next");
    }

    private void renderStepOne() {
        titleLabel.setText("Welcome " + safeName() + ", let's personalize your app");
        subtitleLabel.setText("Choose language and theme preferences.");
        updateTitleStyling();

        stepBody.getChildren().addAll(
            radioSection("Language", "language_preference", mapOf(
                "FR", "French",
                "EN", "English",
                "AR", "Arabic",
                "FR_AR", "Bilingual"
            ), pref("language_preference", "FR")),
            radioSection("Theme", "theme_preference", mapOf(
                "DARK", "Dark",
                "LIGHT", "Light"
            ), pref("theme_preference", "DARK"))
        );
    }

    private void updateTitleStyling() {
        ThemeManager tm = ThemeManager.getInstance();
        if (tm.isDarkMode()) {
            titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: 800; -fx-text-fill: white; -fx-wrap-text: true;");
            subtitleLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: rgba(255,255,255,0.70); -fx-line-spacing: 4;");
        } else {
            titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: 800; -fx-text-fill: #0f172a; -fx-wrap-text: true;");
            subtitleLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #475569; -fx-line-spacing: 4;");
        }
    }

    private void renderStepTwo() {
        titleLabel.setText("Notifications");
        subtitleLabel.setText("Set your preferred channel and frequency.");
        updateTitleStyling();

        stepBody.getChildren().addAll(
            radioSection("Channel", "notification_channel", mapOf(
                "EMAIL", "Email",
                "SMS", "SMS",
                "IN_APP", "In-app",
                "ALL", "All",
                "NONE", "None"
            ), pref("notification_channel", "EMAIL")),
            radioSection("Frequency", "notification_frequency", mapOf(
                "INSTANT", "Instant",
                "DAILY_DIGEST", "Daily digest",
                "WEEKLY_DIGEST", "Weekly digest",
                "MONTHLY", "Monthly",
                "IMPORTANT_ONLY", "Important only"
            ), pref("notification_frequency", "DAILY_DIGEST"))
        );
    }

    private void renderStepThree() {
        titleLabel.setText("Your property");
        subtitleLabel.setText("Tell us about your living space.");
        updateTitleStyling();
        subtitleLabel.setText("Tell us how you live in your residence.");

        stepBody.getChildren().addAll(
            radioSection("Property type", "property_type", mapOf(
                "APARTMENT", "Apartment",
                "STUDIO", "Studio",
                "DUPLEX", "Duplex"
            ), pref("property_type", "APARTMENT")),
            radioSection("Occupancy", "occupancy_status", mapOf(
                "OWNER_OCCUPIED", "Owner occupied",
                "OWNER_RENTING", "Owner renting",
                "TENANT", "Tenant",
                "TEMPORARY", "Temporary"
            ), pref("occupancy_status", "OWNER_OCCUPIED")),
            radioSection("Parking", "parking_type", mapOf(
                "NONE", "None",
                "OUTDOOR", "Outdoor",
                "COVERED", "Covered",
                "UNDERGROUND", "Underground",
                "MULTIPLE", "Multiple"
            ), pref("parking_type", "NONE"))
        );
    }

    private void renderStepFour() {
        titleLabel.setText("Communication preferences");
        subtitleLabel.setText("Choose how you want to receive and interact with building communication.");
        updateTitleStyling();

        stepBody.getChildren().addAll(
            radioSection("Meeting participation", "meeting_participation", mapOf(
                "IN_PERSON", "In person",
                "ONLINE", "Online",
                "HYBRID", "Hybrid",
                "PROXY_ONLY", "Proxy only"
            ), pref("meeting_participation", "HYBRID")),
            radioSection("Document delivery", "document_delivery", mapOf(
                "DIGITAL", "Digital",
                "PAPER", "Paper",
                "BOTH", "Both",
                "ECO_FRIENDLY", "Eco friendly"
            ), pref("document_delivery", "DIGITAL")),
            radioSection("Contact preference", "contact_preference", mapOf(
                "EMAIL", "Email",
                "PHONE", "Phone",
                "WHATSAPP", "WhatsApp",
                "IN_PERSON", "In person",
                "NO_CONTACT", "Emergency only"
            ), pref("contact_preference", "EMAIL"))
        );
    }

    private void renderStepFive() {
        titleLabel.setText("Community engagement");
        subtitleLabel.setText("Adjust maintenance and participation preferences.");
        updateTitleStyling();

        stepBody.getChildren().addAll(
            radioSection("Maintenance priority", "maintenance_priority", mapOf(
                "URGENT_ONLY", "Urgent only",
                "PREVENTIVE", "Preventive",
                "SCHEDULED", "Scheduled",
                "FLEXIBLE", "Flexible"
            ), pref("maintenance_priority", "FLEXIBLE")),
            radioSection("Community engagement", "community_engagement", mapOf(
                "VERY_ACTIVE", "Very active",
                "ACTIVE", "Active",
                "MODERATE", "Moderate",
                "OBSERVER", "Observer",
                "MINIMAL", "Minimal"
            ), pref("community_engagement", "MODERATE"))
        );
    }

    private void renderStepSix() {
        titleLabel.setText("Final preferences");
        subtitleLabel.setText("Set your payment and accessibility profile.");
        updateTitleStyling();

        stepBody.getChildren().addAll(
            radioSection("Payment method", "payment_method_preference", mapOf(
                "BANK_TRANSFER", "Bank transfer",
                "CASH", "Cash",
                "CHECK", "Check",
                "ONLINE", "Online",
                "AUTO_DEBIT", "Auto debit"
            ), pref("payment_method_preference", "ONLINE")),
            radioSection("Noise sensitivity", "noise_sensitivity", mapOf(
                "HIGH", "High",
                "MODERATE", "Moderate",
                "LOW", "Low",
                "NOT_CONCERNED", "Not concerned"
            ), pref("noise_sensitivity", "MODERATE")),
            radioSection("Pets", "pets_status", mapOf(
                "NO_PETS", "No pets",
                "CAT", "Cat",
                "DOG", "Dog",
                "MULTIPLE", "Multiple",
                "OTHER", "Other"
            ), pref("pets_status", "NO_PETS")),
            radioSection("Accessibility", "accessibility_needs", mapOf(
                "NONE", "None",
                "ELEVATOR_REQUIRED", "Elevator required",
                "WHEELCHAIR", "Wheelchair",
                "VISUAL_IMPAIRMENT", "Visual impairment",
                "OTHER", "Other"
            ), pref("accessibility_needs", "NONE"))
        );
    }

    private void renderStepSeven() {
        titleLabel.setText("Almost done");
        subtitleLabel.setText("Share any suggestions and finish onboarding.");
        updateTitleStyling();

        suggestionsArea = new TextArea(onboarding.getSuggestions() == null ? "" : onboarding.getSuggestions());
        suggestionsArea.setPromptText("Your feedback, ideas or requests...");
        suggestionsArea.setWrapText(true);
        suggestionsArea.setPrefRowCount(5);
        
        ThemeManager tm = ThemeManager.getInstance();
        if (tm.isDarkMode()) {
            suggestionsArea.setStyle("-fx-background-color: rgba(255,255,255,0.06);"
                + "-fx-text-fill: white;"
                + "-fx-control-inner-background: rgba(255,255,255,0.06);"
                + "-fx-prompt-text-fill: rgba(255,255,255,0.40);"
                + "-fx-background-radius: 14px;"
                + "-fx-border-radius: 14px;"
                + "-fx-border-color: rgba(255,255,255,0.12);"
                + "-fx-border-width: 1px;"
                + "-fx-padding: 12;");
        } else {
            suggestionsArea.setStyle("-fx-background-color: #ffffff;"
                + "-fx-text-fill: #0f172a;"
                + "-fx-control-inner-background: #ffffff;"
                + "-fx-prompt-text-fill: #94a3b8;"
                + "-fx-background-radius: 14px;"
                + "-fx-border-radius: 14px;"
                + "-fx-border-color: #e2e8f0;"
                + "-fx-border-width: 1px;"
                + "-fx-padding: 12;");
        }

        stepBody.getChildren().add(suggestionsArea);
    }

    private VBox radioSection(String labelText, String key, LinkedHashMap<String, String> options, String current) {
        ThemeManager tm = ThemeManager.getInstance();
        
        Label label = new Label(labelText);
        String labelColor = tm.isDarkMode() ? "white" : "#0f172a";
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + labelColor + ";");

        ToggleGroup group = new ToggleGroup();
        activeGroups.put(key, group);

        FlowPane row = new FlowPane();
        row.setHgap(10);
        row.setVgap(10);
        row.setPrefWrapLength(680);
        row.setAlignment(Pos.CENTER_LEFT);

        options.forEach((value, text) -> {
            RadioButton radio = new RadioButton(text);
            radio.setUserData(value);
            radio.setToggleGroup(group);
            String radioColor = tm.isDarkMode() ? "rgba(255,255,255,0.92)" : "#1e293b";
            radio.setStyle(
                "-fx-text-fill: " + radioColor + ";"
                + "-fx-font-size: 13px;"
                + "-fx-background-color: rgba(255,255,255,0.04);"
                + "-fx-background-radius: 999px;"
                + "-fx-border-radius: 999px;"
                + "-fx-padding: 9 14 9 14;"
            );
            
            if (value.equals(current)) {
                radio.setSelected(true);
            }

            radio.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    if ("theme_preference".equals(key)) {
                        applyThemeChangeImmediately("LIGHT".equals(value));
                    }
                }
            });

            row.getChildren().add(radio);
        });

        VBox section = new VBox(8, label, row);
        section.setPadding(new Insets(4, 0, 4, 0));
        return section;
    }

    private void applyThemeChangeImmediately(boolean isLight) {
        ThemeManager tm = ThemeManager.getInstance();
        tm.setDarkModePreference(!isLight);
        
        updateBackgroundStyle();
        updateCardStyle();
        
        if (stepBody.getChildren().size() > 0) {
            stepBody.getChildren().forEach(node -> {
                if (node instanceof VBox) {
                    VBox section = (VBox) node;
                    section.getChildren().forEach(child -> {
                        if (child instanceof Label) {
                            Label lbl = (Label) child;
                            String newColor = isLight ? "#0f172a" : "white";
                            String style = lbl.getStyle();
                            style = style.replaceAll("-fx-text-fill: [^;]+;", "-fx-text-fill: " + newColor + ";");
                            lbl.setStyle(style);
                        }
                        if (child instanceof FlowPane) {
                            FlowPane flowPane = (FlowPane) child;
                            flowPane.getChildren().forEach(flowChild -> {
                                if (flowChild instanceof RadioButton) {
                                    RadioButton radio = (RadioButton) flowChild;
                                    String radioColor = isLight ? "#1e293b" : "rgba(255,255,255,0.92)";
                                    String style = radio.getStyle();
                                    style = style.replaceAll("-fx-text-fill: [^;]+;", "-fx-text-fill: " + radioColor + ";");
                                    if (isLight) {
                                        style = style.replaceAll("-fx-background-color: [^;]+;", "-fx-background-color: rgba(15,23,42,0.04);");
                                    } else {
                                        style = style.replaceAll("-fx-background-color: [^;]+;", "-fx-background-color: rgba(255,255,255,0.04);");
                                    }
                                    radio.setStyle(style);
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    private void previousStep() {
        applyCurrentStepToModel();
        onboarding.setStep(Math.max(1, onboarding.getStep() - 1));
        onboarding.setUpdatedAt(LocalDateTime.now());
        onboardingController.saveOnboarding(onboarding);
        renderCurrentStep();
    }

    private void nextStep() {
        applyCurrentStepToModel();

        if (onboarding.getStep() >= 7) {
            onboarding.setStep(7);
            onboarding.setCompleted(true);
            onboarding.setCompletedAt(LocalDateTime.now());
            onboarding.setUpdatedAt(LocalDateTime.now());
            onboardingController.saveOnboarding(onboarding);
            completeAndClose();
            return;
        }

        onboarding.setStep(onboarding.getStep() + 1);
        onboarding.setUpdatedAt(LocalDateTime.now());
        onboardingController.saveOnboarding(onboarding);
        renderCurrentStep();
    }

    private void cancelOnboarding() {
        applyCurrentStepToModel();
        onboarding.setCompleted(true);
        onboarding.setCompletedAt(LocalDateTime.now());
        onboarding.setUpdatedAt(LocalDateTime.now());
        onboardingController.saveOnboarding(onboarding);
        completeAndClose();
    }

    private void saveOnboardingProgress() {
        onboarding.setSelectedPreferencesJson("{}");
        onboarding.setSuggestions(null);
        onboarding.setSelectedLocale("fr");
        onboarding.setSelectedTheme("dark");
        onboarding.setStep(onboarding.getStep());
        onboarding.setCompleted(true);
        onboarding.setCompletedAt(LocalDateTime.now());
        onboarding.setUpdatedAt(LocalDateTime.now());
        onboardingController.saveOnboarding(onboarding);
        completeAndClose();
    }

    private void applyCurrentStepToModel() {
        activeGroups.forEach((key, group) -> {
            Toggle selected = group.getSelectedToggle();
            if (selected != null && selected.getUserData() != null) {
                prefs.put(key, selected.getUserData().toString());
            }
        });

        if (onboarding.getStep() == 7 && suggestionsArea != null) {
            onboarding.setSuggestions(suggestionsArea.getText());
        }

        String lang = pref("language_preference", "FR");
        switch (lang) {
            case "EN" -> onboarding.setSelectedLocale("en");
            case "AR" -> onboarding.setSelectedLocale("ar");
            case "FR_AR" -> onboarding.setSelectedLocale("fr_ar");
            default -> onboarding.setSelectedLocale("fr");
        }

        String theme = pref("theme_preference", "DARK");
        onboarding.setSelectedTheme("LIGHT".equals(theme) ? "light" : "dark");
        onboarding.setSelectedPreferencesJson(prefs.toString());

        ThemeManager tm = ThemeManager.getInstance();
        boolean wantsDark = !"LIGHT".equals(theme);
        if (tm.isDarkMode() != wantsDark) {
            tm.setDarkModePreference(wantsDark);
        }

        syncProfileMirror();
    }

    private void syncProfileMirror() {
        if (user == null || user.getIdUser() == null || user.getIdUser() <= 0) {
            return;
        }

        profileController.profileByUserId(user.getIdUser()).ifPresent(profile -> {
            profile.setLocale(onboarding.getSelectedLocale());
            profile.setTheme("light".equalsIgnoreCase(onboarding.getSelectedTheme()) ? 1 : 0);
            profileController.profileUpdate(profile);

            Profile sessionProfile = sessionManager.getCurrentProfile();
            if (sessionProfile != null && sessionProfile.getIdProfile() != null
                && profile.getIdProfile() != null
                && sessionProfile.getIdProfile().equals(profile.getIdProfile())) {
                sessionManager.setCurrentProfile(profile);
            }
        });
    }

    private void completeAndClose() {
        root.setVisible(false);
        root.setManaged(false);
        shouldShow = false;
        if (onCompleted != null) {
            onCompleted.run();
        }
    }

    private String pref(String key, String fallback) {
        return prefs.has(key) ? prefs.optString(key, fallback) : fallback;
    }

    private String safeName() {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        if (first.isBlank()) {
            return "there";
        }
        return first;
    }

    private void styleButton(Button button, String normalStyle, String hoverStyle) {
        String base = "-fx-font-weight: 700; -fx-font-size: 13px; -fx-padding: 8 18 8 18; -fx-min-height: 38px; -fx-pref-height: 38px; -fx-max-height: 38px; -fx-background-radius: 999px; -fx-border-radius: 999px; -fx-border-width: 0; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-insets: 0; -fx-alignment: center;";
        button.setFocusTraversable(false);
        button.setMinWidth(0);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle(base + normalStyle);
        button.setOnMouseEntered(e -> button.setStyle(base + hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(base + normalStyle));
    }

    private LinkedHashMap<String, String> mapOf(String... data) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < data.length; i += 2) {
            map.put(data[i], data[i + 1]);
        }
        return map;
    }
}

