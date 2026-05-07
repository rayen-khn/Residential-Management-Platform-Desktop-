package com.syndicati.models.user;

import java.time.LocalDateTime;

/**
 * Onboarding entity aligned with web table/column naming.
 */
public class Onboarding {

    private Integer idOnboarding;
    private Integer userId;
    private int step = 1;
    private boolean completed;
    private String selectedLocale = "fr";
    private String selectedTheme = "dark";
    private String selectedPreferencesJson;
    private String suggestions;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    public Integer getIdOnboarding() {
        return idOnboarding;
    }

    public void setIdOnboarding(Integer idOnboarding) {
        this.idOnboarding = idOnboarding;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getSelectedLocale() {
        return selectedLocale;
    }

    public void setSelectedLocale(String selectedLocale) {
        this.selectedLocale = selectedLocale;
    }

    public String getSelectedTheme() {
        return selectedTheme;
    }

    public void setSelectedTheme(String selectedTheme) {
        this.selectedTheme = selectedTheme;
    }

    public String getSelectedPreferencesJson() {
        return selectedPreferencesJson;
    }

    public void setSelectedPreferencesJson(String selectedPreferencesJson) {
        this.selectedPreferencesJson = selectedPreferencesJson;
    }

    public String getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(String suggestions) {
        this.suggestions = suggestions;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}


