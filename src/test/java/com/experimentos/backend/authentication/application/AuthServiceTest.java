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
                        "maria", "maria@example.com", "password123", "password123", "Maria Lopez");
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
                                                && !user.getPasswordHash().equals("password123")
                                                && passwordEncoder.matches(
                                                        "password123", user.getPasswordHash())
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
}
