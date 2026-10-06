package com.experimentos.backend.shared.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.experimentos.backend.activity.application.ActivityService;
import com.experimentos.backend.activity.interfaces.ActivityController;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitConfig(SecurityConfigValidationTest.Config.class)
@WebAppConfiguration
class SecurityConfigValidationTest {
    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, ActivityController.class})
    static class Config {
        @Bean
        UserRepository users() {
            return mock(UserRepository.class);
        }

        @Bean
        JwtService jwt() {
            return mock(JwtService.class);
        }

        @Bean
        JwtAuthenticationFilter filter(JwtService jwt, UserRepository users) {
            return new JwtAuthenticationFilter(jwt, users);
        }

        @Bean
        ActivityService activities() {
            return mock(ActivityService.class);
        }

        @Bean
        Probe probe() {
            return new Probe();
        }
    }

    @RestController
    static class Probe {
        @GetMapping({
            "/api/v1/auth/probe",
            "/swagger-ui/probe",
            "/api/v1/ai/probe",
            "/api/v1/admin/probe",
            "/api/v1/profile/probe"
        })
        String ok() {
            return "ok";
        }
    }

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy chain;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired ActivityService activities;
    @Autowired SecurityConfig config;

    @BeforeEach
    void resetState() {
        reset(users, jwt, activities);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearState() {
        reset(users, jwt, activities);
        SecurityContextHolder.clearContext();
    }

    User actor(Role role) {
        return new User("actor", "actor@example.test", "synthetic-hash", "Actor", role);
    }

    record Case(String path, Role role, int status) {}

    static Stream<Case> routes() {
        return Stream.of(
                new Case("/api/v1/ai/probe", Role.EMPLOYEE, 200),
                new Case("/api/v1/ai/probe", Role.HR_MEMBER, 403),
                new Case("/api/v1/ai/probe", Role.SYSTEM_ADMIN, 403),
                new Case("/api/v1/ai/probe", null, 401),
                new Case("/api/v1/admin/probe", Role.EMPLOYEE, 403),
                new Case("/api/v1/admin/probe", Role.HR_MEMBER, 403),
                new Case("/api/v1/admin/probe", Role.SYSTEM_ADMIN, 200),
                new Case("/api/v1/admin/probe", null, 401),
                new Case("/api/v1/auth/probe", null, 200),
                new Case("/swagger-ui/probe", null, 200),
                new Case("/api/v1/profile/probe", Role.EMPLOYEE, 200),
                new Case("/api/v1/profile/probe", null, 401),
                new Case("/api/v1/activities/managed", Role.EMPLOYEE, 403),
                new Case("/api/v1/activities/managed", Role.HR_MEMBER, 200),
                new Case("/api/v1/activities/managed", Role.SYSTEM_ADMIN, 200),
                new Case("/api/v1/activities/managed", null, 401));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("routes")
    void integrationRoutes(Case c) throws Exception {
        var request = get(c.path());
        if (c.role() != null) {
            when(jwt.username("synthetic")).thenReturn("actor");
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(Optional.of(actor(c.role())));
            request.header("Authorization", "Bearer synthetic");
        }
        var result =
                MockMvcBuilders.webAppContextSetup(context)
                        .addFilters(chain)
                        .build()
                        .perform(request)
                        .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(c.status());
        if (c.path().endsWith("managed")) {
            if (c.status() == 200) verify(activities).managed();
            else verifyNoInteractions(activities);
        }
        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void unitUnknownUserRejected() {
        assertThatThrownBy(() -> config.userDetailsService(users).loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void unitDisabledAccountReflectsStoredState() {
        var u = actor(Role.EMPLOYEE);
        u.disable();
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                .thenReturn(Optional.of(u));
        assertThat(config.userDetailsService(users).loadUserByUsername("actor").isEnabled())
                .isFalse();
    }

    @Test
    void unitMissingHashUsesUnmatchablePlaceholder() {
        var u = actor(Role.EMPLOYEE);
        u.changePassword(null);
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                .thenReturn(Optional.of(u));
        assertThat(config.userDetailsService(users).loadUserByUsername("actor").getPassword())
                .isEqualTo("!");
    }

    @Test
    void unitStoredAdminAuthorityRetained() {
        var u = actor(Role.SYSTEM_ADMIN);
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                .thenReturn(Optional.of(u));
        assertThat(config.userDetailsService(users).loadUserByUsername("actor").getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_SYSTEM_ADMIN");
    }
}
