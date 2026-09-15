package com.experimentos.backend.activity.infrastructure;

import com.experimentos.backend.activity.domain.ActivityStatus;
import com.experimentos.backend.activity.domain.WeeklyActivity;
import com.experimentos.backend.shared.infrastructure.firebase.repositories.AbstractFirestoreRepository;
import com.google.cloud.firestore.Firestore;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class WeeklyActivityRepository extends AbstractFirestoreRepository<WeeklyActivity, Long> {
    public WeeklyActivityRepository(Firestore firestore) {
        super(firestore, WeeklyActivity.class, "weekly_activities");
    }

    public List<WeeklyActivity> findByStatusOrderByIdDesc(ActivityStatus status) {
        return readAll().stream().filter(activity -> activity.getStatus() == status).toList().reversed();
    }

    public List<WeeklyActivity> findAllByOrderByIdDesc() {
        return readAll().reversed();
    }

    public boolean existsByCreatedById(Long userId) {
        return readAll().stream()
                .anyMatch(activity -> activity.getCreatedBy() != null && userId.equals(activity.getCreatedBy().getId()));
    }
}
