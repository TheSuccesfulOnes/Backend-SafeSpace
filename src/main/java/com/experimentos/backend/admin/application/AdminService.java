package com.experimentos.backend.admin.application;

import com.experimentos.backend.activity.infrastructure.WeeklyActivityRepository;
import com.experimentos.backend.admin.interfaces.AdminDtos;
import com.experimentos.backend.audit.application.AuditService;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.CurrentUser;
import com.experimentos.backend.shared.security.Role;
import com.experimentos.backend.survey.infrastructure.SurveyRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for privileged system administrator operations. */
@Service
public class AdminService {
    private final UserRepository users;
    private final SurveyRepository surveys;
    private final WeeklyActivityRepository activities;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AdminService(
            UserRepository users,
            SurveyRepository surveys,
            WeeklyActivityRepository activities,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.users = users;
        this.surveys = surveys;
        this.activities = activities;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.UserSummary> listUsers() {
        return users.findAll().stream().map(AdminDtos.UserSummary::from).toList();
    }

    @Transactional
    public AdminDtos.UserSummary createUser(AdminDtos.CreateUserRequest request) {
        User actor = currentAdmin();
        User created =
                create(
                        request.username(),
                        request.email(),
                        request.password(),
                        request.displayName(),
                        request.role());
        auditService.record(actor, "CREATE_USER", "USER", created.getId().toString());
        return AdminDtos.UserSummary.from(created);
    }

    @Transactional
    public AdminDtos.UserSummary createHr(AdminDtos.CreateHrRequest request) {
        User actor = currentAdmin();
        User created =
                create(
                        request.username(),
                        request.email(),
                        request.password(),
                        request.displayName(),
                        Role.HR_MEMBER);
        auditService.record(actor, "CREATE_HR_MEMBER", "USER", created.getId().toString());
        return AdminDtos.UserSummary.from(created);
    }

    @Transactional
    public AdminDtos.UserSummary updateUser(Long id, AdminDtos.UpdateUserRequest request) {
        User actor = currentAdmin();
        User user = findUser(id);
        ensureCanManageAccount(actor, user);
        if (request.username() == null
                && request.email() == null
                && request.displayName() == null
                && request.role() == null) {
            throw new IllegalArgumentException("At least one user field must be provided");
        }
        if (actor.getId().equals(id)
                && request.role() != null
                && request.role() != user.getRole()) {
            throw new IllegalArgumentException("An administrator cannot change their own role");
        }
        if (request.username() != null) {
            String username = normalizedRequired(request.username(), "Username");
            if (users.existsByUsernameIgnoreCaseAndIdNot(username, id))
                throw new IllegalArgumentException("Username is already in use");
            user.updateProfile(username, user.getEmail(), user.getDisplayName());
        }
        if (request.email() != null) {
            String email = normalizedRequired(request.email(), "Email").toLowerCase();
            if (users.existsByEmailIgnoreCaseAndIdNot(email, id))
                throw new IllegalArgumentException("Email is already in use");
            user.updateProfile(user.getUsername(), email, user.getDisplayName());
        }
        if (request.displayName() != null) {
            user.updateProfile(
                    user.getUsername(),
                    user.getEmail(),
                    normalizedRequired(request.displayName(), "Display name"));
        }
        if (request.role() != null) changeRole(actor, user, request.role());
        users.save(user);
        auditService.record(actor, "UPDATE_USER", "USER", id.toString());
        return AdminDtos.UserSummary.from(user);
    }

    @Transactional
    public AdminDtos.UserSummary enableUser(Long id) {
        User actor = currentAdmin();
        User user = findUser(id);
        ensureCanManageAccount(actor, user);
        user.enable();
        users.save(user);
        auditService.record(actor, "ENABLE_USER", "USER", id.toString());
        return AdminDtos.UserSummary.from(user);
    }

    @Transactional
    public AdminDtos.UserSummary disableUser(Long id) {
        User actor = currentAdmin();
        User user = findUser(id);
        ensureCanChangeAdministrativeState(actor, user);
        user.disable();
        users.save(user);
        auditService.record(actor, "DISABLE_USER", "USER", id.toString());
        return AdminDtos.UserSummary.from(user);
    }

    @Transactional
    public void resetPassword(Long id, AdminDtos.ResetPasswordRequest request) {
        User actor = currentAdmin();
        User user = findUser(id);
        ensureCanManageAccount(actor, user);
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        users.save(user);
        auditService.record(actor, "RESET_PASSWORD", "USER", id.toString());
    }

    @Transactional
    public void deleteUser(Long id) {
        User actor = currentAdmin();
        User user = findUser(id);
        ensureCanChangeAdministrativeState(actor, user);
        if (surveys.existsByCreatedById(id) || activities.existsByCreatedById(id)) {
            throw new IllegalArgumentException(
                    "User owns surveys or activities and must be disabled instead");
        }
        users.delete(user);
        users.flush();
        auditService.record(actor, "DELETE_USER", "USER", id.toString());
    }

    private User create(
            String username, String email, String password, String displayName, Role role) {
        String normalizedUsername = normalizedRequired(username, "Username");
        String normalizedEmail = normalizedOptional(email);
        if (normalizedEmail != null) {
            normalizedEmail = normalizedEmail.toLowerCase();
        }
        String normalizedDisplayName = normalizedRequired(displayName, "Display name");
        if (users.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new IllegalArgumentException("Username is already in use");
        }
        if (normalizedEmail != null && users.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email is already in use");
        }
        return users.save(
                new User(
                        normalizedUsername,
                        normalizedEmail,
                        passwordEncoder.encode(password),
                        normalizedDisplayName,
                        role));
    }

    private void changeRole(User actor, User user, Role role) {
        if (user.getRole() == Role.SYSTEM_ADMIN
                && role != Role.SYSTEM_ADMIN
                && users.countByRole(Role.SYSTEM_ADMIN) <= 1) {
            throw new IllegalArgumentException("At least one system administrator must remain");
        }
        user.changeRole(role);
    }

    private void ensureCanChangeAdministrativeState(User actor, User user) {
        ensureCanManageAccount(actor, user);
        if (actor.getId().equals(user.getId()))
            throw new IllegalArgumentException("An administrator cannot modify their own access");
        if (user.getRole() == Role.SYSTEM_ADMIN && users.countByRole(Role.SYSTEM_ADMIN) <= 1) {
            throw new IllegalArgumentException("At least one system administrator must remain");
        }
    }

    private void ensureCanManageAccount(User actor, User user) {
        if (user.isSystemOwner() && !actor.isSystemOwner()) {
            throw new IllegalArgumentException(
                    "The system owner account can only be managed by itself");
        }
    }

    private User findUser(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User was not found"));
    }

    private User currentAdmin() {
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        CurrentUser.username(), CurrentUser.username())
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Authenticated administrator was not found"));
    }

    private String normalizedRequired(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) throw new IllegalArgumentException(field + " cannot be blank");
        return normalized;
    }

    private String normalizedOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
