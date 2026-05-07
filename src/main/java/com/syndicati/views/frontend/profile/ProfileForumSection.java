package com.syndicati.views.frontend.profile;

import com.syndicati.controllers.forum.PublicationController;
import com.syndicati.models.forum.Publication;
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

/**
 * Forum section showing user's publications, comments, reactions, and bookmarks.
 */
public class ProfileForumSection {
    
    private final VBox root = new VBox(16);
    private final PublicationController publicationController;
    
    // UI Elements for switching between sub-tabs
    private final VBox publicationListView = new VBox(10);
    private final VBox commentListView = new VBox(10);
    private final VBox reactionListView = new VBox(10);
    private final VBox bookmarkListView = new VBox(10);
    
    // Current page for pagination
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 3;

    public ProfileForumSection() {
        this.publicationController = new PublicationController();
        buildLayout();
    }

    private void buildLayout() {
        root.setPadding(new Insets(16, 0, 0, 0));
        
        VBox card = new VBox(16);
        card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 1; -fx-background-radius: 20; -fx-border-radius: 20;");
        card.setPadding(new Insets(22));
        
        // Title
        Label title = new Label("Your Forum Activity");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);
        card.getChildren().add(title);
        
        // Category Filters
        HBox categoryFilters = createCategoryFilters();
        card.getChildren().add(categoryFilters);
        
        // Sub-tabs for Publications, Comments, Reactions, Bookmarks
        HBox subTabs = createSubTabs();
        card.getChildren().add(subTabs);
        
        // Content Container (switches between sub-tabs)
        VBox contentContainer = new VBox(publicationListView, commentListView, reactionListView, bookmarkListView);
        VBox.setVgrow(contentContainer, Priority.ALWAYS);
        card.getChildren().add(contentContainer);
        
        // Pagination
        HBox pagination = createPagination();
        card.getChildren().add(pagination);
        
        // Load real data
        loadPublications();
        
