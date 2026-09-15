package com.experimentos.backend.activity.infrastructure;

import com.experimentos.backend.activity.domain.ActivityVote;
import com.experimentos.backend.shared.infrastructure.firebase.repositories.AbstractFirestoreRepository;
import com.google.cloud.firestore.Firestore;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ActivityVoteRepository
        extends AbstractFirestoreRepository<ActivityVote, ActivityVote.VoteId> {
    public ActivityVoteRepository(Firestore firestore) {
        super(firestore, ActivityVote.class, "activity_votes");
    }

    public Optional<ActivityVote> findByActivityIdAndUserId(Long activityId, Long userId) {
        return readAll().stream()
                .filter(
                        vote ->
                                Objects.equals(activityId, vote.getActivityId())
                                        && Objects.equals(userId, vote.getUserId()))
                .findFirst();
    }

    public long countByActivityId(Long activityId) {
        return readAll().stream()
                .filter(vote -> Objects.equals(activityId, vote.getActivityId()))
                .count();
    }

    public long countByOptionId(Long optionId) {
        return readAll().stream()
                .filter(vote -> vote.getOption() != null)
                .filter(vote -> optionId.equals(vote.getOption().getId()))
                .count();
    }

    public void deleteByActivityId(Long activityId) {
        readAll().stream()
                .filter(vote -> Objects.equals(activityId, vote.getActivityId()))
                .toList()
                .forEach(this::delete);
    }

    @Override
    protected String documentId(ActivityVote entity, Object id) {
        return entity.getActivityId() + "_" + entity.getUserId();
    }

    public boolean existsById(ActivityVote.VoteId id) {
        return findByActivityIdAndUserId(id.activityId(), id.userId()).isPresent();
    }

    public void deleteById(ActivityVote.VoteId id) {
        if (id == null) return;
        deleteDocumentById(id.activityId() + "_" + id.userId());
    }
}
