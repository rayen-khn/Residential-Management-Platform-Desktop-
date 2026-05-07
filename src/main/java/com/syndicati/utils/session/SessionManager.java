package com.syndicati.utils.session;

import com.syndicati.models.user.User;
import com.syndicati.models.user.Profile;
import com.syndicati.models.user.UserStanding;
import com.syndicati.controllers.user.standing.UserStandingController;

/**
 * Session manager to track the currently logged-in user.
 * Singleton pattern for global access.
 */
public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private Profile currentProfile;
    private UserStanding currentStanding;
    private long lastXpAwardAt;

    private SessionManager() {
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentProfile(Profile profile) {
        this.currentProfile = profile;
    }

    public Profile getCurrentProfile() {
        return currentProfile;
    }

    public void setCurrentStanding(UserStanding standing) {
        this.currentStanding = standing;
    }

    public UserStanding getCurrentStanding() {
        return currentStanding;
    }

    public synchronized UserStanding awardXp(int xpDelta) {
        if (currentUser == null || currentUser.getIdUser() == null || currentUser.getIdUser() <= 0) {
            return currentStanding;
        }

        int safeDelta = Math.max(0, xpDelta);
        if (safeDelta <= 0) {
            return currentStanding;
        }

        long now = System.currentTimeMillis();
        if (now - lastXpAwardAt < 120) {
            return currentStanding;
        }
        lastXpAwardAt = now;

        UserStandingController controller = new UserStandingController();
        currentStanding = controller.addExperience(currentUser.getIdUser(), safeDelta);
        return currentStanding;
    }

    public void clear() {
        currentUser = null;
        currentProfile = null;
        currentStanding = null;
        lastXpAwardAt = 0L;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public String getCurrentUserEmail() {
        return currentUser != null ? currentUser.getEmailUser() : null;
    }

    public String getCurrentUserName() {
        if (currentUser == null) return null;
        String firstName = currentUser.getFirstName() != null ? currentUser.getFirstName() : "";
        String lastName = currentUser.getLastName() != null ? currentUser.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? null : fullName;
    }
}

