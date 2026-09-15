package com.experimentos.backend.survey.infrastructure;

import com.experimentos.backend.shared.infrastructure.firebase.repositories.AbstractFirestoreRepository;
import com.experimentos.backend.survey.domain.SurveyAnswer;
import com.google.cloud.firestore.Firestore;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SurveyAnswerRepository extends AbstractFirestoreRepository<SurveyAnswer, Long> {
    public SurveyAnswerRepository(Firestore firestore) {
        super(firestore, SurveyAnswer.class, "survey_answers");
    }

    public Optional<SurveyAnswer> findBySurveyIdAndUserId(Long surveyId, Long userId) {
        return readAll().stream()
                .filter(
                        answer ->
                                answer.getSurvey() != null
                                        && answer.getUser() != null
                                        && surveyId.equals(answer.getSurvey().getId())
                                        && userId.equals(answer.getUser().getId()))
                .findFirst();
    }

    public long countBySurveyId(Long surveyId) {
        return readAll().stream()
                .filter(answer -> answer.getSurvey() != null && surveyId.equals(answer.getSurvey().getId()))
                .count();
    }

    public List<SurveyAnswer> findBySurveyIdOrderByIdAsc(Long surveyId) {
        return readAll().stream()
                .filter(answer -> answer.getSurvey() != null && surveyId.equals(answer.getSurvey().getId()))
                .toList();
    }
}
