package com.experimentos.backend.authentication.application;

import com.experimentos.backend.authentication.domain.PasswordResetNotificationPort;
import com.experimentos.backend.authentication.domain.PasswordResetToken;
import com.experimentos.backend.authentication.infrastructure.PasswordResetTokenRepository;
import com.experimentos.backend.authentication.interfaces.AuthDtos;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.Role;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates password recovery without exposing account existence to public callers. */
@Service
public class PasswordResetService {
    private static final String GENERIC_MESSAGE =
            "Si la cuenta existe, recibirás instrucciones para recuperar el acceso.";
    private static final String SUCCESS_MESSAGE =
            "Tu contraseña fue actualizada. Ya puedes iniciar sesión.";

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetNotificationPort notificationPort;
    private final PasswordResetProperties properties;
    private final PasswordResetRateLimiter rateLimiter;
    private final SecureRandom secureRandom;
    private final Clock clock;

    @Autowired
    public PasswordResetService(
            UserRepository users,
            PasswordResetTokenRepository tokens,
            PasswordEncoder passwordEncoder,
            PasswordResetNotificationPort notificationPort,
            PasswordResetProperties properties,
            PasswordResetRateLimiter rateLimiter) {
        this(
                users,
                tokens,
                passwordEncoder,
                notificationPort,
                properties,
                rateLimiter,
                new SecureRandom(),
                Clock.systemUTC());
    }

    PasswordResetService(
            UserRepository users,
            PasswordResetTokenRepository tokens,
            PasswordEncoder passwordEncoder,
            PasswordResetNotificationPort notificationPort,
            PasswordResetProperties properties,
            PasswordResetRateLimiter rateLimiter,
            SecureRandom secureRandom,
            Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.notificationPort = notificationPort;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    @Transactional
    public AuthDtos.PasswordRecoveryResponse requestRecovery(String identifier) {
        String normalizedIdentifier = normalize(identifier);
        if (normalizedIdentifier.isBlank() || !rateLimiter.allow(normalizedIdentifier)) {
            return new AuthDtos.PasswordRecoveryResponse(GENERIC_MESSAGE);
        }

        User user = findEmployeeWithEmail(normalizedIdentifier);
        if (user == null) {
            return new AuthDtos.PasswordRecoveryResponse(GENERIC_MESSAGE);
        }

        tokens.deleteByUserId(user.getId());
        String rawToken = createRawToken();
        PasswordResetToken token =
                new PasswordResetToken(
                        user,
                        hashToken(rawToken),
                        clock.instant().plusSeconds(properties.expirationMinutes() * 60L));
        tokens.save(token);
        notificationPort.send(user, properties.urlBase() + rawToken);
        return new AuthDtos.PasswordRecoveryResponse(GENERIC_MESSAGE);
    }

    @Transactional
    public AuthDtos.PasswordRecoveryResponse confirmRecovery(
            AuthDtos.PasswordResetConfirmRequest request) {
        String rawToken = request.token() == null ? "" : request.token().trim();
        PasswordResetToken token =
                tokens.findByTokenHash(hashToken(rawToken))
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Invalid or expired reset token"));
        Instant now = clock.instant();
        if (!token.isUsableAt(now)) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = token.getUser();
        if (!user.isEnabled() || user.getRole() != Role.EMPLOYEE) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        token.markUsed(now);
        tokens.deleteByUserId(user.getId());
        return new AuthDtos.PasswordRecoveryResponse(SUCCESS_MESSAGE);
    }

    private User findEmployeeWithEmail(String identifier) {
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
                .filter(user -> user.getRole() == Role.EMPLOYEE)
                .filter(User::isEnabled)
                .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                .orElse(null);
    }

    private String createRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
