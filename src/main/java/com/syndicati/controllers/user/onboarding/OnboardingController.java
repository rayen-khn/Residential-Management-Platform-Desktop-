package com.syndicati.controllers.user.onboarding;

import com.syndicati.models.user.Onboarding;
import com.syndicati.services.user.onboarding.OnboardingService;

import java.util.List;
import java.util.Optional;

/**
 * Onboarding-focused controller for Java-side CRUD orchestration.
 */
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController() {
        this.onboardingService = new OnboardingService();
    }

    public List<Onboarding> onboardings() {
        return onboardingService.listOnboardings();
    }

    public Optional<Onboarding> onboardingById(int idOnboarding) {
        return onboardingService.findById(idOnboarding);
    }

    public Optional<Onboarding> onboardingByUserId(int userId) {
        return onboardingService.findOneByUserId(userId);
    }

    public Optional<Onboarding> findOneByUserId(int userId) {
        return onboardingService.findOneByUserId(userId);
    }

    public Optional<Onboarding> findOrCreateByUserId(int userId) {
        return onboardingService.findOrCreateByUserId(userId);
    }

    public boolean saveOnboarding(Onboarding onboarding) {
        return onboardingService.saveOnboarding(onboarding);
    }

    public boolean onboardingEdit(Onboarding onboarding) {
        return onboardingService.updateOnboarding(onboarding);
    }
}
