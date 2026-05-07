package com.syndicati.views.frontend.profile;

import com.syndicati.controllers.evenement.EvenementController;
import com.syndicati.controllers.evenement.ParticipationController;
import com.syndicati.models.evenement.Evenement;
import com.syndicati.models.evenement.Participation;
import com.syndicati.models.user.User;
import com.syndicati.utils.session.SessionManager;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enhanced Events section with status filters, detail modal, smooth animations.
 * Matches Horizon profile 1:1 with interactive filters and detailed event views.
 */
public class ProfileEventsSectionEnhanced {
    
    private final VBox root = new VBox(16);
    private final EvenementController eventController;
    private final ParticipationController participationController;
    private final StackPane switcherContainer = new StackPane();
    
    // Switcher faces
    private final VBox faceListView = new VBox(16);
    private final VBox faceDetailsView = new VBox(16);
    
    // State
    private String currentStatusFilter = "all";
    private String currentOwnerFilter = "my";
    private List<Evenement> allEvents = new ArrayList<>();
    private List<Participation> allParticipations = new ArrayList<>();
    private List<EventFeedEntry> filteredEntries = new ArrayList<>();
    private Evenement selectedEvent = null;
    private Participation selectedParticipation = null;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 3;
    
    // Content containers
    private final VBox eventList = new VBox(10);
    private final HBox paginationContainer = new HBox(8);

    public ProfileEventsSectionEnhanced() {
        this.eventController = new EvenementController();
        this.participationController = new ParticipationController();

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (isAdminUser(currentUser)) {
            currentOwnerFilter = "all";
        }

        buildLayout();
    }

    private void buildLayout() {
        root.setPadding(new Insets(16, 0, 0, 0));
        
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 1; -fx-background-radius: 20; -fx-border-radius: 20;");
        card.setPrefHeight(600);
        
        // Switcher container
        switcherContainer.setPrefHeight(500);
        VBox.setVgrow(switcherContainer, Priority.ALWAYS);
        
        buildListView();
        buildDetailsView();
        
        // Add details view first (back), list view on top (front)
        switcherContainer.getChildren().addAll(faceDetailsView, faceListView);
        faceDetailsView.setVisible(false);
        faceDetailsView.setManaged(false);
        
        card.getChildren().add(switcherContainer);
        root.getChildren().add(card);
        
        loadEvents();
    }

    private void buildListView() {
        faceListView.setStyle("-fx-background-color: transparent;");
        faceListView.setPadding(new Insets(20));
        
        // Header with title and admin filter
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("📅 Your Events");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);
        
        HBox adminFilter = createAdminFilter();
        HBox.setHgrow(title, Priority.ALWAYS);
        header.getChildren().addAll(title, adminFilter);
        
        faceListView.getChildren().add(header);
        
        // Status filters
        HBox statusFilters = createStatusFilters();
        faceListView.getChildren().add(statusFilters);
        
        // Event list with scroll
        ScrollPane listScroll = new ScrollPane();
        listScroll.setStyle("-fx-control-inner-background: transparent; -fx-padding: 0;");
        listScroll.setFitToWidth(true);
        listScroll.setPrefHeight(400);
        listScroll.setContent(eventList);
        VBox.setVgrow(listScroll, Priority.ALWAYS);
        faceListView.getChildren().add(listScroll);
        
