package com.experimentos.backend.ai.domain;

import com.experimentos.backend.iam.domain.User;
import java.time.Instant;

public class AiConversation {
    private Long id;

    private User user;

    private String title = "Nueva conversación";

    private Instant createdAt;

    protected AiConversation() {}

    public AiConversation(User user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTitle() {
        return title;
    }

    public void rename(String title) {
        this.title = title;
    }
}
