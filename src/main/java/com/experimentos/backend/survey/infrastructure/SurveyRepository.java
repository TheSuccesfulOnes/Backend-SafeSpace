package com.experimentos.backend.survey.infrastructure;

import com.experimentos.backend.survey.domain.Survey;
import com.experimentos.backend.survey.domain.SurveyStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyRepository extends JpaRepository<Survey, Long> {
    List<Survey> findByStatusOrderByIdDesc(SurveyStatus status);

    List<Survey> findAllByOrderByIdDesc();

    boolean existsByCreatedById(Long userId);
}
