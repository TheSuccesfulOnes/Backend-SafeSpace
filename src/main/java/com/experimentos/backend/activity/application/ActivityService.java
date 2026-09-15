package com.experimentos.backend.activity.application;

import com.experimentos.backend.activity.domain.*;
import com.experimentos.backend.activity.infrastructure.*;
import com.experimentos.backend.activity.interfaces.ActivityDtos;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.CurrentUser;
import java.util.List;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {
    private final WeeklyActivityRepository activities;
    private final ActivityVoteRepository votes;
    private final UserRepository users;

    public ActivityService(
            WeeklyActivityRepository activities,
            ActivityVoteRepository votes,
            UserRepository users) {
        this.activities = activities;
        this.votes = votes;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ActivityDtos.ActivityResponse> open() {
        return activities.findByStatusOrderByIdDesc(ActivityStatus.OPEN).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Lists all activity states so HR can recover closed activities after a reload. */
    @Transactional(readOnly = true)
    public List<ActivityDtos.ActivityResponse> managed() {
        return activities.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ActivityDtos.ActivityResponse create(ActivityDtos.CreateActivityRequest request) {
        WeeklyActivity activity =
                new WeeklyActivity(
                        request.title().trim(),
                        normalizeNullable(request.description()),
                        currentUser());
        request.options().forEach(activity::addOption);
        return toResponse(activities.save(activity));
    }

    @Transactional
    public void close(Long id) {
        WeeklyActivity activity = find(id);
        activity.close();
        activities.save(activity);
    }

    @Transactional
    public void vote(Long id, ActivityDtos.VoteRequest request) {
        WeeklyActivity activity = find(id);
        if (activity.getStatus() != ActivityStatus.OPEN)
            throw new IllegalArgumentException("Activity is closed");
        if (request == null || request.optionId() == null)
            throw new IllegalArgumentException("Option is required");
        ActivityOption option =
                activity.getOptions().stream()
                        .filter(item -> Objects.equals(item.getId(), request.optionId()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Option does not belong to this activity"));
        User user = currentEmployee();
        votes.findByActivityIdAndUserId(id, user.getId())
                .ifPresent(vote -> votes.deleteById(new ActivityVote.VoteId(id, user.getId())));
        votes.save(new ActivityVote(id, user.getId(), option));
    }

    private ActivityDtos.ActivityResponse toResponse(WeeklyActivity activity) {
        long total = votes.countByActivityId(activity.getId());
        List<ActivityDtos.OptionResponse> options =
                activity.getOptions().stream()
                        .map(
                                option -> {
                                    long optionVotes = votes.countByOptionId(option.getId());
                                    double percentage =
                                            total == 0 ? 0 : (optionVotes * 100.0) / total;
                                    return new ActivityDtos.OptionResponse(
                                            option.getId(),
                                            option.getLabel(),
                                            optionVotes,
                                            percentage);
                                })
                        .toList();
        return new ActivityDtos.ActivityResponse(
                activity.getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getStatus().name(),
                options);
    }

    private WeeklyActivity find(Long id) {
        return activities
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity was not found"));
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
            throw new AccessDeniedException("Only employees can vote in activities");
        }
        return user;
    }

    private String normalizeNullable(String value) {
        return value == null ? null : value.trim();
    }
}
