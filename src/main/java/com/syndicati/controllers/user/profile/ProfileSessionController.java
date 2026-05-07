package com.syndicati.controllers.user.profile;

import com.syndicati.controllers.user.relationship.UserRelationshipController;
import com.syndicati.controllers.user.standing.UserStandingController;
import com.syndicati.controllers.user.user.UserController;
import com.syndicati.models.user.Profile;
import com.syndicati.models.user.User;
import com.syndicati.models.user.UserRelationship;
import com.syndicati.models.user.UserStanding;
import com.syndicati.utils.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates profile-related session data for the profile view.
 */
public class ProfileSessionController {

    private final UserController userController;
    private final ProfileController profileController;
    private final UserStandingController standingController;
    private final UserRelationshipController relationshipController;

    public ProfileSessionController() {
        this.userController = new UserController();
        this.profileController = new ProfileController();
        this.standingController = new UserStandingController();
        this.relationshipController = new UserRelationshipController();
    }

    public SessionSnapshot hydrate(SessionManager sessionManager) {
        User currentUser = sessionManager.getCurrentUser();
        Profile currentProfile = sessionManager.getCurrentProfile();
        UserStanding currentStanding = null;
        List<User> currentCircleFriends = new ArrayList<>();
        List<UserRelationship> currentPendingRelationships = new ArrayList<>();

        if (currentUser != null && currentUser.getIdUser() != null) {
            currentUser = userController.findById(currentUser.getIdUser()).orElse(currentUser);
            sessionManager.setCurrentUser(currentUser);

            if (currentProfile == null) {
                currentProfile = profileController.findOneByUserId(currentUser.getIdUser()).orElse(null);
                if (currentProfile != null) {
                    sessionManager.setCurrentProfile(currentProfile);
                }
            }

            currentStanding = standingController.findOrCreateByUserId(currentUser.getIdUser());
            sessionManager.setCurrentStanding(currentStanding);
            currentCircleFriends = relationshipController.findFriends(currentUser, 8);
            currentPendingRelationships = relationshipController.findPendingRequestsFor(currentUser);
        }

        return new SessionSnapshot(currentUser, currentProfile, currentStanding, currentCircleFriends, currentPendingRelationships);
    }

    public static class SessionSnapshot {
        private final User currentUser;
        private final Profile currentProfile;
        private final UserStanding currentStanding;
        private final List<User> currentCircleFriends;
        private final List<UserRelationship> currentPendingRelationships;

        public SessionSnapshot(
            User currentUser,
            Profile currentProfile,
            UserStanding currentStanding,
            List<User> currentCircleFriends,
            List<UserRelationship> currentPendingRelationships
        ) {
            this.currentUser = currentUser;
            this.currentProfile = currentProfile;
            this.currentStanding = currentStanding;
            this.currentCircleFriends = currentCircleFriends;
            this.currentPendingRelationships = currentPendingRelationships;
        }

        public User getCurrentUser() {
            return currentUser;
        }

        public Profile getCurrentProfile() {
            return currentProfile;
        }

        public UserStanding getCurrentStanding() {
            return currentStanding;
        }

        public List<User> getCurrentCircleFriends() {
            return currentCircleFriends;
        }

        public List<UserRelationship> getCurrentPendingRelationships() {
            return currentPendingRelationships;
        }
    }
}