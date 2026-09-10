package com.experimentos.backend.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Provides the authenticated identity to application use cases. */
public final class CurrentUser {
    private CurrentUser() {}

    public static String username() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("An authenticated user is required");
        }
        return authentication.getName();
    }
}
