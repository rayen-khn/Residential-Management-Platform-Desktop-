package com.syndicati.views.backend.dashboard;

import com.syndicati.models.log.AppEventLog;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

final class DashboardActivitySection {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd HH:mm");

    private DashboardActivitySection() {
    }

    static VBox build(DashboardView view) {
        return view.moduleModeView(
            "Activity Log",
            "\u23F1",
            new String[]{"Overview", "Timeline", "Signals"},
            key -> {
                if ("Timeline".equals(key)) {
                    return timelinePane(view);
                }
                if ("Signals".equals(key)) {
                    return signalsPane(view);
                }
                return overviewPane(view);
            }
        );
    }

    private static VBox overviewPane(DashboardView view) {
        VBox wrap = new VBox(16);
        wrap.setFillWidth(true);

        Map<String, Integer> stats = activityStats(view);
        HBox cards = new HBox(16);
        view.addStatCards(cards,
            new String[]{"USR", "ACT", "CLK", "WKY"},
            new String[]{"Active Today", "Interactions Today", "Page Views", "Weekly Activity"},
            new String[]{
                String.valueOf(stats.getOrDefault("active_today", 0)),
                String.valueOf(stats.getOrDefault("interactions_today", 0)),
                String.valueOf(stats.getOrDefault("page_views", 0)),
                String.valueOf(stats.getOrDefault("active_week", 0))
            },
            new String[]{"#60a5fa", "#34d399", "#a78bfa", "#fbbf24"}
        );

        HBox grid = new HBox(16);
        VBox chart = activityChart(view);
        HBox.setHgrow(chart, Priority.ALWAYS);
        VBox topUsers = topUsersPane(view);
        topUsers.setPrefWidth(300);
        topUsers.setMinWidth(300);
        topUsers.setMaxWidth(300);
        grid.getChildren().addAll(chart, topUsers);

        wrap.getChildren().addAll(cards, grid);
        return wrap;
    }

