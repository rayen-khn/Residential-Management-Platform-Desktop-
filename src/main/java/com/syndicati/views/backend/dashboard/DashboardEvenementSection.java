package com.syndicati.views.backend.dashboard;

import com.syndicati.models.evenement.Evenement;
import com.syndicati.models.evenement.Participation;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

@SuppressWarnings("SpellCheckingInspection")
final class DashboardEvenementSection {

    private DashboardEvenementSection() {
    }

    static VBox build(DashboardView view) {
        return view.moduleModeView(
            "Evenement",
            "\uD83C\uDF89",
            new String[]{"Evenements", "Participations"},
            key -> "Participations".equals(key) ? participationsPane(view) : eventsPane(view)
        );
    }

    private static VBox eventsPane(DashboardView view) {
        VBox wrap = new VBox(16);
        List<Evenement> evenements = view.dashboardAdminService().evenements();

        int total = evenements.size();
        int open = 0;
        int full = 0;
        int closed = 0;

        for (Evenement e : evenements) {
            String statut = e.getStatutEvent();
            if ("planifie".equals(statut)) {
                open++;
            } else if ("termine".equals(statut)) {
                closed++;
            } else if (e.getNbRestants() != null && e.getNbRestants() <= 0) {
                full++;
            }
        }

        HBox stats = new HBox(16);
        stats.setFillHeight(true);
        view.addStatCards(stats,
            new String[]{"TOT", "OPN", "FULL", "CLS"},
            new String[]{"Total", "Open", "Full", "Closed"},
            new String[]{String.valueOf(total), String.valueOf(open), String.valueOf(full), String.valueOf(closed)},
            new String[]{"#60a5fa", "#34d399", "#fbbf24", "#a78bfa"}
        );

        List<String[]> baseRows = new ArrayList<>();
        for (Evenement e : evenements) {
            String dateStr = e.getDateEvent() != null ? e.getDateEvent().toLocalDate().toString() : "";
            String location = view.safe(e.getLieuEvent());
            String description = view.safe(e.getDescriptionEvent());
            String totalPlaces = e.getNbPlaces() != null ? String.valueOf(e.getNbPlaces()) : "";
            String availablePlaces = e.getNbRestants() != null ? String.valueOf(e.getNbRestants()) : "";
            String image = view.safe(e.getImageEvent());

            baseRows.add(new String[]{
                view.safe(e.getTitreEvent()),
                view.safe(e.getTypeEvent()),
                description,
                dateStr,
                location,
                totalPlaces,
                availablePlaces,
                image
            });
        }

        DashboardTableQueryEngine.QueryState queryState = new DashboardTableQueryEngine.QueryState(1_000_000);

        TextField searchField = new TextField();
        searchField.setPromptText("Search events by title, type or date...");
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
            {"type", "Type"},
            {"date", "Date"}
        };

        HBox filterRow = new HBox(6);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        Map<String, Button> filterButtons = new LinkedHashMap<>();

        VBox tableHost = new VBox();

