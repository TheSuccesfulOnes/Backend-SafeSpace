package com.experimentos.backend.validation;

import static org.mockito.Mockito.mock;

import com.experimentos.backend.profile.application.ProfileService;
import com.experimentos.backend.profile.interfaces.ProfileController;
import com.experimentos.backend.profile.interfaces.ProfileDtos;
import java.util.List;

class ProfileDtosValidationTest extends DtoContract {
    private final ProfileService service = mock(ProfileService.class);

    @Override
    protected Object controller() {
        return new ProfileController(service);
    }

    @Override
    protected List<Object> services() {
        return List.of(service);
    }

    @Override
    protected List<Case> cases() {
        return List.of(
                new Case(
                        "username null",
                        ProfileDtos.UpdateAccountRequest.class,
                        "PUT",
                        "/api/v1/profile/account",
                        "{\"username\":null,\"email\":\"maria@example.com\",\"displayName\":\"Maria\"}",
                        "username",
                        false),
                new Case(
                        "username blank",
                        ProfileDtos.UpdateAccountRequest.class,
                        "PUT",
                        "/api/v1/profile/account",
                        "{\"username\":\" \",\"email\":\"maria@example.com\",\"displayName\":\"Maria\"}",
                        "username",
                        false),
                new Case(
                        "username maximum",
                        ProfileDtos.UpdateAccountRequest.class,
                        "PUT",
                        "/api/v1/profile/account",
                        "{\"username\":\"uuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu\",\"email\":\"maria@example.com\",\"displayName\":\"Maria\"}",
                        null,
                        false),
                new Case(
                        "username overflow",
                        ProfileDtos.UpdateAccountRequest.class,
                        "PUT",
                        "/api/v1/profile/account",
                        "{\"username\":\"uuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu\",\"email\":\"maria@example.com\",\"displayName\":\"Maria\"}",
                        "username",
                        false),
                new Case(
                        "optional email null",
                        ProfileDtos.UpdateAccountRequest.class,
                        "PUT",
                        "/api/v1/profile/account",
                        "{\"username\":\"maria\",\"email\":null,\"displayName\":\"Maria\"}",
                        null,
                        false),
                new Case(
                        "malformed email",
                        ProfileDtos.UpdateAccountRequest.class,
                        "PUT",
                        "/api/v1/profile/account",
                        "{\"username\":\"maria\",\"email\":\"bad\",\"displayName\":\"Maria\"}",
                        "email",
                        false),
                new Case(
                        "displayName null",
                        ProfileDtos.UpdateAccountRequest.class,
                        "PUT",
                        "/api/v1/profile/account",
                        "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"displayName\":null}",
                        "displayName",
                        false),
                new Case(
                        "displayName whitespace",
                        ProfileDtos.UpdateAccountRequest.class,
                        "PUT",
                        "/api/v1/profile/account",
                        "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"displayName\":\"\\t\"}",
                        "displayName",
                        false),
                new Case(
                        "displayName maximum",
                        ProfileDtos.UpdateAccountRequest.class,
                        "PUT",
                        "/api/v1/profile/account",
                        "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"displayName\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}",
                        null,
                        false),
                new Case(
                        "displayName overflow",
                        ProfileDtos.UpdateAccountRequest.class,
                        "PUT",
                        "/api/v1/profile/account",
                        "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"displayName\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}",
                        "displayName",
                        false),
                new Case(
                        "account valid",
                        ProfileDtos.UpdateAccountRequest.class,
                        "PUT",
                        "/api/v1/profile/account",
                        "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"displayName\":\"Maria\"}",
                        null,
                        true),
                new Case(
                        "account email object",
                        ProfileDtos.UpdateAccountRequest.class,
                        "PUT",
                        "/api/v1/profile/account",
                        "{\"username\":\"maria\",\"email\":{},\"displayName\":\"Maria\"}",
                        "binding",
                        true),
                new Case(
                        "account username array",
                        ProfileDtos.UpdateAccountRequest.class,
                        "PUT",
                        "/api/v1/profile/account",
                        "{\"username\":[],\"email\":\"maria@example.com\",\"displayName\":\"Maria\"}",
                        "binding",
                        true),
                new Case(
                        "preferences valid",
                        ProfileDtos.UpdatePreferencesRequest.class,
                        "PUT",
                        "/api/v1/profile/preferences",
                        "{\"language\":\"es\",\"theme\":\"LIGHT\"}",
                        null,
                        true),
                new Case(
                        "language null",
                        ProfileDtos.UpdatePreferencesRequest.class,
                        "PUT",
                        "/api/v1/profile/preferences",
                        "{\"language\":null,\"theme\":\"LIGHT\"}",
                        "language",
                        true),
                new Case(
                        "language empty",
                        ProfileDtos.UpdatePreferencesRequest.class,
                        "PUT",
                        "/api/v1/profile/preferences",
                        "{\"language\":\"\",\"theme\":\"LIGHT\"}",
                        "language",
                        true),
                new Case(
                        "theme null",
                        ProfileDtos.UpdatePreferencesRequest.class,
                        "PUT",
                        "/api/v1/profile/preferences",
                        "{\"language\":\"es\",\"theme\":null}",
                        "theme",
                        true),
                new Case(
                        "theme blank",
                        ProfileDtos.UpdatePreferencesRequest.class,
                        "PUT",
                        "/api/v1/profile/preferences",
                        "{\"language\":\"es\",\"theme\":\" \"}",
                        "theme",
                        true),
                new Case(
                        "preferences missing both",
                        ProfileDtos.UpdatePreferencesRequest.class,
                        "PUT",
                        "/api/v1/profile/preferences",
                        "{}",
                        "language",
                        true),
                new Case(
                        "theme object",
                        ProfileDtos.UpdatePreferencesRequest.class,
                        "PUT",
                        "/api/v1/profile/preferences",
                        "{\"language\":\"es\",\"theme\":{}}",
                        "binding",
                        true));
    }
}