    private static VBox timelinePane(DashboardView view) {
        List<AppEventLog> logs = view.dashboardAdminService().recentActivityLogs(200);
        List<String[]> rows = new ArrayList<>();

        for (AppEventLog log : logs) {
            String subject = log.getEntityType() == null ? "-" : log.getEntityType();
            String event = log.getEventType() == null ? "-" : log.getEventType();
            String level = log.getLevel() == null ? "INFO" : log.getLevel();
            String outcome = log.getOutcome() == null ? "UNKNOWN" : log.getOutcome();
            String actor = log.getUser() == null ? "Anonymous" : view.safe(log.getUser().getFirstName()) + " " + view.safe(log.getUser().getLastName());
            String metadata = shortMetadata(log.getMetadataJson());
            String createdAt = log.getEventTimestamp() == null
                ? (log.getCreatedAt() == null ? "-" : log.getCreatedAt().format(DATE_FMT))
                : log.getEventTimestamp().format(DATE_FMT);
            String trace = blankOrDash(log.getTraceId());
            String session = blankOrDash(log.getSessionId());
            String risk = log.getRiskScore() == null ? "-" : log.getRiskScore().toPlainString();
            String duration = log.getDurationMs() == null ? "-" : log.getDurationMs() + " ms";
            rows.add(new String[]{event, level, outcome, subject, actor.trim(), trace, session, risk, duration, metadata, createdAt});
        }

        DashboardTableQueryEngine.QueryState state = new DashboardTableQueryEngine.QueryState(12);
        TextField searchField = dashboardSearchField(view, "Search logs by event, entity, user or metadata...");
        Button sortPill = view.pillAction("Order: A-Z", false);
        HBox filterRow = new HBox(6);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        HBox headerControls = dashboardHeaderControls(searchField, sortPill, filterRow);
        VBox tableHost = new VBox();

        String[][] filters = new String[][]{
            {"PAGE_VIEW", "Page View"},
            {"UI_CLICK", "UI Click"},
            {"FAILURE", "Failure"},
            {"ERROR", "Error"},
            {"all", "All"}
        };

        for (String[] filter : filters) {
            String key = filter[0];
            Button button = view.pillAction(filter[1], false);
            button.setOnAction(e -> {
                state.filterKey = key.equals(state.filterKey) ? "all" : key;
                state.page = 1;
                renderActivityTable(view, rows, state, tableHost, sortPill, headerControls, filterRow);
            });
            filterRow.getChildren().add(button);
        }

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            state.searchTerm = newVal == null ? "" : newVal;
            state.page = 1;
            renderActivityTable(view, rows, state, tableHost, sortPill, headerControls, filterRow);
        });

        sortPill.setOnAction(e -> {
            state.ascending = !state.ascending;
            renderActivityTable(view, rows, state, tableHost, sortPill, headerControls, filterRow);
        });

        renderActivityTable(view, rows, state, tableHost, sortPill, headerControls, filterRow);

        VBox wrap = new VBox(14, headerControls, tableHost);
        wrap.setFillWidth(true);
        return wrap;
    }

    private static VBox signalsPane(DashboardView view) {
        VBox wrap = new VBox(16);

        HBox rows = new HBox(16);
        VBox pagesCard = signalCard(view, "Top Pages");
        VBox clicksCard = signalCard(view, "Top Clicks");
        VBox devicesCard = signalCard(view, "Device Breakdown");

        fillRankRows(view, pagesCard, view.dashboardAdminService().topPages(), new String[]{"route", "count"});
        fillClickRows(view, clicksCard, view.dashboardAdminService().topClicks());
        fillDeviceRows(view, devicesCard, view.dashboardAdminService().deviceBreakdown());

        rows.getChildren().addAll(pagesCard, clicksCard, devicesCard);
        HBox.setHgrow(pagesCard, Priority.ALWAYS);
        HBox.setHgrow(clicksCard, Priority.ALWAYS);
        HBox.setHgrow(devicesCard, Priority.ALWAYS);

        HBox advanced = new HBox(16);
        VBox outcomesCard = signalCard(view, "Outcomes (30d)");
        VBox levelsCard = signalCard(view, "Levels (30d)");
        VBox riskCard = signalCard(view, "Risk Signals");
        fillRankRows(view, outcomesCard, view.dashboardAdminService().outcomeBreakdown(), new String[]{"outcome", "count"});
        fillRankRows(view, levelsCard, view.dashboardAdminService().levelBreakdown(), new String[]{"level", "count"});
        fillRiskRows(view, riskCard, view.dashboardAdminService().riskSignals(5));
        advanced.getChildren().addAll(outcomesCard, levelsCard, riskCard);
        HBox.setHgrow(outcomesCard, Priority.ALWAYS);
        HBox.setHgrow(levelsCard, Priority.ALWAYS);
        HBox.setHgrow(riskCard, Priority.ALWAYS);

        wrap.getChildren().addAll(rows, advanced);
        return wrap;
    }

    private static VBox activityChart(DashboardView view) {
        VBox card = view.sectionCard();
        Text title = view.t("Activity Pulse", view.boldFont(), FontWeight.BOLD, 15);
        title.setFill(view.textPrimaryColor());
        Text sub = view.t("Page views and UI clicks - last 7 days", view.lightFont(), FontWeight.NORMAL, 13);
        sub.setFill(view.textMutedColor());

        HBox chartRow = new HBox(8);
        chartRow.setAlignment(Pos.BOTTOM_LEFT);
        chartRow.setPrefHeight(140);
        chartRow.setPadding(new Insets(8, 0, 0, 0));

        List<String[]> trends = view.dashboardAdminService().interactionTrends();
        if (trends.isEmpty()) {
            trends = defaultTrends();
        }

        for (String[] trend : trends) {
            VBox col = new VBox(4);
            col.setAlignment(Pos.BOTTOM_CENTER);
            HBox.setHgrow(col, Priority.ALWAYS);

            int views = parseInt(trend, 1);
            int clicks = parseInt(trend, 2);
            HBox pair = new HBox(3, view.barR(Math.max(8, views * 2), "#a78bfa"), view.barR(Math.max(8, clicks * 2), "#34d399"));
            pair.setAlignment(Pos.BOTTOM_CENTER);
            Text label = view.t(trend[0], view.lightFont(), FontWeight.NORMAL, 11);
            label.setFill(view.textMutedColor());
            col.getChildren().addAll(pair, label);
            chartRow.getChildren().add(col);
        }

        HBox footer = new HBox(20);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 0, 0, 0));
        footer.setStyle("-fx-border-color:rgba(255,255,255,0.06);-fx-border-width:1 0 0 0;");
        footer.getChildren().addAll(
            metric(view, "P", "Top Pages", pageCount(view), "#a78bfa"),
            metric(view, "C", "Top Clicks", clickCount(view), "#34d399"),
            metric(view, "U", "Top Users", userCount(view), "#60a5fa")
        );

        card.getChildren().addAll(title, sub, chartRow, footer);
        return card;
    }

    private static VBox topUsersPane(DashboardView view) {
        VBox card = view.sectionCard();
        Text title = view.t("Top Active Users", view.boldFont(), FontWeight.BOLD, 14);
        title.setFill(view.textPrimaryColor());
        VBox list = new VBox(6);
        for (String[] user : view.dashboardAdminService().topUsers()) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 8, 6, 8));
            row.setStyle("-fx-background-color:rgba(255,255,255,0.025);-fx-background-radius:8;");
            Text ini = view.t(initial(user[0]), view.boldFont(), FontWeight.BOLD, 14);
            ini.setFill(javafx.scene.paint.Color.web("#fbbf24"));
            VBox info = new VBox(1);
            HBox.setHgrow(info, Priority.ALWAYS);
            Text nm = view.t((user[0] + " " + user[1]).trim(), view.lightFont(), FontWeight.NORMAL, 14);
            nm.setFill(view.textSecondaryColor());
            Text rl = view.t(user[2], view.lightFont(), FontWeight.NORMAL, 12);
            rl.setFill(javafx.scene.paint.Color.web("#fbbf24"));
            info.getChildren().addAll(nm, rl);
            Text pts = view.t(user[3], view.boldFont(), FontWeight.BOLD, 13);
            pts.setFill(view.textPrimaryColor());
            row.getChildren().addAll(new javafx.scene.layout.StackPane(ini), info, pts);
            list.getChildren().add(row);
        }
        card.getChildren().addAll(title, list);
        return card;
    }

    private static void renderActivityTable(
        DashboardView view,
        List<String[]> rows,
        DashboardTableQueryEngine.QueryState state,
        VBox tableHost,
        Button sortPill,
        HBox headerControls,
        HBox filterRow
    ) {
        DashboardTableQueryEngine.QueryResult result = DashboardTableQueryEngine.apply(rows, state, (row, filterKey) -> {
            if (filterKey == null || filterKey.isBlank() || "all".equalsIgnoreCase(filterKey)) {
                return true;
            }
            return matchesTimelineFilter(row, filterKey);
        }, 10);

        VBox table = view.sectionCard();
        table.setFillWidth(true);

        HBox head = new HBox(10);
        head.setAlignment(Pos.CENTER_LEFT);
        Text title = view.t("Recent Activity", view.boldFont(), FontWeight.BOLD, 18);
        title.setFill(view.textPrimaryColor());
        Text sub = view.t(result.totalRows + " records", view.lightFont(), FontWeight.NORMAL, 13);
        sub.setFill(view.textMutedColor());
        VBox titleWrap = new VBox(2, title, sub);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        head.getChildren().addAll(titleWrap, spacer);
        if (headerControls != null) {
            head.getChildren().add(headerControls);
        }

        List<Node> rowsNodes = new ArrayList<>();
        for (String[] row : result.pageRows) {
            HBox line = new HBox(12);
            line.setAlignment(Pos.CENTER_LEFT);
            line.setPadding(new Insets(10, 12, 10, 12));
            line.setStyle("-fx-background-color:rgba(255,255,255,0.02);-fx-background-radius:12px;");
            VBox details = new VBox(2,
                textCell(view, row[3] + " • " + row[4]),
                metaCell(view, "trace=" + compactId(row[5]) + " • sess=" + compactId(row[6]) + " • risk=" + row[7] + " • " + row[8])
            );
            HBox.setHgrow(details, Priority.ALWAYS);
            line.getChildren().addAll(
                pill(view, row[0], "#a78bfa"),
                pill(view, row[1], levelColor(row[1])),
                pill(view, row[2], outcomeColor(row[2])),
                details,
                metaCell(view, row[9]),
                timeCell(view, row[10])
            );
            rowsNodes.add(line);
        }

        VBox list = new VBox(8);
        if (rowsNodes.isEmpty()) {
            list.getChildren().add(view.buildEmptyState("No activity logs found"));
        } else {
            list.getChildren().addAll(rowsNodes);
        }

        table.getChildren().addAll(head, list, pagerRow(view, state, result, rows, tableHost, sortPill, headerControls, filterRow));
        tableHost.getChildren().setAll(table);
    }

    private static HBox pagerRow(
        DashboardView view,
        DashboardTableQueryEngine.QueryState state,
        DashboardTableQueryEngine.QueryResult result,
        List<String[]> rows,
        VBox tableHost,
        Button sortPill,
        HBox headerControls,
        HBox filterRow
    ) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 0, 0, 0));
        Text pageLabel = view.t("Page " + result.page + " / " + result.totalPages + " | " + result.totalRows + " records", view.lightFont(), FontWeight.NORMAL, 12);
        pageLabel.setFill(view.textMutedColor());
        Button prev = view.pillAction("<", false);
        Button next = view.pillAction(">", false);
        prev.setDisable(result.page <= 1);
        next.setDisable(result.page >= result.totalPages);
        prev.setOnAction(e -> {
            state.page = Math.max(1, state.page - 1);
            renderActivityTable(view, rows, state, tableHost, sortPill, headerControls, filterRow);
        });
        next.setOnAction(e -> {
            state.page = Math.min(result.totalPages, state.page + 1);
            renderActivityTable(view, rows, state, tableHost, sortPill, headerControls, filterRow);
        });
        row.getChildren().addAll(prev, next, pageLabel);
        return row;
    }

    private static HBox dashboardHeaderControls(TextField searchField, Button sortPill, HBox filterRow) {
        HBox headerControls = new HBox(8, searchField, sortPill, filterRow);
        headerControls.setAlignment(Pos.CENTER_LEFT);
        return headerControls;
    }

    private static TextField dashboardSearchField(DashboardView view, String prompt) {
        TextField searchField = new TextField();
        searchField.setPromptText(prompt);
        searchField.setPrefWidth(320);
        searchField.setStyle("-fx-background-color:" + (view.isDark() ? "rgba(255,255,255,0.05)" : "rgba(15,23,42,0.05)") + ";-fx-background-radius:10px;-fx-border-color:" + (view.isDark() ? "rgba(255,255,255,0.14)" : "rgba(15,23,42,0.14)") + ";-fx-border-radius:10px;-fx-text-fill:" + (view.isDark() ? "#ffffff" : "#111827") + ";");
        return searchField;
    }

    private static VBox signalCard(DashboardView view, String title) {
        VBox card = view.sectionCard();
        card.setPrefWidth(300);
        card.setSpacing(10);
        Text heading = view.t(title, view.boldFont(), FontWeight.BOLD, 15);
        heading.setFill(view.textPrimaryColor());
        card.getChildren().add(heading);
        return card;
    }

    private static void fillRankRows(DashboardView view, VBox card, List<String[]> rows, String[] labels) {
        for (String[] row : rows) {
            HBox line = new HBox(10);
            line.setAlignment(Pos.CENTER_LEFT);
            line.getChildren().addAll(textCell(view, row[0]), new Region(), textCell(view, row[row.length - 1]));
            HBox.setHgrow(line.getChildren().get(1), Priority.ALWAYS);
            card.getChildren().add(line);
        }
    }

    private static void fillClickRows(DashboardView view, VBox card, List<String[]> rows) {
        for (String[] row : rows) {
            VBox box = new VBox(2);
            box.getChildren().addAll(textCell(view, row[0] + " • " + row[1]), metaCell(view, row[2] + " clicks"));
            card.getChildren().add(box);
        }
    }

    private static void fillDeviceRows(DashboardView view, VBox card, List<String[]> rows) {
        for (String[] row : rows) {
            HBox line = new HBox(10);
            line.setAlignment(Pos.CENTER_LEFT);
            line.getChildren().addAll(textCell(view, row[0]), new Region(), textCell(view, row[1] + "%"));
            HBox.setHgrow(line.getChildren().get(1), Priority.ALWAYS);
            card.getChildren().add(line);
        }
    }

    private static void fillRiskRows(DashboardView view, VBox card, List<String[]> rows) {
        for (String[] row : rows) {
            String title = row[0] + " • " + row[1] + " • " + row[5];
            String detail = "risk " + row[2] + " | anomaly " + row[3] + " | " + row[4] + " ms | " + row[6] + "/" + row[7];
            card.getChildren().add(new VBox(2, textCell(view, title), metaCell(view, detail)));
        }
    }

    private static VBox buildSimpleCard(DashboardView view, String title, List<String[]> rows) {
        VBox card = view.sectionCard();
        Text heading = view.t(title, view.boldFont(), FontWeight.BOLD, 15);
        heading.setFill(view.textPrimaryColor());
        card.getChildren().add(heading);
        for (String[] row : rows) {
            HBox line = new HBox(10);
            line.setAlignment(Pos.CENTER_LEFT);
            line.getChildren().addAll(textCell(view, row[0]), new Region(), textCell(view, row[row.length - 1]));
            HBox.setHgrow(line.getChildren().get(1), Priority.ALWAYS);
            card.getChildren().add(line);
        }
        return card;
    }

    private static HBox metric(DashboardView view, String icon, String label, String value, String color) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        Text ic = view.t(icon, view.boldFont(), FontWeight.BOLD, 13);
        ic.setFill(javafx.scene.paint.Color.web(color));
        Text lb = view.t(label, view.lightFont(), FontWeight.NORMAL, 13);
        lb.setFill(view.textMutedColor());
        Text vl = view.t(value, view.boldFont(), FontWeight.BOLD, 11);
        vl.setFill(javafx.scene.paint.Color.web(color));
        row.getChildren().addAll(ic, lb, vl);
        return row;
    }

    private static HBox textCell(DashboardView view, String text) {
        Text tx = view.t(text == null ? "-" : text, view.lightFont(), FontWeight.NORMAL, 13);
        tx.setFill(view.textSecondaryColor());
        return new HBox(tx);
    }

    private static HBox metaCell(DashboardView view, String text) {
        Text tx = view.t(text == null ? "-" : text, view.lightFont(), FontWeight.NORMAL, 12);
        tx.setFill(view.textMutedColor());
        return new HBox(tx);
    }

    private static HBox timeCell(DashboardView view, String text) {
        Text tx = view.t(text == null ? "-" : text, view.lightFont(), FontWeight.NORMAL, 12);
        tx.setFill(view.textMutedColor());
        return new HBox(tx);
    }

    private static HBox pill(DashboardView view, String text, String color) {
        HBox box = new HBox(view.t(text == null ? "-" : text, view.boldFont(), FontWeight.BOLD, 11));
        box.setPadding(new Insets(4, 10, 4, 10));
        box.setStyle("-fx-background-color:" + color + "22;-fx-background-radius:999px;");
        return box;
    }

    private static String shortMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return "-";
        }
        String normalized = metadata.replace('{', ' ').replace('}', ' ').trim();
        if (normalized.length() > 42) {
            return normalized.substring(0, 42) + "...";
        }
        return normalized;
    }

    private static String blankOrDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    private static boolean matchesTimelineFilter(String[] row, String filterKey) {
        if (row == null || row.length < 3) {
            return false;
        }
        String normalized = filterKey == null ? "" : filterKey.trim().toUpperCase();
        if (normalized.isBlank() || "ALL".equals(normalized)) {
            return true;
        }
        return normalized.equalsIgnoreCase(blankOrDash(row[0]))
            || normalized.equalsIgnoreCase(blankOrDash(row[1]))
            || normalized.equalsIgnoreCase(blankOrDash(row[2]));
    }

    private static String compactId(String value) {
        if (value == null || value.isBlank() || "-".equals(value)) {
            return "-";
        }
        if (value.length() <= 10) {
            return value;
        }
        return value.substring(0, 6) + "..." + value.substring(value.length() - 4);
    }

    private static String levelColor(String level) {
        String normalized = level == null ? "INFO" : level.toUpperCase();
        if ("ERROR".equals(normalized)) {
            return "#ef4444";
        }
        if ("WARN".equals(normalized) || "WARNING".equals(normalized)) {
            return "#f59e0b";
        }
        if ("DEBUG".equals(normalized) || "TRACE".equals(normalized)) {
            return "#60a5fa";
        }
        return "#34d399";
    }

    private static String outcomeColor(String outcome) {
        String normalized = outcome == null ? "UNKNOWN" : outcome.toUpperCase();
        if ("FAILURE".equals(normalized) || "FAILED".equals(normalized) || "ERROR".equals(normalized)) {
            return "#ef4444";
        }
        if ("WARNING".equals(normalized) || "PARTIAL".equals(normalized)) {
            return "#f59e0b";
        }
        if ("SUCCESS".equals(normalized) || "OK".equals(normalized)) {
            return "#34d399";
        }
        return "#a78bfa";
    }

    private static String initial(String value) {
        if (value == null || value.isBlank()) {
            return "U";
        }
        return value.substring(0, 1).toUpperCase();
    }

    private static int parseInt(String[] row, int index) {
        if (row == null || index < 0 || index >= row.length) {
            return 0;
        }
        try {
            return Integer.parseInt(row[index]);
        } catch (Exception e) {
            return 0;
        }
    }

    private static List<String[]> defaultTrends() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"MON", "5", "2"});
        rows.add(new String[]{"TUE", "7", "3"});
        rows.add(new String[]{"WED", "6", "4"});
        rows.add(new String[]{"THU", "9", "5"});
        rows.add(new String[]{"FRI", "8", "4"});
        rows.add(new String[]{"SAT", "4", "2"});
        rows.add(new String[]{"SUN", "6", "3"});
        return rows;
    }

    private static String pageCount(DashboardView view) {
        return String.valueOf(view.dashboardAdminService().topPages().size());
    }

    private static String clickCount(DashboardView view) {
        return String.valueOf(view.dashboardAdminService().topClicks().size());
    }

    private static String userCount(DashboardView view) {
        return String.valueOf(view.dashboardAdminService().topUsers().size());
    }

    private static Map<String, Integer> activityStats(DashboardView view) {
        return new HashMap<>(view.dashboardAdminService().activityHeartbeat());
    }
}