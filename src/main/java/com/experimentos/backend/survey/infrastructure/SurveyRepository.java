package com.experimentos.backend.survey.infrastructure;

import com.experimentos.backend.shared.infrastructure.firebase.repositories.AbstractFirestoreRepository;
import com.experimentos.backend.survey.domain.Survey;
import com.experimentos.backend.survey.domain.SurveyStatus;
import com.google.cloud.firestore.Firestore;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class SurveyRepository extends AbstractFirestoreRepository<Survey, Long> {
    public SurveyRepository(Firestore firestore) {
        super(firestore, Survey.class, "surveys");
    }

    public List<Survey> findByStatusOrderByIdDesc(SurveyStatus status) {
        return readAll().stream().filter(survey -> survey.getStatus() == status).toList().reversed();
    }

    public List<Survey> findAllByOrderByIdDesc() {
        return readAll().reversed();
    }

    public boolean existsByCreatedById(Long userId) {
        return readAll().stream()
                .anyMatch(survey -> survey.getCreatedBy() != null && userId.equals(survey.getCreatedBy().getId()));
    }
}
