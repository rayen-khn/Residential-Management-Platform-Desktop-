package com.syndicati.views.frontend.profile;

import com.syndicati.controllers.user.onboarding.OnboardingController;
import com.syndicati.controllers.user.user.UserController;
import com.syndicati.models.user.Onboarding;
import com.syndicati.models.user.User;
import com.syndicati.utils.session.SessionManager;
import com.syndicati.views.frontend.home.OnboardingOverlayView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Account section showing user profile information and onboarding choices.
 */
public class ProfileAccountSection {

    private final VBox root = new VBox(16);
    private final UserController userController;
    private final OnboardingController onboardingController;
    private final User currentUser;
    private Onboarding currentOnboarding;
    private OnboardingOverlayView inlineOnboardingEditor;
    private boolean showInlineEditor;

    public ProfileAccountSection() {
        this.userController = new UserController();
        this.onboardingController = new OnboardingController();
        this.currentUser = SessionManager.getInstance().getCurrentUser();
        buildLayout();
    }

    private void buildLayout() {
        root.getChildren().clear();
        root.setPadding(new Insets(16, 0, 0, 0));
        refreshOnboarding();

        root.getChildren().addAll(createAccountCard(), createOnboardingCard());
    }

    private VBox createAccountCard() {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 1; -fx-background-radius: 20; -fx-border-radius: 20;");
        card.setPadding(new Insets(22));

        Label title = new Label("Account");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);

        if (currentUser != null) {
            card.getChildren().addAll(
                title,
                createInfoLine("First name", currentUser.getFirstName()),
                createInfoLine("Last name", currentUser.getLastName()),
                createInfoLine("Email", currentUser.getEmailUser()),
                createInfoLine("Role", currentUser.getRoleUser()),
                createInfoLine("Verified", currentUser.isVerified() ? "Yes" : "No"),
                createInfoLine("Account created", currentUser.getCreatedAt() != null ? currentUser.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—"),
                createInfoLine("Timezone", "—"),
                createInfoLine("Phone", currentUser.getPhone() != null ? currentUser.getPhone() : "—")
            );
        } else {
            card.getChildren().add(new Label("User not logged in"));
        }

        return card;
    }

    private VBox createOnboardingCard() {
        VBox card = new VBox(16);
        card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 1; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 22;");
        card.setPadding(new Insets(22));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Your onboarding choices");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.WHITE);
        HBox.setHgrow(title, Priority.ALWAYS);

        Button editButton = new Button("Edit choices");
        editButton.setStyle("-fx-padding: 8 14 8 14; -fx-background-color: rgba(99, 102, 241, 0.2); -fx-text-fill: white; -fx-border-color: rgba(99, 102, 241, 0.3); -fx-border-width: 1; -fx-background-radius: 999px; -fx-border-radius: 999px; -fx-font-weight: bold; -fx-font-size: 11;");
        editButton.setOnAction(e -> openOnboardingEditor());

        if (showInlineEditor) {
            editButton.setText("Close editor");
            editButton.setOnAction(e -> closeOnboardingEditor());
        }

        header.getChildren().addAll(title, editButton);

        Label subtitle = new Label("Update language, theme and profile preferences");
        subtitle.setFont(Font.font("Segoe UI", 13));
        subtitle.setTextFill(Color.color(1, 1, 1, 0.65));

        if (showInlineEditor && inlineOnboardingEditor != null && inlineOnboardingEditor.shouldShow()) {
            VBox inlineEditorWrap = new VBox(inlineOnboardingEditor.getRoot());
            inlineEditorWrap.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1; -fx-background-radius: 14; -fx-border-radius: 14;");
            card.getChildren().addAll(header, subtitle, inlineEditorWrap);
            return card;
        }

        if (currentOnboarding == null) {
            card.getChildren().addAll(header, subtitle, createNoOnboardingMessage());
            return card;
        }

        HBox content = new HBox(18);
        content.setAlignment(Pos.TOP_LEFT);

        VBox left = new VBox(10);
        left.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);

        GridPane summary = new GridPane();
        summary.setHgap(8);
        summary.setVgap(8);
        summary.add(createPreferencePill("Language", displayLanguage(currentOnboarding.getSelectedLocale())), 0, 0);
        summary.add(createPreferencePill("Theme", displayTheme(currentOnboarding.getSelectedTheme())), 1, 0);
        summary.add(createPreferencePill("Status", currentOnboarding.isCompleted() ? "Completed" : "In progress"), 0, 1);
        summary.add(createPreferencePill("Step", String.valueOf(currentOnboarding.getStep())), 1, 1);
        summary.add(createPreferencePill("Started", formatDate(currentOnboarding.getStartedAt())), 0, 2);
        summary.add(createPreferencePill("Updated", formatDate(currentOnboarding.getUpdatedAt())), 1, 2);
        left.getChildren().add(summary);

        VBox right = new VBox(8);
        right.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(right, Priority.ALWAYS);

        Label extrasTitle = new Label("Selected preferences");
        extrasTitle.setTextFill(Color.color(1, 1, 1, 0.6));
        right.getChildren().add(extrasTitle);

