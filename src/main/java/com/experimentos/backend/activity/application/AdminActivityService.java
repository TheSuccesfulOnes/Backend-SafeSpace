package com.experimentos.backend.activity.application;

import com.experimentos.backend.activity.domain.ActivityOption;
import com.experimentos.backend.activity.domain.WeeklyActivity;
import com.experimentos.backend.activity.infrastructure.ActivityVoteRepository;
import com.experimentos.backend.activity.infrastructure.WeeklyActivityRepository;
import com.experimentos.backend.activity.interfaces.ActivityAdminDtos;
import com.experimentos.backend.activity.interfaces.ActivityDtos;
import com.experimentos.backend.audit.application.AuditService;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.CurrentUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administrative use cases for complete weekly activity management. */
@Service
public class AdminActivityService {
    private final WeeklyActivityRepository activities;
    private final ActivityVoteRepository votes;
    private final UserRepository users;
    private final AuditService auditService;

    public AdminActivityService(
            WeeklyActivityRepository activities,
            ActivityVoteRepository votes,
            UserRepository users,
            AuditService auditService) {
        this.activities = activities;
        this.votes = votes;
        this.users = users;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ActivityAdminDtos.ActivityResponse> listAll() {
        return activities.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ActivityAdminDtos.ActivityResponse create(ActivityAdminDtos.ActivityRequest request) {
        User actor = currentAdmin();
        WeeklyActivity activity =
                new WeeklyActivity(
                        required(request.title()), normalizeNullable(request.description()), actor);
        request.options().forEach(option -> activity.addOption(required(option)));
        WeeklyActivity created = activities.save(activity);
        auditService.record(actor, "CREATE_ACTIVITY", "ACTIVITY", created.getId().toString());
        return toResponse(created);
    }

    @Transactional
    public ActivityAdminDtos.ActivityResponse update(
            Long id, ActivityAdminDtos.ActivityRequest request) {
        User actor = currentAdmin();
        WeeklyActivity activity = find(id);
        List<String> optionLabels = request.options().stream().map(this::required).toList();
        if (votes.countByActivityId(id) > 0
                && !activity.getOptions().stream()
                        .map(ActivityOption::getLabel)
                        .toList()
                        .equals(optionLabels)) {
            throw new IllegalArgumentException(
                    "Activity options cannot be changed after voting has started");
        }
        activity.update(
                required(request.title()), normalizeNullable(request.description()), optionLabels);
        auditService.record(actor, "UPDATE_ACTIVITY", "ACTIVITY", id.toString());
        return toResponse(activity);
    }

    @Transactional
    public ActivityAdminDtos.ActivityResponse open(Long id) {
        return changeStatus(id, "OPEN_ACTIVITY", WeeklyActivity::reopen);
    }

    @Transactional
    public ActivityAdminDtos.ActivityResponse close(Long id) {
        return changeStatus(id, "CLOSE_ACTIVITY", WeeklyActivity::close);
    }

    @Transactional
    public void delete(Long id) {
        User actor = currentAdmin();
        WeeklyActivity activity = find(id);
        activities.delete(activity);
        activities.flush();
        auditService.record(actor, "DELETE_ACTIVITY", "ACTIVITY", id.toString());
    }

    private ActivityAdminDtos.ActivityResponse changeStatus(
            Long id, String action, java.util.function.Consumer<WeeklyActivity> transition) {
        User actor = currentAdmin();
        WeeklyActivity activity = find(id);
        transition.accept(activity);
        auditService.record(actor, action, "ACTIVITY", id.toString());
        return toResponse(activity);
    }

    private ActivityAdminDtos.ActivityResponse toResponse(WeeklyActivity activity) {
        long total = votes.countByActivityId(activity.getId());
        List<ActivityDtos.OptionResponse> options =
                activity.getOptions().stream()
                        .map(option -> optionResponse(option, total))
                        .toList();
        return new ActivityAdminDtos.ActivityResponse(
                activity.getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getStatus().name(),
                options,
                activity.getCreatedBy().getUsername(),
                activity.getCreatedAt());
    }

    private ActivityDtos.OptionResponse optionResponse(ActivityOption option, long total) {
        long optionVotes = votes.countByOptionId(option.getId());
        double percentage = total == 0 ? 0 : (optionVotes * 100.0) / total;
        return new ActivityDtos.OptionResponse(
                option.getId(), option.getLabel(), optionVotes, percentage);
    }

    private WeeklyActivity find(Long id) {
        return activities
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity was not found"));
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

    private String normalizeNullable(String value) {
        return value == null ? null : value.trim();
    }
}
