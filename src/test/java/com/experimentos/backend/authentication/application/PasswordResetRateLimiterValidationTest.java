package com.experimentos.backend.authentication.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.authentication.domain.PasswordResetNotificationPort;
import com.experimentos.backend.authentication.infrastructure.PasswordResetTokenRepository;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordResetRateLimiterValidationTest {
    static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    static class MutableClock extends Clock {
        volatile Instant now = NOW;

        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        public Clock withZone(ZoneId zone) {
            return this;
        }

        public Instant instant() {
            return now;
        }
    }

    static class Fixture {
        final MutableClock clock = new MutableClock();
        final PasswordResetProperties properties =
                new PasswordResetProperties("https://local.invalid/?token=", 15, 3, 15);
        final PasswordResetRateLimiter limiter = new PasswordResetRateLimiter(properties, clock);
        final UserRepository users = mock(UserRepository.class);
        final PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        final PasswordResetNotificationPort notifications =
                mock(PasswordResetNotificationPort.class);
        final PasswordResetService service =
                new PasswordResetService(
                        users,
                        tokens,
                        mock(PasswordEncoder.class),
                        notifications,
                        properties,
                        limiter,
                        new java.security.SecureRandom(),
                        clock);

        void exhaust() {
            for (int n = 0; n < 3; n++) assertThat(limiter.allow("actor")).isTrue();
        }
    }

    @FunctionalInterface
    interface Checked {
        void run() throws Exception;
    }

    record Case(String label, Checked run) {}

    @TestFactory
    Stream<DynamicTest> unit() {
        return Stream.of(
                        new Case(
                                "first request allowed",
                                () -> {
                                    var f = new Fixture();
                                    assertThat(f.limiter.allow("actor")).isTrue();
                                }),
                        new Case(
                                "third at quota allowed",
                                () -> {
                                    var f = new Fixture();
                                    assertThat(f.limiter.allow("actor")).isTrue();
                                    assertThat(f.limiter.allow("actor")).isTrue();
                                    assertThat(f.limiter.allow("actor")).isTrue();
                                }),
                        new Case(
                                "fourth request denied",
                                () -> {
                                    var f = new Fixture();
                                    f.exhaust();
                                    assertThat(f.limiter.allow("actor")).isFalse();
                                }),
                        new Case(
                                "case variants share quota",
                                () -> {
                                    var f = new Fixture();
                                    f.limiter.allow("ACTOR");
                                    f.limiter.allow("Actor");
                                    f.limiter.allow("actor");
                                    assertThat(f.limiter.allow("ACTOR")).isFalse();
                                }),
                        new Case(
                                "whitespace shares quota",
                                () -> {
                                    var f = new Fixture();
                                    f.limiter.allow(" actor ");
                                    f.limiter.allow("actor");
                                    f.limiter.allow("\tactor");
                                    assertThat(f.limiter.allow("actor ")).isFalse();
                                }),
                        new Case(
                                "identifiers have independent quotas",
                                () -> {
                                    var f = new Fixture();
                                    f.exhaust();
                                    assertThat(f.limiter.allow("someone-else")).isTrue();
                                }),
                        new Case(
                                "just before window remains blocked",
                                () -> {
                                    var f = new Fixture();
                                    f.exhaust();
                                    f.clock.now = NOW.plusSeconds(899);
                                    assertThat(f.limiter.allow("actor")).isFalse();
                                }),
                        new Case(
                                "exact window boundary resets",
                                () -> {
                                    var f = new Fixture();
                                    f.exhaust();
                                    f.clock.now = NOW.plusSeconds(900);
                                    assertThat(f.limiter.allow("actor")).isTrue();
                                }),
                        new Case(
                                "after window resets",
                                () -> {
                                    var f = new Fixture();
                                    f.exhaust();
                                    f.clock.now = NOW.plusSeconds(901);
                                    assertThat(f.limiter.allow("actor")).isTrue();
                                }),
                        new Case(
                                "denials do not slide starting instant",
                                () -> {
                                    var f = new Fixture();
                                    f.exhaust();
                                    f.clock.now = NOW.plusSeconds(899);
                                    f.limiter.allow("actor");
                                    f.clock.now = NOW.plusSeconds(900);
                                    assertThat(f.limiter.allow("actor")).isTrue();
                                }),
                        new Case(
                                "reset window reestablishes full quota",
                                () -> {
                                    var f = new Fixture();
                                    f.exhaust();
                                    f.clock.now = NOW.plusSeconds(900);
                                    f.exhaust();
                                    assertThat(f.limiter.allow("actor")).isFalse();
                                }),
                        new Case(
                                "concurrent requests enforce exactly three",
                                () -> {
                                    var f = new Fixture();
                                    var pool = java.util.concurrent.Executors.newFixedThreadPool(8);
                                    try {
                                        var jobs =
                                                new java.util.ArrayList<
                                                        java.util.concurrent.Callable<Boolean>>();
                                        for (int n = 0; n < 40; n++)
                                            jobs.add(() -> f.limiter.allow("actor"));
                                        long accepted = 0;
                                        for (var result : pool.invokeAll(jobs))
                                            if (result.get()) accepted++;
                                        assertThat(accepted).isEqualTo(3);
                                    } finally {
                                        pool.shutdownNow();
                                        assertThat(
                                                        pool.awaitTermination(
                                                                5,
                                                                java.util.concurrent.TimeUnit
                                                                        .SECONDS))
                                                .isTrue();
                                    }
                                }))
                .map(c -> DynamicTest.dynamicTest(c.label(), c.run()::run));
    }

    @TestFactory
    Stream<DynamicTest> integration() {
        return Stream.of(
                        new Case(
                                "recovery first request queries only mocked users",
                                () -> {
                                    var f = new Fixture();
                                    f.service.requestRecovery("actor");
                                    verify(f.users)
                                            .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                                    "actor", "actor");
                                    verifyNoInteractions(f.tokens, f.notifications);
                                }),
                        new Case(
                                "recovery quota caps repository access",
                                () -> {
                                    var f = new Fixture();
                                    for (int n = 0; n < 4; n++) f.service.requestRecovery("actor");
                                    verify(f.users, times(3))
                                            .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                                    "actor", "actor");
                                }),
                        new Case(
                                "recovery mixed casing shares limiter",
                                () -> {
                                    var f = new Fixture();
                                    for (String value :
                                            java.util.List.of("Actor", " ACTOR ", "actor", "aCtOr"))
                                        f.service.requestRecovery(value);
                                    verify(f.users, times(3))
                                            .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                                    "actor", "actor");
                                }),
                        new Case(
                                "recovery exact reset resumes lookup",
                                () -> {
                                    var f = new Fixture();
                                    for (int n = 0; n < 4; n++) f.service.requestRecovery("actor");
                                    f.clock.now = NOW.plusSeconds(900);
                                    f.service.requestRecovery("actor");
                                    verify(f.users, times(4))
                                            .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                                    "actor", "actor");
                                }),
                        new Case(
                                "recovery separate identity retains lookup",
                                () -> {
                                    var f = new Fixture();
                                    for (int n = 0; n < 4; n++) f.service.requestRecovery("actor");
                                    f.service.requestRecovery("second");
                                    verify(f.users)
                                            .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                                    "second", "second");
                                }),
                        new Case(
                                "blank recovery does not consume quota",
                                () -> {
                                    var f = new Fixture();
                                    for (int n = 0; n < 5; n++) f.service.requestRecovery(" ");
                                    for (int n = 0; n < 3; n++) f.service.requestRecovery("actor");
                                    verify(f.users, times(3))
                                            .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                                    "actor", "actor");
                                }),
                        new Case(
                                "rate denial has same public response",
                                () -> {
                                    var f = new Fixture();
                                    var first = f.service.requestRecovery("actor");
                                    f.service.requestRecovery("actor");
                                    f.service.requestRecovery("actor");
                                    assertThat(f.service.requestRecovery("actor")).isEqualTo(first);
                                }),
                        new Case(
                                "recovery repeated window quotas stable",
                                () -> {
                                    var f = new Fixture();
                                    for (int n = 0; n < 4; n++) f.service.requestRecovery("actor");
                                    f.clock.now = NOW.plusSeconds(900);
                                    for (int n = 0; n < 4; n++) f.service.requestRecovery("actor");
                                    verify(f.users, times(6))
                                            .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                                    "actor", "actor");
                                }))
                .map(c -> DynamicTest.dynamicTest(c.label(), c.run()::run));
    }
}
