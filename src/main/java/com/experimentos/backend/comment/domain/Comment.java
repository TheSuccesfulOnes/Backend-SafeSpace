package com.experimentos.backend.comment.domain;

import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.survey.domain.Survey;
import java.time.Instant;

public class Comment {
    private Long id;

    private Survey survey;

    private User user;

    private Comment parent;

    private String content;

    private Instant createdAt;

    protected Comment() {}

    public Comment(Survey survey, User user, Comment parent, String content) {
        this.survey = survey;
        this.user = user;
        this.parent = parent;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public Survey getSurvey() {
        return survey;
    }

    public Comment getParent() {
        return parent;
    }

    public boolean isOwnedBy(Long userId) {
        return user != null && user.getId().equals(userId);
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
