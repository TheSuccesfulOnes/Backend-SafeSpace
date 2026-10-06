package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.iam.domain.*;
import com.experimentos.backend.iam.infrastructure.*;
import com.experimentos.backend.profile.application.*;
import com.experimentos.backend.profile.interfaces.*;
import com.experimentos.backend.shared.domain.Theme;
import com.experimentos.backend.shared.security.*;
import java.util.*;
import org.springframework.security.core.context.SecurityContextHolder;

class ProfileServiceValidationTest extends ScenarioContract {
    static final String ACCOUNT =
            "{\"username\":\"actor\",\"email\":\"actor@example.test\",\"display_name\":\"Actor\"}";

    static class Fixture {
        final UserRepository users = mock(UserRepository.class);
        final UserPreferencesRepository preferences = mock(UserPreferencesRepository.class);
        final JwtService jwt = mock(JwtService.class);
        final User actor = user(1, Role.EMPLOYEE);
        final ProfileService service = new ProfileService(users, preferences, jwt);
        final ProfileController controller = new ProfileController(service);

        Fixture() {
            authenticate(actor);
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(Optional.of(actor));
            when(jwt.createToken(any())).thenReturn("new-token");
        }

        void noSave() {
            verify(users, never()).save(any());
            verifyNoInteractions(jwt);
        }
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                unit(
                        "unsupported language rejected before save",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.service.updatePreferences(
                                                    new ProfileDtos.UpdatePreferencesRequest(
                                                            "fr", "LIGHT")),
                                    "Language must be es or en");
                            verify(f.preferences, never()).save(any());
                        }),
                unit(
                        "language normalization retains supported contract",
                        () -> {
                            var f = new Fixture();
                            f.service.updatePreferences(
                                    new ProfileDtos.UpdatePreferencesRequest(" EN ", "DARK"));
                            verify(f.preferences).save(argThat(p -> p.getLanguage().equals("en")));
                        }),
                integration(
                        "unsupported language returns400",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "PUT",
                                    "/api/v1/profile/preferences",
                                    "{\"language\":\"fr\",\"theme\":\"LIGHT\"}",
                                    400);
                            verify(f.preferences, never()).save(any());
                        }),
                integration(
                        "Spanish normalized before HTTP persistence",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "PUT",
                                    "/api/v1/profile/preferences",
                                    "{\"language\":\" ES \",\"theme\":\"LIGHT\"}",
                                    200);
                            verify(f.preferences).save(argThat(p -> p.getLanguage().equals("es")));
                        }),
                unit(
                        "employee missing email",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.service.updateAccount(
                                                    new ProfileDtos.UpdateAccountRequest(
                                                            "actor", null, "Actor")),
                                    "Email is required");
                            f.noSave();
                        }),
                unit(
                        "employee blank email",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.service.updateAccount(
                                                    new ProfileDtos.UpdateAccountRequest(
                                                            "actor", " ", "Actor")),
                                    "Email is required");
                            f.noSave();
                        }),
                unit(
                        "duplicate normalized username",
                        () -> {
                            var f = new Fixture();
                            when(f.users.existsByUsernameIgnoreCaseAndIdNot("taken", 1L))
                                    .thenReturn(true);
                            rejected(
                                    () ->
                                            f.service.updateAccount(
                                                    new ProfileDtos.UpdateAccountRequest(
                                                            " taken ",
                                                            "actor@example.test",
                                                            "Actor")),
                                    "Username");
                            f.noSave();
                        }),
                unit(
                        "duplicate normalized email",
                        () -> {
                            var f = new Fixture();
                            when(f.users.existsByEmailIgnoreCaseAndIdNot("taken@example.test", 1L))
                                    .thenReturn(true);
                            rejected(
                                    () ->
                                            f.service.updateAccount(
                                                    new ProfileDtos.UpdateAccountRequest(
                                                            "actor",
                                                            " TAKEN@EXAMPLE.TEST ",
                                                            "Actor")),
                                    "Email");
                            f.noSave();
                        }),
                unit(
                        "unknown current user",
                        () -> {
                            var f = new Fixture();
                            when(f.users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                            "actor", "actor"))
                                    .thenReturn(Optional.empty());
                            rejected(() -> f.service.getProfile(), "Authenticated user");
                        }),
                unit(
                        "unauthenticated profile",
                        () -> {
                            var f = new Fixture();
                            SecurityContextHolder.clearContext();
                            assertThatThrownBy(f.service::getProfile)
                                    .isInstanceOf(IllegalStateException.class);
                        }),
                unit(
                        "HR optional email",
                        () -> {
                            var f = new Fixture();
                            f.actor.changeRole(Role.HR_MEMBER);
                            var r =
                                    f.service.updateAccount(
                                            new ProfileDtos.UpdateAccountRequest(
                                                    "actor", null, "Actor"));
                            assertThat(r.profile().email()).isNull();
                            verify(f.jwt).createToken(f.actor);
                        }),
                unit(
                        "admin optional blank email",
                        () -> {
                            var f = new Fixture();
                            f.actor.changeRole(Role.SYSTEM_ADMIN);
                            assertThat(
                                            f.service
                                                    .updateAccount(
                                                            new ProfileDtos.UpdateAccountRequest(
                                                                    "actor", " ", "Actor"))
                                                    .profile()
                                                    .email())
                                    .isNull();
                        }),
                unit(
                        "new username requires new subject token",
                        () -> {
                            var f = new Fixture();
                            var r =
                                    f.service.updateAccount(
                                            new ProfileDtos.UpdateAccountRequest(
                                                    " new-name ",
                                                    " ACTOR@EXAMPLE.TEST ",
                                                    " New Person "));
                            assertThat(r.profile().username()).isEqualTo("new-name");
                            verify(f.jwt)
                                    .createToken(argThat(u -> u.getUsername().equals("new-name")));
                        }),
                unit(
                        "invalid theme blocked before persistence",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.service.updatePreferences(
                                                    new ProfileDtos.UpdatePreferencesRequest(
                                                            "es", "invalid")),
                                    "No enum constant");
                            verify(f.preferences, never()).save(any());
                        }),
                unit(
                        "lowercase theme normalized",
                        () -> {
                            var f = new Fixture();
                            f.service.updatePreferences(
                                    new ProfileDtos.UpdatePreferencesRequest("en", "dark"));
                            verify(f.preferences)
                                    .save(
                                            argThat(
                                                    p ->
                                                            p.getTheme() == Theme.DARK
                                                                    && p.getLanguage()
                                                                            .equals("en")));
                        }),
                unit(
                        "missing preferences default Spanish light",
                        () -> {
                            var f = new Fixture();
                            var r = f.service.getProfile();
                            assertThat(r.language()).isEqualTo("es");
                            assertThat(r.theme()).isEqualTo("LIGHT");
                        }),
                integration(
                        "account snake case integration",
                        () -> {
                            var f = new Fixture();
                            var r =
                                    http(
                                            f.controller,
                                            "PUT",
                                            "/api/v1/profile/account",
                                            ACCOUNT,
                                            200);
                            assertThat(r.getResponse().getContentAsString()).contains("new-token");
                            verify(f.users).save(f.actor);
                        }),
                integration(
                        "employee empty email through HTTP",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "PUT",
                                    "/api/v1/profile/account",
                                    "{\"username\":\"actor\",\"display_name\":\"Actor\"}",
                                    400);
                            f.noSave();
                        }),
                integration(
                        "username conflict HTTP",
                        () -> {
                            var f = new Fixture();
                            when(f.users.existsByUsernameIgnoreCaseAndIdNot("actor", 1L))
                                    .thenReturn(true);
                            http(f.controller, "PUT", "/api/v1/profile/account", ACCOUNT, 400);
                            f.noSave();
                        }),
                integration(
                        "email conflict HTTP",
                        () -> {
                            var f = new Fixture();
                            when(f.users.existsByEmailIgnoreCaseAndIdNot("actor@example.test", 1L))
                                    .thenReturn(true);
                            http(f.controller, "PUT", "/api/v1/profile/account", ACCOUNT, 400);
                            f.noSave();
                        }),
                integration(
                        "light preferences persist HTTP",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "PUT",
                                    "/api/v1/profile/preferences",
                                    "{\"language\":\"es\",\"theme\":\"LIGHT\"}",
                                    200);
                            verify(f.preferences).save(argThat(p -> p.getTheme() == Theme.LIGHT));
                        }),
                integration(
                        "dark preferences persist HTTP",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "PUT",
                                    "/api/v1/profile/preferences",
                                    "{\"language\":\"en\",\"theme\":\"DARK\"}",
                                    200);
                            verify(f.preferences).save(argThat(p -> p.getTheme() == Theme.DARK));
                        }),
                integration(
                        "invalid theme HTTP",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "PUT",
                                    "/api/v1/profile/preferences",
                                    "{\"language\":\"es\",\"theme\":\"blue\"}",
                                    400);
                            verify(f.preferences, never()).save(any());
                        }),
                integration(
                        "unknown user HTTP",
                        () -> {
                            var f = new Fixture();
                            when(f.users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                            "actor", "actor"))
                                    .thenReturn(Optional.empty());
                            http(f.controller, "GET", "/api/v1/profile", "", 400);
                            f.noSave();
                        }));
    }
}
