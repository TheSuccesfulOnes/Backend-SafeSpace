package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.activity.application.*;
import com.experimentos.backend.activity.domain.*;
import com.experimentos.backend.activity.infrastructure.*;
import com.experimentos.backend.activity.interfaces.*;
import com.experimentos.backend.audit.application.*;
import com.experimentos.backend.iam.domain.*;
import com.experimentos.backend.iam.infrastructure.*;
import com.experimentos.backend.shared.security.*;
import java.util.*;

class AdminActivityServiceValidationTest extends ScenarioContract {
    static final String JSON = "{\"title\":\" Walk \",\"options\":[\"Yoga\"]}";

    static class Fixture {
        final WeeklyActivityRepository activities = mock(WeeklyActivityRepository.class);
        final ActivityVoteRepository votes = mock(ActivityVoteRepository.class);
        final UserRepository users = mock(UserRepository.class);
        final AuditService audit = mock(AuditService.class);
        final User actor = user(1, Role.SYSTEM_ADMIN);
        final WeeklyActivity activity = id(new WeeklyActivity("Walk", null, actor), 10);
        final AdminActivityService service =
                new AdminActivityService(activities, votes, users, audit);
        final AdminActivityController controller = new AdminActivityController(service);

        Fixture() {
            authenticate(actor);
            activity.addOption("Yoga");
            id(activity.getOptions().getFirst(), 20);
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(Optional.of(actor));
            when(activities.findById(10L)).thenReturn(Optional.of(activity));
            when(activities.save(any())).thenAnswer(i -> id(i.getArgument(0), 10));
        }

        ActivityAdminDtos.ActivityRequest request(String title, List<String> options) {
            return new ActivityAdminDtos.ActivityRequest(title, null, options);
        }

