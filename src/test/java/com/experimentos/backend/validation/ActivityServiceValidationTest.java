package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.activity.application.*;
import com.experimentos.backend.activity.domain.*;
import com.experimentos.backend.activity.infrastructure.*;
import com.experimentos.backend.activity.interfaces.*;
import com.experimentos.backend.iam.domain.*;
import com.experimentos.backend.iam.infrastructure.*;
import com.experimentos.backend.shared.security.*;
import java.util.*;

class ActivityServiceValidationTest extends ScenarioContract {
    static class Fixture {
        final WeeklyActivityRepository activities = mock(WeeklyActivityRepository.class);
        final ActivityVoteRepository votes = mock(ActivityVoteRepository.class);
        final UserRepository users = mock(UserRepository.class);
        final User actor = user(1, Role.EMPLOYEE);
        final WeeklyActivity activity = id(new WeeklyActivity("Walk", null, actor), 10);
        final ActivityService service = new ActivityService(activities, votes, users);
        final ActivityController controller = new ActivityController(service);

        Fixture() {
            authenticate(actor);
            activity.addOption("Yoga");
            activity.addOption("Walk");
            id(activity.getOptions().get(0), 20);
            id(activity.getOptions().get(1), 21);
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(Optional.of(actor));
            when(activities.findById(10L)).thenReturn(Optional.of(activity));
            when(activities.save(any())).thenAnswer(i -> id(i.getArgument(0), 10));
        }

        void vote(Long option) {
            service.vote(10L, new ActivityDtos.VoteRequest(option));
        }

        void noVote() {
            verify(votes, never()).save(any());
            verify(votes, never()).deleteById(any());
        }
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                unit(
                        "vote missing activity",
                        () -> {
                            var f = new Fixture();
                            when(f.activities.findById(10L)).thenReturn(Optional.empty());
                            rejected(() -> f.vote(20L), "not found");
                            f.noVote();
                        }),
                unit(
                        "closed activity rejected",
                        () -> {
                            var f = new Fixture();
                            f.activity.close();
                            rejected(() -> f.vote(20L), "closed");
                            f.noVote();
                        }),
                unit(
                        "null vote request",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.service.vote(10L, null), "Option is required");
                            f.noVote();
                        }),
                unit(
                        "null option id",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.vote(null), "Option is required");
                            f.noVote();
                        }),
                unit(
                        "foreign option",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.vote(999L), "does not belong");
                            f.noVote();
                        }),
                unit(
                        "HR voting forbidden",
                        () -> {
                            var f = new Fixture();
                            f.actor.changeRole(Role.HR_MEMBER);
                            assertThatThrownBy(() -> f.vote(20L))
                                    .isInstanceOf(
                                            org.springframework.security.access
                                                    .AccessDeniedException.class);
                            f.noVote();
                        }),
                unit(
                        "admin voting forbidden",
                        () -> {
                            var f = new Fixture();
                            f.actor.changeRole(Role.SYSTEM_ADMIN);
                            assertThatThrownBy(() -> f.vote(20L))
                                    .isInstanceOf(
                                            org.springframework.security.access
                                                    .AccessDeniedException.class);
                            f.noVote();
                        }),
                unit(
                        "unknown authenticated employee",
                        () -> {
                            var f = new Fixture();
                            when(f.users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                            "actor", "actor"))
                                    .thenReturn(Optional.empty());
                            rejected(() -> f.vote(20L), "Authenticated user");
                            f.noVote();
                        }),
                unit(
                        "first vote does not delete",
                        () -> {
                            var f = new Fixture();
                            f.vote(20L);
                            verify(f.votes, never()).deleteById(any());
                            verify(f.votes)
                                    .save(
                                            argThat(
                                                    v ->
                                                            v.getUserId() == 1L
                                                                    && v.getOption().getId()
                                                                            == 20L));
                        }),
                unit(
                        "changed vote replaces same composite identity",
                        () -> {
                            var f = new Fixture();
                            when(f.votes.findByActivityIdAndUserId(10L, 1L))
                                    .thenReturn(
                                            Optional.of(
                                                    new ActivityVote(
                                                            10L,
                                                            1L,
                                                            f.activity.getOptions().getFirst())));
                            f.vote(21L);
                            var order = inOrder(f.votes);
                            order.verify(f.votes).deleteById(new ActivityVote.VoteId(10L, 1L));
                            order.verify(f.votes).save(argThat(v -> v.getOption().getId() == 21L));
                        }),
                unit(
                        "close persisted state",
                        () -> {
                            var f = new Fixture();
                            f.service.close(10L);
                            assertThat(f.activity.getStatus()).isEqualTo(ActivityStatus.CLOSED);
                            verify(f.activities).save(f.activity);
                        }),
                unit(
                        "create normalizes all labels",
                        () -> {
                            var f = new Fixture();
                            var r =
                                    f.service.create(
                                            new ActivityDtos.CreateActivityRequest(
                                                    " Walk ", " Outside ", List.of(" Yoga ")));
                            assertThat(r.title()).isEqualTo("Walk");
                            assertThat(r.options().getFirst().label()).isEqualTo("Yoga");
                        }),
                integration(
                        "vote endpoint saves employee choice",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/activities/10/votes",
                                    "{\"option_id\":20}",
                                    200);
                            verify(f.votes).save(any());
                        }),
                integration(
                        "closed vote endpoint rejects",
                        () -> {
                            var f = new Fixture();
                            f.activity.close();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/activities/10/votes",
                                    "{\"option_id\":20}",
                                    400);
                            f.noVote();
                        }),
                integration(
                        "foreign choice endpoint rejects",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/activities/10/votes",
                                    "{\"option_id\":999}",
                                    400);
                            f.noVote();
                        }),
                integration(
                        "HR vote endpoint forbidden",
                        () -> {
                            var f = new Fixture();
                            f.actor.changeRole(Role.HR_MEMBER);
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/activities/10/votes",
                                    "{\"option_id\":20}",
                                    403);
                            f.noVote();
                        }),
                integration(
                        "admin vote endpoint forbidden",
                        () -> {
                            var f = new Fixture();
                            f.actor.changeRole(Role.SYSTEM_ADMIN);
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/activities/10/votes",
                                    "{\"option_id\":20}",
                                    403);
                            f.noVote();
                        }),
                integration(
                        "missing activity vote endpoint",
                        () -> {
                            var f = new Fixture();
                            when(f.activities.findById(10L)).thenReturn(Optional.empty());
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/activities/10/votes",
                                    "{\"option_id\":20}",
                                    400);
                            f.noVote();
                        }),
                integration(
                        "close endpoint persists",
                        () -> {
                            var f = new Fixture();
                            http(f.controller, "POST", "/api/v1/activities/10/close", "", 200);
                            verify(f.activities).save(f.activity);
                            assertThat(f.activity.getStatus()).isEqualTo(ActivityStatus.CLOSED);
                        }),
                integration(
                        "create endpoint persists normalized data",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/activities",
                                    "{\"title\":\" Walk \",\"options\":[\" Yoga \"]}",
                                    200);
                            verify(f.activities)
                                    .save(
                                            argThat(
                                                    a ->
                                                            a.getTitle().equals("Walk")
                                                                    && a.getOptions()
                                                                            .getFirst()
                                                                            .getLabel()
                                                                            .equals("Yoga")));
                        }));
    }
}
