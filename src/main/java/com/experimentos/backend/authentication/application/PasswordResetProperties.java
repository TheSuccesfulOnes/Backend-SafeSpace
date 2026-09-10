package com.experimentos.backend.authentication.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for employee password recovery. */
@ConfigurationProperties(prefix = "app.password-reset")
public record PasswordResetProperties(
        String resetUrlBase,
        int tokenExpirationMinutes,
        int maxRequestsPerWindow,
        int windowMinutes) {

    public int expirationMinutes() {
        return Math.max(5, tokenExpirationMinutes);
    }

    public int requestLimit() {
        return Math.max(1, maxRequestsPerWindow);
    }

    public int windowDurationMinutes() {
        return Math.max(1, windowMinutes);
    }

    public String urlBase() {
        return resetUrlBase == null || resetUrlBase.isBlank()
                ? "http://localhost:8080/reset-password?token="
                : resetUrlBase.trim();
    }
}
