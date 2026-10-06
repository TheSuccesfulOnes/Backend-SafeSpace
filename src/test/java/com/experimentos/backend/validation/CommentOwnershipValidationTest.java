package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.comment.application.CommentService;
import com.experimentos.backend.comment.domain.Comment;
import com.experimentos.backend.comment.infrastructure.*;
import com.experimentos.backend.comment.interfaces.CommentController;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.Role;
import com.experimentos.backend.survey.domain.*;
import com.experimentos.backend.survey.infrastructure.SurveyRepository;
import java.util.*;

class CommentOwnershipValidationTest extends ScenarioContract {
    static class Fixture {
        final User actor = user(1, Role.EMPLOYEE);
        final Survey survey = id(new Survey("Daily", "How?", SurveyType.DAILY, true, actor), 10);
        Comment comment = id(new Comment(survey, actor, null, "Content"), 20);
        final CommentRepository comments = mock(CommentRepository.class);
        final CommentLikeRepository likes = mock(CommentLikeRepository.class);
        final SurveyRepository surveys = mock(SurveyRepository.class);
        final UserRepository users = mock(UserRepository.class);
        final CommentController controller =
                new CommentController(new CommentService(comments, likes, surveys, users));

        void setup(Role role, boolean owns) {
            actor.changeRole(role);
            authenticate(actor);
            if (!owns)
                comment = id(new Comment(survey, user(2, Role.EMPLOYEE), null, "Content"), 20);
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(Optional.of(actor));
            when(surveys.findById(10L)).thenReturn(Optional.of(survey));
            when(comments.findById(20L)).thenReturn(Optional.of(comment));
            when(comments.findBySurveyIdAndParentIsNullOrderByIdAsc(10L))
                    .thenReturn(List.of(comment));
        }
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                unit(
                        "same persisted user owns comment",
                        () -> {
                            var f = new Fixture();
                            assertThat(f.comment.isOwnedBy(1L)).isTrue();
                        }),
                unit(
                        "different persisted user cannot own comment",
                        () -> {
                            var f = new Fixture();
                            assertThat(f.comment.isOwnedBy(2L)).isFalse();
                        }),
                unit(
                        "absent caller cannot own comment",
                        () -> {
                            var f = new Fixture();
                            assertThat(f.comment.isOwnedBy(null)).isFalse();
                        }),
                unit(
                        "orphan without author cannot be owned",
                        () -> {
                            var f = new Fixture();
                            assertThat(new Comment(f.survey, null, null, "Orphan").isOwnedBy(1L))
                                    .isFalse();
                        }),
                unit(
                        "author without persisted identity rejected",
                        () -> {
                            var f = new Fixture();
                            org.springframework.test.util.ReflectionTestUtils.setField(
                                    f.actor, "id", null);
                            assertThat(f.comment.isOwnedBy(1L)).isFalse();
                        }),
                unit(
                        "both identities absent never imply ownership",
                        () -> {
                            var f = new Fixture();
                            org.springframework.test.util.ReflectionTestUtils.setField(
                                    f.actor, "id", null);
                            assertThat(f.comment.isOwnedBy(null)).isFalse();
                        }),
                unit(
                        "same username different account never owns",
                        () -> {
                            var f = new Fixture();
                            assertThat(f.comment.isOwnedBy(user(2, Role.EMPLOYEE).getId()))
                                    .isFalse();
                        }),
                unit(
                        "username changes do not revoke ownership",
                        () -> {
                            var f = new Fixture();
                            f.actor.updateProfile("renamed", "other@example.test", "Other");
                            assertThat(f.comment.isOwnedBy(1L)).isTrue();
                        }),
                integration(
                        "EMPLOYEE list deletion flag own",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.EMPLOYEE, true);
                            var r =
                                    http(
                                            f.controller,
                                            "GET",
                                            "/api/v1/surveys/10/comments",
                                            "",
                                            200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"can_delete\":true");
                        }),
                integration(
                        "EMPLOYEE author deletion allowed",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.EMPLOYEE, true);
                            http(f.controller, "DELETE", "/api/v1/surveys/10/comments/20", "", 200);
                            verify(f.comments).delete(f.comment);
                        }),
                integration(
                        "EMPLOYEE list deletion flag foreign",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.EMPLOYEE, false);
                            var r =
                                    http(
                                            f.controller,
                                            "GET",
                                            "/api/v1/surveys/10/comments",
                                            "",
                                            200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"can_delete\":false");
                        }),
                integration(
                        "EMPLOYEE author deletion forbidden",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.EMPLOYEE, false);
                            http(f.controller, "DELETE", "/api/v1/surveys/10/comments/20", "", 403);
                            verify(f.comments, never()).delete(any());
                        }),
                integration(
                        "HR_MEMBER list deletion flag own",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.HR_MEMBER, true);
                            var r =
                                    http(
                                            f.controller,
                                            "GET",
                                            "/api/v1/surveys/10/comments",
                                            "",
                                            200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"can_delete\":true");
                        }),
                integration(
                        "HR_MEMBER author deletion allowed",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.HR_MEMBER, true);
                            http(f.controller, "DELETE", "/api/v1/surveys/10/comments/20", "", 200);
                            verify(f.comments).delete(f.comment);
                        }),
                integration(
                        "HR_MEMBER list deletion flag foreign",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.HR_MEMBER, false);
                            var r =
                                    http(
                                            f.controller,
                                            "GET",
                                            "/api/v1/surveys/10/comments",
                                            "",
                                            200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"can_delete\":false");
                        }),
                integration(
                        "HR_MEMBER author deletion forbidden",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.HR_MEMBER, false);
                            http(f.controller, "DELETE", "/api/v1/surveys/10/comments/20", "", 403);
                            verify(f.comments, never()).delete(any());
                        }),
                integration(
                        "SYSTEM_ADMIN list deletion flag own",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.SYSTEM_ADMIN, true);
                            var r =
                                    http(
                                            f.controller,
                                            "GET",
                                            "/api/v1/surveys/10/comments",
                                            "",
                                            200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"can_delete\":true");
                        }),
                integration(
                        "SYSTEM_ADMIN author deletion allowed",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.SYSTEM_ADMIN, true);
                            http(f.controller, "DELETE", "/api/v1/surveys/10/comments/20", "", 200);
                            verify(f.comments).delete(f.comment);
                        }),
                integration(
                        "SYSTEM_ADMIN list deletion flag foreign",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.SYSTEM_ADMIN, false);
                            var r =
                                    http(
                                            f.controller,
                                            "GET",
                                            "/api/v1/surveys/10/comments",
                                            "",
                                            200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"can_delete\":false");
                        }),
                integration(
                        "SYSTEM_ADMIN author deletion forbidden",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.SYSTEM_ADMIN, false);
                            http(f.controller, "DELETE", "/api/v1/surveys/10/comments/20", "", 403);
                            verify(f.comments, never()).delete(any());
                        }));
    }
}
