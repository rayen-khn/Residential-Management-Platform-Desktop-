package com.syndicati.services.observability;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration for LogAI anomaly detection worker integration.
 */
public class LogAIConfig {

    private static final boolean DEFAULT_ENABLED = false;
    private static final String DEFAULT_WORKER_URL = "http://localhost:8001";
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_INTERVAL_SECONDS = 300;
    private static final double DEFAULT_ANOMALY_THRESHOLD = 0.65;

    private boolean enabled;
    private String workerUrl;
    private int batchSize;
    private int timeoutSeconds;
    private int scheduleIntervalSeconds;
    private double anomalyThreshold;

    public LogAIConfig() {
        loadFromProperties();
    }

    private void loadFromProperties() {
        Properties props = new Properties();

        try (InputStream is = getClass().getResourceAsStream("/application.local.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            System.out.println("[LogAIConfig] Could not load classpath properties: " + e.getMessage());
        }

        File fsProps = new File("config/application.local.properties");
        if (fsProps.exists()) {
            try (InputStream is = new FileInputStream(fsProps)) {
                props.load(is);
            } catch (IOException e) {
                System.out.println("[LogAIConfig] Could not load filesystem properties: " + e.getMessage());
            }
        }

        enabled = getBooleanProperty(props, "logai.enabled", DEFAULT_ENABLED);
        workerUrl = getStringProperty(props, "logai.worker_url", DEFAULT_WORKER_URL);
        batchSize = getIntProperty(props, "logai.batch_size", DEFAULT_BATCH_SIZE);
        timeoutSeconds = getIntProperty(props, "logai.timeout_seconds", DEFAULT_TIMEOUT_SECONDS);
        scheduleIntervalSeconds = getIntProperty(props, "logai.schedule_interval_seconds", DEFAULT_INTERVAL_SECONDS);
        anomalyThreshold = getDoubleProperty(props, "logai.anomaly_threshold", DEFAULT_ANOMALY_THRESHOLD);

        if (enabled) {
            System.out.println("[LogAIConfig] LogAI enabled. Worker URL: " + workerUrl);
        }
    }

    private boolean getBooleanProperty(Properties props, String key, boolean defaultValue) {
        String env = System.getenv(toEnvKey(key));
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
        String env = System.getenv(toEnvKey(key));
        if (env != null && !env.isBlank()) {
            return env;
        }
        String prop = props.getProperty(key);
        if (prop != null && !prop.isBlank()) {
            return prop;
        }
        return defaultValue;
    }

    private int getIntProperty(Properties props, String key, int defaultValue) {
        String env = System.getenv(toEnvKey(key));
        if (env != null && !env.isBlank()) {
            try {
                return Integer.parseInt(env);
            } catch (NumberFormatException ignored) {
            }
        }
        String prop = props.getProperty(key);
        if (prop != null && !prop.isBlank()) {
            try {
                return Integer.parseInt(prop);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private double getDoubleProperty(Properties props, String key, double defaultValue) {
        String env = System.getenv(toEnvKey(key));
        if (env != null && !env.isBlank()) {
            try {
                return Double.parseDouble(env);
            } catch (NumberFormatException ignored) {
            }
        }
        String prop = props.getProperty(key);
        if (prop != null && !prop.isBlank()) {
            try {
                return Double.parseDouble(prop);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private String toEnvKey(String propKey) {
        return propKey.toUpperCase().replace('.', '_');
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getWorkerUrl() {
        return workerUrl;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getScheduleIntervalSeconds() {
        return scheduleIntervalSeconds;
    }

    public double getAnomalyThreshold() {
        return anomalyThreshold;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setWorkerUrl(String workerUrl) {
        this.workerUrl = workerUrl;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public void setScheduleIntervalSeconds(int scheduleIntervalSeconds) {
        this.scheduleIntervalSeconds = scheduleIntervalSeconds;
    }

    public void setAnomalyThreshold(double anomalyThreshold) {
        this.anomalyThreshold = anomalyThreshold;
    }
}
