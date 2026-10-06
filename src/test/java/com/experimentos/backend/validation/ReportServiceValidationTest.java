package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.iam.domain.*;
import com.experimentos.backend.iam.infrastructure.*;
import com.experimentos.backend.report.application.*;
import com.experimentos.backend.report.domain.*;
import com.experimentos.backend.report.infrastructure.*;
import com.experimentos.backend.report.interfaces.*;
import com.experimentos.backend.shared.security.*;
import java.util.*;

class ReportServiceValidationTest extends ScenarioContract {
    static class Fixture {
        final ReportRepository reports = mock(ReportRepository.class);
        final UserRepository users = mock(UserRepository.class);
        final User actor = user(1, Role.EMPLOYEE);
        final Report anonymous =
                id(new Report(actor, "Work", "Issue", "Detail", ReportPriority.NORMAL, true), 11);
        final Report named =
                id(new Report(actor, "Work", "Issue", "Detail", ReportPriority.NORMAL, false), 10);
        final ReportService service = new ReportService(reports, users);
        final ReportController controller = new ReportController(service);

        Fixture() {
            authenticate(actor);
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(Optional.of(actor));
            when(reports.findById(10L)).thenReturn(Optional.of(named));
            when(reports.save(any())).thenAnswer(i -> id(i.getArgument(0), 10));
        }

        ReportDtos.CreateReportRequest request(boolean anon) {
            return new ReportDtos.CreateReportRequest(
                    " Work ", " Issue ", " Detail ", ReportPriority.NORMAL, anon);
        }

