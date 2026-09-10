package com.experimentos.backend.report.interfaces;

import com.experimentos.backend.report.application.ReportService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @PostMapping
    public ReportDtos.ReportResponse create(
            @Valid @RequestBody ReportDtos.CreateReportRequest request) {
        return service.create(request);
    }

    @GetMapping("/mine")
    public List<ReportDtos.ReportResponse> mine() {
        return service.mine();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR_MEMBER', 'SYSTEM_ADMIN')")
    public List<ReportDtos.ReportResponse> all() {
        return service.all();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR_MEMBER', 'SYSTEM_ADMIN')")
    public ReportDtos.ReportResponse updateStatus(
            @PathVariable Long id, @Valid @RequestBody ReportDtos.UpdateStatusRequest request) {
        return service.updateStatus(id, request);
    }
}
