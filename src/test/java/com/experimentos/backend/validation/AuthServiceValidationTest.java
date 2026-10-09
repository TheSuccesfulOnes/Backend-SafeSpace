package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.authentication.application.*;
import com.experimentos.backend.authentication.interfaces.*;
import com.experimentos.backend.iam.domain.*;
import com.experimentos.backend.iam.infrastructure.*;
import com.experimentos.backend.shared.security.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceValidationTest extends ScenarioContract {
    static class Fixture {
        final UserRepository users = mock(UserRepository.class);
        final JwtService jwt = mock(JwtService.class);
        final PasswordEncoder encoder = mock(PasswordEncoder.class);
        final AuthService service = new AuthService(users, encoder, jwt);
        final PasswordResetService reset = mock(PasswordResetService.class);
        final AuthController controller = new AuthController(service, reset);
        final User actor = user(1, Role.EMPLOYEE);

        Fixture() {
            when(encoder.encode(anyString())).thenReturn("encoded-test");
            when(users.save(any())).thenAnswer(i -> id(i.getArgument(0), 1));
            when(jwt.createToken(any())).thenReturn("synthetic-token");
        }

        void loginUser() {
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(Optional.of(actor));
            when(encoder.matches("Password1!", actor.getPasswordHash())).thenReturn(true);
        }

        AuthDtos.RegisterRequest registration() {
            return new AuthDtos.RegisterRequest(
                    "actor", "actor@example.test", "Password1!", "Password1!", "Actor");
        }

        void noToken() {
            verify(jwt, never()).createToken(any());
        }
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                unit(
                        "duplicate username prevents hashing",
                        () -> {
                            var f = new Fixture();
                            when(f.users.existsByUsernameIgnoreCase("actor")).thenReturn(true);
                            rejected(() -> f.service.register(f.registration()), "Username");
                            verifyNoInteractions(f.encoder);
                            f.noToken();
                        }),
                unit(
                        "duplicate email prevents hashing",
                        () -> {
                            var f = new Fixture();
                            when(f.users.existsByEmailIgnoreCase("actor@example.test"))
                                    .thenReturn(true);
                            rejected(() -> f.service.register(f.registration()), "Email");
                            verifyNoInteractions(f.encoder);
                        }),
                unit(
                        "missing account generic credentials",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.service.login(
                                                    new AuthDtos.LoginRequest("unknown", "secret")),
                                    "Invalid credentials");
                            f.noToken();
                        }),
                unit(
                        "disabled account cannot obtain token",
                        () -> {
                            var f = new Fixture();
                            f.loginUser();
                            f.actor.disable();
                            rejected(
                                    () ->
                                            f.service.login(
                                                    new AuthDtos.LoginRequest(
                                                            "actor", "Password1!")),
                                    "Invalid credentials");
                            f.noToken();
                        }),
                unit(
                        "account without password cannot login",
                        () -> {
                            var f = new Fixture();
                            f.loginUser();
                            f.actor.changePassword(null);
                            rejected(
                                    () ->
                                            f.service.login(
                                                    new AuthDtos.LoginRequest(
                                                            "actor", "Password1!")),
                                    "Invalid credentials");
                            f.noToken();
                        }),
                unit(
                        "legacy missing displayName uses username",
                        () -> {
                            var f = new Fixture();
                            var r =
                                    f.service.register(
                                            new AuthDtos.RegisterRequest(
                                                    "actor",
                                                    "actor@example.test",
                                                    "Password1!",
                                                    "Password1!"));
                            assertThat(r.displayName()).isEqualTo("actor");
                        }),
                unit(
                        "blank displayName uses normalized username",
                        () -> {
                            var f = new Fixture();
                            var r =
                                    f.service.register(
                                            new AuthDtos.RegisterRequest(
                                                    " actor ",
                                                    "ACTOR@EXAMPLE.TEST",
                                                    "Password1!",
                                                    "Password1!",
                                                    " "));
                            assertThat(r.username()).isEqualTo("actor");
                            assertThat(r.displayName()).isEqualTo("actor");
                            verify(f.users)
                                    .save(argThat(u -> u.getEmail().equals("actor@example.test")));
                        }),
                unit(
                        "registration cannot select privileged role",
                        () -> {
                            var f = new Fixture();
                            assertThat(f.service.register(f.registration()).role())
                                    .isEqualTo("EMPLOYEE");
                        }),
                unit(
                        "login employee grants employee response",
                        () -> {
                            var f = new Fixture();
                            f.loginUser();
                            assertThat(
                                            f.service
                                                    .login(
                                                            new AuthDtos.LoginRequest(
                                                                    "actor", "Password1!"))
                                                    .role())
                                    .isEqualTo("EMPLOYEE");
                        }),
                unit(
                        "login HR retains stored role",
                        () -> {
                            var f = new Fixture();
                            f.loginUser();
                            f.actor.changeRole(Role.HR_MEMBER);
                            assertThat(
                                            f.service
                                                    .login(
                                                            new AuthDtos.LoginRequest(
                                                                    "actor", "Password1!"))
                                                    .role())
                                    .isEqualTo("HR_MEMBER");
                        }),
                unit(
                        "login admin retains stored role",
                        () -> {
                            var f = new Fixture();
                            f.loginUser();
                            f.actor.changeRole(Role.SYSTEM_ADMIN);
                            assertThat(
                                            f.service
                                                    .login(
                                                            new AuthDtos.LoginRequest(
                                                                    "actor", "Password1!"))
                                                    .role())
                                    .isEqualTo("SYSTEM_ADMIN");
                        }),
                unit(
                        "email login uses repository identity",
                        () -> {
                            var f = new Fixture();
                            when(f.users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                            "actor@example.test", "actor@example.test"))
                                    .thenReturn(Optional.of(f.actor));
                            when(f.encoder.matches("Password1!", f.actor.getPasswordHash()))
                                    .thenReturn(true);
                            assertThat(
                                            f.service
                                                    .login(
                                                            new AuthDtos.LoginRequest(
                                                                    "actor@example.test",
                                                                    "Password1!"))
                                                    .username())
                                    .isEqualTo("actor");
                        }),
                integration(
                        "register JSON snake case roundtrip",
                        () -> {
                            var f = new Fixture();
                            var r =
                                    http(
                                            f.controller,
                                            "POST",
                                            "/api/v1/auth/register",
                                            "{\"username\":\"actor\",\"email\":\"actor@example.test\",\"password\":\"Password1!\",\"confirm_password\":\"Password1!\",\"display_name\":\"Person\"}",
                                            201);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"display_name\":\"Person\"");
                            verify(f.users).save(any());
                        }),
                integration(
                        "password mismatch returns400 without save",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/auth/register",
                                    "{\"username\":\"actor\",\"email\":\"actor@example.test\",\"password\":\"Password1!\",\"confirm_password\":\"Different1!\"}",
                                    400);
                            verify(f.users, never()).save(any());
                        }),
                integration(
                        "weak password passes size but rejected by policy",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/auth/register",
                                    "{\"username\":\"actor\",\"email\":\"actor@example.test\",\"password\":\"password1\",\"confirm_password\":\"password1\"}",
                                    400);
                            verify(f.users, never()).save(any());
                        }),
                integration(
                        "username conflict through controller",
                        () -> {
                            var f = new Fixture();
                            when(f.users.existsByUsernameIgnoreCase("actor")).thenReturn(true);
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/auth/register",
                                    "{\"username\":\"actor\",\"email\":\"actor@example.test\",\"password\":\"Password1!\",\"confirm_password\":\"Password1!\"}",
                                    400);
                            f.noToken();
                        }),
                integration(
                        "email conflict through controller",
                        () -> {
                            var f = new Fixture();
                            when(f.users.existsByEmailIgnoreCase("actor@example.test"))
                                    .thenReturn(true);
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/auth/register",
                                    "{\"username\":\"actor\",\"email\":\"actor@example.test\",\"password\":\"Password1!\",\"confirm_password\":\"Password1!\"}",
                                    400);
                            f.noToken();
                        }),
                integration(
                        "login real service issues token",
                        () -> {
                            var f = new Fixture();
                            f.loginUser();
                            var r =
                                    http(
                                            f.controller,
                                            "POST",
                                            "/api/v1/auth/login",
                                            "{\"identifier\":\"actor\",\"password\":\"Password1!\"}",
                                            200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("synthetic-token");
                        }),
                integration(
                        "disabled login returns400",
                        () -> {
                            var f = new Fixture();
                            f.loginUser();
                            f.actor.disable();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/auth/login",
                                    "{\"identifier\":\"actor\",\"password\":\"Password1!\"}",
                                    400);
                            f.noToken();
                        }),
                integration(
                        "username login HTTP trims identifier",
                        () -> {
                            var f = new Fixture();
                            f.loginUser();
                            var r =
                                    http(
                                            f.controller,
                                            "POST",
                                            "/api/v1/auth/login",
                                            "{\"identifier\":\" actor \",\"password\":\"Password1!\"}",
                                            200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("synthetic-token");
                            verify(f.users)
                                    .findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor");
                        }),
                integration(
                        "email login HTTP trims identifier and preserves password",
                        () -> {
                            var f = new Fixture();
                            when(f.users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                            "actor@example.test", "actor@example.test"))
                                    .thenReturn(Optional.of(f.actor));
                            when(f.encoder.matches(" Password1! ", f.actor.getPasswordHash()))
                                    .thenReturn(true);
                            var r =
                                    http(
                                            f.controller,
                                            "POST",
                                            "/api/v1/auth/login",
                                            "{\"identifier\":\" actor@example.test \",\"password\":\" Password1! \"}",
                                            200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("synthetic-token");
                            verify(f.encoder).matches(" Password1! ", f.actor.getPasswordHash());
                        }),
                integration(
                        "blank login identifier HTTP rejects before account lookup",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/auth/login",
                                    "{\"identifier\":\"   \",\"password\":\"Password1!\"}",
                                    400);
                            verifyNoInteractions(f.users);
                            f.noToken();
                        }),
                integration(
                        "wrong password returns400",
                        () -> {
                            var f = new Fixture();
                            f.loginUser();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/auth/login",
                                    "{\"identifier\":\"actor\",\"password\":\"Wrong1!\"}",
                                    400);
                            f.noToken();
                        }));
    }
}
