package com.experimentos.backend.authentication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.experimentos.backend.authentication.domain.PasswordResetNotificationPort;
import com.experimentos.backend.authentication.domain.PasswordResetToken;
import com.experimentos.backend.authentication.infrastructure.PasswordResetTokenRepository;
import com.experimentos.backend.authentication.interfaces.AuthDtos;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Unit tests for the employee password recovery use cases. */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-03T12:00:00Z");

    @Mock UserRepository users;
    @Mock PasswordResetTokenRepository tokens;
    @Mock PasswordResetNotificationPort notificationPort;

    private BCryptPasswordEncoder passwordEncoder;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        PasswordResetProperties properties =
                new PasswordResetProperties("http://test/reset?token=", 15, 3, 15);
        PasswordResetRateLimiter rateLimiter =
                new PasswordResetRateLimiter(properties, Clock.fixed(NOW, ZoneOffset.UTC));
        service =
                new PasswordResetService(
                        users,
                        tokens,
                        passwordEncoder,
                        notificationPort,
                        properties,
                        rateLimiter,
                        new java.security.SecureRandom(),
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void doesNotRevealWhetherIdentifierExists() {
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("unknown", "unknown"))
                .thenReturn(Optional.empty());

        AuthDtos.PasswordRecoveryResponse response = service.requestRecovery("unknown");

        assertThat(response.message())
                .isEqualTo(
                        "Si la cuenta existe, recibirás instrucciones para recuperar el acceso.");
        verify(tokens, never()).save(any());
        verify(notificationPort, never()).send(any(), anyString());
    }

    @Test
    void createsSingleUseTokenAndChangesEmployeePassword() {
        User user = new User("maria", "maria@example.com", null, "Maria", Role.EMPLOYEE);
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("maria", "maria"))
                .thenReturn(Optional.of(user));
        when(tokens.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.requestRecovery("maria");

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationPort).send(any(User.class), linkCaptor.capture());
        String rawToken =
                linkCaptor.getValue().substring(linkCaptor.getValue().indexOf("token=") + 6);
        PasswordResetToken token = new PasswordResetToken(user, "hash", NOW.plusSeconds(900));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        AuthDtos.PasswordRecoveryResponse response =
                service.confirmRecovery(
                        new AuthDtos.PasswordResetConfirmRequest(
                                rawToken, "NewPassword123!", "NewPassword123!"));

        assertThat(response.message())
                .isEqualTo("Tu contraseña fue actualizada. Ya puedes iniciar sesión.");
        assertThat(passwordEncoder.matches("NewPassword123!", user.getPasswordHash())).isTrue();
        verify(tokens, org.mockito.Mockito.times(2)).deleteByUserId(user.getId());
    }

    @Test
    void rejectsExpiredTokenBeforeChangingPassword() {
        User user = new User("maria", "maria@example.com", null, "Maria", Role.EMPLOYEE);
        PasswordResetToken expired = new PasswordResetToken(user, "hash", NOW.minusSeconds(1));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(
                        () ->
                                service.confirmRecovery(
                                        new AuthDtos.PasswordResetConfirmRequest(
                                                "expired", "NewPassword123!", "NewPassword123!")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid or expired reset token");
        assertThat(user.getPasswordHash()).isNull();
    }
}
