package com.experimentos.backend.authentication.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Limits recovery requests per normalized identifier on this application instance. */
@Component
public class PasswordResetRateLimiter {
    private final PasswordResetProperties properties;
    private final Clock clock;
    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    @Autowired
    public PasswordResetRateLimiter(PasswordResetProperties properties) {
        this(properties, Clock.systemUTC());
    }

    PasswordResetRateLimiter(PasswordResetProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public boolean allow(String identifier) {
        String key = identifier.trim().toLowerCase();
        Instant now = clock.instant();
        AttemptWindow updated =
                attempts.compute(
                        key,
                        (ignored, current) -> {
                            if (current == null
                                    || Duration.between(current.startedAt(), now).toMinutes()
                                            >= properties.windowDurationMinutes()) {
                                return new AttemptWindow(now, 1);
                            }
                            return new AttemptWindow(current.startedAt(), current.count() + 1);
                        });
        return updated.count() <= properties.requestLimit();
    }

    private record AttemptWindow(Instant startedAt, int count) {}
}
