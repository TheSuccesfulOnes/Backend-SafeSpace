package com.experimentos.backend.comment.infrastructure;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class CommentLikeEntity {
    private Long commentId;

    private Long userId;

    private Instant createdAt;

    protected CommentLikeEntity() {}

    public CommentLikeEntity(Long commentId, Long userId) {
        this.commentId = commentId;
        this.userId = userId;
    }

    public Long getCommentId() {
        return commentId;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static class CommentLikeId implements Serializable {
        private Long commentId;
        private Long userId;

        public CommentLikeId() {}

        public CommentLikeId(Long commentId, Long userId) {
            this.commentId = commentId;
            this.userId = userId;
        }

        public Long commentId() {
            return commentId;
        }

        public Long userId() {
            return userId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof CommentLikeId that)) return false;
            return Objects.equals(commentId, that.commentId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(commentId, userId);
        }
    }
}
