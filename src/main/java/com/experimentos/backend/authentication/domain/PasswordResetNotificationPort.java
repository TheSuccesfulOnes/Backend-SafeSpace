package com.experimentos.backend.authentication.domain;

import com.experimentos.backend.iam.domain.User;

/** Outbound port for delivering password recovery links. */
public interface PasswordResetNotificationPort {
    void send(User user, String resetLink);
}
