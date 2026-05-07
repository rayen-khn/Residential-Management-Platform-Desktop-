package com.syndicati.views.backend.dashboard;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

@SuppressWarnings("SpellCheckingInspection")
final class DashboardResidenceSection {

    private DashboardResidenceSection() {
    }

    static VBox build(DashboardView view) {
        return view.moduleModeView(
            "Residence",
            "\uD83C\uDFE2",
            new String[]{"Residences", "Appartements", "Maintenance"},
            key -> "Appartements".equals(key) ? apartmentsPane(view) :
                   "Maintenance".equals(key) ? maintenancePane(view) :
                   residencesPane(view)
        );
    }

    private static VBox residencesPane(DashboardView view) {
        VBox wrap = new VBox(14);
        wrap.getChildren().add(view.dataTableWithCrud("Residences", "Residence",
            new String[]{"Residence", "Address", "Units", "Syndic", "Status"},
            new String[][]{
                {"Residence Jasmin", "Bardo", "120", "Ahmed B.", "Active"},
                {"Residence Mimosa", "Lac 2", "86", "Leila M.", "Active"},
                {"Residence Olive", "Menzah", "64", "Karim S.", "Maintenance"}
            }, true
        ));
        return wrap;
    }

    private static VBox apartmentsPane(DashboardView view) {
        VBox wrap = new VBox(14);
        wrap.getChildren().add(view.dataTableWithCrud("Appartements", "Appartement",
            new String[]{"Unit", "Residence", "Resident", "Floor", "State"},
            new String[][]{
                {"A-101", "Jasmin", "Ahmed B.", "1", "Occupied"},
                {"A-202", "Jasmin", "Leila M.", "2", "Occupied"},
                {"B-105", "Mimosa", "-", "1", "Available"},
                {"C-401", "Olive", "-", "4", "Available"}
            }, true
        ));
        return wrap;
    }

    private static VBox maintenancePane(DashboardView view) {
        VBox wrap = new VBox(14);
        wrap.getChildren().add(view.dataTableWithCrud("Maintenance", "Maintenance Ticket",
            new String[]{"Ticket", "Residence", "Issue", "Priority", "Status"},
            new String[][]{
                {"MNT-301", "Jasmin", "Water Pump", "High", "In Progress"},
                {"MNT-298", "Mimosa", "Garage Lighting", "Medium", "Open"},
                {"MNT-296", "Olive", "Lift Noise", "Low", "Scheduled"}
            }, false
        ));
        return wrap;
    }
}
