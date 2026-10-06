package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.comment.application.*;
import com.experimentos.backend.comment.domain.*;
import com.experimentos.backend.comment.infrastructure.*;
import com.experimentos.backend.comment.interfaces.*;
import com.experimentos.backend.iam.domain.*;
import com.experimentos.backend.iam.infrastructure.*;
import com.experimentos.backend.shared.security.*;
import com.experimentos.backend.survey.domain.*;
import com.experimentos.backend.survey.infrastructure.*;
import java.util.*;

class CommentServiceValidationTest extends ScenarioContract {
    static class Fixture {
        final CommentRepository comments = mock(CommentRepository.class);
        final CommentLikeRepository likes = mock(CommentLikeRepository.class);
        final SurveyRepository surveys = mock(SurveyRepository.class);
        final UserRepository users = mock(UserRepository.class);
        final User actor = user(1, Role.EMPLOYEE);
        final Survey survey = id(new Survey("Daily", "How?", SurveyType.DAILY, true, actor), 10);
        Comment parent = id(new Comment(survey, actor, null, "Parent"), 20);
        final CommentService service = new CommentService(comments, likes, surveys, users);
        final CommentController controller = new CommentController(service);

        Fixture() {
            authenticate(actor);
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(Optional.of(actor));
            when(surveys.findById(10L)).thenReturn(Optional.of(survey));
            stubParent();
            when(comments.save(any())).thenAnswer(i -> id(i.getArgument(0), 30));
        }

        void stubParent() {
            when(comments.findById(20L)).thenReturn(Optional.of(parent));
        }

        void create(String content, Long parentId) {
            service.create(10L, new CommentDtos.CreateCommentRequest(content, parentId));
        }

