package com.experimentos.backend.shared.infrastructure.firebase.repositories;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.experimentos.backend.activity.domain.ActivityVote;
import com.experimentos.backend.activity.infrastructure.ActivityVoteRepository;
import com.experimentos.backend.comment.infrastructure.CommentLikeEntity;
import com.experimentos.backend.comment.infrastructure.CommentLikeRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import org.junit.jupiter.api.Test;

/** Regression tests for repositories that persist composite Firestore document identifiers. */
class CompositeIdRepositoryTest {
    @Test
    void deletesActivityVoteUsingItsCompositeDocumentId() throws Exception {
        Firestore firestore = mock(Firestore.class);
        CollectionReference collection = mock(CollectionReference.class);
        DocumentReference document = mock(DocumentReference.class);
        ApiFuture<WriteResult> deleteFuture = mock(ApiFuture.class);
        when(firestore.collection("activity_votes")).thenReturn(collection);
        when(collection.document("12_7")).thenReturn(document);
        when(document.delete()).thenReturn(deleteFuture);
        when(deleteFuture.get()).thenReturn(null);

        new ActivityVoteRepository(firestore).deleteById(new ActivityVote.VoteId(12L, 7L));

        verify(collection).document("12_7");
        verify(document).delete();
    }

    @Test
    void deletesCommentLikeUsingItsCompositeDocumentId() throws Exception {
        Firestore firestore = mock(Firestore.class);
        CollectionReference collection = mock(CollectionReference.class);
        DocumentReference document = mock(DocumentReference.class);
        ApiFuture<WriteResult> deleteFuture = mock(ApiFuture.class);
        when(firestore.collection("comment_likes")).thenReturn(collection);
        when(collection.document("12_7")).thenReturn(document);
        when(document.delete()).thenReturn(deleteFuture);
        when(deleteFuture.get()).thenReturn(null);

        new CommentLikeRepository(firestore)
                .deleteById(new CommentLikeEntity.CommentLikeId(12L, 7L));

        verify(collection).document("12_7");
        verify(document).delete();
    }
}
