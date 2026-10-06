package com.experimentos.backend.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/** Twenty isolated configuration checks: 12 units and 8 Spring binding integrations. */
class AiPropertiesValidationTest {
    private static final String TEST_KEY = "test-only-not-a-provider-key";

    @ParameterizedTest(name = "unit: {0}")
    @MethodSource("unitCases")
    void normalizesConfiguration(String name, AiProperties input, Expected expected) {
        assertConfiguration(input, expected);
    }

    static Stream<Arguments> unitCases() {
        return Stream.of(
                unit(
                        "disabled despite a key",
                        false,
                        TEST_KEY,
                        10,
                        20,
                        30,
                        4,
                        false,
                        10,
                        20,
                        30,
                        4),
                unit("enabled without a key", true, null, 10, 20, 30, 4, false, 10, 20, 30, 4),
                unit("empty key", true, "", 10, 20, 30, 4, false, 10, 20, 30, 4),
                unit("whitespace key", true, " \t\n", 10, 20, 30, 4, false, 10, 20, 30, 4),
                unit("configured provider", true, TEST_KEY, 10, 20, 30, 4, true, 10, 20, 30, 4),
                unit("negative history", true, TEST_KEY, 10, 20, 30, -1, true, 10, 20, 30, 0),
                unit("zero history", true, TEST_KEY, 10, 20, 30, 0, true, 10, 20, 30, 0),
                unit("positive history", true, TEST_KEY, 10, 20, 30, 15, true, 10, 20, 30, 15),
                unit("zero input limit", true, TEST_KEY, 0, 20, 30, 4, true, 1, 20, 30, 4),
                unit("negative output limit", true, TEST_KEY, 10, 20, -100, 4, true, 10, 20, 1, 4),
                unit("zero token limit", true, TEST_KEY, 10, 0, 30, 4, true, 10, 1, 30, 4),
                unit("minimum positive limits", true, TEST_KEY, 1, 1, 1, 1, true, 1, 1, 1, 1));
    }

    @ParameterizedTest(name = "integration: {0}")
    @MethodSource("bindingCases")
    void bindsExternalConfigurationWithoutCallingAProvider(
            String name, Map<String, Object> values, Expected expected) {
        AiProperties properties =
                new Binder(new MapConfigurationPropertySource(values))
                        .bind("app.ai", Bindable.of(AiProperties.class))
                        .orElseThrow(() -> new AssertionError("AI properties were not bound"));
        assertConfiguration(properties, expected);
        assertThat(properties.model()).isEqualTo("test-model");
        assertThat(properties.baseUrl()).isEqualTo("https://example.invalid");
    }

    static Stream<Arguments> bindingCases() {
        return Stream.of(
                binding(
                        "disabled",
                        false,
                        TEST_KEY,
                        10,
                        20,
                        30,
                        4,
                        new Expected(false, 10, 20, 30, 4)),
                binding(
                        "missing key",
                        true,
                        null,
                        10,
                        20,
                        30,
                        4,
                        new Expected(false, 10, 20, 30, 4)),
                binding(
                        "enabled",
                        true,
                        TEST_KEY,
                        10,
                        20,
                        30,
                        4,
                        new Expected(true, 10, 20, 30, 4)),
                binding(
                        "negative input",
                        true,
                        TEST_KEY,
                        -1,
                        20,
                        30,
                        4,
                        new Expected(true, 1, 20, 30, 4)),
                binding(
                        "negative output",
                        true,
                        TEST_KEY,
                        10,
                        20,
                        -1,
                        4,
                        new Expected(true, 10, 20, 1, 4)),
                binding(
                        "negative tokens",
                        true,
                        TEST_KEY,
                        10,
                        -1,
                        30,
                        4,
                        new Expected(true, 10, 1, 30, 4)),
                binding(
                        "negative history",
                        true,
                        TEST_KEY,
                        10,
                        20,
                        30,
                        -1,
                        new Expected(true, 10, 20, 30, 0)),
                binding(
                        "custom positive limits",
                        true,
                        TEST_KEY,
                        4000,
                        512,
                        8000,
                        12,
                        new Expected(true, 4000, 512, 8000, 12)));
    }

    private static Arguments unit(
            String name,
            boolean enabled,
            String key,
            int input,
            int tokens,
            int output,
            int history,
            boolean configured,
            int expectedInput,
            int expectedTokens,
            int expectedOutput,
            int expectedHistory) {
        return Arguments.of(
                name,
                new AiProperties(
                        enabled,
                        key,
                        "test-model",
                        "https://example.invalid",
                        input,
                        tokens,
                        output,
                        history),
                new Expected(
                        configured,
                        expectedInput,
                        expectedTokens,
                        expectedOutput,
                        expectedHistory));
    }

    private static Arguments binding(
            String name,
            boolean enabled,
            String key,
            int input,
            int tokens,
            int output,
            int history,
            Expected expected) {
        var values = new java.util.LinkedHashMap<String, Object>();
        values.put("app.ai.enabled", Boolean.toString(enabled));
        if (key != null) values.put("app.ai.api-key", key);
        values.put("app.ai.model", "test-model");
        values.put("app.ai.base-url", "https://example.invalid");
        values.put("app.ai.max-input-characters", Integer.toString(input));
        values.put("app.ai.max-output-tokens", Integer.toString(tokens));
        values.put("app.ai.max-output-characters", Integer.toString(output));
        values.put("app.ai.max-history-messages", Integer.toString(history));
        return Arguments.of(name, values, expected);
    }

    private static void assertConfiguration(AiProperties properties, Expected expected) {
        assertThat(properties.isConfigured()).isEqualTo(expected.configured());
        assertThat(properties.inputLimit()).isEqualTo(expected.input());
        assertThat(properties.outputTokenLimit()).isEqualTo(expected.tokens());
        assertThat(properties.outputLimit()).isEqualTo(expected.output());
        assertThat(properties.historyLimit()).isEqualTo(expected.history());
    }

    private record Expected(boolean configured, int input, int tokens, int output, int history) {}
}
