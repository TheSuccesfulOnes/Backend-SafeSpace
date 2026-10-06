package com.experimentos.backend.shared.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Provides the authenticated identity to application use cases. */
public final class CurrentUser {
    private CurrentUser() {}

    public static String username() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("An authenticated user is required");
        }
        return authentication.getName();
    }
}
