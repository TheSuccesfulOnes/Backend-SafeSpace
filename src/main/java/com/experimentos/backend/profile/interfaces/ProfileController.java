package com.experimentos.backend.profile.interfaces;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final com.experimentos.backend.profile.application.ProfileService service;

    public ProfileController(com.experimentos.backend.profile.application.ProfileService service) {
        this.service = service;
    }

    @GetMapping
    public ProfileDtos.ProfileResponse getProfile() {
        return service.getProfile();
    }

    @PutMapping("/account")
    public ProfileDtos.UpdateAccountResponse updateAccount(
            @Valid @RequestBody ProfileDtos.UpdateAccountRequest request) {
        return service.updateAccount(request);
    }

    @PutMapping("/preferences")
    public ProfileDtos.ProfileResponse updatePreferences(
            @Valid @RequestBody ProfileDtos.UpdatePreferencesRequest request) {
        return service.updatePreferences(request);
    }
}
