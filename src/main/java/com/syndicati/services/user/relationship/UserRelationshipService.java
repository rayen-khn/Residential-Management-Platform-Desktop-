package com.syndicati.services.user.relationship;

import com.syndicati.models.user.User;
import com.syndicati.models.user.UserRelationship;
import com.syndicati.models.user.data.UserRelationshipRepository;
import com.syndicati.services.user.user.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service for native resident friendships and pending requests.
 */
public class UserRelationshipService {

    public static final String STATUS_PENDING_FIRST_SECOND = "PENDING_FIRST_SECOND";
    public static final String STATUS_PENDING_SECOND_FIRST = "PENDING_SECOND_FIRST";
    public static final String STATUS_FRIENDS = "FRIENDS";
    public static final String STATUS_BLOCKED_FIRST_SECOND = "BLOCKED_FIRST_SECOND";
    public static final String STATUS_BLOCKED_SECOND_FIRST = "BLOCKED_SECOND_FIRST";

    private final UserRelationshipRepository relationshipRepository;
    private final UserService userService;

    public UserRelationshipService() {
        this.relationshipRepository = new UserRelationshipRepository();
        this.userService = new UserService();
    }

    public int countFriends(User user) {
        if (user == null || user.getIdUser() == null || user.getIdUser() <= 0) {
            return 0;
        }
        return relationshipRepository.countFriends(user.getIdUser());
    }

    public int countPendingRequests(User user) {
        if (user == null || user.getIdUser() == null || user.getIdUser() <= 0) {
            return 0;
        }
        return relationshipRepository.countPendingRequests(user.getIdUser());
    }

    public List<User> findFriends(User user, int limit) {
        if (user == null || user.getIdUser() == null || user.getIdUser() <= 0) {
            return Collections.emptyList();
        }

        List<UserRelationship> relationships = relationshipRepository.findFriendRelationships(user.getIdUser(), limit > 0 ? limit : null);
        List<User> friends = new ArrayList<>();
        for (UserRelationship relationship : relationships) {
            int friendId = relationship.getUserFirstId() != null && relationship.getUserFirstId().equals(user.getIdUser())
                ? safeId(relationship.getUserSecondId())
                : safeId(relationship.getUserFirstId());
            if (friendId <= 0) {
                continue;
            }
            userService.findById(friendId).ifPresent(friends::add);
        }
        return friends;
    }

    public List<UserRelationship> findPendingRequestsFor(User user) {
        if (user == null || user.getIdUser() == null || user.getIdUser() <= 0) {
            return Collections.emptyList();
        }
        return relationshipRepository.findPendingRelationshipsFor(user.getIdUser());
    }

    public Optional<UserRelationship> findRelationship(User first, User second) {
        if (!isValidUser(first) || !isValidUser(second)) {
            return Optional.empty();
        }
        return relationshipRepository.findRelationship(first.getIdUser(), second.getIdUser());
    }

    public Optional<UserRelationship> findRelationship(int firstUserId, int secondUserId) {
        if (firstUserId <= 0 || secondUserId <= 0 || firstUserId == secondUserId) {
            return Optional.empty();
        }
        return relationshipRepository.findRelationship(firstUserId, secondUserId);
    }

    public boolean sendFriendRequest(int senderUserId, int recipientUserId) {
        if (senderUserId <= 0 || recipientUserId <= 0 || senderUserId == recipientUserId) {
            return false;
        }

        Optional<UserRelationship> existing = relationshipRepository.findRelationship(senderUserId, recipientUserId);
        if (existing.isPresent()) {
            String status = normalizeStatus(existing.get().getStatus());
            return STATUS_FRIENDS.equals(status) || STATUS_PENDING_FIRST_SECOND.equals(status) || STATUS_PENDING_SECOND_FIRST.equals(status);
        }

        UserRelationship relationship = new UserRelationship();
        relationship.setUserFirstId(senderUserId);
        relationship.setUserSecondId(recipientUserId);
        relationship.setStatus(STATUS_PENDING_FIRST_SECOND);
        relationship.setCreatedAt(LocalDateTime.now());
        relationship.setUpdatedAt(LocalDateTime.now());
        return relationshipRepository.save(relationship) > 0;
    }

    public boolean acceptRequest(int relationshipId, int currentUserId) {
        Optional<UserRelationship> relationshipOpt = relationshipRepository.findById(relationshipId);
        if (relationshipOpt.isEmpty()) {
            return false;
        }

        UserRelationship relationship = relationshipOpt.get();
        if (!isPendingForCurrentUser(relationship, currentUserId)) {
            return false;
        }

        relationship.setStatus(STATUS_FRIENDS);
        relationship.setUpdatedAt(LocalDateTime.now());
        return relationshipRepository.save(relationship) > 0;
    }

