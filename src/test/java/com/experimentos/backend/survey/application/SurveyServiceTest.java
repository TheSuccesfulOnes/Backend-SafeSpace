package com.experimentos.backend.survey.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.Role;
import com.experimentos.backend.survey.domain.Survey;
import com.experimentos.backend.survey.domain.SurveyAnswer;
import com.experimentos.backend.survey.domain.SurveyStatus;
import com.experimentos.backend.survey.domain.SurveyType;
import com.experimentos.backend.survey.infrastructure.SurveyAnswerRepository;
import com.experimentos.backend.survey.infrastructure.SurveyRepository;
import com.experimentos.backend.survey.interfaces.SurveyDtos;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for the employee survey response rules and state projection. */
@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {
    @Mock SurveyRepository surveys;
    @Mock SurveyAnswerRepository answers;
    @Mock UserRepository users;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publishedSurveysExposeWhetherTheCurrentUserAlreadyAnswered() {
        User employee = user(2L, "maria", Role.EMPLOYEE);
        Survey survey = survey(10L, employee);
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("maria", "maria"))
                .thenReturn(Optional.of(employee));
        when(surveys.findByStatusOrderByIdDesc(SurveyStatus.PUBLISHED)).thenReturn(List.of(survey));
        when(answers.findBySurveyIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(new SurveyAnswer(survey, employee, "Already answered")));
        when(answers.countBySurveyId(10L)).thenReturn(4L);
        authenticateAs("maria");

        SurveyDtos.SurveyResponse response = service().published().getFirst();

        assertThat(response.answered()).isTrue();
        assertThat(response.answers()).isEqualTo(4L);
    }

    @Test
    void duplicateSurveyAnswerIsRejectedBeforeSaving() {
        User employee = user(2L, "maria", Role.EMPLOYEE);
        Survey survey = survey(10L, employee);
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("maria", "maria"))
                .thenReturn(Optional.of(employee));
        when(surveys.findById(10L)).thenReturn(Optional.of(survey));
        when(answers.findBySurveyIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(new SurveyAnswer(survey, employee, "Already answered")));
        authenticateAs("maria");

        assertThatThrownBy(
                        () ->
                                service()
                                        .answer(
                                                10L,
                                                new SurveyDtos.AnswerRequest("A second answer")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Survey has already been answered");
        verify(answers, never()).save(any());
    }

    private SurveyService service() {
        return new SurveyService(surveys, answers, users);
    }

    private Survey survey(Long id, User creator) {
        Survey survey = new Survey("Daily", "How was work?", SurveyType.DAILY, true, creator);
        ReflectionTestUtils.setField(survey, "id", id);
        survey.publish();
        return survey;
    }

    private User user(Long id, String username, Role role) {
        User user = new User(username, username + "@example.com", "hash", username, role);
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
