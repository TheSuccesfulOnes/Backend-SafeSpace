package com.experimentos.backend.authentication.interfaces;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 72) String confirmPassword,
            @Size(max = 100) String displayName) {
        /** Keeps compatibility with clients that predate the display name field. */
        public RegisterRequest(
                String username, String email, String password, String confirmPassword) {
            this(username, email, password, confirmPassword, null);
        }
    }

    public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {}

    public record PasswordRecoveryRequest(@NotBlank @Size(max = 255) String identifier) {}

    public record PasswordResetConfirmRequest(
            @NotBlank @Size(max = 256) String token,
            @NotBlank @Size(min = 8, max = 72) String newPassword,
            @NotBlank @Size(max = 72) String confirmPassword) {}

    public record PasswordRecoveryResponse(String message) {}

    public record AuthResponse(
            String token, String username, String displayName, String role, Long userId) {}
}
