package com.experimentos.backend.activity.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "activity_votes")
@IdClass(ActivityVote.VoteId.class)
public class ActivityVote {
    @Id
    @Column(name = "activity_id")
    private Long activityId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private ActivityOption option;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ActivityVote() {}

    public ActivityVote(Long activityId, Long userId, ActivityOption option) {
        this.activityId = activityId;
        this.userId = userId;
        this.option = option;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public static class VoteId implements Serializable {
        private Long activityId;
        private Long userId;

        public VoteId() {}

        public VoteId(Long activityId, Long userId) {
            this.activityId = activityId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof VoteId that)) return false;
            return Objects.equals(activityId, that.activityId)
                    && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(activityId, userId);
        }
    }
}
