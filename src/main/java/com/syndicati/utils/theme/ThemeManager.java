package com.syndicati.utils.theme;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.layout.Pane;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.Duration;
import com.syndicati.utils.shared.AppPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Theme Manager - Handles dark and light theme switching
 */
public class ThemeManager {
    
    private static ThemeManager instance;
    private boolean isDarkMode = true; // Start in dark mode
    private Scene currentScene;
    private BooleanProperty isDarkModeProperty = new SimpleBooleanProperty(isDarkMode);
    private String accentColor = AppPreferences.DEFAULT_ACCENT_COLOR;
    private String accentGradient = AppPreferences.DEFAULT_ACCENT_GRADIENT;
    private StringProperty accentColorProperty = new SimpleStringProperty(accentColor);
    private boolean animatedAccents = AppPreferences.DEFAULT_ANIM_ACCENTS;

    // Listeners notified when accent or theme changes (used by SettingsView to refresh UI)
    private final List<Runnable> accentChangeListeners = new ArrayList<>();
    private final DoubleProperty gradientPhase = new SimpleDoubleProperty(0.0);
    private Timeline gradientTimeline;
    
    private ThemeManager() {
        // Load persisted preferences
        String savedTheme = AppPreferences.get(AppPreferences.KEY_THEME, AppPreferences.DEFAULT_THEME);
        this.isDarkMode = "dark".equals(savedTheme);
        this.isDarkModeProperty.set(isDarkMode);

        String savedAccent = AppPreferences.get(AppPreferences.KEY_ACCENT_COLOR, AppPreferences.DEFAULT_ACCENT_COLOR);
        this.accentColor = savedAccent;
        this.accentColorProperty.set(accentColor);

        String savedGradient = AppPreferences.get(
            AppPreferences.KEY_ACCENT_GRADIENT,
            buildGradientFromAccent(savedAccent)
        );
        this.accentGradient = savedGradient;

        this.animatedAccents = AppPreferences.getBoolean(AppPreferences.KEY_ANIM_ACCENTS, AppPreferences.DEFAULT_ANIM_ACCENTS);
        initGradientTimeline();
    }

