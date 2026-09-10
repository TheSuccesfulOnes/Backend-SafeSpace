package com.experimentos.backend.profile.application;

import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.domain.UserPreferences;
import com.experimentos.backend.iam.infrastructure.UserPreferencesRepository;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.profile.interfaces.ProfileDtos;
import com.experimentos.backend.shared.domain.Theme;
import com.experimentos.backend.shared.security.CurrentUser;
import com.experimentos.backend.shared.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final UserRepository users;
    private final UserPreferencesRepository preferences;
    private final JwtService jwtService;

    public ProfileService(
            UserRepository users, UserPreferencesRepository preferences, JwtService jwtService) {
        this.users = users;
        this.preferences = preferences;
        this.jwtService = jwtService;
    }

    @Transactional
    public ProfileDtos.UpdateAccountResponse updateAccount(
            ProfileDtos.UpdateAccountRequest request) {
        User user = findCurrentUser();
        String username = request.username().trim();
        String email = normalizeOptionalEmail(request.email());
        String displayName = request.displayName().trim();

        if (user.getRole() == com.experimentos.backend.shared.security.Role.EMPLOYEE
                && (email == null || email.isBlank())) {
            throw new IllegalArgumentException("Email is required for employees");
        }

        if (users.existsByUsernameIgnoreCaseAndIdNot(username, user.getId())) {
            throw new IllegalArgumentException("Username is already in use");
        }
        if (email != null && users.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        user.updateProfile(username, email, displayName);
        users.save(user);

        // Reissue the token because the username is the JWT subject and may have changed.
        return new ProfileDtos.UpdateAccountResponse(
                toProfileResponse(user), jwtService.createToken(user));
    }

    @Transactional(readOnly = true)
    public ProfileDtos.ProfileResponse getProfile() {
        User user = findCurrentUser();
        UserPreferences preference =
                preferences.findById(user.getId()).orElseGet(() -> new UserPreferences(user));
        return toProfileResponse(user, preference);
    }

    private ProfileDtos.ProfileResponse toProfileResponse(User user) {
        UserPreferences preference =
                preferences.findById(user.getId()).orElseGet(() -> new UserPreferences(user));
        return toProfileResponse(user, preference);
    }

    private ProfileDtos.ProfileResponse toProfileResponse(User user, UserPreferences preference) {
        return new ProfileDtos.ProfileResponse(
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                preference.getLanguage(),
                preference.getTheme().name(),
                user.getId());
    }

    @Transactional
    public ProfileDtos.ProfileResponse updatePreferences(
            ProfileDtos.UpdatePreferencesRequest request) {
        User user = findCurrentUser();
        UserPreferences preference =
                preferences.findById(user.getId()).orElseGet(() -> new UserPreferences(user));
        preference.update(request.language(), Theme.valueOf(request.theme().toUpperCase()));
        preferences.save(preference);
        return getProfile();
    }

    private User findCurrentUser() {
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        CurrentUser.username(), CurrentUser.username())
                .orElseThrow(
                        () -> new IllegalArgumentException("Authenticated user was not found"));
    }

    private String normalizeOptionalEmail(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase();
    }
}