    public boolean declineRequest(int relationshipId, int currentUserId) {
        Optional<UserRelationship> relationshipOpt = relationshipRepository.findById(relationshipId);
        if (relationshipOpt.isEmpty()) {
            return false;
        }

        UserRelationship relationship = relationshipOpt.get();
        if (!isPendingForCurrentUser(relationship, currentUserId)) {
            return false;
        }

        return relationshipRepository.deleteById(relationshipId);
    }

    public String getRelationshipState(int currentUserId, int otherUserId) {
        Optional<UserRelationship> relationship = findRelationship(currentUserId, otherUserId);
        if (relationship.isEmpty()) {
            return "NONE";
        }

        String status = normalizeStatus(relationship.get().getStatus());
        if (STATUS_FRIENDS.equals(status)) {
            return "FRIENDS";
        }
        if (STATUS_BLOCKED_FIRST_SECOND.equals(status) || STATUS_BLOCKED_SECOND_FIRST.equals(status)) {
            return "BLOCKED";
        }
        return "PENDING";
    }

    public String getRelationshipLabel(int currentUserId, int otherUserId) {
        String state = getRelationshipState(currentUserId, otherUserId);
        return switch (state) {
            case "FRIENDS" -> "Friends";
            case "BLOCKED" -> "Blocked";
            case "PENDING" -> "Pending";
            default -> "Connect";
        };
    }

    public boolean canConnect(int currentUserId, int targetUserId) {
        if (currentUserId <= 0 || targetUserId <= 0 || currentUserId == targetUserId) {
            return false;
        }
        return findRelationship(currentUserId, targetUserId).isEmpty();
    }

    public boolean removeConnection(int currentUserId, int otherUserId) {
        if (currentUserId <= 0 || otherUserId <= 0 || currentUserId == otherUserId) {
            return false;
        }

        Optional<UserRelationship> relationshipOpt = findRelationship(currentUserId, otherUserId);
        if (relationshipOpt.isEmpty()) {
            return false;
        }

        UserRelationship relationship = relationshipOpt.get();
        Integer first = relationship.getUserFirstId();
        Integer second = relationship.getUserSecondId();
        boolean isParticipant = (first != null && (first == currentUserId || first == otherUserId))
            || (second != null && (second == currentUserId || second == otherUserId));
        if (!isParticipant || relationship.getId() == null) {
            return false;
        }

        return relationshipRepository.deleteById(relationship.getId());
    }

    public boolean cancelOutgoingRequest(int senderUserId, int recipientUserId) {
        if (senderUserId <= 0 || recipientUserId <= 0 || senderUserId == recipientUserId) {
            return false;
        }

        Optional<UserRelationship> relationshipOpt = findRelationship(senderUserId, recipientUserId);
        if (relationshipOpt.isEmpty()) {
            return false;
        }

        UserRelationship relationship = relationshipOpt.get();
        if (relationship.getId() == null) {
            return false;
        }

        boolean isOutgoing = relationship.getUserFirstId() != null
            && relationship.getUserSecondId() != null
            && relationship.getUserFirstId() == senderUserId
            && relationship.getUserSecondId() == recipientUserId
            && STATUS_PENDING_FIRST_SECOND.equals(normalizeStatus(relationship.getStatus()));
        if (!isOutgoing) {
            return false;
        }

        return relationshipRepository.deleteById(relationship.getId());
    }

    public boolean isPendingForCurrentUser(UserRelationship relationship, int currentUserId) {
        if (relationship == null || currentUserId <= 0) {
            return false;
        }

        String status = normalizeStatus(relationship.getStatus());
        boolean recipientIsCurrentUser = relationship.getUserSecondId() != null && relationship.getUserSecondId() == currentUserId && STATUS_PENDING_FIRST_SECOND.equals(status);
        boolean senderIsCurrentUser = relationship.getUserFirstId() != null && relationship.getUserFirstId() == currentUserId && STATUS_PENDING_SECOND_FIRST.equals(status);
        return recipientIsCurrentUser || senderIsCurrentUser;
    }

    private boolean isValidUser(User user) {
        return user != null && user.getIdUser() != null && user.getIdUser() > 0;
    }

    private int safeId(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_PENDING_FIRST_SECOND;
        }
        return status.trim().toUpperCase();
    }
}

