package com.syndicati.controllers.user.profile;

import com.syndicati.models.user.Profile;
import com.syndicati.services.user.profile.ProfileService;

import java.util.List;
import java.util.Optional;

/**
 * Profile-focused controller for Java-side CRUD orchestration.
 */
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController() {
        this.profileService = new ProfileService();
    }

    public List<Profile> profiles() {
        return profileService.listProfiles();
    }

    public Optional<Profile> profileById(int idProfile) {
        return profileService.findById(idProfile);
    }

    public Optional<Profile> profileByUserId(int userId) {
        return profileService.findOneByUserId(userId);
    }

    public Optional<Profile> findOneByUserId(int userId) {
        return profileService.findOneByUserId(userId);
    }

    public boolean profileEdit(Profile profile) {
        return profileService.updateProfile(profile);
    }

    public Optional<Integer> profileCreate(Profile profile) {
        return profileService.createProfile(profile);
    }

    public boolean profileUpdate(Profile profile) {
        return profileService.updateProfile(profile);
    }

    public boolean updateProfile(Profile profile) {
        return profileService.updateProfile(profile);
    }

    public boolean profileDelete(int idProfile) {
        return profileService.deleteProfile(idProfile);
    }
}
