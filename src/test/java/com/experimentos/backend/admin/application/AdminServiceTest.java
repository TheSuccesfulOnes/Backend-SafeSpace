package com.experimentos.backend.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.experimentos.backend.activity.infrastructure.WeeklyActivityRepository;
import com.experimentos.backend.admin.interfaces.AdminDtos;
import com.experimentos.backend.audit.application.AuditService;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.Role;
import com.experimentos.backend.survey.infrastructure.SurveyRepository;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for privileged account-management rules. */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    @Mock UserRepository users;
    @Mock SurveyRepository surveys;
    @Mock WeeklyActivityRepository activities;
    @Mock AuditService auditService;

    private BCryptPasswordEncoder passwordEncoder;
    private AdminService service;
    private User admin;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new AdminService(users, surveys, activities, passwordEncoder, auditService);
        admin = user(1L, "admin", Role.SYSTEM_ADMIN);
        admin.markAsSystemOwner();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                "admin",
                                null,
                                java.util.List.of(
                                        new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
        lenient()
                .when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(admin));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminCanCreateAnEmployeeWithAnEncodedPassword() {
        User created = user(2L, "maria", Role.EMPLOYEE);
        when(users.existsByUsernameIgnoreCase("maria")).thenReturn(false);
        when(users.existsByEmailIgnoreCase("maria@example.com")).thenReturn(false);
        when(users.save(any(User.class))).thenReturn(created);

        AdminDtos.UserSummary response =
                service.createUser(
                        new AdminDtos.CreateUserRequest(
                                "maria",
                                "maria@example.com",
                                "password123",
                                "Maria",
                                Role.EMPLOYEE));

        assertThat(response.role()).isEqualTo(Role.EMPLOYEE);
        verify(users)
                .save(
                        org.mockito.ArgumentMatchers.argThat(
                                user ->
                                        passwordEncoder.matches(
                                                "password123", user.getPasswordHash())));
    }

    @Test
    void adminCanCreateAnHrMemberWithoutAnEmail() {
        when(users.existsByUsernameIgnoreCase("hr-user")).thenReturn(false);
        when(users.save(any(User.class)))
                .thenAnswer(
                        invocation -> {
                            User created = invocation.getArgument(0);
                            ReflectionTestUtils.setField(created, "id", 2L);
                            return created;
                        });

        AdminDtos.UserSummary response =
                service.createHr(
                        new AdminDtos.CreateHrRequest("hr-user", null, "password123", "HR User"));

        assertThat(response.role()).isEqualTo(Role.HR_MEMBER);
        assertThat(response.email()).isNull();
        verify(users)
                .save(
                        org.mockito.ArgumentMatchers.argThat(
                                user ->
                                        user.getRole() == Role.HR_MEMBER
                                                && user.getEmail() == null
                                                && passwordEncoder.matches(
                                                        "password123", user.getPasswordHash())));
    }

    @Test
    void adminCannotCreateUserWithAnExistingUsername() {
        when(users.existsByUsernameIgnoreCase("maria")).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                service.createUser(
                                        new AdminDtos.CreateUserRequest(
                                                "maria",
                                                "maria@example.com",
                                                "password123",
                                                "Maria",
                                                Role.EMPLOYEE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username is already in use");
        verify(users, never()).save(any(User.class));
    }

    @Test
    void adminCannotCreateUserWithAnExistingEmail() {
        when(users.existsByUsernameIgnoreCase("maria")).thenReturn(false);
        when(users.existsByEmailIgnoreCase("maria@example.com")).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                service.createUser(
                                        new AdminDtos.CreateUserRequest(
                                                "maria",
                                                "maria@example.com",
                                                "password123",
                                                "Maria",
                                                Role.EMPLOYEE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is already in use");
        verify(users, never()).save(any(User.class));
    }

    @Test
    void adminCanUpdateUserAndResetPassword() {
        User target = user(2L, "old-name", Role.HR_MEMBER);
        when(users.findById(2L)).thenReturn(Optional.of(target));
        when(users.existsByUsernameIgnoreCaseAndIdNot("new-name", 2L)).thenReturn(false);
        when(users.existsByEmailIgnoreCaseAndIdNot("new@example.com", 2L)).thenReturn(false);

        AdminDtos.UserSummary response =
                service.updateUser(
                        2L,
                        new AdminDtos.UpdateUserRequest(
                                "new-name", "new@example.com", "New name", null));
        service.resetPassword(2L, new AdminDtos.ResetPasswordRequest("new-password"));

        assertThat(response.username()).isEqualTo("new-name");
        assertThat(target.getEmail()).isEqualTo("new@example.com");
        assertThat(target.getDisplayName()).isEqualTo("New name");
        assertThat(passwordEncoder.matches("new-password", target.getPasswordHash())).isTrue();
    }

    @Test
    void adminCanDisableAndReenableAUser() {
        User target = user(2L, "worker", Role.EMPLOYEE);
        when(users.findById(2L)).thenReturn(Optional.of(target));

        service.disableUser(2L);
        assertThat(target.isEnabled()).isFalse();

        service.enableUser(2L);
        assertThat(target.isEnabled()).isTrue();
    }

    @Test
    void adminCannotDeleteTheOnlySystemAdministrator() {
        User target = user(2L, "other-admin", Role.SYSTEM_ADMIN);
        when(users.findById(2L)).thenReturn(Optional.of(target));
        when(users.countByRole(Role.SYSTEM_ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteUser(2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one system administrator must remain");
    }

    @Test
    void adminCanDeleteAUserWithoutOwnedContent() {
        User target = user(2L, "worker", Role.EMPLOYEE);
        when(users.findById(2L)).thenReturn(Optional.of(target));
        when(surveys.existsByCreatedById(2L)).thenReturn(false);
        when(activities.existsByCreatedById(2L)).thenReturn(false);

        service.deleteUser(2L);

        verify(users).delete(target);
        verify(users).flush();
        verify(auditService).record(admin, "DELETE_USER", "USER", "2");
    }

    @Test
    void secondaryAdminCannotManageTheSystemOwnerAccount() {
        User secondary = user(2L, "secondary-admin", Role.SYSTEM_ADMIN);
        User owner = user(1L, "admin", Role.SYSTEM_ADMIN);
        owner.markAsSystemOwner();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                "secondary-admin",
                                null,
                                java.util.List.of(
                                        new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("secondary-admin", "secondary-admin"))
                .thenReturn(Optional.of(secondary));
        when(users.findById(1L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(
                        () ->
                                service.updateUser(
                                        1L,
                                        new AdminDtos.UpdateUserRequest(
                                                "renamed", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The system owner account can only be managed by itself");
        assertThatThrownBy(
                        () ->
                                service.resetPassword(
                                        1L, new AdminDtos.ResetPasswordRequest("new-password")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The system owner account can only be managed by itself");
        assertThatThrownBy(() -> service.enableUser(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The system owner account can only be managed by itself");
        assertThatThrownBy(() -> service.disableUser(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The system owner account can only be managed by itself");
        assertThatThrownBy(() -> service.deleteUser(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The system owner account can only be managed by itself");

        verify(auditService, never()).record(any(), any(), any(), any());
        verify(users, never()).delete(any(User.class));
    }

    private User user(Long id, String username, Role role) {
        User user = new User(username, username + "@example.com", "hash", username, role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
