package com.experimentos.backend.survey.domain;

import com.experimentos.backend.iam.domain.User;
import java.time.Instant;

public class SurveyAnswer {
    private Long id;

    private Survey survey;

    private User user;

    private String answerText;

    private Instant createdAt;

    protected SurveyAnswer() {}

    public SurveyAnswer(Survey survey, User user, String answerText) {
        this.survey = survey;
        this.user = user;
        this.answerText = answerText;
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
