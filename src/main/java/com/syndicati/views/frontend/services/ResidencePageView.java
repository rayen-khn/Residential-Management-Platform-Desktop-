package com.syndicati.views.frontend.services;

import com.syndicati.MainApplication;
import com.syndicati.interfaces.ViewInterface;
import com.syndicati.utils.theme.ThemeManager;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Residence page mirrored from /templates/frontend/residence with switcher flow:
 * residences list -> apartments list -> apartment details.
 * UI only: no CRUD/network wiring.
 */
public class ResidencePageView implements ViewInterface {

    private final VBox root;
    private final ThemeManager tm = ThemeManager.getInstance();

    private final StackPane switcher = new StackPane();
    private final VBox residenceFace = new VBox(28);
    private final VBox apartmentsFace = new VBox(20);
    private final VBox detailsFace = new VBox(20);
    private final VBox paginationBox = new VBox(12);

    private int selectedResidence = 0;
    private Apartment selectedApartment;

    private final List<Residence> residences = List.of(
        new Residence("Azure Residence", "Lac 2, Tunis", 12, 120, 3, 2026),
        new Residence("Palm Heights", "La Marsa", 9, 88, 2, 2025),
        new Residence("Jardin Central", "Mutuelleville", 14, 160, 4, 2024),
        new Residence("Skyline Harbor", "Sidi Bousaid", 16, 210, 5, 2026),
        new Residence("Olive Gardens", "Menzah", 8, 74, 2, 2023),
        new Residence("Royal Bay", "Gammarth", 11, 102, 3, 2025)
    );

    private final List<List<Apartment>> apartmentsByResidence = List.of(
        List.of(
            new Apartment("A1", "A", 1, true, 1800, 126, true, "Bright apartment with open terrace and premium finishes."),
            new Apartment("B5", "B", 5, false, 2100, 138, true, "Large family apartment with harbor-facing windows."),
            new Apartment("C3", "C", 3, true, 1650, 114, false, "Compact and elegant layout with smart storage."),
            new Apartment("A8", "A", 8, true, 2300, 149, true, "Top-floor apartment with panoramic city view.")
        ),
        List.of(
            new Apartment("P2", "P", 2, true, 1500, 102, true, "Quiet corner apartment with modern kitchen."),
            new Apartment("H4", "H", 4, false, 1720, 118, false, "Rented unit with upgraded flooring and lighting."),
            new Apartment("H7", "H", 7, true, 1990, 131, true, "Sunny premium unit near rooftop amenities.")
        ),
        List.of(
            new Apartment("J3", "J", 3, true, 1580, 110, false, "Community-facing apartment with soft natural light."),
            new Apartment("J6", "J", 6, true, 1870, 123, true, "Well-balanced layout for couples and small families."),
            new Apartment("C9", "C", 9, false, 2240, 145, true, "High-floor rented unit with premium insulation.")
        ),
        List.of(
            new Apartment("S5", "S", 5, true, 2450, 152, true, "Luxury apartment with double-height living area."),
            new Apartment("S10", "S", 10, false, 2780, 170, true, "Rented executive apartment with marina panorama."),
            new Apartment("H3", "H", 3, true, 2190, 136, false, "Contemporary design with clean modular spaces.")
        ),
        List.of(
            new Apartment("O1", "O", 1, true, 1320, 94, false, "Affordable premium unit ideal for first tenants."),
            new Apartment("O4", "O", 4, true, 1490, 101, true, "Balanced apartment with practical room distribution.")
        ),
        List.of(
            new Apartment("R2", "R", 2, false, 2010, 127, true, "Rented apartment with sea-adjacent cross ventilation."),
            new Apartment("R7", "R", 7, true, 2390, 148, true, "High-end apartment with lounge and office nook."),
            new Apartment("B1", "B", 1, true, 1880, 119, false, "Calm residence-facing apartment with large balcony.")
        )
    );

