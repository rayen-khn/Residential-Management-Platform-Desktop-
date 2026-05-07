package com.syndicati.services.observability;

import java.io.*;
import java.util.Properties;

/**
 * Configuration for Langfuse LLM observability integration.
 * Loads settings from application.local.properties or environment variables.
 *
 * Properties:
 *   langfuse.enabled=true|false
 *   langfuse.base_url=https://cloud.langfuse.com
 *   langfuse.public_key=pk_...
 *   langfuse.secret_key=sk_...
 */
public class LangfuseConfig {

    private boolean enabled;
    private String baseUrl;
    private String publicKey;
    private String secretKey;

    // Defaults
    private static final boolean DEFAULT_ENABLED = false;
    private static final String DEFAULT_BASE_URL = "https://cloud.langfuse.com";
    private static final String DEFAULT_PUBLIC_KEY = "";
    private static final String DEFAULT_SECRET_KEY = "";

    public LangfuseConfig() {
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
            System.out.println("[LangfuseConfig] Could not load application.local.properties: " + e.getMessage());
        }

        // Also try to load from file system
        File propsFile = new File("config/application.local.properties");
        if (propsFile.exists()) {
            try (InputStream is = new FileInputStream(propsFile)) {
                props.load(is);
            } catch (IOException e) {
                System.out.println("[LangfuseConfig] Could not load from file: " + e.getMessage());
            }
        }

        // Load values with environment variable overrides
        this.enabled = getBooleanProperty(props, "langfuse.enabled", DEFAULT_ENABLED);
        this.baseUrl = getStringProperty(props, "langfuse.base_url", DEFAULT_BASE_URL);
        this.publicKey = getStringProperty(props, "langfuse.public_key", DEFAULT_PUBLIC_KEY);
        this.secretKey = getStringProperty(props, "langfuse.secret_key", DEFAULT_SECRET_KEY);

        if (this.enabled) {
            System.out.println("[LangfuseConfig] Langfuse enabled. URL: " + this.baseUrl);
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

    /**
     * Convert property key to environment variable name (e.g., langfuse.enabled -> LANGFUSE_ENABLED).
     */
    private String envKey(String propKey) {
        return propKey.toUpperCase().replace(".", "_");
    }

    // Getters
    public boolean isEnabled() {
        return enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    // Setters (for testing)
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String toString() {
        return "LangfuseConfig{" +
                "enabled=" + enabled +
                ", baseUrl='" + baseUrl + '\'' +
                ", publicKey='" + (publicKey != null && publicKey.length() > 5 ? publicKey.substring(0, 5) + "..." : publicKey) + '\'' +
                '}';
    }
}