        for (String[] filter : filters) {
            String key = filter[0];
            Button b = view.pillAction(filter[1], false);
            b.setOnAction(e -> {
                queryState.filterKey = key.equals(queryState.filterKey) ? "" : key;
                queryState.page = 1;
                filterButtons.forEach((k, btn) -> styleQueryPill(view, btn, k.equals(queryState.filterKey)));
                renderEventsTable(view, baseRows, queryState, tableHost, sortPill);
            });
            filterButtons.put(key, b);
            filterRow.getChildren().add(b);
        }

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            queryState.searchTerm = newVal == null ? "" : newVal;
            queryState.page = 1;
            renderEventsTable(view, baseRows, queryState, tableHost, sortPill);
        });

        sortPill.setOnAction(e -> {
            queryState.ascending = !queryState.ascending;
            renderEventsTable(view, baseRows, queryState, tableHost, sortPill);
        });

        filterButtons.forEach((k, btn) -> styleQueryPill(view, btn, false));
        renderEventsTable(view, baseRows, queryState, tableHost, sortPill);

        HBox controls = new HBox(8, searchField, sortPill, filterRow);
        controls.setAlignment(Pos.CENTER_LEFT);

        wrap.getChildren().addAll(stats, controls, tableHost);
        return wrap;
    }

    private static void renderEventsTable(
        DashboardView view,
        List<String[]> baseRows,
        DashboardTableQueryEngine.QueryState queryState,
        VBox tableHost,
        Button sortPill
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
                case "type"   -> row[1] != null && row[1].toLowerCase().contains(scopedTerm);
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
            0
        );

        String[][] visibleRows;
        if (result.pageRows.isEmpty()) {
            visibleRows = new String[][]{{"No events found", "-", "-", "-", "-", "-", "-", "-"}};
        } else {
            visibleRows = result.pageRows.toArray(new String[0][]);
        }

        sortPill.setText(queryState.ascending ? "Order: A-Z" : "Order: Z-A");
        tableHost.getChildren().clear();
        tableHost.getChildren().add(
            view.dataTableWithCrud(
                "Evenements",
                "Event",
                new String[]{"Title", "Type", "Description", "Date", "Location", "Total Places", "Available Places", "Image"},
                visibleRows,
                true
            )
        );
    }

    private static VBox participationsPane(DashboardView view) {
        VBox wrap = new VBox(16);
        List<Participation> participations = view.dashboardAdminService().participations();

        int total = participations.size();
        int confirmed = 0;
        int pending = 0;
        int refused = 0;

        for (Participation p : participations) {
            String statut = p.getStatutParticipation();
            if ("confirme".equals(statut)) {
                confirmed++;
            } else if ("en_attente".equals(statut)) {
                pending++;
            } else if ("refuse".equals(statut)) {
                refused++;
            }
        }

        HBox stats = new HBox(16);
        stats.setFillHeight(true);
        view.addStatCards(stats,
            new String[]{"TOT", "OK", "PND", "RFD"},
            new String[]{"Total", "Confirmed", "Pending", "Refused"},
            new String[]{String.valueOf(total), String.valueOf(confirmed), String.valueOf(pending), String.valueOf(refused)},
            new String[]{"#60a5fa", "#34d399", "#fbbf24", "#a78bfa"}
        );

        List<String[]> baseRows = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        for (Participation p : participations) {
            String eventTitle = p.getEvenement() != null ? view.safe(p.getEvenement().getTitreEvent()) : "Unknown";
            String userName = p.getUser() != null && p.getUser().getFirstName() != null
                ? p.getUser().getFirstName() + " " + (p.getUser().getLastName() != null ? p.getUser().getLastName() : "")
                : "Unknown";
            String seats = p.getNbAccompagnants() != null ? String.valueOf(p.getNbAccompagnants() + 1) : "1";
            String checkedIn = "No";
            String dateStr = p.getDateParticipation() != null ? p.getDateParticipation().format(formatter) : "TBD";

            baseRows.add(new String[]{
                eventTitle,
                userName.trim(),
                seats,
                checkedIn,
                dateStr
            });
        }

        DashboardTableQueryEngine.QueryState queryState = new DashboardTableQueryEngine.QueryState(1_000_000);

        TextField searchField = new TextField();
        searchField.setPromptText("Search by event, resident, or date...");
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
            {"event", "Event"},
            {"resident", "Resident"},
            {"status", "Status"}
        };

        HBox filterRow = new HBox(6);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        Map<String, Button> filterButtons = new LinkedHashMap<>();

        VBox tableHost = new VBox();

        for (String[] filter : filters) {
            String key = filter[0];
            Button b = view.pillAction(filter[1], false);
            b.setOnAction(e -> {
                queryState.filterKey = key.equals(queryState.filterKey) ? "" : key;
                queryState.page = 1;
                filterButtons.forEach((k, btn) -> styleQueryPill(view, btn, k.equals(queryState.filterKey)));
                renderParticipationsTable(view, baseRows, queryState, tableHost, sortPill);
            });
            filterButtons.put(key, b);
            filterRow.getChildren().add(b);
        }

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            queryState.searchTerm = newVal == null ? "" : newVal;
            queryState.page = 1;
            renderParticipationsTable(view, baseRows, queryState, tableHost, sortPill);
        });

        sortPill.setOnAction(e -> {
            queryState.ascending = !queryState.ascending;
            renderParticipationsTable(view, baseRows, queryState, tableHost, sortPill);
        });

        filterButtons.forEach((k, btn) -> styleQueryPill(view, btn, false));
        renderParticipationsTable(view, baseRows, queryState, tableHost, sortPill);

        HBox controls = new HBox(8, searchField, sortPill, filterRow);
        controls.setAlignment(Pos.CENTER_LEFT);

        wrap.getChildren().addAll(stats, controls, tableHost);
        return wrap;
    }

    private static void renderParticipationsTable(
        DashboardView view,
        List<String[]> baseRows,
        DashboardTableQueryEngine.QueryState queryState,
        VBox tableHost,
        Button sortPill
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
                case "event"   -> row[0] != null && row[0].toLowerCase().contains(scopedTerm);
                case "resident" -> row[1] != null && row[1].toLowerCase().contains(scopedTerm);
                case "status"  -> row[3] != null && row[3].toLowerCase().contains(scopedTerm);
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
            0
        );

        String[][] visibleRows;
        if (result.pageRows.isEmpty()) {
            visibleRows = new String[][]{{"No participations found", "-", "-", "-", "-"}};
        } else {
            visibleRows = result.pageRows.toArray(new String[0][]);
        }

        sortPill.setText(queryState.ascending ? "Order: A-Z" : "Order: Z-A");
        tableHost.getChildren().clear();
        tableHost.getChildren().add(
            view.dataTableWithCrud(
                "Participations",
                "Participation",
                new String[]{"Event", "Resident", "Seats", "Checked-in", "Date"},
                visibleRows,
                false
            )
        );
    }

    private static void styleQueryPill(DashboardView view, Button btn, boolean active) {
        if (active) {
            btn.setStyle(
                "-fx-background-color: " + view.accentRgba(1.0) + ";" +
                "-fx-text-fill: white;" +
                "-fx-border-width: 0;" +
                "-fx-font-weight: bold;"
            );
        } else {
            btn.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: " + (view.isDark() ? "rgba(255,255,255,0.6)" : "rgba(15,23,42,0.6)") + ";" +
                "-fx-border-color: " + (view.isDark() ? "rgba(255,255,255,0.2)" : "rgba(15,23,42,0.2)") + ";" +
                "-fx-border-width: 1;"
            );
        }
    }
}
