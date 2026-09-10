package com.experimentos.backend.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.experimentos.backend.activity.domain.WeeklyActivity;
import com.experimentos.backend.activity.infrastructure.ActivityVoteRepository;
import com.experimentos.backend.activity.infrastructure.WeeklyActivityRepository;
import com.experimentos.backend.activity.interfaces.ActivityAdminDtos;
import com.experimentos.backend.audit.application.AuditService;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.Role;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for administrative weekly activity management. */
@ExtendWith(MockitoExtension.class)
class AdminActivityServiceTest {
    @Mock WeeklyActivityRepository activities;
    @Mock ActivityVoteRepository votes;
    @Mock UserRepository users;
    @Mock AuditService auditService;

    private User admin;
    private AdminActivityService service;

    @BeforeEach
    void setUp() {
        admin = new User("admin", "admin@example.com", "hash", "Admin", Role.SYSTEM_ADMIN);
        ReflectionTestUtils.setField(admin, "id", 1L);
        service = new AdminActivityService(activities, votes, users, auditService);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                "admin",
                                null,
                                java.util.List.of(
                                        new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(admin));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminCanCreateAnActivityWithOptions() {
        WeeklyActivity created = new WeeklyActivity("Weekend", "Choose one", admin);
        ReflectionTestUtils.setField(created, "id", 20L);
        created.addOption("Karaoke");
        when(activities.save(any(WeeklyActivity.class))).thenReturn(created);
        when(votes.countByActivityId(20L)).thenReturn(0L);

        ActivityAdminDtos.ActivityResponse response =
                service.create(
                        new ActivityAdminDtos.ActivityRequest(
                                "Weekend", "Choose one", List.of("Karaoke")));

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.options()).singleElement().extracting("label").isEqualTo("Karaoke");
    }

    @Test
    void adminCanReopenAClosedActivity() {
        WeeklyActivity activity = new WeeklyActivity("Weekend", "Choose one", admin);
        ReflectionTestUtils.setField(activity, "id", 20L);
        activity.close();
        when(activities.findById(20L)).thenReturn(Optional.of(activity));
        when(votes.countByActivityId(20L)).thenReturn(0L);

        ActivityAdminDtos.ActivityResponse response = service.open(20L);

        assertThat(response.status()).isEqualTo("OPEN");
    }

    @Test
    void adminCannotChangeOptionsAfterVotingHasStarted() {
        WeeklyActivity activity = new WeeklyActivity("Weekend", "Choose one", admin);
        ReflectionTestUtils.setField(activity, "id", 20L);
        activity.addOption("Karaoke");
        when(activities.findById(20L)).thenReturn(Optional.of(activity));
        when(votes.countByActivityId(20L)).thenReturn(1L);

        assertThatThrownBy(
                        () ->
                                service.update(
                                        20L,
                                        new ActivityAdminDtos.ActivityRequest(
                                                "Weekend", "Choose one", List.of("Cinema"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Activity options cannot be changed after voting has started");
    }
}