    private void initGradientTimeline() {
        gradientTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(gradientPhase, 0.0, Interpolator.EASE_BOTH)),
            new KeyFrame(Duration.seconds(7.5), new KeyValue(gradientPhase, 1.0, Interpolator.EASE_BOTH))
        );
        gradientTimeline.setAutoReverse(true);
        gradientTimeline.setCycleCount(Animation.INDEFINITE);
        if (animatedAccents) gradientTimeline.play();
    }
    
    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }
    
    public boolean isDarkMode() {
        return isDarkMode;
    }
    
    public void setDarkMode(boolean darkMode) {
        this.isDarkMode = darkMode;
        this.isDarkModeProperty.set(darkMode);
        AppPreferences.set(AppPreferences.KEY_THEME, darkMode ? "dark" : "light");
        applyTheme();
        notifyAccentListeners();
    }
    
    public void toggleTheme() {
        this.isDarkMode = !this.isDarkMode;
        this.isDarkModeProperty.set(this.isDarkMode);
        AppPreferences.set(AppPreferences.KEY_THEME, isDarkMode ? "dark" : "light");
        System.out.println("[INFO] ThemeManager: Theme toggled to " + (isDarkMode ? "Dark" : "Light") + " mode");
        applyTheme();
        notifyAccentListeners();
    }

    public void setDarkModePreference(boolean dark) {
        this.isDarkMode = dark;
        this.isDarkModeProperty.set(isDarkMode);
        AppPreferences.set(AppPreferences.KEY_THEME, isDarkMode ? "dark" : "light");
        applyTheme();
        notifyAccentListeners();
    }
    public String getAccentHex() {
        return accentColor;
    }

    public StringProperty accentColorProperty() {
        return accentColorProperty;
    }

    public String getAccentGradient() {
        return accentGradient;
    }

    /**
     * Returns a Paint for filling Text nodes using the exact chosen swatch gradient.
     */
    public Paint getAccentGradientPaint() {
        try {
            return Paint.valueOf(accentGradient);
        } catch (Exception e) {
            try { return Color.web(accentColor); } catch (Exception ex) { return Color.PURPLE; }
        }
    }

    /**
     * Kept for compatibility - delegates to getAccentGradientPaint().
     */
    public Paint buildAccentLinearGradient(String ignoredHex) {
        return getAccentGradientPaint();
    }

    /**
     * Returns an animated CSS gradient string for -fx-background-color.
     * Keeps the exact chosen accent colors and gently drifts the gradient along
     * its diagonal instead of rotating it, which feels calmer and less chaotic.
     */
    public String getAnimatedAccentGradient() {
        double phase = gradientPhase.get();
        double drift = -16.0 + (phase * 32.0);
        double x1 = drift;
        double y1 = drift;
        double x2 = 100.0 + drift;
        double y2 = 100.0 + drift;

        // Preserve the original chosen color stops exactly; only the direction window moves.
        try {
            int firstComma = accentGradient.indexOf(",");
            if (firstComma < 0) return accentGradient;
            String stops = accentGradient.substring(firstComma);
            return String.format(Locale.ROOT, "linear-gradient(from %.1f%% %.1f%% to %.1f%% %.1f%%%s",
                x1, y1, x2, y2, stops);
        } catch (Exception e) {
            return accentGradient;
        }
    }

    /** Returns the gradient to use for CSS borders/backgrounds - animated if enabled */
    public String getEffectiveAccentGradient() {
        String base = animatedAccents ? getAnimatedAccentGradient() : accentGradient;
        return base;
    }

    public void setAccentColor(String hex) {
        setAccentTheme(hex, buildGradientFromAccent(hex));
    }

    public void setAccentTheme(String hex, String gradient) {
        this.accentColor = hex;
        this.accentGradient = gradient;
        this.accentColorProperty.set(hex);
        AppPreferences.set(AppPreferences.KEY_ACCENT_COLOR, hex);
        AppPreferences.set(AppPreferences.KEY_ACCENT_GRADIENT, gradient);
        notifyAccentListeners();
    }
    public boolean isAnimatedAccents() { return animatedAccents; }

    public void setAnimatedAccents(boolean value) {
        this.animatedAccents = value;
        AppPreferences.setBoolean(AppPreferences.KEY_ANIM_ACCENTS, value);
        if (gradientTimeline != null) {
            if (value) gradientTimeline.play();
            else { gradientTimeline.stop(); gradientPhase.set(0.0); }
        }
        notifyAccentListeners();
    }

    /** Phase property - bind a node's style to this to react to animation ticks */
    public DoubleProperty gradientPhaseProperty() { return gradientPhase; }
    public void addAccentChangeListener(Runnable listener) {
        if (listener != null && !accentChangeListeners.contains(listener)) {
            accentChangeListeners.add(listener);
        }
    }

    public void removeAccentChangeListener(Runnable listener) {
        accentChangeListeners.remove(listener);
    }

    private void notifyAccentListeners() {
        List<Runnable> snapshot = new ArrayList<>(accentChangeListeners);
        for (Runnable listener : snapshot) {
            try {
                listener.run();
            } catch (Exception ex) {
                System.err.println("ThemeManager listener error: " + ex.getMessage());
            }
        }
    }
    
    public BooleanProperty isDarkModeProperty() {
        return isDarkModeProperty;
    }
    
    public void setScene(Scene scene) {
        this.currentScene = scene;
        applyTheme();
    }
    
    private void applyTheme() {
        if (currentScene == null) return;
        
        if (isDarkMode) {
            applyDarkTheme();
        } else {
            applyLightTheme();
        }
    }
    
    private void applyDarkTheme() {
        // Window uses custom rounded chrome; keep scene transparent so corners show through
        // Previously: currentScene.setFill(Color.web("#000000"));
        currentScene.setFill(Color.TRANSPARENT);
        
        // Don't apply background color to root pane - let ImageBackground handle it
        // Pane root = (Pane) currentScene.getRoot();
        // root.setStyle("-fx-background-color: #0a0b0f;");
        
        System.out.println("[INFO] Dark theme applied (transparent scene; backgrounds handled by views)");
    }
    
    private void applyLightTheme() {
        // Keep scene transparent in light mode as well to preserve rounded window
        // Previously: currentScene.setFill(Color.web("#f8fafc"));
        currentScene.setFill(Color.TRANSPARENT);
        
        // Don't apply background color to root pane - let ImageBackground handle it
        // Pane root = (Pane) currentScene.getRoot();
        // root.setStyle(
        //     "-fx-background-color: linear-gradient(to bottom, #ffffff 0%, #f8fafc 100%);"
        // );
        
        System.out.println("[INFO] Modern light theme applied (transparent scene; backgrounds handled by views)");
    }
    
    public String getBackgroundColor() {
        // Dark = black page; Light = bright page background
        return isDarkMode ? "#000000" : "linear-gradient(to bottom, #ffffff 0%, #f8fafc 100%)";
    }
    
    public String getTextColor() {
        // Dark: white text; Light: slate/dark text for readability on white
        return isDarkMode ? "#ffffff" : "#0f172a";
    }
    
    public String getSecondaryTextColor() {
        // Stronger secondary contrast for dense content blocks
        return isDarkMode ? "#e2e8f0" : "#334155";
    }
    
    public String getDynamicIslandBackground() {
        // Balanced glass: translucent enough to feel alive, opaque enough to preserve readability.
        return isDarkMode ? "rgba(8, 8, 12, 0.86)" : "rgba(0, 0, 0, 0.85)";
    }
    
    public String getDynamicIslandBorder() {
        return isDarkMode
            ? toRgba(accentColor, 0.58)
            : "rgba(15, 23, 42, 0.34)";
    }
    
    public String getTabHoverColor() {
        return isDarkMode ? toRgba(accentColor, 0.24) : "rgba(15, 23, 42, 0.10)";
    }
    
    public String getActiveTabColor() {
        return accentGradient;
    }
    
    public String getToggleTrackBackground() {
        return isDarkMode ? "rgba(10, 10, 10, 0.72)" : "rgba(226, 232, 240, 0.92)";
    }
    
    public String getToggleTrackBorder() {
        return isDarkMode ? toRgba(accentColor, 0.60) : "rgba(15, 23, 42, 0.22)";
    }
    
    public String getToggleTrackHover() {
        return isDarkMode ? toRgba(accentColor, 0.34) : "rgba(15, 23, 42, 0.16)";
    }
    
    // Dark liquid glass colors - blackish with transparency
    public String getLiquidGlassBackground() {
        // Keep the liquid-glass identity while avoiding see-through text issues.
        return isDarkMode ? "rgba(10, 10, 14, 0.84)" : "rgba(255, 255, 255, 0.90)";
    }
    
    public String getLiquidGlassBorder() {
        return isDarkMode ? toRgba(accentColor, 0.62) : "rgba(15, 23, 42, 0.20)";
    }
    
    public String getLiquidGlassHover() {
        return isDarkMode ? toRgba(accentColor, 0.28) : "rgba(255, 255, 255, 0.95)";
    }
    
    public String getLiquidGlassFocus() {
        return isDarkMode ? toRgba(accentColor, 0.40) : "rgba(99, 102, 241, 0.15)";
    }

    /** Convert a hex color to rgba(r, g, b, alpha) string */
    public String toRgba(String hex, double alpha) {
        try {
            Color c = Color.web(hex);
            double effectiveAlpha = alpha;
            if (isDarkMode && isCurrentAccentColor(hex) && alpha > 0) {
                // Dark mode needs a stronger accent alpha to match perceived intensity in light mode.
                effectiveAlpha = Math.min(0.92, alpha * 1.30);
            }
                return String.format(Locale.ROOT, "rgba(%d, %d, %d, %.2f)",
                    (int)(c.getRed() * 255),
                    (int)(c.getGreen() * 255),
                    (int)(c.getBlue() * 255),
                    effectiveAlpha);
        } catch (Exception e) {
            return "rgba(255, 46, 46, " + alpha + ")";
        }
    }

    private boolean isCurrentAccentColor(String hex) {
        try {
            return toHex(Color.web(hex)).equalsIgnoreCase(toHex(Color.web(accentColor)));
        } catch (Exception e) {
            return false;
        }
    }

    /** CSS declarations used by app-scrollbar.css looked-up colors to match current accent/theme. */
    public String getScrollbarVariableStyle() {
        String top = toRgba(accentColor, isDarkMode ? 0.82 : 0.76);
        String bottom = toRgba(deriveColor(accentColor, isDarkMode ? 0.74 : 0.82), isDarkMode ? 0.90 : 0.82);
        String topHover = toRgba(accentColor, isDarkMode ? 0.92 : 0.88);
        String bottomHover = toRgba(deriveColor(accentColor, isDarkMode ? 0.80 : 0.88), isDarkMode ? 0.94 : 0.90);
        String topPressed = toRgba(deriveColor(accentColor, isDarkMode ? 0.68 : 0.76), isDarkMode ? 0.92 : 0.88);
        String bottomPressed = toRgba(deriveColor(accentColor, isDarkMode ? 0.56 : 0.66), isDarkMode ? 0.94 : 0.90);
        String track = isDarkMode ? "rgba(255, 255, 255, 0.13)" : "rgba(17, 24, 39, 0.14)";
        String trackHover = isDarkMode ? "rgba(255, 255, 255, 0.20)" : "rgba(17, 24, 39, 0.24)";
        String trackBorder = isDarkMode ? "rgba(255, 255, 255, 0.24)" : "rgba(17, 24, 39, 0.28)";
        String thumbBorder = isDarkMode ? toRgba(accentColor, 0.54) : "rgba(255, 255, 255, 0.68)";
        return "-sy-scroll-track: " + track + ";"
            + "-sy-scroll-track-border: " + trackBorder + ";"
            + "-sy-scroll-track-hover: " + trackHover + ";"
            + "-sy-scroll-thumb-top: " + top + ";"
            + "-sy-scroll-thumb-bottom: " + bottom + ";"
            + "-sy-scroll-thumb-top-hover: " + topHover + ";"
            + "-sy-scroll-thumb-bottom-hover: " + bottomHover + ";"
            + "-sy-scroll-thumb-top-pressed: " + topPressed + ";"
            + "-sy-scroll-thumb-bottom-pressed: " + bottomPressed + ";"
            + "-sy-scroll-thumb-border: " + thumbBorder + ";"
            + "-sy-scroll-thumb-shadow: rgba(0, 0, 0, 0.30);";
    }
    
    // Dark red liquid glass colors for sign up button
    public String getDarkRedLiquidGlassBackground() {
        return "rgba(139, 0, 0, 0.3)"; // Dark red with same opacity as black
    }
    
    public String getDarkRedLiquidGlassBorder() {
        return "rgba(255, 255, 255, 0.2)"; // Same white border
    }
    
    public String getDarkRedLiquidGlassHover() {
        return "rgba(139, 0, 0, 0.4)"; // Darker red on hover
    }
    
    public String getDarkRedLiquidGlassFocus() {
        return "rgba(139, 0, 0, 0.5)"; // Darkest red on focus
    }
    
    public String getModernAccentColor() {
        // Use persisted accent; in light mode keep accent visible (not white)
        return accentColor;
    }

    public String getModernSecondaryColor() {
        // Derived darker shade - keep simple
        return isDarkMode ? deriveColor(accentColor, 0.8) : deriveColor(accentColor, 0.9);
    }

    /** Naively darken a hex color by multiplying RGB channels by factor (0..1) */
    private String deriveColor(String hex, double factor) {
        try {
            Color c = Color.web(hex);
            int r = (int)(c.getRed()   * factor * 255);
            int g = (int)(c.getGreen() * factor * 255);
            int b = (int)(c.getBlue()  * factor * 255);
            return String.format("#%02x%02x%02x", r, g, b);
        } catch (Exception e) {
            return hex;
        }
    }

    private String buildGradientFromAccent(String hex) {
        try {
            Color base = Color.web(hex);
            Color bright = base.deriveColor(0, 1.0, 1.28, 1.0);
            Color deep = base.deriveColor(0, 1.0, 0.78, 1.0);
            return String.format(
                "linear-gradient(from 0%% 0%% to 100%% 100%%, %s 0%%, %s 52%%, %s 100%%)",
                toHex(bright),
                toHex(base),
                toHex(deep)
            );
        } catch (Exception e) {
            return AppPreferences.DEFAULT_ACCENT_GRADIENT;
        }
    }

    private String toHex(Color color) {
        return String.format(
            "#%02x%02x%02x",
            (int) Math.round(color.getRed() * 255),
            (int) Math.round(color.getGreen() * 255),
            (int) Math.round(color.getBlue() * 255)
        );
    }

    // Glow colors derived from current accent
    public Color getNeonGlowColor() {
        try { return Color.web(accentColor); } catch (Exception e) { return Color.web("#FF2E2E"); }
    }

    public Color getAmberGlowColor() {
        try { return Color.web(deriveColor(accentColor, 0.8)); } catch (Exception e) { return Color.web("#CC1F1F"); }
    }

    // Island-specific text colors (header/footer) to ensure contrast on black island in light mode
    public String getIslandTextColor() {
        return "#FFFFFF";
    }

    public String getIslandSecondaryTextColor() {
        return "rgba(255, 255, 255, 0.90)";
    }
}


