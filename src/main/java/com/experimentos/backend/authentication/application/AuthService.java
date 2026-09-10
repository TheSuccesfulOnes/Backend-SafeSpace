package com.experimentos.backend.authentication.application;

import com.experimentos.backend.authentication.interfaces.AuthDtos;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.JwtService;
import com.experimentos.backend.shared.security.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        validatePasswordConfirmation(request.password(), request.confirmPassword());
        if (users.existsByUsernameIgnoreCase(request.username()))
            throw new IllegalArgumentException("Username is already in use");
        if (users.existsByEmailIgnoreCase(request.email()))
            throw new IllegalArgumentException("Email is already in use");
        String displayName =
                request.displayName() == null || request.displayName().isBlank()
                        ? request.username().trim()
                        : request.displayName().trim();
        User user =
                users.save(
                        new User(
                                request.username().trim(),
                                request.email().trim().toLowerCase(),
                                passwordEncoder.encode(request.password()),
                                displayName,
                                Role.EMPLOYEE));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        User user =
                users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                request.identifier(), request.identifier())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!user.isEnabled()
                || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash()))
            throw new IllegalArgumentException("Invalid credentials");
        return toResponse(user);
    }

    private AuthDtos.AuthResponse toResponse(User user) {
        return new AuthDtos.AuthResponse(
                jwtService.createToken(user),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getId());
    }

    private void validatePasswordConfirmation(String password, String confirmation) {
        if (!password.equals(confirmation))
            throw new IllegalArgumentException("Passwords do not match");
        if (password.length() < 8)
            throw new IllegalArgumentException("Password must contain at least 8 characters");
    }
}
