package com.syndicati.controllers.user.relationship;

import com.syndicati.models.user.User;
import com.syndicati.models.user.UserRelationship;
import com.syndicati.services.user.relationship.UserRelationshipService;

import java.util.List;
import java.util.Optional;

/**
 * Relationship-focused controller facade for profile/social workflows.
 */
public class UserRelationshipController {

    private final UserRelationshipService relationshipService;

    public UserRelationshipController() {
        this.relationshipService = new UserRelationshipService();
    }

    public int countFriends(User user) {
        return relationshipService.countFriends(user);
    }

    public int countPendingRequests(User user) {
        return relationshipService.countPendingRequests(user);
    }

    public List<User> findFriends(User user, int limit) {
        return relationshipService.findFriends(user, limit);
    }

    public List<UserRelationship> findPendingRequestsFor(User user) {
        return relationshipService.findPendingRequestsFor(user);
    }

    public Optional<UserRelationship> findRelationship(User first, User second) {
        return relationshipService.findRelationship(first, second);
    }

    public Optional<UserRelationship> findRelationship(int firstUserId, int secondUserId) {
        return relationshipService.findRelationship(firstUserId, secondUserId);
    }

    public boolean sendFriendRequest(int senderUserId, int recipientUserId) {
        return relationshipService.sendFriendRequest(senderUserId, recipientUserId);
    }

    public boolean acceptRequest(int relationshipId, int currentUserId) {
        return relationshipService.acceptRequest(relationshipId, currentUserId);
    }

    public boolean declineRequest(int relationshipId, int currentUserId) {
        return relationshipService.declineRequest(relationshipId, currentUserId);
    }

    public String getRelationshipState(int currentUserId, int otherUserId) {
        return relationshipService.getRelationshipState(currentUserId, otherUserId);
    }

    public String getRelationshipLabel(int currentUserId, int otherUserId) {
        return relationshipService.getRelationshipLabel(currentUserId, otherUserId);
    }

    public boolean canConnect(int currentUserId, int targetUserId) {
        return relationshipService.canConnect(currentUserId, targetUserId);
    }

    public boolean removeConnection(int currentUserId, int otherUserId) {
        return relationshipService.removeConnection(currentUserId, otherUserId);
    }

    public boolean cancelOutgoingRequest(int senderUserId, int recipientUserId) {
        return relationshipService.cancelOutgoingRequest(senderUserId, recipientUserId);
    }
}