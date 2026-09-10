package com.experimentos.backend.comment.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentLikeRepository
        extends JpaRepository<CommentLikeEntity, CommentLikeEntity.CommentLikeId> {
    @Query("select count(l) from CommentLikeEntity l where l.commentId = :commentId")
    long countByCommentId(@Param("commentId") Long commentId);
}
