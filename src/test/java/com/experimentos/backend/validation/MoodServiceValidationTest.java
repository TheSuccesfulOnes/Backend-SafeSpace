package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.iam.domain.*;
import com.experimentos.backend.iam.infrastructure.*;
import com.experimentos.backend.mood.application.*;
import com.experimentos.backend.mood.domain.*;
import com.experimentos.backend.mood.infrastructure.*;
import com.experimentos.backend.mood.interfaces.*;
import com.experimentos.backend.shared.security.*;
import java.time.*;
import java.util.*;

class MoodServiceValidationTest extends ScenarioContract {
    static class MutableClock extends Clock {
        Instant now = NOW;

        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        public Instant instant() {
            return now;
        }
    }

    static class Fixture {
        final MoodEntryRepository moods = mock(MoodEntryRepository.class);
        final UserRepository users = mock(UserRepository.class);
        final User actor = user(1, Role.EMPLOYEE);
        final MutableClock clock = new MutableClock();
        final MoodService service = new MoodService(users, moods, clock);
        final MoodController controller = new MoodController(service);

        Fixture() {
            authenticate(actor);
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(Optional.of(actor));
            when(moods.save(any())).thenAnswer(i -> id(i.getArgument(0), 10));
        }

        void duplicate() {
            when(moods.findByUserIdAndMoodDate(1L, DATE))
                    .thenReturn(Optional.of(new MoodEntry(actor, Mood.BAD, DATE)));
        }

        void submit() {
            service.submit(new MoodDtos.SubmitMoodRequest(Mood.GOOD));
        }

        void noSave() {
            verify(moods, never()).save(any());
        }

