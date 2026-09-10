package com.experimentos.backend.report.interfaces;

import com.experimentos.backend.report.domain.ReportPriority;
import com.experimentos.backend.report.domain.ReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class ReportDtos {
    private ReportDtos() {}

    public record CreateReportRequest(
            @NotBlank String category,
            @NotBlank String title,
            @NotBlank String description,
            @NotNull ReportPriority priority,
            boolean anonymous) {}

    public record UpdateStatusRequest(@NotNull ReportStatus status) {}

    public record ReportResponse(
            Long id,
            String category,
            String title,
            String description,
            ReportPriority priority,
            ReportStatus status,
            boolean anonymous,
            String reporterDisplayName,
            Instant createdAt) {}
}
