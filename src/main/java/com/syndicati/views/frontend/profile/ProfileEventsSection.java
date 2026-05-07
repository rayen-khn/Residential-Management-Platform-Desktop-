package com.syndicati.views.frontend.profile;

import com.syndicati.controllers.evenement.EvenementController;
import com.syndicati.models.evenement.Evenement;
import com.syndicati.utils.session.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Events section showing user's events with pagination and filtering.
 */
public class ProfileEventsSection {
    
    private final VBox root = new VBox(16);
    private final EvenementController eventController;
    private final VBox eventListView = new VBox(10);
    
    // Pagination
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 3;
    private List<Evenement> currentUserEvents;

    public ProfileEventsSection() {
        this.eventController = new EvenementController();
        buildLayout();
    }

    private void buildLayout() {
        root.setPadding(new Insets(16, 0, 0, 0));
        
        VBox card = new VBox(16);
        card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 1; -fx-background-radius: 20; -fx-border-radius: 20;");
        card.setPadding(new Insets(22));
        
        // Title
        Label title = new Label("Your Events");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);
        card.getChildren().add(title);
        
        // Events List
        VBox.setVgrow(eventListView, Priority.ALWAYS);
        card.getChildren().add(eventListView);
        
        // Pagination
        HBox pagination = createPagination();
        card.getChildren().add(pagination);
        
        // Load real data
        loadEvents();
        
        root.getChildren().add(card);
    }

    private void loadEvents() {
        eventListView.getChildren().clear();
        
        // Get all events and filter for current user
        List<Evenement> allEvents = eventController.evenements();
        
        if (allEvents == null || allEvents.isEmpty()) {
            Label noData = new Label("No events found");
            noData.setTextFill(Color.color(1, 1, 1, 0.5));
            eventListView.getChildren().add(noData);
        } else {
            // Filter for current user's events (optional - show all if needed)
            currentUserEvents = allEvents.stream()
                .filter(e -> e.getUser() != null && e.getUser().equals(SessionManager.getInstance().getCurrentUser()))
                .collect(Collectors.toList());
            
            if (currentUserEvents.isEmpty()) {
                // If no user-specific events, show all
                currentUserEvents = allEvents;
            }
            
            // Show paginated events (3 per page)
            int start = currentPage * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, currentUserEvents.size());
            
            for (int i = start; i < end; i++) {
                Evenement event = currentUserEvents.get(i);
                eventListView.getChildren().add(createEventItem(event));
            }
        }
    }

    private VBox createEventItem(Evenement event) {
        VBox item = new VBox(8);
        item.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-border-color: rgba(255, 255, 255, 0.05); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-padding: 12;");
        
        // Header: Title, Status, Type
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label icon = new Label("📅");
        icon.setFont(Font.font(16));
        
        Label title = new Label(event.getTitreEvent());
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        title.setTextFill(Color.WHITE);
        
        Label status = new Label(formatStatus(event.getStatutEvent()));
        status.setFont(Font.font("Segoe UI", 10));
        status.setStyle("-fx-text-fill: rgba(99, 102, 241, 0.9); -fx-padding: 4 8 4 8; -fx-background-color: rgba(99, 102, 241, 0.1); -fx-background-radius: 6;");
        
        HBox.setHgrow(title, Priority.ALWAYS);
        header.getChildren().addAll(icon, title, status);
        
        // Date, Location, Description
        String dateStr = event.getDateEvent() != null 
            ? event.getDateEvent().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
            : "—";
        
        Label dateLocation = new Label("📍 " + dateStr + " • " + (event.getLieuEvent() != null ? event.getLieuEvent() : "Location TBD"));
        dateLocation.setFont(Font.font("Segoe UI", 11));
        dateLocation.setTextFill(Color.color(1, 1, 1, 0.6));
        
        Label description = new Label(event.getDescriptionEvent());
        description.setFont(Font.font("Segoe UI", 11));
        description.setTextFill(Color.color(1, 1, 1, 0.65));
        description.setWrapText(true);
        
        // Capacity info
        String capacityStr = event.getNbRestants() != null && event.getNbPlaces() != null
            ? (event.getNbPlaces() - event.getNbRestants()) + "/" + event.getNbPlaces() + " enrolled"
            : "—";
        
        Label capacity = new Label("👥 " + capacityStr);
        capacity.setFont(Font.font("Segoe UI", 10));
        capacity.setTextFill(Color.color(1, 1, 1, 0.5));
        
        item.getChildren().addAll(header, dateLocation, description, capacity);
        return item;
    }

    private String formatStatus(String status) {
        if (status == null) return "Planned";
        return switch (status.toLowerCase()) {
            case "planifié" -> "Planned";
            case "en cours" -> "In progress";
            case "terminé" -> "Completed";
            case "annulé" -> "Cancelled";
            default -> status;
        };
    }

    private HBox createPagination() {
        HBox pagination = new HBox(8);
        pagination.setAlignment(Pos.CENTER);
        pagination.setPadding(new Insets(8));
        pagination.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 1; -fx-background-radius: 999; -fx-border-radius: 999;");
        
        Button page1 = createPageButton("1", 0);
        Button page2 = createPageButton("2", 1);
        
        pagination.getChildren().addAll(page1, page2);
        return pagination;
    }

    private Button createPageButton(String label, int pageNum) {
        Button btn = new Button(label);
        boolean active = pageNum == currentPage;
        btn.setStyle(
            "-fx-min-width: 40; -fx-min-height: 40; -fx-padding: 0;" +
            "-fx-background-color: " + (active ? "linear-gradient(to right, #6366f1, #8b5cf6);" : "rgba(255, 255, 255, 0.05);") +
            "-fx-text-fill: " + (active ? "white;" : "rgba(255, 255, 255, 0.5);") +
            "-fx-border-radius: 999; -fx-background-radius: 999;" +
            "-fx-font-weight: bold;"
        );
        btn.setOnAction(e -> {
            currentPage = pageNum;
            loadEvents();
        });
        return btn;
    }

    public VBox getRoot() {
        return root;
    }

    public void cleanup() {
        // Cleanup resources if needed
    }
}
