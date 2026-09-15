package com.experimentos.backend.activity.domain;

import com.experimentos.backend.iam.domain.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class WeeklyActivity {
    private Long id;

    private String title;

    private String description;

    private ActivityStatus status = ActivityStatus.OPEN;

    private User createdBy;

    private List<ActivityOption> options = new ArrayList<>();

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
