package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.shared.interfaces.ApiExceptionHandler;
import com.experimentos.backend.shared.security.Role;
import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

abstract class ScenarioContract {
    @FunctionalInterface
    interface Checked {
        void run() throws Exception;
    }

    record Scenario(String name, boolean integration, Checked action) {}

    protected abstract List<Scenario> scenarios();

    static Scenario unit(String name, Checked action) {
        return new Scenario(name, false, action);
    }

    static Scenario integration(String name, Checked action) {
        return new Scenario(name, true, action);
    }

    @TestFactory
    Stream<DynamicTest> unit() {
        return tests(false);
    }

    @TestFactory
    Stream<DynamicTest> integration() {
        return tests(true);
    }

    private Stream<DynamicTest> tests(boolean integrated) {
        return scenarios().stream()
                .filter(s -> s.integration() == integrated)
                .map(
                        s ->
                                DynamicTest.dynamicTest(
                                        s.name(),
                                        () -> {
                                            SecurityContextHolder.clearContext();
                                            try {
                                                s.action().run();
                                            } finally {
                                                SecurityContextHolder.clearContext();
                                            }
                                        }));
    }

    static final Instant NOW = Instant.parse("2026-01-31T05:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final LocalDate DATE = LocalDate.of(2026, 1, 31);

    static void authenticate(User user) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                user.getUsername(),
                                null,
                                List.of(() -> "ROLE_" + user.getRole().name())));
    }

    static User user(long id, Role role) {
        User u = new User("actor", "actor@example.test", "test-hash", "Actor", role);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    static <T> T id(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    static void rejected(Checked action, String message) {
        assertThatThrownBy(action::run)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(message);
    }

    static MvcResult http(Object controller, String method, String path, String json, int status)
            throws Exception {
        var mapper =
                new ObjectMapper()
                        .findAndRegisterModules()
                        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var mvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new ApiExceptionHandler())
                        .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                        .build();
        var result =
                mvc.perform(
                                MockMvcRequestBuilders.request(HttpMethod.valueOf(method), path)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(json))
                        .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
        return result;
    }
}
