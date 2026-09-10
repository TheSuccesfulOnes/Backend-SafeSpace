package com.experimentos.backend.survey.domain;

import com.experimentos.backend.iam.domain.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "survey_answers",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_answer_survey_user",
                        columnNames = {"survey_id", "user_id"}))
public class SurveyAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "answer_text", nullable = false, length = 1000)
    private String answerText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SurveyAnswer() {}

    public SurveyAnswer(Survey survey, User user, String answerText) {
        this.survey = survey;
        this.user = user;
        this.answerText = answerText;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public String getAnswerText() {
        return answerText;
    }

    public Long getId() {
        return id;
    }

    public Survey getSurvey() {
        return survey;
    }

    public User getUser() {
        return user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
