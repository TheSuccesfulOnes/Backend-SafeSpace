package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.comment.infrastructure.*;
import com.experimentos.backend.iam.domain.*;
import com.experimentos.backend.iam.infrastructure.*;
import com.experimentos.backend.shared.security.*;
import com.experimentos.backend.survey.application.*;
import com.experimentos.backend.survey.domain.*;
import com.experimentos.backend.survey.infrastructure.*;
import com.experimentos.backend.survey.interfaces.*;
import java.util.*;

class SurveyServiceValidationTest extends ScenarioContract {
    static class Fixture {
        final SurveyRepository surveys = mock(SurveyRepository.class);
        final SurveyAnswerRepository answers = mock(SurveyAnswerRepository.class);
        final CommentRepository comments = mock(CommentRepository.class);
        final UserRepository users = mock(UserRepository.class);
        final User actor = user(1, Role.EMPLOYEE);
        final Survey survey = id(new Survey("Daily", "How?", SurveyType.DAILY, true, actor), 10);
        final SurveyService service = new SurveyService(surveys, answers, comments, users);
        final SurveyController controller = new SurveyController(service);

        Fixture() {
            authenticate(actor);
            survey.publish();
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(Optional.of(actor));
            when(surveys.findById(10L)).thenReturn(Optional.of(survey));
            when(surveys.save(any())).thenAnswer(i -> id(i.getArgument(0), 10));
            when(answers.save(any())).thenAnswer(i -> id(i.getArgument(0), 20));
            when(comments.save(any())).thenAnswer(i -> id(i.getArgument(0), 30));
        }

        void answer(String text) {
            service.answer(10L, new SurveyDtos.AnswerRequest(text));
        }

