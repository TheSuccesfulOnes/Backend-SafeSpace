package com.experimentos.backend.comment.interfaces;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public final class CommentDtos {
    private CommentDtos() {}

    public record CreateCommentRequest(@NotBlank String content, Long parentId) {}

    public record CommentResponse(
            Long id,
            String content,
            Instant createdAt,
            long likes,
            boolean canDelete,
            java.util.List<CommentResponse> replies) {}
}
