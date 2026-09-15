package com.experimentos.backend.admin.interfaces;

import com.experimentos.backend.shared.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AdminDtos {
    private AdminDtos() {}

    public record CreateUserRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 100) String displayName,
            @NotNull Role role) {}

    public record CreateHrRequest(
            @NotBlank @Size(max = 50) String username,
            @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 100) String displayName) {}

    public record UpdateUserRequest(
            @Size(max = 50) String username,
            @Email @Size(max = 255) String email,
            @Size(max = 100) String displayName,
            Role role) {}

    public record ResetPasswordRequest(@NotBlank @Size(min = 8, max = 72) String newPassword) {}

    public record UserSummary(
            Long id,
            String username,
            String email,
            String displayName,
            Role role,
            boolean enabled,
            boolean systemOwner) {
        public static UserSummary from(com.experimentos.backend.iam.domain.User user) {
            return new UserSummary(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getRole(),
                    user.isEnabled(),
                    user.isSystemOwner());
        }
    }
}
