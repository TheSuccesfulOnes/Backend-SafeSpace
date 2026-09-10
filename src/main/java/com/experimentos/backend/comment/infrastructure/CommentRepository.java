package com.experimentos.backend.comment.infrastructure;

import com.experimentos.backend.comment.domain.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findBySurveyIdAndParentIsNullOrderByIdAsc(Long surveyId);

    List<Comment> findByParentIdOrderByIdAsc(Long parentId);
}
