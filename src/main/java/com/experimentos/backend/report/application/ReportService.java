package com.experimentos.backend.report.application;

import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.report.domain.Report;
import com.experimentos.backend.report.infrastructure.ReportRepository;
import com.experimentos.backend.report.interfaces.ReportDtos;
import com.experimentos.backend.shared.security.CurrentUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private final ReportRepository reports;
    private final UserRepository users;

    public ReportService(ReportRepository reports, UserRepository users) {
        this.reports = reports;
        this.users = users;
    }

    @Transactional
    public ReportDtos.ReportResponse create(ReportDtos.CreateReportRequest request) {
        User user = currentUser();
        Report report =
                new Report(
                        request.anonymous() ? null : user,
                        request.category().trim(),
                        request.title().trim(),
                        request.description().trim(),
                        request.priority(),
                        request.anonymous());
        return toResponse(reports.save(report));
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.ReportResponse> mine() {
        return reports.findByUserIdOrderByIdDesc(currentUser().getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.ReportResponse> all() {
        return reports.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ReportDtos.ReportResponse updateStatus(Long id, ReportDtos.UpdateStatusRequest request) {
        Report report =
                reports.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Report was not found"));
        report.updateStatus(request.status());
        reports.save(report);
        return toResponse(report);
    }

    private ReportDtos.ReportResponse toResponse(Report report) {
        return new ReportDtos.ReportResponse(
                report.getId(),
                report.getCategory(),
                report.getTitle(),
                report.getDescription(),
                report.getPriority(),
                report.getStatus(),
                report.isAnonymous(),
                report.isAnonymous() ? null : report.getReporterDisplayName(),
                report.getCreatedAt());
    }

    private User currentUser() {
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        CurrentUser.username(), CurrentUser.username())
                .orElseThrow(
                        () -> new IllegalArgumentException("Authenticated user was not found"));
    }
}
