package com.experimentos.backend.ai.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for the optional Gemini integration. Secrets are read from the environment. */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        boolean enabled,
        String apiKey,
        String model,
        String baseUrl,
        int maxInputCharacters,
        int maxOutputTokens,
        int maxOutputCharacters,
        int maxHistoryMessages) {

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public int historyLimit() {
        return Math.max(0, maxHistoryMessages);
    }

    public int inputLimit() {
        return Math.max(1, maxInputCharacters);
    }

    public int outputLimit() {
        return Math.max(1, maxOutputCharacters);
    }

    public int outputTokenLimit() {
        return Math.max(1, maxOutputTokens);
    }
}
