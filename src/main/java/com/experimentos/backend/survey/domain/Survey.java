package com.experimentos.backend.survey.domain;

import com.experimentos.backend.iam.domain.User;
import java.time.Instant;

public class Survey {
    private Long id;

    private String title;

    private String question;

    private SurveyStatus status = SurveyStatus.DRAFT;

    private SurveyType type = SurveyType.DAILY;

    private boolean allowComments = true;

    private User createdBy;

    private Instant createdAt;

    protected Survey() {}

    public Survey(
            String title, String question, SurveyType type, boolean allowComments, User createdBy) {
        this.title = title;
        this.question = question;
        this.type = type;
        this.allowComments = allowComments;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getQuestion() {
        return question;
    }

    public SurveyStatus getStatus() {
        return status;
    }

    public SurveyType getType() {
        return type;
    }

    public boolean isAllowComments() {
        return allowComments;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void update(String title, String question, SurveyType type, boolean allowComments) {
        this.title = title;
        this.question = question;
        this.type = type;
        this.allowComments = allowComments;
    }

    public void publish() {
        status = SurveyStatus.PUBLISHED;
    }

    public void close() {
        status = SurveyStatus.CLOSED;
    }

    public void reopen() {
        status = SurveyStatus.DRAFT;
    }
}
