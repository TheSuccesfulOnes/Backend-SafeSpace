package com.experimentos.backend.iam.application;

import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
public class LocalAdminInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;

    public LocalAdminInitializer(
            UserRepository users,
            PasswordEncoder encoder,
            @Value("${app.local-admin.username}") String username,
            @Value("${app.local-admin.password}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        User admin =
                users.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, username)
                        .orElseGet(
                                () ->
                                        new User(
                                                username,
                                                "admin@localhost",
                                                encoder.encode(password),
                                                "Local Administrator",
                                                Role.SYSTEM_ADMIN));
        users.findBySystemOwnerTrue()
                .filter(owner -> !owner.getId().equals(admin.getId()))
                .ifPresent(
                        owner -> {
                            throw new IllegalStateException(
                                    "Only one system owner account is allowed");
                        });
        if (!admin.isSystemOwner()) {
            admin.markAsSystemOwner();
        }
        if (admin.getRole() != Role.SYSTEM_ADMIN) {
            admin.changeRole(Role.SYSTEM_ADMIN);
        }
        users.save(admin);
    }
}
