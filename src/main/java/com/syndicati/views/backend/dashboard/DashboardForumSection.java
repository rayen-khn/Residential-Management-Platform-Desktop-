package com.syndicati.views.backend.dashboard;

import com.syndicati.models.forum.Commentaire;
import com.syndicati.models.forum.Publication;
import com.syndicati.models.forum.Reaction;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import com.syndicati.services.forum.SentimentAnalysisService;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("SpellCheckingInspection")
final class DashboardForumSection {

    private DashboardForumSection() {
    }

    static VBox build(DashboardView view) {
        return view.moduleModeView(
            "Forum",
            "\uD83D\uDCAC",
            new String[]{"Publications", "Commentaires", "Reactions"},
            key -> "Commentaires".equals(key) ? commentsPane(view) :
                   "Reactions".equals(key) ? reactionsPane(view) :
                   publicationsPane(view)
        );
    }

    private static VBox publicationsPane(DashboardView view) {
        VBox wrap = new VBox(14);
        List<Publication> publications = view.dashboardAdminService().publications();
        List<Reaction> allReactions = view.dashboardAdminService().reactions();

        int totalPubs = publications.size();
        Map<String, Integer> catCounts = new HashMap<>();
        for (Publication pub : publications) {
            String cat = pub.getCategoriePub() != null ? pub.getCategoriePub() : "Other";
            catCounts.put(cat, catCounts.getOrDefault(cat, 0) + 1);
        }

        HBox stats = new HBox(16);
        stats.setFillHeight(true);
        
        // Prepare top categories for stat cards
        List<Map.Entry<String, Integer>> sortedCats = new ArrayList<>(catCounts.entrySet());
        sortedCats.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        List<String> codes = new ArrayList<>(List.of("TOT"));
        List<String> labels = new ArrayList<>(List.of("Total Pubs"));
        List<String> values = new ArrayList<>(List.of(String.valueOf(totalPubs)));
        List<String> colors = new ArrayList<>(List.of("#6366f1")); // Indigo for total

        String[] palette = {"#ec4899", "#8b5cf6", "#06b6d4", "#10b981", "#f59e0b", "#3b82f6", "#ef4444", "#6366f1"};
        int cardIdx = 0;
        for (Map.Entry<String, Integer> entry : sortedCats) {
            String catName = entry.getKey();
            String code = catName.length() >= 3 ? catName.substring(0, 3).toUpperCase() : catName.toUpperCase();
            codes.add(code);
            labels.add(catName);
            values.add(String.valueOf(entry.getValue()));
            colors.add(palette[cardIdx % palette.length]);
            cardIdx++;
        }

        view.addStatCards(stats,
            codes.toArray(new String[0]),
            labels.toArray(new String[0]),
            values.toArray(new String[0]),
            colors.toArray(new String[0])
        );

        ScrollPane statsScroll = new ScrollPane(stats);
        statsScroll.setFitToHeight(true);
        statsScroll.setPannable(true);
        statsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        statsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        statsScroll.setStyle("-fx-background-color:transparent; -fx-background:transparent; -fx-border-width:0;");
        statsScroll.setPadding(new Insets(0, 0, 10, 0));

        // Index reactions by publication ID for fast lookup O(1)
        java.util.Map<Integer, List<Reaction>> pubReactions = allReactions.stream()
            .filter(r -> r.getPublication() != null)
            .collect(java.util.stream.Collectors.groupingBy(r -> r.getPublication().getIdPublication()));

        List<String[]> baseRows = new ArrayList<>();
        for (Publication pub : publications) {
            String title = view.safe(pub.getTitrePub());
            String category = view.safe(pub.getCategoriePub());
            String description = view.safe(pub.getDescriptionPub());
            if (description.length() > 50) description = description.substring(0, 47) + "...";
            
            List<Reaction> reactions = pubReactions.getOrDefault(pub.getIdPublication(), java.util.Collections.emptyList());
            
            long likes = reactions.stream().filter(r -> "Like".equalsIgnoreCase(r.getKind())).count();
            long dislikes = reactions.stream().filter(r -> "Dislike".equalsIgnoreCase(r.getKind())).count();
            long bookmarks = reactions.stream().filter(r -> "Bookmark".equalsIgnoreCase(r.getKind())).count();
            long reports = reactions.stream().filter(r -> "Report".equalsIgnoreCase(r.getKind())).count();
            
            java.util.Map<String, Long> emojiCounts = reactions.stream()
                .filter(r -> "Emoji".equalsIgnoreCase(r.getKind()) && r.getEmoji() != null)
                .collect(java.util.stream.Collectors.groupingBy(Reaction::getEmoji, java.util.stream.Collectors.counting()));
            
            StringBuilder emojiSummary = new StringBuilder();
            emojiCounts.forEach((emoji, count) -> emojiSummary.append(emoji).append(": ").append(count).append("  "));
            if (emojiSummary.length() == 0) emojiSummary.append("-");

            String reportReasons = reactions.stream()
                .filter(r -> "Report".equalsIgnoreCase(r.getKind()) && r.getReportReason() != null)
                .map(Reaction::getReportReason)
                .collect(java.util.stream.Collectors.joining("\n"));
            if (reportReasons.isEmpty()) reportReasons = "-";

            String image = view.safe(pub.getImagePub());
            String date = pub.getDateCreationPub() != null ? pub.getDateCreationPub().toLocalDate().toString() : "";

            baseRows.add(new String[]{
                title, category, description, image, date,
                String.valueOf(likes), String.valueOf(dislikes), emojiSummary.toString(),
                String.valueOf(reports), reportReasons, String.valueOf(bookmarks)
            });
        }

        DashboardTableQueryEngine.QueryState queryState = new DashboardTableQueryEngine.QueryState(1_000_000);
        TextField searchField = dashboardSearchField(view, "Search publications by title, category, description or date...");
        Button sortPill = view.pillAction("Order: A-Z", false);
        HBox filterRow = new HBox(6);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        HBox headerControls = dashboardHeaderControls(searchField, sortPill, filterRow);
        VBox tableHost = new VBox();

        String[][] filters = new String[][]{
            {"title", "Title"},
            {"category", "Category"},
            {"date", "Date"}
        };

        for (String[] filter : filters) {
            String key = filter[0];
            Button button = view.pillAction(filter[1], false);
            button.setOnAction(e -> {
                queryState.filterKey = key.equals(queryState.filterKey) ? "" : key;
                queryState.page = 1;
                renderForumTable(view, baseRows, queryState, tableHost, sortPill, headerControls, filterRow, "Forum Publications", "Publication", new String[]{"Title", "Category", "Description", "Image", "Date", "[H]Likes", "[H]Dislikes", "[H]Emojis", "[H]Reports", "[H]Reasons", "[H]Bookmarks"}, "No publications found");
            });
            filterRow.getChildren().add(button);
        }

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            queryState.searchTerm = newVal == null ? "" : newVal;
            queryState.page = 1;
            renderForumTable(view, baseRows, queryState, tableHost, sortPill, headerControls, filterRow, "Forum Publications", "Publication", new String[]{"Title", "Category", "Description", "Image", "Date", "[H]Likes", "[H]Dislikes", "[H]Emojis", "[H]Reports", "[H]Reasons", "[H]Bookmarks"}, "No publications found");
        });

        sortPill.setOnAction(e -> {
            queryState.ascending = !queryState.ascending;
            renderForumTable(view, baseRows, queryState, tableHost, sortPill, headerControls, filterRow, "Forum Publications", "Publication", new String[]{"Title", "Category", "Description", "Image", "Date", "[H]Likes", "[H]Dislikes", "[H]Emojis", "[H]Reports", "[H]Reasons", "[H]Bookmarks"}, "No publications found");
        });

        filterRow.getChildren().forEach(node -> {
            if (node instanceof Button button) {
                styleQueryPill(view, button, false);
            }
        });

        renderForumTable(view, baseRows, queryState, tableHost, sortPill, headerControls, filterRow, "Forum Publications", "Publication", new String[]{"Title", "Category", "Description", "Image", "Date", "[H]Likes", "[H]Dislikes", "[H]Emojis", "[H]Reports", "[H]Reasons", "[H]Bookmarks"}, "No publications found");

        wrap.getChildren().addAll(statsScroll, headerControls, tableHost);
        return wrap;
    }

    private static VBox commentsPane(DashboardView view) {
        VBox wrap = new VBox(14);
        List<Commentaire> commentaires = view.dashboardAdminService().commentaires();
        List<Reaction> allReactions = view.dashboardAdminService().reactions();

        // Index reactions by comment ID for fast lookup O(1)
        java.util.Map<Integer, List<Reaction>> commReactions = allReactions.stream()
            .filter(r -> r.getCommentaire() != null)
            .collect(java.util.stream.Collectors.groupingBy(r -> r.getCommentaire().getIdCommentaire()));

        int totalComments = commentaires.size();
        HBox stats = new HBox(16);
        stats.setFillHeight(true);
        view.addStatCards(stats,
            new String[]{"COM"},
            new String[]{"Total Comments"},
            new String[]{String.valueOf(totalComments)},
            new String[]{"#10b981"}
        );

        List<String[]> baseRows = new ArrayList<>();
        for (Commentaire comment : commentaires) {
            String publication = comment.getPublication() != null ? view.safe(comment.getPublication().getTitrePub()) : "Unknown";
            String author = comment.getUser() != null ? view.safe(comment.getUser().getFirstName()) + " " + view.safe(comment.getUser().getLastName()) : "Anonymous";
            String description = view.safe(comment.getDescriptionCommentaire());
            if (description.length() > 50) description = description.substring(0, 47) + "...";
            
            List<Reaction> reactions = commReactions.getOrDefault(comment.getIdCommentaire(), java.util.Collections.emptyList());
            
            long likes = reactions.stream().filter(r -> "Like".equalsIgnoreCase(r.getKind())).count();
            long dislikes = reactions.stream().filter(r -> "Dislike".equalsIgnoreCase(r.getKind())).count();
            long reports = reactions.stream().filter(r -> "Report".equalsIgnoreCase(r.getKind())).count();
            
            java.util.Map<String, Long> emojiCounts = reactions.stream()
                .filter(r -> "Emoji".equalsIgnoreCase(r.getKind()) && r.getEmoji() != null)
                .collect(java.util.stream.Collectors.groupingBy(Reaction::getEmoji, java.util.stream.Collectors.counting()));
            
            StringBuilder emojiSummary = new StringBuilder();
            emojiCounts.forEach((emoji, count) -> emojiSummary.append(emoji).append(": ").append(count).append("  "));
            if (emojiSummary.length() == 0) emojiSummary.append("-");

            String reportReasons = reactions.stream()
                .filter(r -> "Report".equalsIgnoreCase(r.getKind()) && r.getReportReason() != null)
                .map(Reaction::getReportReason)
                .collect(java.util.stream.Collectors.joining("\n"));
            if (reportReasons.isEmpty()) reportReasons = "-";

            String image = view.safe(comment.getImageCommentaire());
            String date = comment.getCreatedAt() != null ? comment.getCreatedAt().toLocalDate().toString() : "";

            baseRows.add(new String[]{
                publication, author.trim(), description, image, date,
                String.valueOf(likes), String.valueOf(dislikes), emojiSummary.toString(),
                String.valueOf(reports), reportReasons
            });
        }

        DashboardTableQueryEngine.QueryState queryState = new DashboardTableQueryEngine.QueryState(1_000_000);
        TextField searchField = dashboardSearchField(view, "Search comments by publication, author, description or date...");
        Button sortPill = view.pillAction("Order: A-Z", false);
        HBox filterRow = new HBox(6);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        HBox headerControls = dashboardHeaderControls(searchField, sortPill, filterRow);
        VBox tableHost = new VBox();

        String[][] filters = new String[][]{
            {"publication", "Publication"},
            {"author", "Author"},
            {"date", "Date"}
        };

        for (String[] filter : filters) {
            String key = filter[0];
            Button button = view.pillAction(filter[1], false);
            button.setOnAction(e -> {
                queryState.filterKey = key.equals(queryState.filterKey) ? "" : key;
                queryState.page = 1;
                renderForumTable(view, baseRows, queryState, tableHost, sortPill, headerControls, filterRow, "Forum Comments", "Comment", new String[]{"Publication", "Author", "Description", "Image", "Date", "[H]Likes", "[H]Dislikes", "[H]Emojis", "[H]Reports", "[H]Reasons"}, "No comments found");
            });
            filterRow.getChildren().add(button);
        }

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            queryState.searchTerm = newVal == null ? "" : newVal;
            queryState.page = 1;
            renderForumTable(view, baseRows, queryState, tableHost, sortPill, headerControls, filterRow, "Forum Comments", "Comment", new String[]{"Publication", "Author", "Description", "Image", "Date", "[H]Likes", "[H]Dislikes", "[H]Emojis", "[H]Reports", "[H]Reasons"}, "No comments found");
        });

        sortPill.setOnAction(e -> {
            queryState.ascending = !queryState.ascending;
            renderForumTable(view, baseRows, queryState, tableHost, sortPill, headerControls, filterRow, "Forum Comments", "Comment", new String[]{"Publication", "Author", "Description", "Image", "Date", "[H]Likes", "[H]Dislikes", "[H]Emojis", "[H]Reports", "[H]Reasons"}, "No comments found");
        });

        filterRow.getChildren().forEach(node -> {
            if (node instanceof Button button) {
                styleQueryPill(view, button, false);
            }
        });

        renderForumTable(view, baseRows, queryState, tableHost, sortPill, headerControls, filterRow, "Forum Comments", "Comment", new String[]{"Publication", "Author", "Description", "Image", "Date", "[H]Likes", "[H]Dislikes", "[H]Emojis", "[H]Reports", "[H]Reasons"}, "No comments found");

        wrap.getChildren().addAll(stats, headerControls, tableHost);
        return wrap;
    }

    private static VBox reactionsPane(DashboardView view) {
        VBox wrap = new VBox(14);
        List<Reaction> reactions = view.dashboardAdminService().reactions();

        List<String[]> baseRows = new ArrayList<>();
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MMM dd");

        for (Reaction reaction : reactions) {
            String user = reaction.getUser() != null ? view.safe(reaction.getUser().getFirstName()) + " " + view.safe(reaction.getUser().getLastName()) : "Anonymous";
            user = user.trim();
            
            String target = "";
            if (reaction.getPublication() != null) {
                target = "Pub: " + view.safe(reaction.getPublication().getTitrePub());
            } else if (reaction.getCommentaire() != null) {
                target = "Comment";
            } else {
                target = "Unknown";
            }
            
            String kind = view.safe(reaction.getKind());
            String reason = "Report".equalsIgnoreCase(kind) ? view.safe(reaction.getReportReason()) : "-";
            String date = reaction.getUpdatedAt() != null ? reaction.getUpdatedAt().format(dateFormat) : "N/A";

            baseRows.add(new String[]{
                user,
                target,
                kind,
                reason,
                date
            });
        }

        DashboardTableQueryEngine.QueryState queryState = new DashboardTableQueryEngine.QueryState(1_000_000);
        TextField searchField = dashboardSearchField(view, "Search reactions by user, target, type or date...");
        Button sortPill = view.pillAction("Order: A-Z", false);
        HBox filterRow = new HBox(6);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        HBox headerControls = dashboardHeaderControls(searchField, sortPill, filterRow);
        VBox tableHost = new VBox();

        String[][] filters = new String[][]{
            {"user", "User"},
            {"target", "Target"},
            {"type", "Type"},
            {"date", "Date"}
        };

        for (String[] filter : filters) {
            String key = filter[0];
            Button button = view.pillAction(filter[1], false);
            button.setOnAction(e -> {
                queryState.filterKey = key.equals(queryState.filterKey) ? "" : key;
                queryState.page = 1;
                renderForumTable(view, baseRows, queryState, tableHost, sortPill, headerControls, filterRow, "Forum Reactions", "Reaction", new String[]{"User", "Target", "Type", "Reason", "Updated"}, "No reactions found");
            });
            filterRow.getChildren().add(button);
        }

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            queryState.searchTerm = newVal == null ? "" : newVal;
            queryState.page = 1;
            renderForumTable(view, baseRows, queryState, tableHost, sortPill, headerControls, filterRow, "Forum Reactions", "Reaction", new String[]{"User", "Target", "Type", "Reason", "Updated"}, "No reactions found");
        });

        sortPill.setOnAction(e -> {
            queryState.ascending = !queryState.ascending;
            renderForumTable(view, baseRows, queryState, tableHost, sortPill, headerControls, filterRow, "Forum Reactions", "Reaction", new String[]{"User", "Target", "Type", "Reason", "Updated"}, "No reactions found");
        });

        filterRow.getChildren().forEach(node -> {
            if (node instanceof Button button) {
                styleQueryPill(view, button, false);
            }
        });

        renderForumTable(view, baseRows, queryState, tableHost, sortPill, headerControls, filterRow, "Forum Reactions", "Reaction", new String[]{"User", "Target", "Type", "Reason", "Updated"}, "No reactions found");

        wrap.getChildren().addAll(headerControls, tableHost);
        return wrap;
    }

    private static HBox dashboardHeaderControls(TextField searchField, Button sortPill, HBox filterRow) {
        HBox headerControls = new HBox(8, searchField, sortPill, filterRow);
        headerControls.setAlignment(Pos.CENTER_LEFT);
        return headerControls;
    }

    private static void renderForumTable(
        DashboardView view,
        List<String[]> baseRows,
        DashboardTableQueryEngine.QueryState queryState,
        VBox tableHost,
        Button sortPill,
        HBox headerControls,
        HBox filterRow,
        String tableTitle,
        String entityLabel,
        String[] columns,
        String emptyMessage
    ) {
        String scope = queryState.filterKey;
        List<String[]> filteredRows = new ArrayList<>();
        for (String[] row : baseRows) {
            if (queryState.searchTerm == null || queryState.searchTerm.trim().isEmpty()) {
                filteredRows.add(row);
                continue;
            }

            String term = queryState.searchTerm.trim().toLowerCase();
            boolean matches = switch (scope) {
                case "title" -> row.length > 0 && row[0] != null && row[0].toLowerCase().contains(term);
                case "category" -> row.length > 1 && row[1] != null && row[1].toLowerCase().contains(term);
                case "publication" -> row.length > 0 && row[0] != null && row[0].toLowerCase().contains(term);
                case "author" -> row.length > 1 && row[1] != null && row[1].toLowerCase().contains(term);
                case "user" -> row.length > 0 && row[0] != null && row[0].toLowerCase().contains(term);
                case "target" -> row.length > 1 && row[1] != null && row[1].toLowerCase().contains(term);
                case "type" -> row.length > 2 && row[2] != null && row[2].toLowerCase().contains(term);
                case "date" -> row.length > 4 && row[4] != null && row[4].toLowerCase().contains(term);
                default -> {
                    boolean found = false;
                    for (String cell : row) {
                        if (cell != null && cell.toLowerCase().contains(term)) {
                            found = true;
                            break;
                        }
                    }
                    yield found;
                }
            };

            if (matches) {
                filteredRows.add(row);
            }
        }

        DashboardTableQueryEngine.QueryResult result = DashboardTableQueryEngine.apply(
            filteredRows,
            queryState,
            (row, key) -> true,
            0
        );

        String[][] visibleRows = result.pageRows.isEmpty()
            ? new String[][]{{emptyMessage, "-", "-", "-", "-"}}
            : result.pageRows.toArray(new String[0][]);

        sortPill.setText(queryState.ascending ? "Order: A-Z" : "Order: Z-A");
        tableHost.getChildren().clear();
        tableHost.getChildren().add(view.dataTableWithCrud(tableTitle, entityLabel, columns, visibleRows, true, headerControls));
    }

    private static TextField dashboardSearchField(DashboardView view, String promptText) {
        TextField searchField = new TextField();
        searchField.setPromptText(promptText);
        searchField.setPrefWidth(280);
        searchField.setFont(Font.font(view.lightFont(), FontWeight.NORMAL, 12));
        searchField.setStyle(
            "-fx-background-color:" + (view.isDark() ? "rgba(255,255,255,0.06)" : "rgba(15,23,42,0.04)") + ";" +
            "-fx-border-color:" + (view.isDark() ? "rgba(255,255,255,0.16)" : "rgba(15,23,42,0.14)") + ";" +
            "-fx-border-width:1;" +
            "-fx-border-radius:10px;" +
            "-fx-background-radius:10px;" +
            "-fx-text-fill:" + (view.isDark() ? "white" : "#111827") + ";" +
            "-fx-prompt-text-fill:" + (view.isDark() ? "rgba(255,255,255,0.45)" : "rgba(15,23,42,0.45)") + ";"
        );
        return searchField;
    }

    private static void styleQueryPill(DashboardView view, Button btn, boolean active) {
        if (active) {
            btn.setStyle(
                "-fx-background-color:" + view.accentRgba(1.0) + ";" +
                "-fx-text-fill:white;" +
                "-fx-border-width:0;" +
                "-fx-font-weight:bold;"
            );
        } else {
            btn.setStyle(
                "-fx-background-color:transparent;" +
                "-fx-text-fill:" + (view.isDark() ? "rgba(255,255,255,0.6)" : "rgba(15,23,42,0.6)") + ";" +
                "-fx-border-color:" + (view.isDark() ? "rgba(255,255,255,0.2)" : "rgba(15,23,42,0.2)") + ";" +
                "-fx-border-width:1;"
            );
        }
    }
}
