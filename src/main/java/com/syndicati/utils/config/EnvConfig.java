package com.syndicati.utils.config;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Native Java config loader with precedence:
 * 1) JVM system properties (-DKEY=value)
 * 2) OS environment variables
 * 3) config/application.local.properties
 * 4) config/application.properties
 * 5) legacy dotenv files (.env*) as compatibility fallback
 */
public final class EnvConfig {

    private static final Map<String, String> LOADED = load();

    private EnvConfig() {
    }

    public static String get(String key) {
        String systemProperty = System.getProperty(key);
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty;
        }

        String real = System.getenv(key);
        if (real != null && !real.isBlank()) {
            return real;
        }
        return LOADED.get(key);
    }

    public static String getOrDefault(String key, String defaultValue) {
        String value = get(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return "1".equals(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }

    private static Map<String, String> load() {
        Map<String, String> env = new HashMap<>();

        // Native Java properties first
        loadPropertiesFile(Path.of("config", "application.properties"), env);
        loadPropertiesFile(Path.of("config", "application.local.properties"), env);

        // Legacy dotenv fallback for compatibility
        loadDotenvFile(Path.of(".env"), env);
        loadDotenvFile(Path.of(".env.local"), env);

        String appEnv = env.getOrDefault("APP_ENV", "dev");
        loadPropertiesFile(Path.of("config", "application." + appEnv + ".properties"), env);
        loadPropertiesFile(Path.of("config", "application." + appEnv + ".local.properties"), env);
        loadDotenvFile(Path.of(".env." + appEnv), env);
        loadDotenvFile(Path.of(".env." + appEnv + ".local"), env);

        return env;
    }

    private static void loadPropertiesFile(Path path, Map<String, String> env) {
        if (!Files.exists(path)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
        } catch (IOException ignored) {
            return;
        }

        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            if (value != null) {
                env.put(key, value.trim());
            }
        }
    }

    private static void loadDotenvFile(Path path, Map<String, String> env) {
        if (!Files.exists(path)) {
            return;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return;
        }

        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith("export ")) {
                line = line.substring("export ".length()).trim();
            }

            int idx = line.indexOf('=');
            if (idx <= 0) {
                continue;
            }

            String key = line.substring(0, idx).trim();
            String value = line.substring(idx + 1).trim();
            value = stripQuotes(value);
            env.put(key, value);
        }
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}

