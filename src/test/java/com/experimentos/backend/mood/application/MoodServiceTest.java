package com.experimentos.backend.mood.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.mood.domain.Mood;
import com.experimentos.backend.mood.domain.MoodEntry;
import com.experimentos.backend.mood.infrastructure.MoodEntryRepository;
import com.experimentos.backend.mood.interfaces.MoodDtos;
import com.experimentos.backend.shared.security.Role;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Unit tests for the daily mood business rules. */
@ExtendWith(MockitoExtension.class)
class MoodServiceTest {
    @Mock UserRepository users;
    @Mock MoodEntryRepository moods;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void employeeCanSubmitMoodOncePerDay() {
        authenticateAs("maria");
        User user = new User("maria", "maria@example.com", "hash", "Maria", Role.EMPLOYEE);
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("maria", "maria"))
                .thenReturn(Optional.of(user));
        when(moods.findByUserIdAndMoodDate(eq(user.getId()), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(moods.save(any(MoodEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MoodDtos.MoodResponse response =
                new MoodService(users, moods).submit(new MoodDtos.SubmitMoodRequest(Mood.GOOD));

        assertThat(response.mood()).isEqualTo(Mood.GOOD);
        verify(moods).save(any(MoodEntry.class));
    }

    @Test
    void duplicateDailyMoodIsRejected() {
        authenticateAs("maria");
        User user = new User("maria", "maria@example.com", "hash", "Maria", Role.EMPLOYEE);
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("maria", "maria"))
                .thenReturn(Optional.of(user));
        when(moods.findByUserIdAndMoodDate(eq(user.getId()), any(LocalDate.class)))
                .thenReturn(Optional.of(new MoodEntry(user, Mood.GOOD, LocalDate.now())));

        assertThatThrownBy(
                        () ->
                                new MoodService(users, moods)
                                        .submit(new MoodDtos.SubmitMoodRequest(Mood.VERY_GOOD)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mood has already been submitted for today");
        verify(moods, never()).save(any());
    }

    @Test
    void summaryCalculatesAnonymousParticipationMetrics() {
        LocalDate date = LocalDate.of(2026, 9, 1);
        User employeeOne = new User("maria", "maria@example.com", "hash", "Maria", Role.EMPLOYEE);
        User employeeTwo = new User("juan", "juan@example.com", "hash", "Juan", Role.EMPLOYEE);
        when(moods.findByMoodDate(date))
                .thenReturn(
                        List.of(
                                new MoodEntry(employeeOne, Mood.VERY_GOOD, date),
                                new MoodEntry(employeeTwo, Mood.GOOD, date)));
        when(users.countByRoleAndEnabledTrue(Role.EMPLOYEE)).thenReturn(4L);

        MoodDtos.MoodSummary summary = new MoodService(users, moods).summary(date);

        assertThat(summary.totalResponses()).isEqualTo(2);
        assertThat(summary.activeEmployees()).isEqualTo(4);
        assertThat(summary.responseRate()).isEqualTo(50);
        assertThat(summary.distribution()).containsEntry(Mood.VERY_GOOD, 1L);
        verify(users).countByRoleAndEnabledTrue(Role.EMPLOYEE);
    }

    @Test
    void summaryWithoutDateUsesBusinessTimezoneDate() {
        LocalDate businessDate = LocalDate.now(ZoneId.of("America/Lima"));
        when(moods.findByMoodDate(businessDate)).thenReturn(List.of());
        when(users.countByRoleAndEnabledTrue(Role.EMPLOYEE)).thenReturn(0L);

        MoodDtos.MoodSummary summary = new MoodService(users, moods).summary(null);

        assertThat(summary.date()).isEqualTo(businessDate);
        verify(moods).findByMoodDate(businessDate);
    }

    private void authenticateAs(String username) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                java.util.List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))));
    }
}
