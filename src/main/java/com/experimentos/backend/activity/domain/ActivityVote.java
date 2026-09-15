package com.experimentos.backend.activity.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class ActivityVote {
    private Long activityId;

    private Long userId;

    private ActivityOption option;

    private Instant createdAt;

    protected ActivityVote() {}

    public ActivityVote(Long activityId, Long userId, ActivityOption option) {
        this.activityId = activityId;
        this.userId = userId;
        this.option = option;
    }

    public Long getActivityId() {
        return activityId;
    }

    public Long getUserId() {
        return userId;
    }

    public ActivityOption getOption() {
        return option;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static class VoteId implements Serializable {
        private Long activityId;
        private Long userId;

        public VoteId() {}

        public VoteId(Long activityId, Long userId) {
            this.activityId = activityId;
            this.userId = userId;
        }

        public Long activityId() {
            return activityId;
        }

        public Long userId() {
            return userId;
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