        void noWrite() {
            verify(activities, never()).save(any());
            verifyNoInteractions(audit);
        }
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                unit(
                        "create blank title rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () -> f.service.create(f.request(" ", List.of("Yoga"))),
                                    "blank");
                            f.noWrite();
                        }),
                unit(
                        "create blank option rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () -> f.service.create(f.request("Walk", List.of(" "))),
                                    "blank");
                            f.noWrite();
                        }),
                unit(
                        "create null title rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () -> f.service.create(f.request(null, List.of("Yoga"))),
                                    "blank");
                            f.noWrite();
                        }),
                unit(
                        "update null option rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.service.update(
                                                    10L,
                                                    f.request(
                                                            "Walk", Arrays.asList((String) null))),
                                    "blank");
                            f.noWrite();
                        }),
                unit(
                        "voted activity options cannot reorder",
                        () -> {
                            var f = new Fixture();
                            f.activity.addOption("Cinema");
                            when(f.votes.countByActivityId(10L)).thenReturn(1L);
                            rejected(
                                    () ->
                                            f.service.update(
                                                    10L,
                                                    f.request("Walk", List.of("Cinema", "Yoga"))),
                                    "cannot be changed");
                            f.noWrite();
                        }),
                unit(
                        "voted activity cannot remove options",
                        () -> {
                            var f = new Fixture();
                            f.activity.addOption("Cinema");
                            when(f.votes.countByActivityId(10L)).thenReturn(1L);
                            rejected(
                                    () -> f.service.update(10L, f.request("Walk", List.of("Yoga"))),
                                    "cannot be changed");
                            f.noWrite();
                        }),
                unit(
                        "voted activity same labels can rename title",
                        () -> {
                            var f = new Fixture();
                            when(f.votes.countByActivityId(10L)).thenReturn(1L);
                            f.service.update(10L, f.request("Renamed", List.of(" Yoga ")));
                            assertThat(f.activity.getTitle()).isEqualTo("Renamed");
                        }),
                unit(
                        "unvoted activity can replace options",
                        () -> {
                            var f = new Fixture();
                            f.service.update(10L, f.request("Walk", List.of("Cinema")));
                            assertThat(f.activity.getOptions())
                                    .extracting(ActivityOption::getLabel)
                                    .containsExactly("Cinema");
                        }),
                unit(
                        "missing activity update rejected",
                        () -> {
                            var f = new Fixture();
                            when(f.activities.findById(10L)).thenReturn(Optional.empty());
                            rejected(
                                    () -> f.service.update(10L, f.request("Walk", List.of("Yoga"))),
                                    "not found");
                            f.noWrite();
                        }),
                unit(
                        "missing administrator prevents create",
                        () -> {
                            var f = new Fixture();
                            when(f.users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                            "actor", "actor"))
                                    .thenReturn(Optional.empty());
                            assertThatThrownBy(
                                            () ->
                                                    f.service.create(
                                                            f.request("Walk", List.of("Yoga"))))
                                    .isInstanceOf(IllegalStateException.class);
                            f.noWrite();
                        }),
                unit(
                        "close audited",
                        () -> {
                            var f = new Fixture();
                            assertThat(f.service.close(10L).status()).isEqualTo("CLOSED");
                            verify(f.audit).record(f.actor, "CLOSE_ACTIVITY", "ACTIVITY", "10");
                        }),
                unit(
                        "delete removes votes before entity",
                        () -> {
                            var f = new Fixture();
                            f.service.delete(10L);
                            var order = inOrder(f.votes, f.activities, f.audit);
                            order.verify(f.votes).deleteByActivityId(10L);
                            order.verify(f.activities).delete(f.activity);
                            order.verify(f.activities).flush();
                            order.verify(f.audit)
                                    .record(f.actor, "DELETE_ACTIVITY", "ACTIVITY", "10");
                        }),
                integration(
                        "create HTTP persists and audits",
                        () -> {
                            var f = new Fixture();
                            http(f.controller, "POST", "/api/v1/admin/activities", JSON, 201);
                            verify(f.activities).save(any());
                            verify(f.audit).record(f.actor, "CREATE_ACTIVITY", "ACTIVITY", "10");
                        }),
                integration(
                        "update HTTP title trimmed",
                        () -> {
                            var f = new Fixture();
                            http(f.controller, "PUT", "/api/v1/admin/activities/10", JSON, 200);
                            assertThat(f.activity.getTitle()).isEqualTo("Walk");
                            verify(f.audit).record(f.actor, "UPDATE_ACTIVITY", "ACTIVITY", "10");
                        }),
                integration(
                        "voted option change HTTP blocked",
                        () -> {
                            var f = new Fixture();
                            when(f.votes.countByActivityId(10L)).thenReturn(1L);
                            http(
                                    f.controller,
                                    "PUT",
                                    "/api/v1/admin/activities/10",
                                    "{\"title\":\"Walk\",\"options\":[\"Cinema\"]}",
                                    400);
                            f.noWrite();
                        }),
                integration(
                        "voted same options HTTP accepted",
                        () -> {
                            var f = new Fixture();
                            when(f.votes.countByActivityId(10L)).thenReturn(1L);
                            http(f.controller, "PUT", "/api/v1/admin/activities/10", JSON, 200);
                            verify(f.activities).save(f.activity);
                        }),
                integration(
                        "open HTTP lifecycle",
                        () -> {
                            var f = new Fixture();
                            f.activity.close();
                            http(
                                    f.controller,
                                    "PATCH",
                                    "/api/v1/admin/activities/10/open",
                                    "",
                                    200);
                            assertThat(f.activity.getStatus()).isEqualTo(ActivityStatus.OPEN);
                        }),
                integration(
                        "close HTTP lifecycle",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "PATCH",
                                    "/api/v1/admin/activities/10/close",
                                    "",
                                    200);
                            assertThat(f.activity.getStatus()).isEqualTo(ActivityStatus.CLOSED);
                        }),
                integration(
                        "delete HTTP cleans dependent votes",
                        () -> {
                            var f = new Fixture();
                            http(f.controller, "DELETE", "/api/v1/admin/activities/10", "", 204);
                            verify(f.votes).deleteByActivityId(10L);
                            verify(f.activities).delete(f.activity);
                        }),
                integration(
                        "missing administrator HTTP unavailable",
                        () -> {
                            var f = new Fixture();
                            when(f.users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                            "actor", "actor"))
                                    .thenReturn(Optional.empty());
                            http(f.controller, "POST", "/api/v1/admin/activities", JSON, 503);
                            f.noWrite();
                        }));
    }
}
