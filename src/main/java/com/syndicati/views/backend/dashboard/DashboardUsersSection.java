package com.syndicati.views.backend.dashboard;

import com.syndicati.models.user.Onboarding;
import com.syndicati.models.user.Profile;
import com.syndicati.models.user.User;
import java.time.LocalDateTime;
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
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

@SuppressWarnings("SpellCheckingInspection")
final class DashboardUsersSection {

    private DashboardUsersSection() {
    }

    static VBox build(DashboardView view) {
        return view.moduleModeView(
            "Users",
            "\uD83D\uDC65",
            new String[]{"Users", "Profile", "Onboarding"},
            key -> "Profile".equals(key) ? usersProfilePane(view) :
                   "Onboarding".equals(key) ? usersOnboardingPane(view) :
                   usersTablePane(view)
        );
    }

    private static VBox usersTablePane(DashboardView view) {
        VBox wrap = new VBox(16);
        List<User> users = view.dashboardAdminService().users();

        int registered = users.size();
        int verified = 0;
        int pending = 0;
        int active7Day = 0;
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        for (User user : users) {
            if (user.isVerified()) {
                verified++;
            } else {
                pending++;
            }

            LocalDateTime updatedAt = user.getUpdatedAt();
            if (updatedAt != null && updatedAt.isAfter(sevenDaysAgo)) {
                active7Day++;
            }
        }

        HBox stats = new HBox(16);
        stats.setFillHeight(true);
        view.addStatCards(stats,
            new String[]{"USR", "VER", "PEN", "ACT"},
            new String[]{"Registered", "Verified", "Pending", "7-Day Active"},
            new String[]{String.valueOf(registered), String.valueOf(verified), String.valueOf(pending), String.valueOf(active7Day)},
            new String[]{"#60a5fa", "#34d399", "#fbbf24", "#a78bfa"}
        );

        List<String[]> baseRows = new ArrayList<>();
        for (User u : users) {
            String fullName = ((u.getFirstName() == null ? "" : u.getFirstName()) + " " + (u.getLastName() == null ? "" : u.getLastName())).trim();
            if (fullName.isEmpty()) {
                fullName = "User #" + (u.getIdUser() == null ? "-" : u.getIdUser());
            }
            String status = u.isDisabled() ? "Disabled" : (u.isVerified() ? "Active" : "Pending");

            baseRows.add(new String[]{
                fullName,
                view.safe(u.getEmailUser()),
                view.safe(u.getRoleUser()),
                u.isVerified() ? "Yes" : "No",
                status
            });
        }

        // Users section owns query controls; shared card owns row pagination.
        DashboardTableQueryEngine.QueryState queryState = new DashboardTableQueryEngine.QueryState(1_000_000);

        TextField searchField = new TextField();
        searchField.setPromptText("Search users by name, email, role or status...");
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
            {"name", "Name"},
            {"email", "Email"},
            {"role", "Role"},
            {"verified", "Verified"},
            {"status", "Status"}
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
                renderUsersTable(view, baseRows, queryState, tableHost, sortPill, headerControls);
            });
            filterButtons.put(key, b);
            filterRow.getChildren().add(b);
        }

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            queryState.searchTerm = newVal == null ? "" : newVal;
            queryState.page = 1;
            renderUsersTable(view, baseRows, queryState, tableHost, sortPill, headerControls);
        });

        sortPill.setOnAction(e -> {
            queryState.ascending = !queryState.ascending;
            renderUsersTable(view, baseRows, queryState, tableHost, sortPill, headerControls);
        });

        filterButtons.forEach((k, btn) -> styleQueryPill(view, btn, false));
        renderUsersTable(view, baseRows, queryState, tableHost, sortPill, headerControls);

        wrap.getChildren().addAll(stats, tableHost);
        return wrap;
    }

    private static void renderUsersTable(
        DashboardView view,
        List<String[]> baseRows,
        DashboardTableQueryEngine.QueryState queryState,
        VBox tableHost,
        Button sortPill,
        Node headerControls
    ) {
        String scopedTerm = queryState.searchTerm.trim().toLowerCase();
        String scope = queryState.filterKey;

        List<String[]> scopedRows = new ArrayList<>();
        for (String[] row : baseRows) {
            if (scopedTerm.isEmpty()) {
                scopedRows.add(row);
                continue;
            }

            boolean matches;
            matches = switch (scope) {
                case "name"     -> row[0] != null && row[0].toLowerCase().contains(scopedTerm);
                case "email"    -> row[1] != null && row[1].toLowerCase().contains(scopedTerm);
                case "role"     -> row[2] != null && row[2].toLowerCase().contains(scopedTerm);
                case "verified" -> row[3] != null && row[3].toLowerCase().contains(scopedTerm);
                case "status"   -> row[4] != null && row[4].toLowerCase().contains(scopedTerm);
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
            getUsersFilterColumnIndex(scope)
        );

        String[][] visibleRows;
        if (result.pageRows.isEmpty()) {
            visibleRows = new String[][]{{"No users found", "-", "-", "-", "-"}};
        } else {
            visibleRows = result.pageRows.toArray(new String[0][]);
        }

        tableHost.getChildren().setAll(view.dataTableWithCrud(
            "Users Table",
            "User",
            new String[]{"Name", "Email", "Role", "Verified", "Status"},
            visibleRows,
            true,
            headerControls
        ));

        styleQueryPill(view, sortPill, queryState.ascending);
        String filterLabel = scope.isEmpty() ? "Name" : scope.substring(0, 1).toUpperCase() + scope.substring(1);
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

    private static int getUsersFilterColumnIndex(String filterKey) {
        return switch (filterKey) {
            case "name"     -> 0;
            case "email"    -> 1;
            case "role"     -> 2;
            case "verified" -> 3;
            case "status"   -> 4;
            default         -> 0;
        };
    }

    private static int getProfileFilterColumnIndex(String filterKey) {
        return switch (filterKey) {
            case "locale"   -> 0;
            case "theme"    -> 1;
            case "timezone" -> 2;
            case "avatar"   -> 3;
            default         -> 0;
        };
    }

    private static int getOnboardingFilterColumnIndex(String filterKey) {
        return switch (filterKey) {
            case "step"      -> 0;
            case "completed" -> 1;
            case "locale"    -> 2;
            case "theme"     -> 3;
            default          -> 0;
        };
    }

    private static VBox usersProfilePane(DashboardView view) {
        VBox wrap = new VBox(16);
        List<Profile> profiles = view.dashboardAdminService().profiles();

        HBox stats = new HBox(16);
        stats.setFillHeight(true);
        int totalProfiles = profiles.size();
        int withAvatar = (int) profiles.stream().filter(p -> p.getAvatar() != null && !p.getAvatar().isEmpty()).count();
        int withDescription = (int) profiles.stream().filter(p -> p.getDescriptionProfile() != null && !p.getDescriptionProfile().isEmpty()).count();

        view.addStatCards(stats,
            new String[]{"PFL", "AVT", "BIO", "LOC"},
            new String[]{"Total Profiles", "With Avatar", "With Bio", "Locales Set"},
            new String[]{String.valueOf(totalProfiles), String.valueOf(withAvatar), String.valueOf(withDescription), "-"},
            new String[]{"#60a5fa", "#34d399", "#a78bfa", "#fbbf24"}
        );

        List<String[]> baseRows = new ArrayList<>();
        for (Profile p : profiles) {
            baseRows.add(new String[]{
                view.safe(p.getLocale()),
                p.getTheme() == null ? "-" : String.valueOf(p.getTheme()),
                p.getTimezone() == null ? "-" : String.valueOf(p.getTimezone()),
                view.safe(p.getAvatar())
            });
        }

        DashboardTableQueryEngine.QueryState queryState = new DashboardTableQueryEngine.QueryState(1_000_000);

        TextField searchField = new TextField();
        searchField.setPromptText("Search profiles by id, user, locale, theme or timezone...");
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

        Button sortPill = view.pillAction("Order: ID Asc", false);

        String[][] filters = new String[][]{
            {"locale", "Locale"},
            {"theme", "Theme"},
            {"timezone", "Timezone"},
            {"avatar", "Avatar"}
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
                renderProfilesTable(view, baseRows, queryState, tableHost, sortPill, headerControls);
            });
            filterButtons.put(key, b);
            filterRow.getChildren().add(b);
        }

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            queryState.searchTerm = newVal == null ? "" : newVal;
            queryState.page = 1;
            renderProfilesTable(view, baseRows, queryState, tableHost, sortPill, headerControls);
        });

        sortPill.setOnAction(e -> {
            queryState.ascending = !queryState.ascending;
            renderProfilesTable(view, baseRows, queryState, tableHost, sortPill, headerControls);
        });

        queryState.filterKey = "";
        filterButtons.forEach((k, btn) -> styleQueryPill(view, btn, false));
        renderProfilesTable(view, baseRows, queryState, tableHost, sortPill, headerControls);

        wrap.getChildren().addAll(stats, tableHost);
        return wrap;
    }

    private static VBox usersOnboardingPane(DashboardView view) {
        VBox wrap = new VBox(14);
        List<Onboarding> onboardings = view.dashboardAdminService().onboardings();

        int total = onboardings.size();
        int completed = (int) onboardings.stream().filter(Onboarding::isCompleted).count();
        int inProgress = total - completed;
        int localeSet = (int) onboardings.stream().filter(o -> o.getSelectedLocale() != null && !o.getSelectedLocale().isBlank()).count();

        HBox stats = new HBox(16);
        stats.setFillHeight(true);
        view.addStatCards(stats,
            new String[]{"ONB", "CMP", "RUN", "LOC"},
            new String[]{"Total Onboarding", "Completed", "In Progress", "Locale Set"},
            new String[]{String.valueOf(total), String.valueOf(completed), String.valueOf(inProgress), String.valueOf(localeSet)},
            new String[]{"#60a5fa", "#34d399", "#fbbf24", "#a78bfa"}
        );

        List<String[]> baseRows = new ArrayList<>();
        for (Onboarding o : onboardings) {
            baseRows.add(new String[]{
                String.valueOf(o.getStep()),
                o.isCompleted() ? "Yes" : "No",
                view.safe(o.getSelectedLocale()),
                view.safe(o.getSelectedTheme()),
                view.formatDateTime(o.getUpdatedAt())
            });
        }

        DashboardTableQueryEngine.QueryState queryState = new DashboardTableQueryEngine.QueryState(1_000_000);

        TextField searchField = new TextField();
        searchField.setPromptText("Search onboarding by user, locale, theme, step or status...");
        searchField.setPrefWidth(300);
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

        Button sortPill = view.pillAction("Order: ID Asc", false);

        String[][] filters = new String[][]{
            {"step", "Step"},
            {"completed", "Completed"},
            {"locale", "Locale"},
            {"theme", "Theme"}
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
                renderOnboardingTable(view, baseRows, queryState, tableHost, sortPill, headerControls);
            });
            filterButtons.put(key, b);
            filterRow.getChildren().add(b);
        }

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            queryState.searchTerm = newVal == null ? "" : newVal;
            queryState.page = 1;
            renderOnboardingTable(view, baseRows, queryState, tableHost, sortPill, headerControls);
        });

        sortPill.setOnAction(e -> {
            queryState.ascending = !queryState.ascending;
            renderOnboardingTable(view, baseRows, queryState, tableHost, sortPill, headerControls);
        });

        queryState.filterKey = "";
        filterButtons.forEach((k, btn) -> styleQueryPill(view, btn, false));
        renderOnboardingTable(view, baseRows, queryState, tableHost, sortPill, headerControls);

        wrap.getChildren().addAll(stats, tableHost);
        return wrap;
    }

    private static void renderProfilesTable(
        DashboardView view,
        List<String[]> baseRows,
        DashboardTableQueryEngine.QueryState queryState,
        VBox tableHost,
        Button sortPill,
        Node headerControls
    ) {
        String scopedTerm = queryState.searchTerm.trim().toLowerCase();
        String scope = queryState.filterKey;

        List<String[]> scopedRows = new ArrayList<>();
        for (String[] row : baseRows) {
            if (scopedTerm.isEmpty()) {
                scopedRows.add(row);
                continue;
            }

            boolean matches;
            matches = switch (scope) {
                case "locale"   -> row[0] != null && row[0].toLowerCase().contains(scopedTerm);
                case "theme"    -> row[1] != null && row[1].toLowerCase().contains(scopedTerm);
                case "timezone" -> row[2] != null && row[2].toLowerCase().contains(scopedTerm);
                case "avatar"   -> row[3] != null && row[3].toLowerCase().contains(scopedTerm);
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
            getProfileFilterColumnIndex(scope)
        );

        String[][] visibleRows;
        if (result.pageRows.isEmpty()) {
            visibleRows = new String[][]{{"-", "-", "No profiles found", "-"}};
        } else {
            visibleRows = result.pageRows.toArray(new String[0][]);
        }

        tableHost.getChildren().setAll(view.dataTableWithCrud(
            "User Profile Data",
            "Profile",
            new String[]{"Locale", "Theme", "Timezone", "Avatar"},
            visibleRows,
            false,
            headerControls
        ));

        styleQueryPill(view, sortPill, queryState.ascending);
        String filterLabel = scope.isEmpty() ? "Locale" : scope.substring(0, 1).toUpperCase() + scope.substring(1);
        sortPill.setText(queryState.ascending ? "Order: " + filterLabel + " A-Z" : "Order: " + filterLabel + " Z-A");
    }

    private static void renderOnboardingTable(
        DashboardView view,
        List<String[]> baseRows,
        DashboardTableQueryEngine.QueryState queryState,
        VBox tableHost,
        Button sortPill,
        Node headerControls
    ) {
        String scopedTerm = queryState.searchTerm.trim().toLowerCase();
        String scope = queryState.filterKey;

        List<String[]> scopedRows = new ArrayList<>();
        for (String[] row : baseRows) {
            if (scopedTerm.isEmpty()) {
                scopedRows.add(row);
                continue;
            }

            boolean matches;
            matches = switch (scope) {
                case "step"      -> row[0] != null && row[0].toLowerCase().contains(scopedTerm);
                case "completed" -> row[1] != null && row[1].toLowerCase().contains(scopedTerm);
                case "locale"    -> row[2] != null && row[2].toLowerCase().contains(scopedTerm);
                case "theme"     -> row[3] != null && row[3].toLowerCase().contains(scopedTerm);
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
            getOnboardingFilterColumnIndex(scope)
        );

        String[][] visibleRows;
        if (result.pageRows.isEmpty()) {
            visibleRows = new String[][]{{"1", "No", "No onboarding found", "-", "-"}};
        } else {
            visibleRows = result.pageRows.toArray(new String[0][]);
        }

        tableHost.getChildren().setAll(view.dataTableWithCrud(
            "Onboarding Data",
            "Onboarding",
            new String[]{"Step", "Completed", "Locale", "Theme", "Updated"},
            visibleRows,
            false,
            headerControls
        ));

        styleQueryPill(view, sortPill, queryState.ascending);
        String filterLabel = scope.isEmpty() ? "Step" : scope.substring(0, 1).toUpperCase() + scope.substring(1);
        sortPill.setText(queryState.ascending ? "Order: " + filterLabel + " A-Z" : "Order: " + filterLabel + " Z-A");
    }
}
