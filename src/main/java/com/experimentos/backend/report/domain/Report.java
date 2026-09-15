package com.experimentos.backend.report.domain;

import com.experimentos.backend.iam.domain.User;
import java.time.Instant;

public class Report {
    private Long id;

    private User user;

    private String category;

    private String title;

    private String description;

    private ReportPriority priority;

    private ReportStatus status = ReportStatus.NEW;

    private boolean anonymous;

    private Instant createdAt;

    protected Report() {}

    public Report(
            User user,
            String category,
            String title,
            String description,
            ReportPriority priority,
            boolean anonymous) {
        this.user = user;
        this.category = category;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.anonymous = anonymous;
    }

    public Long getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ReportPriority getPriority() {
        return priority;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public String getReporterDisplayName() {
        return anonymous || user == null ? null : user.getDisplayName();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void updateStatus(ReportStatus status) {
        this.status = status;
    }
}
