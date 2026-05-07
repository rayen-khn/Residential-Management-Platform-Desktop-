package com.syndicati.controllers.user.standing;

import com.syndicati.models.user.UserStanding;
import com.syndicati.services.user.standing.UserStandingService;

/**
 * Standing-focused controller facade for profile workflows.
 */
public class UserStandingController {

    private final UserStandingService standingService;

    public UserStandingController() {
        this.standingService = new UserStandingService();
    }

    public UserStanding findOrCreateByUserId(int userId) {
        return standingService.findOrCreateByUserId(userId);
    }

    public boolean save(UserStanding standing) {
        return standingService.save(standing);
    }

    public UserStanding addExperience(int userId, int xpDelta) {
        return standingService.addExperience(userId, xpDelta);
    }
}