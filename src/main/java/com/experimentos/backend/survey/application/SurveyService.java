package com.experimentos.backend.survey.application;

import com.experimentos.backend.comment.domain.Comment;
import com.experimentos.backend.comment.infrastructure.CommentRepository;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.CurrentUser;
import com.experimentos.backend.survey.domain.Survey;
import com.experimentos.backend.survey.domain.SurveyAnswer;
import com.experimentos.backend.survey.domain.SurveyStatus;
import com.experimentos.backend.survey.infrastructure.SurveyAnswerRepository;
import com.experimentos.backend.survey.infrastructure.SurveyRepository;
import com.experimentos.backend.survey.interfaces.SurveyDtos;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SurveyService {
    private final SurveyRepository surveys;
    private final SurveyAnswerRepository answers;
    private final CommentRepository comments;
    private final UserRepository users;

    public SurveyService(
            SurveyRepository surveys,
            SurveyAnswerRepository answers,
            CommentRepository comments,
            UserRepository users) {
        this.surveys = surveys;
        this.answers = answers;
        this.comments = comments;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<SurveyDtos.SurveyResponse> published() {
        Long currentUserId = currentUser().getId();
        return surveys.findByStatusOrderByIdDesc(SurveyStatus.PUBLISHED).stream()
                .map(survey -> toResponse(survey, currentUserId))
                .toList();
    }

    /** Lists all survey states for the HR management view. */
    @Transactional(readOnly = true)
    public List<SurveyDtos.SurveyResponse> managed() {
        return surveys.findAllByOrderByIdDesc().stream()
                .map(survey -> toResponse(survey, null))
                .toList();
    }

    @Transactional
    public SurveyDtos.SurveyResponse create(SurveyDtos.CreateSurveyRequest request) {
        User creator = currentUser();
        Survey survey =
                surveys.save(
                        new Survey(
                                request.title().trim(),
                                request.question().trim(),
                                request.type(),
                                request.allowComments(),
                                creator));
        return toResponse(survey, null);
    }

    @Transactional
    public SurveyDtos.SurveyResponse publish(Long id) {
        Survey survey = find(id);
        survey.publish();
        surveys.save(survey);
        return toResponse(survey, null);
    }

    @Transactional
    public SurveyDtos.SurveyResponse close(Long id) {
        Survey survey = find(id);
        survey.close();
        surveys.save(survey);
        return toResponse(survey, null);
    }

    @Transactional
    public void answer(Long id, SurveyDtos.AnswerRequest request) {
        Survey survey = find(id);
        if (survey.getStatus() != SurveyStatus.PUBLISHED)
            throw new IllegalArgumentException("Survey is not open");
        User user = currentEmployee();
        if (answers.findBySurveyIdAndUserId(id, user.getId()).isPresent())
            throw new IllegalArgumentException("Survey has already been answered");
        String answerText = request.answerText() == null ? "" : request.answerText().trim();
        if (answerText.isBlank()) throw new IllegalArgumentException("Answer cannot be blank");
        SurveyAnswer answer = answers.save(new SurveyAnswer(survey, user, answerText));
        try {
            comments.save(new Comment(survey, user, null, answerText));
        } catch (RuntimeException exception) {
            // Firestore writes are not covered by Spring's relational transaction manager.
            // Compensate the first write so answering never leaves a half-created response.
            answers.delete(answer);
            throw exception;
        }
    }

    private SurveyDtos.SurveyResponse toResponse(Survey survey, Long currentUserId) {
        boolean answered =
                currentUserId != null
                        && answers.findBySurveyIdAndUserId(survey.getId(), currentUserId)
                                .isPresent();
        return new SurveyDtos.SurveyResponse(
                survey.getId(),
                survey.getTitle(),
                survey.getQuestion(),
                survey.getType(),
                survey.getStatus().name(),
                survey.isAllowComments(),
                answers.countBySurveyId(survey.getId()),
                answered);
    }

    private Survey find(Long id) {
        return surveys.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Survey was not found"));
    }

    private User currentUser() {
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        CurrentUser.username(), CurrentUser.username())
                .orElseThrow(
                        () -> new IllegalArgumentException("Authenticated user was not found"));
    }

    private User currentEmployee() {
        User user = currentUser();
        if (user.getRole() != com.experimentos.backend.shared.security.Role.EMPLOYEE) {
            throw new AccessDeniedException("Only employees can answer surveys");
        }
        return user;
    }
}
