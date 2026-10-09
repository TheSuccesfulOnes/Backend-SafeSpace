package com.experimentos.backend.authentication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.experimentos.backend.authentication.interfaces.AuthDtos;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Unit tests for authentication use cases without requiring a database. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock JwtService jwtService;

    private AuthService authService;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(users, passwordEncoder, jwtService);
    }

    @Test
    void registerCreatesAnEmployeeWithEncodedPassword() {
        AuthDtos.RegisterRequest request =
                new AuthDtos.RegisterRequest(
                        "maria",
                        "maria@example.com",
                        "Password123!",
                        "Password123!",
                        "Maria Lopez");
        when(users.existsByUsernameIgnoreCase("maria")).thenReturn(false);
        when(users.existsByEmailIgnoreCase("maria@example.com")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.createToken(any(User.class))).thenReturn("token");

        AuthDtos.AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("token");
        verify(users)
                .save(
                        argThat(
                                user ->
                                        user.getPasswordHash() != null
                                                && !user.getPasswordHash().equals("Password123!")
                                                && passwordEncoder.matches(
                                                        "Password123!", user.getPasswordHash())
                                                && user.getDisplayName().equals("Maria Lopez")));
    }

    @Test
    void registerRejectsDifferentPasswords() {
        AuthDtos.RegisterRequest request =
                new AuthDtos.RegisterRequest(
                        "maria", "maria@example.com", "password123", "different123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Passwords do not match");
        verifyNoInteractions(users, jwtService);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "Ab1!",
                "password1!",
                "PASSWORD1!",
                "Password!",
                "Password1",
                "Password1 ",
                "Password1\u00a0"
            })
    void registerRejectsWeakPasswordsBeforeAccessingPersistence(String password) {
        AuthDtos.RegisterRequest request =
                new AuthDtos.RegisterRequest(
                        "maria", "maria@example.com", password, password, "Maria");
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(users, jwtService);
    }

    @Test
    void registerRejectsPasswordsExceedingBcryptByteLimit() {
        String password = "Áb1!" + "é".repeat(34);
        assertThatThrownBy(
                        () ->
                                authService.register(
                                        new AuthDtos.RegisterRequest(
                                                "maria", "maria@example.com", password, password)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password exceeds the maximum allowed length");
        verifyNoInteractions(users, jwtService);
    }

    @Test
    void loginRejectsInvalidPassword() {
        User user =
                new User(
                        "maria",
                        "maria@example.com",
                        passwordEncoder.encode("password123"),
                        "Maria",
                        com.experimentos.backend.shared.security.Role.EMPLOYEE);
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("maria", "maria"))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(
                        () ->
                                authService.login(
                                        new AuthDtos.LoginRequest("maria", "wrong-password")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid credentials");
        verifyNoInteractions(jwtService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"maria", " maria ", "maria@example.com", " maria@example.com "})
    void loginNormalizesIdentifierWithoutChangingPassword(String identifier) {
        String password = " Password123! ";
        User user =
                new User(
                        "maria",
                        "maria@example.com",
                        passwordEncoder.encode(password),
                        "Maria",
                        com.experimentos.backend.shared.security.Role.EMPLOYEE);
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier.trim(), identifier.trim()))
                .thenReturn(Optional.of(user));
        when(jwtService.createToken(user)).thenReturn("synthetic-token");

        AuthDtos.AuthResponse result =
                authService.login(new AuthDtos.LoginRequest(identifier, password));

        assertThat(result.username()).isEqualTo("maria");
        assertThat(result.token()).isEqualTo("synthetic-token");
        verify(users)
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier.trim(), identifier.trim());
        verify(jwtService).createToken(user);
    }
}