        void entries(int count) {
            when(moods.findByMoodDate(DATE))
                    .thenReturn(
                            java.util.stream.IntStream.range(0, count)
                                    .mapToObj(i -> new MoodEntry(actor, Mood.GOOD, DATE))
                                    .toList());
        }
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                unit(
                        "VERY_BAD persisted on fixed Lima date",
                        () -> {
                            var f = new Fixture();
                            assertThat(
                                            f.service
                                                    .submit(
                                                            new MoodDtos.SubmitMoodRequest(
                                                                    Mood.VERY_BAD))
                                                    .date())
                                    .isEqualTo(DATE);
                            verify(f.moods)
                                    .save(
                                            argThat(
                                                    e ->
                                                            e.getMood() == Mood.VERY_BAD
                                                                    && e.getMoodDate()
                                                                            .equals(DATE)));
                        }),
                unit(
                        "BAD persisted on fixed Lima date",
                        () -> {
                            var f = new Fixture();
                            assertThat(
                                            f.service
                                                    .submit(
                                                            new MoodDtos.SubmitMoodRequest(
                                                                    Mood.BAD))
                                                    .date())
                                    .isEqualTo(DATE);
                            verify(f.moods)
                                    .save(
                                            argThat(
                                                    e ->
                                                            e.getMood() == Mood.BAD
                                                                    && e.getMoodDate()
                                                                            .equals(DATE)));
                        }),
                unit(
                        "GOOD persisted on fixed Lima date",
                        () -> {
                            var f = new Fixture();
                            assertThat(
                                            f.service
                                                    .submit(
                                                            new MoodDtos.SubmitMoodRequest(
                                                                    Mood.GOOD))
                                                    .date())
                                    .isEqualTo(DATE);
                            verify(f.moods)
                                    .save(
                                            argThat(
                                                    e ->
                                                            e.getMood() == Mood.GOOD
                                                                    && e.getMoodDate()
                                                                            .equals(DATE)));
                        }),
                unit(
                        "VERY_GOOD persisted on fixed Lima date",
                        () -> {
                            var f = new Fixture();
                            assertThat(
                                            f.service
                                                    .submit(
                                                            new MoodDtos.SubmitMoodRequest(
                                                                    Mood.VERY_GOOD))
                                                    .date())
                                    .isEqualTo(DATE);
                            verify(f.moods)
                                    .save(
                                            argThat(
                                                    e ->
                                                            e.getMood() == Mood.VERY_GOOD
                                                                    && e.getMoodDate()
                                                                            .equals(DATE)));
                        }),
                unit(
                        "HR submission forbidden",
                        () -> {
                            var f = new Fixture();
                            f.actor.changeRole(Role.HR_MEMBER);
                            assertThatThrownBy(() -> f.submit())
                                    .isInstanceOf(
                                            org.springframework.security.access
                                                    .AccessDeniedException.class);
                            f.noSave();
                        }),
                unit(
                        "admin submission forbidden",
                        () -> {
                            var f = new Fixture();
                            f.actor.changeRole(Role.SYSTEM_ADMIN);
                            assertThatThrownBy(() -> f.submit())
                                    .isInstanceOf(
                                            org.springframework.security.access
                                                    .AccessDeniedException.class);
                            f.noSave();
                        }),
                unit(
                        "duplicate same day no write",
                        () -> {
                            var f = new Fixture();
                            f.duplicate();
                            rejected(f::submit, "already been submitted");
                            f.noSave();
                        }),
                unit(
                        "missing authenticated employee no write",
                        () -> {
                            var f = new Fixture();
                            when(f.users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                            "actor", "actor"))
                                    .thenReturn(Optional.empty());
                            rejected(f::submit, "Authenticated user");
                            f.noSave();
                        }),
                unit(
                        "zero employees zero rate",
                        () -> {
                            var f = new Fixture();
                            f.entries(1);
                            assertThat(f.service.summary(DATE).responseRate()).isZero();
                        }),
                unit(
                        "response rate rounded",
                        () -> {
                            var f = new Fixture();
                            f.entries(2);
                            when(f.users.countByRoleAndEnabledTrue(Role.EMPLOYEE)).thenReturn(3L);
                            assertThat(f.service.summary(DATE).responseRate()).isEqualTo(67);
                        }),
                unit(
                        "response rate capped at hundred",
                        () -> {
                            var f = new Fixture();
                            f.entries(2);
                            when(f.users.countByRoleAndEnabledTrue(Role.EMPLOYEE)).thenReturn(1L);
                            assertThat(f.service.summary(DATE).responseRate()).isEqualTo(100);
                        }),
                unit(
                        "no respondents retains zero categories",
                        () -> {
                            var f = new Fixture();
                            var r = f.service.summary(DATE);
                            assertThat(r.distribution())
                                    .hasSize(4)
                                    .allSatisfy((k, v) -> assertThat(v).isZero());
                            assertThat(r.totalResponses()).isZero();
                        }),
                integration(
                        "submit HTTP persists today",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/mood/today",
                                    "{\"mood\":\"GOOD\"}",
                                    200);
                            verify(f.moods).save(argThat(e -> e.getMoodDate().equals(DATE)));
                        }),
                integration(
                        "duplicate HTTP blocked",
                        () -> {
                            var f = new Fixture();
                            f.duplicate();
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/mood/today",
                                    "{\"mood\":\"GOOD\"}",
                                    400);
                            f.noSave();
                        }),
                integration(
                        "HR HTTP forbidden",
                        () -> {
                            var f = new Fixture();
                            f.actor.changeRole(Role.HR_MEMBER);
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/mood/today",
                                    "{\"mood\":\"GOOD\"}",
                                    403);
                            f.noSave();
                        }),
                integration(
                        "admin HTTP forbidden",
                        () -> {
                            var f = new Fixture();
                            f.actor.changeRole(Role.SYSTEM_ADMIN);
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/mood/today",
                                    "{\"mood\":\"GOOD\"}",
                                    403);
                            f.noSave();
                        }),
                integration(
                        "today HTTP reveals only own date lookup",
                        () -> {
                            var f = new Fixture();
                            when(f.moods.findByUserIdAndMoodDate(1L, DATE))
                                    .thenReturn(
                                            Optional.of(new MoodEntry(f.actor, Mood.BAD, DATE)));
                            var r = http(f.controller, "GET", "/api/v1/mood/today", "", 200);
                            assertThat(r.getResponse().getContentAsString())
                                    .contains("BAD", "2026-01-31");
                            verify(f.moods).findByUserIdAndMoodDate(1L, DATE);
                        }),
                integration(
                        "default summary HTTP fixed business day",
                        () -> {
                            var f = new Fixture();
                            http(f.controller, "GET", "/api/v1/mood/summary", "", 200);
                            verify(f.moods).findByMoodDate(DATE);
                            verify(f.users).countByRoleAndEnabledTrue(Role.EMPLOYEE);
                        }),
                integration(
                        "explicit summary date HTTP honored",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller,
                                    "GET",
                                    "/api/v1/mood/summary?date=2025-12-31",
                                    "",
                                    200);
                            verify(f.moods).findByMoodDate(java.time.LocalDate.of(2025, 12, 31));
                        }),
                integration(
                        "Lima midnight rollover releases prior day duplicate",
                        () -> {
                            var f = new Fixture();
                            f.clock.now = NOW.minusSeconds(1);
                            when(f.moods.findByUserIdAndMoodDate(1L, DATE.minusDays(1)))
                                    .thenReturn(
                                            Optional.of(
                                                    new MoodEntry(
                                                            f.actor, Mood.BAD, DATE.minusDays(1))));
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/mood/today",
                                    "{\"mood\":\"GOOD\"}",
                                    400);
                            f.clock.now = NOW;
                            http(
                                    f.controller,
                                    "POST",
                                    "/api/v1/mood/today",
                                    "{\"mood\":\"GOOD\"}",
                                    200);
                            verify(f.moods).save(argThat(e -> e.getMoodDate().equals(DATE)));
                        }));
    }
}
