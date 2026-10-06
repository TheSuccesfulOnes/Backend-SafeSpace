package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.experimentos.backend.activity.application.ActivityService;
import com.experimentos.backend.activity.interfaces.ActivityController;
import com.experimentos.backend.ai.application.AiChatService;
import com.experimentos.backend.ai.interfaces.AiChatController;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.mood.application.MoodService;
import com.experimentos.backend.mood.interfaces.MoodController;
import com.experimentos.backend.report.application.ReportService;
import com.experimentos.backend.report.interfaces.ReportController;
import com.experimentos.backend.shared.security.*;
import com.experimentos.backend.shared.security.Role;
import com.experimentos.backend.survey.application.SurveyService;
import com.experimentos.backend.survey.interfaces.SurveyController;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitConfig(ControllerSecurityContract.Config.class)
@WebAppConfiguration
abstract class ControllerSecurityContract {
    @Configuration
    @EnableWebMvc
    @Import({
        SecurityConfig.class,
        ActivityController.class,
        SurveyController.class,
        MoodController.class,
        ReportController.class,
        AiChatController.class
    })
    static class Config {
        @Bean
        UserRepository users() {
            return mock(UserRepository.class);
        }

        @Bean
        JwtService jwt() {
            return new JwtService(JwtServiceValidationTest.SECRET, 60);
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
        SurveyService surveys() {
            return mock(SurveyService.class);
        }

        @Bean
        MoodService moods() {
            return mock(MoodService.class);
        }

        @Bean
        ReportService reports() {
            return mock(ReportService.class);
        }

        @Bean
        AiChatService ai() {
            return mock(AiChatService.class);
        }
    }

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy chain;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired ActivityService activities;
    @Autowired SurveyService surveys;
    @Autowired MoodService moods;
    @Autowired ReportService reports;
    @Autowired AiChatService ai;

    protected abstract String path();

    protected abstract Object service();

    protected abstract boolean employeeOnly();

    record Case(
            String name,
            Role issued,
            Role current,
            boolean enabled,
            boolean exists,
            String format) {}

    List<Case> cases() {
        return List.of(
                new Case("missing bearer identity", null, null, true, false, "missing"),
                new Case(
                        "employee authorization",
                        Role.EMPLOYEE,
                        Role.EMPLOYEE,
                        true,
                        true,
                        "valid"),
                new Case("HR authorization", Role.HR_MEMBER, Role.HR_MEMBER, true, true, "valid"),
                new Case(
                        "administrator authorization",
                        Role.SYSTEM_ADMIN,
                        Role.SYSTEM_ADMIN,
                        true,
                        true,
                        "valid"),
                new Case(
                        "disabled employee blocked",
                        Role.EMPLOYEE,
                        Role.EMPLOYEE,
                        false,
                        true,
                        "valid"),
                new Case(
                        "disabled HR blocked",
                        Role.HR_MEMBER,
                        Role.HR_MEMBER,
                        false,
                        true,
                        "valid"),
                new Case(
                        "disabled administrator blocked",
                        Role.SYSTEM_ADMIN,
                        Role.SYSTEM_ADMIN,
                        false,
                        true,
                        "valid"),
                new Case(
                        "deleted employee blocked",
                        Role.EMPLOYEE,
                        Role.EMPLOYEE,
                        true,
                        false,
                        "valid"),
                new Case(
                        "deleted HR blocked", Role.HR_MEMBER, Role.HR_MEMBER, true, false, "valid"),
                new Case(
                        "deleted administrator blocked",
                        Role.SYSTEM_ADMIN,
                        Role.SYSTEM_ADMIN,
                        true,
                        false,
                        "valid"),
                new Case(
                        "malformed token blocked",
                        Role.EMPLOYEE,
                        Role.EMPLOYEE,
                        true,
                        true,
                        "malformed"),
                new Case(
                        "expired token blocked",
                        Role.SYSTEM_ADMIN,
                        Role.SYSTEM_ADMIN,
                        true,
                        true,
                        "expired"),
                new Case(
                        "wrong signing key blocked",
                        Role.SYSTEM_ADMIN,
                        Role.SYSTEM_ADMIN,
                        true,
                        true,
                        "wrongKey"),
                new Case(
                        "tampered signature blocked",
                        Role.SYSTEM_ADMIN,
                        Role.SYSTEM_ADMIN,
                        true,
                        true,
                        "tampered"),
                new Case(
                        "unsigned token blocked",
                        Role.SYSTEM_ADMIN,
                        Role.SYSTEM_ADMIN,
                        true,
                        true,
                        "unsigned"),
                new Case(
                        "promotion takes effect from stored role",
                        Role.EMPLOYEE,
                        Role.HR_MEMBER,
                        true,
                        true,
                        "valid"),
                new Case(
                        "HR demotion takes effect from stored role",
                        Role.HR_MEMBER,
                        Role.EMPLOYEE,
                        true,
                        true,
                        "valid"),
                new Case(
                        "administrator demotion takes effect from stored role",
                        Role.SYSTEM_ADMIN,
                        Role.EMPLOYEE,
                        true,
                        true,
                        "valid"),
                new Case(
                        "Basic header cannot bypass JWT",
                        Role.SYSTEM_ADMIN,
                        Role.SYSTEM_ADMIN,
                        true,
                        true,
                        "basic"),
                new Case(
                        "signed privileged claim cannot override stored employee",
                        Role.SYSTEM_ADMIN,
                        Role.EMPLOYEE,
                        true,
                        true,
                        "valid"));
    }

