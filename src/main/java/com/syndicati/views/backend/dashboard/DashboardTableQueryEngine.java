package com.syndicati.views.backend.dashboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class DashboardTableQueryEngine {

    private DashboardTableQueryEngine() {
    }

    static final class QueryState {
        String searchTerm = "";
        String filterKey = "all";
        boolean ascending = true;
        int page = 1;
        final int pageSize;

        QueryState(int pageSize) {
            this.pageSize = Math.max(1, pageSize);
        }
    }

    interface RowFilter {
        boolean test(String[] row, String filterKey);
    }

    static final class QueryResult {
        final List<String[]> pageRows;
        final int page;
        final int totalPages;
        final int totalRows;

        QueryResult(List<String[]> pageRows, int page, int totalPages, int totalRows) {
            this.pageRows = pageRows;
            this.page = page;
            this.totalPages = totalPages;
            this.totalRows = totalRows;
        }
    }

    static QueryResult apply(List<String[]> rows, QueryState state, RowFilter filter, int sortColumn) {
        List<String[]> searched = searchRows(rows, state.searchTerm);
        List<String[]> filtered = filterRows(searched, state.filterKey, filter);
        List<String[]> sorted = sortRows(filtered, sortColumn, state.ascending);

        int totalRows = sorted.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalRows / (double) state.pageSize));
        int page = Math.max(1, Math.min(state.page, totalPages));
        state.page = page;

        int fromIndex = (page - 1) * state.pageSize;
        int toIndex = Math.min(fromIndex + state.pageSize, totalRows);
        List<String[]> pageRows = fromIndex >= toIndex
            ? new ArrayList<>()
            : new ArrayList<>(sorted.subList(fromIndex, toIndex));

        return new QueryResult(pageRows, page, totalPages, totalRows);
    }

    private static List<String[]> searchRows(List<String[]> rows, String term) {
        String normalized = term == null ? "" : term.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return new ArrayList<>(rows);
        }

        List<String[]> out = new ArrayList<>();
        for (String[] row : rows) {
            if (matchesSearch(row, normalized)) {
                out.add(row);
            }
        }
        return out;
    }

    private static boolean matchesSearch(String[] row, String normalizedTerm) {
        for (String cell : row) {
            if (cell != null && cell.toLowerCase().contains(normalizedTerm)) {
                return true;
            }
        }
        return false;
    }

    private static List<String[]> filterRows(List<String[]> rows, String filterKey, RowFilter filter) {
        if (filter == null) {
            return new ArrayList<>(rows);
        }

        List<String[]> out = new ArrayList<>();
        for (String[] row : rows) {
            if (filter.test(row, filterKey)) {
                out.add(row);
            }
        }
        return out;
    }

    private static List<String[]> sortRows(List<String[]> rows, int sortColumn, boolean ascending) {
        List<String[]> out = new ArrayList<>(rows);
        Comparator<String[]> comparator = Comparator.comparing(
            row -> safeCell(row, sortColumn),
            String.CASE_INSENSITIVE_ORDER
        );

        out.sort(ascending ? comparator : comparator.reversed());
        return out;
    }

    private static String safeCell(String[] row, int index) {
        if (row == null || index < 0 || index >= row.length || row[index] == null) {
            return "";
        }
        return row[index];
    }
}
