package com.syndicati.models.user.data;

import com.syndicati.models.user.Onboarding;
import com.syndicati.services.DatabaseService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC onboarding repository aligned with web OnboardingRepository behavior.
 */
public class OnboardingRepository {

    private final DatabaseService databaseService;

    public OnboardingRepository() {
        this.databaseService = DatabaseService.getInstance();
    }

    public List<Onboarding> findAllByIdDesc() {
        String sql = "SELECT * FROM onboarding ORDER BY id_onboarding DESC";
        List<Onboarding> items = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return items;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("OnboardingRepository.findAllByIdDesc error: " + e.getMessage());
        }

        return items;
    }

    public Optional<Onboarding> findById(int idOnboarding) {
        String sql = "SELECT * FROM onboarding WHERE id_onboarding = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return Optional.empty();
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idOnboarding);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("OnboardingRepository.findById error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<Onboarding> findOneByUserId(int userId) {
        String sql = "SELECT * FROM onboarding WHERE user_id = ? ORDER BY id_onboarding DESC LIMIT 1";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return Optional.empty();
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("OnboardingRepository.findOneByUserId error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<Integer> create(Onboarding onboarding) {
        String sql = "INSERT INTO onboarding (user_id, step, completed, selected_locale, selected_theme, selected_preferences, suggestions, started_at, completed_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return Optional.empty();
            }

            try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, onboarding.getUserId());
                ps.setInt(2, onboarding.getStep());
                ps.setBoolean(3, onboarding.isCompleted());
                ps.setString(4, onboarding.getSelectedLocale());
                ps.setString(5, onboarding.getSelectedTheme());
                ps.setString(6, onboarding.getSelectedPreferencesJson());
                ps.setString(7, onboarding.getSuggestions());
                ps.setTimestamp(8, toTimestamp(onboarding.getStartedAt()));
                ps.setTimestamp(9, toTimestamp(onboarding.getCompletedAt()));
                ps.setTimestamp(10, toTimestamp(onboarding.getUpdatedAt()));

                int affected = ps.executeUpdate();
                if (affected <= 0) {
                    return Optional.empty();
                }

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return Optional.of(keys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("OnboardingRepository.create error: " + e.getMessage());
        }

        return Optional.empty();
    }

    public boolean update(Onboarding onboarding) {
        String sql = "UPDATE onboarding SET user_id = ?, step = ?, completed = ?, selected_locale = ?, selected_theme = ?, selected_preferences = ?, suggestions = ?, started_at = ?, completed_at = ?, updated_at = ? WHERE id_onboarding = ?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, onboarding.getUserId());
                ps.setInt(2, onboarding.getStep());
                ps.setBoolean(3, onboarding.isCompleted());
                ps.setString(4, onboarding.getSelectedLocale());
                ps.setString(5, onboarding.getSelectedTheme());
                ps.setString(6, onboarding.getSelectedPreferencesJson());
                ps.setString(7, onboarding.getSuggestions());
                ps.setTimestamp(8, toTimestamp(onboarding.getStartedAt()));
                ps.setTimestamp(9, toTimestamp(onboarding.getCompletedAt()));
                ps.setTimestamp(10, toTimestamp(onboarding.getUpdatedAt()));
                ps.setInt(11, onboarding.getIdOnboarding());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.out.println("OnboardingRepository.update error: " + e.getMessage());
        }

        return false;
    }

    private Onboarding mapRow(ResultSet rs) throws SQLException {
        Onboarding onboarding = new Onboarding();
        onboarding.setIdOnboarding(rs.getInt("id_onboarding"));
        onboarding.setUserId(rs.getInt("user_id"));
        onboarding.setStep(rs.getInt("step"));
        onboarding.setCompleted(rs.getBoolean("completed"));
        onboarding.setSelectedLocale(rs.getString("selected_locale"));
        onboarding.setSelectedTheme(rs.getString("selected_theme"));
        onboarding.setSelectedPreferencesJson(rs.getString("selected_preferences"));
        onboarding.setSuggestions(rs.getString("suggestions"));
        onboarding.setStartedAt(toLocalDateTime(rs.getTimestamp("started_at")));
        onboarding.setCompletedAt(toLocalDateTime(rs.getTimestamp("completed_at")));
        onboarding.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return onboarding;
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}


