package com.experimentos.backend.comment.infrastructure;

import com.experimentos.backend.shared.infrastructure.firebase.repositories.AbstractFirestoreRepository;
import com.google.cloud.firestore.Firestore;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CommentLikeRepository
        extends AbstractFirestoreRepository<CommentLikeEntity, CommentLikeEntity.CommentLikeId> {
    public CommentLikeRepository(Firestore firestore) {
        super(firestore, CommentLikeEntity.class, "comment_likes");
    }

    public long countByCommentId(Long commentId) {
        return readAll().stream().filter(like -> commentId.equals(like.getCommentId())).count();
    }

    public boolean existsById(CommentLikeEntity.CommentLikeId id) {
        return findByKey(id).isPresent();
    }

    public void deleteById(CommentLikeEntity.CommentLikeId id) {
        if (id == null) return;
        deleteDocumentById(id.commentId() + "_" + id.userId());
    }

    public void deleteByCommentId(Long commentId) {
        readAll().stream()
                .filter(like -> commentId.equals(like.getCommentId()))
                .toList()
                .forEach(this::delete);
    }

    @Override
    protected String documentId(CommentLikeEntity entity, Object id) {
        return entity.getCommentId() + "_" + entity.getUserId();
    }

    private Optional<CommentLikeEntity> findByKey(CommentLikeEntity.CommentLikeId id) {
        return readAll().stream()
                .filter(
                        like ->
                                id.commentId().equals(like.getCommentId())
                                        && id.userId().equals(like.getUserId()))
                .findFirst();
    }
}
