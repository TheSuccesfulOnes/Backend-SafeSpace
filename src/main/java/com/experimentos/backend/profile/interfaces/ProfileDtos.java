package com.experimentos.backend.profile.interfaces;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ProfileDtos {
    private ProfileDtos() {}

    public record ProfileResponse(
            String username,
            String email,
            String displayName,
            String role,
            String language,
            String theme,
            Long userId) {}

    public record UpdateAccountRequest(
            @NotBlank @Size(max = 50) String username,
            @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 100) String displayName) {}

    public record UpdateAccountResponse(ProfileResponse profile, String token) {}

    public record UpdatePreferencesRequest(@NotBlank String language, @NotBlank String theme) {}
}
