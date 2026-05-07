package com.syndicati.views.frontend.services;

import com.syndicati.MainApplication;
import com.syndicati.controllers.syndicat.ReclamationController;
import com.syndicati.interfaces.ViewInterface;
import com.syndicati.models.syndicat.Reclamation;
import com.syndicati.models.user.User;
import com.syndicati.utils.session.SessionManager;
import com.syndicati.utils.theme.ThemeManager;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.scene.Scene;
import javafx.beans.binding.Bindings;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Syndicat page view - mirrors frontend/syndicat/index.html.twig structure.
 * Complete MVC implementation with CRUD functionality and file uploads.
 */
public class SyndicatPageView implements ViewInterface {

    private static final int SUBJECT_MIN_LENGTH = 5;
    private static final int SUBJECT_MAX_LENGTH = 255;
    private static final int DESCRIPTION_MIN_LENGTH = 10;
    private static final int DESCRIPTION_MAX_LENGTH = 255;

    private final VBox root;
    private final ThemeManager tm = ThemeManager.getInstance();
    private final ReclamationController controller = new ReclamationController();
    
    // Form fields
    private TextField subjectField;
    private LocalDateTime selectedDateTime;
    private TextArea descriptionField;
    private Label subjectValidationLabel;
    private Label descriptionValidationLabel;
    private Button submitButton;
    private List<File> selectedFiles = new ArrayList<>();
    private Text fileStatusText;
    private FlowPane previewPane;

