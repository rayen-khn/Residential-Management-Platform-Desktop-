package com.syndicati.views.frontend.settings;

// Rebuilt to match Horizon settings page (settings.html.twig)
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import com.syndicati.interfaces.ViewInterface;
import com.syndicati.utils.shared.AppPreferences;
import com.syndicati.utils.theme.ThemeManager;
import com.syndicati.MainApplication;

/**
 * SettingsView mirrors Horizon /templates/frontend/settings/settings.html.twig.
 *
 * Sections:
 *   1. Theme Mode      - Light / Dark selection cards
 *   2. Accent Color    - 10 preset swatches + custom ColorPicker + hex field
 *   3. Language        - Dropdown (English / French / Arabic)
 *   4. Animations      - Animated borders toggle
 *   5. Notifications   - Push / Email / Sound toggles
 *   6. Privacy         - Visibility / Activity / Analytics
 *   7. General         - Auto-update / Launch on startup
 */
public class SettingsView implements ViewInterface {

    private final VBox root;
    private final ThemeManager tm = ThemeManager.getInstance();
    private final Runnable accentListener = this::rebuildSections;
    private VBox sectionsContainer;

    private static final String[][] ACCENT_SWATCHES = {
        {"#6c5ce7", "Violet", "linear-gradient(from 0% 0% to 100% 100%, #6c5ce7 0%, #8b5cf6 50%, #06b6d4 100%)"},
        {"#ff1493", "Rose", "linear-gradient(from 0% 0% to 100% 100%, #ff1493 0%, #8b5cf6 50%, #06b6d4 100%)"},
        {"#06b6d4", "Cyan", "linear-gradient(from 0% 0% to 100% 100%, #06b6d4 0%, #6c5ce7 50%, #ff1493 100%)"},
        {"#800020", "Bordeaux", "linear-gradient(from 0% 0% to 100% 100%, #800020 0%, #9f1239 50%, #800020 100%)"},
        {"#ff6b6b", "Coral", "linear-gradient(from 0% 0% to 100% 100%, #ff6b6b 0%, #feca57 50%, #ff9ff3 100%)"},
        {"#0abde3", "Ocean", "linear-gradient(from 0% 0% to 100% 100%, #0abde3 0%, #2e86de 50%, #54a0ff 100%)"},
        {"#1dd1a1", "Forest", "linear-gradient(from 0% 0% to 100% 100%, #1dd1a1 0%, #10ac84 50%, #00d2d3 100%)"},
        {"#f39c12", "Amber", "linear-gradient(from 0% 0% to 100% 100%, #f39c12 0%, #e74c3c 45%, #e67e22 100%)"},
        {"#a29bfe", "Lavender", "linear-gradient(from 0% 0% to 100% 100%, #a29bfe 0%, #6c5ce7 50%, #fd79a8 100%)"},
        {"#3b82f6", "Sky High", "linear-gradient(from 0% 0% to 100% 100%, #3b82f6 0%, #8b5cf6 50%, #ec4899 100%)"},
    };

    private static final String[][] LANGUAGES = {
        {"en", "\uD83C\uDDFA\uD83C\uDDF8 English"},
        {"fr", "\uD83C\uDDEB\uD83C\uDDF7 Fran\u00E7ais"},
        {"ar", "\uD83C\uDDF8\uD83C\uDDE6 \u0627\u0644\u0639\u0631\u0628\u064A\u0629"},
    };

    // Kept as fields so ColorPicker <-> TextField can cross-update
    private TextField hexField;

    public SettingsView() {
        this.root = new VBox();
        root.setStyle("-fx-background-color: transparent;");
        buildAll();
        tm.addAccentChangeListener(accentListener);
    }

    private void buildAll() {
        root.getChildren().clear();

        sectionsContainer = new VBox(28);
        sectionsContainer.setAlignment(Pos.TOP_CENTER);
        sectionsContainer.setPadding(new Insets(0, 0, 48, 0));
        sectionsContainer.setMaxWidth(Double.MAX_VALUE);
        buildSections();

        ScrollPane scroll = new ScrollPane(sectionsContainer);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox wrapper = new VBox(24, buildPageHeader(), scroll);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setPadding(new Insets(32, 40, 32, 40));
        wrapper.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().add(wrapper);
        VBox.setVgrow(wrapper, Priority.ALWAYS);
    }

    private void rebuildSections() {
        if (sectionsContainer != null) {
            sectionsContainer.getChildren().clear();
            buildSections();
        }
    }

