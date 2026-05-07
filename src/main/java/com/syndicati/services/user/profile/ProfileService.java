package com.syndicati.services.user.profile;

import com.syndicati.models.user.Profile;
import com.syndicati.models.user.data.ProfileRepository;

import java.util.List;
import java.util.Optional;

/**
 * Profile service aligned with web admin profile edit flow.
 */
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileService() {
        this.profileRepository = new ProfileRepository();
    }

    public List<Profile> listProfiles() {
        return profileRepository.findAllByIdDesc();
    }

    public Optional<Profile> findById(int idProfile) {
        return profileRepository.findById(idProfile);
    }

    public Optional<Profile> findOneByUserId(int userId) {
        return profileRepository.findOneByUserId(userId);
    }

    public boolean updateProfile(Profile profile) {
        if (profile == null || profile.getIdProfile() == null || profile.getIdProfile() <= 0) {
            return false;
        }

        if (profile.getUserId() == null || profile.getUserId() <= 0) {
            return false;
        }

        return profileRepository.update(profile);
    }

    /**
     * Create a new profile for a user.
     * The profile should have userId set; idProfile will be auto-generated.
     *
     * @param profile Profile entity with userId and other fields set
     * @return Optional containing the generated profileId, or empty if creation failed
     */
    public Optional<Integer> createProfile(Profile profile) {
        if (profile == null || profile.getUserId() == null || profile.getUserId() <= 0) {
            return Optional.empty();
        }

        return profileRepository.insert(profile);
    }

    /**
     * Delete a profile by ID.
     *
     * @param idProfile Profile ID to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteProfile(int idProfile) {
        if (idProfile <= 0) {
            return false;
        }

        return profileRepository.delete(idProfile);
    }
}


