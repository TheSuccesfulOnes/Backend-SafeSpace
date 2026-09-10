package com.experimentos.backend.activity.infrastructure;

import com.experimentos.backend.activity.domain.ActivityVote;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityVoteRepository extends JpaRepository<ActivityVote, ActivityVote.VoteId> {
    Optional<ActivityVote> findByActivityIdAndUserId(Long activityId, Long userId);

    long countByActivityId(Long activityId);

    long countByOptionId(Long optionId);
}
