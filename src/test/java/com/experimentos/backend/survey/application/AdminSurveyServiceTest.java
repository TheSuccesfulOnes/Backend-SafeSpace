package com.experimentos.backend.survey.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.experimentos.backend.audit.application.AuditService;
import com.experimentos.backend.comment.infrastructure.CommentLikeRepository;
import com.experimentos.backend.comment.infrastructure.CommentRepository;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.Role;
import com.experimentos.backend.survey.domain.Survey;
import com.experimentos.backend.survey.domain.SurveyAnswer;
import com.experimentos.backend.survey.domain.SurveyType;
import com.experimentos.backend.survey.infrastructure.SurveyAnswerRepository;
import com.experimentos.backend.survey.infrastructure.SurveyRepository;
import com.experimentos.backend.survey.interfaces.SurveyAdminDtos;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for administrative survey management and detailed answers. */
@ExtendWith(MockitoExtension.class)
class AdminSurveyServiceTest {
    @Mock SurveyRepository surveys;
    @Mock SurveyAnswerRepository answers;
    @Mock CommentRepository comments;
    @Mock CommentLikeRepository likes;
    @Mock UserRepository users;
    @Mock AuditService auditService;

    private User admin;
    private AdminSurveyService service;

    @BeforeEach
    void setUp() {
        admin = user(1L, "admin", Role.SYSTEM_ADMIN);
        service = new AdminSurveyService(surveys, answers, comments, likes, users, auditService);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                "admin",
                                null,
                                java.util.List.of(
                                        new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminCanListEverySurveyAndItsAnswerCount() {
        Survey survey = new Survey("Daily", "How was work?", SurveyType.DAILY, true, admin);
        ReflectionTestUtils.setField(survey, "id", 10L);
        when(surveys.findAllByOrderByIdDesc()).thenReturn(List.of(survey));
        when(answers.countBySurveyId(10L)).thenReturn(3L);

        List<SurveyAdminDtos.SurveyResponse> result = service.listAll();

        assertThat(result)
                .singleElement()
                .satisfies(
                        item -> {
                            assertThat(item.id()).isEqualTo(10L);
                            assertThat(item.answers()).isEqualTo(3L);
                            assertThat(item.createdBy()).isEqualTo("admin");
                        });
    }

    @Test
    void adminCanReadDetailedSurveyAnswers() {
        Survey survey = new Survey("Daily", "How was work?", SurveyType.DAILY, true, admin);
        ReflectionTestUtils.setField(survey, "id", 10L);
        User employee = user(2L, "maria", Role.EMPLOYEE);
        SurveyAnswer answer = new SurveyAnswer(survey, employee, "It was good");
        ReflectionTestUtils.setField(answer, "id", 50L);
        when(surveys.findById(10L)).thenReturn(Optional.of(survey));
        when(answers.findBySurveyIdOrderByIdAsc(10L)).thenReturn(List.of(answer));

        List<SurveyAdminDtos.AnswerResponse> result = service.listAnswers(10L);

        assertThat(result)
                .singleElement()
                .satisfies(
                        item -> {
                            assertThat(item.id()).isEqualTo(50L);
                            assertThat(item.username()).isEqualTo("maria");
                            assertThat(item.answerText()).isEqualTo("It was good");
                        });
    }

    @Test
    void adminCanReopenAClosedSurvey() {
        Survey survey = new Survey("Daily", "How was work?", SurveyType.DAILY, true, admin);
        ReflectionTestUtils.setField(survey, "id", 10L);
        survey.close();
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(admin));
        when(surveys.findById(10L)).thenReturn(Optional.of(survey));
        when(answers.countBySurveyId(10L)).thenReturn(0L);

        SurveyAdminDtos.SurveyResponse result = service.reopen(10L);

        assertThat(result.status()).isEqualTo("DRAFT");
    }

    private User user(Long id, String username, Role role) {
        User user = new User(username, username + "@example.com", "hash", username, role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
