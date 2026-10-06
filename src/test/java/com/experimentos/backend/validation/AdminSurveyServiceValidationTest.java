package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.audit.application.*;
import com.experimentos.backend.comment.infrastructure.*;
import com.experimentos.backend.iam.domain.*;
import com.experimentos.backend.iam.infrastructure.*;
import com.experimentos.backend.shared.security.*;
import com.experimentos.backend.survey.application.*;
import com.experimentos.backend.survey.domain.*;
import com.experimentos.backend.survey.infrastructure.*;
import com.experimentos.backend.survey.interfaces.*;
import java.util.*;

class AdminSurveyServiceValidationTest extends ScenarioContract {
    static final String JSON =
            "{\"title\":\" Daily \",\"question\":\" How? \",\"type\":\"WEEKLY\",\"allow_comments\":false}";

    static class Fixture {
        final SurveyRepository surveys = mock(SurveyRepository.class);
        final SurveyAnswerRepository answers = mock(SurveyAnswerRepository.class);
        final CommentRepository comments = mock(CommentRepository.class);
        final CommentLikeRepository likes = mock(CommentLikeRepository.class);
        final UserRepository users = mock(UserRepository.class);
        final AuditService audit = mock(AuditService.class);
        final User actor = user(1, Role.SYSTEM_ADMIN);
        final Survey survey = id(new Survey("Daily", "How?", SurveyType.DAILY, true, actor), 10);
        final AdminSurveyService service =
                new AdminSurveyService(surveys, answers, comments, likes, users, audit);
        final AdminSurveyController controller = new AdminSurveyController(service);

        Fixture() {
            authenticate(actor);
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(Optional.of(actor));
            when(surveys.findById(10L)).thenReturn(Optional.of(survey));
            when(surveys.save(any())).thenAnswer(i -> id(i.getArgument(0), 10));
        }

        SurveyAdminDtos.SurveyRequest request(String title, String question) {
            return new SurveyAdminDtos.SurveyRequest(title, question, SurveyType.DAILY, true);
        }

