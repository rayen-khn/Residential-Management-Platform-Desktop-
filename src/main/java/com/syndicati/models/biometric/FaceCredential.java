package com.syndicati.models.biometric;

import java.time.LocalDateTime;

/**
 * FaceID Credential Entity
 * Stores encrypted face embeddings for users
 * Matches web version: App\Entity\FaceCred\FaceCredential
 */
public class FaceCredential {

    private Integer idFacecred;
    private Integer userId;
    private String deviceId;
    private byte[] encryptedFaceid;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastUsedAt;
    private String flag; // 'active' or 'inactive'

    public FaceCredential() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.lastUsedAt = LocalDateTime.now();
        this.flag = "active";
    }

    // Getters and Setters

    public Integer getIdFacecred() {
        return idFacecred;
    }

    public void setIdFacecred(Integer idFacecred) {
        this.idFacecred = idFacecred;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public byte[] getEncryptedFaceid() {
        return encryptedFaceid;
    }

    public void setEncryptedFaceid(byte[] encryptedFaceid) {
        this.encryptedFaceid = encryptedFaceid;
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

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public boolean isActive() {
        return "active".equalsIgnoreCase(flag);
    }
}


