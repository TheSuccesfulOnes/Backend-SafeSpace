package com.experimentos.backend.comment.infrastructure;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "comment_likes")
@IdClass(CommentLikeEntity.CommentLikeId.class)
public class CommentLikeEntity {
    @Id
    @Column(name = "comment_id")
    private Long commentId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CommentLikeEntity() {}

    public CommentLikeEntity(Long commentId, Long userId) {
        this.commentId = commentId;
        this.userId = userId;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public static class CommentLikeId implements Serializable {
        private Long commentId;
        private Long userId;

        public CommentLikeId() {}

        public CommentLikeId(Long commentId, Long userId) {
            this.commentId = commentId;
            this.userId = userId;
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
