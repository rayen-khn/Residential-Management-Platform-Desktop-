package com.syndicati.models.biometric.data;

import com.syndicati.models.biometric.FaceCredential;
import com.syndicati.services.DatabaseService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for FaceCredential persistence
 * Handles face enrollment data storage and retrieval
 */
public class FaceCredentialRepository {

    private final DatabaseService databaseService;

    public FaceCredentialRepository() {
        this.databaseService = DatabaseService.getInstance();
    }

    /**
     * Save or update a face credential
     */
    public FaceCredential save(FaceCredential credential) {
        if (credential.getIdFacecred() == null) {
            return insert(credential);
        } else {
            return update(credential);
        }
    }

    /**
     * Insert a new face credential
     */
    private FaceCredential insert(FaceCredential credential) {
        String sql = "INSERT INTO facecred (user_id, device_id, encrypted_faceid, created_at, updated_at, last_used_at, flag) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return credential;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, credential.getUserId());
                ps.setString(2, credential.getDeviceId());
                ps.setBytes(3, credential.getEncryptedFaceid());
                ps.setTimestamp(4, Timestamp.valueOf(credential.getCreatedAt()));
                ps.setTimestamp(5, Timestamp.valueOf(credential.getUpdatedAt()));
                ps.setTimestamp(6, Timestamp.valueOf(credential.getLastUsedAt()));
                ps.setString(7, credential.getFlag());

                ps.executeUpdate();

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        credential.setIdFacecred(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("FaceCredentialRepository.insert error: " + e.getMessage());
            e.printStackTrace();
        }

        return credential;
    }

    /**
     * Update an existing face credential
     */
    private FaceCredential update(FaceCredential credential) {
        String sql = "UPDATE facecred SET user_id=?, device_id=?, encrypted_faceid=?, updated_at=?, last_used_at=?, flag=? " +
                     "WHERE id_facecred=?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return credential;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, credential.getUserId());
                ps.setString(2, credential.getDeviceId());
                ps.setBytes(3, credential.getEncryptedFaceid());
                ps.setTimestamp(4, Timestamp.valueOf(credential.getUpdatedAt()));
                ps.setTimestamp(5, Timestamp.valueOf(credential.getLastUsedAt()));
                ps.setString(6, credential.getFlag());
                ps.setInt(7, credential.getIdFacecred());

                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("FaceCredentialRepository.update error: " + e.getMessage());
            e.printStackTrace();
        }

        return credential;
    }

    /**
     * Find an active face credential for a specific user and device
     */
    public Optional<FaceCredential> findActiveByUserAndDevice(Integer userId, String deviceId) {
        String sql = "SELECT * FROM facecred " +
                     "WHERE user_id=? AND LOWER(TRIM(device_id))=LOWER(TRIM(?)) AND flag='active' " +
                     "ORDER BY updated_at DESC LIMIT 1";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return Optional.empty();
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setString(2, deviceId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("FaceCredentialRepository.findActiveByUserAndDevice error: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }
    
    /**
     * Find any active face credential for a user (device-agnostic)
     */
    public Optional<FaceCredential> findAnyActiveByUser(Integer userId) {
        String sql = "SELECT * FROM facecred WHERE user_id=? AND flag='active' " +
                     "ORDER BY updated_at DESC LIMIT 1";

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
            System.err.println("FaceCredentialRepository.findAnyActiveByUser error: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Find all active credentials for a user
     */
    public List<FaceCredential> findAllActiveByUser(Integer userId) {
        String sql = "SELECT * FROM facecred WHERE user_id=? AND flag='active' ";
        List<FaceCredential> credentials = new ArrayList<>();

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return credentials;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            credentials.add(mapRow(rs));
                        } catch (SQLException rowError) {
                            System.err.println("FaceCredentialRepository.findAllActiveByUser skipped row: " + rowError.getMessage());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("FaceCredentialRepository.findAllActiveByUser error: " + e.getMessage());
            e.printStackTrace();
        }

        return credentials;
    }

    /**
     * Delete/deactivate a face credential
     */
    public boolean deactivate(Integer credentialId) {
        String sql = "UPDATE facecred SET flag='inactive' WHERE id_facecred=?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, credentialId);
                int rows = ps.executeUpdate();
                return rows > 0;
            }
        } catch (SQLException e) {
            System.err.println("FaceCredentialRepository.deactivate error: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Delete all face credentials for a user
     */
    public boolean deleteAllByUser(Integer userId) {
        String sql = "DELETE FROM facecred WHERE user_id=?";

        try (Connection conn = databaseService.getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                int rows = ps.executeUpdate();
                return rows > 0;
            }
        } catch (SQLException e) {
            System.err.println("FaceCredentialRepository.deleteAllByUser error: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Map ResultSet row to FaceCredential object
     */
    private FaceCredential mapRow(ResultSet rs) throws SQLException {
        FaceCredential credential = new FaceCredential();
        credential.setIdFacecred(rs.getInt("id_facecred"));
        credential.setUserId(rs.getInt("user_id"));
        credential.setDeviceId(rs.getString("device_id"));
        credential.setEncryptedFaceid(rs.getBytes("encrypted_faceid"));

        Timestamp createdAtTs = rs.getTimestamp("created_at");
        if (createdAtTs != null) {
            credential.setCreatedAt(createdAtTs.toLocalDateTime());
        }

        Timestamp updatedAtTs = rs.getTimestamp("updated_at");
        if (updatedAtTs != null) {
            credential.setUpdatedAt(updatedAtTs.toLocalDateTime());
        }

        Timestamp lastUsedAtTs = rs.getTimestamp("last_used_at");
        if (lastUsedAtTs != null) {
            credential.setLastUsedAt(lastUsedAtTs.toLocalDateTime());
        }

        credential.setFlag(rs.getString("flag"));

        return credential;
    }
}


