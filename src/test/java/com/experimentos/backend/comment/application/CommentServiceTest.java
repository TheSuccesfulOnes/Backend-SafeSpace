package com.experimentos.backend.comment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.experimentos.backend.comment.domain.Comment;
import com.experimentos.backend.comment.infrastructure.CommentLikeRepository;
import com.experimentos.backend.comment.infrastructure.CommentRepository;
import com.experimentos.backend.comment.interfaces.CommentDtos;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.Role;
import com.experimentos.backend.survey.domain.Survey;
import com.experimentos.backend.survey.domain.SurveyType;
import com.experimentos.backend.survey.infrastructure.SurveyRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for comment ownership and thread projection rules. */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    @Mock CommentRepository comments;
    @Mock CommentLikeRepository likes;
    @Mock SurveyRepository surveys;
    @Mock UserRepository users;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void marksOnlyOwnedCommentsAsDeletable() {
        User currentUser = user(2L, "maria");
        User otherUser = user(3L, "carlos");
        Survey survey = survey(10L, currentUser);
        Comment ownComment = comment(11L, survey, currentUser, "My comment");
        Comment otherComment = comment(12L, survey, otherUser, "Another comment");
        authenticateAs("maria");
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("maria", "maria"))
                .thenReturn(Optional.of(currentUser));
        when(surveys.findById(10L)).thenReturn(Optional.of(survey));
        when(comments.findBySurveyIdAndParentIsNullOrderByIdAsc(10L))
                .thenReturn(List.of(ownComment, otherComment));
        when(comments.findByParentIdOrderByIdAsc(anyLong())).thenReturn(List.of());
        when(likes.countByCommentId(anyLong())).thenReturn(0L);

        var response = new CommentService(comments, likes, surveys, users).list(10L);

        assertThat(response)
                .extracting(CommentDtos.CommentResponse::canDelete)
                .containsExactly(true, false);
    }

    @Test
    void allowsDeletingOwnedComment() {
        User currentUser = user(2L, "maria");
        Survey survey = survey(10L, currentUser);
        Comment comment = comment(11L, survey, currentUser, "My comment");
        authenticateAs("maria");
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("maria", "maria"))
                .thenReturn(Optional.of(currentUser));
        when(comments.findById(11L)).thenReturn(Optional.of(comment));

        new CommentService(comments, likes, surveys, users).delete(10L, 11L);

        verify(comments).delete(comment);
    }

    @Test
    void rejectsDeletingAnotherUsersComment() {
        User currentUser = user(2L, "maria");
        User otherUser = user(3L, "carlos");
        Survey survey = survey(10L, currentUser);
        Comment comment = comment(11L, survey, otherUser, "Not mine");
        authenticateAs("maria");
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("maria", "maria"))
                .thenReturn(Optional.of(currentUser));
        when(comments.findById(11L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(
                        () -> new CommentService(comments, likes, surveys, users).delete(10L, 11L))
                .isInstanceOf(AccessDeniedException.class);
        verify(comments, never()).delete(any(Comment.class));
    }

    private Survey survey(Long id, User creator) {
        Survey survey = new Survey("Daily", "How was work?", SurveyType.DAILY, true, creator);
        ReflectionTestUtils.setField(survey, "id", id);
        return survey;
    }

    private Comment comment(Long id, Survey survey, User author, String content) {
        Comment comment = new Comment(survey, author, null, content);
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    private User user(Long id, String username) {
        User user = new User(username, username + "@example.com", "hash", username, Role.EMPLOYEE);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void authenticateAs(String username) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                username, null, List.of()));
    }
}
