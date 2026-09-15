package com.experimentos.backend.authentication.domain;

import com.experimentos.backend.iam.domain.User;
import java.time.Instant;

/** Single-use token used to establish a new employee password. */
public class PasswordResetToken {
    private Long id;

    private User user;

    private String tokenHash;

    private Instant expiresAt;

    private Instant usedAt;

    private Instant createdAt;

    protected PasswordResetToken() {}

    public PasswordResetToken(User user, String tokenHash, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public User getUser() {
        return user;
    }

    public boolean isUsableAt(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    public void markUsed(Instant now) {
        usedAt = now;
    }
}
