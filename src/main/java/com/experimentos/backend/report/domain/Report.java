package com.experimentos.backend.report.domain;

import com.experimentos.backend.iam.domain.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reports")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.NEW;

    @Column(nullable = false)
    private boolean anonymous;

    @Column(name = "created_at", nullable = false, updatable = false)
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

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
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
        return user == null ? null : user.getDisplayName();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void updateStatus(ReportStatus status) {
        this.status = status;
    }
}
