package com.experimentos.backend.comment.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class CommentDtos {
    private CommentDtos() {}

    public record CreateCommentRequest(@NotBlank @Size(max = 1000) String content, Long parentId) {}

    public record CommentResponse(
            Long id,
            String content,
            Instant createdAt,
            long likes,
            boolean canDelete,
            java.util.List<CommentResponse> replies) {}
}