    public ResidencePageView() {
        root = new VBox(28);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20, 0, 46, 0));
        root.setMaxWidth(Double.MAX_VALUE);
        root.setStyle("-fx-background-color: transparent;");

        root.getChildren().addAll(
            buildHero(),
            buildShowcase(),
            buildPagination()
        );

        rebuildResidenceFace();
        switchToFace("main");
    }

    private StackPane buildHero() {
        StackPane hero = new StackPane();
        hero.prefWidthProperty().bind(Bindings.min(root.widthProperty().multiply(0.95), 1800));
        hero.setMinHeight(320);
        hero.setPrefHeight(360);
        hero.setMaxHeight(390);
        hero.setPadding(new Insets(62, 44, 62, 44));
        hero.paddingProperty().bind(Bindings.createObjectBinding(
            () -> root.getWidth() < 980 ? new Insets(34, 24, 34, 24) : new Insets(62, 44, 62, 44),
            root.widthProperty()
        ));
        hero.setStyle(
            "-fx-background-color: " + surfaceStrong() + ";" +
            "-fx-background-radius: 48px;" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 48px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.48), 36, 0.16, 0, 10);"
        );

        Region glow = new Region();
        glow.setPrefSize(620, 620);
        glow.setStyle(
            "-fx-background-color: radial-gradient(center 50% 50%, radius 60%, " +
            tm.toRgba(tm.getAccentHex(), 0.20) + " 0%, " + tm.toRgba(tm.getAccentHex(), 0.00) + " 70%);"
        );
        glow.setTranslateX(260);
        glow.setTranslateY(-90);

        VBox content = new VBox(14);
        content.setAlignment(Pos.CENTER_LEFT);

        StackPane badge = pill("Premium Living", 12, 0.10, 0.30);

        Text title = text("Luxury Living\nRedefined.", 62, true, "#ffffff");
        title.setFont(Font.font(MainApplication.getInstance().getBoldFontFamily(), FontWeight.BLACK, 62));
        // Keep hero typography stable during hover/reflow by avoiding live width jitter.
        title.setWrappingWidth(760);

        Text subtitle = text(
            "Explore our curated selection of high-end residences and apartments. Experience a new standard of comfort and elegance with Horizon.",
            18,
            false,
            textMuted()
        );
        subtitle.setWrappingWidth(720);

        Button cta = actionBtn("View Residences");

        content.getChildren().addAll(badge, title, subtitle, cta);
        hero.getChildren().addAll(glow, content);
        StackPane.setAlignment(content, Pos.CENTER_LEFT);
        return hero;
    }

    private StackPane buildShowcase() {
        StackPane wrap = new StackPane();
        wrap.prefWidthProperty().bind(Bindings.min(root.widthProperty().multiply(0.95), 1600));
        wrap.setPadding(new Insets(40, 0, 0, 0));

        switcher.setMinHeight(720);
        switcher.setStyle("-fx-background-color: transparent;");

        residenceFace.setVisible(true);
        apartmentsFace.setVisible(false);
        detailsFace.setVisible(false);
        residenceFace.setManaged(true);
        apartmentsFace.setManaged(false);
        detailsFace.setManaged(false);

        switcher.getChildren().addAll(residenceFace, apartmentsFace, detailsFace);
        wrap.getChildren().add(switcher);
        return wrap;
    }

    private VBox buildPagination() {
        paginationBox.setAlignment(Pos.CENTER);
        paginationBox.prefWidthProperty().bind(Bindings.min(root.widthProperty().multiply(0.95), 1600));
        paginationBox.setPadding(new Insets(18, 0, 0, 0));

        HBox pages = new HBox(8,
            pageBtn("<", false),
            pageBtn("1", true),
            pageBtn("2", false),
            pageBtn(">", false)
        );
        pages.setAlignment(Pos.CENTER);

        Text info = text("Showing 6 of 6 residences - Page 1 of 2", 13, false, textMuted());
        paginationBox.getChildren().addAll(pages, info);
        return paginationBox;
    }

    private void rebuildResidenceFace() {
        residenceFace.getChildren().clear();

        VBox sectionLabel = new VBox(10);
        sectionLabel.setAlignment(Pos.CENTER);
        Text h = text("Our Residences", 50, true, tm.getAccentHex());
        Text s = text("Discover the perfect space that suits your lifestyle.", 18, false, textMuted());
        sectionLabel.getChildren().addAll(h, s);

        GridPane cards = responsiveGrid();
        List<Node> cardNodes = new ArrayList<>();
        for (int i = 0; i < residences.size(); i++) {
            cardNodes.add(residenceCard(residences.get(i), i));
        }
        rebuildResponsiveGrid(cards, cardNodes, residenceFace.getWidth(), 3, 2, 1);
        residenceFace.widthProperty().addListener((obs, oldW, newW) ->
            rebuildResponsiveGrid(cards, cardNodes, newW.doubleValue(), 3, 2, 1)
        );

        residenceFace.getChildren().addAll(sectionLabel, cards);
    }

    private VBox residenceCard(Residence r, int index) {
        VBox card = new VBox();
        card.setStyle(
            "-fx-background-color: " + surfaceCard() + ";" +
            "-fx-background-radius: 32px;" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 32px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.24), 18, 0.12, 0, 6);"
        );

        StackPane media = new StackPane();
        media.setMinHeight(280);
        media.setStyle(
            "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.22) + ";" +
            "-fx-background-radius: 32px 32px 0 0;"
        );

        StackPane yearTag = new StackPane(text(String.valueOf(r.year), 12, true, "#ffffff"));
        yearTag.setPadding(new Insets(7, 12, 7, 12));
        yearTag.setStyle(
            "-fx-background-color: rgba(0,0,0,0.6);" +
            "-fx-background-radius: 20px;" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 20px;"
        );
        StackPane.setAlignment(yearTag, Pos.TOP_RIGHT);
        StackPane.setMargin(yearTag, new Insets(20, 20, 0, 0));
        media.getChildren().add(yearTag);

        VBox body = new VBox(14);
        body.setPadding(new Insets(26, 22, 22, 22));

        body.getChildren().addAll(
            miniType("Residence"),
            text(r.name, 30, true, "#ffffff"),
            iconLine("Map", r.address)
        );

        HBox stats = new HBox(12,
            statBox(String.valueOf(r.floors), "Floors"),
            statBox(String.valueOf(r.units), "Units"),
            statBox(String.valueOf(r.blocks), "Blocks")
        );
        stats.setPadding(new Insets(16, 0, 0, 0));
        stats.setStyle("-fx-border-color: " + borderSoft() + " transparent transparent transparent; -fx-border-width: 1px 0 0 0;");

        HBox actions = new HBox(10);
        Button seeApts = mainBtn("See Apartments");
        seeApts.setOnAction(e -> openApartments(index));
        Button export = mainBtn("Export PDF");
        export.setDisable(true);
        export.setOpacity(0.7);
        HBox.setHgrow(seeApts, Priority.ALWAYS);
        HBox.setHgrow(export, Priority.ALWAYS);
        seeApts.setMaxWidth(Double.MAX_VALUE);
        export.setMaxWidth(Double.MAX_VALUE);
        actions.getChildren().addAll(seeApts, export);

        Region bar = new Region();
        bar.setPrefHeight(4);
        bar.setStyle("-fx-background-color: " + tm.getEffectiveAccentGradient() + "; -fx-background-radius: 0 0 32px 32px;");
        bar.setScaleX(0);

        body.getChildren().addAll(stats, actions);
        card.getChildren().addAll(media, body, bar);

        addCardHover(card, bar);
        return card;
    }

    private void openApartments(int residenceIndex) {
        selectedResidence = residenceIndex;
        rebuildApartmentsFace();
        switchToFace("apartments");
    }

    private void rebuildApartmentsFace() {
        apartmentsFace.getChildren().clear();
        Residence residence = residences.get(selectedResidence);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(
            backBtn(() -> switchToFace("main")),
            text("Apartments in " + residence.name, 34, true, "#ffffff")
        );

        GridPane cards = responsiveGrid();
        List<Node> nodes = new ArrayList<>();
        List<Apartment> apartments = apartmentsByResidence.get(selectedResidence);
        for (Apartment apt : apartments) {
            nodes.add(apartmentCard(apt));
        }
        rebuildResponsiveGrid(cards, nodes, apartmentsFace.getWidth(), 3, 2, 1);
        apartmentsFace.widthProperty().addListener((obs, oldW, newW) ->
            rebuildResponsiveGrid(cards, nodes, newW.doubleValue(), 3, 2, 1)
        );

        apartmentsFace.getChildren().addAll(header, cards);
    }

    private VBox apartmentCard(Apartment apt) {
        VBox card = new VBox();
        card.setStyle(
            "-fx-background-color: " + surfaceCard() + ";" +
            "-fx-background-radius: 32px;" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 32px;"
        );

        StackPane media = new StackPane();
        media.setMinHeight(240);
        media.setStyle(
            "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.18) + ";" +
            "-fx-background-radius: 32px 32px 0 0;"
        );

        StackPane availability = pill(apt.available ? "Available" : "Rented", 11, 0.12, 0.25);
        StackPane.setAlignment(availability, Pos.TOP_RIGHT);
        StackPane.setMargin(availability, new Insets(18, 18, 0, 0));
        media.getChildren().add(availability);

        VBox body = new VBox(10);
        body.setPadding(new Insets(20));
        body.getChildren().addAll(
            text("Apartment " + apt.type, 26, true, "#ffffff"),
            infoRow("Bloc", apt.bloc),
            infoRow("Floor", String.valueOf(apt.floor))
        );

        HBox features = new HBox(8);
        if (apt.parking) {
            features.getChildren().add(featureTag("Parking"));
        }
        if (apt.available) {
            features.getChildren().add(featureTag(apt.rent + " TND"));
        }

        Button seeDetails = mainBtn("Voir les details");
        seeDetails.setOnAction(e -> {
            selectedApartment = apt;
            rebuildDetailsFace();
            switchToFace("details");
        });
        seeDetails.setMaxWidth(Double.MAX_VALUE);

        body.getChildren().addAll(features, line(), seeDetails);
        card.getChildren().addAll(media, body);
        addLift(card);
        return card;
    }

    private void rebuildDetailsFace() {
        detailsFace.getChildren().clear();
        Residence residence = residences.get(selectedResidence);
        Apartment apt = selectedApartment != null ? selectedApartment : apartmentsByResidence.get(selectedResidence).get(0);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(
            backBtn(() -> switchToFace("apartments")),
            text("Details de l'Appartement", 34, true, "#ffffff")
        );

        HBox top = new HBox(16);
        top.setAlignment(Pos.TOP_LEFT);

        StackPane image = new StackPane();
        image.setMinHeight(400);
        image.setPrefWidth(620);
        image.setStyle(
            "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.20) + ";" +
            "-fx-background-radius: 24px;" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 24px;"
        );

        VBox info = new VBox(12);
        info.setPadding(new Insets(22));
        info.setStyle(shell(22, "rgba(255,255,255,0.03)", 1.2));
        HBox.setHgrow(info, Priority.ALWAYS);

        info.getChildren().addAll(
            pill("Type " + apt.type, 11, 0.10, 0.25),
            text(residence.name, 30, true, "#ffffff"),
            infoRow("Loyer", apt.rent + " TND"),
            infoRow("Surface", apt.area + " m2"),
            infoRow("Bloc", apt.bloc),
            infoRow("Etage", String.valueOf(apt.floor))
        );
        if (apt.parking) {
            info.getChildren().add(featureTag("Parking inclus"));
        }

        top.getChildren().addAll(image, info);

        VBox recommendations = new VBox(12);
        recommendations.getChildren().add(text("Appartements Similaires", 28, true, "#ffffff"));
        FlowPane recGrid = new FlowPane();
        recGrid.setHgap(12);
        recGrid.setVgap(12);

        List<Apartment> apartments = apartmentsByResidence.get(selectedResidence);
        for (Apartment rec : apartments) {
            if (rec == apt) {
                continue;
            }
            recGrid.getChildren().add(recommendationCard(rec));
        }

        VBox contact = buildContactSection();

        detailsFace.getChildren().addAll(header, top, recommendations, recGrid, contact);
    }

    private VBox recommendationCard(Apartment apt) {
        VBox card = new VBox(8);
        card.setPrefWidth(240);
        card.setPadding(new Insets(10));
        card.setStyle(
            "-fx-background-color: " + surfaceCard() + ";" +
            "-fx-background-radius: 18px;" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 18px;"
        );

        StackPane image = new StackPane();
        image.setMinHeight(120);
        image.setStyle("-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.16) + "; -fx-background-radius: 14px;");

        Button view = mainBtn("Consulter");
        view.setStyle(view.getStyle() + "-fx-font-size: 11px; -fx-padding: 8 10 8 10;");
        view.setOnAction(e -> {
            selectedApartment = apt;
            rebuildDetailsFace();
            switchToFace("details");
        });

        card.getChildren().addAll(
            image,
            text(apt.type + " - " + apt.rent + " TND", 15, true, "#ffffff"),
            view
        );
        return card;
    }

    private VBox buildContactSection() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(22));
        box.setStyle(shell(24, "rgba(255,255,255,0.03)", 1.2));

        HBox row = new HBox(16);

        VBox left = new VBox(10);
        HBox.setHgrow(left, Priority.ALWAYS);
        left.getChildren().addAll(
            text("Contacter le proprietaire", 28, true, "#ffffff"),
            text("Envoyez une demande directe. UI only preview (no network submit).", 14, false, textMuted())
        );

        TextField name = input("Votre nom complet");
        TextField email = input("Votre email");
        TextArea msg = new TextArea();
        msg.setPromptText("Votre message (optionnel)");
        msg.setPrefRowCount(3);
        msg.setStyle(name.getStyle());

        Button send = mainBtn("Envoyer le message");
        send.setMaxWidth(Double.MAX_VALUE);

        left.getChildren().addAll(name, email, msg, send);

        VBox right = new VBox(10);
        right.setAlignment(Pos.TOP_CENTER);
        right.setPrefWidth(260);

        StackPane qr = new StackPane(text("QR", 34, true, tm.getTextColor()));
        qr.setPrefSize(180, 180);
        qr.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16px;"
        );

        right.getChildren().addAll(
            qr,
            text("Scannez pour envoyer un SMS", 12, false, textSoft()),
            mainBtn("Envoyer demande via SMS")
        );

        row.getChildren().addAll(left, right);
        box.getChildren().add(row);
        return box;
    }

    private void switchToFace(String face) {
        boolean main = "main".equals(face);
        boolean apartments = "apartments".equals(face);
        boolean details = "details".equals(face);

        residenceFace.setVisible(main);
        residenceFace.setManaged(main);
        apartmentsFace.setVisible(apartments);
        apartmentsFace.setManaged(apartments);
        detailsFace.setVisible(details);
        detailsFace.setManaged(details);

        paginationBox.setVisible(main);
        paginationBox.setManaged(main);
    }

    private FlowPane responsivePane() {
        FlowPane pane = new FlowPane();
        pane.setHgap(20);
        pane.setVgap(20);
        pane.setAlignment(Pos.TOP_LEFT);
        pane.setMaxWidth(Double.MAX_VALUE);
        pane.prefWrapLengthProperty().bind(root.widthProperty().subtract(120));
        return pane;
    }

    private GridPane responsiveGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setMaxWidth(Double.MAX_VALUE);
        return grid;
    }

    private void rebuildResponsiveGrid(GridPane grid, List<Node> cards, double width, int desktopCols, int tabletCols, int mobileCols) {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();

        // Keep 2 columns through minimum app sizes; only drop to 1 on genuinely narrow widths.
        int cols = width < 700 ? mobileCols : (width < 1400 ? tabletCols : desktopCols);
        for (int i = 0; i < cols; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(100.0 / cols);
            c.setHgrow(Priority.ALWAYS);
            c.setFillWidth(true);
            grid.getColumnConstraints().add(c);
        }

        for (int i = 0; i < cards.size(); i++) {
            Node card = cards.get(i);
            if (card instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
                region.setMinWidth(260);
            }
            GridPane.setFillWidth(card, true);
            grid.add(card, i % cols, i / cols);
        }
    }

    private void bindResponsiveCards(FlowPane pane, List<Node> cards, int desktopCols, int tabletCols, int mobileCols) {
        pane.getChildren().setAll(cards);
        for (Node node : cards) {
            if (node instanceof Region region) {
                region.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> {
                    // Use the largest effective width to avoid accidental early collapse to one column.
                    double available = Math.max(320, Math.max(pane.getWidth(), pane.getPrefWrapLength()));
                    int cols = available >= 1500 ? desktopCols : (available >= 820 ? tabletCols : mobileCols);
                    double totalGap = (cols - 1) * 20;
                    return Math.max(260, (available - totalGap) / cols);
                }, pane.widthProperty(), pane.prefWrapLengthProperty()));
            }
        }
    }

    private HBox iconLine(String iconText, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
            text(iconText + ":", 13, true, tm.getAccentHex()),
            text(value, 14, false, textMuted())
        );
        return row;
    }

    private HBox infoRow(String label, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle("-fx-background-color: " + surfaceSoft() + "; -fx-background-radius: 10px;");
        row.getChildren().addAll(
            text(label + ":", 13, true, tm.getAccentHex()),
            text(value, 13, false, textSoft())
        );
        return row;
    }

    private StackPane statBox(String value, String label) {
        VBox v = new VBox(2,
            text(value, 16, true, "#ffffff"),
            text(label, 10, true, textMuted())
        );
        v.setAlignment(Pos.CENTER);
        StackPane box = new StackPane(v);
        box.setPadding(new Insets(8, 10, 8, 10));
        box.setStyle("-fx-background-color: " + surfaceSoft() + "; -fx-background-radius: 10px;");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private StackPane miniType(String text) {
        return pill(text, 11, 0.05, 0.10);
    }

    private StackPane featureTag(String value) {
        StackPane tag = new StackPane(text(value, 11, true, tm.getAccentHex()));
        tag.setPadding(new Insets(6, 10, 6, 10));
        tag.setStyle(
            "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), 0.10) + ";" +
            "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), 0.22) + ";" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;"
        );
        return tag;
    }

    private StackPane pill(String label, int size, double bgAlpha, double borderAlpha) {
        StackPane p = new StackPane(text(label, size, true, "#ffffff"));
        p.setPadding(new Insets(7, 12, 7, 12));
        p.setMaxWidth(StackPane.USE_PREF_SIZE);
        p.setStyle(
            "-fx-background-color: " + tm.toRgba(tm.getAccentHex(), bgAlpha) + ";" +
            "-fx-border-color: " + tm.toRgba(tm.getAccentHex(), borderAlpha) + ";" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 100px;" +
            "-fx-border-radius: 100px;"
        );
        return p;
    }

    private Button actionBtn(String label) {
        Button b = new Button(label);
        b.setStyle(
            "-fx-background-color: " + tm.getEffectiveAccentGradient() + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: 700;" +
            "-fx-font-size: 14px;" +
            "-fx-padding: 12 28 12 28;" +
            "-fx-background-radius: 12px;"
        );
        return b;
    }

    private Button mainBtn(String label) {
        Button b = new Button(label);
        b.setStyle(
            "-fx-background-color: " + tm.getEffectiveAccentGradient() + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: 700;" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 10 12 10 12;" +
            "-fx-background-radius: 12px;"
        );
        return b;
    }

    private Button pageBtn(String label, boolean active) {
        Button b = new Button(label);
        if (active) {
            b.setStyle(
                "-fx-background-color: " + tm.getEffectiveAccentGradient() + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: 700;" +
                "-fx-min-width: 48px; -fx-min-height: 48px;" +
                "-fx-background-radius: 12px;"
            );
        } else {
            b.setStyle(
                "-fx-background-color: rgba(255,255,255,0.03);" +
                "-fx-border-color: " + borderSoft() + ";" +
                "-fx-border-width: 1px;" +
                "-fx-text-fill: " + textSoft() + ";" +
                "-fx-font-weight: 700;" +
                "-fx-min-width: 48px; -fx-min-height: 48px;" +
                "-fx-background-radius: 12px; -fx-border-radius: 12px;"
            );
        }
        return b;
    }

    private HBox backBtn(Runnable onClick) {
        Button back = new Button("<");
        back.setOnAction(e -> onClick.run());
        back.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06);" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-text-fill: " + textSoft() + ";" +
            "-fx-font-weight: 700;" +
            "-fx-min-width: 44px; -fx-min-height: 44px;" +
            "-fx-background-radius: 999px; -fx-border-radius: 999px;"
        );
        HBox row = new HBox(12, back);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private TextField input(String placeholder) {
        TextField f = new TextField();
        f.setPromptText(placeholder);
        f.setStyle(
            "-fx-background-color: " + surfaceSoft() + ";" +
            "-fx-border-color: " + borderSoft() + ";" +
            "-fx-border-width: 1px;" +
            "-fx-text-fill: " + tm.getTextColor() + ";" +
            "-fx-prompt-text-fill: " + textMuted() + ";" +
            "-fx-background-radius: 12px;" +
            "-fx-border-radius: 12px;"
        );
        return f;
    }

    private Region line() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setStyle("-fx-background-color: " + borderSoft() + ";");
        return r;
    }

    private String surfaceStrong() {
        return tm.isDarkMode()
            ? "linear-gradient(from 0% 0% to 100% 100%, #020202 0%, #070707 58%, #0b0b0b 100%)"
            : "linear-gradient(from 0% 0% to 100% 100%, #ffffff 0%, #f8fafc 100%)";
    }

    private String surfaceCard() {
        return tm.isDarkMode()
            ? "linear-gradient(from 0% 0% to 100% 100%, rgba(10,10,10,0.94) 0%, rgba(14,14,14,0.94) 62%, " + tm.toRgba(tm.getAccentHex(), 0.10) + " 100%)"
            : "linear-gradient(from 0% 0% to 100% 100%, rgba(255,255,255,0.98) 0%, rgba(248,250,252,0.96) 100%)";
    }

    private String surfaceSoft() {
        return tm.isDarkMode() ? "rgba(255,255,255,0.08)" : "rgba(15,23,42,0.06)";
    }

    private String borderSoft() {
        return tm.isDarkMode() ? tm.toRgba(tm.getAccentHex(), 0.34) : "rgba(15,23,42,0.16)";
    }

    private String textSoft() {
        return tm.isDarkMode() ? "rgba(255,255,255,0.93)" : "rgba(15,23,42,0.90)";
    }

    private String textMuted() {
        return tm.isDarkMode() ? "rgba(255,255,255,0.79)" : "rgba(30,41,59,0.82)";
    }

    private String shell(double radius, String inner, double inset) {
        double innerRadius = Math.max(0, radius - inset);
        return "-fx-background-color: " + tm.getEffectiveAccentGradient() + ", " + inner + ";" +
            "-fx-background-insets: 0, " + inset + ";" +
            "-fx-background-radius: " + radius + "px, " + innerRadius + "px;" +
            "-fx-border-color: transparent;";
    }

    private Text text(String value, int size, boolean bold, String color) {
        Text t = new Text(value);
        t.setFont(Font.font(
            bold ? MainApplication.getInstance().getBoldFontFamily() : MainApplication.getInstance().getLightFontFamily(),
            bold ? FontWeight.BOLD : FontWeight.NORMAL,
            size
        ));
        t.setFill(Color.web(color));
        return t;
    }

    private void addCardHover(VBox card, Region accentBar) {
        card.setOnMouseEntered(e -> {
            TranslateTransition lift = new TranslateTransition(Duration.millis(260), card);
            lift.setToY(-15);
            lift.play();

            ScaleTransition scale = new ScaleTransition(Duration.millis(260), card);
            scale.setToX(1.02);
            scale.setToY(1.02);
            scale.play();

            ScaleTransition bar = new ScaleTransition(Duration.millis(220), accentBar);
            bar.setToX(1.0);
            bar.play();
        });
        card.setOnMouseExited(e -> {
            TranslateTransition lift = new TranslateTransition(Duration.millis(220), card);
            lift.setToY(0);
            lift.play();

            ScaleTransition scale = new ScaleTransition(Duration.millis(220), card);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();

            ScaleTransition bar = new ScaleTransition(Duration.millis(200), accentBar);
            bar.setToX(0.0);
            bar.play();
        });
    }

    private void addLift(VBox card) {
        card.setOnMouseEntered(e -> {
            TranslateTransition t = new TranslateTransition(Duration.millis(220), card);
            t.setToY(-8);
            t.play();
        });
        card.setOnMouseExited(e -> {
            TranslateTransition t = new TranslateTransition(Duration.millis(220), card);
            t.setToY(0);
            t.play();
        });
    }

    @Override
    public Pane getRoot() {
        return root;
    }

    @Override
    public void cleanup() {}

    private record Residence(String name, String address, int floors, int units, int blocks, int year) {}

    private record Apartment(String type, String bloc, int floor, boolean available, int rent, int area, boolean parking, String description) {}
}