    public SyndicatPageView() {
        root = new VBox(22);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20, 0, 40, 0));
        root.setMaxWidth(Double.MAX_VALUE);
        root.setStyle("-fx-background-color: transparent;");

        VBox form = buildFormCard();
        VBox.setMargin(form, new Insets(-90, 0, 0, 0));
        root.getChildren().addAll(buildHero(), form);
    }

    private StackPane buildHero() {
        StackPane hero = new StackPane();
        hero.setMaxWidth(1800);
        hero.prefWidthProperty().bind(Bindings.min(root.widthProperty().multiply(0.95), 1800));
        hero.setMinHeight(500);
        hero.setPadding(new Insets(92, 64, 92, 64));
        hero.setStyle(
            "-fx-background-color: " + surfaceStrong() + ";" +
            "-fx-background-radius: 48px;" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 48px;"
        );

        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER_LEFT);

        Text badgeText = new Text("Syndicat Services");
        badgeText.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 12));
        badgeText.setFill(Color.WHITE);
        StackPane badge = new StackPane(badgeText);
        badge.setPadding(new Insets(7, 14, 7, 14));
        badge.setMaxWidth(StackPane.USE_PREF_SIZE);
        badge.setStyle(
            "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.12) + ";" +
            "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.25) + ";" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 999px;" +
            "-fx-border-radius: 999px;"
        );

        Text title = line("Voice Your\nConcerns.", 72, true, tm.getAccentHex());
        Text subtitle = line("We are here to listen and resolve. Submit your reclamation directly to the syndicat and track its progress in real-time.", 21, false, textMuted());
        subtitle.setWrappingWidth(660);

        content.getChildren().addAll(badge, title, subtitle);
        hero.getChildren().add(content);
        StackPane.setAlignment(content, Pos.CENTER_LEFT);
        return hero;
    }

    private VBox buildFormCard() {
        VBox form = new VBox(16);
        form.setMaxWidth(1000);
        form.prefWidthProperty().bind(Bindings.min(root.widthProperty().multiply(0.90), 1000));
        form.setPadding(new Insets(48, 48, 48, 48));
        form.setStyle(
            "-fx-background-color: " + surfaceCard() + ";" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 40px;" +
            "-fx-border-radius: 40px;"
        );

        Text title = line("Submit a Reclamation", 34, true, tm.getAccentHex());
        Text sub = line("Fill in the details below. We'll get back to you shortly.", 15, false, textMuted());

        // Subject field with label
        VBox subjectSection = new VBox(6);
        subjectSection.getChildren().add(label("Subject"));
        subjectField = input("What is this regarding?");
        subjectValidationLabel = createLiveValidationLabel("Start typing your subject...");
        subjectSection.getChildren().addAll(subjectField, subjectValidationLabel);

        // Date field with label
        VBox dateSection = new VBox(6);
        dateSection.getChildren().add(label("Date of Incident"));
        dateSection.getChildren().add(buildCustomCalendar());

        // Description field with label
        VBox descSection = new VBox(4);
        descSection.getChildren().add(label("Description"));

        String descBoxBaseStyle =
            "-fx-background-color: " + inputBg() + ";" +
            "-fx-background-radius: 14px;" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-radius: 14px;" +
            "-fx-border-width: 1.5px;" +
            "-fx-padding: 12 18 12 18;";

        StackPane descriptionWrapper = new StackPane();
        descriptionWrapper.setStyle(descBoxBaseStyle);

        descriptionField = new TextArea();
        descriptionField.setPromptText("Please describe the issue in detail...");
        descriptionField.setPrefRowCount(6);
        descriptionField.setWrapText(true);
        descriptionField.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-control-inner-background: transparent;" +
            "-fx-text-fill: " + tm.getTextColor() + ";" +
            "-fx-prompt-text-fill: " + textMuted() + ";" +
            "-fx-background-insets: 0;" +
            "-fx-border-color: transparent;" +
            "-fx-padding: 0;" +
            "-fx-font-size: 13px;"
        );
        descriptionField.setMinHeight(120);
        descriptionField.setPrefHeight(120);
        StackPane.setAlignment(descriptionField, Pos.CENTER_LEFT);
        descriptionWrapper.getChildren().add(descriptionField);

        // Force inner TextArea skins to stay transparent so no second "box" appears.
        Platform.runLater(() -> {
            if (descriptionField.lookup(".content") != null) {
                descriptionField.lookup(".content").setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
            }
            if (descriptionField.lookup(".viewport") != null) {
                descriptionField.lookup(".viewport").setStyle("-fx-background-color: transparent;");
            }
            if (descriptionField.lookup(".scroll-pane") != null) {
                descriptionField.lookup(".scroll-pane").setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
            }
        });

        descriptionField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                descriptionWrapper.setStyle(
                    "-fx-background-color: " + inputBg() + ";" +
                    "-fx-background-radius: 14px;" +
                    "-fx-border-color: " + tm.getAccentHex() + ";" +
                    "-fx-border-radius: 14px;" +
                    "-fx-border-width: 2px;" +
                    "-fx-padding: 12 18 12 18;"
                );
            } else {
                descriptionWrapper.setStyle(descBoxBaseStyle);
            }
        });
        descSection.getChildren().add(descriptionWrapper);
        descriptionValidationLabel = createLiveValidationLabel("Describe your issue with at least 10 characters...");
        descSection.getChildren().add(descriptionValidationLabel);

        // Attachments zone with drag-and-drop and click to upload
        VBox attachmentSection = new VBox(6);
        attachmentSection.getChildren().add(label("Attachments"));

        VBox attachmentZone = new VBox(8);
        attachmentZone.setAlignment(Pos.CENTER);
        attachmentZone.setPrefHeight(80);
        attachmentZone.setCursor(Cursor.HAND);
        
        Text uploadIcon = line("\u2B06", 32, false, tm.getAccentHex());
        Text uploadText = line("Click or drag images", 12, false, textMuted());
        fileStatusText = line("No files selected", 11, false, textMuted());
        
        attachmentZone.getChildren().addAll(uploadIcon, uploadText, fileStatusText);
        
        attachmentZone.setStyle(
            "-fx-background-color: " + surfaceSoft() + ";" +
            "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.4) + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 14px;" +
            "-fx-background-radius: 14px;" +
            "-fx-border-style: dashed;" +
            "-fx-cursor: hand;"
        );
        
        // Hover effect for drag-and-drop
        attachmentZone.setOnDragOver(this::handleDragOver);
        attachmentZone.setOnDragExited(event -> {
            attachmentZone.setStyle(
                "-fx-background-color: " + surfaceSoft() + ";" +
                "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.4) + ";" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 14px;" +
                "-fx-background-radius: 14px;" +
                "-fx-border-style: dashed;"
            );
            attachmentZone.setCursor(Cursor.HAND);
        });
        attachmentZone.setOnDragDropped(this::handleDragDropped);
        
        // Click to upload
        attachmentZone.setOnMouseClicked(e -> openFileChooser());
        attachmentZone.setOnMouseEntered(e -> {
            attachmentZone.setStyle(
                "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.08) + ";"
                + "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.6) + ";"
                + "-fx-border-width: 2px;"
                + "-fx-border-radius: 14px;"
                + "-fx-background-radius: 14px;"
                + "-fx-border-style: dashed;"
            );
        });
        attachmentZone.setOnMouseExited(e -> {
            attachmentZone.setStyle(
                "-fx-background-color: " + surfaceSoft() + ";"
                + "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.4) + ";"
                + "-fx-border-width: 2px;"
                + "-fx-border-radius: 14px;"
                + "-fx-background-radius: 14px;"
                + "-fx-border-style: dashed;"
            );
        });
        
        // Preview pane for selected images
        previewPane = new FlowPane();
        previewPane.setHgap(8);
        previewPane.setVgap(8);
        previewPane.setPadding(new Insets(8, 0, 0, 0));
        previewPane.setStyle("-fx-background-color: transparent;");
        
        attachmentSection.getChildren().addAll(attachmentZone, previewPane);
        VBox.setMargin(attachmentSection, new Insets(0, 0, 0, 0));

        // Submit button
        submitButton = new Button("Submit Reclamation");
        submitButton.setCursor(Cursor.HAND);
        submitButton.setStyle(
            "-fx-background-color: " + tm.getEffectiveAccentGradient() + ";"
            + "-fx-text-fill: white;"
            + "-fx-font-size: 14px;"
            + "-fx-font-weight: 800;"
            + "-fx-background-radius: 12px;"
            + "-fx-padding: 14 24 14 24;"
            + "-fx-letter-spacing: 1px;"
        );
        submitButton.setMaxWidth(Double.MAX_VALUE);
        submitButton.setPrefHeight(48);
        addButtonPulse(submitButton);
        setupSubmitAction();
        setupLiveValidation();

        form.getChildren().addAll(
            title, 
            sub, 
            new Pane(), // spacer
            subjectSection, 
            dateSection, 
            descSection, 
            attachmentSection,
            new Pane(), // spacer
            submitButton
        );
        
        return form;
    }

    private void setupSubmitAction() {
        submitButton.setOnAction(e -> handleSubmit());
    }

    private void setupLiveValidation() {
        subjectField.textProperty().addListener((obs, oldText, newText) -> updateSubjectValidation(newText));
        descriptionField.textProperty().addListener((obs, oldText, newText) -> updateDescriptionValidation(newText));

        updateSubjectValidation(subjectField.getText());
        updateDescriptionValidation(descriptionField.getText());
    }

    private Label createLiveValidationLabel(String message) {
        Label label = new Label(message);
        label.setTextFill(Color.web(textMuted()));
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: 600;");
        return label;
    }

    private void updateSubjectValidation(String value) {
        String subject = value == null ? "" : value.trim();
        if (subject.isEmpty()) {
            subjectValidationLabel.setText("Start typing your subject...");
            subjectValidationLabel.setTextFill(Color.web(textMuted()));
            return;
        }
        String error = reclamationFieldError(subject, descriptionField.getText(), "Title ");
        if (error != null) {
            subjectValidationLabel.setText(error);
            subjectValidationLabel.setTextFill(Color.web("#ff3b30"));
            return;
        }
        subjectValidationLabel.setText("✓ Subject looks good");
        subjectValidationLabel.setTextFill(Color.web("#2ecc71")); // Success Green
    }

    private void updateDescriptionValidation(String value) {
        String description = value == null ? "" : value.trim();
        if (description.isEmpty()) {
            descriptionValidationLabel.setText("Describe your issue with at least 10 characters...");
            descriptionValidationLabel.setTextFill(Color.web(textMuted()));
            return;
        }
        String error = reclamationFieldError(subjectField.getText(), description, "Description ");
        if (error != null) {
            descriptionValidationLabel.setText(error);
            descriptionValidationLabel.setTextFill(Color.web("#ff3b30"));
            return;
        }
        
        // Custom check: must start with a letter
        if (!description.isEmpty() && !Character.isLetter(description.charAt(0))) {
            descriptionValidationLabel.setText("Description must start with a letter");
            descriptionValidationLabel.setTextFill(Color.web("#ff3b30"));
            return;
        }

        descriptionValidationLabel.setText("✓ Description looks good");
        descriptionValidationLabel.setTextFill(Color.web("#2ecc71")); // Success Green
    }

    private void handleSubmit() {
        String subject = subjectField.getText().trim();
        String description = descriptionField.getText().trim();
        LocalDateTime date = selectedDateTime != null ? selectedDateTime : LocalDateTime.now();

        // Get current user
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            showError("Error", "You must be logged in to submit a reclamation");
            return;
        }

        Reclamation draft = buildReclamationDraft(subject, description, date, currentUser);
        List<String> errors = draft.validateForCreate();
        if (!errors.isEmpty()) {
            showError("Validation Error", errors.getFirst());
            return;
        }

        // Process file uploads (store first attachment only, aligned with single image DB field)
        String imagePath = null;
        if (!selectedFiles.isEmpty()) {
            try {
                imagePath = copyFileToUploads(selectedFiles.getFirst());
            } catch (IOException e) {
                showError("Upload Error", "Failed to upload images: " + e.getMessage());
                return;
            }
        }

        // Submit via controller
        Integer reclamationId = controller.reclamationCreate(
            subject,
            description,
            date,
            imagePath,
            currentUser
        );

        if (reclamationId > 0) {
            showSuccess("Success", "Your reclamation has been submitted successfully.\nWe will review it shortly.");
            clearForm();
        } else {
            showError("Submission Failed", "Unable to create reclamation. Please ensure subject/description are within allowed limits and try again.");
        }
    }

    private String reclamationFieldError(String title, String description, String fieldPrefix) {
        Reclamation draft = buildReclamationDraft(
            title == null ? "" : title.trim(),
            description == null ? "" : description.trim(),
            selectedDateTime != null ? selectedDateTime : LocalDateTime.now(),
            validationUser()
        );
        for (String error : draft.validateForCreate()) {
            if (error.startsWith(fieldPrefix)) {
                return error;
            }
        }
        return null;
    }

    private Reclamation buildReclamationDraft(String title, String description, LocalDateTime date, User user) {
        Reclamation draft = new Reclamation();
        draft.setTitreReclamations(title);
        draft.setDescReclamation(description);
        draft.setDateReclamation(date);
        draft.setUser(user);
        return draft;
    }

    private User validationUser() {
        User current = SessionManager.getInstance().getCurrentUser();
        if (current != null && current.getIdUser() != null && current.getIdUser() > 0) {
            return current;
        }
        User placeholder = new User();
        placeholder.setIdUser(1);
        return placeholder;
    }

    private void clearForm() {
        subjectField.clear();
        selectedDateTime = null;
        descriptionField.clear();
        selectedFiles.clear();
        previewPane.getChildren().clear();
        fileStatusText.setText("No files selected");
    }

    private void handleDragOver(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        if (dragboard.hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
    }

    private void handleDragDropped(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        if (dragboard.hasFiles()) {
            List<File> files = dragboard.getFiles();
            addFilesToSelection(files);
            event.setDropCompleted(true);
            
            // Visual feedback
            VBox zone = (VBox) event.getSource();
            zone.setStyle(
                "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.15) + ";" +
                "-fx-border-color: " + tm.getAccentHex() + ";" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 14px;" +
                "-fx-background-radius: 14px;" +
                "-fx-border-style: dashed;" +
                "-fx-cursor: hand;"
            );
        }
        event.consume();
    }

    private void openFileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Images");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        try {
            List<File> files = chooser.showOpenMultipleDialog(root.getScene() != null ? root.getScene().getWindow() : null);
            if (files != null && !files.isEmpty()) {
                addFilesToSelection(files);
            }
        } catch (Exception e) {
            System.out.println("Error opening file chooser: " + e.getMessage());
        }
    }

    private void addFilesToSelection(List<File> files) {
        for (File file : files) {
            if (!selectedFiles.contains(file)) {
                selectedFiles.add(file);
                addImagePreview(file);
            }
        }
        updateFileStatus();
    }

    private void addImagePreview(File file) {
        VBox previewBox = new VBox(4);
        previewBox.setAlignment(Pos.CENTER);
        previewBox.setPadding(new Insets(6));
        previewBox.setStyle(
            "-fx-background-color: " + surfaceSoft() + ";" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-border-width: 1px;"
        );
        
        try {
            // Load and display image preview
            Image image = new Image(file.toURI().toString(), 60, 60, true, true);
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(60);
            imageView.setFitHeight(60);
            imageView.setStyle("-fx-border-radius: 6px;");
            
            // File name label
            String fileName = file.getName();
            if (fileName.length() > 10) {
                fileName = fileName.substring(0, 10) + "...";
            }
            Text nameText = new Text(fileName);
            nameText.setFont(Font.font(MainApplication.getInstance().getLightFontFamily(), 10));
            nameText.setFill(Color.web(textMuted()));
            
            // Remove button
            Button removeBtn = new Button("×");
            removeBtn.setCursor(Cursor.HAND);
            removeBtn.setStyle(
                "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.2) + ";"
                + "-fx-text-fill: " + tm.getAccentHex() + ";"
                + "-fx-font-size: 10px;"
                + "-fx-padding: 2 6 2 6;"
                + "-fx-background-radius: 4px;"
            );
            removeBtn.setOnAction(e -> {
                selectedFiles.remove(file);
                previewPane.getChildren().remove(previewBox);
                updateFileStatus();
            });
            removeBtn.setOnMouseEntered(e -> {
                removeBtn.setStyle(
                    "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.4) + ";"
                    + "-fx-text-fill: " + tm.getAccentHex() + ";"
                    + "-fx-font-size: 10px;"
                    + "-fx-padding: 2 6 2 6;"
                    + "-fx-background-radius: 4px;"
                );
            });
            removeBtn.setOnMouseExited(e -> {
                removeBtn.setStyle(
                    "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.2) + ";"
                    + "-fx-text-fill: " + tm.getAccentHex() + ";"
                    + "-fx-font-size: 10px;"
                    + "-fx-padding: 2 6 2 6;"
                    + "-fx-background-radius: 4px;"
                );
            });
            
            previewBox.getChildren().addAll(imageView, nameText, removeBtn);
            previewPane.getChildren().add(previewBox);
        } catch (Exception e) {
            System.out.println("Error loading image preview: " + e.getMessage());
        }
    }

    private void updateFileStatus() {
        if (selectedFiles.isEmpty()) {
            fileStatusText.setText("No files selected");
            fileStatusText.setFill(Color.web(textMuted()));
        } else {
            fileStatusText.setText("✓ " + selectedFiles.size() + " file" + (selectedFiles.size() > 1 ? "s" : "") + " selected");
            fileStatusText.setFill(Color.web(tm.getAccentHex()));
        }
    }

    private String copyFileToUploads(File sourceFile) throws IOException {
        String uploadsPath = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "reclamation_images";
        File uploadsDir = new File(uploadsPath);
        
        if (!uploadsDir.exists()) {
            uploadsDir.mkdirs();
        }
        
        String fileName = System.currentTimeMillis() + "_" + sourceFile.getName();
        File destFile = new File(uploadsDir, fileName);
        
        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        return "reclamation_images" + File.separator + fileName;
    }

    private void showError(String title, String message) {
        showPremiumNotification(title, message, false);
    }

    private void showSuccess(String title, String message) {
        showPremiumNotification(title, message, true);
    }

    private void showPremiumNotification(String title, String message, boolean isSuccess) {
        // Create an overlay on the root StackPane if we had one, 
        // but here we'll use a custom Stage for a cleaner pop-up effect.
        javafx.stage.Stage alertStage = new javafx.stage.Stage();
        alertStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        alertStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        if (root.getScene() != null) {
            alertStage.initOwner(root.getScene().getWindow());
        }

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));
        content.setMinWidth(420);
        content.setMaxWidth(420);
        
        // Premium Background with Glassmorphism (Neutral Dark)
        String successGreen = "#2ecc71";
        content.setStyle(
            "-fx-background-color: rgba(20, 20, 25, 0.92);" + // Deep dark neutral
            "-fx-background-radius: 32px;" +
            "-fx-border-color: " + (isSuccess ? successGreen : "#ff3b30") + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 32px;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 40, 0, 0, 20);"
        );

        // Vibrant Icon
        String iconChar = isSuccess ? "\u2713" : "\u26A0";
        String iconColor = isSuccess ? successGreen : "#ff3b30";
        Text icon = new Text(iconChar);
        icon.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 54));
        icon.setFill(Color.web(iconColor));
        
        StackPane iconCircle = new StackPane(icon);
        iconCircle.setPrefSize(90, 90);
        iconCircle.setMaxSize(90, 90);
        iconCircle.setStyle(
            "-fx-background-color: " + tm.toRgba(iconColor, 0.1) + ";" +
            "-fx-background-radius: 999px;" +
            "-fx-border-color: " + tm.toRgba(iconColor, 0.2) + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 999px;"
        );

        Text titleText = new Text(title);
        titleText.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 22));
        titleText.setFill(Color.WHITE);

        Text msgText = new Text(message);
        msgText.setFont(Font.font(MainApplication.getInstance().getLightFontFamily(), 14));
        msgText.setFill(Color.web(textMuted()));
        msgText.setWrappingWidth(340);
        msgText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button closeBtn = new Button("Continue");
        closeBtn.setCursor(Cursor.HAND);
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setPrefHeight(44);
        String btnGradient = isSuccess 
            ? "linear-gradient(to right, #2ecc71, #27ae60)" 
            : "linear-gradient(to right, #ff3b30, #ff7b30)";
            
        closeBtn.setStyle(
            "-fx-background-color: " + btnGradient + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: 800;" +
            "-fx-background-radius: 14px;" +
            "-fx-padding: 0 30 0 30;"
        );
        closeBtn.setOnAction(e -> alertStage.close());
        addButtonPulse(closeBtn);

        content.getChildren().addAll(iconCircle, titleText, msgText, closeBtn);

        // Entrance Animation
        content.setOpacity(0);
        content.setScaleX(0.9);
        content.setScaleY(0.9);
        
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(300), content);
        ft.setToValue(1.0);
        
        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(Duration.millis(350), content);
        st.setToX(1.0);
        st.setToY(1.0);
        st.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        StackPane sceneRoot = new StackPane(content);
        sceneRoot.setStyle("-fx-background-color: transparent;");
        sceneRoot.setPadding(new Insets(50));
        
        Scene alertScene = new Scene(sceneRoot);
        alertScene.setFill(Color.TRANSPARENT);
        alertStage.setScene(alertScene);
        
        alertStage.show();
        ft.play();
        st.play();
    }

    private TextField input(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(44);
        
        String baseStyle =
            "-fx-background-color: " + inputBg() + ";" +
            "-fx-text-fill: " + tm.getTextColor() + ";" +
            "-fx-prompt-text-fill: " + textMuted() + ";" +
            "-fx-background-radius: 14px;" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-radius: 14px;" +
            "-fx-border-width: 1.5px;" +
            "-fx-padding: 12 18 12 18;" +
            "-fx-font-size: 13px;" +
            "-fx-focus-color: " + tm.getAccentHex() + ";";
        
        field.setStyle(baseStyle);
        
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle(baseStyle +
                    "-fx-border-color: " + tm.getAccentHex() + ";" +
                    "-fx-border-width: 2px;"
                );
            } else {
                field.setStyle(baseStyle);
            }
        });
        
        return field;
    }

    private Text label(String txt) {
        Text t = new Text(txt);
        t.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 13));
        // Use accent color as fallback if secondary not available
        String labelColor = textMuted();
        try {
            labelColor = tm.getSecondaryTextColor();
        } catch (Exception e) {
            // Fallback to text color
            labelColor = tm.getTextColor();
        }
        t.setFill(Color.web(labelColor));
        return t;
    }

    private Text line(String txt, int size, boolean bold, String color) {
        Text t = new Text(txt);
        t.setFont(Font.font(
            bold ? MainApplication.getInstance().getBoldFontFamily() : MainApplication.getInstance().getLightFontFamily(),
            bold ? FontWeight.BOLD : FontWeight.NORMAL,
            size
        ));
        t.setFill(Color.web(color));
        return t;
    }

    private void addButtonPulse(Button button) {
        button.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(180), button);
            st.setToX(1.04);
            st.setToY(1.04);
            st.play();
        });
        button.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(180), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    private VBox buildCustomCalendar() {
        VBox calendarContainer = new VBox(12);
        calendarContainer.setStyle("-fx-background-color: transparent;");
        calendarContainer.setPadding(new Insets(12, 0, 0, 0));
        
        // Current month reference (mutable for navigation)
        LocalDate[] currentMonth = {LocalDate.now().withDayOfMonth(1)};
        
        // Selected datetime display
        VBox selectedDisplay = new VBox(4);
        selectedDisplay.setAlignment(Pos.CENTER);
        selectedDisplay.setStyle(
            "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.1) + ";" +
            "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.3) + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 8px;" +
            "-fx-padding: 12;"
        );
        
        Text selectedDateDisplay = new Text("Select a date and time");
        selectedDateDisplay.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 13));
        selectedDateDisplay.setFill(Color.web(tm.getTextColor()));
        
        selectedDisplay.getChildren().add(selectedDateDisplay);
        
        // Month/Year header with navigation
        HBox monthHeader = new HBox(12);
        monthHeader.setAlignment(Pos.CENTER);
        monthHeader.setPadding(new Insets(4));
        
        Button prevMonth = new Button("<");
        prevMonth.setCursor(Cursor.HAND);
        prevMonth.setStyle(
            "-fx-background-color: " + surfaceSoft() + ";" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 6px;" +
            "-fx-border-radius: 6px;" +
            "-fx-text-fill: " + tm.getTextColor() + ";" +
            "-fx-font-weight: 700;" +
            "-fx-padding: 6 12 6 12;"
        );
        
        Button nextMonth = new Button(">");
        nextMonth.setCursor(Cursor.HAND);
        nextMonth.setStyle(prevMonth.getStyle());
        
        Text monthText = new Text(currentMonth[0].getMonth() + " " + currentMonth[0].getYear());
        monthText.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 14));
        monthText.setFill(Color.web(tm.getTextColor()));
        
        monthHeader.getChildren().addAll(prevMonth, monthText, nextMonth);
        
        // Calendar grid container
        GridPane calendarGrid = new GridPane();
        calendarGrid.setHgap(4);
        calendarGrid.setVgap(4);
        
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(14.285);
            cc.setHalignment(HPos.CENTER);
            calendarGrid.getColumnConstraints().add(cc);
        }
        
        // Time picker
        HBox timePicker = new HBox(8);
        timePicker.setAlignment(Pos.CENTER);
        timePicker.setPadding(new Insets(8, 0, 0, 0));
        
        Text timeLabel = new Text("Time:");
        timeLabel.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 12));
        timeLabel.setFill(Color.web(tm.getTextColor()));
        
        // Hour spinner
        TextField hourField = new TextField("00");
        hourField.setMaxWidth(50);
        hourField.setStyle(
            "-fx-background-color: " + inputBg() + ";" +
            "-fx-text-fill: " + tm.getTextColor() + ";" +
            "-fx-background-radius: 6px;" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-radius: 6px;" +
            "-fx-border-width: 1px;" +
            "-fx-padding: 6 8 6 8;" +
            "-fx-font-size: 12px;"
        );
        hourField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*") || (newVal.length() > 2) || (!newVal.isEmpty() && Integer.parseInt(newVal) > 23)) {
                hourField.setText(oldVal);
            }
        });
        
        Text colonText = new Text(":");
        colonText.setFill(Color.web(tm.getTextColor()));
        
        // Minute spinner
        TextField minuteField = new TextField("00");
        minuteField.setMaxWidth(50);
        minuteField.setStyle(hourField.getStyle());
        minuteField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*") || (newVal.length() > 2) || (!newVal.isEmpty() && Integer.parseInt(newVal) > 59)) {
                minuteField.setText(oldVal);
            }
        });
        
        timePicker.getChildren().addAll(timeLabel, hourField, colonText, minuteField);
        
        // Rebuild calendar grid function
        Runnable rebuildCalendar = () -> {
            monthText.setText(currentMonth[0].getMonth() + " " + currentMonth[0].getYear());
            rebuildCalendarGrid(calendarGrid, currentMonth[0], selectedDateDisplay, hourField, minuteField);
        };
        
        // Initial build
        rebuildCalendar.run();
        
        // Month navigation
        prevMonth.setOnAction(e -> {
            currentMonth[0] = currentMonth[0].minusMonths(1);
            rebuildCalendar.run();
        });
        
        nextMonth.setOnAction(e -> {
            currentMonth[0] = currentMonth[0].plusMonths(1);
            rebuildCalendar.run();
        });
        
        // Update time when fields change
        hourField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedDateTime != null && !newVal.isEmpty()) {
                int hour = Integer.parseInt(newVal);
                selectedDateTime = selectedDateTime.withHour(hour);
                updateSelectedDateDisplay(selectedDateDisplay);
            }
        });
        
        minuteField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedDateTime != null && !newVal.isEmpty()) {
                int minute = Integer.parseInt(newVal);
                selectedDateTime = selectedDateTime.withMinute(minute);
                updateSelectedDateDisplay(selectedDateDisplay);
            }
        });
        
        calendarContainer.getChildren().addAll(selectedDisplay, monthHeader, calendarGrid, timePicker);
        return calendarContainer;
    }
    
    private void rebuildCalendarGrid(GridPane grid, LocalDate month, Text selectedDateDisplay, TextField hourField, TextField minuteField) {
        grid.getChildren().clear();
        LocalDate today = LocalDate.now();
        
        // Day labels
        String[] dayLabels = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};
        for (int i = 0; i < 7; i++) {
            Text dayLabel = new Text(dayLabels[i]);
            dayLabel.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 11));
            dayLabel.setFill(Color.web(textMuted()));
            StackPane labelCell = new StackPane(dayLabel);
            labelCell.setMinHeight(24);
            grid.add(labelCell, i, 0);
        }
        
        int firstDayOfWeek = month.getDayOfWeek().getValue() - 1;
        int daysInMonth = month.lengthOfMonth();
        
        int dayCounter = 1;
        for (int row = 1; row <= 6; row++) {
            for (int col = 0; col < 7; col++) {
                int cellIndex = (row - 1) * 7 + col;
                
                StackPane dayCell = new StackPane();
                dayCell.setMinHeight(36);
                dayCell.setPrefHeight(36);
                dayCell.setStyle(
                    "-fx-background-color: " + surfaceSoft() + ";" +
                    "-fx-background-radius: 8px;" +
                    "-fx-border-color: transparent;" +
                    "-fx-border-width: 1px;"
                );
                
                if (cellIndex >= firstDayOfWeek && dayCounter <= daysInMonth) {
                    int day = dayCounter;
                    LocalDate cellDate = month.withDayOfMonth(day);
                    boolean isToday = cellDate.equals(today);
                    
                    Text dayText = new Text(String.valueOf(day));
                    dayText.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BOLD, 12));
                    dayText.setFill(Color.web(tm.getTextColor()));
                    dayCell.getChildren().add(dayText);
                    dayCell.setCursor(Cursor.HAND);
                    
                    if (isToday) {
                        dayCell.setStyle(
                            "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.2) + ";" +
                            "-fx-background-radius: 8px;" +
                            "-fx-border-color: " + tm.getAccentHex() + ";" +
                            "-fx-border-width: 1px;"
                        );
                    }
                    
                    dayCell.setOnMouseEntered(e -> {
                        if (selectedDateTime == null || !cellDate.equals(selectedDateTime.toLocalDate())) {
                            dayCell.setStyle(
                                "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.15) + ";" +
                                "-fx-background-radius: 8px;" +
                                "-fx-border-color: transparent;"
                            );
                        }
                    });
                    
                    dayCell.setOnMouseExited(e -> {
                        if (selectedDateTime == null || !cellDate.equals(selectedDateTime.toLocalDate())) {
                            dayCell.setStyle(
                                "-fx-background-color: " + surfaceSoft() + ";" +
                                "-fx-background-radius: 8px;" +
                                "-fx-border-color: transparent;"
                            );
                            if (isToday) {
                                dayCell.setStyle(
                                    "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.2) + ";" +
                                    "-fx-background-radius: 8px;" +
                                    "-fx-border-color: " + tm.getAccentHex() + ";" +
                                    "-fx-border-width: 1px;"
                                );
                            }
                        }
                    });
                    
                    dayCell.setOnMouseClicked(e -> {
                        int hour = Integer.parseInt(hourField.getText().isEmpty() ? "0" : hourField.getText());
                        int minute = Integer.parseInt(minuteField.getText().isEmpty() ? "0" : minuteField.getText());
                        selectedDateTime = cellDate.atTime(hour, minute);
                        updateSelectedDateDisplay(selectedDateDisplay);
                        rebuildCalendarGrid(grid, month, selectedDateDisplay, hourField, minuteField);
                    });
                    
                    if (selectedDateTime != null && cellDate.equals(selectedDateTime.toLocalDate())) {
                        dayCell.setStyle(
                            "-fx-background-color: " + tm.getEffectiveAccentGradient() + ";" +
                            "-fx-background-radius: 8px;" +
                            "-fx-border-color: " + tm.getAccentHex() + ";" +
                            "-fx-border-width: 2px;"
                        );
                        dayText.setFill(Color.WHITE);
                    }
                    
                    dayCounter++;
                }
                
                grid.add(dayCell, col, row);
            }
        }
    }
    
    private void updateSelectedDateDisplay(Text displayText) {
        if (selectedDateTime != null) {
            displayText.setText(
                String.format("%s %s, %d at %02d:%02d",
                    selectedDateTime.getDayOfWeek(),
                    selectedDateTime.getMonth(),
                    selectedDateTime.getDayOfMonth(),
                    selectedDateTime.getHour(),
                    selectedDateTime.getMinute())
            );
        } else {
            displayText.setText("Select a date and time");
        }
    }

    private String surfaceStrong() {
        return tm.isDarkMode()
            ? "linear-gradient(from 0% 0% to 100% 100%, #020202 0%, #070707 58%, #0b0b0b 100%)"
            : "linear-gradient(from 0% 0% to 100% 100%, #ffffff 0%, #f8fafc 100%)";
    }

    private String surfaceCard() {
        return tm.isDarkMode()
            ? "linear-gradient(from 0% 0% to 100% 100%, rgba(10,10,10,0.94) 0%, rgba(14,14,14,0.94) 62%, " + tm.toRgba(tm.getAccentHex(), 0.10) + " 100%)"
            : "linear-gradient(from 0% 0% to 100% 100%, rgba(255,255,255,0.98) 0%, rgba(248,250,252,0.96) 100%)";
    }

    private String surfaceSoft() {
        return tm.isDarkMode() ? "rgba(255,255,255,0.08)" : "rgba(15,23,42,0.06)";
    }

    private String inputBg() {
        return tm.isDarkMode() ? "rgba(255,255,255,0.08)" : "rgba(15,23,42,0.06)";
    }

    private String borderSoft() {
        return tm.isDarkMode() ? tm.toRgba(tm.getAccentHex(), 0.34) : "rgba(15,23,42,0.16)";
    }

    private String textMuted() {
        return tm.isDarkMode() ? "rgba(255,255,255,0.79)" : "rgba(30,41,59,0.82)";
    }

    @Override
    public Pane getRoot() {
        return root;
    }

    @Override
    public void cleanup() {}
}


