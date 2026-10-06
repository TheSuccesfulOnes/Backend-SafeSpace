package com.experimentos.backend.validation;

import static org.mockito.Mockito.mock;

import com.experimentos.backend.report.application.ReportService;
import com.experimentos.backend.report.interfaces.ReportController;
import com.experimentos.backend.report.interfaces.ReportDtos;
import java.util.List;

class ReportDtosValidationTest extends DtoContract {
    private final ReportService service = mock(ReportService.class);

    @Override
    protected Object controller() {
        return new ReportController(service);
    }

    @Override
    protected List<Object> services() {
        return List.of(service);
    }

    @Override
    protected List<Case> cases() {
        return List.of(
                new Case(
                        "category null",
                        ReportDtos.CreateReportRequest.class,
                        "POST",
                        "/api/v1/reports",
                        "{\"category\":null,\"title\":\"Issue\",\"description\":\"Detail\",\"priority\":\"LOW\",\"anonymous\":true}",
                        "category",
                        false),
                new Case(
                        "category blank",
                        ReportDtos.CreateReportRequest.class,
                        "POST",
                        "/api/v1/reports",
                        "{\"category\":\" \",\"title\":\"Issue\",\"description\":\"Detail\",\"priority\":\"LOW\",\"anonymous\":true}",
                        "category",
                        false),
                new Case(
                        "category maximum",
                        ReportDtos.CreateReportRequest.class,
                        "POST",
                        "/api/v1/reports",
                        "{\"category\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"title\":\"Issue\",\"description\":\"Detail\",\"priority\":\"LOW\",\"anonymous\":true}",
                        null,
                        false),
                new Case(
                        "category overflow",
                        ReportDtos.CreateReportRequest.class,
                        "POST",
                        "/api/v1/reports",
                        "{\"category\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"title\":\"Issue\",\"description\":\"Detail\",\"priority\":\"LOW\",\"anonymous\":true}",
                        "category",
                        false),
                new Case(
                        "title null",
                        ReportDtos.CreateReportRequest.class,
                        "POST",
                        "/api/v1/reports",
                        "{\"category\":\"Work\",\"title\":null,\"description\":\"Detail\",\"priority\":\"LOW\",\"anonymous\":true}",
                        "title",
                        false),
                new Case(
                        "title maximum",
                        ReportDtos.CreateReportRequest.class,
                        "POST",
                        "/api/v1/reports",
                        "{\"category\":\"Work\",\"title\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"description\":\"Detail\",\"priority\":\"LOW\",\"anonymous\":true}",
                        null,
                        false),
                new Case(
                        "title overflow",
                        ReportDtos.CreateReportRequest.class,
                        "POST",
                        "/api/v1/reports",
                        "{\"category\":\"Work\",\"title\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"description\":\"Detail\",\"priority\":\"LOW\",\"anonymous\":true}",
                        "title",
                        false),
                new Case(
                        "description blank",
                        ReportDtos.CreateReportRequest.class,
                        "POST",
                        "/api/v1/reports",
                        "{\"category\":\"Work\",\"title\":\"Issue\",\"description\":\" \",\"priority\":\"LOW\",\"anonymous\":true}",
                        "description",
                        false),
                new Case(
                        "description maximum",
                        ReportDtos.CreateReportRequest.class,
                        "POST",
                        "/api/v1/reports",
                        "{\"category\":\"Work\",\"title\":\"Issue\",\"description\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"priority\":\"LOW\",\"anonymous\":true}",
                        null,
                        false),
                new Case(
                        "description overflow",
                        ReportDtos.CreateReportRequest.class,
                        "POST",
                        "/api/v1/reports",
                        "{\"category\":\"Work\",\"title\":\"Issue\",\"description\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"priority\":\"LOW\",\"anonymous\":true}",
                        "description",
                        false),
                new Case(
                        "anonymous report valid",
                        ReportDtos.CreateReportRequest.class,
                        "POST",
                        "/api/v1/reports",
                        "{\"category\":\"Work\",\"title\":\"Issue\",\"description\":\"Detail\",\"priority\":\"LOW\",\"anonymous\":true}",
                        null,
                        true),
                new Case(
                        "named report valid",
                        ReportDtos.CreateReportRequest.class,
                        "POST",
                        "/api/v1/reports",
                        "{\"category\":\"Work\",\"title\":\"Issue\",\"description\":\"Detail\",\"priority\":\"LOW\",\"anonymous\":false}",
                        null,
                        true),
                new Case(
                        "priority null",
                        ReportDtos.CreateReportRequest.class,
                        "POST",
                        "/api/v1/reports",
                        "{\"category\":\"Work\",\"title\":\"Issue\",\"description\":\"Detail\",\"priority\":null,\"anonymous\":true}",
                        "priority",
                        true),
                new Case(
                        "unknown priority",
                        ReportDtos.CreateReportRequest.class,
                        "POST",
                        "/api/v1/reports",
                        "{\"category\":\"Work\",\"title\":\"Issue\",\"description\":\"Detail\",\"priority\":\"UNKNOWN\",\"anonymous\":true}",
                        "binding",
                        true),
                new Case(
                        "description object",
                        ReportDtos.CreateReportRequest.class,
                        "POST",
                        "/api/v1/reports",
                        "{\"category\":\"Work\",\"title\":\"Issue\",\"description\":{},\"priority\":\"LOW\",\"anonymous\":true}",
                        "binding",
                        true),
                new Case(
                        "status valid",
                        ReportDtos.UpdateStatusRequest.class,
                        "PATCH",
                        "/api/v1/reports/1/status",
                        "{\"status\":\"NEW\"}",
                        null,
                        true),
                new Case(
                        "status null",
                        ReportDtos.UpdateStatusRequest.class,
                        "PATCH",
                        "/api/v1/reports/1/status",
                        "{\"status\":null}",
                        "status",
                        true),
                new Case(
                        "unknown status",
                        ReportDtos.UpdateStatusRequest.class,
                        "PATCH",
                        "/api/v1/reports/1/status",
                        "{\"status\":\"HACKED\"}",
                        "binding",
                        true),
                new Case(
                        "status missing",
                        ReportDtos.UpdateStatusRequest.class,
                        "PATCH",
                        "/api/v1/reports/1/status",
                        "{}",
                        "status",
                        true),
                new Case(
                        "status array",
                        ReportDtos.UpdateStatusRequest.class,
                        "PATCH",
                        "/api/v1/reports/1/status",
                        "{\"status\":[]}",
                        "binding",
                        true));
    }
}
