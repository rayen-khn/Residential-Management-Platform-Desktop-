package com.syndicati.models.biometric;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * WebAuthn Credential Entity
 * Stores WebAuthn/FIDO2 passkey credentials for users
 * Matches web version: App\Entity\WebAuthn\WebAuthnCredential
 */
public class WebAuthnCredential {

    private Integer idWebauthn;
    private Integer userId;
    private String credentialId;      // Base64url-encoded
    private String publicKey;          // Base64url-encoded CBOR
    private Integer signCount;         // Counter for cloning detection
    private List<String> transports;   // ['internal', 'hybrid', 'usb', 'nfc', 'ble']
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;

    public WebAuthnCredential() {
        this.createdAt = LocalDateTime.now();
        this.lastUsedAt = LocalDateTime.now();
        this.transports = new ArrayList<>();
        this.signCount = 0;
    }

    // Getters and Setters

    public Integer getIdWebauthn() {
        return idWebauthn;
    }

    public void setIdWebauthn(Integer idWebauthn) {
        this.idWebauthn = idWebauthn;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public Integer getSignCount() {
        return signCount;
    }

    public void setSignCount(Integer signCount) {
        this.signCount = signCount;
    }

    public List<String> getTransports() {
        return transports;
    }

    public void setTransports(List<String> transports) {
        this.transports = transports != null ? transports : new ArrayList<>();
    }

    public void addTransport(String transport) {
        if (this.transports == null) {
            this.transports = new ArrayList<>();
        }
        this.transports.add(transport);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}