        String json(boolean anon) {
            return "{\"category\":\"Work\",\"title\":\"Issue\",\"description\":\"Detail\",\"priority\":\"NORMAL\",\"anonymous\":"
                    + anon
                    + "}";
        }
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                unit(
                        "anonymous creation masks reporter",
                        () -> {
                            var f = new Fixture();
                            assertThat(f.service.create(f.request(true)).reporterDisplayName())
                                    .isNull();
                        }),
                unit(
                        "named creation preserves reporter",
                        () -> {
                            var f = new Fixture();
                            assertThat(f.service.create(f.request(false)).reporterDisplayName())
                                    .isEqualTo("Actor");
                        }),
                unit(
                        "mine scopes repository to current user",
                        () -> {
                            var f = new Fixture();
                            f.service.mine();
                            verify(f.reports).findByUserIdOrderByIdDesc(1L);
                            verify(f.reports, never()).findAllByOrderByIdDesc();
                        }),
                unit(
                        "mine anonymous identity masked",
                        () -> {
                            var f = new Fixture();
                            when(f.reports.findByUserIdOrderByIdDesc(1L))
                                    .thenReturn(List.of(f.anonymous));
                            assertThat(f.service.mine().getFirst().reporterDisplayName()).isNull();
                        }),
                unit(
                        "missing user cannot create report",
                        () -> {
                            var f = new Fixture();
                            when(f.users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                            "actor", "actor"))
                                    .thenReturn(Optional.empty());
                            rejected(() -> f.service.create(f.request(true)), "Authenticated user");
                            verify(f.reports, never()).save(any());
                        }),
                unit(
                        "missing user cannot access mine",
                        () -> {
                            var f = new Fixture();
                            when(f.users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                            "actor", "actor"))
                                    .thenReturn(Optional.empty());
                            rejected(() -> f.service.mine(), "Authenticated user");
                            verifyNoInteractions(f.reports);
                        }),
                unit(
                        "missing report status update rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.service.updateStatus(
                                                    999L,
                                                    new ReportDtos.UpdateStatusRequest(
                                                            ReportStatus.CLOSED)),
                                    "not found");
                            verify(f.reports, never()).save(any());
                        }),
                unit(
                        "all list retains anonymity",
                        () -> {
                            var f = new Fixture();
                            when(f.reports.findAllByOrderByIdDesc())
                                    .thenReturn(List.of(f.anonymous, f.named));
                            assertThat(f.service.all())
                                    .extracting(ReportDtos.ReportResponse::reporterDisplayName)
                                    .containsExactly(null, "Actor");
                        }),
                unit(
                        "status NEW persists",
                        () -> {
                            var f = new Fixture();
                            assertThat(
                                            f.service
                                                    .updateStatus(
                                                            10L,
                                                            new ReportDtos.UpdateStatusRequest(
                                                                    ReportStatus.NEW))
                                                    .status())
                                    .isEqualTo(ReportStatus.NEW);
                            verify(f.reports).save(f.named);
                        }),
                unit(
                        "status IN_REVIEW persists",
                        () -> {
                            var f = new Fixture();
                            assertThat(
                                            f.service
                                                    .updateStatus(
                                                            10L,
                                                            new ReportDtos.UpdateStatusRequest(
                                                                    ReportStatus.IN_REVIEW))
                                                    .status())
                                    .isEqualTo(ReportStatus.IN_REVIEW);
                            verify(f.reports).save(f.named);
                        }),
                unit(
                        "status ADDRESSED persists",
                        () -> {
                            var f = new Fixture();
                            assertThat(
                                            f.service
                                                    .updateStatus(
                                                            10L,
                                                            new ReportDtos.UpdateStatusRequest(
                                                                    ReportStatus.ADDRESSED))
                                                    .status())
                                    .isEqualTo(ReportStatus.ADDRESSED);
                            verify(f.reports).save(f.named);
                        }),
                unit(
                        "status CLOSED persists",
                        () -> {
                            var f = new Fixture();
                            assertThat(
                                            f.service
                                                    .updateStatus(
                                                            10L,
                                                            new ReportDtos.UpdateStatusRequest(
                                                                    ReportStatus.CLOSED))
                                                    .status())
                                    .isEqualTo(ReportStatus.CLOSED);
                            verify(f.reports).save(f.named);
                        }),
                integration(
                        "anonymous report HTTP no identity disclosure",
                        () -> {
                            var f = new Fixture();
                            var r =
                                    http(
                                            f.controller,
                                            "POST",
                                            "/api/v1/reports",
                                            f.json(true),
                                            200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"reporter_display_name\":null")
                                    .doesNotContain("Actor", "actor@example.test");
                        }),
                integration(
                        "named report HTTP disclosure matches choice",
                        () -> {
                            var f = new Fixture();
                            var r =
                                    http(
                                            f.controller,
                                            "POST",
                                            "/api/v1/reports",
                                            f.json(false),
                                            200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("\"reporter_display_name\":\"Actor\"");
                        }),
                integration(
                        "mine HTTP scoped and anonymous",
                        () -> {
                            var f = new Fixture();
                            when(f.reports.findByUserIdOrderByIdDesc(1L))
                                    .thenReturn(List.of(f.anonymous));
                            var r = http(f.controller, "GET", "/api/v1/reports/mine", "", 200);
                            assertThat(r.getResponse().getContentAsString())
                                    .doesNotContain("Actor");
                            verify(f.reports).findByUserIdOrderByIdDesc(1L);
                        }),
                integration(
                        "unknown status HTTP blocked",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "PATCH",
                                    "/api/v1/reports/10/status",
                                    "{\"status\":\"INVALID\"}",
                                    400);
                            verify(f.reports, never()).save(any());
                        }),
                integration(
                        "NEW status HTTP lifecycle",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "PATCH",
                                    "/api/v1/reports/10/status",
                                    "{\"status\":\"NEW\"}",
                                    200);
                            assertThat(f.named.getStatus()).isEqualTo(ReportStatus.NEW);
                            verify(f.reports).save(f.named);
                        }),
                integration(
                        "IN_REVIEW status HTTP lifecycle",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "PATCH",
                                    "/api/v1/reports/10/status",
                                    "{\"status\":\"IN_REVIEW\"}",
                                    200);
                            assertThat(f.named.getStatus()).isEqualTo(ReportStatus.IN_REVIEW);
                            verify(f.reports).save(f.named);
                        }),
                integration(
                        "ADDRESSED status HTTP lifecycle",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "PATCH",
                                    "/api/v1/reports/10/status",
                                    "{\"status\":\"ADDRESSED\"}",
                                    200);
                            assertThat(f.named.getStatus()).isEqualTo(ReportStatus.ADDRESSED);
                            verify(f.reports).save(f.named);
                        }),
                integration(
                        "CLOSED status HTTP lifecycle",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "PATCH",
                                    "/api/v1/reports/10/status",
                                    "{\"status\":\"CLOSED\"}",
                                    200);
                            assertThat(f.named.getStatus()).isEqualTo(ReportStatus.CLOSED);
                            verify(f.reports).save(f.named);
                        }));
    }
}
