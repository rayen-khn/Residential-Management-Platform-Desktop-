package com.syndicati.views.frontend.services;

import com.syndicati.MainApplication;
import com.syndicati.controllers.evenement.EvenementController;
import com.syndicati.controllers.evenement.ParticipationController;
import com.syndicati.interfaces.ViewInterface;
import com.syndicati.models.evenement.Evenement;
import com.syndicati.models.user.User;
import com.syndicati.utils.session.SessionManager;
import com.syndicati.utils.theme.ThemeManager;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Evenement page mirrored from Horizon twig/css structure.
 */
public class EvenementPageView implements ViewInterface {

    private final VBox root;
    private final ThemeManager tm = ThemeManager.getInstance();
    private final EvenementController evenementController = new EvenementController();
    private final ParticipationController participationController = new ParticipationController();
    private GridPane eventsGrid;
    private VBox eventsSection;
    private List<Evenement> currentEvents;
    private int currentPage = 0;
    private static final int CARDS_PER_PAGE = 3;
    
    // Form fields (class level for access across methods)
    private TextField titleField;
    private TextField locationField;
    private DatePicker datePicker;
    private ComboBox<String> typeCombo;
    private TextArea descField;
    private TextField placesField;
    private File selectedImageFile = null;

    public EvenementPageView() {
        root = new VBox(26);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20, 0, 44, 0));
        root.setStyle("-fx-background-color: transparent;");

        root.getChildren().addAll(
            buildHeroSection(),
            buildDashboardSection(),
            buildEventsSection()
        );
    }

    private StackPane buildHeroSection() {
        StackPane hero = sectionShell(48, new Insets(128, 64, 128, 64), surfaceStrong(), borderSoft());
        hero.paddingProperty().bind(Bindings.createObjectBinding(
            () -> root.getWidth() < 1050 ? new Insets(58, 30, 58, 30) : new Insets(128, 64, 128, 64),
            root.widthProperty()
        ));

        VBox left = new VBox(18);
        left.setAlignment(Pos.CENTER_LEFT);

        left.getChildren().add(sectionPill("Community Experiences"));

        Text title = text("Discover. Connect.\nExperience.", 80, true, tm.getAccentHex());
        title.wrappingWidthProperty().bind(Bindings.max(300, hero.widthProperty().subtract(120)));
        Text subtitle = text(
            "Join exclusive events, workshops, and gatherings designed for our community. Your next great story starts here.",
            21,
            false,
            textMuted()
        );
        subtitle.wrappingWidthProperty().bind(Bindings.max(280, hero.widthProperty().subtract(180)));

        Button explore = gradientButton("Explore Events", 14, new Insets(12, 26, 12, 26));
        explore.setOnAction(e -> {
            // Scroll to events section
            root.getParent().requestLayout();
        });

        left.getChildren().addAll(title, subtitle, explore);
        hero.getChildren().add(left);
        return hero;
    }

    private StackPane buildDashboardSection() {
        StackPane dashboard = new StackPane();
        dashboard.setPadding(new Insets(96, 80, 96, 80));
        dashboard.prefWidthProperty().bind(Bindings.min(root.widthProperty().multiply(0.95), 1800));
        dashboard.paddingProperty().bind(Bindings.createObjectBinding(
            () -> root.getWidth() < 1100 ? new Insets(38, 24, 38, 24) : new Insets(96, 80, 96, 80),
            root.widthProperty()
        ));
        dashboard.setStyle(
            "-fx-background-color: " + surfaceStrong() + ";" +
            "-fx-background-radius: 48px;" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 48px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.38), 32, 0.16, 0, 8);"
        );

        GridPane content = new GridPane();
        content.setHgap(48);
        content.setVgap(34);

        ColumnConstraints leftCol = new ColumnConstraints();
        leftCol.setPercentWidth(55);
        leftCol.setHgrow(Priority.ALWAYS);
        leftCol.setFillWidth(true);
        ColumnConstraints rightCol = new ColumnConstraints();
        rightCol.setPercentWidth(45);
        rightCol.setHgrow(Priority.ALWAYS);
        rightCol.setFillWidth(true);
        content.getColumnConstraints().addAll(leftCol, rightCol);

        VBox left = new VBox(24);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setMinWidth(0);
        left.setMaxWidth(Double.MAX_VALUE);

        left.getChildren().add(sectionPill("Event Dashboard"));
        
        // Dynamic messaging based on event count
        Text dashTitle = new Text();
        dashTitle.setFont(Font.font(
            MainApplication.getInstance().getBoldFontFamily(),
            FontWeight.BOLD,
            48
        ));
        dashTitle.setFill(Color.web(tm.getAccentHex()));
        dashTitle.wrappingWidthProperty().bind(Bindings.max(280, left.widthProperty().subtract(10)));
        
        Text dashSubtitle = new Text();
        dashSubtitle.setFont(Font.font(
            MainApplication.getInstance().getLightFontFamily(),
            FontWeight.NORMAL,
            18
        ));
        dashSubtitle.setFill(Color.web(textMuted()));
        dashSubtitle.wrappingWidthProperty().bind(Bindings.max(260, left.widthProperty().subtract(12)));

        // Update messaging based on events list
        if (currentEvents != null && !currentEvents.isEmpty()) {
            dashTitle.setText("Join the Action.\n" + currentEvents.size() + " Events Upcoming.");
            dashSubtitle.setText("The community is buzzing! Browse the events below and secure your spot before they fill up.");
        } else {
            dashTitle.setText("The Stage is Set...\nAwaiting Your Pulse.");
            dashSubtitle.setText("It looks like there are no events scheduled at the moment. Be the heartbeat of our community and organize something truly legendary.");
        }

        left.getChildren().addAll(dashTitle, dashSubtitle);

        StackPane statPill = new StackPane(text("Join 500+ members in our next gathering", 13, false, textSoft()));
        statPill.setPadding(new Insets(14, 24, 14, 24));
        statPill.setStyle(
            "-fx-background-color: " + surfaceSoft() + ";" +
            "-fx-background-radius: 20px;" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 20px;"
        );
        left.getChildren().add(statPill);

        StackPane switcher = new StackPane();
        switcher.setMinWidth(0);
        switcher.setMaxWidth(Double.MAX_VALUE);
        switcher.setStyle(
            "-fx-background-color: " + surfaceCard() + ";" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 32px;" +
            "-fx-border-radius: 32px;" +
            "-fx-padding: 26px;"
        );

        VBox mainFace = new VBox(18);
        mainFace.setAlignment(Pos.TOP_LEFT);
        mainFace.setFillWidth(true);
        VBox extraFace = new VBox(14);
        extraFace.setAlignment(Pos.TOP_LEFT);
        extraFace.setFillWidth(true);
        extraFace.setVisible(false);
        extraFace.setManaged(false);

        Button hostTrigger = buildHostTrigger();
        hostTrigger.setOnAction(e -> {
            switchFace(mainFace, extraFace);
            // Clear previous form values
            titleField.clear();
            locationField.clear();
            datePicker.setValue(java.time.LocalDate.now().plusDays(7));
            typeCombo.setValue("social");
            descField.clear();
            placesField.clear();
        });

        mainFace.getChildren().addAll(
            hostTrigger,
            buildGlassCalendar(),
            buildCalendarPreview()
        );

        Button closeForm = iconButton("x");
        closeForm.setOnAction(e -> switchFace(extraFace, mainFace));

        HBox formHead = new HBox();
        formHead.setAlignment(Pos.CENTER_LEFT);
        formHead.getChildren().addAll(
            text("Organize Event", 24, true, tm.getTextColor()),
            spacer(),
            closeForm
        );

        titleField = createTextField("Event Title");
        locationField = createTextField("Location");
        datePicker = createDatePicker();
        typeCombo = createTypeCombo();
        descField = createTextArea("Event Description", 80);
        placesField = createTextField("Total Places");

        Label titleLive = createLiveValidationLabel("Type the event title...");
        Label locationLive = createLiveValidationLabel("Type the event location...");
        Label descLive = createLiveValidationLabel("Describe your event in at least 10 characters...");
        Label placesLive = createLiveValidationLabel("Enter the total number of places...");
        Label dateLive = createLiveValidationLabel("Pick a date for the event...");
        Label typeLive = createLiveValidationLabel("Choose an event type...");

        Button organizeBtn = gradientButton("Organize Now", 12, new Insets(13, 18, 13, 18));
        installLiveValidation(titleField, titleLive, value -> firstEventError(value, locationField.getText(), datePicker, typeCombo, descField.getText(), placesField.getText(), "Title "));
        installLiveValidation(locationField, locationLive, value -> firstEventError(titleField.getText(), value, datePicker, typeCombo, descField.getText(), placesField.getText(), "Location "));
        installLiveValidation(descField, descLive, value -> firstEventError(titleField.getText(), locationField.getText(), datePicker, typeCombo, value, placesField.getText(), "Description "));
        installLiveValidation(placesField, placesLive, value -> firstEventError(titleField.getText(), locationField.getText(), datePicker, typeCombo, descField.getText(), value, "Number of places "));
        installLiveValidation(datePicker, dateLive, value -> firstEventError(titleField.getText(), locationField.getText(), datePicker, typeCombo, descField.getText(), placesField.getText(), "Event date "));
        installLiveValidation(typeCombo, typeLive, value -> firstEventError(titleField.getText(), locationField.getText(), datePicker, typeCombo, descField.getText(), placesField.getText(), "Event type "));

        organizeBtn.setOnAction(e -> {
            if (validateFormFields(titleField, locationField, datePicker, typeCombo, descField, placesField)) {
                User currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    // Handle image upload
                    String imageFileName = null;
                    if (selectedImageFile != null) {
                        imageFileName = copyImageToUploadsFolder(selectedImageFile);
                    }
                    
                    Integer eventId = evenementController.evenementCreate(
                        titleField.getText(),
                        descField.getText(),
                        LocalDateTime.of(datePicker.getValue().atStartOfDay().toLocalDate(), java.time.LocalTime.of(14, 0)),
                        locationField.getText(),
                        Integer.parseInt(placesField.getText()),
                        typeCombo.getValue(),
                        imageFileName,
                        currentUser
                    );
                    if (eventId > 0) {
                        clearFormFields(titleField, locationField, datePicker, descField, placesField);
                        selectedImageFile = null;
                        switchFace(extraFace, mainFace);
                        refreshEventsList();
                        showNotification("Event created successfully!");
                    } else {
                        showNotification("Failed to create event");
                    }
                } else {
                    showNotification("User not logged in");
                }
            }
        });

        extraFace.getChildren().addAll(
            formHead,
            text("Share your vision with the community", 13, false, textMuted()),
            formRowField("Event Title", vboxField(titleField, titleLive)),
            twoColRow(
                formRowField("Date", vboxField(datePicker, dateLive)),
                formRowField("Event Type", vboxField(typeCombo, typeLive))
            ),
            formRowField("Location", vboxField(locationField, locationLive)),
            formRowField("Description", vboxField(descField, descLive)),
            twoColRow(
                formRowField("Total Places", vboxField(placesField, placesLive)),
                formRowField("Initial Remaining", createTextField("80"))
            ),
            buildImageUploadField(),
            organizeBtn
        );

        switcher.getChildren().addAll(mainFace, extraFace);

        content.add(left, 0, 0);
        content.add(switcher, 1, 0);
        updateDashboardColumns(content, switcher, dashboard.getWidth());
        dashboard.widthProperty().addListener((obs, oldW, newW) -> updateDashboardColumns(content, switcher, newW.doubleValue()));

        dashboard.getChildren().add(content);
        return dashboard;
    }

    private void updateDashboardColumns(GridPane content, Node switcher, double width) {
        boolean narrow = width < 1200;
        GridPane.setColumnIndex(switcher, narrow ? 0 : 1);
        GridPane.setRowIndex(switcher, narrow ? 1 : 0);
        if (narrow) {
            content.getColumnConstraints().get(0).setPercentWidth(100);
            content.getColumnConstraints().get(1).setPercentWidth(0);
        } else {
            content.getColumnConstraints().get(0).setPercentWidth(55);
            content.getColumnConstraints().get(1).setPercentWidth(45);
        }
    }

    private VBox buildGlassCalendar() {
        VBox wrap = new VBox(14);
        wrap.setPadding(new Insets(4, 0, 0, 0));

        HBox head = new HBox(8);
        head.setAlignment(Pos.CENTER_LEFT);
        Button prevBtn = iconButton("<");
        prevBtn.setCursor(javafx.scene.Cursor.HAND);
        prevBtn.setOnAction(e -> System.out.println("Previous month"));
        
        Button nextBtn = iconButton(">");
        nextBtn.setCursor(javafx.scene.Cursor.HAND);
        nextBtn.setOnAction(e -> System.out.println("Next month"));
        
        head.getChildren().addAll(
            text("March 2026", 18, true, tm.getAccentHex()),
            spacer(),
            prevBtn,
            nextBtn
        );

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(14.285);
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            grid.getColumnConstraints().add(cc);
        }

        String[] dayLabels = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};
        for (int i = 0; i < dayLabels.length; i++) {
            Text lbl = text(dayLabels[i], 11, true, textMuted());
            StackPane labelCell = new StackPane(lbl);
            labelCell.setMinHeight(20);
            grid.add(labelCell, i, 0);
        }

        int startOffset = 6;
        int day = 1;
        for (int row = 1; row <= 6; row++) {
            for (int col = 0; col < 7; col++) {
                StackPane cell = new StackPane();
                cell.setMaxWidth(Double.MAX_VALUE);
                cell.setMinHeight(34);
                cell.setStyle(
                    "-fx-background-color: " + surfaceSoft() + ";" +
                    "-fx-background-radius: 10px;" +
                    "-fx-border-color: transparent;" +
                    "-fx-border-width: 1px;" +
                    "-fx-border-radius: 10px;"
                );
                cell.setCursor(javafx.scene.Cursor.HAND);

                if (row == 1 && col < startOffset) {
                    cell.getChildren().add(text("", 12, false, textMuted()));
                } else if (day <= 31) {
                    boolean hasEvent = day == 22 || day == 28 || day == 2 || day == 18;
                    boolean active = day == 22;
                    int dayNum = day;

                    Text d = text(String.valueOf(day), 12, true, tm.getTextColor());
                    cell.getChildren().add(d);

                    if (hasEvent) {
                        cell.setStyle(
                            "-fx-background-color: " + surfaceSoft() + ";" +
                            "-fx-background-radius: 10px;" +
                            "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.35) + ";" +
                            "-fx-border-width: 1px;" +
                            "-fx-border-radius: 10px;"
                        );
                        Circle dot = new Circle(3, Color.web(tm.getAccentHex()));
                        StackPane.setAlignment(dot, Pos.BOTTOM_CENTER);
                        StackPane.setMargin(dot, new Insets(0, 0, 4, 0));
                        cell.getChildren().add(dot);
                    }

                    if (active) {
                        cell.setStyle(
                            "-fx-background-color: " + tm.getEffectiveAccentGradient() + ";" +
                            "-fx-background-radius: 10px;" +
                            "-fx-border-color: transparent;" +
                            "-fx-border-width: 1px;" +
                            "-fx-border-radius: 10px;" +
                            "-fx-effect: dropshadow(gaussian, " + tm.toRgba(tm.getAccentHex(), 0.45) + ", 14, 0.3, 0, 3);"
                        );
                    }

                    cell.setOnMouseClicked(e -> System.out.println("Clicked day: " + dayNum));
                    cell.setOnMouseEntered(e -> {
                        if (!active && !hasEvent) {
                            cell.setStyle(
                                "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.15) + ";" +
                                "-fx-background-radius: 10px;" +
                                "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.25) + ";" +
                                "-fx-border-width: 1px;" +
                                "-fx-border-radius: 10px;"
                            );
                        }
                    });
                    cell.setOnMouseExited(e -> {
                        if (!active && !hasEvent) {
                            cell.setStyle(
                                "-fx-background-color: " + surfaceSoft() + ";" +
                                "-fx-background-radius: 10px;" +
                                "-fx-border-color: transparent;" +
                                "-fx-border-width: 1px;" +
                                "-fx-border-radius: 10px;"
                            );
                        }
                    });
                    
                    day++;
                }

                grid.add(cell, col, row);
            }
        }

        HBox tags = new HBox(8,
            tag("#Workshops"),
            tag("#Meetups"),
            tag("#Social"),
            tag("#Sports"),
            tag("+ 6 Events")
        );
        tags.setPadding(new Insets(8, 0, 0, 0));

        wrap.getChildren().addAll(head, grid, tags);
        return wrap;
    }

    private VBox buildCalendarPreview() {
        VBox preview = new VBox(8);
        preview.setPadding(new Insets(14));
        preview.setStyle(
            "-fx-background-color: " + surfaceCard() + ";" +
            "-fx-background-radius: 20px;" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 20px;"
        );

        StackPane banner = new StackPane();
        banner.setMinHeight(90);
        banner.setStyle(
            "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.25) + ";" +
            "-fx-background-radius: 14px;"
        );

        preview.getChildren().addAll(
            banner,
            tag("Meeting"),
            text("Assemblee generale des coproprietaires", 16, true, tm.getTextColor()),
            text("Mar 22, 2026 - 18:30", 12, false, textSoft()),
            gradientButton("View Details", 11, new Insets(8, 10, 8, 10))
        );
        return preview;
    }

    private VBox buildEventsSection() {
        eventsSection = new VBox(18);
        eventsSection.prefWidthProperty().bind(Bindings.min(root.widthProperty().multiply(0.95), 1800));
        eventsSection.setPadding(new Insets(0, 48, 0, 48));
        eventsSection.paddingProperty().bind(Bindings.createObjectBinding(
            () -> root.getWidth() < 992 ? new Insets(0, 14, 0, 14) : new Insets(0, 48, 0, 48),
            root.widthProperty()
        ));

        // Header with section pill and pagination info
        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        
        Text countText = text("Showing 0 of 0", 13, false, textMuted());
        top.getChildren().addAll(
            sectionPill("Upcoming Events"),
            spacer(),
            countText
        );

        eventsGrid = new GridPane();
        eventsGrid.setHgap(48);
        eventsGrid.setVgap(48);
        
        refreshEventsList();
        eventsSection.widthProperty().addListener((obs, oldW, newW) -> rebuildEventsGrid(eventsGrid, newW.doubleValue()));

        // Pagination controls
        HBox pagination = new HBox(8);
        pagination.setAlignment(Pos.CENTER);
        pagination.setStyle("-fx-padding: 20 0 0 0;");
        
        Button prevBtn = paginationBtn("<", false);
        Button nextBtn = paginationBtn(">", false);
        
        HBox pageButtonsContainer = new HBox(4);
        pageButtonsContainer.setAlignment(Pos.CENTER);
        
        prevBtn.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                performPaginationWithScrollPreservation(() -> {
                    rebuildEventsGrid(eventsGrid, eventsSection.getWidth());
                    updatePaginationUI(countText, pageButtonsContainer, prevBtn, nextBtn);
                });
            }
        });
        
        nextBtn.setOnAction(e -> {
            int totalPages = (currentEvents != null) ? (int) Math.ceil((double) currentEvents.size() / CARDS_PER_PAGE) : 0;
            if (currentPage < totalPages - 1) {
                currentPage++;
                performPaginationWithScrollPreservation(() -> {
                    rebuildEventsGrid(eventsGrid, eventsSection.getWidth());
                    updatePaginationUI(countText, pageButtonsContainer, prevBtn, nextBtn);
                });
            }
        });
        
        pagination.getChildren().addAll(prevBtn, pageButtonsContainer, nextBtn);

        eventsSection.getChildren().addAll(top, eventsGrid, pagination);
        return eventsSection;
    }

    private void updatePaginationUI(Text countText, HBox pageButtonsContainer, Button prevBtn, Button nextBtn) {
        if (currentEvents == null || currentEvents.isEmpty()) return;
        
        int totalPages = (int) Math.ceil((double) currentEvents.size() / CARDS_PER_PAGE);
        int start = currentPage * CARDS_PER_PAGE + 1;
        int end = Math.min((currentPage + 1) * CARDS_PER_PAGE, currentEvents.size());
        
        countText.setText("Showing " + start + "-" + end + " of " + currentEvents.size());
        
        pageButtonsContainer.getChildren().clear();
        for (int i = 0; i < totalPages; i++) {
            final int pageNum = i;
            Button pageBtn = paginationBtn(String.valueOf(i + 1), i == currentPage);
            pageBtn.setOnAction(e -> {
                currentPage = pageNum;
                performPaginationWithScrollPreservation(() -> {
                    rebuildEventsGrid(eventsGrid, root.getWidth());
                    updatePaginationUI(countText, pageButtonsContainer, prevBtn, nextBtn);
                });
            });
            pageButtonsContainer.getChildren().add(pageBtn);
        }
        
        prevBtn.setDisable(currentPage == 0);
        nextBtn.setDisable(currentPage >= totalPages - 1);
    }

    private void performPaginationWithScrollPreservation(Runnable updateAction) {
        // Store scroll position before pagination update
        Node parent = eventsSection != null ? eventsSection.getParent() : root.getParent();
        ScrollPane scrollPane = null;
        double savedVvalue = 0;
        
        while (parent != null) {
            if (parent instanceof ScrollPane) {
                scrollPane = (ScrollPane) parent;
                savedVvalue = scrollPane.getVvalue();
                break;
            }
            parent = parent.getParent();
        }
        
        // Perform the update
        updateAction.run();
        
        // Restore scroll position multiple times
        if (scrollPane != null) {
            final ScrollPane sp = scrollPane;
            final double targetVvalue = savedVvalue;
            
            // Immediate restore
            sp.setVvalue(targetVvalue);
            
            // Restore after layout pass
            Platform.runLater(() -> {
                sp.setVvalue(targetVvalue);
            });
            
            // One more restore after a short delay
            Platform.runLater(() -> {
                javafx.animation.Timeline restoreTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(50), e -> {
                        sp.setVvalue(targetVvalue);
                    })
                );
                restoreTimeline.play();
            });
        }
    }

    private void refreshEventsList() {
        currentEvents = evenementController.evenements();
        if (eventsGrid != null) {
            rebuildEventsGrid(eventsGrid, root.getWidth());
        }
    }

    private void rebuildEventsGrid(GridPane grid, double width) {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();

        double effectiveWidth = Math.max(width, grid.getWidth());
        int cols = effectiveWidth < 820 ? 1 : (effectiveWidth < 1400 ? 2 : 3);
        for (int i = 0; i < cols; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(100.0 / cols);
            c.setHgrow(Priority.ALWAYS);
            c.setFillWidth(true);
            grid.getColumnConstraints().add(c);
        }

        if (currentEvents == null || currentEvents.isEmpty()) {
            Text noEvents = text("No events available. Be the first to create one!", 16, false, textMuted());
            StackPane noEventsPane = new StackPane(noEvents);
            noEventsPane.setPadding(new Insets(60));
            grid.add(noEventsPane, 0, 0);
        } else {
            // Calculate which events to show for current page
            int start = currentPage * CARDS_PER_PAGE;
            int end = Math.min(start + CARDS_PER_PAGE, currentEvents.size());
            
            int displayIndex = 0;
            for (int i = start; i < end; i++) {
                Evenement event = currentEvents.get(i);
                VBox card = eventCard(event);
                GridPane.setFillWidth(card, true);
                card.setMaxWidth(Double.MAX_VALUE);
                card.setMinWidth(260);
                grid.add(card, displayIndex % cols, displayIndex / cols);
                displayIndex++;
            }
        }
    }

    private VBox eventCard(Evenement event) {
        VBox card = new VBox();
        card.setMinWidth(280);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
            "-fx-background-color: " + tm.toRgba("#1a1a2e", 0.6) + ";" +
            "-fx-background-radius: 32px;" +
            "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.2) + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 32px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0.15, 0, 8);"
        );
        
        // Dynamic clip for card to prevent overflow
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        clip.widthProperty().bind(card.widthProperty());
        clip.heightProperty().bind(card.heightProperty());
        card.setClip(clip);

        StackPane switcher = new StackPane();
        switcher.setMaxWidth(Double.MAX_VALUE);
        switcher.setMinWidth(0);

        // FACE 1: MAIN
        VBox mainFace = new VBox(0);
        mainFace.setMaxWidth(Double.MAX_VALUE);
        mainFace.setMinWidth(0);
        
        // FACE 2: DETAILS
        VBox detailsFace = new VBox(12);
        detailsFace.setVisible(false);
        detailsFace.setManaged(false);
        detailsFace.setPadding(new Insets(16));
        detailsFace.setMaxWidth(Double.MAX_VALUE);
        detailsFace.setMinWidth(0);
        
        // FACE 3: PARTICIPATE
        VBox participateFace = new VBox(14);
        participateFace.setVisible(false);
        participateFace.setManaged(false);
        participateFace.setPadding(new Insets(16));
        participateFace.setMaxWidth(Double.MAX_VALUE);
        participateFace.setMinWidth(0);
        
        // FACE 4: EDIT
        VBox editFace = new VBox(14);
        editFace.setVisible(false);
        editFace.setManaged(false);
        editFace.setPadding(new Insets(16));
        editFace.setMaxWidth(Double.MAX_VALUE);
        editFace.setMinWidth(0);

        // Image banner with actual event image if available
        StackPane image = new StackPane();
        image.setMinHeight(180);
        image.setMaxHeight(180);
        image.setMaxWidth(Double.MAX_VALUE);
        image.setMinWidth(0);
        image.setStyle(
            "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.12) + ";" +
            "-fx-background-radius: 20px;"
        );
        StackPane.setAlignment(image, Pos.CENTER);

        // Inner container for just the image (with clip)
        StackPane imageClipContainer = new StackPane();
        imageClipContainer.setMinHeight(180);
        imageClipContainer.setMaxHeight(180);
        imageClipContainer.setMaxWidth(Double.MAX_VALUE);
        imageClipContainer.setMinWidth(0);
        
        // Try to load event image
        if (event.getImageEvent() != null && !event.getImageEvent().isEmpty()) {
            try {
                File imageFile = new File("uploads/event_images/" + event.getImageEvent());
                if (imageFile.exists()) {
                    Image eventImage = new Image(imageFile.toURI().toString());
                    ImageView imageView = new ImageView(eventImage);
                    // Bind to container size so it fills completely
                    imageView.fitWidthProperty().bind(imageClipContainer.widthProperty());
                    imageView.fitHeightProperty().bind(imageClipContainer.heightProperty());
                    imageView.setPreserveRatio(false);
                    StackPane.setAlignment(imageView, Pos.CENTER);
                    imageClipContainer.getChildren().add(imageView);
                }
            } catch (Exception e) {
                System.err.println("Failed to load event image: " + e.getMessage());
            }
        }
        
        // Apply clip that adapts to container size
        javafx.scene.shape.Rectangle imageClip = new javafx.scene.shape.Rectangle();
        imageClip.widthProperty().bind(imageClipContainer.widthProperty());
        imageClip.heightProperty().bind(imageClipContainer.heightProperty());
        imageClip.setArcWidth(20);
        imageClip.setArcHeight(20);
        imageClipContainer.setClip(imageClip);
        
        image.getChildren().add(imageClipContainer);

        // Add tag on top (not affected by image clip)
        StackPane typeTag = tag(event.getTypeEvent());
        StackPane.setAlignment(typeTag, Pos.TOP_RIGHT);
        StackPane.setMargin(typeTag, new Insets(12, 12, 0, 0));
        image.getChildren().add(typeTag);

        // Card body
        VBox body = new VBox(12);
        body.setPadding(new Insets(20));
        body.setMaxWidth(Double.MAX_VALUE);
        body.setMinWidth(0);

        String dateStr = event.getDateEvent() != null ? event.getDateEvent().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "TBD";
        
        // Meta info with proper wrapping
        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.setMaxWidth(Double.MAX_VALUE);
        meta.setStyle("-fx-wrap-text: true;");
        Text dateText = text(dateStr, 12, false, textMuted());
        dateText.setWrappingWidth(100);
        Text locationText = text(event.getLieuEvent(), 12, false, textMuted());
        locationText.setWrappingWidth(160);
        locationText.wrappingWidthProperty().bind(body.widthProperty().subtract(140));
        meta.getChildren().addAll(dateText, locationText);
        
        Text title = text(event.getTitreEvent(), 24, true, tm.getTextColor());
        title.setWrappingWidth(280);
        title.wrappingWidthProperty().bind(body.widthProperty().subtract(40));
        
        Text desc = text(event.getDescriptionEvent() != null ? event.getDescriptionEvent() : "", 14, false, textSoft());
        desc.setWrappingWidth(280);
        desc.wrappingWidthProperty().bind(body.widthProperty().subtract(40));
        
        Region pushFooter = new Region();
        VBox.setVgrow(pushFooter, Priority.ALWAYS);

        // Footer with places and button
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 14, 10, 14));
        footer.setStyle(
            "-fx-background-color: " + surfaceSoft() + ";" +
            "-fx-background-radius: 20px;" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 20px;"
        );
        VBox avail = new VBox(2,
            text(String.valueOf(event.getNbRestants() != null ? event.getNbRestants() : 0), 18, true, tm.getTextColor()),
            text("Places Left", 11, false, textMuted())
        );
        Button details = gradientButton("View Details", 11, new Insets(8, 14, 8, 14));
        details.setOnAction(ev -> switchFace(mainFace, detailsFace));
        footer.getChildren().addAll(avail, spacer(), details);

        body.getChildren().addAll(meta, title, desc, pushFooter, footer);
        mainFace.getChildren().addAll(image, body);

        // ============ FACE 2: DETAILS ============
        HBox detailsHead = new HBox();
        detailsHead.setAlignment(Pos.CENTER_LEFT);
        detailsHead.setMaxWidth(Double.MAX_VALUE);
        detailsHead.setMinWidth(0);
        Button backFromDetails = iconButton("<");
        backFromDetails.setPrefSize(40, 40);
        backFromDetails.setOnAction(ev -> switchFace(detailsFace, mainFace));
        detailsHead.getChildren().addAll(text("Details", 22, true, tm.getTextColor()), spacer(), backFromDetails);

        // Scrollable content area
        VBox detailsScrollContent = new VBox(14);
        detailsScrollContent.setMaxWidth(Double.MAX_VALUE);
        detailsScrollContent.setMinWidth(0);
        detailsScrollContent.setStyle("-fx-padding: 0;");

        // Image
        StackPane detailImage = new StackPane();
        detailImage.setMinHeight(160);
        detailImage.setMaxHeight(160);
        detailImage.setMaxWidth(Double.MAX_VALUE);
        detailImage.setMinWidth(0);
        
        // Inner container for just the image (with clip)
        StackPane detailImageClipContainer = new StackPane();
        detailImageClipContainer.setMinHeight(160);
        detailImageClipContainer.setMaxHeight(160);
        detailImageClipContainer.setMaxWidth(Double.MAX_VALUE);
        detailImageClipContainer.setMinWidth(0);
        
        if (event.getImageEvent() != null && !event.getImageEvent().isEmpty()) {
            try {
                File imageFile = new File("uploads/event_images/" + event.getImageEvent());
                if (imageFile.exists()) {
                    Image eventImage = new Image(imageFile.toURI().toString());
                    ImageView imageView = new ImageView(eventImage);
                    // Bind to container size so it fills completely
                    imageView.fitWidthProperty().bind(detailImageClipContainer.widthProperty());
                    imageView.fitHeightProperty().bind(detailImageClipContainer.heightProperty());
                    imageView.setPreserveRatio(false);
                    StackPane.setAlignment(imageView, Pos.CENTER);
                    detailImageClipContainer.getChildren().add(imageView);
                }
            } catch (Exception e) {
                System.err.println("Failed to load image: " + e.getMessage());
            }
        }
        
        // Apply clip that adapts to container size
        javafx.scene.shape.Rectangle detailClip = new javafx.scene.shape.Rectangle();
        detailClip.widthProperty().bind(detailImageClipContainer.widthProperty());
        detailClip.heightProperty().bind(detailImageClipContainer.heightProperty());
        detailClip.setArcWidth(16);
        detailClip.setArcHeight(16);
        detailImageClipContainer.setClip(detailClip);
        
        detailImage.getChildren().add(detailImageClipContainer);
        detailImage.setStyle(
            "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.12) + ";" +
            "-fx-background-radius: 16px;"
        );

        // Split layout (Left: core info, Right: availability + organizer)
        HBox splitLayout = new HBox(12);
        splitLayout.setPadding(new Insets(12, 0, 0, 0));
        splitLayout.setStyle("-fx-border-color: " + tm.toRgba("ffffff", 0.1) + "; -fx-border-width: 1 0 0 0;");
        splitLayout.setMaxWidth(Double.MAX_VALUE);
        splitLayout.setMinWidth(0);
        
        // Left side: Core info
        VBox leftInfo = new VBox(8);
        leftInfo.setMaxWidth(Double.MAX_VALUE);
        leftInfo.setMinWidth(0);
        Text detailTitle = text(event.getTitreEvent(), 18, true, tm.getTextColor());
        detailTitle.setWrappingWidth(160);
        detailTitle.wrappingWidthProperty().bind(leftInfo.widthProperty().subtract(20));
        
        HBox datePill = new HBox(4);
        datePill.setStyle("-fx-background-color: " + tm.toRgba("ffffff", 0.05) + "; -fx-padding: 6 10 6 10; -fx-background-radius: 8;");
        datePill.getChildren().addAll(text("📅", 10, false, tm.getAccentHex()), text(dateStr, 10, false, textMuted()));
        
        HBox locPill = new HBox(4);
        locPill.setStyle("-fx-background-color: " + tm.toRgba("ffffff", 0.05) + "; -fx-padding: 6 10 6 10; -fx-background-radius: 8;");
        locPill.getChildren().addAll(text("📍", 10, false, tm.getAccentHex()), text(event.getLieuEvent(), 10, false, textMuted()));
        
        leftInfo.getChildren().addAll(detailTitle, datePill, locPill);
        HBox.setHgrow(leftInfo, Priority.ALWAYS);
        
        // Right side: Availability + Organizer
        VBox rightInfo = new VBox(10);
        rightInfo.setAlignment(Pos.TOP_RIGHT);
        rightInfo.setPadding(new Insets(0, 0, 0, 12));
        rightInfo.setStyle("-fx-border-color: " + tm.toRgba("ffffff", 0.05) + "; -fx-border-width: 0 0 0 1;");
        rightInfo.setMaxWidth(Double.MAX_VALUE);
        rightInfo.setMinWidth(0);
        
        VBox availBadge = new VBox(2);
        availBadge.setAlignment(Pos.CENTER_RIGHT);
        availBadge.getChildren().addAll(
            text(String.valueOf(event.getNbRestants() != null ? event.getNbRestants() : 0), 20, true, tm.getAccentHex()),
            text("PLACES LEFT", 9, true, textMuted())
        );
        
        VBox organizerBadge = new VBox(2);
        organizerBadge.setAlignment(Pos.CENTER_RIGHT);
        organizerBadge.getChildren().addAll(
            text("HOSTED BY", 8, true, textMuted()),
            text(event.getUser() != null ? (event.getUser().getFirstName() + " " + event.getUser().getLastName()) : "Community", 11, true, tm.getTextColor())
        );
        
        rightInfo.getChildren().addAll(availBadge, organizerBadge);
        
        splitLayout.getChildren().addAll(leftInfo, rightInfo);
        
        // Map & Weather placeholders
        VBox mapWeatherBox = new VBox(12);
        mapWeatherBox.setMaxWidth(Double.MAX_VALUE);
        mapWeatherBox.setPadding(new Insets(12));
        mapWeatherBox.setStyle(
            "-fx-background-color: " + tm.toRgba("ffffff", 0.02) + ";" +
            "-fx-border-color: " + tm.toRgba("ffffff", 0.04) + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 20;" +
            "-fx-background-radius: 20;"
        );
        
        HBox mapPlaceholder = new HBox();
        mapPlaceholder.setAlignment(Pos.CENTER);
        mapPlaceholder.setMinHeight(100);
        mapPlaceholder.setMaxWidth(Double.MAX_VALUE);
        mapPlaceholder.setStyle(
            "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.08) + ";" +
            "-fx-border-radius: 12;"
        );
        mapPlaceholder.getChildren().add(text("📍 Map would appear here", 12, false, textMuted()));
        
        HBox weatherPlaceholder = new HBox();
        weatherPlaceholder.setAlignment(Pos.CENTER);
        weatherPlaceholder.setMinHeight(80);
        weatherPlaceholder.setMaxWidth(Double.MAX_VALUE);
        weatherPlaceholder.setStyle(
            "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.08) + ";" +
            "-fx-border-radius: 12;"
        );
        weatherPlaceholder.getChildren().add(text("☀️ Weather widget would appear here", 12, false, textMuted()));
        
        mapWeatherBox.getChildren().addAll(mapPlaceholder, weatherPlaceholder);
        
        // Description
        Text detailDesc = text(event.getDescriptionEvent() != null ? event.getDescriptionEvent() : "No description provided", 12, false, textSoft());
        detailDesc.setWrappingWidth(280);
        detailDesc.wrappingWidthProperty().bind(detailsScrollContent.widthProperty().subtract(40));
        
        VBox descBox = new VBox();
        descBox.setMaxWidth(Double.MAX_VALUE);
        descBox.setPadding(new Insets(12));
        descBox.setStyle(
            "-fx-background-color: " + tm.toRgba("ffffff", 0.02) + ";" +
            "-fx-border-color: " + tm.toRgba("ffffff", 0.04) + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 20;" +
            "-fx-background-radius: 20;"
        );
        descBox.getChildren().add(detailDesc);
        
        detailsScrollContent.getChildren().addAll(detailImage, splitLayout, mapWeatherBox, descBox);
        detailsFace.getChildren().addAll(detailsHead, detailsScrollContent);

        // Action buttons for details face
        User currentUser = SessionManager.getInstance().getCurrentUser();
        HBox detailsActions = new HBox(8);
        detailsActions.setMaxWidth(Double.MAX_VALUE);
        detailsActions.setMinWidth(0);
        detailsActions.setPadding(new Insets(12, 0, 0, 0));
        detailsActions.setStyle("-fx-border-color: " + tm.toRgba("ffffff", 0.1) + "; -fx-border-width: 1 0 0 0;");
        
        if (currentUser != null && event.getNbRestants() != null && event.getNbRestants() > 0) {
            Button joinDetailsBtn = gradientButton("Join Now", 12, new Insets(11, 16, 11, 16));
            joinDetailsBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(joinDetailsBtn, Priority.ALWAYS);
            joinDetailsBtn.setOnAction(e -> switchFace(detailsFace, participateFace));
            detailsActions.getChildren().add(joinDetailsBtn);
        }
        
        if (currentUser != null && event.getUser() != null && event.getUser().getIdUser().equals(currentUser.getIdUser())) {
            Button editBtn = new Button("✏️ Edit");
            editBtn.setStyle(
                "-fx-background-color: " + tm.toRgba("ffffff", 0.05) + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 12;" +
                "-fx-background-radius: 14;" +
                "-fx-padding: 11 16 11 16;" +
                "-fx-border-color: " + tm.toRgba("ffffff", 0.1) + ";" +
                "-fx-border-width: 1;"
            );
            editBtn.setMaxWidth(Double.MAX_VALUE);
            editBtn.setPrefHeight(40);
            HBox.setHgrow(editBtn, Priority.ALWAYS);
            editBtn.setOnAction(e -> switchFace(detailsFace, editFace));
            detailsActions.getChildren().add(editBtn);
            
            Button deleteDetailsBtn = new Button("🗑️ Delete");
            deleteDetailsBtn.setStyle(
                "-fx-background-color: rgba(255, 77, 77, 0.1);" +
                "-fx-text-fill: #ff6b6b;" +
                "-fx-font-size: 12;" +
                "-fx-background-radius: 14;" +
                "-fx-padding: 11 16 11 16;" +
                "-fx-border-color: rgba(255, 77, 77, 0.2);" +
                "-fx-border-width: 1;"
            );
            deleteDetailsBtn.setMaxWidth(Double.MAX_VALUE);
            deleteDetailsBtn.setPrefHeight(40);
            HBox.setHgrow(deleteDetailsBtn, Priority.ALWAYS);
            deleteDetailsBtn.setOnAction(e -> {
                if (evenementController.evenementDelete(event.getIdEvent())) {
                    showNotification("✓ Event deleted successfully!");
                    refreshEventsList();
                } else {
                    showNotification("Failed to delete event");
                }
            });
            detailsActions.getChildren().add(deleteDetailsBtn);
        }
        
        detailsFace.getChildren().add(detailsActions);

        // ============ FACE 3: PARTICIPATE (Form) ============
        HBox partHead = new HBox();
        partHead.setAlignment(Pos.CENTER_LEFT);
        partHead.setMaxWidth(Double.MAX_VALUE);
        partHead.setMinWidth(0);
        Button backFromParticipate = iconButton("<");
        backFromParticipate.setPrefSize(40, 40);
        backFromParticipate.setOnAction(ev -> switchFace(participateFace, detailsFace));
        partHead.getChildren().addAll(text("Register", 22, true, tm.getTextColor()), spacer(), backFromParticipate);

        TextField accompagnantsField = createTextField("0");
        accompagnantsField.setStyle(accompagnantsField.getStyle() + "-fx-text-alignment: center;");
        
        TextArea noteField = new TextArea();
        noteField.setWrapText(true);
        noteField.setPrefRowCount(3);
        noteField.setPromptText("Anything we should know?");
        noteField.setStyle(
            "-fx-control-inner-background: " + tm.toRgba("ffffff", 0.05) + ";" +
            "-fx-font-size: 12;" +
            "-fx-font-family: '" + MainApplication.getInstance().getLightFontFamily() + "';" +
            "-fx-text-fill: white;" +
            "-fx-padding: 12;" +
            "-fx-border-color: " + tm.toRgba("ffffff", 0.1) + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;" +
            "-fx-focus-color: " + tm.getAccentHex() + ";"
        );
        
        Button confirmParticipationBtn = gradientButton("Confirm Spot", 12, new Insets(12, 18, 12, 18));
        confirmParticipationBtn.setMaxWidth(Double.MAX_VALUE);
        confirmParticipationBtn.setPrefHeight(50);
        confirmParticipationBtn.setOnAction(e -> {
            try {
                int accompagnants = Integer.parseInt(accompagnantsField.getText());
                Integer participationId = participationController.participationCreate(event, currentUser, accompagnants, noteField.getText());
                if (participationId > 0) {
                    showNotification("✓ You joined the event!");
                    refreshEventsList();
                    switchFace(participateFace, mainFace);
                } else {
                    showNotification("Failed to join event");
                }
            } catch (NumberFormatException ex) {
                showNotification("Please enter a valid number for guests");
            }
        });

        participateFace.getChildren().addAll(
            partHead,
            formRowField("Guests Joining You", accompagnantsField),
            formRowField("Special Note", noteField),
            confirmParticipationBtn
        );

        // ============ FACE 4: EDIT (Form) ============
        HBox editHead = new HBox();
        editHead.setAlignment(Pos.CENTER_LEFT);
        editHead.setMaxWidth(Double.MAX_VALUE);
        editHead.setMinWidth(0);
        Button backFromEdit = iconButton("<");
        backFromEdit.setPrefSize(40, 40);
        backFromEdit.setOnAction(ev -> switchFace(editFace, detailsFace));
        editHead.getChildren().addAll(text("Edit Event", 22, true, tm.getTextColor()), spacer(), backFromEdit);

        TextField editTitleField = createTextField(event.getTitreEvent());
        TextField editLocationField = createTextField(event.getLieuEvent());
        TextArea editDescField = new TextArea();
        editDescField.setText(event.getDescriptionEvent() != null ? event.getDescriptionEvent() : "");
        editDescField.setWrapText(true);
        editDescField.setPrefRowCount(3);
        editDescField.setStyle(
            "-fx-control-inner-background: " + tm.toRgba("ffffff", 0.05) + ";" +
            "-fx-font-size: 12;" +
            "-fx-font-family: '" + MainApplication.getInstance().getLightFontFamily() + "';" +
            "-fx-text-fill: white;" +
            "-fx-padding: 12;" +
            "-fx-border-color: " + tm.toRgba("ffffff", 0.1) + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;" +
            "-fx-focus-color: " + tm.getAccentHex() + ";"
        );
        
        // Date picker for editing event date
        javafx.scene.control.DatePicker editDatePicker = new javafx.scene.control.DatePicker();
        if (event.getDateEvent() != null) {
            editDatePicker.setValue(event.getDateEvent().toLocalDate());
        }
        editDatePicker.setStyle(
            "-fx-background-color: " + tm.toRgba("ffffff", 0.05) + ";" +
            "-fx-border-color: " + tm.toRgba("ffffff", 0.1) + ";" +
            "-fx-border-radius: 12;" +
            "-fx-padding: 10;"
        );
        
        ComboBox<String> editTypeCombo = new ComboBox<>();
        editTypeCombo.getItems().addAll("reunion", "social", "formation", "maintenance", "culturel", "sportif");
        editTypeCombo.setValue(event.getTypeEvent());
        editTypeCombo.setStyle(
            "-fx-font-size: 12;" +
            "-fx-text-fill: white;" +
            "-fx-background-color: " + tm.toRgba("ffffff", 0.05) + ";" +
            "-fx-border-color: " + tm.toRgba("ffffff", 0.1) + ";" +
            "-fx-border-radius: 12;" +
            "-fx-padding: 12;"
        );
        
        TextField editPlacesField = createTextField(String.valueOf(event.getNbPlaces()));

        Label editTitleLive = createLiveValidationLabel("Type the event title...");
        Label editLocationLive = createLiveValidationLabel("Type the event location...");
        Label editDescLive = createLiveValidationLabel("Describe your event in at least 10 characters...");
        Label editPlacesLive = createLiveValidationLabel("Enter the total number of places...");
        Label editDateLive = createLiveValidationLabel("Pick a date for the event...");
        Label editTypeLive = createLiveValidationLabel("Choose an event type...");

        installLiveValidation(editTitleField, editTitleLive, value -> firstEventError(value, editLocationField.getText(), editDatePicker, editTypeCombo, editDescField.getText(), editPlacesField.getText(), "Title "));
        installLiveValidation(editLocationField, editLocationLive, value -> firstEventError(editTitleField.getText(), value, editDatePicker, editTypeCombo, editDescField.getText(), editPlacesField.getText(), "Location "));
        installLiveValidation(editDescField, editDescLive, value -> firstEventError(editTitleField.getText(), editLocationField.getText(), editDatePicker, editTypeCombo, value, editPlacesField.getText(), "Description "));
        installLiveValidation(editPlacesField, editPlacesLive, value -> firstEventError(editTitleField.getText(), editLocationField.getText(), editDatePicker, editTypeCombo, editDescField.getText(), value, "Number of places "));
        installLiveValidation(editDatePicker, editDateLive, value -> firstEventError(editTitleField.getText(), editLocationField.getText(), editDatePicker, editTypeCombo, editDescField.getText(), editPlacesField.getText(), "Event date "));
        installLiveValidation(editTypeCombo, editTypeLive, value -> firstEventError(editTitleField.getText(), editLocationField.getText(), editDatePicker, editTypeCombo, editDescField.getText(), editPlacesField.getText(), "Event type "));
        
        Button saveEditBtn = gradientButton("Save Changes", 12, new Insets(12, 18, 12, 18));
        saveEditBtn.setMaxWidth(Double.MAX_VALUE);
        saveEditBtn.setPrefHeight(50);
        saveEditBtn.setOnAction(e -> {
            if (validateFormFields(editTitleField, editLocationField, editDatePicker, editTypeCombo, editDescField, editPlacesField)) {
                if (evenementController.evenementUpdate(
                    event.getIdEvent(),
                    editTitleField.getText(),
                    editDescField.getText(),
                    java.time.LocalDateTime.of(editDatePicker.getValue(), java.time.LocalTime.of(10, 0)),
                    editLocationField.getText(),
                    Integer.parseInt(editPlacesField.getText()),
                    editTypeCombo.getValue()
                )) {
                    showNotification("✓ Event updated successfully!");
                    refreshEventsList();
                    switchFace(editFace, mainFace);
                } else {
                    showNotification("Failed to update event");
                }
            }
        });

        editFace.getChildren().addAll(
            editHead,
            formRowField("Event Title", vboxField(editTitleField, editTitleLive)),
            formRowField("Location", vboxField(editLocationField, editLocationLive)),
            formRowField("Event Date", vboxField(editDatePicker, editDateLive)),
            formRowField("Event Type", vboxField(editTypeCombo, editTypeLive)),
            formRowField("Description", vboxField(editDescField, editDescLive)),
            formRowField("Total Places", vboxField(editPlacesField, editPlacesLive)),
            saveEditBtn
        );

        switcher.getChildren().addAll(mainFace, detailsFace, participateFace, editFace);
        card.getChildren().add(switcher);

        addHoverLift(card);
        return card;
    }

    private String copyImageToUploadsFolder(File sourceFile) {
        try {
            String fileName = sourceFile.getName();
            String uniqueFileName = fileName.replaceFirst("\\.[^.]+$", "") + "-" + System.currentTimeMillis() + fileName.substring(fileName.lastIndexOf("."));
            
            File uploadsDir = new File("uploads/event_images");
            if (!uploadsDir.exists()) {
                uploadsDir.mkdirs();
            }
            
            File destFile = new File(uploadsDir, uniqueFileName);
            java.nio.file.Files.copy(sourceFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            return uniqueFileName;
        } catch (Exception e) {
            System.err.println("Failed to copy image file: " + e.getMessage());
            return null;
        }
    }

    private void switchFace(Node from, Node to) {
        // Find and store scroll position to prevent unwanted scrolling
        Node parent = eventsSection != null ? eventsSection.getParent() : root.getParent();
        ScrollPane scrollPane = null;
        double savedVvalue = 0;
        
        while (parent != null) {
            if (parent instanceof ScrollPane) {
                scrollPane = (ScrollPane) parent;
                savedVvalue = scrollPane.getVvalue();
                break;
            }
            parent = parent.getParent();
        }
        
        // Switch faces
        from.setVisible(false);
        from.setManaged(false);
        to.setVisible(true);
        to.setManaged(true);
        
        // Restore scroll position multiple times to ensure it sticks
        if (scrollPane != null) {
            final ScrollPane sp = scrollPane;
            final double targetVvalue = savedVvalue;
            
            // Immediate restore
            sp.setVvalue(targetVvalue);
            
            // Restore after layout pass
            Platform.runLater(() -> {
                sp.setVvalue(targetVvalue);
            });
            
            // One more restore after a short delay for layout finalization
            Platform.runLater(() -> {
                javafx.animation.Timeline restoreTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(50), e -> {
                        sp.setVvalue(targetVvalue);
                    })
                );
                restoreTimeline.play();
            });
        }
    }

    private StackPane sectionPill(String value) {
        StackPane pill = new StackPane(text(value, 11, true, "#ffffff"));
        pill.setPadding(new Insets(8, 14, 8, 14));
        pill.setMaxWidth(StackPane.USE_PREF_SIZE);
        pill.setStyle(
            "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.12) + ";" +
            "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.25) + ";" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 100px;" +
            "-fx-border-radius: 100px;"
        );
        return pill;
    }

    private StackPane tag(String value) {
        StackPane pill = new StackPane(text(value, 8, true, textSoft()));
        pill.setPadding(new Insets(2, 7, 2, 7));
        pill.setMaxWidth(StackPane.USE_PREF_SIZE);
        pill.setMaxHeight(StackPane.USE_PREF_SIZE);
        pill.setStyle(
            "-fx-background-color: " + surfaceSoft() + ";" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 14px;" +
            "-fx-border-radius: 14px;"
        );
        return pill;
    }

    private Button buildHostTrigger() {
        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMaxWidth(Double.MAX_VALUE);
        
        // Icon box
        StackPane iconBox = new StackPane(text("+", 20, true, "#ffffff"));
        iconBox.setStyle(
            "-fx-background-color: " + tm.getEffectiveAccentGradient() + ";" +
            "-fx-background-radius: 12px;" +
            "-fx-min-width: 40px;" +
            "-fx-min-height: 40px;"
        );
        
        // Text section
        VBox textSection = new VBox(2);
        textSection.setAlignment(Pos.CENTER_LEFT);
        Text title = text("Host an Event", 14, true, "#ffffff");
        Text subtitle = text("Start your community journey", 11, false, "rgba(255,255,255,0.4)");
        textSection.getChildren().addAll(title, subtitle);
        
        // Chevron
        Text chevron = text(">", 18, true, "rgba(255,255,255,0.5)");
        
        content.getChildren().addAll(iconBox, textSection, spacer(), chevron);
        
        Button btn = new Button();
        btn.setGraphic(content);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setCursor(javafx.scene.Cursor.HAND);
        btn.setStyle(
            "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.12) + ";" +
            "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.28) + ";" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 20px;" +
            "-fx-border-radius: 20px;" +
            "-fx-padding: 12 16 12 16;"
        );
        return btn;
    }

    private Button gradientButton(String label, int fontSize, Insets padding) {
        Button b = new Button(label);
        b.setCursor(javafx.scene.Cursor.HAND);
        b.setStyle(
            "-fx-background-color: " + tm.getEffectiveAccentGradient() + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: 700;" +
            "-fx-font-size: " + fontSize + "px;" +
            "-fx-background-radius: 14px;"
        );
        b.setPadding(padding);
        return b;
    }

    private Button iconButton(String label) {
        Button b = new Button(label);
        b.setCursor(javafx.scene.Cursor.HAND);
        b.setStyle(
            "-fx-background-color: " + surfaceSoft() + ";" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 10px;" +
            "-fx-border-radius: 10px;" +
            "-fx-text-fill: " + textSoft() + ";" +
            "-fx-font-weight: 700;"
        );
        return b;
    }

    private Button paginationBtn(String label, boolean active) {
        Button b = new Button(label);
        b.setCursor(javafx.scene.Cursor.HAND);
        if (active) {
            b.setStyle(
                "-fx-background-color: " + tm.getEffectiveAccentGradient() + ";" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 10px;" +
                "-fx-font-weight: 700;" +
                "-fx-min-width: 40px; -fx-min-height: 40px;"
            );
        } else {
            b.setStyle(
                "-fx-background-color: " + surfaceSoft() + ";" +
                "-fx-border-color: " + borderSoft() + ";" +
                "-fx-border-width: 1px;" +
                "-fx-text-fill: " + textSoft() + ";" +
                "-fx-background-radius: 10px;" +
                "-fx-border-radius: 10px;" +
                "-fx-font-weight: 700;" +
                "-fx-min-width: 40px; -fx-min-height: 40px;"
            );
        }
        return b;
    }

    private VBox formRow(String label, String value) {
        VBox row = new VBox(4);
        Text l = text(label, 11, true, textSoft());
        StackPane field = new StackPane(text(value, 12, false, tm.getTextColor()));
        field.setAlignment(Pos.CENTER_LEFT);
        field.setPadding(new Insets(10, 12, 10, 12));
        field.setStyle(
            "-fx-background-color: " + surfaceSoft() + ";" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 16px;" +
            "-fx-border-radius: 16px;"
        );
        row.getChildren().addAll(l, field);
        return row;
    }

    private VBox formRowField(String label, Node field) {
        VBox row = new VBox(4);
        Text l = text(label, 11, true, textSoft());
        row.getChildren().addAll(l, field);
        return row;
    }

    private TextField createTextField(String initialValue) {
        TextField field = new TextField();
        if (initialValue != null && !initialValue.isBlank()) {
            field.setText(initialValue);
        }
        field.setStyle(
            "-fx-background-color: " + surfaceSoft() + ";" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 16px;" +
            "-fx-border-radius: 16px;" +
            "-fx-text-fill: " + tm.getTextColor() + ";" +
            "-fx-padding: 10 12 10 12;" +
            "-fx-font-size: 12px;"
        );
        return field;
    }

    private TextArea createTextArea(String prompt, double height) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefRowCount(4);
        area.setWrapText(true);
        area.setStyle(
            "-fx-background-color: " + surfaceSoft() + ";" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 16px;" +
            "-fx-border-radius: 16px;" +
            "-fx-text-fill: " + tm.getTextColor() + ";" +
            "-fx-padding: 10 12 10 12;" +
            "-fx-font-size: 12px;"
        );
        return area;
    }

    private DatePicker createDatePicker() {
        DatePicker picker = new DatePicker();
        picker.setStyle(
            "-fx-background-color: " + surfaceSoft() + ";" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 16px;" +
            "-fx-border-radius: 16px;" +
            "-fx-text-fill: " + tm.getTextColor() + ";" +
            "-fx-padding: 10 12 10 12;"
        );
        picker.setValue(java.time.LocalDate.now().plusDays(7));
        return picker;
    }

    private ComboBox<String> createTypeCombo() {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(
            "reunion", "social", "formation", "maintenance", "culturel", "sportif"
        );
        combo.setValue("social");
        combo.setStyle(
            "-fx-background-color: " + surfaceSoft() + ";" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 16px;" +
            "-fx-border-radius: 16px;" +
            "-fx-text-fill: " + tm.getTextColor() + ";" +
            "-fx-padding: 10 12 10 12;"
        );
        return combo;
    }

    private boolean validateFormFields(TextField title, TextField location, DatePicker date, ComboBox<String> type, TextArea desc, TextField places) {
        List<String> errors = buildEventDraft(
            title.getText(),
            location.getText(),
            date,
            type,
            desc.getText(),
            places.getText()
        ).validateForCreate();
        if (!errors.isEmpty()) {
            showNotification(errors.getFirst());
            return false;
        }
        return true;
    }

    private interface LiveValidator {
        String validate(String value);
    }

    private VBox vboxField(Node input, Label liveLabel) {
        VBox box = new VBox(4, input, liveLabel);
        box.setFillWidth(true);
        return box;
    }

    private Label createLiveValidationLabel(String message) {
        Label label = new Label(message);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: 600;");
        label.setTextFill(Color.web(textMuted()));
        return label;
    }

    private void installLiveValidation(TextField field, Label label, LiveValidator validator) {
        Runnable update = () -> updateLiveValidationLabel(label, validator.validate(field.getText()));
        field.textProperty().addListener((obs, oldValue, newValue) -> update.run());
        update.run();
    }

    private void installLiveValidation(TextArea field, Label label, LiveValidator validator) {
        Runnable update = () -> updateLiveValidationLabel(label, validator.validate(field.getText()));
        field.textProperty().addListener((obs, oldValue, newValue) -> update.run());
        update.run();
    }

    private void installLiveValidation(DatePicker field, Label label, LiveValidator validator) {
        Runnable update = () -> updateLiveValidationLabel(label, validator.validate(field.getValue() == null ? "" : field.getValue().toString()));
        field.valueProperty().addListener((obs, oldValue, newValue) -> update.run());
        update.run();
    }

    private void installLiveValidation(ComboBox<String> field, Label label, LiveValidator validator) {
        Runnable update = () -> updateLiveValidationLabel(label, validator.validate(field.getValue()));
        field.valueProperty().addListener((obs, oldValue, newValue) -> update.run());
        update.run();
    }

    private void updateLiveValidationLabel(Label label, String error) {
        if (error == null || error.isBlank()) {
            label.setText("✓ Looks good");
            label.setTextFill(Color.web(tm.getAccentHex()));
            return;
        }
        label.setText(error);
        label.setTextFill(Color.web("#ff3b30"));
    }

    private String validateEventTitleInput(String value) {
        return firstEventError(value, locationField.getText(), datePicker, typeCombo, descField.getText(), placesField.getText(), "Title ");
    }

    private String validateEventLocationInput(String value) {
        return firstEventError(titleField.getText(), value, datePicker, typeCombo, descField.getText(), placesField.getText(), "Location ");
    }

    private String validateEventDescriptionInput(String value) {
        return firstEventError(titleField.getText(), locationField.getText(), datePicker, typeCombo, value, placesField.getText(), "Description ");
    }

    private String validateEventPlacesInput(String value) {
        return firstEventError(titleField.getText(), locationField.getText(), datePicker, typeCombo, descField.getText(), value, "Number of places ");
    }

    private String validateEventDateInput(DatePicker picker) {
        return firstEventError(titleField.getText(), locationField.getText(), picker, typeCombo, descField.getText(), placesField.getText(), "Event date ");
    }

    private String validateEventTypeInput(ComboBox<String> combo) {
        return firstEventError(titleField.getText(), locationField.getText(), datePicker, combo, descField.getText(), placesField.getText(), "Event type ");
    }

    private String firstEventError(String title, String location, DatePicker date, ComboBox<String> type, String description, String places, String prefix) {
        List<String> errors = buildEventDraft(title, location, date, type, description, places).validateForCreate();
        for (String error : errors) {
            if (error.startsWith(prefix)) {
                return error;
            }
        }
        return null;
    }

    private Evenement buildEventDraft(String title, String location, DatePicker date, ComboBox<String> type, String description, String places) {
        Evenement draft = new Evenement();
        draft.setTitreEvent(title == null ? "" : title.trim());
        draft.setLieuEvent(location == null ? "" : location.trim());
        draft.setDescriptionEvent(description == null ? "" : description.trim());
        draft.setDateEvent(date != null && date.getValue() != null
            ? LocalDateTime.of(date.getValue(), java.time.LocalTime.of(10, 0))
            : null);
        draft.setTypeEvent(type == null ? null : type.getValue());

        Integer parsedPlaces = null;
        try {
            if (places != null && !places.trim().isEmpty()) {
                parsedPlaces = Integer.parseInt(places.trim());
            }
        } catch (NumberFormatException e) {
            parsedPlaces = null;
        }
        draft.setNbPlaces(parsedPlaces);

        User current = SessionManager.getInstance().getCurrentUser();
        if (current != null && current.getIdUser() != null && current.getIdUser() > 0) {
            draft.setUser(current);
        } else {
            User placeholder = new User();
            placeholder.setIdUser(1);
            draft.setUser(placeholder);
        }

        return draft;
    }

    private void clearFormFields(TextField title, TextField location, DatePicker date, TextArea desc, TextField places) {
        title.clear();
        location.clear();
        date.setValue(java.time.LocalDate.now().plusDays(7));
        desc.clear();
        places.clear();
    }

    private void showNotification(String message) {
        System.out.println("[Notification] " + message);
    }

    private VBox buildImageUploadField() {
        VBox row = new VBox(4);
        Text label = text("Event Banner", 11, true, textSoft());
        
        StackPane uploadArea = new StackPane();
        uploadArea.setMinHeight(100);
        uploadArea.setStyle(
            "-fx-background-color: " + surfaceSoft() + ";" +
            "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.25) + ";" +
            "-fx-border-width: 2px;" +
            "-fx-border-style: dashed;" +
            "-fx-background-radius: 16px;" +
            "-fx-border-radius: 16px;" +
            "-fx-cursor: hand;"
        );
        
        VBox uploadContent = new VBox(8);
        uploadContent.setAlignment(Pos.CENTER);
        Text uploadIcon = text("📁", 32, false, tm.getAccentHex());
        Text uploadText = text("Upload Event Banner", 12, false, textMuted());
        Text uploadSubtext = text("PNG, JPG up to 10MB", 10, false, "rgba(255,255,255,0.4)");
        
        uploadContent.getChildren().addAll(uploadIcon, uploadText, uploadSubtext);
        uploadArea.getChildren().add(uploadContent);
        
        // File chooser on click
        uploadArea.setOnMouseClicked(e -> {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            chooser.setTitle("Select Event Banner Image");
            chooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"),
                new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*")
            );
            
            javafx.stage.Window window = root.getScene() != null ? root.getScene().getWindow() : null;
            if (window != null) {
                File selectedFile = chooser.showOpenDialog(window);
                if (selectedFile != null) {
                    // Update UI to show selected file
                    uploadContent.getChildren().clear();
                    Text checkIcon = text("✓", 28, false, "#4caf50");
                    Text fileName = text(selectedFile.getName(), 12, true, tm.getTextColor());
                    fileName.wrappingWidthProperty().bind(uploadArea.widthProperty().subtract(20));
                    Text fileSize = text(String.format("%.2f KB", selectedFile.length() / 1024.0), 10, false, textMuted());
                    uploadContent.getChildren().addAll(checkIcon, fileName, fileSize);
                    
                    // Store file reference
                    selectedImageFile = selectedFile;
                    
                    // Update border color to indicate success
                    uploadArea.setStyle(
                        "-fx-background-color: " + surfaceSoft() + ";" +
                        "-fx-border-color: #4caf50;" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-style: solid;" +
                        "-fx-background-radius: 16px;" +
                        "-fx-border-radius: 16px;" +
                        "-fx-cursor: hand;"
                    );
                }
            }
        });
        
        row.getChildren().addAll(label, uploadArea);
        return row;
    }

    private String surfaceStrong() {
        return tm.isDarkMode()
            ? "linear-gradient(from 0% 0% to 100% 100%, #020202 0%, #070707 58%, #0b0b0b 100%)"
            : "linear-gradient(from 0% 0% to 100% 100%, #ffffff 0%, #f8fafc 100%)";
    }

    private String surfaceCard() {
        return tm.isDarkMode()
            ? "linear-gradient(from 0% 0% to 100% 100%, rgba(10,10,10,0.93) 0%, rgba(14,14,14,0.93) 62%, " + tm.toRgba(tm.getAccentHex(), 0.10) + " 100%)"
            : "linear-gradient(from 0% 0% to 100% 100%, rgba(255,255,255,0.98) 0%, rgba(248,250,252,0.96) 100%)";
    }

    private String surfaceSoft() {
        return tm.isDarkMode() ? "rgba(255,255,255,0.08)" : "rgba(15,23,42,0.06)";
    }

    private String borderSoft() {
        return tm.isDarkMode() ? tm.toRgba(tm.getAccentHex(), 0.34) : "rgba(15,23,42,0.16)";
    }

    private String textSoft() {
        return tm.isDarkMode() ? "rgba(255,255,255,0.92)" : "rgba(15,23,42,0.90)";
    }

    private String textMuted() {
        return tm.isDarkMode() ? "rgba(255,255,255,0.78)" : "rgba(30,41,59,0.82)";
    }

    private HBox twoColRow(VBox left, VBox right) {
        HBox row = new HBox(10, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setMaxWidth(Double.MAX_VALUE);
        right.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private HBox twoColRow(Node left, Node right) {
        HBox row = new HBox(10, left, right);
        HBox.setHgrow((Region) left, Priority.ALWAYS);
        HBox.setHgrow((Region) right, Priority.ALWAYS);
        if (left instanceof Region) ((Region) left).setMaxWidth(Double.MAX_VALUE);
        if (right instanceof Region) ((Region) right).setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private StackPane sectionShell(double radius, Insets padding, String bg, String border) {
        StackPane pane = new StackPane();
        pane.prefWidthProperty().bind(Bindings.min(root.widthProperty().multiply(0.95), 1800));
        pane.setMinWidth(0);
        pane.setPadding(padding);
        pane.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-background-radius: " + radius + "px;" +
            "-fx-border-color: " + border + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: " + radius + "px;"
        );
        return pane;
    }

    private Text text(String value, int size, boolean bold, String color) {
        Text t = new Text(value);
        t.setFont(Font.font(
            bold ? MainApplication.getInstance().getBoldFontFamily() : MainApplication.getInstance().getLightFontFamily(),
            bold ? FontWeight.BOLD : FontWeight.NORMAL,
            size
        ));
        t.setFill(Color.web(color));
        return t;
    }

    private void addHoverLift(VBox card) {
        card.setOnMouseEntered(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(220), card);
            tt.setToY(-8);
            tt.play();
            ScaleTransition st = new ScaleTransition(Duration.millis(220), card);
            st.setToX(1.01);
            st.setToY(1.01);
            st.play();
        });
        card.setOnMouseExited(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(220), card);
            tt.setToY(0);
            tt.play();
            ScaleTransition st = new ScaleTransition(Duration.millis(220), card);
            st.setToX(1);
            st.setToY(1);
            st.play();
        });
    }

    @Override
    public Pane getRoot() {
        return root;
    }

    @Override
    public void cleanup() {}
}

