package com.syndicati.models.user;

import java.time.LocalDateTime;

/**
 * Native resident relationship record aligned with Horizon's user_relationship table.
 */
public class UserRelationship {

    private Integer id;
    private Integer userFirstId;
    private Integer userSecondId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserFirstId() {
        return userFirstId;
    }

    public void setUserFirstId(Integer userFirstId) {
        this.userFirstId = userFirstId;
    }

    public Integer getUserSecondId() {
        return userSecondId;
    }

    public void setUserSecondId(Integer userSecondId) {
        this.userSecondId = userSecondId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

