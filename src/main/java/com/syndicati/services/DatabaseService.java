package com.syndicati.services;

import com.syndicati.utils.config.EnvConfig;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database Service - Handles database operations and connectivity
 */
public class DatabaseService {
    
    private static DatabaseService instance;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private int connectionTimeout;
    
    private DatabaseService() {
        // Initialize from DATABASE_URL when available.
        DbConfig cfg = parseDatabaseConfig(EnvConfig.get("DATABASE_URL"));
        this.dbUrl = cfg.jdbcUrl;
        this.dbUser = cfg.username;
        this.dbPassword = cfg.password;
        this.connectionTimeout = 5000; // 5 seconds - longer timeout for debugging
    }
    
    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }
    
    /**
     * Test database connection
     * @return true if connection successful, false otherwise
     */
    public boolean testConnection() {
        System.out.println("=== Database Connection Test ===");
        System.out.println("URL: " + dbUrl);
        System.out.println("User: " + dbUser);
        System.out.println("Password: " + (dbPassword.isEmpty() ? "[empty]" : "[set]"));
        System.out.println("Timeout: " + connectionTimeout + "ms");
        
        // Try multiple connection approaches
        return testConnectionWithProperties() || 
               testConnectionSimple() || 
               testConnectionWithoutDatabase() ||
               testCommonConfigurations();
    }
    
    private boolean testConnectionWithProperties() {
        System.out.println("--- Testing with full properties ---");
        try {
            Properties props = new Properties();
            props.setProperty("user", dbUser);
            props.setProperty("password", dbPassword);
            props.setProperty("connectTimeout", String.valueOf(connectionTimeout));
            props.setProperty("socketTimeout", String.valueOf(connectionTimeout));
            props.setProperty("autoReconnect", "true");
            props.setProperty("useSSL", "false");
            props.setProperty("allowPublicKeyRetrieval", "true");
            props.setProperty("serverTimezone", "UTC");
            props.setProperty("zeroDateTimeBehavior", "CONVERT_TO_NULL");
            
            try (Connection connection = DriverManager.getConnection(dbUrl, props)) {
                if (connection != null && !connection.isClosed()) {
                    System.out.println("[OK] Database connection successful!");
                    return true;
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Connection with properties failed: " + e.getMessage());
            System.out.println("   Error Code: " + e.getErrorCode());
            System.out.println("   SQL State: " + e.getSQLState());
        }
        return false;
    }
    
    private boolean testConnectionSimple() {
        System.out.println("--- Testing with simple connection ---");
        try {
            try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                if (connection != null && !connection.isClosed()) {
                    System.out.println("[OK] Simple database connection successful!");
                    return true;
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Simple connection failed: " + e.getMessage());
        }
        return false;
    }
    
    private boolean testConnectionWithoutDatabase() {
        System.out.println("--- Testing connection to MySQL server (no database) ---");
        try {
            String serverUrl = "jdbc:mysql://" + extractHostPort(dbUrl) + "/";
            try (Connection connection = DriverManager.getConnection(serverUrl, dbUser, dbPassword)) {
                if (connection != null && !connection.isClosed()) {
                    System.out.println("[OK] MySQL server connection successful!");
                    System.out.println("   Server is running, but configured database might not exist or be accessible");
                    return false; // Still return false since we need the specific database
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] MySQL server connection failed: " + e.getMessage());
            System.out.println("   This suggests MySQL server is not running or not accessible");
        }
        return false;
    }
    
    private boolean testCommonConfigurations() {
        System.out.println("--- Using configured DATABASE_URL only (no schema fallback) ---");
        return false;
    }
    
    /**
     * Get database connection
     * @return Connection object or null if failed
     */
    public Connection getConnection() {
        try {
            Properties props = new Properties();
            props.setProperty("user", dbUser);
            props.setProperty("password", dbPassword);
            props.setProperty("connectTimeout", String.valueOf(connectionTimeout));
            props.setProperty("socketTimeout", String.valueOf(connectionTimeout));
            props.setProperty("autoReconnect", "true");
            props.setProperty("zeroDateTimeBehavior", "CONVERT_TO_NULL");
            
            return DriverManager.getConnection(dbUrl, props);
        } catch (SQLException e) {
            System.out.println("Failed to get database connection: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if database is available
     * @return true if database is reachable, false otherwise
     */
    public boolean isDatabaseAvailable() {
        return testConnection();
    }
    
    // Getters and setters for configuration
    public String getDbUrl() {
        return dbUrl;
    }
    
    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }
    
    public String getDbUser() {
        return dbUser;
    }
    
    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }
    
    public String getDbPassword() {
        return dbPassword;
    }
    
    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    private static DbConfig parseDatabaseConfig(String databaseUrl) {
        String fallbackJdbc = "jdbc:mysql://127.0.0.1:3306/syndicati?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=CONVERT_TO_NULL";
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return new DbConfig(fallbackJdbc, "root", "");
        }

        try {
            URI uri = URI.create(databaseUrl);
            String userInfo = uri.getUserInfo();
            String username = "root";
            String password = "";

            if (userInfo != null && !userInfo.isBlank()) {
                String[] parts = userInfo.split(":", 2);
                username = decode(parts[0]);
                if (parts.length > 1) {
                    password = decode(parts[1]);
                }
            }

            String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 3306;
            String path = uri.getPath() == null ? "/syndicati" : uri.getPath();
            String dbName = path.startsWith("/") ? path.substring(1) : path;
            if (dbName.isBlank()) {
                dbName = "syndicati";
            }

            String query = uri.getQuery();
            StringBuilder jdbc = new StringBuilder("jdbc:mysql://")
                .append(host)
                .append(":")
                .append(port)
                .append("/")
                .append(dbName);

            if (query != null && !query.isBlank()) {
                jdbc.append("?").append(query);
                if (!query.contains("zeroDateTimeBehavior=")) {
                    jdbc.append("&zeroDateTimeBehavior=CONVERT_TO_NULL");
                }
            } else {
                jdbc.append("?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=CONVERT_TO_NULL");
            }

            return new DbConfig(jdbc.toString(), username, password);
        } catch (Exception ex) {
            return new DbConfig(fallbackJdbc, "root", "");
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String extractHostPort(String jdbcUrl) {
        String cleaned = jdbcUrl.replace("jdbc:mysql://", "");
        int slashIndex = cleaned.indexOf('/');
        if (slashIndex > 0) {
            return cleaned.substring(0, slashIndex);
        }
        return "127.0.0.1:3306";
    }

    private static class DbConfig {
        private final String jdbcUrl;
        private final String username;
        private final String password;

        private DbConfig(String jdbcUrl, String username, String password) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }
    }
}


