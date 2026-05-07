package com.syndicati.models.log.data;

import com.syndicati.models.log.AppEventLog;
import com.syndicati.models.user.User;
import com.syndicati.models.user.data.UserRepository;
import com.syndicati.services.DatabaseService;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AppEventLogRepository {

    private final DatabaseService databaseService;
    private final UserRepository userRepository;

    public AppEventLogRepository() {
        this.databaseService = DatabaseService.getInstance();
        this.userRepository = new UserRepository();
    }

    public int create(AppEventLog log) {
        if (log == null || log.getEventType() == null || log.getEventType().isBlank()) {
            return -1;
        }

        String sql = """
            INSERT INTO app_event_log (
                event_id, user_id, session_id, request_id, trace_id, span_id,
                event_type, category, action, outcome, message, ip_address, user_agent,
                duration_ms, risk_score, anomaly_score,
                entity_type, entity_id, metadata, created_at, event_timestamp,
                level, service_name, environment, application_version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return -1;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                String eventId = log.getEventId();
                if (eventId == null || eventId.isBlank()) {
                    eventId = UUID.randomUUID().toString();
                    log.setEventId(eventId);
                }
                ps.setString(1, eventId);

                if (log.getUser() != null && log.getUser().getIdUser() != null) {
                    ps.setInt(2, log.getUser().getIdUser());
                } else {
                    ps.setNull(2, java.sql.Types.INTEGER);
                }
                setNullableString(ps, 3, log.getSessionId());
                setNullableString(ps, 4, log.getRequestId());
                setNullableString(ps, 5, log.getTraceId());
                setNullableString(ps, 6, log.getSpanId());

                ps.setString(7, log.getEventType());
                setNullableString(ps, 8, log.getCategory());
                setNullableString(ps, 9, log.getAction());
                setNullableString(ps, 10, log.getOutcome());
                setNullableString(ps, 11, log.getMessage());
                setNullableIp(ps, 12, log.getIpAddress());
                setNullableString(ps, 13, log.getUserAgent());

                if (log.getDurationMs() != null) {
                    ps.setInt(14, log.getDurationMs());
                } else {
                    ps.setNull(14, java.sql.Types.INTEGER);
                }
                if (log.getRiskScore() != null) {
                    ps.setBigDecimal(15, log.getRiskScore());
                } else {
                    ps.setNull(15, java.sql.Types.DECIMAL);
                }
                if (log.getAnomalyScore() != null) {
                    ps.setBigDecimal(16, log.getAnomalyScore());
                } else {
                    ps.setNull(16, java.sql.Types.DECIMAL);
                }

                ps.setString(17, log.getEntityType());
                if (log.getEntityId() != null) {
                    ps.setInt(18, log.getEntityId());
                } else {
                    ps.setNull(18, java.sql.Types.INTEGER);
                }
                ps.setString(19, log.getMetadataJson());
                LocalDateTime createdAt = log.getCreatedAt() != null ? log.getCreatedAt() : LocalDateTime.now();
                LocalDateTime eventTimestamp = log.getEventTimestamp() != null ? log.getEventTimestamp() : createdAt;
                ps.setTimestamp(20, Timestamp.valueOf(createdAt));
                ps.setTimestamp(21, Timestamp.valueOf(eventTimestamp));

                ps.setString(22, log.getLevel() == null || log.getLevel().isBlank() ? "INFO" : log.getLevel());
                setNullableString(ps, 23, log.getServiceName());
                setNullableString(ps, 24, log.getEnvironment());
                setNullableString(ps, 25, log.getApplicationVersion());

                int affected = ps.executeUpdate();
                if (affected == 0) {
                    return -1;
                }

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        log.setId((long) id);
                        log.setCreatedAt(createdAt);
                        log.setEventTimestamp(eventTimestamp);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.create error: " + e.getMessage());
        }

        return -1;
    }

    public List<AppEventLog> findLatest(int limit) {
        String sql = "SELECT * FROM app_event_log ORDER BY created_at DESC LIMIT ?";
        List<AppEventLog> logs = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return logs;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, Math.max(1, limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        logs.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.findLatest error: " + e.getMessage());
        }

        return logs;
    }

    public List<AppEventLog> findLatestByUser(int userId, int limit) {
        String sql = "SELECT * FROM app_event_log WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
        List<AppEventLog> logs = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return logs;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, Math.max(1, limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        logs.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.findLatestByUser error: " + e.getMessage());
        }

        return logs;
    }

    public List<AppEventLog> findLatestBySession(String sessionId, int limit) {
        String sql = "SELECT * FROM app_event_log WHERE session_id = ? ORDER BY created_at DESC LIMIT ?";
        List<AppEventLog> logs = new ArrayList<>();

        if (sessionId == null || sessionId.isBlank()) {
            return logs;
        }

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return logs;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, sessionId);
                ps.setInt(2, Math.max(1, limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        logs.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.findLatestBySession error: " + e.getMessage());
        }

        return logs;
    }

    public List<AppEventLog> findUnscoredEvents(int limit) {
        String sql = "SELECT * FROM app_event_log WHERE anomaly_score IS NULL ORDER BY created_at DESC LIMIT ?";
        List<AppEventLog> logs = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return logs;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, Math.max(1, limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        logs.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.findUnscoredEvents error: " + e.getMessage());
        }

        return logs;
    }

    public boolean updateAnomalyData(long eventId, BigDecimal anomalyScore, String metadataJson) {
        String sql = "UPDATE app_event_log SET anomaly_score = ?, metadata = ? WHERE id = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (anomalyScore != null) {
                    ps.setBigDecimal(1, anomalyScore);
                } else {
                    ps.setNull(1, java.sql.Types.DECIMAL);
                }
                ps.setString(2, metadataJson == null || metadataJson.isBlank() ? "{}" : metadataJson);
                ps.setLong(3, eventId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.updateAnomalyData error: " + e.getMessage());
            return false;
        }
    }

    public List<AppEventLog> findByEntityType(String entityType, int entityId) {
        String sql = "SELECT * FROM app_event_log WHERE entity_type = ? AND entity_id = ? ORDER BY created_at DESC";
        List<AppEventLog> logs = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return logs;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, entityType);
                ps.setInt(2, entityId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        logs.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.findByEntityType error: " + e.getMessage());
        }

        return logs;
    }

    public int countSince(LocalDateTime since) {
        String sql = "SELECT COUNT(*) FROM app_event_log WHERE created_at >= ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return 0;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.valueOf(since));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.countSince error: " + e.getMessage());
        }

        return 0;
    }

    public int countByEventTypeSince(String eventType, LocalDateTime since) {
        String sql = "SELECT COUNT(*) FROM app_event_log WHERE event_type = ? AND created_at >= ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return 0;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, eventType);
                ps.setTimestamp(2, Timestamp.valueOf(since));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.countByEventTypeSince error: " + e.getMessage());
        }

        return 0;
    }

    public int countDistinctActiveSince(LocalDateTime since) {
        String sql = """
            SELECT COUNT(DISTINCT COALESCE(CAST(user_id AS CHAR), JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.host'))))
            FROM app_event_log
            WHERE created_at >= ?
            """;

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return 0;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.valueOf(since));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.countDistinctActiveSince error: " + e.getMessage());
        }

        return 0;
    }

    public List<String[]> fetchInteractionTrends(LocalDateTime since) {
        String sql = """
            SELECT DATE(created_at) AS log_date,
                   SUM(CASE WHEN event_type = 'PAGE_VIEW' THEN 1 ELSE 0 END) AS views,
                   SUM(CASE WHEN event_type = 'UI_CLICK' THEN 1 ELSE 0 END) AS clicks
            FROM app_event_log
            WHERE created_at >= ?
            GROUP BY DATE(created_at)
            ORDER BY log_date ASC
            """;

        List<String[]> rows = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return rows;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.valueOf(since));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new String[]{
                            String.valueOf(rs.getDate("log_date")),
                            String.valueOf(rs.getInt("views")),
                            String.valueOf(rs.getInt("clicks"))
                        });
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.fetchInteractionTrends error: " + e.getMessage());
        }

        return rows;
    }

    public List<String[]> fetchTopPages(int limit) {
        String sql = """
            SELECT COALESCE(JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.route')), 'Direct/Unknown') AS route,
                   COUNT(*) AS visit_count
            FROM app_event_log
            WHERE event_type = 'PAGE_VIEW'
            GROUP BY route
            ORDER BY visit_count DESC
            LIMIT ?
            """;

        List<String[]> rows = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return rows;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, Math.max(1, limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new String[]{
                            rs.getString("route"),
                            String.valueOf(rs.getInt("visit_count"))
                        });
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.fetchTopPages error: " + e.getMessage());
        }

        return rows;
    }

    public List<String[]> fetchTopClicks(int limit) {
        String sql = """
            SELECT COALESCE(JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.text')), 'Unknown') AS element_text,
                   COALESCE(JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.target')), 'Unknown') AS target,
                   COUNT(*) AS click_count
            FROM app_event_log
            WHERE event_type = 'UI_CLICK'
            GROUP BY element_text, target
            ORDER BY click_count DESC
            LIMIT ?
            """;

        List<String[]> rows = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return rows;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, Math.max(1, limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new String[]{
                            rs.getString("element_text"),
                            rs.getString("target"),
                            String.valueOf(rs.getInt("click_count"))
                        });
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.fetchTopClicks error: " + e.getMessage());
        }

        return rows;
    }

    public List<String[]> fetchTopUsers(LocalDateTime since, int limit) {
        String sql = """
            SELECT u.first_name,
                   u.last_name,
                   u.role_user,
                   COUNT(l.id) AS activity_count
            FROM app_event_log l
            JOIN user u ON l.user_id = u.id_user
            WHERE l.created_at >= ?
            GROUP BY u.id_user, u.first_name, u.last_name, u.role_user
            ORDER BY activity_count DESC
            LIMIT ?
            """;

        List<String[]> rows = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return rows;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.valueOf(since));
                ps.setInt(2, Math.max(1, limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new String[]{
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("role_user"),
                            String.valueOf(rs.getInt("activity_count"))
                        });
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.fetchTopUsers error: " + e.getMessage());
        }

        return rows;
    }

    public List<String[]> fetchDeviceBreakdown(LocalDateTime since) {
        String sql = """
            SELECT COALESCE(user_agent, JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.user_agent'))) AS ua
            FROM app_event_log
            WHERE created_at >= ?
            """;

        int desktop = 0;
        int mobile = 0;
        int chrome = 0;
        int safari = 0;
        int firefox = 0;
        int edge = 0;
        int other = 0;
        int total = 0;

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return new ArrayList<>();
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.valueOf(since));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String ua = rs.getString("ua");
                        if (ua == null || ua.isBlank()) {
                            continue;
                        }

                        String lower = ua.toLowerCase();
                        total++;

                        if (lower.matches(".*(mobi|android|touch|mini).*")) {
                            mobile++;
                        } else {
                            desktop++;
                        }

                        if (lower.contains("edg/")) {
                            edge++;
                        } else if (lower.contains("chrome") || lower.contains("crios")) {
                            chrome++;
                        } else if (lower.contains("firefox") || lower.contains("fxios")) {
                            firefox++;
                        } else if (lower.contains("safari") && !lower.contains("chrome")) {
                            safari++;
                        } else {
                            other++;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.fetchDeviceBreakdown error: " + e.getMessage());
        }

        int safeTotal = Math.max(1, total);
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"desktop", String.valueOf(Math.round((desktop / (double) safeTotal) * 100))});
        rows.add(new String[]{"mobile", String.valueOf(Math.round((mobile / (double) safeTotal) * 100))});
        rows.add(new String[]{"Chrome", String.valueOf(Math.round((chrome / (double) safeTotal) * 100))});
        rows.add(new String[]{"Safari", String.valueOf(Math.round((safari / (double) safeTotal) * 100))});
        rows.add(new String[]{"Firefox", String.valueOf(Math.round((firefox / (double) safeTotal) * 100))});
        rows.add(new String[]{"Edge", String.valueOf(Math.round((edge / (double) safeTotal) * 100))});
        rows.add(new String[]{"Other", String.valueOf(Math.round((other / (double) safeTotal) * 100))});
        return rows;
    }

    public List<String[]> fetchOutcomeBreakdown(LocalDateTime since) {
        String sql = """
            SELECT COALESCE(outcome, 'UNKNOWN') AS outcome, COUNT(*) AS cnt
            FROM app_event_log
            WHERE created_at >= ?
            GROUP BY COALESCE(outcome, 'UNKNOWN')
            ORDER BY cnt DESC
            """;
        return fetchLabelCountRows(sql, since);
    }

    public List<String[]> fetchLevelBreakdown(LocalDateTime since) {
        String sql = """
            SELECT COALESCE(level, 'INFO') AS level, COUNT(*) AS cnt
            FROM app_event_log
            WHERE created_at >= ?
            GROUP BY COALESCE(level, 'INFO')
            ORDER BY cnt DESC
            """;
        return fetchLabelCountRows(sql, since);
    }

    public List<String[]> fetchRiskSignals(int limit) {
        String sql = """
            SELECT event_type,
                   COALESCE(outcome, 'UNKNOWN') AS outcome,
                   COALESCE(risk_score, 0) AS risk_score,
                   COALESCE(anomaly_score, 0) AS anomaly_score,
                   COALESCE(duration_ms, 0) AS duration_ms,
                   COALESCE(level, 'INFO') AS level,
                   COALESCE(service_name, '-') AS service_name,
                   COALESCE(environment, '-') AS environment,
                   created_at
            FROM app_event_log
            ORDER BY COALESCE(risk_score, 0) DESC, COALESCE(anomaly_score, 0) DESC, created_at DESC
            LIMIT ?
            """;

        List<String[]> rows = new ArrayList<>();
        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return rows;
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, Math.max(1, limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new String[]{
                            rs.getString("event_type"),
                            rs.getString("outcome"),
                            rs.getString("risk_score"),
                            rs.getString("anomaly_score"),
                            rs.getString("duration_ms"),
                            rs.getString("level"),
                            rs.getString("service_name"),
                            rs.getString("environment"),
                            String.valueOf(rs.getTimestamp("created_at"))
                        });
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.fetchRiskSignals error: " + e.getMessage());
        }
        return rows;
    }

    public List<AppEventLog> findRecentAnomalies(double minAnomalyScore, int limit) {
        String sql = "SELECT * FROM app_event_log WHERE COALESCE(anomaly_score, 0) >= ? ORDER BY created_at DESC LIMIT ?";
        List<AppEventLog> logs = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return logs;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBigDecimal(1, BigDecimal.valueOf(Math.max(0.0, minAnomalyScore)));
                ps.setInt(2, Math.max(1, limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        logs.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.findRecentAnomalies error: " + e.getMessage());
        }

        return logs;
    }

    public List<String[]> fetchSuspiciousUsers(LocalDateTime since, int minFailures, int limit) {
        String sql = """
            SELECT l.user_id,
                   MAX(u.email_user) AS email,
                   COUNT(*) AS failure_count,
                   MAX(l.created_at) AS last_seen
            FROM app_event_log l
            LEFT JOIN user u ON l.user_id = u.id_user
            WHERE l.created_at >= ?
              AND l.user_id IS NOT NULL
              AND (l.event_type = 'AUTH_FAILURE' OR COALESCE(l.outcome, '') = 'FAILURE')
            GROUP BY l.user_id
            HAVING COUNT(*) >= ?
            ORDER BY failure_count DESC, last_seen DESC
            LIMIT ?
            """;

        List<String[]> rows = new ArrayList<>();
        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return rows;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.valueOf(since));
                ps.setInt(2, Math.max(1, minFailures));
                ps.setInt(3, Math.max(1, limit));

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new String[]{
                            String.valueOf(rs.getInt("user_id")),
                            rs.getString("email"),
                            String.valueOf(rs.getInt("failure_count")),
                            String.valueOf(rs.getTimestamp("last_seen"))
                        });
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.fetchSuspiciousUsers error: " + e.getMessage());
        }

        return rows;
    }

    public List<String[]> fetchFeatureUsage(int limit) {
        String sql = """
            SELECT COALESCE(action, event_type, 'UNKNOWN') AS feature_key,
                   COUNT(*) AS usage_count
            FROM app_event_log
            GROUP BY COALESCE(action, event_type, 'UNKNOWN')
            ORDER BY usage_count DESC
            LIMIT ?
            """;

        List<String[]> rows = new ArrayList<>();
        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return rows;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, Math.max(1, limit));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new String[]{
                            rs.getString("feature_key"),
                            String.valueOf(rs.getInt("usage_count"))
                        });
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.fetchFeatureUsage error: " + e.getMessage());
        }

        return rows;
    }

    private List<String[]> fetchLabelCountRows(String sql, LocalDateTime since) {
        List<String[]> rows = new ArrayList<>();
        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return rows;
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.valueOf(since));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new String[]{
                            rs.getString(1),
                            String.valueOf(rs.getInt(2))
                        });
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("AppEventLogRepository.fetchLabelCountRows error: " + e.getMessage());
        }
        return rows;
    }

    private void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            ps.setNull(index, java.sql.Types.VARCHAR);
            return;
        }
        ps.setString(index, value);
    }

    private void setNullableIp(PreparedStatement ps, int index, String ipAddress) throws SQLException {
        if (ipAddress == null || ipAddress.isBlank()) {
            ps.setNull(index, java.sql.Types.VARBINARY);
            return;
        }
        try {
            ps.setBytes(index, InetAddress.getByName(ipAddress).getAddress());
        } catch (UnknownHostException e) {
            ps.setNull(index, java.sql.Types.VARBINARY);
        }
    }

    private AppEventLog mapRow(ResultSet rs) throws SQLException {
        AppEventLog log = new AppEventLog();
        log.setId(rs.getLong("id"));
        log.setEventId(rs.getString("event_id"));
        log.setSessionId(rs.getString("session_id"));
        log.setRequestId(rs.getString("request_id"));
        log.setTraceId(rs.getString("trace_id"));
        log.setSpanId(rs.getString("span_id"));
        log.setEventType(rs.getString("event_type"));
        log.setCategory(rs.getString("category"));
        log.setAction(rs.getString("action"));
        log.setOutcome(rs.getString("outcome"));
        log.setMessage(rs.getString("message"));
        log.setIpAddress(ipToString(rs.getBytes("ip_address")));
        log.setUserAgent(rs.getString("user_agent"));

        int duration = rs.getInt("duration_ms");
        if (!rs.wasNull()) {
            log.setDurationMs(duration);
        }

        log.setRiskScore(rs.getBigDecimal("risk_score"));
        log.setAnomalyScore(rs.getBigDecimal("anomaly_score"));
        log.setEntityType(rs.getString("entity_type"));

        int entityId = rs.getInt("entity_id");
        if (!rs.wasNull()) {
            log.setEntityId(entityId);
        }

        String metadata = rs.getString("metadata");
        log.setMetadataJson(metadata);

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            log.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp eventTimestamp = rs.getTimestamp("event_timestamp");
        if (eventTimestamp != null) {
            log.setEventTimestamp(eventTimestamp.toLocalDateTime());
        }
        log.setLevel(rs.getString("level"));
        log.setServiceName(rs.getString("service_name"));
        log.setEnvironment(rs.getString("environment"));
        log.setApplicationVersion(rs.getString("application_version"));

        int userId = rs.getInt("user_id");
        if (!rs.wasNull()) {
            Optional<User> user = userRepository.findById(userId);
            user.ifPresent(log::setUser);
        }

        return log;
    }

    private String ipToString(byte[] raw) {
        if (raw == null || raw.length == 0) {
            return null;
        }
        try {
            return InetAddress.getByAddress(raw).getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }
}