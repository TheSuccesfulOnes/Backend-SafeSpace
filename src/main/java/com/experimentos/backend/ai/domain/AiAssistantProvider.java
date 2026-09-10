package com.experimentos.backend.ai.domain;

/** Port for the AI provider used by the employee wellbeing assistant. */
public interface AiAssistantProvider {

    /** Returns whether an external provider is configured and enabled. */
    boolean isAvailable();

    /** Generates a response for the supplied conversation context. */
    String generateReply(AiGenerationRequest request);
}