        JSONObject prefs = parsePreferences(currentOnboarding.getSelectedPreferencesJson());
        if (prefs.isEmpty()) {
            Label empty = new Label("No extra preferences saved yet.");
            empty.setTextFill(Color.color(1, 1, 1, 0.8));
            right.getChildren().add(empty);
        } else {
            List<String> keys = new ArrayList<>(prefs.keySet());
            Collections.sort(keys);

            FlowPane flow = new FlowPane();
            flow.setHgap(8);
            flow.setVgap(8);
            flow.setPrefWrapLength(620);

            for (String key : keys) {
                flow.getChildren().add(createCompactChoiceChip(key, String.valueOf(prefs.opt(key))));
            }

            right.getChildren().add(flow);
        }

        if (currentOnboarding.getSuggestions() != null && !currentOnboarding.getSuggestions().isBlank()) {
            Label suggestion = new Label("Suggestions: " + currentOnboarding.getSuggestions());
            suggestion.setWrapText(true);
            suggestion.setTextFill(Color.color(1, 1, 1, 0.75));
            suggestion.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 8;");
            right.getChildren().add(suggestion);
        }

        content.getChildren().addAll(left, right);

        card.getChildren().addAll(header, subtitle, content);
        return card;
    }

    private HBox createInfoLine(String key, String value) {
        HBox line = new HBox();
        line.setAlignment(Pos.CENTER_LEFT);
        line.setSpacing(20);

        Label keyLabel = new Label(key);
        keyLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        keyLabel.setTextFill(Color.color(1, 1, 1, 0.5));
        keyLabel.setMinWidth(120);

        Label valueLabel = new Label(value == null ? "—" : value);
        valueLabel.setFont(Font.font("Segoe UI", 13));
        valueLabel.setTextFill(Color.WHITE);
        valueLabel.setWrapText(true);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);

        line.getChildren().addAll(keyLabel, valueLabel);
        return line;
    }

    private VBox createPreferencePill(String key, String value) {
        VBox pill = new VBox(4);
        pill.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 8;");
        pill.setAlignment(Pos.CENTER);
        pill.setMinWidth(120);

        Label keyLabel = new Label(key);
        keyLabel.setFont(Font.font("Segoe UI", 9));
        keyLabel.setTextFill(Color.color(1, 1, 1, 0.6));

        Label valueLabel = new Label(value == null ? "—" : value);
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        valueLabel.setTextFill(Color.WHITE);
        valueLabel.setWrapText(true);

        pill.getChildren().addAll(keyLabel, valueLabel);
        return pill;
    }

    private HBox createCompactChoiceChip(String key, String value) {
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1; -fx-background-radius: 999px; -fx-border-radius: 999px; -fx-padding: 6 10 6 10;");

        Label keyLabel = new Label(key + ":");
        keyLabel.setTextFill(Color.color(1, 1, 1, 0.58));
        keyLabel.setFont(Font.font("Segoe UI", 10));

        Label valueLabel = new Label(value == null ? "—" : value);
        valueLabel.setTextFill(Color.WHITE);
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));

        chip.getChildren().addAll(keyLabel, valueLabel);
        return chip;
    }

    private VBox createNoOnboardingMessage() {
        VBox box = new VBox(6);
        Label message = new Label("No onboarding data found yet.");
        message.setTextFill(Color.color(1, 1, 1, 0.8));
        Label hint = new Label("Use Edit choices to create or update your onboarding settings.");
        hint.setTextFill(Color.color(1, 1, 1, 0.55));
        box.getChildren().addAll(message, hint);
        return box;
    }

    private void refreshOnboarding() {
        if (currentUser == null || currentUser.getIdUser() == null) {
            currentOnboarding = null;
            return;
        }
        currentOnboarding = onboardingController.findOrCreateByUserId(currentUser.getIdUser()).orElse(null);
    }

    private void openOnboardingEditor() {
        if (currentUser == null || currentUser.getIdUser() == null) {
            return;
        }

        OnboardingOverlayView editor = new OnboardingOverlayView(currentUser, () -> {
            showInlineEditor = false;
            inlineOnboardingEditor = null;
            buildLayout();
        }, true);
        if (!editor.shouldShow()) {
            return;
        }

        inlineOnboardingEditor = editor;
        showInlineEditor = true;
        buildLayout();
    }

    private void closeOnboardingEditor() {
        showInlineEditor = false;
        inlineOnboardingEditor = null;
        buildLayout();
    }

    private JSONObject parsePreferences(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return new JSONObject();
        }
        try {
            return new JSONObject(rawJson);
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private String displayLanguage(String locale) {
        if (locale == null || locale.isBlank()) {
            return "—";
        }
        return switch (locale.toLowerCase()) {
            case "en" -> "English";
            case "fr" -> "French";
            case "ar" -> "Arabic";
            case "fr_ar" -> "French / Arabic";
            default -> locale;
        };
    }

    private String displayTheme(String theme) {
        if (theme == null || theme.isBlank()) {
            return "—";
        }
        return theme.substring(0, 1).toUpperCase() + theme.substring(1).toLowerCase();
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "—";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public VBox getRoot() {
        return root;
    }

    public void cleanup() {
        // Cleanup resources if needed
    }
}
