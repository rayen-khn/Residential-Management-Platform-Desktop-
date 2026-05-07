package com.syndicati.models.user;

import java.time.LocalDateTime;

/**
 * Native resident standing record aligned with Horizon's user_standing table.
 */
public class UserStanding {

    private Integer userId;
    private int level;
    private int points;
    private String standingLabel = "NORMAL";
    private LocalDateTime updatedAt;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getStandingLabel() {
        return standingLabel;
    }

    public void setStandingLabel(String standingLabel) {
        this.standingLabel = standingLabel;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

