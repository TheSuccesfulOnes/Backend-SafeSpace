package com.experimentos.backend.activity.domain;

import com.experimentos.backend.iam.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "weekly_activities")
public class WeeklyActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityStatus status = ActivityStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityOption> options = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WeeklyActivity() {}

    public WeeklyActivity(String title, String description, User createdBy) {
        this.title = title;
        this.description = description;
        this.createdBy = createdBy;
    }

    public void addOption(String label) {
        options.add(new ActivityOption(this, label.trim()));
    }

    public void close() {
        status = ActivityStatus.CLOSED;
    }

    public void reopen() {
        status = ActivityStatus.OPEN;
    }

    public void update(String title, String description, List<String> optionLabels) {
        this.title = title;
        this.description = description;
        List<String> currentLabels = options.stream().map(ActivityOption::getLabel).toList();
        if (!currentLabels.equals(optionLabels)) {
            options.clear();
            optionLabels.forEach(this::addOption);
        }
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public ActivityStatus getStatus() {
        return status;
    }

    public List<ActivityOption> getOptions() {
        return options;
    }
}
