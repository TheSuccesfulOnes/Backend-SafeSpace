package com.experimentos.backend.authentication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/** Twenty isolated configuration checks: 12 units and 8 Spring binding integrations. */
class PasswordResetPropertiesValidationTest {
    private static final String DEFAULT_URL = "http://localhost:8080/reset-password?token=";
    private static final String TEST_URL = "https://example.invalid/reset?token=";

    @ParameterizedTest(name = "unit: {0}")
    @MethodSource("unitCases")
    void appliesSafeConfigurationBounds(
            String name, PasswordResetProperties properties, Expected expected) {
        assertConfiguration(properties, expected);
    }

    static Stream<Arguments> unitCases() {
        return Stream.of(
                unit("negative expiration", TEST_URL, -1, 3, 60, new Expected(TEST_URL, 5, 3, 60)),
                unit("zero expiration", TEST_URL, 0, 3, 60, new Expected(TEST_URL, 5, 3, 60)),
                unit(
                        "below expiration minimum",
                        TEST_URL,
                        4,
                        3,
                        60,
                        new Expected(TEST_URL, 5, 3, 60)),
                unit(
                        "exact expiration minimum",
                        TEST_URL,
                        5,
                        3,
                        60,
                        new Expected(TEST_URL, 5, 3, 60)),
                unit("positive expiration", TEST_URL, 90, 3, 60, new Expected(TEST_URL, 90, 3, 60)),
                unit(
                        "negative request limit",
                        TEST_URL,
                        15,
                        -5,
                        60,
                        new Expected(TEST_URL, 15, 1, 60)),
                unit("zero request limit", TEST_URL, 15, 0, 60, new Expected(TEST_URL, 15, 1, 60)),
                unit(
                        "positive request limit",
                        TEST_URL,
                        15,
                        8,
                        60,
                        new Expected(TEST_URL, 15, 8, 60)),
                unit("zero window", TEST_URL, 15, 3, 0, new Expected(TEST_URL, 15, 3, 1)),
                unit("positive window", TEST_URL, 15, 3, 45, new Expected(TEST_URL, 15, 3, 45)),
                unit("missing URL", null, 15, 3, 60, new Expected(DEFAULT_URL, 15, 3, 60)),
                unit(
                        "trimmed URL",
                        "  " + TEST_URL + "  ",
                        15,
                        3,
                        60,
                        new Expected(TEST_URL, 15, 3, 60)));
    }

    @ParameterizedTest(name = "integration: {0}")
    @MethodSource("bindingCases")
    void bindsRecoverySettingsWithoutSendingEmail(
            String name, Map<String, Object> values, Expected expected) {
        Binder binder = new Binder(new MapConfigurationPropertySource(values));
        if (expected == null) {
            assertThatThrownBy(
                            () ->
                                    binder.bind(
                                            "app.password-reset",
                                            Bindable.of(PasswordResetProperties.class)))
                    .isInstanceOf(BindException.class);
            return;
        }
        PasswordResetProperties properties =
                binder.bind("app.password-reset", Bindable.of(PasswordResetProperties.class))
                        .orElseThrow(
                                () -> new AssertionError("Recovery properties were not bound"));
        assertConfiguration(properties, expected);
    }

    static Stream<Arguments> bindingCases() {
        return Stream.of(
                binding(
                        "negative expiration",
                        TEST_URL,
                        "-10",
                        "3",
                        "60",
                        new Expected(TEST_URL, 5, 3, 60)),
                binding(
                        "exact expiration boundary",
                        TEST_URL,
                        "5",
                        "3",
                        "60",
                        new Expected(TEST_URL, 5, 3, 60)),
                binding(
                        "negative request limit",
                        TEST_URL,
                        "15",
                        "-2",
                        "60",
                        new Expected(TEST_URL, 15, 1, 60)),
                binding(
                        "negative window",
                        TEST_URL,
                        "15",
                        "3",
                        "-30",
                        new Expected(TEST_URL, 15, 3, 1)),
                binding("blank URL", "  ", "15", "3", "60", new Expected(DEFAULT_URL, 15, 3, 60)),
                binding(
                        "custom overrides",
                        " " + TEST_URL + " ",
                        "30",
                        "10",
                        "120",
                        new Expected(TEST_URL, 30, 10, 120)),
                binding("omitted URL", null, "15", "3", "60", new Expected(DEFAULT_URL, 15, 3, 60)),
                binding("invalid integer", TEST_URL, "not-an-integer", "3", "60", null));
    }

    private static Arguments unit(
            String name, String url, int expiration, int requests, int window, Expected expected) {
        return Arguments.of(
                name, new PasswordResetProperties(url, expiration, requests, window), expected);
    }

    private static Arguments binding(
            String name,
            String url,
            String expiration,
            String requests,
            String window,
            Expected expected) {
        var values = new java.util.LinkedHashMap<String, Object>();
        if (url != null) values.put("app.password-reset.reset-url-base", url);
        values.put("app.password-reset.token-expiration-minutes", expiration);
        values.put("app.password-reset.max-requests-per-window", requests);
        values.put("app.password-reset.window-minutes", window);
        return Arguments.of(name, values, expected);
    }

    private static void assertConfiguration(PasswordResetProperties properties, Expected expected) {
        assertThat(properties.urlBase()).isEqualTo(expected.url());
        assertThat(properties.expirationMinutes()).isEqualTo(expected.expiration());
        assertThat(properties.requestLimit()).isEqualTo(expected.requests());
        assertThat(properties.windowDurationMinutes()).isEqualTo(expected.window());
    }

    private record Expected(String url, int expiration, int requests, int window) {}
}
