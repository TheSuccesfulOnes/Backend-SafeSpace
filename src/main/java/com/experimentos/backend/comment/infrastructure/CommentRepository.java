package com.experimentos.backend.comment.infrastructure;

import com.experimentos.backend.comment.domain.Comment;
import com.experimentos.backend.shared.infrastructure.firebase.repositories.AbstractFirestoreRepository;
import com.google.cloud.firestore.Firestore;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CommentRepository extends AbstractFirestoreRepository<Comment, Long> {
    public CommentRepository(Firestore firestore) {
        super(firestore, Comment.class, "comments");
    }

    public List<Comment> findBySurveyIdAndParentIsNullOrderByIdAsc(Long surveyId) {
        return readAll().stream()
                .filter(comment -> belongsToSurvey(comment, surveyId) && comment.getParent() == null)
                .toList();
    }

    public List<Comment> findByParentIdOrderByIdAsc(Long parentId) {
        return readAll().stream()
                .filter(comment -> comment.getParent() != null && parentId.equals(comment.getParent().getId()))
                .toList();
    }

    private boolean belongsToSurvey(Comment comment, Long surveyId) {
        return comment.getSurvey() != null && surveyId.equals(comment.getSurvey().getId());
    }
}
