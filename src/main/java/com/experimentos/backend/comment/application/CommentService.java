package com.experimentos.backend.comment.application;

import com.experimentos.backend.comment.domain.Comment;
import com.experimentos.backend.comment.infrastructure.CommentLikeEntity;
import com.experimentos.backend.comment.infrastructure.CommentLikeRepository;
import com.experimentos.backend.comment.infrastructure.CommentRepository;
import com.experimentos.backend.comment.interfaces.CommentDtos;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.CurrentUser;
import com.experimentos.backend.survey.domain.Survey;
import com.experimentos.backend.survey.infrastructure.SurveyRepository;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {
    private final CommentRepository comments;
    private final CommentLikeRepository likes;
    private final SurveyRepository surveys;
    private final UserRepository users;

    public CommentService(
            CommentRepository comments,
            CommentLikeRepository likes,
            SurveyRepository surveys,
            UserRepository users) {
        this.comments = comments;
        this.likes = likes;
        this.surveys = surveys;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<CommentDtos.CommentResponse> list(Long surveyId) {
        User currentUser = currentUser();
        return comments.findBySurveyIdAndParentIsNullOrderByIdAsc(surveyId).stream()
                .map(comment -> toResponse(comment, currentUser.getId()))
                .toList();
    }

    @Transactional
    public CommentDtos.CommentResponse create(
            Long surveyId, CommentDtos.CreateCommentRequest request) {
        Survey survey =
                surveys.findById(surveyId)
                        .orElseThrow(() -> new IllegalArgumentException("Survey was not found"));
        if (!survey.isAllowComments())
            throw new IllegalArgumentException("Comments are disabled for this survey");
        Comment parent =
                request.parentId() == null
                        ? null
                        : comments.findById(request.parentId())
                                .orElseThrow(
                                        () ->
                                                new IllegalArgumentException(
                                                        "Parent comment was not found"));
        if (parent != null && !parent.getSurvey().getId().equals(surveyId)) {
            throw new IllegalArgumentException("Parent comment does not belong to this survey");
        }
        User user = currentUser();
        return toResponse(
                comments.save(new Comment(survey, user, parent, request.content().trim())),
                user.getId());
    }

    @Transactional
    public void like(Long commentId) {
        if (!comments.existsById(commentId))
            throw new IllegalArgumentException("Comment was not found");
        Long userId = currentUser().getId();
        CommentLikeEntity.CommentLikeId key =
                new CommentLikeEntity.CommentLikeId(commentId, userId);
        if (likes.existsById(key)) likes.deleteById(key);
        else likes.save(new CommentLikeEntity(commentId, userId));
    }

    @Transactional
    public void delete(Long surveyId, Long commentId) {
        Comment comment =
                comments.findById(commentId)
                        .orElseThrow(() -> new IllegalArgumentException("Comment was not found"));
        if (!comment.getSurvey().getId().equals(surveyId)) {
            throw new IllegalArgumentException("Comment does not belong to this survey");
        }
        User user = currentUser();
        if (!comment.isOwnedBy(user.getId())) {
            throw new AccessDeniedException("Only the comment author can delete this comment");
        }
        comments.delete(comment);
    }

    private CommentDtos.CommentResponse toResponse(Comment comment, Long currentUserId) {
        List<CommentDtos.CommentResponse> replies =
                comments.findByParentIdOrderByIdAsc(comment.getId()).stream()
                        .map(reply -> toResponse(reply, currentUserId))
                        .toList();
        return new CommentDtos.CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                likes.countByCommentId(comment.getId()),
                comment.isOwnedBy(currentUserId),
                replies);
    }

    private User currentUser() {
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        CurrentUser.username(), CurrentUser.username())
                .orElseThrow(
                        () -> new IllegalArgumentException("Authenticated user was not found"));
    }
}
