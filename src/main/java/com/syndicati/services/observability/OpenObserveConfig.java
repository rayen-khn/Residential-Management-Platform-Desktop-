package com.syndicati.services.observability;

import java.io.*;
import java.util.Properties;

/**
 * Configuration for OpenObserve integration.
 * Loads settings from application.local.properties or environment variables.
 *
 * Properties:
 *   openobserve.enabled=true|false
 *   openobserve.url=http://localhost:5080
 *   openobserve.username=admin
 *   openobserve.password=password
 *   openobserve.stream_name=syndicati
 *   openobserve.batch_size=10
 *   openobserve.batch_timeout_ms=5000
 */
public class OpenObserveConfig {

    private boolean enabled;
    private String url;
    private String username;
    private String password;
    private String streamName;
    private int batchSize;
    private long batchTimeoutMs;

    // Defaults
    private static final boolean DEFAULT_ENABLED = false;
    private static final String DEFAULT_URL = "http://localhost:5080";
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_STREAM_NAME = "syndicati";
    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final long DEFAULT_BATCH_TIMEOUT_MS = 5000;

    public OpenObserveConfig() {
        loadFromProperties();
    }

    /**
     * Load configuration from application.local.properties file.
     */
    private void loadFromProperties() {
        Properties props = new Properties();
        
        try (InputStream is = getClass().getResourceAsStream("/application.local.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            System.out.println("[OpenObserveConfig] Could not load application.local.properties: " + e.getMessage());
        }

        // Also try to load from file system
        File propsFile = new File("config/application.local.properties");
        if (propsFile.exists()) {
            try (InputStream is = new FileInputStream(propsFile)) {
                props.load(is);
            } catch (IOException e) {
                System.out.println("[OpenObserveConfig] Could not load from file: " + e.getMessage());
            }
        }

        // Load values with environment variable overrides
        this.enabled = getBooleanProperty(props, "openobserve.enabled", DEFAULT_ENABLED);
        this.url = getStringProperty(props, "openobserve.url", DEFAULT_URL);
        this.username = getStringProperty(props, "openobserve.username", DEFAULT_USERNAME);
        this.password = getStringProperty(props, "openobserve.password", DEFAULT_PASSWORD);
        this.streamName = getStringProperty(props, "openobserve.stream_name", DEFAULT_STREAM_NAME);
        this.batchSize = getIntProperty(props, "openobserve.batch_size", DEFAULT_BATCH_SIZE);
        this.batchTimeoutMs = getLongProperty(props, "openobserve.batch_timeout_ms", DEFAULT_BATCH_TIMEOUT_MS);

        if (this.enabled) {
            System.out.println("[OpenObserveConfig] OpenObserve enabled. URL: " + this.url + ", Stream: " + this.streamName);
        }
    }

    private boolean getBooleanProperty(Properties props, String key, boolean defaultValue) {
        String env = System.getenv(envKey(key));
        if (env != null) {
            return Boolean.parseBoolean(env);
        }
        String prop = props.getProperty(key);
        if (prop != null) {
            return Boolean.parseBoolean(prop);
        }
        return defaultValue;
    }

    private String getStringProperty(Properties props, String key, String defaultValue) {
        String env = System.getenv(envKey(key));
        if (env != null && !env.isEmpty()) {
            return env;
        }
        String prop = props.getProperty(key);
        if (prop != null && !prop.isEmpty()) {
            return prop;
        }
        return defaultValue;
    }

    private int getIntProperty(Properties props, String key, int defaultValue) {
        String env = System.getenv(envKey(key));
        if (env != null && !env.isEmpty()) {
            try {
                return Integer.parseInt(env);
            } catch (NumberFormatException e) {
                // Ignore, use default
            }
        }
        String prop = props.getProperty(key);
        if (prop != null && !prop.isEmpty()) {
            try {
                return Integer.parseInt(prop);
            } catch (NumberFormatException e) {
                // Ignore, use default
            }
        }
        return defaultValue;
    }

    private long getLongProperty(Properties props, String key, long defaultValue) {
        String env = System.getenv(envKey(key));
        if (env != null && !env.isEmpty()) {
            try {
                return Long.parseLong(env);
            } catch (NumberFormatException e) {
                // Ignore, use default
            }
        }
        String prop = props.getProperty(key);
        if (prop != null && !prop.isEmpty()) {
            try {
                return Long.parseLong(prop);
            } catch (NumberFormatException e) {
                // Ignore, use default
            }
        }
        return defaultValue;
    }

    /**
     * Convert property key to environment variable name (e.g., openobserve.enabled -> OPENOBSERVE_ENABLED).
     */
    private String envKey(String propKey) {
        return propKey.toUpperCase().replace(".", "_");
    }

    // Getters
    public boolean isEnabled() {
        return enabled;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getStreamName() {
        return streamName;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public long getBatchTimeoutMs() {
        return batchTimeoutMs;
    }

    // Setters (for testing/manual configuration)
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setBatchTimeoutMs(long batchTimeoutMs) {
        this.batchTimeoutMs = batchTimeoutMs;
    }

    @Override
    public String toString() {
        return "OpenObserveConfig{" +
                "enabled=" + enabled +
                ", url='" + url + '\'' +
                ", username='" + username + '\'' +
                ", streamName='" + streamName + '\'' +
                ", batchSize=" + batchSize +
                ", batchTimeoutMs=" + batchTimeoutMs +
                '}';
    }
}