        // Pagination
        paginationContainer.setAlignment(Pos.CENTER);
        paginationContainer.setPadding(new Insets(12, 0, 0, 0));
        faceListView.getChildren().add(paginationContainer);
    }

    private void buildDetailsView() {
        faceDetailsView.setStyle("-fx-background-color: rgba(20, 20, 25, 0.98);");
        faceDetailsView.setPadding(new Insets(20));
        
        // Back button and title
        HBox backHeader = new HBox(12);
        backHeader.setAlignment(Pos.CENTER_LEFT);
        
        Button backBtn = new Button("←");
        backBtn.setStyle("-fx-padding: 8; -fx-font-size: 14; -fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-background-radius: 50%; -fx-border-radius: 50%;");
        backBtn.setOnAction(e -> showListView());
        
        Label detailTitle = new Label("Event Details");
        detailTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        detailTitle.setTextFill(Color.WHITE);
        
        backHeader.getChildren().addAll(backBtn, detailTitle);
        faceDetailsView.getChildren().add(backHeader);
        
        // Detail content
        ScrollPane detailScroll = new ScrollPane();
        detailScroll.setStyle("-fx-control-inner-background: transparent; -fx-padding: 0;");
        detailScroll.setFitToWidth(true);
        
        VBox detailContent = new VBox(12);
        detailContent.setPadding(new Insets(16, 0, 0, 0));
        Label placeholder = new Label("Select an event to view details");
        placeholder.setTextFill(Color.color(1, 1, 1, 0.5));
        detailContent.getChildren().add(placeholder);
        
        detailScroll.setContent(detailContent);
        VBox.setVgrow(detailScroll, Priority.ALWAYS);
        faceDetailsView.getChildren().add(detailScroll);
    }

    private HBox createAdminFilter() {
        HBox filter = new HBox(8);
        filter.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 1; -fx-padding: 4; -fx-background-radius: 999; -fx-border-radius: 999;");
        
        User currentUser = SessionManager.getInstance().getCurrentUser();
        boolean isAdmin = currentUser != null && (currentUser.getRoleUser().equals("ADMIN") || currentUser.getRoleUser().equals("OWNER"));
        
        if (isAdmin) {
            Button myOwnBtn = createFilterButton("My Own", "my".equals(currentOwnerFilter));
            Button everyoneBtn = createFilterButton("Everyone", "all".equals(currentOwnerFilter));
            myOwnBtn.setOnAction(e -> switchAdminFilter("my", myOwnBtn, everyoneBtn));
            everyoneBtn.setOnAction(e -> switchAdminFilter("all", myOwnBtn, everyoneBtn));
            filter.getChildren().addAll(myOwnBtn, everyoneBtn);
        }
        
        return filter;
    }

    private Button createFilterButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-padding: 6 12 6 12; " +
            "-fx-background-color: " + (active ? "rgba(99, 102, 241, 0.8);" : "transparent;") +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 999; " +
            "-fx-font-size: 11;"
        );
        return btn;
    }

    private void switchAdminFilter(String filterType, Button myOwnBtn, Button everyoneBtn) {
        currentOwnerFilter = filterType;
        boolean isMyOwn = filterType.equals("my");
        myOwnBtn.setStyle(
            "-fx-padding: 6 12 6 12; " +
            "-fx-background-color: " + (isMyOwn ? "rgba(99, 102, 241, 0.8);" : "transparent;") +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 999; " +
            "-fx-font-size: 11;"
        );
        everyoneBtn.setStyle(
            "-fx-padding: 6 12 6 12; " +
            "-fx-background-color: " + (!isMyOwn ? "rgba(99, 102, 241, 0.8);" : "transparent;") +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 999; " +
            "-fx-font-size: 11;"
        );
        currentPage = 0;
        loadEvents();
    }

    private HBox createStatusFilters() {
        HBox filters = new HBox(8);
        filters.setAlignment(Pos.CENTER_LEFT);
        
        Button allBtn = createStatusButton("All", true);
        Button plannedBtn = createStatusButton("📋 Planned", false);
        Button inProgressBtn = createStatusButton("🔄 In Progress", false);
        Button completedBtn = createStatusButton("✅ Completed", false);
        Button cancelledBtn = createStatusButton("❌ Cancelled", false);
        
        allBtn.setOnAction(e -> switchStatus("all", allBtn, plannedBtn, inProgressBtn, completedBtn, cancelledBtn));
        plannedBtn.setOnAction(e -> switchStatus("planifié", allBtn, plannedBtn, inProgressBtn, completedBtn, cancelledBtn));
        inProgressBtn.setOnAction(e -> switchStatus("en cours", allBtn, plannedBtn, inProgressBtn, completedBtn, cancelledBtn));
        completedBtn.setOnAction(e -> switchStatus("terminé", allBtn, plannedBtn, inProgressBtn, completedBtn, cancelledBtn));
        cancelledBtn.setOnAction(e -> switchStatus("annulé", allBtn, plannedBtn, inProgressBtn, completedBtn, cancelledBtn));
        
        filters.getChildren().addAll(allBtn, plannedBtn, inProgressBtn, completedBtn, cancelledBtn);
        return filters;
    }

    private Button createStatusButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-padding: 6 12 6 12; " +
            "-fx-background-color: " + (active ? "rgba(99, 102, 241, 0.2);" : "rgba(255,255,255,0.05);") +
            "-fx-text-fill: " + (active ? "white;" : "rgba(255,255,255,0.7);") +
            "-fx-border-color: " + (active ? "rgba(99, 102, 241, 0.3);" : "rgba(255,255,255,0.1);") +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 999; " +
            "-fx-border-radius: 999; " +
            "-fx-font-size: 11; " +
            "-fx-font-weight: " + (active ? "bold;" : "normal;")
        );
        return btn;
    }

    private void switchStatus(String status, Button... buttons) {
        currentStatusFilter = status;
        currentPage = 0;
        
        // Update button styles
        updateStatusButtons(status, buttons);
        applyFilters();
        renderEventList();
    }

    private void updateStatusButtons(String activeStatus, Button[] buttons) {
        String[] statuses = {"all", "planifié", "en cours", "terminé", "annulé"};
        for (int i = 0; i < buttons.length; i++) {
            boolean isActive = activeStatus.equals(statuses[i]);
            buttons[i].setStyle(
                "-fx-padding: 6 12 6 12; " +
                "-fx-background-color: " + (isActive ? "rgba(99, 102, 241, 0.2);" : "rgba(255,255,255,0.05);") +
                "-fx-text-fill: " + (isActive ? "white;" : "rgba(255,255,255,0.7);") +
                "-fx-border-color: " + (isActive ? "rgba(99, 102, 241, 0.3);" : "rgba(255,255,255,0.1);") +
                "-fx-border-width: 1; " +
                "-fx-background-radius: 999; " +
                "-fx-border-radius: 999; " +
                "-fx-font-size: 11; " +
                "-fx-font-weight: " + (isActive ? "bold;" : "normal;")
            );
        }
    }

    private void loadEvents() {
        allEvents = new ArrayList<>(eventController.evenements());
        allParticipations = new ArrayList<>(participationController.participations());
        applyFilters();
        renderEventList();
    }

    private void applyFilters() {
        filteredEntries = new ArrayList<>();

        for (Evenement event : allEvents) {
            if (event == null) {
                continue;
            }
            if (!matchesOwnerFilter(event)) {
                continue;
            }
            if (!matchesStatusFilter(event)) {
                continue;
            }
            filteredEntries.add(EventFeedEntry.fromEvent(event));
        }

        for (Participation participation : allParticipations) {
            if (participation == null || participation.getEvenement() == null) {
                continue;
            }
            if (!matchesOwnerFilter(participation)) {
                continue;
            }
            if (!matchesStatusFilter(participation)) {
                continue;
            }
            filteredEntries.add(EventFeedEntry.fromParticipation(participation));
        }

        filteredEntries = filteredEntries.stream()
            .sorted((left, right) -> right.sortDate().compareTo(left.sortDate()))
            .collect(Collectors.toList());
    }

    private void renderEventList() {
        eventList.getChildren().clear();
        paginationContainer.getChildren().clear();
        
        int maxPage = (int) Math.ceil((double) filteredEntries.size() / ITEMS_PER_PAGE);
        if (maxPage == 0) maxPage = 1;
        
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, filteredEntries.size());
        
        if (start >= filteredEntries.size()) {
            Label noData = new Label("No events or participations found");
            noData.setTextFill(Color.color(1, 1, 1, 0.5));
            eventList.getChildren().add(noData);
            return;
        }
        
        // Add items with animation
        for (int i = start; i < end; i++) {
            VBox item = createEventItem(filteredEntries.get(i));
            eventList.getChildren().add(item);
            animateItemEntry(item);
        }
        
        // Add pagination buttons
        for (int page = 0; page < maxPage; page++) {
            Button pageBtn = new Button(String.valueOf(page + 1));
            final int pageNum = page;
            boolean isActive = page == currentPage;
            pageBtn.setStyle(
                "-fx-padding: 4 10 4 10; " +
                "-fx-background-color: " + (isActive ? "rgba(99, 102, 241, 0.8);" : "rgba(255,255,255,0.05);") +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 6; " +
                "-fx-font-size: 10;"
            );
            pageBtn.setOnAction(e -> {
                currentPage = pageNum;
                renderEventList();
            });
            paginationContainer.getChildren().add(pageBtn);
        }
    }

    private VBox createEventItem(EventFeedEntry entry) {
        Evenement event = entry.event();
        VBox item = new VBox(8);
        item.setStyle(
            "-fx-background-color: rgba(99, 102, 241, 0.05); " +
            "-fx-border-color: rgba(99, 102, 241, 0.1); " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 12; " +
            "-fx-border-radius: 12; " +
            "-fx-padding: 12; " +
            "-fx-cursor: hand;"
        );
        item.setOnMouseClicked(e -> showDetailsView(entry));
        
        // Hover effect
        item.setOnMouseEntered(e -> item.setStyle(
            "-fx-background-color: rgba(99, 102, 241, 0.1); " +
            "-fx-border-color: rgba(99, 102, 241, 0.2); " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 12; " +
            "-fx-border-radius: 12; " +
            "-fx-padding: 12; " +
            "-fx-cursor: hand;"
        ));
        item.setOnMouseExited(e -> item.setStyle(
            "-fx-background-color: rgba(99, 102, 241, 0.05); " +
            "-fx-border-color: rgba(99, 102, 241, 0.1); " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 12; " +
            "-fx-border-radius: 12; " +
            "-fx-padding: 12; " +
            "-fx-cursor: hand;"
        ));
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label icon = new Label("📅");
        icon.setFont(Font.font(16));
        
        VBox titleSection = new VBox(4);
        Label title = new Label(event.getTitreEvent());
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        title.setTextFill(Color.WHITE);
        
        String dateStr = event.getDateEvent() != null 
            ? event.getDateEvent().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
            : "—";
        String locationStr = event.getLieuEvent() != null ? event.getLieuEvent() : "—";
        
        Label dateLocation = new Label("📍 " + locationStr + " • " + dateStr);
        dateLocation.setFont(Font.font("Segoe UI", 10));
        dateLocation.setTextFill(Color.color(1, 1, 1, 0.5));
        
        titleSection.getChildren().addAll(title, dateLocation);
        HBox.setHgrow(titleSection, Priority.ALWAYS);
        
        String statusText = entry.isParticipation()
            ? getParticipationStatusDisplay(entry.participation().getStatutParticipation())
            : getStatusDisplay(event.getStatutEvent());
        Label status = new Label(statusText);
        status.setFont(Font.font("Segoe UI", 9));
        status.setStyle("-fx-text-fill: white; -fx-background-color: " + (entry.isParticipation()
            ? getParticipationStatusColor(entry.participation().getStatutParticipation())
            : getStatusColor(event.getStatutEvent())) + "; -fx-padding: 4 8 4 8; -fx-background-radius: 6;");

        Label chevron = new Label("→");
        chevron.setFont(Font.font(14));
        chevron.setTextFill(Color.color(1, 1, 1, 0.5));

        if (entry.isParticipation()) {
            Label kind = new Label("👤 Participation");
            kind.setFont(Font.font("Segoe UI", 9));
            kind.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-background-color: rgba(255,255,255,0.12); -fx-padding: 4 8 4 8; -fx-background-radius: 6;");
            header.getChildren().addAll(icon, titleSection, kind, status, chevron);
        } else {
            header.getChildren().addAll(icon, titleSection, status, chevron);
        }

        item.getChildren().add(header);
        
        return item;
    }

    private String getStatusDisplay(String status) {
        return switch (status) {
            case "planifié" -> "📋 Planned";
            case "en cours" -> "🔄 In Progress";
            case "terminé" -> "✅ Completed";
            case "annulé" -> "❌ Cancelled";
            default -> status;
        };
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "planifié" -> "rgba(100, 200, 255, 0.2)";
            case "en cours" -> "rgba(255, 180, 100, 0.2)";
            case "terminé" -> "rgba(100, 255, 150, 0.2)";
            case "annulé" -> "rgba(255, 100, 100, 0.2)";
            default -> "rgba(150, 150, 150, 0.2)";
        };
    }

    private String getParticipationStatusDisplay(String status) {
        if (status == null) {
            return "👤 Participation";
        }
        return switch (status) {
            case "confirme" -> "✅ Confirmed";
            case "en_attente" -> "⏳ Pending";
            case "refuse" -> "🚫 Refused";
            case "annule" -> "❌ Cancelled";
            default -> "👤 " + status;
        };
    }

    private String getParticipationStatusColor(String status) {
        if (status == null) {
            return "rgba(120, 120, 120, 0.2)";
        }
        return switch (status) {
            case "confirme" -> "rgba(100, 255, 150, 0.2)";
            case "en_attente" -> "rgba(255, 190, 100, 0.2)";
            case "refuse", "annule" -> "rgba(255, 100, 100, 0.2)";
            default -> "rgba(120, 120, 120, 0.2)";
        };
    }

    private void animateItemEntry(VBox item) {
        item.setOpacity(0);
        item.setScaleY(0.9);
        
        FadeTransition fade = new FadeTransition(Duration.millis(300), item);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        ScaleTransition scale = new ScaleTransition(Duration.millis(300), item);
        scale.setFromY(0.9);
        scale.setToY(1);
        
        fade.play();
        scale.play();
    }

    private void showDetailsView(EventFeedEntry entry) {
        selectedEvent = entry.event();
        selectedParticipation = entry.participation();
        Evenement event = entry.event();

        // Populate detail content
        ScrollPane detailScroll = (ScrollPane) faceDetailsView.getChildren().get(1);
        VBox detailContent = (VBox) detailScroll.getContent();
        detailContent.getChildren().clear();
        
        VBox detailCard = new VBox(12);
        detailCard.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-padding: 16;");
        
        Label eventTitle = new Label(event.getTitreEvent() != null ? event.getTitreEvent() : "—");
        eventTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        eventTitle.setTextFill(Color.WHITE);
        
        Label location = new Label("📍 " + (event.getLieuEvent() != null ? event.getLieuEvent() : "TBA"));
        location.setFont(Font.font("Segoe UI", 12));
        location.setTextFill(Color.color(1, 1, 1, 0.8));
        
        Label dateTime = new Label("📅 " + (event.getDateEvent() != null 
            ? event.getDateEvent().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
            : "—"));
        dateTime.setFont(Font.font("Segoe UI", 11));
        dateTime.setTextFill(Color.color(1, 1, 1, 0.6));
        
        Label description = new Label(event.getDescriptionEvent() != null ? event.getDescriptionEvent() : "No description");
        description.setFont(Font.font("Segoe UI", 11));
        description.setTextFill(Color.color(1, 1, 1, 0.8));
        description.setWrapText(true);
        
        Label capacity = new Label("🙋 " + event.getNbPlaces() + " seats (✅ " + event.getNbRestants() + " available)");
        capacity.setFont(Font.font("Segoe UI", 11));
        capacity.setTextFill(Color.color(1, 1, 1, 0.7));
        
        Label status = new Label(entry.isParticipation()
            ? getParticipationStatusDisplay(entry.participation().getStatutParticipation())
            : getStatusDisplay(event.getStatutEvent()));
        status.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        status.setStyle("-fx-text-fill: white; -fx-background-color: " + (entry.isParticipation()
            ? getParticipationStatusColor(entry.participation().getStatutParticipation())
            : getStatusColor(event.getStatutEvent())) + "; -fx-padding: 6 12 6 12; -fx-background-radius: 6;");
        
        detailCard.getChildren().addAll(eventTitle, location, dateTime, description, capacity, status);

        if (entry.isParticipation()) {
            Participation participation = entry.participation();
            Label partDate = new Label("🧾 Participation date: " + (participation.getDateParticipation() != null
                ? participation.getDateParticipation().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
                : "—"));
            partDate.setFont(Font.font("Segoe UI", 11));
            partDate.setTextFill(Color.color(1, 1, 1, 0.7));

            Label companions = new Label("👥 Accompanying persons: " + (participation.getNbAccompagnants() == null ? 0 : participation.getNbAccompagnants()));
            companions.setFont(Font.font("Segoe UI", 11));
            companions.setTextFill(Color.color(1, 1, 1, 0.7));

            Label note = new Label("💬 Note: " + (participation.getCommentaireParticipation() == null || participation.getCommentaireParticipation().isBlank()
                ? "—"
                : participation.getCommentaireParticipation()));
            note.setWrapText(true);
            note.setFont(Font.font("Segoe UI", 11));
            note.setTextFill(Color.color(1, 1, 1, 0.8));

            detailCard.getChildren().addAll(partDate, companions, note);
        }

        detailContent.getChildren().add(detailCard);
        
        animateSwitcherFace(false);
    }

    private boolean matchesOwnerFilter(Evenement event) {
        if ("all".equals(currentOwnerFilter)) {
            return true;
        }

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getIdUser() == null || event.getUser() == null || event.getUser().getIdUser() == null) {
            return false;
        }
        return currentUser.getIdUser().equals(event.getUser().getIdUser());
    }

    private boolean matchesOwnerFilter(Participation participation) {
        if ("all".equals(currentOwnerFilter)) {
            return true;
        }

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getIdUser() == null || participation.getUser() == null || participation.getUser().getIdUser() == null) {
            return false;
        }
        return currentUser.getIdUser().equals(participation.getUser().getIdUser());
    }

    private boolean matchesStatusFilter(Evenement event) {
        if ("all".equals(currentStatusFilter)) {
            return true;
        }

        String normalized = normalizeStatus(event.getStatutEvent());
        return normalizeStatus(currentStatusFilter).equals(normalized);
    }

    private boolean matchesStatusFilter(Participation participation) {
        if ("all".equals(currentStatusFilter)) {
            return true;
        }

        String mapped = mapParticipationToEventStatus(participation.getStatutParticipation());
        return normalizeStatus(currentStatusFilter).equals(normalizeStatus(mapped));
    }

    private String mapParticipationToEventStatus(String participationStatus) {
        if (participationStatus == null) {
            return "all";
        }
        return switch (participationStatus) {
            case "en_attente" -> "planifié";
            case "confirme" -> "en cours";
            case "refuse", "annule" -> "annulé";
            default -> "all";
        };
    }

    private String normalizeStatus(String value) {
        if (value == null) {
            return "";
        }
        return value
            .toLowerCase()
            .replace("_", " ")
            .replace("é", "e")
            .replace("è", "e")
            .trim();
    }

    private boolean isAdminUser(User user) {
        if (user == null || user.getRoleUser() == null) {
            return false;
        }
        String role = user.getRoleUser();
        return "ADMIN".equalsIgnoreCase(role) || "OWNER".equalsIgnoreCase(role);
    }

    private record EventFeedEntry(Evenement event, Participation participation, boolean isParticipation) {
        static EventFeedEntry fromEvent(Evenement event) {
            return new EventFeedEntry(event, null, false);
        }

        static EventFeedEntry fromParticipation(Participation participation) {
            return new EventFeedEntry(participation.getEvenement(), participation, true);
        }

        java.time.LocalDateTime sortDate() {
            if (isParticipation && participation != null && participation.getDateParticipation() != null) {
                return participation.getDateParticipation();
            }
            if (event != null && event.getDateEvent() != null) {
                return event.getDateEvent();
            }
            return java.time.LocalDateTime.MIN;
        }
    }

    private void showListView() {
        animateSwitcherFace(true);
    }

    private void animateSwitcherFace(boolean showList) {
        VBox toShow = showList ? faceListView : faceDetailsView;
        VBox toHide = showList ? faceDetailsView : faceListView;
        
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toHide);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            toHide.setVisible(false);
            toHide.setManaged(false);
        });
        
        toShow.setVisible(true);
        toShow.setManaged(true);
        toShow.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toShow);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        
        fadeOut.play();
        fadeIn.play();
    }

    public VBox getRoot() {
        return root;
    }
}
