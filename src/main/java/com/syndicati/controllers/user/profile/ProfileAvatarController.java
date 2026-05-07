package com.syndicati.controllers.user.profile;

import com.syndicati.models.user.Profile;
import com.syndicati.services.ProfileImageService;

/**
 * Handles profile avatar upload/update workflow for views.
 */
public class ProfileAvatarController {

    private final ProfileController profileController;

    public ProfileAvatarController() {
        this.profileController = new ProfileController();
    }

    public AvatarUpdateResult updateAvatar(Profile profile, byte[] fileData, String originalFileName) {
        if (profile == null) {
            return AvatarUpdateResult.failure("No profile loaded");
        }

        String newImagePath = ProfileImageService.saveAvatarImage(fileData, originalFileName, profile.getIdProfile());
        if (newImagePath == null) {
            return AvatarUpdateResult.failure("Failed to save image");
        }

        profile.setAvatar(newImagePath);
        boolean updated = profileController.updateProfile(profile);
        if (!updated) {
            return AvatarUpdateResult.failure("Failed to update profile");
        }

        return AvatarUpdateResult.success(newImagePath);
    }

    public static class AvatarUpdateResult {
        private final boolean success;
        private final String imagePath;
        private final String message;

        private AvatarUpdateResult(boolean success, String imagePath, String message) {
            this.success = success;
            this.imagePath = imagePath;
            this.message = message;
        }

        public static AvatarUpdateResult success(String imagePath) {
            return new AvatarUpdateResult(true, imagePath, "");
        }

        public static AvatarUpdateResult failure(String message) {
            return new AvatarUpdateResult(false, "", message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getImagePath() {
            return imagePath;
        }

        public String getMessage() {
            return message;
        }
    }
}