        void noWrite() {
            verify(surveys, never()).save(any());
            verifyNoInteractions(audit);
        }
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                unit(
                        "create title null",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.service.create(f.request(null, "How?")), "blank");
                            f.noWrite();
                        }),
                unit(
                        "create title blank",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.service.create(f.request(" ", "How?")), "blank");
                            f.noWrite();
                        }),
                unit(
                        "create question null",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.service.create(f.request("Daily", null)), "blank");
                            f.noWrite();
                        }),
                unit(
                        "create question blank",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.service.create(f.request("Daily", " ")), "blank");
                            f.noWrite();
                        }),
                unit(
                        "update title blank",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.service.update(10L, f.request(" ", "How?")), "blank");
                            f.noWrite();
                        }),
                unit(
                        "update question blank",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.service.update(10L, f.request("Daily", " ")), "blank");
                            f.noWrite();
                        }),
                unit(
                        "update absent survey",
                        () -> {
                            var f = new Fixture();
                            when(f.surveys.findById(10L)).thenReturn(Optional.empty());
                            rejected(
                                    () -> f.service.update(10L, f.request("Daily", "How?")),
                                    "not found");
                            f.noWrite();
                        }),
                unit(
                        "listAnswers absent survey",
                        () -> {
                            var f = new Fixture();
                            when(f.surveys.findById(10L)).thenReturn(Optional.empty());
                            rejected(() -> f.service.listAnswers(10L), "not found");
                            verifyNoInteractions(f.answers);
                        }),
                unit(
                        "unknown admin cannot create",
                        () -> {
                            var f = new Fixture();
                            when(f.users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                            "actor", "actor"))
                                    .thenReturn(Optional.empty());
                            assertThatThrownBy(() -> f.service.create(f.request("Daily", "How?")))
                                    .isInstanceOf(IllegalStateException.class);
                            f.noWrite();
                        }),
                unit(
                        "publish writes and audits",
                        () -> {
                            var f = new Fixture();
                            assertThat(f.service.publish(10L).status()).isEqualTo("PUBLISHED");
                            verify(f.audit).record(f.actor, "PUBLISH_SURVEY", "SURVEY", "10");
                        }),
                unit(
                        "reopen resets to draft and audits",
                        () -> {
                            var f = new Fixture();
                            f.survey.close();
                            assertThat(f.service.reopen(10L).status()).isEqualTo("DRAFT");
                            verify(f.audit).record(f.actor, "REOPEN_SURVEY", "SURVEY", "10");
                        }),
                unit(
                        "delete cleans likes comments answers before survey",
                        () -> {
                            var f = new Fixture();
                            var comment =
                                    id(
                                            new com.experimentos.backend.comment.domain.Comment(
                                                    f.survey, f.actor, null, "Comment"),
                                            20);
                            when(f.comments.findBySurveyId(10L)).thenReturn(List.of(comment));
                            f.service.delete(10L);
                            var order = inOrder(f.likes, f.comments, f.answers, f.surveys);
                            order.verify(f.likes).deleteByCommentId(20L);
                            order.verify(f.comments).deleteBySurveyId(10L);
                            order.verify(f.answers).deleteBySurveyId(10L);
                            order.verify(f.surveys).delete(f.survey);
                        }),
                integration(
                        "create HTTP writes trimmed survey",
                        () -> {
                            var f = new Fixture();
                            http(f.controller, "POST", "/api/v1/admin/surveys", JSON, 201);
                            verify(f.surveys)
                                    .save(
                                            argThat(
                                                    s ->
                                                            s.getTitle().equals("Daily")
                                                                    && s.getQuestion()
                                                                            .equals("How?")));
                        }),
                integration(
                        "update HTTP persists business fields",
                        () -> {
                            var f = new Fixture();
                            http(f.controller, "PUT", "/api/v1/admin/surveys/10", JSON, 200);
                            verify(f.surveys).save(f.survey);
                            verify(f.audit).record(f.actor, "UPDATE_SURVEY", "SURVEY", "10");
                        }),
                integration(
                        "publish HTTP state",
                        () -> {
                            var f = new Fixture();
                            http(f.controller, "POST", "/api/v1/admin/surveys/10/publish", "", 200);
                            assertThat(f.survey.getStatus()).isEqualTo(SurveyStatus.PUBLISHED);
                        }),
                integration(
                        "close HTTP state",
                        () -> {
                            var f = new Fixture();
                            f.survey.publish();
                            http(f.controller, "POST", "/api/v1/admin/surveys/10/close", "", 200);
                            assertThat(f.survey.getStatus()).isEqualTo(SurveyStatus.CLOSED);
                        }),
                integration(
                        "reopen HTTP state",
                        () -> {
                            var f = new Fixture();
                            f.survey.close();
                            http(f.controller, "POST", "/api/v1/admin/surveys/10/reopen", "", 200);
                            assertThat(f.survey.getStatus()).isEqualTo(SurveyStatus.DRAFT);
                        }),
                integration(
                        "delete HTTP dependency cleanup",
                        () -> {
                            var f = new Fixture();
                            http(f.controller, "DELETE", "/api/v1/admin/surveys/10", "", 204);
                            verify(f.answers).deleteBySurveyId(10L);
                            verify(f.comments).deleteBySurveyId(10L);
                            verify(f.surveys).delete(f.survey);
                        }),
                integration(
                        "missing survey answers HTTP",
                        () -> {
                            var f = new Fixture();
                            when(f.surveys.findById(10L)).thenReturn(Optional.empty());
                            http(f.controller, "GET", "/api/v1/admin/surveys/10/answers", "", 400);
                            verifyNoInteractions(f.answers);
                        }),
                integration(
                        "missing admin create HTTP unavailable",
                        () -> {
                            var f = new Fixture();
                            when(f.users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                            "actor", "actor"))
                                    .thenReturn(Optional.empty());
                            http(f.controller, "POST", "/api/v1/admin/surveys", JSON, 503);
                            f.noWrite();
                        }));
    }
}
