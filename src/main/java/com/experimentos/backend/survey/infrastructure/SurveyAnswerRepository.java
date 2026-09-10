package com.experimentos.backend.survey.infrastructure;

import com.experimentos.backend.survey.domain.SurveyAnswer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, Long> {
    Optional<SurveyAnswer> findBySurveyIdAndUserId(Long surveyId, Long userId);

    long countBySurveyId(Long surveyId);

    List<SurveyAnswer> findBySurveyIdOrderByIdAsc(Long surveyId);
}