    private void buildSections() {
        sectionsContainer.getChildren().addAll(
            buildSection("\uD83C\uDFA8", "Theme Mode",
                         "Choose how the app looks - light or dark.",
                         buildThemeModeContent()),
            buildSection("\u2728", "Accent Color",
                         "Personalize your accent color across the entire app.",
                         buildAccentContent()),
            buildSection("\uD83C\uDF0D", "Language",
                         "Choose your preferred display language.",
                         buildLanguageContent()),
            buildSection("\u2728", "Animations",
                         "Enable animated glowing borders throughout the UI.",
                         buildAnimationsContent()),
            buildSection("\uD83D\uDD14", "Notifications",
                         "Manage how and when you receive notifications.",
                         buildNotificationsContent()),
            buildSection("\uD83D\uDD12", "Privacy",
                         "Control your privacy and data preferences.",
                         buildPrivacyContent()),
            buildSection("\u2699", "General",
                         "Application preferences and startup behavior.",
                         buildGeneralContent())
        );
    }

    private VBox buildPageHeader() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.TOP_CENTER);

        Text title = new Text("\u2699  Settings");
        title.setFont(Font.font(bold(), FontWeight.BOLD, 38));
        title.setFill(tm.getAccentGradientPaint());
        glow(title, tm.getAccentHex(), 22);

        Text subtitle = new Text("Customize your Syndicati experience - theme, accent, language and more.");
        subtitle.setFont(Font.font(light(), FontWeight.NORMAL, 15));
        subtitle.setFill(Color.web(tm.getSecondaryTextColor()));

        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private HBox buildSection(String emoji, String title, String desc, javafx.scene.Node content) {
        HBox card = new HBox(28);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(28, 36, 28, 28));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(sectionStyle(false));
        shadow(card, 12, 0.25);

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(64, 64);
        iconBox.setMinSize(64, 64);
        iconBox.setMaxSize(64, 64);
        iconBox.setStyle(
            "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.15) + ";" +
            "-fx-background-radius: 18px;" +
            "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.40) + ";" +
            "-fx-border-width: 1.5px;" +
            "-fx-border-radius: 18px;"
        );
        Text iconTxt = new Text(emoji);
        iconTxt.setFont(Font.font(28));
        iconBox.getChildren().add(iconTxt);

        VBox col = new VBox(12);
        col.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(col, Priority.ALWAYS);

        Text t = new Text(title);
        t.setFont(Font.font(bold(), FontWeight.BOLD, 20));
        t.setFill(tm.getAccentGradientPaint());
        glow(t, tm.getAccentHex(), 10);

        Text d = new Text(desc);
        d.setFont(Font.font(light(), FontWeight.NORMAL, 13));
        d.setFill(Color.web(tm.getSecondaryTextColor()));

        col.getChildren().addAll(t, d, content);
        card.getChildren().addAll(iconBox, col);

        card.setOnMouseEntered(e -> {
            card.setStyle(sectionStyle(true));
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), card);
            tt.setToY(-6); tt.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle(sectionStyle(false));
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), card);
            tt.setToY(0); tt.play();
        });
        return card;
    }

    private String sectionStyle(boolean hover) {
        String bg;
        if (tm.isDarkMode()) {
              String accentSoft = tm.toRgba(tm.getAccentHex(), hover ? 0.18 : 0.12);
              bg = "radial-gradient(focus-angle 24deg, focus-distance 24%, center 14% 10%, radius 132%, " + accentSoft + " 0%, rgba(12,12,18,0.88) 64%, rgba(8,8,12,0.93) 100%), " +
                  "linear-gradient(to bottom right, rgba(30,30,42,0.42), rgba(18,18,24,0.52) 52%, rgba(10,10,14,0.66) 100%)";
        } else {
            bg = hover
                ? "linear-gradient(to bottom right, rgba(255,255,255,0.96), rgba(255,255,255,0.90) 56%, rgba(242,248,255,0.96) 100%)"
                : "linear-gradient(to bottom right, rgba(255,255,255,0.92), rgba(255,255,255,0.84) 56%, rgba(242,248,255,0.92) 100%)";
        }
        String border = hover
            ? tm.toRgba(tm.getAccentHex(), 0.70)
            : tm.toRgba(tm.getAccentHex(), 0.42);
        return "-fx-background-color:" + bg + ";" +
               "-fx-background-radius:28px;" +
               "-fx-border-color:" + border + ";" +
               "-fx-border-width:1px;-fx-border-radius:28px;" +
               "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.30), 30, 0.16, 0, 10);";
    }

    private HBox buildThemeModeContent() {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        boolean isDark = tm.isDarkMode();
        StackPane lightCard = buildThemeCard("\u2600\uFE0F", "Light", "light", !isDark);
        StackPane darkCard  = buildThemeCard("\uD83C\uDF19", "Dark",  "dark",  isDark);

        lightCard.setOnMouseClicked(e -> {
            tm.setDarkModePreference(false);
            lightCard.getChildren().clear(); darkCard.getChildren().clear();
            applyThemeCardStyle(lightCard, true);  lightCard.getChildren().add(themeCardInner("\u2600\uFE0F","Light","light",true));
            applyThemeCardStyle(darkCard,  false); darkCard.getChildren().add(themeCardInner("\uD83C\uDF19","Dark","dark",false));
        });
        darkCard.setOnMouseClicked(e -> {
            tm.setDarkModePreference(true);
            lightCard.getChildren().clear(); darkCard.getChildren().clear();
            applyThemeCardStyle(lightCard, false); lightCard.getChildren().add(themeCardInner("\u2600\uFE0F","Light","light",false));
            applyThemeCardStyle(darkCard,  true);  darkCard.getChildren().add(themeCardInner("\uD83C\uDF19","Dark","dark",true));
        });

        row.getChildren().addAll(lightCard, darkCard);
        return row;
    }

    private StackPane buildThemeCard(String icon, String label, String value, boolean selected) {
        StackPane card = new StackPane();
        card.setPrefSize(190, 72); card.setMinSize(160, 64);
        card.setCursor(Cursor.HAND);
        applyThemeCardStyle(card, selected);
        card.getChildren().add(themeCardInner(icon, label, value, selected));
        return card;
    }

    private void applyThemeCardStyle(StackPane card, boolean selected) {
        String bg = selected
            ? tm.toRgba(tm.getAccentHex(), 0.22)
            : (tm.isDarkMode() ? "rgba(0,0,0,0.40)" : "rgba(0,0,0,0.05)");
        String border = selected
            ? tm.getEffectiveAccentGradient() + ", transparent"
            : tm.toRgba(tm.getAccentHex(), 0.22);
        String insets = selected ? "0, 2" : "0";
        String radii  = selected ? "16px, 14px" : "16px";
        card.setStyle(
            "-fx-background-color: " + (selected ? border : bg) + ";" +
            "-fx-background-insets: " + insets + ";" +
            "-fx-background-radius: " + radii + ";" +
            "-fx-cursor: hand;"
        );
        if (selected) {
            DropShadow ds = new DropShadow(BlurType.GAUSSIAN,
                Color.web(tm.getAccentHex()).deriveColor(0,1,1.3,0.7), 18, 0.2, 0, 0);
            card.setEffect(ds);
        } else {
            card.setEffect(null);
        }
    }

    private HBox themeCardInner(String icon, String label, String value, boolean selected) {
        HBox inner = new HBox(14);
        inner.setAlignment(Pos.CENTER_LEFT);
        inner.setPadding(new Insets(0, 16, 0, 16));

        Text iconT = new Text(icon); iconT.setFont(Font.font(26));

        VBox info = new VBox(3);
        Text nm = new Text(label);
        nm.setFont(Font.font(bold(), FontWeight.BOLD, 14));
        nm.setFill(Color.web(tm.isDarkMode() ? "#ffffff" : "#1a1a2e"));
        Text tag = new Text(value.toUpperCase());
        tag.setFont(Font.font(light(), FontWeight.NORMAL, 10));
        tag.setFill(Color.web(tm.isDarkMode() ? "rgba(255,255,255,0.5)" : "rgba(0,0,0,0.45)"));
        info.getChildren().addAll(nm, tag);

        StackPane check = new StackPane();
        check.setPrefSize(22, 22); check.setMinSize(22, 22);
        String checkBg = selected ? tm.getAccentHex() : "transparent";
        String checkBorder = selected ? "transparent" : tm.toRgba(tm.getAccentHex(), 0.35);
        String checkBorderWidth = selected ? "0" : "2px";
        check.setStyle(
            "-fx-background-color:" + checkBg + ";" +
            "-fx-background-radius:11px;" +
            "-fx-border-color:" + checkBorder + ";" +
            "-fx-border-width:" + checkBorderWidth + ";" +
            "-fx-border-radius:11px;"
        );
        if (selected) {
            Text tick = new Text("\u2713");
            tick.setFont(Font.font(bold(), FontWeight.BOLD, 12));
            tick.setFill(Color.WHITE);
            check.getChildren().add(tick);
        }

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        inner.getChildren().addAll(iconT, info, sp, check);
        return inner;
    }

    private VBox buildAccentContent() {
        VBox col = new VBox(16);
        col.setAlignment(Pos.TOP_LEFT);
        String cur = tm.getAccentHex();

        FlowPane swatches = new FlowPane(10, 10);
        swatches.setAlignment(Pos.CENTER_LEFT);

        for (String[] sw : ACCENT_SWATCHES) {
            String hex = sw[0]; String name = sw[1]; String gradient = sw[2];
            boolean active = hex.equalsIgnoreCase(cur);
            StackPane swatch = makeSwatch(gradient, name, active);
            swatch.setOnMouseClicked(e -> {
                tm.setAccentTheme(hex, gradient);
                AppPreferences.set(AppPreferences.KEY_ACCENT_NAME, name);
                rebuildSections();
            });
            swatches.getChildren().add(swatch);
        }

        // Custom row
        HBox custom = new HBox(12);
        custom.setAlignment(Pos.CENTER_LEFT);
        custom.setPadding(new Insets(12));
        custom.setStyle(
            "-fx-background-color:" + (tm.isDarkMode() ? "rgba(0,0,0,0.30)" : "rgba(0,0,0,0.04)") + ";" +
            "-fx-background-radius:12px;" +
            "-fx-border-color:" + tm.toRgba(tm.getAccentHex(), 0.20) + ";" +
            "-fx-border-width:1px;-fx-border-radius:12px;"
        );

        ColorPicker picker = new ColorPicker(safeColor(cur));
        picker.setPrefSize(52, 52);
        picker.setStyle("-fx-background-radius:12px;-fx-color-label-visible:false;-fx-cursor:hand;");

        hexField = new TextField(cur);
        hexField.setPromptText("#rrggbb");
        hexField.setPrefWidth(150);
        hexField.setFont(Font.font("monospace", 14));
        hexField.setStyle(
            "-fx-background-color:" + (tm.isDarkMode() ? "rgba(0,0,0,0.4)" : "rgba(0,0,0,0.05)") + ";" +
            "-fx-border-color:" + tm.toRgba(tm.getAccentHex(), 0.25) + ";" +
            "-fx-border-width:1.5px;-fx-border-radius:10px;" +
            "-fx-background-radius:10px;-fx-padding:10 14 10 14;"
        );

        picker.setOnAction(e -> {
            String h = toHex(picker.getValue());
            hexField.setText(h);
            tm.setAccentColor(h);
            AppPreferences.set(AppPreferences.KEY_ACCENT_NAME, "Custom");
        });

        hexField.setOnAction(e -> {
            String val = hexField.getText().trim();
            if (!val.startsWith("#")) val = "#" + val;
            try {
                Color c = Color.web(val);
                tm.setAccentColor(val);
                picker.setValue(c);
                AppPreferences.set(AppPreferences.KEY_ACCENT_NAME, "Custom");
            } catch (Exception ignored) {}
        });

        Text hint = new Text("Enter a hex color, e.g. #ff6b6b - changes apply immediately");
        hint.setFont(Font.font(light(), 11));
        hint.setFill(Color.web(tm.getSecondaryTextColor()));

        VBox right = new VBox(4, hexField, hint);
        right.setAlignment(Pos.CENTER_LEFT);
        custom.getChildren().addAll(picker, right);

        col.getChildren().addAll(swatches, custom);
        return col;
    }

    private StackPane makeSwatch(String gradient, String name, boolean selected) {
        StackPane sp = new StackPane();
        sp.setPrefSize(46, 46); sp.setMinSize(46, 46); sp.setMaxSize(46, 46);
        sp.setCursor(Cursor.HAND);
        sp.setStyle(
            "-fx-background-color:" + gradient + ";" +
            "-fx-background-radius:12px;" +
            "-fx-border-color:" + (selected ? "white" : "rgba(255,255,255,0.22)") + ";" +
            "-fx-border-width:" + (selected ? "2.5" : "1.5") + "px;-fx-border-radius:12px;"
        );
        if (selected) {
            Text tick = new Text("\u2713");
            tick.setFont(Font.font(bold(), FontWeight.BOLD, 14));
            tick.setFill(Color.WHITE);
            sp.getChildren().add(tick);
            // Glow uses gradient start color approximated by just using accent
            sp.setEffect(new DropShadow(BlurType.GAUSSIAN,
                Color.web(tm.getAccentHex()).deriveColor(0, 1, 1.3, 0.9), 14, 0.25, 0, 0));
        }
        sp.setOnMouseEntered(e -> { ScaleTransition st = new ScaleTransition(Duration.millis(140), sp); st.setToX(1.18); st.setToY(1.18); st.play(); });
        sp.setOnMouseExited(e ->  { ScaleTransition st = new ScaleTransition(Duration.millis(140), sp); st.setToX(1.0); st.setToY(1.0); st.play(); });
        Tooltip.install(sp, new Tooltip(name));
        return sp;
    }

    private VBox buildLanguageContent() {
        VBox col = new VBox(10);
        String saved = AppPreferences.get(AppPreferences.KEY_LANGUAGE, AppPreferences.DEFAULT_LANGUAGE);
        ComboBox<String> combo = new ComboBox<>();
        int selIdx = 0;
        for (int i = 0; i < LANGUAGES.length; i++) {
            combo.getItems().add(LANGUAGES[i][1]);
            if (LANGUAGES[i][0].equals(saved)) selIdx = i;
        }
        combo.getSelectionModel().select(selIdx);
        combo.setPrefWidth(260);
        combo.setStyle(comboStyle());
        combo.setOnAction(e -> {
            int idx = combo.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < LANGUAGES.length)
                AppPreferences.set(AppPreferences.KEY_LANGUAGE, LANGUAGES[idx][0]);
        });
        col.getChildren().add(combo);
        return col;
    }

    private VBox buildAnimationsContent() {
        VBox col = new VBox(12);
        col.getChildren().add(toggle("Animated Borders", "Enable glowing animated accent borders",
            tm.isAnimatedAccents(), v -> tm.setAnimatedAccents(v)));
        return col;
    }

    private VBox buildNotificationsContent() {
        VBox col = new VBox(12);
        col.getChildren().addAll(
            toggle("Push Notifications", "Receive in-app alerts in real time",
                AppPreferences.getBoolean("notif-push", true),   v -> AppPreferences.setBoolean("notif-push", v)),
            toggle("Email Notifications", "Receive email updates and digests",
                AppPreferences.getBoolean("notif-email", true),  v -> AppPreferences.setBoolean("notif-email", v)),
            toggle("Sound Effects", "Play audio cues for notifications",
                AppPreferences.getBoolean("notif-sound", false), v -> AppPreferences.setBoolean("notif-sound", v))
        );
        return col;
    }

    private VBox buildPrivacyContent() {
        VBox col = new VBox(14);
        String savedVis = AppPreferences.get("privacy-visibility", "Everyone");
        col.getChildren().add(labeledCombo("Profile Visibility", "Who can see your profile",
            new String[]{"Everyone", "Friends Only", "Private"}, savedVis,
            v -> AppPreferences.set("privacy-visibility", v)));
        col.getChildren().addAll(
            toggle("Show Activity Status", "Let others see when you're online",
                AppPreferences.getBoolean("privacy-activity", true),   v -> AppPreferences.setBoolean("privacy-activity", v)),
            toggle("Analytics", "Help improve the app with anonymous usage data",
                AppPreferences.getBoolean("privacy-analytics", true),  v -> AppPreferences.setBoolean("privacy-analytics", v))
        );
        return col;
    }

    private VBox buildGeneralContent() {
        VBox col = new VBox(12);
        col.getChildren().addAll(
            toggle("Auto-Update", "Automatically download and install updates",
                AppPreferences.getBoolean("general-auto-update", true),  v -> AppPreferences.setBoolean("general-auto-update", v)),
            toggle("Launch on Startup", "Open Syndicati when the computer starts",
                AppPreferences.getBoolean("general-startup", false), v -> AppPreferences.setBoolean("general-startup", v))
        );
        return col;
    }

    @FunctionalInterface interface BoolConsumer { void accept(boolean v); }

    private HBox toggle(String title, String desc, boolean init, BoolConsumer onChange) {
        HBox row = new HBox(16); row.setAlignment(Pos.CENTER_LEFT);
        VBox textCol = new VBox(3); HBox.setHgrow(textCol, Priority.ALWAYS);
        Text t = new Text(title); t.setFont(Font.font(bold(), FontWeight.BOLD, 14)); t.setFill(Color.web(tm.getTextColor()));
        Text d = new Text(desc);  d.setFont(Font.font(light(), FontWeight.NORMAL, 12)); d.setFill(Color.web(tm.getSecondaryTextColor()));
        textCol.getChildren().addAll(t, d);

        final boolean[] st = {init};
        StackPane track = new StackPane();
        track.setPrefSize(50, 26); track.setMinSize(50, 26); track.setMaxSize(50, 26);
        track.setCursor(Cursor.HAND);

        StackPane thumb = new StackPane();
        thumb.setPrefSize(20, 20); thumb.setMinSize(20, 20); thumb.setMaxSize(20, 20);
        thumb.setStyle("-fx-background-color: white; -fx-background-radius: 10px;");
        thumb.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.color(0,0,0,0.3), 4, 0, 0, 1));

        StackPane.setAlignment(thumb, Pos.CENTER_LEFT);
        StackPane.setMargin(thumb, new Insets(0, 0, 0, st[0] ? 26 : 3));
        track.setStyle(trackStyle(st[0]));
        track.getChildren().add(thumb);

        track.setOnMouseClicked(e -> {
            st[0] = !st[0];
            track.setStyle(trackStyle(st[0]));
            StackPane.setMargin(thumb, new Insets(0, 0, 0, st[0] ? 26 : 3));
            onChange.accept(st[0]);
        });

        row.getChildren().addAll(textCol, track);
        return row;
    }

    private String trackStyle(boolean on) {
        if (on) {
            // Use the effective (potentially animated) gradient when ON
            return "-fx-background-color:" + tm.getEffectiveAccentGradient() + ";-fx-background-radius:13px;" +
                   "-fx-border-color:" + tm.toRgba(tm.getAccentHex(), 0.0) + ";" +
                   "-fx-border-width:0;-fx-border-radius:13px;";
        } else {
            return "-fx-background-color:" + (tm.isDarkMode() ? "rgba(255,255,255,0.12)" : "rgba(0,0,0,0.12)") + ";-fx-background-radius:13px;" +
                   "-fx-border-color:" + tm.toRgba(tm.getAccentHex(), 0.35) + ";" +
                   "-fx-border-width:1px;-fx-border-radius:13px;";
        }
    }

    private VBox labeledCombo(String title, String desc, String[] opts, String cur,
                               java.util.function.Consumer<String> onChange) {
        VBox col = new VBox(8);
        Text t = new Text(title); t.setFont(Font.font(bold(), FontWeight.BOLD, 14)); t.setFill(Color.web(tm.getTextColor()));
        Text d = new Text(desc);  d.setFont(Font.font(light(), FontWeight.NORMAL, 12)); d.setFill(Color.web(tm.getSecondaryTextColor()));
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(opts); combo.setValue(cur);
        combo.setPrefWidth(220); combo.setStyle(comboStyle());
        combo.setOnAction(e -> { if (combo.getValue() != null) onChange.accept(combo.getValue()); });
        col.getChildren().addAll(t, d, combo);
        return col;
    }

    private String comboStyle() {
        return "-fx-background-color:" + (tm.isDarkMode() ? "rgba(0,0,0,0.4)" : "rgba(0,0,0,0.05)") + ";" +
               "-fx-text-fill:" + (tm.isDarkMode() ? "#ffffff" : "#1a1a2e") + ";" +
               "-fx-border-color:" + tm.toRgba(tm.getAccentHex(), 0.25) + ";" +
               "-fx-border-width:1.5px;-fx-border-radius:12px;" +
               "-fx-background-radius:12px;-fx-padding:6 14 6 14;" +
               "-fx-font-size:14px;-fx-cursor:hand;";
    }

    private void glow(javafx.scene.Node n, String hex, double r) {
        try {
            Color base = Color.web(hex);
            // Brighter, more saturated glow
            Color glowColor = base.deriveColor(0, 1.0, 1.3, 0.75);
            n.setEffect(new DropShadow(BlurType.GAUSSIAN, glowColor, r, 0.15, 0, 0));
        } catch (Exception ignored) {}
    }
    private void shadow(javafx.scene.Node n, double r, double op) {
        n.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.color(0,0,0,op), r, 0,0,5));
    }
    private Color safeColor(String hex) {
        try { return Color.web(hex); } catch (Exception e) { return Color.web("#6c5ce7"); }
    }
    private String toHex(Color c) {
        return String.format("#%02x%02x%02x", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }
    private String bold()  { return MainApplication.getInstance().getBoldFontFamily(); }
    private String light() { return MainApplication.getInstance().getLightFontFamily(); }

    @Override public VBox getRoot() { return root; }
    @Override public void cleanup() { tm.removeAccentChangeListener(accentListener); }
}
    



