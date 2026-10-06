package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.report.application.ReportService;
import com.experimentos.backend.report.domain.*;
import com.experimentos.backend.report.infrastructure.ReportRepository;
import com.experimentos.backend.report.interfaces.ReportController;
import com.experimentos.backend.shared.security.Role;
import java.util.*;

class ReportPrivacyValidationTest extends ScenarioContract {
    static class Fixture {
        final User actor = user(1, Role.EMPLOYEE);
        Report report = report(true);
        final ReportRepository reports = mock(ReportRepository.class);
        final UserRepository users = mock(UserRepository.class);
        final ReportController controller = new ReportController(new ReportService(reports, users));

        Report report(boolean anon) {
            return id(
                    new Report(actor, "Work", "Issue", "Detail", ReportPriority.NORMAL, anon), 10);
        }

        void setup(Role role, boolean anon) {
            actor.changeRole(role);
            report = report(anon);
            authenticate(actor);
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(Optional.of(actor));
            when(reports.findByUserIdOrderByIdDesc(1L)).thenReturn(List.of(report));
            when(reports.findById(10L)).thenReturn(Optional.of(report));
        }
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                unit(
                        "anonymous report never returns author display name",
                        () -> {
                            var f = new Fixture();
                            assertThat(f.report.getReporterDisplayName()).isNull();
                        }),
                unit(
                        "anonymous orphan safe",
                        () -> {
                            var f = new Fixture();
                            f.report =
                                    new Report(
                                            null,
                                            "Work",
                                            "Issue",
                                            "Detail",
                                            ReportPriority.NORMAL,
                                            true);
                            assertThat(f.report.getReporterDisplayName()).isNull();
                        }),
                unit(
                        "named report returns consenting author display name",
                        () -> {
                            var f = new Fixture();
                            f.report = f.report(false);
                            assertThat(f.report.getReporterDisplayName()).isEqualTo("Actor");
                        }),
                unit(
                        "named orphan safe",
                        () -> {
                            var f = new Fixture();
                            f.report =
                                    new Report(
                                            null,
                                            "Work",
                                            "Issue",
                                            "Detail",
                                            ReportPriority.NORMAL,
                                            false);
                            assertThat(f.report.getReporterDisplayName()).isNull();
                        }),
                unit(
                        "missing display name does not expose username",
                        () -> {
                            var f = new Fixture();
                            f.report = f.report(false);
                            f.actor.updateProfile("private-username", "private@example.test", null);
                            assertThat(f.report.getReporterDisplayName()).isNull();
                        }),
                unit(
                        "profile rename cannot de-anonymize report",
                        () -> {
                            var f = new Fixture();
                            f.actor.updateProfile(
                                    "renamed", "new@example.test", "Changed private name");
                            assertThat(f.report.getReporterDisplayName()).isNull();
                        }),
                unit(
                        "named report never substitutes email for display name",
                        () -> {
                            var f = new Fixture();
                            f.report = f.report(false);
                            f.actor.updateProfile(
                                    "private-username", "private@example.test", "Public alias");
                            assertThat(f.report.getReporterDisplayName()).isEqualTo("Public alias");
                        }),
                unit(
                        "status transitions cannot de-anonymize report",
                        () -> {
                            var f = new Fixture();
                            for (var status : ReportStatus.values()) {
                                f.report.updateStatus(status);
                                assertThat(f.report.getReporterDisplayName()).isNull();
                                assertThat(f.report.isAnonymous()).isTrue();
                            }
                        }),
                integration(
                        "EMPLOYEE own report anonymous serialized",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.EMPLOYEE, true);
                            var r = http(f.controller, "GET", "/api/v1/reports/mine", "", 200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"reporter_display_name\":null")
                                    .doesNotContain("Actor", "actor@example.test");
                        }),
                integration(
                        "EMPLOYEE status response anonymous serialized",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.EMPLOYEE, true);
                            var r =
                                    http(
                                            f.controller,
                                            "PATCH",
                                            "/api/v1/reports/10/status",
                                            "{\"status\":\"IN_REVIEW\"}",
                                            200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"reporter_display_name\":null")
                                    .doesNotContain("Actor", "actor@example.test");
                        }),
                integration(
                        "EMPLOYEE own report named serialized",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.EMPLOYEE, false);
                            var r = http(f.controller, "GET", "/api/v1/reports/mine", "", 200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"reporter_display_name\":\"Actor\"")
                                    .doesNotContain("actor@example.test");
                        }),
                integration(
                        "EMPLOYEE status response named serialized",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.EMPLOYEE, false);
                            var r =
                                    http(
                                            f.controller,
                                            "PATCH",
                                            "/api/v1/reports/10/status",
                                            "{\"status\":\"IN_REVIEW\"}",
                                            200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"reporter_display_name\":\"Actor\"")
                                    .doesNotContain("actor@example.test");
                        }),
                integration(
                        "HR_MEMBER own report anonymous serialized",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.HR_MEMBER, true);
                            var r = http(f.controller, "GET", "/api/v1/reports/mine", "", 200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"reporter_display_name\":null")
                                    .doesNotContain("Actor", "actor@example.test");
                        }),
                integration(
                        "HR_MEMBER status response anonymous serialized",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.HR_MEMBER, true);
                            var r =
                                    http(
                                            f.controller,
                                            "PATCH",
                                            "/api/v1/reports/10/status",
                                            "{\"status\":\"IN_REVIEW\"}",
                                            200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"reporter_display_name\":null")
                                    .doesNotContain("Actor", "actor@example.test");
                        }),
                integration(
                        "HR_MEMBER own report named serialized",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.HR_MEMBER, false);
                            var r = http(f.controller, "GET", "/api/v1/reports/mine", "", 200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"reporter_display_name\":\"Actor\"")
                                    .doesNotContain("actor@example.test");
                        }),
                integration(
                        "HR_MEMBER status response named serialized",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.HR_MEMBER, false);
                            var r =
                                    http(
                                            f.controller,
                                            "PATCH",
                                            "/api/v1/reports/10/status",
                                            "{\"status\":\"IN_REVIEW\"}",
                                            200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"reporter_display_name\":\"Actor\"")
                                    .doesNotContain("actor@example.test");
                        }),
                integration(
                        "SYSTEM_ADMIN own report anonymous serialized",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.SYSTEM_ADMIN, true);
                            var r = http(f.controller, "GET", "/api/v1/reports/mine", "", 200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"reporter_display_name\":null")
                                    .doesNotContain("Actor", "actor@example.test");
                        }),
                integration(
                        "SYSTEM_ADMIN status response anonymous serialized",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.SYSTEM_ADMIN, true);
                            var r =
                                    http(
                                            f.controller,
                                            "PATCH",
                                            "/api/v1/reports/10/status",
                                            "{\"status\":\"IN_REVIEW\"}",
                                            200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"reporter_display_name\":null")
                                    .doesNotContain("Actor", "actor@example.test");
                        }),
                integration(
                        "SYSTEM_ADMIN own report named serialized",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.SYSTEM_ADMIN, false);
                            var r = http(f.controller, "GET", "/api/v1/reports/mine", "", 200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"reporter_display_name\":\"Actor\"")
                                    .doesNotContain("actor@example.test");
                        }),
                integration(
                        "SYSTEM_ADMIN status response named serialized",
                        () -> {
                            var f = new Fixture();
                            f.setup(Role.SYSTEM_ADMIN, false);
                            var r =
                                    http(
                                            f.controller,
                                            "PATCH",
                                            "/api/v1/reports/10/status",
                                            "{\"status\":\"IN_REVIEW\"}",
                                            200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"reporter_display_name\":\"Actor\"")
                                    .doesNotContain("actor@example.test");
                        }));
    }
}
