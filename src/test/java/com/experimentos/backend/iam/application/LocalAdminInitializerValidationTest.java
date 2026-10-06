package com.experimentos.backend.iam.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.Role;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class LocalAdminInitializerValidationTest {
    static class Fixture {
        final UserRepository users = mock(UserRepository.class);
        final PasswordEncoder encoder = mock(PasswordEncoder.class);

        Fixture() {
            when(encoder.encode("test-password")).thenReturn("encoded");
            when(users.save(any()))
                    .thenAnswer(
                            i -> {
                                User u = i.getArgument(0);
                                if (u.getId() == null) ReflectionTestUtils.setField(u, "id", 1L);
                                return u;
                            });
        }

        User user(long id, Role role) {
            var u = new User("local-admin", "admin@localhost", "existing-hash", "Admin", role);
            ReflectionTestUtils.setField(u, "id", id);
            return u;
        }

        void existing(User user) {
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("local-admin", "local-admin"))
                    .thenReturn(Optional.of(user));
        }

        LocalAdminInitializer initializer() {
            return new LocalAdminInitializer(users, encoder, "local-admin", "test-password");
        }

        AnnotationConfigApplicationContext context(
                boolean local, String username, String password) {
            var context = new AnnotationConfigApplicationContext();
            if (local) context.getEnvironment().setActiveProfiles("local");
            context.getEnvironment()
                    .getPropertySources()
                    .addFirst(
                            new MapPropertySource(
                                    "synthetic",
                                    Map.of(
                                            "app.local-admin.username",
                                            username,
                                            "app.local-admin.password",
                                            password)));
            context.registerBean(UserRepository.class, () -> users);
            context.registerBean(PasswordEncoder.class, () -> encoder);
            context.register(LocalAdminInitializer.class);
            try {
                context.refresh();
                return context;
            } catch (RuntimeException e) {
                context.close();
                throw e;
            }
        }
    }

    record Case(String name, Runnable action) {}

    @TestFactory
    Stream<DynamicTest> unit() {
        return Stream.of(
                        new Case(
                                "missing local username",
                                () -> {
                                    var f = new Fixture();
                                    assertThatThrownBy(
                                                    () ->
                                                            new LocalAdminInitializer(
                                                                    f.users,
                                                                    f.encoder,
                                                                    null,
                                                                    "test-password"))
                                            .isInstanceOf(IllegalStateException.class);
                                    verifyNoInteractions(f.users, f.encoder);
                                }),
                        new Case(
                                "empty local username",
                                () -> {
                                    var f = new Fixture();
                                    assertThatThrownBy(
                                                    () ->
                                                            new LocalAdminInitializer(
                                                                    f.users,
                                                                    f.encoder,
                                                                    "",
                                                                    "test-password"))
                                            .isInstanceOf(IllegalStateException.class);
                                    verifyNoInteractions(f.users, f.encoder);
                                }),
                        new Case(
                                "blank local username",
                                () -> {
                                    var f = new Fixture();
                                    assertThatThrownBy(
                                                    () ->
                                                            new LocalAdminInitializer(
                                                                    f.users,
                                                                    f.encoder,
                                                                    " \t",
                                                                    "test-password"))
                                            .isInstanceOf(IllegalStateException.class);
                                    verifyNoInteractions(f.users, f.encoder);
                                }),
                        new Case(
                                "missing local password",
                                () -> {
                                    var f = new Fixture();
                                    assertThatThrownBy(
                                                    () ->
                                                            new LocalAdminInitializer(
                                                                    f.users,
                                                                    f.encoder,
                                                                    "local-admin",
                                                                    null))
                                            .isInstanceOf(IllegalStateException.class);
                                    verifyNoInteractions(f.users, f.encoder);
                                }),
                        new Case(
                                "empty local password",
                                () -> {
                                    var f = new Fixture();
                                    assertThatThrownBy(
                                                    () ->
                                                            new LocalAdminInitializer(
                                                                    f.users,
                                                                    f.encoder,
                                                                    "local-admin",
                                                                    ""))
                                            .isInstanceOf(IllegalStateException.class);
                                    verifyNoInteractions(f.users, f.encoder);
                                }),
                        new Case(
                                "blank local password",
                                () -> {
                                    var f = new Fixture();
                                    assertThatThrownBy(
                                                    () ->
                                                            new LocalAdminInitializer(
                                                                    f.users,
                                                                    f.encoder,
                                                                    "local-admin",
                                                                    " \t"))
                                            .isInstanceOf(IllegalStateException.class);
                                    verifyNoInteractions(f.users, f.encoder);
                                }),
                        new Case(
                                "different existing owner prevents new owner",
                                () -> {
                                    var f = new Fixture();
                                    var owner = f.user(9, Role.SYSTEM_ADMIN);
                                    owner.markAsSystemOwner();
                                    when(f.users.findBySystemOwnerTrue())
                                            .thenReturn(Optional.of(owner));
                                    assertThatThrownBy(() -> f.initializer().run())
                                            .isInstanceOf(IllegalStateException.class)
                                            .hasMessageContaining("Only one");
                                    verify(f.users, never()).save(any());
                                }),
                        new Case(
                                "existing employee upgraded to administrator owner",
                                () -> {
                                    var f = new Fixture();
                                    var u = f.user(1, Role.EMPLOYEE);
                                    f.existing(u);
                                    f.initializer().run();
                                    assertThat(u.getRole()).isEqualTo(Role.SYSTEM_ADMIN);
                                    assertThat(u.isSystemOwner()).isTrue();
                                    verifyNoInteractions(f.encoder);
                                }),
                        new Case(
                                "existing HR upgraded to administrator owner",
                                () -> {
                                    var f = new Fixture();
                                    var u = f.user(1, Role.HR_MEMBER);
                                    f.existing(u);
                                    f.initializer().run();
                                    assertThat(u.getRole()).isEqualTo(Role.SYSTEM_ADMIN);
                                    assertThat(u.isSystemOwner()).isTrue();
                                }),
                        new Case(
                                "same existing owner accepted",
                                () -> {
                                    var f = new Fixture();
                                    var u = f.user(1, Role.SYSTEM_ADMIN);
                                    u.markAsSystemOwner();
                                    f.existing(u);
                                    when(f.users.findBySystemOwnerTrue())
                                            .thenReturn(Optional.of(u));
                                    f.initializer().run();
                                    verify(f.users).save(u);
                                    verifyNoInteractions(f.encoder);
                                }),
                        new Case(
                                "new owner password hashed",
                                () -> {
                                    var f = new Fixture();
                                    f.initializer().run();
                                    verify(f.users)
                                            .save(
                                                    argThat(
                                                            u ->
                                                                    u.getPasswordHash()
                                                                                    .equals(
                                                                                            "encoded")
                                                                            && u.isSystemOwner()
                                                                            && u.getRole()
                                                                                    == Role
                                                                                            .SYSTEM_ADMIN));
                                }),
                        new Case(
                                "repeat initializer preserves credential",
                                () -> {
                                    var f = new Fixture();
                                    var u = f.user(1, Role.SYSTEM_ADMIN);
                                    u.markAsSystemOwner();
                                    f.existing(u);
                                    var init = f.initializer();
                                    init.run();
                                    init.run();
                                    verifyNoInteractions(f.encoder);
                                    assertThat(u.getPasswordHash()).isEqualTo("existing-hash");
                                }))
                .map(c -> DynamicTest.dynamicTest(c.name(), c.action()::run));
    }

    @TestFactory
    Stream<DynamicTest> integration() {
        return Stream.of(
                        new Case(
                                "local inactive without configuration",
                                () -> {
                                    var f = new Fixture();
                                    try (var context =
                                            f.context(false, "local-admin", "test-password")) {
                                        assertThat(
                                                        context.getBeansOfType(
                                                                LocalAdminInitializer.class))
                                                .isEmpty();
                                        verifyNoInteractions(f.users, f.encoder);
                                    }
                                }),
                        new Case(
                                "local active creates owner",
                                () -> {
                                    var f = new Fixture();
                                    try (var context =
                                            f.context(true, "local-admin", "test-password")) {
                                        context.getBean(LocalAdminInitializer.class).run();
                                        verify(f.users).save(argThat(u -> u.isSystemOwner()));
                                    }
                                }),
                        new Case(
                                "local active existing employee repaired",
                                () -> {
                                    var f = new Fixture();
                                    try (var context =
                                            f.context(true, "local-admin", "test-password")) {
                                        var u = f.user(1, Role.EMPLOYEE);
                                        f.existing(u);
                                        context.getBean(LocalAdminInitializer.class).run();
                                        assertThat(u.isSystemOwner()).isTrue();
                                        assertThat(u.getRole()).isEqualTo(Role.SYSTEM_ADMIN);
                                    }
                                }),
                        new Case(
                                "local active existing HR repaired",
                                () -> {
                                    var f = new Fixture();
                                    try (var context =
                                            f.context(true, "local-admin", "test-password")) {
                                        var u = f.user(1, Role.HR_MEMBER);
                                        f.existing(u);
                                        context.getBean(LocalAdminInitializer.class).run();
                                        assertThat(u.getRole()).isEqualTo(Role.SYSTEM_ADMIN);
                                    }
                                }),
                        new Case(
                                "local active owner collision guarded",
                                () -> {
                                    var f = new Fixture();
                                    try (var context =
                                            f.context(true, "local-admin", "test-password")) {
                                        var owner = f.user(9, Role.SYSTEM_ADMIN);
                                        owner.markAsSystemOwner();
                                        when(f.users.findBySystemOwnerTrue())
                                                .thenReturn(Optional.of(owner));
                                        assertThatThrownBy(
                                                        () ->
                                                                context.getBean(
                                                                                LocalAdminInitializer
                                                                                        .class)
                                                                        .run())
                                                .isInstanceOf(IllegalStateException.class);
                                        verify(f.users, never()).save(any());
                                    }
                                }),
                        new Case(
                                "local context repeat run idempotent",
                                () -> {
                                    var f = new Fixture();
                                    try (var context =
                                            f.context(true, "local-admin", "test-password")) {
                                        var u = f.user(1, Role.SYSTEM_ADMIN);
                                        u.markAsSystemOwner();
                                        f.existing(u);
                                        var init = context.getBean(LocalAdminInitializer.class);
                                        init.run();
                                        init.run();
                                        verify(f.users, times(2)).save(u);
                                        verifyNoInteractions(f.encoder);
                                    }
                                }),
                        new Case(
                                "blank username fails local Spring binding construction",
                                () -> {
                                    var f = new Fixture();
                                    assertThatThrownBy(
                                                    () -> {
                                                        try (var context =
                                                                f.context(
                                                                        true,
                                                                        "",
                                                                        "test-password")) {}
                                                    })
                                            .isInstanceOf(
                                                    org.springframework.beans.factory
                                                            .BeanCreationException.class);
                                    verify(f.users, never()).save(any());
                                }),
                        new Case(
                                "blank password fails local Spring construction",
                                () -> {
                                    var f = new Fixture();
                                    assertThatThrownBy(
                                                    () -> {
                                                        try (var context =
                                                                f.context(
                                                                        true, "local-admin", "")) {}
                                                    })
                                            .isInstanceOf(
                                                    org.springframework.beans.factory
                                                            .BeanCreationException.class);
                                    verify(f.users, never()).save(any());
                                }))
                .map(c -> DynamicTest.dynamicTest(c.name(), c.action()::run));
    }
}
