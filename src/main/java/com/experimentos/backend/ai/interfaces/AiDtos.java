package com.experimentos.backend.ai.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AiDtos {
    private AiDtos() {}

    public record ConversationResponse(Long id, String title) {}

    public record UpdateConversationRequest(@NotBlank @Size(max = 160) String title) {}

    public record SendMessageRequest(
            @NotBlank @Size(max = 4000) String content,
            @Pattern(regexp = "es|en", message = "Language must be es or en") String language) {}

    public record MessageResponse(Long id, String sender, String content, Instant createdAt) {}
}
