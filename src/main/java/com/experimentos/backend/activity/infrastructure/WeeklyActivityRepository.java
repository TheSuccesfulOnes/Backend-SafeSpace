package com.experimentos.backend.activity.infrastructure;

import com.experimentos.backend.activity.domain.ActivityStatus;
import com.experimentos.backend.activity.domain.WeeklyActivity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyActivityRepository extends JpaRepository<WeeklyActivity, Long> {
    List<WeeklyActivity> findByStatusOrderByIdDesc(ActivityStatus status);

    List<WeeklyActivity> findAllByOrderByIdDesc();

    boolean existsByCreatedById(Long userId);
}
