package com.experimentos.backend.ai.domain;

import java.util.List;

/** Input required by an AI provider to generate a safe assistant response. */
public record AiGenerationRequest(
        List<AiConversationTurn> history, String userMessage, String language) {}
