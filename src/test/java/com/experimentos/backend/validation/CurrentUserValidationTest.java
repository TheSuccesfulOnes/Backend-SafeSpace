package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.*;
import java.util.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserValidationTest extends ScenarioContract {
    static class Fixture {
        final java.util.concurrent.atomic.AtomicReference<String> identity =
                new java.util.concurrent.atomic.AtomicReference<>();
        boolean expired;

        void run(Role role, boolean disabled, boolean missing, boolean invalid) throws Exception {
            var actor = user(1, role);
            if (disabled) actor.disable();
            var users = mock(UserRepository.class);
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(missing ? Optional.empty() : Optional.of(actor));
            var jwt = new JwtService(JwtServiceValidationTest.SECRET, 60);
            var token =
                    expired
                            ? new JwtService(JwtServiceValidationTest.SECRET, -10)
                                    .createToken(actor)
                            : jwt.createToken(actor);
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + (invalid ? "invalid-token" : token));
            var response = new MockHttpServletResponse();
            new JwtAuthenticationFilter(jwt, users)
                    .doFilter(
                            request,
                            response,
                            (q, r) -> {
                                try {
                                    identity.set(CurrentUser.username());
                                } catch (IllegalStateException expected) {
                                    identity.set("REJECTED");
                                }
                            });
        }
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                unit(
                        "missing authentication rejected",
                        () -> {
                            assertThatThrownBy(CurrentUser::username)
                                    .isInstanceOf(IllegalStateException.class);
                        }),
                unit(
                        "unauthenticated credentials rejected",
                        () -> {
                            SecurityContextHolder.getContext()
                                    .setAuthentication(
                                            UsernamePasswordAuthenticationToken.unauthenticated(
                                                    "actor", "synthetic"));
                            assertThatThrownBy(CurrentUser::username)
                                    .isInstanceOf(IllegalStateException.class);
                        }),
                unit(
                        "context cleared after authentication rejects",
                        () -> {
                            authenticate(user(1, Role.EMPLOYEE));
                            SecurityContextHolder.clearContext();
                            assertThatThrownBy(CurrentUser::username)
                                    .isInstanceOf(IllegalStateException.class);
                        }),
                unit(
                        "employee identity accepted",
                        () -> {
                            authenticate(user(1, Role.EMPLOYEE));
                            assertThat(CurrentUser.username()).isEqualTo("actor");
                        }),
                unit(
                        "HR identity accepted independent of authorization",
                        () -> {
                            authenticate(user(1, Role.HR_MEMBER));
                            assertThat(CurrentUser.username()).isEqualTo("actor");
                        }),
                unit(
                        "administrator identity accepted",
                        () -> {
                            authenticate(user(1, Role.SYSTEM_ADMIN));
                            assertThat(CurrentUser.username()).isEqualTo("actor");
                        }),
                unit(
                        "revoked authentication rejected",
                        () -> {
                            authenticate(user(1, Role.EMPLOYEE));
                            SecurityContextHolder.getContext()
                                    .getAuthentication()
                                    .setAuthenticated(false);
                            assertThatThrownBy(CurrentUser::username)
                                    .isInstanceOf(IllegalStateException.class);
                        }),
                unit(
                        "UserDetails principal extracts stable username",
                        () -> {
                            var principal =
                                    org.springframework.security.core.userdetails.User.withUsername(
                                                    "principal-user")
                                            .password("synthetic")
                                            .roles("EMPLOYEE")
                                            .build();
                            SecurityContextHolder.getContext()
                                    .setAuthentication(
                                            UsernamePasswordAuthenticationToken.authenticated(
                                                    principal, null, List.of()));
                            assertThat(CurrentUser.username()).isEqualTo("principal-user");
                        }),
                unit(
                        "Principal identity name extracted",
                        () -> {
                            java.security.Principal principal = () -> "principal-identity";
                            SecurityContextHolder.getContext()
                                    .setAuthentication(
                                            UsernamePasswordAuthenticationToken.authenticated(
                                                    principal, null, List.of()));
                            assertThat(CurrentUser.username()).isEqualTo("principal-identity");
                        }),
                unit(
                        "new thread does not inherit identity",
                        () -> {
                            authenticate(user(1, Role.EMPLOYEE));
                            var pool = java.util.concurrent.Executors.newSingleThreadExecutor();
                            try {
                                assertThat(
                                                pool.submit(
                                                                () -> {
                                                                    try {
                                                                        CurrentUser.username();
                                                                        return false;
                                                                    } catch (
                                                                            IllegalStateException
                                                                                    expected) {
                                                                        return true;
                                                                    } finally {
                                                                        SecurityContextHolder
                                                                                .clearContext();
                                                                    }
                                                                })
                                                        .get())
                                        .isTrue();
                                assertThat(CurrentUser.username()).isEqualTo("actor");
                            } finally {
                                pool.shutdownNow();
                                assertThat(
                                                pool.awaitTermination(
                                                        5, java.util.concurrent.TimeUnit.SECONDS))
                                        .isTrue();
                            }
                        }),
                unit(
                        "switch to empty context cannot reuse identity",
                        () -> {
                            authenticate(user(1, Role.SYSTEM_ADMIN));
                            var original = SecurityContextHolder.getContext();
                            SecurityContextHolder.setContext(
                                    SecurityContextHolder.createEmptyContext());
                            assertThatThrownBy(CurrentUser::username)
                                    .isInstanceOf(IllegalStateException.class);
                            SecurityContextHolder.setContext(original);
                            assertThat(CurrentUser.username()).isEqualTo("actor");
                        }),
                unit(
                        "anonymous authentication is not an identity",
                        () -> {
                            SecurityContextHolder.getContext()
                                    .setAuthentication(
                                            new org.springframework.security.authentication
                                                    .AnonymousAuthenticationToken(
                                                    "synthetic-key",
                                                    "anonymousUser",
                                                    List.of(() -> "ROLE_ANONYMOUS")));
                            assertThatThrownBy(CurrentUser::username)
                                    .isInstanceOf(IllegalStateException.class);
                        }),
                integration(
                        "real token employee identity reaches downstream",
                        () -> {
                            var f = new Fixture();
                            f.run(Role.EMPLOYEE, false, false, false);
                            assertThat(f.identity.get()).isEqualTo("actor");
                        }),
                integration(
                        "real token HR identity reaches downstream",
                        () -> {
                            var f = new Fixture();
                            f.run(Role.HR_MEMBER, false, false, false);
                            assertThat(f.identity.get()).isEqualTo("actor");
                        }),
                integration(
                        "real token administrator identity reaches downstream",
                        () -> {
                            var f = new Fixture();
                            f.run(Role.SYSTEM_ADMIN, false, false, false);
                            assertThat(f.identity.get()).isEqualTo("actor");
                        }),
                integration(
                        "disabled token user identity rejected",
                        () -> {
                            var f = new Fixture();
                            f.run(Role.EMPLOYEE, true, false, false);
                            assertThat(f.identity.get()).isEqualTo("REJECTED");
                        }),
                integration(
                        "deleted token user identity rejected",
                        () -> {
                            var f = new Fixture();
                            f.run(Role.EMPLOYEE, false, true, false);
                            assertThat(f.identity.get()).isEqualTo("REJECTED");
                        }),
                integration(
                        "invalid token prevents downstream identity",
                        () -> {
                            var f = new Fixture();
                            f.run(Role.EMPLOYEE, false, false, true);
                            assertThat(f.identity.get()).isNull();
                            assertThatThrownBy(CurrentUser::username)
                                    .isInstanceOf(IllegalStateException.class);
                        }),
                integration(
                        "clearing request context prevents subsequent reuse",
                        () -> {
                            var f = new Fixture();
                            f.run(Role.EMPLOYEE, false, false, false);
                            SecurityContextHolder.clearContext();
                            assertThatThrownBy(CurrentUser::username)
                                    .isInstanceOf(IllegalStateException.class);
                        }),
                integration(
                        "expired token clears existing privileged identity",
                        () -> {
                            var f = new Fixture();
                            authenticate(user(99, Role.SYSTEM_ADMIN));
                            f.expired = true;
                            f.run(Role.EMPLOYEE, false, false, false);
                            assertThat(f.identity.get()).isNull();
                            assertThatThrownBy(CurrentUser::username)
                                    .isInstanceOf(IllegalStateException.class);
                        }));
    }
}
