package com.experimentos.backend.authentication.domain;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.authentication.infrastructure.PasswordResetTokenRepository;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.shared.security.Role;
import com.google.cloud.firestore.Firestore;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

class PasswordResetTokenValidationTest {
    static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    static class InMemoryReads extends PasswordResetTokenRepository {
        final List<PasswordResetToken> items = new ArrayList<>();
        final List<PasswordResetToken> deleted = new ArrayList<>();

        InMemoryReads() {
            super(mock(Firestore.class));
        }

        @Override
        protected List<PasswordResetToken> readAll() {
            return List.copyOf(items);
        }

        @Override
        public void delete(PasswordResetToken token) {
            deleted.add(token);
        }
    }

    static class Fixture {
        final User actor =
                new User("actor", "actor@example.test", "synthetic", "Actor", Role.EMPLOYEE);
        final User other =
                new User("other", "other@example.test", "synthetic", "Other", Role.EMPLOYEE);
        PasswordResetToken token = new PasswordResetToken(actor, "hash-a", NOW.plusSeconds(60));
        final InMemoryReads repository = new InMemoryReads();

        Fixture() {
            ReflectionTestUtils.setField(actor, "id", 1L);
            ReflectionTestUtils.setField(other, "id", 2L);
        }
    }

    record Case(String name, Runnable action) {}

    @TestFactory
    Stream<DynamicTest> unit() {
        return Stream.of(
                        new Case(
                                "future token usable",
                                () -> {
                                    var f = new Fixture();
                                    assertThat(f.token.isUsableAt(NOW)).isTrue();
                                }),
                        new Case(
                                "exact expiration token unusable",
                                () -> {
                                    var f = new Fixture();
                                    assertThat(f.token.isUsableAt(NOW.plusSeconds(60))).isFalse();
                                }),
                        new Case(
                                "after expiration token unusable",
                                () -> {
                                    var f = new Fixture();
                                    assertThat(f.token.isUsableAt(NOW.plusSeconds(61))).isFalse();
                                }),
                        new Case(
                                "one nanosecond before expiration usable",
                                () -> {
                                    var f = new Fixture();
                                    assertThat(
                                                    f.token.isUsableAt(
                                                            NOW.plusSeconds(60).minusNanos(1)))
                                            .isTrue();
                                }),
                        new Case(
                                "mark used prevents reuse",
                                () -> {
                                    var f = new Fixture();
                                    f.token.markUsed(NOW);
                                    assertThat(f.token.isUsableAt(NOW)).isFalse();
                                }),
                        new Case(
                                "future used timestamp still consumes token",
                                () -> {
                                    var f = new Fixture();
                                    f.token.markUsed(NOW.plusSeconds(10));
                                    assertThat(f.token.isUsableAt(NOW)).isFalse();
                                }),
                        new Case(
                                "use preserved when clock moves backward",
                                () -> {
                                    var f = new Fixture();
                                    f.token.markUsed(NOW);
                                    assertThat(f.token.isUsableAt(NOW.minusSeconds(1))).isFalse();
                                }),
                        new Case(
                                "exact stored hash matches",
                                () -> {
                                    var f = new Fixture();
                                    assertThat(f.token.matchesHash("hash-a")).isTrue();
                                }),
                        new Case(
                                "case altered hash rejected",
                                () -> {
                                    var f = new Fixture();
                                    assertThat(f.token.matchesHash("HASH-A")).isFalse();
                                }),
                        new Case(
                                "partial hash rejected",
                                () -> {
                                    var f = new Fixture();
                                    assertThat(f.token.matchesHash("hash")).isFalse();
                                }),
                        new Case(
                                "null candidate hash rejected",
                                () -> {
                                    var f = new Fixture();
                                    assertThat(f.token.matchesHash(null)).isFalse();
                                }),
                        new Case(
                                "null stored hash rejects null candidate",
                                () -> {
                                    var f = new Fixture();
                                    f.token =
                                            new PasswordResetToken(
                                                    f.actor, null, NOW.plusSeconds(60));
                                    assertThat(f.token.matchesHash(null)).isFalse();
                                }))
                .map(c -> DynamicTest.dynamicTest(c.name(), c.action()::run));
    }

    @TestFactory
    Stream<DynamicTest> integration() {
        return Stream.of(
                        new Case(
                                "repository selects exact token among alternatives",
                                () -> {
                                    var f = new Fixture();
                                    var other =
                                            new PasswordResetToken(
                                                    f.other, "hash-b", NOW.plusSeconds(60));
                                    f.repository.items.addAll(List.of(other, f.token));
                                    assertThat(f.repository.findByTokenHash("hash-a"))
                                            .contains(f.token);
                                }),
                        new Case(
                                "repository missing hash returns empty",
                                () -> {
                                    var f = new Fixture();
                                    f.repository.items.add(f.token);
                                    assertThat(f.repository.findByTokenHash("unknown")).isEmpty();
                                }),
                        new Case(
                                "repository cannot match shortened hash",
                                () -> {
                                    var f = new Fixture();
                                    f.repository.items.add(f.token);
                                    assertThat(f.repository.findByTokenHash("hash")).isEmpty();
                                }),
                        new Case(
                                "used record lookup remains unusable to service",
                                () -> {
                                    var f = new Fixture();
                                    f.token.markUsed(NOW);
                                    f.repository.items.add(f.token);
                                    assertThat(
                                                    f.repository
                                                            .findByTokenHash("hash-a")
                                                            .orElseThrow()
                                                            .isUsableAt(NOW))
                                            .isFalse();
                                }),
                        new Case(
                                "expired lookup remains unusable at exact boundary",
                                () -> {
                                    var f = new Fixture();
                                    f.repository.items.add(f.token);
                                    assertThat(
                                                    f.repository
                                                            .findByTokenHash("hash-a")
                                                            .orElseThrow()
                                                            .isUsableAt(NOW.plusSeconds(60)))
                                            .isFalse();
                                }),
                        new Case(
                                "null hash record cannot mask matching token",
                                () -> {
                                    var f = new Fixture();
                                    f.repository.items.add(
                                            new PasswordResetToken(
                                                    f.actor, null, NOW.plusSeconds(60)));
                                    f.repository.items.add(f.token);
                                    assertThat(f.repository.findByTokenHash("hash-a"))
                                            .contains(f.token);
                                }),
                        new Case(
                                "user invalidation only removes own tokens",
                                () -> {
                                    var f = new Fixture();
                                    var other =
                                            new PasswordResetToken(
                                                    f.other, "hash-b", NOW.plusSeconds(60));
                                    f.repository.items.addAll(List.of(f.token, other));
                                    f.repository.deleteByUserId(1L);
                                    assertThat(f.repository.deleted).containsExactly(f.token);
                                }),
                        new Case(
                                "orphan record ignored during account invalidation",
                                () -> {
                                    var f = new Fixture();
                                    var orphan =
                                            new PasswordResetToken(
                                                    null, "hash-c", NOW.plusSeconds(60));
                                    f.repository.items.addAll(List.of(orphan, f.token));
                                    f.repository.deleteByUserId(1L);
                                    assertThat(f.repository.deleted).containsExactly(f.token);
                                }))
                .map(c -> DynamicTest.dynamicTest(c.name(), c.action()::run));
    }
}
