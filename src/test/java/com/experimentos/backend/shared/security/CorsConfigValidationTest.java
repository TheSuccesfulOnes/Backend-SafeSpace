package com.experimentos.backend.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.CorsFilter;

/** Twelve configuration units and eight local HTTP integrations, without backend credentials. */
class CorsConfigValidationTest {
    private static final String PATH = "/validation/cors";
    private static final String LOCAL = "http://localhost:5173";
    private static final String DEPLOYED = "https://frontend.example.test";

    @ParameterizedTest(name = "unit: {0}")
    @MethodSource("originCases")
    void parsesOnlyExplicitConfiguredOrigins(String name, String value, List<String> expected) {
        assertThat(configuration(value).getAllowedOrigins()).containsExactlyElementsOf(expected);
    }

    static Stream<Arguments> originCases() {
        return Stream.of(
                Arguments.of("localhost", LOCAL, List.of(LOCAL)),
                Arguments.of(
                        "loopback IP", "http://127.0.0.1:5173", List.of("http://127.0.0.1:5173")),
                Arguments.of("deployed host", DEPLOYED, List.of(DEPLOYED)),
                Arguments.of(
                        "two trimmed origins",
                        " " + LOCAL + " , " + DEPLOYED + " ",
                        List.of(LOCAL, DEPLOYED)),
                Arguments.of(
                        "empty entries omitted",
                        LOCAL + ",, ," + DEPLOYED,
                        List.of(LOCAL, DEPLOYED)),
                Arguments.of("empty configuration", "", List.of()),
                Arguments.of("blank configuration", " \t ", List.of()),
                Arguments.of("edge commas omitted", "," + LOCAL + ",", List.of(LOCAL)),
                Arguments.of("duplicates preserved", LOCAL + "," + LOCAL, List.of(LOCAL, LOCAL)),
                Arguments.of(
                        "three independent hosts",
                        LOCAL + "," + DEPLOYED + ",https://admin.example.test",
                        List.of(LOCAL, DEPLOYED, "https://admin.example.test")));
    }

    @Test
    void onlyRequiredMethodsAndHeadersAreConfigured() {
        CorsConfiguration cors = configuration(LOCAL);
        assertThat(cors.getAllowedMethods())
                .containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(cors.getAllowedHeaders()).containsExactly("Authorization", "Content-Type");
        assertThat(cors.getAllowCredentials()).isTrue();
    }

    @Test
    void credentialedWildcardOriginIsRejectedByCorsValidation() {
        assertThatThrownBy(() -> configuration("*").validateAllowCredentials())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "integration: {0}")
    @MethodSource("httpCases")
    void corsFilterEnforcesTheConfiguredBoundary(
            String name,
            String origin,
            String preflightMethod,
            String headers,
            int status,
            boolean controllerReached)
            throws Exception {
        AtomicInteger calls = new AtomicInteger();
        var mvc =
                MockMvcBuilders.standaloneSetup(new ProbeController(calls))
                        .addFilters(
                                new CorsFilter(
                                        new CorsConfig()
                                                .corsConfigurationSource(LOCAL + "," + DEPLOYED)))
                        .build();
        var builder = request(preflightMethod == null ? HttpMethod.GET : HttpMethod.OPTIONS, PATH);
        if (origin != null) builder.header("Origin", origin);
        if (preflightMethod != null)
            builder.header("Access-Control-Request-Method", preflightMethod);
        if (headers != null) builder.header("Access-Control-Request-Headers", headers);
        var response = mvc.perform(builder).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(calls.get()).isEqualTo(controllerReached ? 1 : 0);
        if (status == 200 && origin != null) {
            assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(origin);
            assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
            if (preflightMethod != null) {
                assertThat(response.getHeader("Access-Control-Allow-Methods"))
                        .contains(preflightMethod);
            }
        } else {
            assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
        }
    }

    static Stream<Arguments> httpCases() {
        return Stream.of(
                Arguments.of("allowed simple request", LOCAL, null, null, 200, true),
                Arguments.of(
                        "allowed authenticated preflight",
                        DEPLOYED,
                        "POST",
                        "authorization,content-type",
                        200,
                        false),
                Arguments.of(
                        "unknown host", "https://unknown.example.test", null, null, 403, false),
                Arguments.of("scheme mismatch", "https://localhost:5173", null, null, 403, false),
                Arguments.of("unlisted method", LOCAL, "TRACE", null, 403, false),
                Arguments.of("unlisted custom header", LOCAL, "POST", "x-user-role", 403, false),
                Arguments.of(
                        "deceptive suffix host",
                        DEPLOYED + ".attacker.test",
                        null,
                        null,
                        403,
                        false),
                Arguments.of("non CORS same origin request", null, null, null, 200, true));
    }

    private static CorsConfiguration configuration(String origins) {
        return new CorsConfig()
                .corsConfigurationSource(origins)
                .getCorsConfiguration(new MockHttpServletRequest("GET", PATH));
    }

    @RestController
    static class ProbeController {
        private final AtomicInteger calls;

        ProbeController(AtomicInteger calls) {
            this.calls = calls;
        }

        @RequestMapping(PATH)
        String probe() {
            calls.incrementAndGet();
            return "ok";
        }
    }
}
