package com.experimentos.backend.authentication.infrastructure;

import com.experimentos.backend.authentication.domain.PasswordResetNotificationPort;
import com.experimentos.backend.iam.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Local-only delivery adapter; replace it with an email provider in deployed environments. */
@Component
@Profile("local")
public class LoggingPasswordResetNotificationAdapter implements PasswordResetNotificationPort {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(LoggingPasswordResetNotificationAdapter.class);

    @Override
    public void send(User user, String resetLink) {
        LOGGER.info(
                "Local password recovery link generated for employee {}. The link is intentionally not logged.",
                user.getUsername());
    }
}
