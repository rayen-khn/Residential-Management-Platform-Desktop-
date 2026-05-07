package com.syndicati.services.user.onboarding;

import com.syndicati.models.user.Onboarding;
import com.syndicati.models.user.data.OnboardingRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Onboarding service aligned with web admin onboarding edit flow.
 */
public class OnboardingService {

    private final OnboardingRepository onboardingRepository;

    public OnboardingService() {
        this.onboardingRepository = new OnboardingRepository();
    }

    public List<Onboarding> listOnboardings() {
        return onboardingRepository.findAllByIdDesc();
    }

    public Optional<Onboarding> findById(int idOnboarding) {
        return onboardingRepository.findById(idOnboarding);
    }

    public Optional<Onboarding> findOneByUserId(int userId) {
        return onboardingRepository.findOneByUserId(userId);
    }

    public Optional<Onboarding> findOrCreateByUserId(int userId) {
        if (userId <= 0) {
            return Optional.empty();
        }

        Optional<Onboarding> existing = onboardingRepository.findOneByUserId(userId);
        if (existing.isPresent()) {
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        Onboarding onboarding = new Onboarding();
        onboarding.setUserId(userId);
        onboarding.setStep(1);
        onboarding.setCompleted(false);
        onboarding.setSelectedLocale("fr");
        onboarding.setSelectedTheme("dark");
        onboarding.setStartedAt(now);
        onboarding.setUpdatedAt(now);

        Optional<Integer> createdId = onboardingRepository.create(onboarding);
        if (createdId.isEmpty()) {
            return Optional.empty();
        }

        onboarding.setIdOnboarding(createdId.get());
        return Optional.of(onboarding);
    }

    public boolean saveOnboarding(Onboarding onboarding) {
        if (onboarding == null) {
            return false;
        }
        if (onboarding.getUserId() == null || onboarding.getUserId() <= 0) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (onboarding.getStartedAt() == null) {
            onboarding.setStartedAt(now);
        }
        onboarding.setUpdatedAt(now);

        if (onboarding.getIdOnboarding() == null || onboarding.getIdOnboarding() <= 0) {
            Optional<Integer> createdId = onboardingRepository.create(onboarding);
            if (createdId.isEmpty()) {
                return false;
            }
            onboarding.setIdOnboarding(createdId.get());
            return true;
        }

        return onboardingRepository.update(onboarding);
    }

    public boolean updateOnboarding(Onboarding onboarding) {
        if (onboarding == null || onboarding.getIdOnboarding() == null || onboarding.getIdOnboarding() <= 0) {
            return false;
        }

        if (onboarding.getUserId() == null || onboarding.getUserId() <= 0) {
            return false;
        }

        onboarding.setUpdatedAt(LocalDateTime.now());
        return onboardingRepository.update(onboarding);
    }
}


