package com.syndicati.utils.security;

import com.syndicati.models.user.User;
import com.syndicati.utils.session.SessionManager;

import java.util.Locale;
import java.util.Set;

/**
 * Centralized access-control helpers that mirror Horizon website behavior.
 */
public final class AccessControlService {

    private static final Set<String> ADMIN_AREA_ROLES = Set.of("OWNER", "ADMIN", "SYNDIC", "SUPERADMIN");

    private AccessControlService() {
    }

    public static boolean isLoggedIn() {
        return SessionManager.getInstance().isLoggedIn();
    }

    public static boolean canAccessProfile() {
        return isLoggedIn();
    }

    public static boolean canAccessAdminArea() {
        User user = SessionManager.getInstance().getCurrentUser();
        return canAccessAdminArea(user);
    }

    public static boolean canAccessAdminArea(User user) {
        if (user == null) {
            return false;
        }
        String normalizedRole = normalizeRole(user.getRoleUser());
        return ADMIN_AREA_ROLES.contains(normalizedRole);
    }

    public static String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String clean = role.trim().toUpperCase(Locale.ROOT);
        if (clean.startsWith("ROLE_")) {
            clean = clean.substring(5);
        }
        return clean;
    }
}
