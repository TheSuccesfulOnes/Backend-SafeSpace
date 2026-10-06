package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.iam.domain.*;
import com.experimentos.backend.iam.infrastructure.*;
import com.experimentos.backend.shared.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterValidationTest extends ScenarioContract {
    static class Fixture {
        JwtService jwt = mock(JwtService.class);
        final UserRepository users = mock(UserRepository.class);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final AtomicInteger calls = new AtomicInteger();
        User actor;
        String token;

        void valid(Role role) {
            actor = user(1, role);
            when(jwt.username("synthetic")).thenReturn("actor");
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(Optional.of(actor));
        }

        void real(Role role) {
            actor = user(1, role);
            jwt = new JwtService(JwtServiceValidationTest.SECRET, 60);
            token = jwt.createToken(actor);
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(Optional.of(actor));
        }

        void run(String header) throws Exception {
            if (header != null) request.addHeader("Authorization", header);
            new JwtAuthenticationFilter(jwt, users)
                    .doFilter(request, response, (q, r) -> calls.incrementAndGet());
        }

        void anonymous() {
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                unit(
                        "no authorization header continues anonymously",
                        () -> {
                            var f = new Fixture();
                            f.run(null);
                            f.anonymous();
                            assertThat(f.calls.get()).isEqualTo(1);
                            verifyNoInteractions(f.jwt, f.users);
                        }),
                unit(
                        "Basic authentication ignored",
                        () -> {
                            var f = new Fixture();
                            f.run("Basic synthetic");
                            f.anonymous();
                            verifyNoInteractions(f.jwt, f.users);
                        }),
                unit(
                        "lowercase bearer ignored",
                        () -> {
                            var f = new Fixture();
                            f.run("bearer synthetic");
                            f.anonymous();
                            verifyNoInteractions(f.jwt, f.users);
                        }),
                unit(
                        "wrong prefix ignored",
                        () -> {
                            var f = new Fixture();
                            f.run("Token synthetic");
                            f.anonymous();
                            verifyNoInteractions(f.jwt, f.users);
                        }),
                unit(
                        "valid enabled employee installed",
                        () -> {
                            var f = new Fixture();
                            f.valid(Role.EMPLOYEE);
                            f.run("Bearer synthetic");
                            assertThat(CurrentUser.username()).isEqualTo("actor");
                            assertThat(
                                            SecurityContextHolder.getContext()
                                                    .getAuthentication()
                                                    .getAuthorities())
                                    .extracting("authority")
                                    .containsExactly("ROLE_EMPLOYEE");
                        }),
                unit(
                        "repository role wins over token role HR",
                        () -> {
                            var f = new Fixture();
                            f.valid(Role.HR_MEMBER);
                            f.run("Bearer synthetic");
                            assertThat(
                                            SecurityContextHolder.getContext()
                                                    .getAuthentication()
                                                    .getAuthorities())
                                    .extracting("authority")
                                    .containsExactly("ROLE_HR_MEMBER");
                        }),
                unit(
                        "repository role wins over token role admin",
                        () -> {
                            var f = new Fixture();
                            f.valid(Role.SYSTEM_ADMIN);
                            f.run("Bearer synthetic");
                            assertThat(
                                            SecurityContextHolder.getContext()
                                                    .getAuthentication()
                                                    .getAuthorities())
                                    .extracting("authority")
                                    .containsExactly("ROLE_SYSTEM_ADMIN");
                        }),
                unit(
                        "disabled user cannot authenticate",
                        () -> {
                            var f = new Fixture();
                            f.valid(Role.EMPLOYEE);
                            f.actor.disable();
                            f.run("Bearer synthetic");
                            f.anonymous();
                            assertThat(f.calls.get()).isEqualTo(1);
                        }),
                unit(
                        "unknown user remains anonymous",
                        () -> {
                            var f = new Fixture();
                            when(f.jwt.username("synthetic")).thenReturn("unknown");
                            f.run("Bearer synthetic");
                            f.anonymous();
                            assertThat(f.calls.get()).isEqualTo(1);
                        }),
                unit(
                        "token exception returns401 stops chain",
                        () -> {
                            var f = new Fixture();
                            when(f.jwt.username("synthetic"))
                                    .thenThrow(new IllegalArgumentException("synthetic"));
                            f.run("Bearer synthetic");
                            assertThat(f.response.getStatus()).isEqualTo(401);
                            assertThat(f.calls.get()).isZero();
                        }),
                unit(
                        "invalid token clears previous identity",
                        () -> {
                            var f = new Fixture();
                            authenticate(user(9, Role.SYSTEM_ADMIN));
                            when(f.jwt.username("synthetic"))
                                    .thenThrow(new IllegalArgumentException("synthetic"));
                            f.run("Bearer synthetic");
                            f.anonymous();
                        }),
                unit(
                        "repository exception clears context401",
                        () -> {
                            var f = new Fixture();
                            f.valid(Role.EMPLOYEE);
                            when(f.users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                            "actor", "actor"))
                                    .thenThrow(new IllegalStateException("synthetic"));
                            f.run("Bearer synthetic");
                            f.anonymous();
                            assertThat(f.response.getStatus()).isEqualTo(401);
                        }),
                integration(
                        "real JWT employee and filter",
                        () -> {
                            var f = new Fixture();
                            f.real(Role.EMPLOYEE);
                            f.run("Bearer " + f.token);
                            assertThat(CurrentUser.username()).isEqualTo("actor");
                        }),
                integration(
                        "real JWT HR and filter",
                        () -> {
                            var f = new Fixture();
                            f.real(Role.HR_MEMBER);
                            f.run("Bearer " + f.token);
                            assertThat(
                                            SecurityContextHolder.getContext()
                                                    .getAuthentication()
                                                    .getAuthorities())
                                    .extracting("authority")
                                    .containsExactly("ROLE_HR_MEMBER");
                        }),
                integration(
                        "real JWT admin and filter",
                        () -> {
                            var f = new Fixture();
                            f.real(Role.SYSTEM_ADMIN);
                            f.run("Bearer " + f.token);
                            assertThat(
                                            SecurityContextHolder.getContext()
                                                    .getAuthentication()
                                                    .getAuthorities())
                                    .extracting("authority")
                                    .containsExactly("ROLE_SYSTEM_ADMIN");
                        }),
                integration(
                        "real expired token401",
                        () -> {
                            var f = new Fixture();
                            f.real(Role.EMPLOYEE);
                            f.token =
                                    new JwtService(JwtServiceValidationTest.SECRET, -10)
                                            .createToken(f.actor);
                            f.run("Bearer " + f.token);
                            assertThat(f.response.getStatus()).isEqualTo(401);
                            assertThat(f.calls.get()).isZero();
                        }),
                integration(
                        "real wrong signing key401",
                        () -> {
                            var f = new Fixture();
                            f.real(Role.EMPLOYEE);
                            f.token =
                                    new JwtService("another-synthetic-test-key-with-32-bytes", 60)
                                            .createToken(f.actor);
                            f.run("Bearer " + f.token);
                            assertThat(f.response.getStatus()).isEqualTo(401);
                            f.anonymous();
                        }),
                integration(
                        "real token disabled after issue",
                        () -> {
                            var f = new Fixture();
                            f.real(Role.EMPLOYEE);
                            f.actor.disable();
                            f.run("Bearer " + f.token);
                            f.anonymous();
                            assertThat(f.calls.get()).isEqualTo(1);
                        }),
                integration(
                        "real token user deleted after issue",
                        () -> {
                            var f = new Fixture();
                            f.real(Role.EMPLOYEE);
                            when(f.users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                            "actor", "actor"))
                                    .thenReturn(Optional.empty());
                            f.run("Bearer " + f.token);
                            f.anonymous();
                        }),
                integration(
                        "real token role changed after issue",
                        () -> {
                            var f = new Fixture();
                            f.real(Role.SYSTEM_ADMIN);
                            f.actor.changeRole(Role.EMPLOYEE);
                            f.run("Bearer " + f.token);
                            assertThat(
                                            SecurityContextHolder.getContext()
                                                    .getAuthentication()
                                                    .getAuthorities())
                                    .extracting("authority")
                                    .containsExactly("ROLE_EMPLOYEE");
                        }));
    }
}