        root.getChildren().add(card);
    }

    private HBox createCategoryFilters() {
        HBox filters = new HBox(8);
        filters.setAlignment(Pos.CENTER_LEFT);
        
        Button announcements = new Button("Announcements");
        announcements.setStyle("-fx-padding: 6 12 6 12; -fx-background-color: rgba(255, 255, 255, 0.05); -fx-text-fill: rgba(255, 255, 255, 0.7); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 1; -fx-background-radius: 999; -fx-border-radius: 999;");
        
        Button general = new Button("General");
        general.setStyle("-fx-padding: 6 12 6 12; -fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white; -fx-border-color: transparent; -fx-background-radius: 999; -fx-border-radius: 999; -fx-font-weight: bold;");
        
        filters.getChildren().addAll(announcements, general);
        return filters;
    }

    private HBox createSubTabs() {
        HBox tabs = new HBox(8);
        tabs.setPadding(new Insets(6));
        tabs.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 1; -fx-background-radius: 999; -fx-border-radius: 999;");
        
        Button pubBtn = new Button("Publications (2)");
        pubBtn.setStyle("-fx-padding: 8 14 8 14; -fx-background-color: rgba(99, 102, 241, 0.2); -fx-text-fill: white; -fx-border-color: rgba(99, 102, 241, 0.3); -fx-background-radius: 8; -fx-border-radius: 8; -fx-font-weight: bold;");
        pubBtn.setOnAction(e -> showPublications());
        
        Button comBtn = new Button("Comments (1)");
        comBtn.setStyle("-fx-padding: 8 14 8 14; -fx-background-color: transparent; -fx-text-fill: rgba(255, 255, 255, 0.6); -fx-border-color: transparent; -fx-background-radius: 8;");
        comBtn.setOnAction(e -> showComments());
        
        Button reactBtn = new Button("Reactions (1)");
        reactBtn.setStyle("-fx-padding: 8 14 8 14; -fx-background-color: transparent; -fx-text-fill: rgba(255, 255, 255, 0.6); -fx-border-color: transparent; -fx-background-radius: 8;");
        reactBtn.setOnAction(e -> showReactions());
        
        Button bookBtn = new Button("Bookmarks (1)");
        bookBtn.setStyle("-fx-padding: 8 14 8 14; -fx-background-color: transparent; -fx-text-fill: rgba(255, 255, 255, 0.6); -fx-border-color: transparent; -fx-background-radius: 8;");
        bookBtn.setOnAction(e -> showBookmarks());
        
        tabs.getChildren().addAll(pubBtn, comBtn, reactBtn, bookBtn);
        return tabs;
    }

    private void loadPublications() {
        publicationListView.getChildren().clear();
        
        List<Publication> publications = publicationController.publications();
        
        if (publications == null || publications.isEmpty()) {
            Label noData = new Label("No publications found");
            noData.setTextFill(Color.color(1, 1, 1, 0.5));
            publicationListView.getChildren().add(noData);
        } else {
            // Show paginated publications (3 per page)
            int start = currentPage * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, publications.size());
            
            for (int i = start; i < end; i++) {
                Publication pub = publications.get(i);
                publicationListView.getChildren().add(createPublicationItem(pub));
            }
        }
    }

    private VBox createPublicationItem(Publication publication) {
        VBox item = new VBox(8);
        item.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-border-color: rgba(255, 255, 255, 0.05); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12; -fx-padding: 12;");
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label icon = new Label("📝");
        icon.setFont(Font.font(16));
        
        Label title = new Label(publication.getTitrePub());
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        title.setTextFill(Color.WHITE);
        
        String dateStr = publication.getDateCreationPub() != null 
            ? publication.getDateCreationPub().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
            : "—";
        
        Label date = new Label(dateStr);
        date.setFont(Font.font("Segoe UI", 11));
        date.setTextFill(Color.color(1, 1, 1, 0.5));
        
        HBox.setHgrow(title, Priority.ALWAYS);
        header.getChildren().addAll(icon, title, date);
        
        Label category = new Label(publication.getCategoriePub() != null ? publication.getCategoriePub() : "General");
        category.setFont(Font.font("Segoe UI", 11));
        category.setTextFill(Color.color(99/255.0, 102/255.0, 241/255.0));
        category.setStyle("-fx-text-fill: rgba(99, 102, 241, 0.9);");
        
        Label description = new Label(publication.getDescriptionPub());
        description.setFont(Font.font("Segoe UI", 12));
        description.setTextFill(Color.color(1, 1, 1, 0.7));
        description.setWrapText(true);
        
        item.getChildren().addAll(header, category, description);
        return item;
    }

    private HBox createPagination() {
        HBox pagination = new HBox(8);
        pagination.setAlignment(Pos.CENTER);
        pagination.setPadding(new Insets(8));
        pagination.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-width: 1; -fx-background-radius: 999; -fx-border-radius: 999;");
        
        Button page1 = createPageButton("1", 0);
        Button page2 = createPageButton("2", 1);
        Button page3 = createPageButton("3", 2);
        
        pagination.getChildren().addAll(page1, page2, page3);
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
            loadPublications();
        });
        return btn;
    }

    private void showPublications() {
        publicationListView.setVisible(true);
        publicationListView.setManaged(true);
        commentListView.setVisible(false);
        commentListView.setManaged(false);
        reactionListView.setVisible(false);
        reactionListView.setManaged(false);
        bookmarkListView.setVisible(false);
        bookmarkListView.setManaged(false);
    }

    private void showComments() {
        publicationListView.setVisible(false);
        publicationListView.setManaged(false);
        commentListView.setVisible(true);
        commentListView.setManaged(true);
        reactionListView.setVisible(false);
        reactionListView.setManaged(false);
        bookmarkListView.setVisible(false);
        bookmarkListView.setManaged(false);
        
        commentListView.getChildren().clear();
        Label placeholder = new Label("Comments feature coming soon");
        placeholder.setTextFill(Color.color(1, 1, 1, 0.5));
        commentListView.getChildren().add(placeholder);
    }

    private void showReactions() {
        publicationListView.setVisible(false);
        publicationListView.setManaged(false);
        commentListView.setVisible(false);
        commentListView.setManaged(false);
        reactionListView.setVisible(true);
        reactionListView.setManaged(true);
        bookmarkListView.setVisible(false);
        bookmarkListView.setManaged(false);
        
        reactionListView.getChildren().clear();
        Label placeholder = new Label("Reactions feature coming soon");
        placeholder.setTextFill(Color.color(1, 1, 1, 0.5));
        reactionListView.getChildren().add(placeholder);
    }

    private void showBookmarks() {
        publicationListView.setVisible(false);
        publicationListView.setManaged(false);
        commentListView.setVisible(false);
        commentListView.setManaged(false);
        reactionListView.setVisible(false);
        reactionListView.setManaged(false);
        bookmarkListView.setVisible(true);
        bookmarkListView.setManaged(true);
        
        bookmarkListView.getChildren().clear();
        Label placeholder = new Label("Bookmarks feature coming soon");
        placeholder.setTextFill(Color.color(1, 1, 1, 0.5));
        bookmarkListView.getChildren().add(placeholder);
    }

    public VBox getRoot() {
        return root;
    }

    public void cleanup() {
        // Cleanup resources if needed
    }
}
