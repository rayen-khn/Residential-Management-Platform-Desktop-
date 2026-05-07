package com.syndicati.utils.navigation;

import javafx.scene.layout.Pane;
import com.syndicati.views.frontend.home.LandingPageView;
import com.syndicati.views.frontend.services.ServicesView;
import com.syndicati.views.frontend.about.AboutView;
import com.syndicati.views.frontend.profile.ProfileView;
import com.syndicati.views.backend.dashboard.DashboardView;
import com.syndicati.views.frontend.services.ServiceDetailView;
import com.syndicati.views.frontend.about.AboutDetailView;
import com.syndicati.views.frontend.settings.SettingsView;
import com.syndicati.views.frontend.services.ResidencePageView;
import com.syndicati.views.frontend.services.ForumPageView;
import com.syndicati.views.frontend.services.SyndicatPageView;
import com.syndicati.views.frontend.services.EvenementPageView;
import com.syndicati.utils.security.AccessControlService;
import com.syndicati.utils.session.SessionManager;
import javafx.scene.control.Alert;

/**
 * Navigation Manager - Handles page navigation and sub-menu management
 */
public class NavigationManager {
    
    private static NavigationManager instance;
    private LandingPageView landingPageView;
    private ServicesView servicesView;
    private AboutView aboutView;
    private ProfileView profileView;
    private DashboardView dashboardView;
    private ServiceDetailView serviceDetailView;
    private AboutDetailView aboutDetailView;
    private SettingsView settingsView;
    private ResidencePageView residenceView;
    private ForumPageView forumView;
    private SyndicatPageView syndicatView;
    private EvenementPageView evenementView;
    
    private NavigationManager() {}
    
    public static NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }
    
    public void setViews(LandingPageView landingPageView) {
        this.landingPageView = landingPageView;
        rebuildThemeSensitiveViews();
    }

    public void rebuildThemeSensitiveViews() {
        // Clear cached views; they will be rebuilt lazily when first requested.
        this.servicesView = null;
        this.aboutView = null;
        this.profileView = null;
        this.dashboardView = null;
        this.serviceDetailView = null;
        this.aboutDetailView = null;
        this.settingsView = null;
        this.residenceView = null;
        this.forumView = null;
        this.syndicatView = null;
        this.evenementView = null;
    }

    private ServicesView servicesView() {
        if (servicesView == null) servicesView = new ServicesView();
        return servicesView;
    }

    private AboutView aboutView() {
        if (aboutView == null) aboutView = new AboutView();
        return aboutView;
    }

    private ProfileView profileView() {
        if (profileView == null) profileView = new ProfileView();
        return profileView;
    }

    private DashboardView dashboardView() {
        if (dashboardView == null) dashboardView = new DashboardView();
        return dashboardView;
    }

    private ServiceDetailView serviceDetailView() {
        if (serviceDetailView == null) serviceDetailView = new ServiceDetailView();
        return serviceDetailView;
    }

    private AboutDetailView aboutDetailView() {
        if (aboutDetailView == null) aboutDetailView = new AboutDetailView();
        return aboutDetailView;
    }

    private SettingsView settingsView() {
        if (settingsView == null) settingsView = new SettingsView();
        return settingsView;
    }

    private ResidencePageView residenceView() {
        if (residenceView == null) residenceView = new ResidencePageView();
        return residenceView;
    }

    private ForumPageView forumView() {
        if (forumView == null) forumView = new ForumPageView();
        return forumView;
    }

    private SyndicatPageView syndicatView() {
        if (syndicatView == null) syndicatView = new SyndicatPageView();
        return syndicatView;
    }

    private EvenementPageView evenementView() {
        if (evenementView == null) evenementView = new EvenementPageView();
        return evenementView;
    }

    public void awardInteractionXp(int xpDelta) {
        SessionManager.getInstance().awardXp(xpDelta);
    }
    
    public Pane getPage(String pageName) {
        switch (pageName.toLowerCase()) {
            case "home":
                return landingPageView.getRoot();
            case "services":
                return servicesView().getRoot();
            case "about":
                return aboutView().getRoot();
            case "profile":
                profileView = new ProfileView();
                return profileView.getRoot();
            case "dashboard":
                return dashboardView().getRoot();
            case "service-detail":
                return serviceDetailView().getRoot();
            case "about-detail":
                return aboutDetailView().getRoot();
            case "settings":
                return settingsView().getRoot();
            case "services/residence":
                return residenceView().getRoot();
            case "services/forum":
                return forumView().getRoot();
            case "services/syndicat":
                return syndicatView().getRoot();
            case "services/evenement":
                return evenementView().getRoot();
            default:
                return landingPageView.getRoot();
        }
    }
    
    public void navigateTo(String pageName) {
        System.out.println("Navigating to: " + pageName);
        String normalizedPage = pageName == null ? "home" : pageName.toLowerCase();

        if ("profile".equals(normalizedPage) && !AccessControlService.canAccessProfile()) {
            showAccessDenied("Please sign in to view your profile.");
            return;
        }

        if ("dashboard".equals(normalizedPage) && !AccessControlService.canAccessAdminArea()) {
            showAccessDenied("Access denied. You do not have permission to access the admin area.");
            return;
        }

        if (landingPageView != null) {
            if ("home".equals(pageName)) {
                landingPageView.navigateToHome();
            } else {
                landingPageView.navigateToPage(pageName);
            }
        }
    }

    private void showAccessDenied(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Access Denied");
        alert.setHeaderText("Permission Required");
        alert.setContentText(message);
        if (landingPageView != null) {
            Pane root = landingPageView.getRoot();
            if (root != null && root.getScene() != null && root.getScene().getWindow() != null) {
                alert.initOwner(root.getScene().getWindow());
            }
        }
        alert.showAndWait();
    }

    /** Exposes the dashboard view so LandingPageView can set its exit callback. */
    public DashboardView getDashboardView() {
        return dashboardView();
    }
}


