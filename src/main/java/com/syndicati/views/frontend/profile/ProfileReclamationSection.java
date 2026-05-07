package com.syndicati.views.frontend.profile;

import com.syndicati.controllers.syndicat.ReclamationController;
import com.syndicati.models.syndicat.Reclamation;
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
 * Reclamation section showing user's complaints with pagination and status filtering.
 */
public class ProfileReclamationSection {
    
    private final VBox root = new VBox(16);
    private final ReclamationController reclamationController;
    private final VBox reclamationListView = new VBox(10);
    
    // Pagination
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 3;
    private List<Reclamation> currentUserReclamations;

    public ProfileReclamationSection() {
        this.reclamationController = new ReclamationController();
        buildLayout();
    }

    private void buildLayout() {
        root.setPadding(new Insets(16, 0, 0, 0));
        
        VBox card = new VBox(16);
        card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 1; -fx-background-radius: 20; -fx-border-radius: 20;");
        card.setPadding(new Insets(22));
        
        // Title
        Label title = new Label("Your Reclamations");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);
        card.getChildren().add(title);
        
        // Reclamations List
        VBox.setVgrow(reclamationListView, Priority.ALWAYS);
        card.getChildren().add(reclamationListView);
        
        // Pagination
        HBox pagination = createPagination();
        card.getChildren().add(pagination);
        
        // Load real data
        loadReclamations();
        
        root.getChildren().add(card);
    }

    private void loadReclamations() {
        reclamationListView.getChildren().clear();
        
        // Get all reclamations and filter for current user
        List<Reclamation> allReclamations = reclamationController.reclamations();
        
        if (allReclamations == null || allReclamations.isEmpty()) {
            Label noData = new Label("No reclamations found");
            noData.setTextFill(Color.color(1, 1, 1, 0.5));
            reclamationListView.getChildren().add(noData);
        } else {
            // Filter for current user's reclamations
            currentUserReclamations = allReclamations.stream()
                .filter(r -> r.getUser() != null && r.getUser().equals(SessionManager.getInstance().getCurrentUser()))
                .collect(Collectors.toList());
            
            if (currentUserReclamations.isEmpty()) {
                // If no user-specific reclamations, show all
                currentUserReclamations = allReclamations;
            }
            
            // Show paginated reclamations (3 per page)
            int start = currentPage * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, currentUserReclamations.size());
            
            for (int i = start; i < end; i++) {
                Reclamation reclamation = currentUserReclamations.get(i);
                reclamationListView.getChildren().add(createReclamationItem(reclamation));
            }
        }
    }

    private VBox createReclamationItem(Reclamation reclamation) {
        VBox item = new VBox(8);
        item.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-border-color: rgba(255, 255, 255, 0.05); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-padding: 12;");
        
        // Header: Title, Status
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label icon = new Label("⚠️");
        icon.setFont(Font.font(16));
        
        Label title = new Label(reclamation.getTitreReclamations());
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        title.setTextFill(Color.WHITE);
        
        String statusColor = getStatusColor(reclamation.getStatutReclamation());
        Label status = new Label(formatStatus(reclamation.getStatutReclamation()));
        status.setFont(Font.font("Segoe UI", 10));
        status.setStyle("-fx-text-fill: white; -fx-padding: 4 8 4 8; -fx-background-color: " + statusColor + "; -fx-background-radius: 6;");
        
        HBox.setHgrow(title, Priority.ALWAYS);
        header.getChildren().addAll(icon, title, status);
        
        // Date submitted
        String dateStr = reclamation.getCreatedAt() != null 
            ? reclamation.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
            : "—";
        
        Label dateSubmitted = new Label("📅 Submitted on " + dateStr);
        dateSubmitted.setFont(Font.font("Segoe UI", 11));
        dateSubmitted.setTextFill(Color.color(1, 1, 1, 0.6));
        
        // Description
        Label description = new Label(reclamation.getDescReclamation());
        description.setFont(Font.font("Segoe UI", 11));
        description.setTextFill(Color.color(1, 1, 1, 0.65));
        description.setWrapText(true);
        
        item.getChildren().addAll(header, dateSubmitted, description);
        return item;
    }

    private String formatStatus(String status) {
        if (status == null) return "Pending";
        return switch (status.toLowerCase()) {
            case "confirme" -> "Confirmed";
            case "active" -> "Active";
            case "en_attente" -> "Pending";
            case "refuse" -> "Refused";
            case "annule" -> "Cancelled";
            case "termine" -> "Completed";
            default -> status;
        };
    }

    private String getStatusColor(String status) {
        if (status == null) return "rgba(250, 166, 26, 0.2)"; // Warning orange
        return switch (status.toLowerCase()) {
            case "confirme", "active" -> "rgba(67, 181, 129, 0.2)"; // Success green
            case "termine" -> "rgba(67, 181, 129, 0.2)"; // Success green
            case "en_attente" -> "rgba(250, 166, 26, 0.2)"; // Warning orange
            case "refuse", "annule" -> "rgba(240, 71, 71, 0.2)"; // Danger red
            default -> "rgba(99, 102, 241, 0.2)"; // Default indigo
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
            loadReclamations();
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
