package com.syndicati.utils.shared;

import java.util.prefs.Preferences;

/**
 * AppPreferences - Persistent key-value storage equivalent to localStorage.
 * Backed by java.util.prefs.Preferences (OS keystore / registry on Windows).
 */
public class AppPreferences {

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(AppPreferences.class);
    public static final String KEY_THEME            = "theme";           // "dark" | "light"
    public static final String KEY_ACCENT_COLOR     = "accent-color";    // hex e.g. "#6c5ce7"
    public static final String KEY_ACCENT_GRADIENT  = "accent-gradient"; // JavaFX linear-gradient(...)
    public static final String KEY_ACCENT_NAME      = "accent-name";     // e.g. "Violet"
    public static final String KEY_LANGUAGE         = "lang";            // "en" | "fr" | "ar"
    public static final String KEY_ANIM_ACCENTS     = "animated-accents";// "true"/"false"
    public static final String DEFAULT_THEME        = "dark";
    public static final String DEFAULT_ACCENT_COLOR = "#6c5ce7";
    public static final String DEFAULT_ACCENT_GRADIENT = "linear-gradient(from 0% 0% to 100% 100%, #6c5ce7 0%, #8b5cf6 50%, #06b6d4 100%)";
    public static final String DEFAULT_ACCENT_NAME  = "Violet";
    public static final String DEFAULT_LANGUAGE     = "en";
    public static final boolean DEFAULT_ANIM_ACCENTS = true;

    private AppPreferences() {}

    public static void set(String key, String value) {
        PREFS.put(key, value);
    }

    public static String get(String key, String defaultValue) {
        return PREFS.get(key, defaultValue);
    }

    public static void setBoolean(String key, boolean value) {
        PREFS.putBoolean(key, value);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return PREFS.getBoolean(key, defaultValue);
    }
}



