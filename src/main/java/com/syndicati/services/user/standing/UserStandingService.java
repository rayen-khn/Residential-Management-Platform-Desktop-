package com.syndicati.services.user.standing;

import com.syndicati.models.user.UserStanding;
import com.syndicati.models.user.data.UserStandingRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for native resident standing data.
 */
public class UserStandingService {

    private final UserStandingRepository standingRepository;

    public UserStandingService() {
        this.standingRepository = new UserStandingRepository();
    }

    public UserStanding findOrCreateByUserId(int userId) {
        if (userId <= 0) {
            return standingRepository.createDefault(0);
        }

        Optional<UserStanding> existing = standingRepository.findByUserId(userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        UserStanding standing = standingRepository.createDefault(userId);
        standing.setUpdatedAt(LocalDateTime.now());
        standingRepository.save(standing);
        return standing;
    }

    public boolean save(UserStanding standing) {
        if (standing == null || standing.getUserId() == null || standing.getUserId() <= 0) {
            return false;
        }

        standing.setUpdatedAt(LocalDateTime.now());
        return standingRepository.save(standing);
    }

    public UserStanding addExperience(int userId, int xpDelta) {
        UserStanding standing = findOrCreateByUserId(userId);
        int safeDelta = Math.max(0, xpDelta);
        if (safeDelta == 0 || standing.getUserId() == null || standing.getUserId() <= 0) {
            return standing;
        }

        int currentPoints = Math.max(0, standing.getPoints());
        int newPoints = currentPoints + safeDelta;
        standing.setPoints(newPoints);

        int computedLevel = Math.max(1, (newPoints / 100) + 1);
        standing.setLevel(Math.max(standing.getLevel(), computedLevel));

        if (standing.getStandingLabel() == null || standing.getStandingLabel().isBlank()) {
            standing.setStandingLabel("NORMAL");
        }

        standing.setUpdatedAt(LocalDateTime.now());
        standingRepository.save(standing);
        return standing;
    }
}

