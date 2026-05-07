package com.syndicati.views.backend.dashboard;

import com.syndicati.controllers.syndicat.ReclamationController;
import com.syndicati.models.syndicat.Reclamation;
import com.syndicati.models.syndicat.Reponse;
import com.syndicati.services.openai.OpenAiReclamationService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

@SuppressWarnings("SpellCheckingInspection")
final class DashboardSyndicatSection {

    private DashboardSyndicatSection() {
    }

    static VBox build(DashboardView view) {
        return view.moduleModeView(
            "Syndicat",
            "📋",
            new String[]{"Reclamations", "Reponses"},
            key -> "Reponses".equals(key) ? reponseTablePane(view) :
                   reclamationsTablePane(view)
        );
    }

    private static VBox reclamationsTablePane(DashboardView view) {
        VBox wrap = new VBox(16);
        ReclamationController controller = new ReclamationController();
        List<Reclamation> reclamations = controller.reclamations();

        int total = reclamations.size();
        int active = 0;
        int pending = 0;
        int resolved = 0;
        int rejected = 0;

        for (Reclamation rec : reclamations) {
            String s = rec.getStatutReclamation();
            if ("active".equals(s)) {
                active++;
            } else if ("en_attente".equals(s)) {
                pending++;
            } else if ("termine".equals(s)) {
                resolved++;
            } else if ("refuse".equals(s)) {
                rejected++;
            }
        }

        HBox stats = new HBox(16);
        stats.setFillHeight(true);
        view.addStatCards(stats,
            new String[]{"TOT", "ACT", "PEN", "RES", "REJ"},
            new String[]{"Total", "Active", "Pending", "Resolved", "Rejected"},
            new String[]{String.valueOf(total), String.valueOf(active), String.valueOf(pending), String.valueOf(resolved), String.valueOf(rejected)},
            new String[]{"#60a5fa", "#10b981", "#f59e0b", "#3b82f6", "#ef4444"}
        );

        List<String[]> baseRows = new ArrayList<>();
        for (Reclamation rec : reclamations) {
            String userName = rec.getUser() != null ? rec.getUser().getFirstName() + " " + rec.getUser().getLastName() : "Unknown";
            String title = rec.getTitreReclamations() != null ? rec.getTitreReclamations() : "-";
            String status = rec.getStatutReclamation() != null ? rec.getStatutReclamation() : "-";
            String date = rec.getCreatedAt() != null ? rec.getCreatedAt().toString().substring(0, 10) : "-";
            List<Reponse> reps = controller.reponsesByReclamation(rec);
            int replyCount = reps != null ? reps.size() : 0;

            baseRows.add(new String[]{
                title,
                userName,
                status,
                date,
                String.valueOf(replyCount)
            });
        }

        DashboardTableQueryEngine.QueryState queryState = new DashboardTableQueryEngine.QueryState(1_000_000);
        final List<String[]>[] currentBaseRows = new List[]{baseRows};
        Button aiFilterBtn = view.pillAction("Physique Problem ✨", false);

        TextField searchField = new TextField();
        searchField.setPromptText("Search reclamations by title, user, or status...");
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

        Button sortPill = view.pillAction("Order: A-Z", false);
        HBox primaryControls = new HBox(8, searchField, sortPill);
        primaryControls.setAlignment(Pos.CENTER_LEFT);

        String[][] filters = new String[][]{
            {"title", "Title"},
            {"user", "User"},
            {"status", "Status"},
            {"date", "Date"}
        };

        HBox filterRow = new HBox(6);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        Map<String, Button> filterButtons = new LinkedHashMap<>();

        VBox tableHost = new VBox();
        HBox headerControls = new HBox(8, filterRow, aiFilterBtn, sortPill, searchField);
        headerControls.setAlignment(Pos.CENTER_LEFT);

        aiFilterBtn.setOnAction(e -> {
            if (currentBaseRows[0].size() < baseRows.size()) {
                currentBaseRows[0] = baseRows;
                styleQueryPill(view, aiFilterBtn, false);
                renderReclamationsTable(view, currentBaseRows[0], queryState, tableHost, sortPill, headerControls);
                return;
            }
            aiFilterBtn.setText("Analyzing...");
            aiFilterBtn.setDisable(true);
            new Thread(() -> {
                List<Reclamation> physicalOnly = OpenAiReclamationService.filterPhysicalProblems(reclamations);
                javafx.application.Platform.runLater(() -> {
                    aiFilterBtn.setText("Physique Problem ✨");
                    aiFilterBtn.setDisable(false);
                    
                    currentBaseRows[0] = convertReclamationsToRows(physicalOnly, controller);
                    styleQueryPill(view, aiFilterBtn, true);
                    renderReclamationsTable(view, currentBaseRows[0], queryState, tableHost, sortPill, headerControls);
                });
            }).start();
        });

        for (String[] filter : filters) {
            String key = filter[0];
            Button b = view.pillAction(filter[1], false);
            b.setOnAction(e -> {
                queryState.filterKey = key.equals(queryState.filterKey) ? "" : key;
                queryState.page = 1;
                filterButtons.forEach((k, btn) -> styleQueryPill(view, btn, k.equals(queryState.filterKey)));
                renderReclamationsTable(view, currentBaseRows[0], queryState, tableHost, sortPill, headerControls);
            });
            filterButtons.put(key, b);
            filterRow.getChildren().add(b);
        }

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            queryState.searchTerm = newVal == null ? "" : newVal;
            queryState.page = 1;
            renderReclamationsTable(view, currentBaseRows[0], queryState, tableHost, sortPill, headerControls);
        });

        sortPill.setOnAction(e -> {
            queryState.ascending = !queryState.ascending;
            renderReclamationsTable(view, currentBaseRows[0], queryState, tableHost, sortPill, headerControls);
        });

        filterButtons.forEach((k, btn) -> styleQueryPill(view, btn, false));
        renderReclamationsTable(view, currentBaseRows[0], queryState, tableHost, sortPill, headerControls);

        wrap.getChildren().addAll(stats, tableHost);
        return wrap;
    }

    private static void renderReclamationsTable(
        DashboardView view,
        List<String[]> baseRows,
        DashboardTableQueryEngine.QueryState queryState,
        VBox tableHost,
        Button sortPill,
        javafx.scene.Node headerControls
    ) {
        String scopedTerm = queryState.searchTerm.trim().toLowerCase();
        String scope = queryState.filterKey;

        List<String[]> scopedRows = new ArrayList<>();
        for (String[] row : baseRows) {
            if (scopedTerm.isEmpty()) {
                scopedRows.add(row);
                continue;
            }

            boolean matches = switch (scope) {
                case "title"  -> row[0] != null && row[0].toLowerCase().contains(scopedTerm);
                case "user"   -> row[1] != null && row[1].toLowerCase().contains(scopedTerm);
                case "status" -> row[2] != null && row[2].toLowerCase().contains(scopedTerm);
                case "date"   -> row[3] != null && row[3].toLowerCase().contains(scopedTerm);
                default -> {
                    boolean found = false;
                    for (String cell : row) {
                        if (cell != null && cell.toLowerCase().contains(scopedTerm)) {
                            found = true;
                            break;
                        }
                    }
                    yield found;
                }
            };

            if (matches) {
                scopedRows.add(row);
            }
        }

        DashboardTableQueryEngine.QueryResult result = DashboardTableQueryEngine.apply(
            scopedRows,
            queryState,
            (row, key) -> true,
            getReclamationsFilterColumnIndex(scope)
        );

        String[][] visibleRows;
        if (result.pageRows.isEmpty()) {
            visibleRows = new String[][]{{"No reclamations found", "-", "-", "-", "-"}};
        } else {
            visibleRows = result.pageRows.toArray(new String[0][]);
        }

        tableHost.getChildren().setAll(view.dataTableWithCrud(
            "Reclamations Table",
            "Reclamation",
            new String[]{"Title", "User", "Status", "Date", "Replies"},
            visibleRows,
            true,
            headerControls
        ));

        styleQueryPill(view, sortPill, queryState.ascending);
        String filterLabel = scope.isEmpty() ? "Title" : scope.substring(0, 1).toUpperCase() + scope.substring(1);
        sortPill.setText(queryState.ascending ? "Order: " + filterLabel + " A-Z" : "Order: " + filterLabel + " Z-A");
    }

    private static VBox reponseTablePane(DashboardView view) {
        VBox wrap = new VBox(16);
        ReclamationController controller = new ReclamationController();
        List<Reponse> reponses = controller.reponses();

        int total = reponses.size();

        HBox stats = new HBox(16);
        stats.setFillHeight(true);
        view.addStatCards(stats,
            new String[]{"TOT"},
            new String[]{"Total"},
            new String[]{String.valueOf(total)},
            new String[]{"#60a5fa"}
        );

        List<String[]> baseRows = new ArrayList<>();
        for (Reponse rep : reponses) {
            String userName = rep.getUser() != null ? rep.getUser().getFirstName() + " " + rep.getUser().getLastName() : "Unknown";
            String message = rep.getMessageReponse() != null ? rep.getMessageReponse() : "-";
            if (message.length() > 50) {
                message = message.substring(0, 47) + "...";
            }
            String reclamationTitle = rep.getReclamation() != null ? rep.getReclamation().getTitreReclamations() : "-";
            String date = rep.getCreatedAt() != null ? rep.getCreatedAt().toString().substring(0, 10) : "-";
            String title = rep.getTitreReponse() != null ? rep.getTitreReponse() : "-";
            String image = rep.getImageReponse() != null ? rep.getImageReponse() : "-";

            baseRows.add(new String[]{
                title,
                message,
                userName,
                reclamationTitle,
                date,
                image
            });
        }

        DashboardTableQueryEngine.QueryState queryState = new DashboardTableQueryEngine.QueryState(1_000_000);

        TextField searchField = new TextField();
        searchField.setPromptText("Search responses...");
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

        Button sortPill = view.pillAction("Order: A-Z", false);
        
        String[][] filters = new String[][]{
            {"title", "Title"},
            {"message", "Message"},
            {"user", "User"},
            {"reclamation", "Reclamation"},
            {"date", "Date"}
        };

        HBox filterRow = new HBox(6);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        Map<String, Button> filterButtons = new LinkedHashMap<>();

        VBox tableHost = new VBox();
        HBox headerControls = new HBox(8, filterRow, sortPill, searchField);
        headerControls.setAlignment(Pos.CENTER_LEFT);

        for (String[] filter : filters) {
            String key = filter[0];
            Button b = view.pillAction(filter[1], false);
            b.setOnAction(e -> {
                queryState.filterKey = key.equals(queryState.filterKey) ? "" : key;
                queryState.page = 1;
                filterButtons.forEach((k, btn) -> styleQueryPill(view, btn, k.equals(queryState.filterKey)));
                renderReponsesTable(view, baseRows, queryState, tableHost, sortPill, headerControls);
            });
            filterButtons.put(key, b);
            filterRow.getChildren().add(b);
        }

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            queryState.searchTerm = newVal == null ? "" : newVal;
            queryState.page = 1;
            renderReponsesTable(view, baseRows, queryState, tableHost, sortPill, headerControls);
        });

        sortPill.setOnAction(e -> {
            queryState.ascending = !queryState.ascending;
            renderReponsesTable(view, baseRows, queryState, tableHost, sortPill, headerControls);
        });

        filterButtons.forEach((k, btn) -> styleQueryPill(view, btn, false));
        renderReponsesTable(view, baseRows, queryState, tableHost, sortPill, headerControls);

        wrap.getChildren().addAll(stats, tableHost);
        return wrap;
    }

    private static void renderReponsesTable(
        DashboardView view,
        List<String[]> baseRows,
        DashboardTableQueryEngine.QueryState queryState,
        VBox tableHost,
        Button sortPill,
        javafx.scene.Node headerControls
    ) {
        String scopedTerm = queryState.searchTerm.trim().toLowerCase();
        String scope = queryState.filterKey;

        List<String[]> scopedRows = new ArrayList<>();
        for (String[] row : baseRows) {
            if (scopedTerm.isEmpty()) {
                scopedRows.add(row);
                continue;
            }

            boolean matches = switch (scope) {
                case "title"       -> row[0] != null && row[0].toLowerCase().contains(scopedTerm);
                case "message"     -> row[1] != null && row[1].toLowerCase().contains(scopedTerm);
                case "user"        -> row[2] != null && row[2].toLowerCase().contains(scopedTerm);
                case "reclamation" -> row[3] != null && row[3].toLowerCase().contains(scopedTerm);
                case "date"        -> row[4] != null && row[4].toLowerCase().contains(scopedTerm);
                default -> {
                    boolean found = false;
                    for (String cell : row) {
                        if (cell != null && cell.toLowerCase().contains(scopedTerm)) {
                            found = true;
                            break;
                        }
                    }
                    yield found;
                }
            };

            if (matches) {
                scopedRows.add(row);
            }
        }

        DashboardTableQueryEngine.QueryResult result = DashboardTableQueryEngine.apply(
            scopedRows,
            queryState,
            (row, key) -> true,
            getReponsesFilterColumnIndex(scope)
        );

        String[][] visibleRows;
        if (result.pageRows.isEmpty()) {
            visibleRows = new String[][]{{"No responses found", "-", "-", "-", "-", "-"}};
        } else {
            visibleRows = result.pageRows.toArray(new String[0][]);
        }

        tableHost.getChildren().setAll(view.dataTableWithCrud(
            "Reponses Table",
            "Reponse",
            new String[]{"Title", "Message", "User", "Reclamation", "Date", "[H]Image"},
            visibleRows,
            true,
            headerControls
        ));

        styleQueryPill(view, sortPill, queryState.ascending);
        String filterLabel = scope.isEmpty() ? "Title" : scope.substring(0, 1).toUpperCase() + scope.substring(1);
        sortPill.setText(queryState.ascending ? "Order: " + filterLabel + " A-Z" : "Order: " + filterLabel + " Z-A");
    }

    private static void styleQueryPill(DashboardView view, Button b, boolean active) {
        if (active) {
            b.setStyle(
                "-fx-background-color:" + view.accentRgba(0.24) + ";" +
                "-fx-border-color:" + view.accentRgba(0.34) + ";" +
                "-fx-border-width:1;" +
                "-fx-background-radius:100px;" +
                "-fx-border-radius:100px;" +
                "-fx-text-fill:white;" +
                "-fx-cursor:hand;"
            );
        } else {
            b.setStyle(
                "-fx-background-color:transparent;" +
                "-fx-border-color:" + (view.isDark() ? "rgba(255,255,255,0.16)" : "rgba(15,23,42,0.20)") + ";" +
                "-fx-border-width:1;" +
                "-fx-background-radius:100px;" +
                "-fx-border-radius:100px;" +
                "-fx-text-fill:" + (view.isDark() ? "rgba(255,255,255,0.80)" : "rgba(15,23,42,0.86)") + ";" +
                "-fx-cursor:hand;"
            );
        }
    }

    private static List<String[]> convertReclamationsToRows(List<Reclamation> list, ReclamationController controller) {
        List<String[]> rows = new ArrayList<>();
        for (Reclamation rec : list) {
            String userName = rec.getUser() != null ? rec.getUser().getFirstName() + " " + rec.getUser().getLastName() : "Unknown";
            String title = rec.getTitreReclamations() != null ? rec.getTitreReclamations() : "-";
            String status = rec.getStatutReclamation() != null ? rec.getStatutReclamation() : "-";
            String date = rec.getCreatedAt() != null ? rec.getCreatedAt().toString().substring(0, 10) : "-";
            List<Reponse> reps = controller.reponsesByReclamation(rec);
            int replyCount = reps != null ? reps.size() : 0;

            rows.add(new String[]{
                title,
                userName,
                status,
                date,
                String.valueOf(replyCount)
            });
        }
        return rows;
    }

    private static int getReclamationsFilterColumnIndex(String filterKey) {
        return switch (filterKey) {
            case "title" -> 0;
            case "user" -> 1;
            case "status" -> 2;
            case "date" -> 3;
            default -> -1;
        };
    }

    private static int getReponsesFilterColumnIndex(String filterKey) {
        return switch (filterKey) {
            case "title" -> 0;
            case "message" -> 1;
            case "user" -> 2;
            case "reclamation" -> 3;
            case "date" -> 4;
            default -> -1;
        };
    }
}
