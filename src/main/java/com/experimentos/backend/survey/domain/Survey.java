package com.experimentos.backend.survey.domain;

import com.experimentos.backend.iam.domain.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "surveys")
public class Survey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 500)
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SurveyStatus status = SurveyStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "survey_type", nullable = false, length = 20)
    private SurveyType type = SurveyType.DAILY;

    @Column(name = "allow_comments", nullable = false)
    private boolean allowComments = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
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
