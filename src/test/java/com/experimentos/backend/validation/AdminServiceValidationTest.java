package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.activity.infrastructure.*;
import com.experimentos.backend.admin.application.*;
import com.experimentos.backend.admin.interfaces.*;
import com.experimentos.backend.audit.application.*;
import com.experimentos.backend.iam.domain.*;
import com.experimentos.backend.iam.infrastructure.*;
import com.experimentos.backend.shared.security.*;
import com.experimentos.backend.survey.infrastructure.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminServiceValidationTest extends ScenarioContract {
    static class Fixture {
        final UserRepository users = mock(UserRepository.class);
        final SurveyRepository surveys = mock(SurveyRepository.class);
        final WeeklyActivityRepository activities = mock(WeeklyActivityRepository.class);
        final AuditService audit = mock(AuditService.class);
        final PasswordEncoder encoder = mock(PasswordEncoder.class);
        User actor = user(1, Role.SYSTEM_ADMIN);
        final User target = user(2, Role.EMPLOYEE);
        final AdminService service = new AdminService(users, surveys, activities, encoder, audit);
        final AdminController controller = new AdminController(service);

        Fixture() {
            actor.markAsSystemOwner();
            authenticate(actor);
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(Optional.of(actor));
            when(users.findById(1L)).thenReturn(Optional.of(actor));
            when(users.findById(2L)).thenReturn(Optional.of(target));
            when(users.save(any())).thenAnswer(i -> id(i.getArgument(0), 3));
            when(encoder.encode(anyString())).thenReturn("encoded");
        }

        void noWrite() {
            verify(users, never()).save(any());
            verifyNoInteractions(audit);
        }
    }

    static User newActor(Fixture f) {
        var actor = user(1, Role.SYSTEM_ADMIN);
        f.target.markAsSystemOwner();
        when(f.users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                .thenReturn(Optional.of(actor));
        return actor;
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                unit(
                        "empty patch rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.service.updateUser(
                                                    2L,
                                                    new AdminDtos.UpdateUserRequest(
                                                            null, null, null, null)),
                                    "At least one");
                            f.noWrite();
                        }),
                unit(
                        "blank username patch rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.service.updateUser(
                                                    2L,
                                                    new AdminDtos.UpdateUserRequest(
                                                            " ", null, null, null)),
                                    "Username cannot");
                            f.noWrite();
                        }),
                unit(
                        "blank email patch rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.service.updateUser(
                                                    2L,
                                                    new AdminDtos.UpdateUserRequest(
                                                            null, " ", null, null)),
                                    "Email cannot");
                            f.noWrite();
                        }),
                unit(
                        "blank displayName patch rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.service.updateUser(
                                                    2L,
                                                    new AdminDtos.UpdateUserRequest(
                                                            null, null, " ", null)),
                                    "Display name");
                            f.noWrite();
                        }),
                unit(
                        "own role cannot change",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.service.updateUser(
                                                    1L,
                                                    new AdminDtos.UpdateUserRequest(
                                                            null, null, null, Role.HR_MEMBER)),
                                    "own role");
                            f.noWrite();
                        }),
                unit(
                        "self disable rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.service.disableUser(1L), "own access");
                            f.noWrite();
                        }),
                unit(
                        "self deletion rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.service.deleteUser(1L), "own access");
                            verify(f.users, never()).delete(any());
                        }),
                unit(
                        "only administrator cannot demote",
                        () -> {
                            var f = new Fixture();
                            f.target.changeRole(Role.SYSTEM_ADMIN);
                            when(f.users.countByRole(Role.SYSTEM_ADMIN)).thenReturn(1L);
                            rejected(
                                    () ->
                                            f.service.updateUser(
                                                    2L,
                                                    new AdminDtos.UpdateUserRequest(
                                                            null, null, null, Role.EMPLOYEE)),
                                    "administrator must remain");
                            f.noWrite();
                        }),
                unit(
                        "survey owner cannot delete",
                        () -> {
                            var f = new Fixture();
                            when(f.surveys.existsByCreatedById(2L)).thenReturn(true);
                            rejected(() -> f.service.deleteUser(2L), "must be disabled");
                            verify(f.users, never()).delete(any());
                        }),
                unit(
                        "activity owner cannot delete",
                        () -> {
                            var f = new Fixture();
                            when(f.activities.existsByCreatedById(2L)).thenReturn(true);
                            rejected(() -> f.service.deleteUser(2L), "must be disabled");
                            verify(f.users, never()).delete(any());
                        }),
                unit(
                        "duplicate username patch",
                        () -> {
                            var f = new Fixture();
                            when(f.users.existsByUsernameIgnoreCaseAndIdNot("taken", 2L))
                                    .thenReturn(true);
                            rejected(
                                    () ->
                                            f.service.updateUser(
                                                    2L,
                                                    new AdminDtos.UpdateUserRequest(
                                                            "taken", null, null, null)),
                                    "Username is already");
                            f.noWrite();
                        }),
                unit(
                        "duplicate email patch",
                        () -> {
                            var f = new Fixture();
                            when(f.users.existsByEmailIgnoreCaseAndIdNot("taken@example.test", 2L))
                                    .thenReturn(true);
                            rejected(
                                    () ->
                                            f.service.updateUser(
                                                    2L,
                                                    new AdminDtos.UpdateUserRequest(
                                                            null,
                                                            "TAKEN@EXAMPLE.TEST",
                                                            null,
                                                            null)),
                                    "Email is already");
                            f.noWrite();
                        }),
                integration(
                        "create employee HTTP hashes then audit",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/admin/users",
                                    "{\"username\":\"new\",\"email\":\"new@example.test\",\"password\":\"Password1!\",\"display_name\":\"New\",\"role\":\"EMPLOYEE\"}",
                                    201);
                            verify(f.users)
                                    .save(
                                            argThat(
                                                    u ->
                                                            u.getRole() == Role.EMPLOYEE
                                                                    && u.getPasswordHash()
                                                                            .equals("encoded")));
                            verify(f.audit).record(f.actor, "CREATE_USER", "USER", "3");
                        }),
                integration(
                        "create HR HTTP optional email",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/admin/hr-members",
                                    "{\"username\":\"hr\",\"password\":\"Password1!\",\"display_name\":\"HR\"}",
                                    201);
                            verify(f.users)
                                    .save(
                                            argThat(
                                                    u ->
                                                            u.getRole() == Role.HR_MEMBER
                                                                    && u.getEmail() == null));
                        }),
                integration(
                        "self disable HTTP prohibited",
                        () -> {
                            var f = new Fixture();
                            http(f.controller, "PATCH", "/api/v1/admin/users/1/disable", "", 400);
                            f.noWrite();
                        }),
                integration(
                        "last administrator disable HTTP",
                        () -> {
                            var f = new Fixture();
                            f.target.changeRole(Role.SYSTEM_ADMIN);
                            when(f.users.countByRole(Role.SYSTEM_ADMIN)).thenReturn(1L);
                            http(f.controller, "PATCH", "/api/v1/admin/users/2/disable", "", 400);
                            f.noWrite();
                        }),
                integration(
                        "owner account patch secondary admin HTTP",
                        () -> {
                            var f = new Fixture();
                            f.actor = newActor(f);
                            http(
                                    f.controller,
                                    "PATCH",
                                    "/api/v1/admin/users/2",
                                    "{\"display_name\":\"Other\"}",
                                    400);
                            f.noWrite();
                        }),
                integration(
                        "enable disabled user HTTP",
                        () -> {
                            var f = new Fixture();
                            f.target.disable();
                            http(f.controller, "PATCH", "/api/v1/admin/users/2/enable", "", 200);
                            assertThat(f.target.isEnabled()).isTrue();
                            verify(f.users).save(f.target);
                        }),
                integration(
                        "reset password HTTP audits",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/admin/users/2/password",
                                    "{\"new_password\":\"Password1!\"}",
                                    204);
                            assertThat(f.target.getPasswordHash()).isEqualTo("encoded");
                            verify(f.audit).record(f.actor, "RESET_PASSWORD", "USER", "2");
                        }),
                integration(
                        "delete owned content HTTP rejects",
                        () -> {
                            var f = new Fixture();
                            when(f.activities.existsByCreatedById(2L)).thenReturn(true);
                            http(f.controller, "DELETE", "/api/v1/admin/users/2", "", 400);
                            verify(f.users, never()).delete(any());
                        }));
    }
}