    @TestFactory
    Stream<DynamicTest> integration() {
        return cases().stream()
                .map(
                        c ->
                                DynamicTest.dynamicTest(
                                        c.name(),
                                        () -> {
                                            reset(users, activities, surveys, moods, reports, ai);
                                            SecurityContextHolder.clearContext();
                                            try {
                                                var request = get(path());
                                                int expected = 401;
                                                if (c.issued() != null) {
                                                    var actor =
                                                            new User(
                                                                    "actor",
                                                                    "actor@example.test",
                                                                    "synthetic-hash",
                                                                    "Actor",
                                                                    c.issued());
                                                    String token = jwt.createToken(actor);
                                                    if (c.format().equals("expired"))
                                                        token =
                                                                new JwtService(
                                                                                JwtServiceValidationTest
                                                                                        .SECRET,
                                                                                -10)
                                                                        .createToken(actor);
                                                    if (c.format().equals("wrongKey"))
                                                        token =
                                                                new JwtService(
                                                                                "different-synthetic-test-key-32-bytes-only",
                                                                                60)
                                                                        .createToken(actor);
                                                    if (c.format().equals("malformed"))
                                                        token = "invalid-token";
                                                    if (c.format().equals("unsigned"))
                                                        token =
                                                                io.jsonwebtoken.Jwts.builder()
                                                                        .subject("actor")
                                                                        .compact();
                                                    if (c.format().equals("tampered")) {
                                                        int dot = token.lastIndexOf('.');
                                                        char changed =
                                                                token.charAt(dot + 1) == 'A'
                                                                        ? 'B'
                                                                        : 'A';
                                                        token =
                                                                token.substring(0, dot + 1)
                                                                        + changed
                                                                        + token.substring(dot + 2);
                                                    }
                                                    actor.changeRole(c.current());
                                                    if (!c.enabled()) actor.disable();
                                                    when(users
                                                                    .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                                                            "actor", "actor"))
                                                            .thenReturn(
                                                                    c.exists()
                                                                            ? Optional.of(actor)
                                                                            : Optional.empty());
                                                    request.header(
                                                            "Authorization",
                                                            c.format().equals("basic")
                                                                    ? "Basic synthetic"
                                                                    : "Bearer " + token);
                                                    if (c.format().equals("valid")
                                                            && c.exists()
                                                            && c.enabled()) {
                                                        boolean allowed =
                                                                employeeOnly()
                                                                        ? c.current()
                                                                                == Role.EMPLOYEE
                                                                        : c.current()
                                                                                != Role.EMPLOYEE;
                                                        expected = allowed ? 200 : 403;
                                                    }
                                                }
                                                var result =
                                                        MockMvcBuilders.webAppContextSetup(context)
                                                                .addFilters(chain)
                                                                .build()
                                                                .perform(request)
                                                                .andReturn();
                                                assertThat(result.getResponse().getStatus())
                                                        .isEqualTo(expected);
                                                assertThat(
                                                                mockingDetails(service())
                                                                        .getInvocations())
                                                        .hasSize(expected == 200 ? 1 : 0);
                                                assertThat(result.getRequest().getSession(false))
                                                        .isNull();
                                            } finally {
                                                SecurityContextHolder.clearContext();
                                                reset(
                                                        users,
                                                        activities,
                                                        surveys,
                                                        moods,
                                                        reports,
                                                        ai);
                                            }
                                        }));
    }
}
