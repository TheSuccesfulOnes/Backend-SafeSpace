package com.experimentos.backend.authentication.interfaces;

import com.experimentos.backend.authentication.application.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@SecurityRequirements
public class AuthController {
    private final AuthService authService;
    private final com.experimentos.backend.authentication.application.PasswordResetService
            passwordResetService;

    public AuthController(
            AuthService authService,
            com.experimentos.backend.authentication.application.PasswordResetService
                    passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/password-recovery/request")
    public AuthDtos.PasswordRecoveryResponse requestPasswordRecovery(
            @Valid @RequestBody AuthDtos.PasswordRecoveryRequest request) {
        return passwordResetService.requestRecovery(request.identifier());
    }

    @PostMapping("/password-recovery/confirm")
    public AuthDtos.PasswordRecoveryResponse confirmPasswordRecovery(
            @Valid @RequestBody AuthDtos.PasswordResetConfirmRequest request) {
        return passwordResetService.confirmRecovery(request);
    }
}