        void noAnswer() {
            verify(answers, never()).save(any());
            verify(comments, never()).save(any());
        }
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                unit(
                        "missing survey answer",
                        () -> {
                            var f = new Fixture();
                            when(f.surveys.findById(10L)).thenReturn(Optional.empty());
                            rejected(() -> f.answer("Fine"), "not found");
                            f.noAnswer();
                        }),
                unit(
                        "draft cannot answer",
                        () -> {
                            var f = new Fixture();
                            f.survey.reopen();
                            rejected(() -> f.answer("Fine"), "not open");
                            f.noAnswer();
                        }),
                unit(
                        "closed cannot answer",
                        () -> {
                            var f = new Fixture();
                            f.survey.close();
                            rejected(() -> f.answer("Fine"), "not open");
                            f.noAnswer();
                        }),
                unit(
                        "HR cannot answer",
                        () -> {
                            var f = new Fixture();
                            f.actor.changeRole(Role.HR_MEMBER);
                            assertThatThrownBy(() -> f.answer("Fine"))
                                    .isInstanceOf(
                                            org.springframework.security.access
                                                    .AccessDeniedException.class);
                            f.noAnswer();
                        }),
                unit(
                        "admin cannot answer",
                        () -> {
                            var f = new Fixture();
                            f.actor.changeRole(Role.SYSTEM_ADMIN);
                            assertThatThrownBy(() -> f.answer("Fine"))
                                    .isInstanceOf(
                                            org.springframework.security.access
                                                    .AccessDeniedException.class);
                            f.noAnswer();
                        }),
                unit(
                        "null answer rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.answer(null), "blank");
                            f.noAnswer();
                        }),
                unit(
                        "blank answer rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.answer(" \t"), "blank");
                            f.noAnswer();
                        }),
                unit(
                        "missing authenticated user",
                        () -> {
                            var f = new Fixture();
                            when(f.users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                            "actor", "actor"))
                                    .thenReturn(Optional.empty());
                            rejected(() -> f.answer("Fine"), "Authenticated user");
                            f.noAnswer();
                        }),
                unit(
                        "comment failure compensates saved answer",
                        () -> {
                            var f = new Fixture();
                            doThrow(new IllegalStateException("synthetic"))
                                    .when(f.comments)
                                    .save(any());
                            assertThatThrownBy(() -> f.answer("Fine")).hasMessage("synthetic");
                            verify(f.answers).delete(any(SurveyAnswer.class));
                        }),
                unit(
                        "failed first save never attempts comment",
                        () -> {
                            var f = new Fixture();
                            doThrow(new IllegalStateException("synthetic"))
                                    .when(f.answers)
                                    .save(any());
                            assertThatThrownBy(() -> f.answer("Fine")).hasMessage("synthetic");
                            verifyNoInteractions(f.comments);
                        }),
                unit(
                        "publish updates persisted state",
                        () -> {
                            var f = new Fixture();
                            f.survey.reopen();
                            assertThat(f.service.publish(10L).status()).isEqualTo("PUBLISHED");
                            verify(f.surveys).save(f.survey);
                        }),
                unit(
                        "closed published again can accept response",
                        () -> {
                            var f = new Fixture();
                            f.survey.close();
                            f.service.publish(10L);
                            f.answer("Fine");
                            verify(f.answers).save(any());
                        }),
                integration(
                        "answer HTTP writes both records",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/surveys/10/answers",
                                    "{\"answer_text\":\" Fine \"}",
                                    200);
                            verify(f.answers).save(argThat(a -> a.getAnswerText().equals("Fine")));
                            verify(f.comments)
                                    .save(
                                            argThat(
                                                    c ->
                                                            c.getContent().equals("Fine")
                                                                    && c.getParent() == null));
                        }),
                integration(
                        "duplicate answer HTTP rejects",
                        () -> {
                            var f = new Fixture();
                            when(f.answers.findBySurveyIdAndUserId(10L, 1L))
                                    .thenReturn(
                                            Optional.of(
                                                    new SurveyAnswer(f.survey, f.actor, "First")));
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/surveys/10/answers",
                                    "{\"answer_text\":\"Second\"}",
                                    400);
                            f.noAnswer();
                        }),
                integration(
                        "draft answer HTTP rejects",
                        () -> {
                            var f = new Fixture();
                            f.survey.reopen();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/surveys/10/answers",
                                    "{\"answer_text\":\"Fine\"}",
                                    400);
                            f.noAnswer();
                        }),
                integration(
                        "HR answer HTTP forbidden",
                        () -> {
                            var f = new Fixture();
                            f.actor.changeRole(Role.HR_MEMBER);
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/surveys/10/answers",
                                    "{\"answer_text\":\"Fine\"}",
                                    403);
                            f.noAnswer();
                        }),
                integration(
                        "blank answer binding no persistence",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/surveys/10/answers",
                                    "{\"answer_text\":\" \"}",
                                    400);
                            f.noAnswer();
                        }),
                integration(
                        "provider repository failure returns503 with compensation",
                        () -> {
                            var f = new Fixture();
                            doThrow(new IllegalStateException("synthetic"))
                                    .when(f.comments)
                                    .save(any());
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/surveys/10/answers",
                                    "{\"answer_text\":\"Fine\"}",
                                    503);
                            verify(f.answers).delete(any());
                        }),
                integration(
                        "create HTTP persists normalized survey",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/surveys",
                                    "{\"title\":\" Daily \",\"question\":\" How? \",\"type\":\"DAILY\",\"allow_comments\":true}",
                                    200);
                            verify(f.surveys)
                                    .save(
                                            argThat(
                                                    s ->
                                                            s.getTitle().equals("Daily")
                                                                    && s.getQuestion()
                                                                            .equals("How?")));
                        }),
                integration(
                        "close HTTP writes closed state",
                        () -> {
                            var f = new Fixture();
                            http(f.controller, "POST", "/api/v1/surveys/10/close", "", 200);
                            assertThat(f.survey.getStatus()).isEqualTo(SurveyStatus.CLOSED);
                            verify(f.surveys).save(f.survey);
                        }));
    }
}
