package com.learntrix.edtech.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class SecurityUtil {

    private SecurityUtil() {}

    private static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static boolean isAuthenticated() {
        Authentication auth = getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    public static UUID getCurrentUserId() {
        Authentication auth = getAuthentication();
        if (!isAuthenticated() || auth == null || auth.getName() == null) {
            return null;
        }

        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String getCurrentUsername() {
        Authentication auth = getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    public static Set<String> getCurrentUserRoles() {
        Authentication auth = getAuthentication();
        if (auth == null) {
            return Collections.emptySet();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    public static boolean hasRole(String role) {
        Set<String> roles = getCurrentUserRoles();
        return roles.contains(role) || roles.contains("ROLE_" + role);
    }
}