        void noSave() {
            verify(comments, never()).save(any());
        }
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                unit(
                        "missing survey create rejected",
                        () -> {
                            var f = new Fixture();
                            when(f.surveys.findById(10L)).thenReturn(Optional.empty());
                            rejected(() -> f.create("Fine", null), "Survey was not found");
                            f.noSave();
                        }),
                unit(
                        "comments disabled rejected",
                        () -> {
                            var f = new Fixture();
                            f.survey.update("Daily", "How?", SurveyType.DAILY, false);
                            rejected(() -> f.create("Fine", null), "disabled");
                            f.noSave();
                        }),
                unit(
                        "null comment rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.create(null, null), "blank");
                            f.noSave();
                        }),
                unit(
                        "blank comment rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.create(" \t", null), "blank");
                            f.noSave();
                        }),
                unit(
                        "oversized comment rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.create("x".repeat(1001), null), "maximum");
                            f.noSave();
                        }),
                unit(
                        "unknown parent rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.create("Fine", 999L), "Parent comment was not found");
                            f.noSave();
                        }),
                unit(
                        "parent from other survey rejected",
                        () -> {
                            var f = new Fixture();
                            f.parent =
                                    new Comment(
                                            id(
                                                    new Survey(
                                                            "Other",
                                                            "How?",
                                                            SurveyType.DAILY,
                                                            true,
                                                            f.actor),
                                                    99),
                                            f.actor,
                                            null,
                                            "Other");
                            f.stubParent();
                            rejected(() -> f.create("Fine", 20L), "does not belong");
                            f.noSave();
                        }),
                unit(
                        "parent without survey rejected",
                        () -> {
                            var f = new Fixture();
                            f.parent = new Comment(null, f.actor, null, "Orphan");
                            f.stubParent();
                            rejected(() -> f.create("Fine", 20L), "does not belong");
                            f.noSave();
                        }),
                unit(
                        "foreign survey like rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.service.like(99L, 20L), "does not belong");
                            verifyNoInteractions(f.likes);
                        }),
                unit(
                        "foreign survey delete rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.service.delete(99L, 20L), "does not belong");
                            verify(f.comments, never()).delete(any());
                        }),
                unit(
                        "foreign owner delete forbidden",
                        () -> {
                            var f = new Fixture();
                            var other = user(2, Role.EMPLOYEE);
                            f.parent = id(new Comment(f.survey, other, null, "Other"), 20);
                            f.stubParent();
                            assertThatThrownBy(() -> f.service.delete(10L, 20L))
                                    .isInstanceOf(
                                            org.springframework.security.access
                                                    .AccessDeniedException.class);
                            verify(f.comments, never()).delete(any());
                        }),
                unit(
                        "delete recursively removes likes before parent",
                        () -> {
                            var f = new Fixture();
                            var child = id(new Comment(f.survey, f.actor, f.parent, "Reply"), 21);
                            when(f.comments.findByParentIdOrderByIdAsc(20L))
                                    .thenReturn(List.of(child));
                            f.service.delete(10L, 20L);
                            var order = inOrder(f.comments, f.likes);
                            order.verify(f.likes).deleteByCommentId(21L);
                            order.verify(f.comments).delete(child);
                            order.verify(f.likes).deleteByCommentId(20L);
                            order.verify(f.comments).delete(f.parent);
                        }),
                integration(
                        "root comment HTTP normalizes and persists",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/surveys/10/comments",
                                    "{\"content\":\" Fine \"}",
                                    200);
                            verify(f.comments)
                                    .save(
                                            argThat(
                                                    c ->
                                                            c.getParent() == null
                                                                    && c.getContent()
                                                                            .equals("Fine")));
                        }),
                integration(
                        "reply HTTP ties parent to same survey",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/surveys/10/comments",
                                    "{\"content\":\"Reply\",\"parent_id\":20}",
                                    200);
                            verify(f.comments).save(argThat(c -> c.getParent() == f.parent));
                        }),
                integration(
                        "like HTTP creates unique composite key",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/surveys/10/comments/20/like",
                                    "",
                                    200);
                            verify(f.likes)
                                    .save(
                                            argThat(
                                                    l ->
                                                            l.getCommentId() == 20L
                                                                    && l.getUserId() == 1L));
                        }),
                integration(
                        "unlike HTTP removes composite key",
                        () -> {
                            var f = new Fixture();
                            when(f.likes.existsById(new CommentLikeEntity.CommentLikeId(20L, 1L)))
                                    .thenReturn(true);
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/surveys/10/comments/20/like",
                                    "",
                                    200);
                            verify(f.likes)
                                    .deleteById(new CommentLikeEntity.CommentLikeId(20L, 1L));
                            verify(f.likes, never()).save(any());
                        }),
                integration(
                        "owned delete HTTP removes tree",
                        () -> {
                            var f = new Fixture();
                            http(f.controller, "DELETE", "/api/v1/surveys/10/comments/20", "", 200);
                            verify(f.comments).delete(f.parent);
                            verify(f.likes).deleteByCommentId(20L);
                        }),
                integration(
                        "foreign owner delete HTTP forbidden",
                        () -> {
                            var f = new Fixture();
                            f.parent =
                                    id(
                                            new Comment(
                                                    f.survey,
                                                    user(2, Role.EMPLOYEE),
                                                    null,
                                                    "Other"),
                                            20);
                            f.stubParent();
                            http(f.controller, "DELETE", "/api/v1/surveys/10/comments/20", "", 403);
                            verify(f.comments, never()).delete(any());
                        }),
                integration(
                        "disabled comments HTTP no save",
                        () -> {
                            var f = new Fixture();
                            f.survey.update("Daily", "How?", SurveyType.DAILY, false);
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/surveys/10/comments",
                                    "{\"content\":\"Fine\"}",
                                    400);
                            f.noSave();
                        }),
                integration(
                        "missing comment like HTTP rejects",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/surveys/10/comments/999/like",
                                    "",
                                    400);
                            verifyNoInteractions(f.likes);
                        }));
    }
}
