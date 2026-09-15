package com.experimentos.backend.survey.application;

import com.experimentos.backend.audit.application.AuditService;
import com.experimentos.backend.comment.infrastructure.CommentLikeRepository;
import com.experimentos.backend.comment.infrastructure.CommentRepository;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.CurrentUser;
import com.experimentos.backend.survey.domain.Survey;
import com.experimentos.backend.survey.infrastructure.SurveyAnswerRepository;
import com.experimentos.backend.survey.infrastructure.SurveyRepository;
import com.experimentos.backend.survey.interfaces.SurveyAdminDtos;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administrative use cases for complete survey lifecycle management. */
@Service
public class AdminSurveyService {
    private final SurveyRepository surveys;
    private final SurveyAnswerRepository answers;
    private final CommentRepository comments;
    private final CommentLikeRepository likes;
    private final UserRepository users;
    private final AuditService auditService;

    public AdminSurveyService(
            SurveyRepository surveys,
            SurveyAnswerRepository answers,
            CommentRepository comments,
            CommentLikeRepository likes,
            UserRepository users,
            AuditService auditService) {
        this.surveys = surveys;
        this.answers = answers;
        this.comments = comments;
        this.likes = likes;
        this.users = users;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SurveyAdminDtos.SurveyResponse> listAll() {
        return surveys.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SurveyAdminDtos.AnswerResponse> listAnswers(Long surveyId) {
        find(surveyId);
        return answers.findBySurveyIdOrderByIdAsc(surveyId).stream()
                .map(
                        answer ->
                                new SurveyAdminDtos.AnswerResponse(
                                        answer.getId(),
                                        surveyId,
                                        answer.getUser() == null
                                                ? null
                                                : answer.getUser().getUsername(),
                                        answer.getUser() == null
                                                ? null
                                                : answer.getUser().getEmail(),
                                        answer.getUser() == null
                                                ? null
                                                : answer.getUser().getDisplayName(),
                                        answer.getAnswerText(),
                                        answer.getCreatedAt()))
                .toList();
    }

    @Transactional
    public SurveyAdminDtos.SurveyResponse create(SurveyAdminDtos.SurveyRequest request) {
        User actor = currentAdmin();
        Survey survey =
                surveys.save(
                        new Survey(
                                required(request.title()),
                                required(request.question()),
                                request.type(),
                                request.allowComments(),
                                actor));
        auditService.record(actor, "CREATE_SURVEY", "SURVEY", survey.getId().toString());
        return toResponse(survey);
    }

    @Transactional
    public SurveyAdminDtos.SurveyResponse update(Long id, SurveyAdminDtos.SurveyRequest request) {
        User actor = currentAdmin();
        Survey survey = find(id);
        survey.update(
                required(request.title()),
                required(request.question()),
                request.type(),
                request.allowComments());
        surveys.save(survey);
        auditService.record(actor, "UPDATE_SURVEY", "SURVEY", id.toString());
        return toResponse(survey);
    }

    @Transactional
    public SurveyAdminDtos.SurveyResponse publish(Long id) {
        return changeStatus(id, "PUBLISH_SURVEY", Survey::publish);
    }

    @Transactional
    public SurveyAdminDtos.SurveyResponse close(Long id) {
        return changeStatus(id, "CLOSE_SURVEY", Survey::close);
    }

    @Transactional
    public SurveyAdminDtos.SurveyResponse reopen(Long id) {
        return changeStatus(id, "REOPEN_SURVEY", Survey::reopen);
    }

    @Transactional
    public void delete(Long id) {
        User actor = currentAdmin();
        Survey survey = find(id);
        comments.findBySurveyId(id).forEach(comment -> likes.deleteByCommentId(comment.getId()));
        comments.deleteBySurveyId(id);
        answers.deleteBySurveyId(id);
        surveys.delete(survey);
        surveys.flush();
        auditService.record(actor, "DELETE_SURVEY", "SURVEY", id.toString());
    }

    private SurveyAdminDtos.SurveyResponse changeStatus(
            Long id, String action, java.util.function.Consumer<Survey> transition) {
        User actor = currentAdmin();
        Survey survey = find(id);
        transition.accept(survey);
        surveys.save(survey);
        auditService.record(actor, action, "SURVEY", id.toString());
        return toResponse(survey);
    }

    private SurveyAdminDtos.SurveyResponse toResponse(Survey survey) {
        return new SurveyAdminDtos.SurveyResponse(
                survey.getId(),
                survey.getTitle(),
                survey.getQuestion(),
                survey.getType(),
                survey.getStatus().name(),
                survey.isAllowComments(),
                answers.countBySurveyId(survey.getId()),
                survey.getCreatedBy() == null ? null : survey.getCreatedBy().getUsername(),
                survey.getCreatedAt());
    }

    private Survey find(Long id) {
        return surveys.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Survey was not found"));
    }

    private User currentAdmin() {
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        CurrentUser.username(), CurrentUser.username())
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Authenticated administrator was not found"));
    }

    private String required(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) throw new IllegalArgumentException("Field cannot be blank");
        return normalized;
    }
}
