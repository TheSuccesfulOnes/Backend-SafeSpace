package com.experimentos.backend.authentication.infrastructure;

import com.experimentos.backend.authentication.domain.PasswordResetNotificationPort;
import com.experimentos.backend.iam.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Keeps non-local environments bootable until a real email adapter is configured. */
@Component
@Profile("!local")
public class UnconfiguredPasswordResetNotificationAdapter implements PasswordResetNotificationPort {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(UnconfiguredPasswordResetNotificationAdapter.class);

    @Override
    public void send(User user, String resetLink) {
        LOGGER.warn(
                "Password recovery delivery is not configured for non-local environments; no link was sent for employee {}.",
                user.getUsername());
    }
}
