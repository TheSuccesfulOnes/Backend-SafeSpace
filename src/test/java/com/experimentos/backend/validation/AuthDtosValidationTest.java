package com.experimentos.backend.validation;

import static org.mockito.Mockito.mock;

import com.experimentos.backend.authentication.application.AuthService;
import com.experimentos.backend.authentication.interfaces.AuthController;
import com.experimentos.backend.authentication.interfaces.AuthDtos;
import java.util.List;

class AuthDtosValidationTest extends DtoContract {
    private final AuthService service = mock(AuthService.class);
    private final com.experimentos.backend.authentication.application.PasswordResetService reset =
            mock(com.experimentos.backend.authentication.application.PasswordResetService.class);

    @Override
    protected Object controller() {
        return new AuthController(service, reset);
    }

    @Override
    protected List<Object> services() {
        return List.of(service, reset);
    }

    @Override
    protected List<Case> cases() {
        return List.of(
                new Case(
                        "username absent",
                        AuthDtos.RegisterRequest.class,
                        "POST",
                        "/api/v1/auth/register",
                        "{\"username\":null,\"email\":\"maria@example.com\",\"password\":\"Password1!\",\"confirmPassword\":\"Password1!\",\"displayName\":\"Maria\"}",
                        "username",
                        false),
                new Case(
                        "username fifty",
                        AuthDtos.RegisterRequest.class,
                        "POST",
                        "/api/v1/auth/register",
                        "{\"username\":\"uuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu\",\"email\":\"maria@example.com\",\"password\":\"Password1!\",\"confirmPassword\":\"Password1!\",\"displayName\":\"Maria\"}",
                        null,
                        false),
                new Case(
                        "username fifty one",
                        AuthDtos.RegisterRequest.class,
                        "POST",
                        "/api/v1/auth/register",
                        "{\"username\":\"uuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu\",\"email\":\"maria@example.com\",\"password\":\"Password1!\",\"confirmPassword\":\"Password1!\",\"displayName\":\"Maria\"}",
                        "username",
                        false),
                new Case(
                        "invalid email",
                        AuthDtos.RegisterRequest.class,
                        "POST",
                        "/api/v1/auth/register",
                        "{\"username\":\"maria\",\"email\":\"not-email\",\"password\":\"Password1!\",\"confirmPassword\":\"Password1!\",\"displayName\":\"Maria\"}",
                        "email",
                        false),
                new Case(
                        "password seven",
                        AuthDtos.RegisterRequest.class,
                        "POST",
                        "/api/v1/auth/register",
                        "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"password\":\"Ab1!xyz\",\"confirmPassword\":\"Password1!\",\"displayName\":\"Maria\"}",
                        "password",
                        false),
                new Case(
                        "password eight",
                        AuthDtos.RegisterRequest.class,
                        "POST",
                        "/api/v1/auth/register",
                        "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"password\":\"Ab1!xyzz\",\"confirmPassword\":\"Password1!\",\"displayName\":\"Maria\"}",
                        null,
                        false),
                new Case(
                        "password seventy three",
                        AuthDtos.RegisterRequest.class,
                        "POST",
                        "/api/v1/auth/register",
                        "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"password\":\"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"confirmPassword\":\"Password1!\",\"displayName\":\"Maria\"}",
                        "password",
                        false),
                new Case(
                        "confirmation absent",
                        AuthDtos.RegisterRequest.class,
                        "POST",
                        "/api/v1/auth/register",
                        "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"password\":\"Password1!\",\"confirmPassword\":null,\"displayName\":\"Maria\"}",
                        "confirmPassword",
                        false),
                new Case(
                        "display name maximum",
                        AuthDtos.RegisterRequest.class,
                        "POST",
                        "/api/v1/auth/register",
                        "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"password\":\"Password1!\",\"confirmPassword\":\"Password1!\",\"displayName\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}",
                        null,
                        false),
                new Case(
                        "display name overflow",
                        AuthDtos.RegisterRequest.class,
                        "POST",
                        "/api/v1/auth/register",
                        "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"password\":\"Password1!\",\"confirmPassword\":\"Password1!\",\"displayName\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}",
                        "displayName",
                        false),
                new Case(
                        "register valid",
                        AuthDtos.RegisterRequest.class,
                        "POST",
                        "/api/v1/auth/register",
                        "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"password\":\"Password1!\",\"confirmPassword\":\"Password1!\",\"displayName\":\"Maria\"}",
                        null,
                        true),
                new Case(
                        "login valid",
                        AuthDtos.LoginRequest.class,
                        "POST",
                        "/api/v1/auth/login",
                        "{\"identifier\":\"maria\",\"password\":\"Password1!\"}",
                        null,
                        true),
                new Case(
                        "login blank identifier",
                        AuthDtos.LoginRequest.class,
                        "POST",
                        "/api/v1/auth/login",
                        "{\"identifier\":\" \",\"password\":\"Password1!\"}",
                        "identifier",
                        true),
                new Case(
                        "login absent password",
                        AuthDtos.LoginRequest.class,
                        "POST",
                        "/api/v1/auth/login",
                        "{\"identifier\":\"maria\",\"password\":null}",
                        "password",
                        true),
                new Case(
                        "recovery valid",
                        AuthDtos.PasswordRecoveryRequest.class,
                        "POST",
                        "/api/v1/auth/password-recovery/request",
                        "{\"identifier\":\"maria\"}",
                        null,
                        true),
                new Case(
                        "recovery identifier overflow",
                        AuthDtos.PasswordRecoveryRequest.class,
                        "POST",
                        "/api/v1/auth/password-recovery/request",
                        "{\"identifier\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}",
                        "identifier",
                        true),
                new Case(
                        "reset valid",
                        AuthDtos.PasswordResetConfirmRequest.class,
                        "POST",
                        "/api/v1/auth/password-recovery/confirm",
                        "{\"token\":\"test-token\",\"newPassword\":\"Password1!\",\"confirmPassword\":\"Password1!\"}",
                        null,
                        true),
                new Case(
                        "reset oversized token",
                        AuthDtos.PasswordResetConfirmRequest.class,
                        "POST",
                        "/api/v1/auth/password-recovery/confirm",
                        "{\"token\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\",\"newPassword\":\"Password1!\",\"confirmPassword\":\"Password1!\"}",
                        "token",
                        true),
                new Case(
                        "reset short new password",
                        AuthDtos.PasswordResetConfirmRequest.class,
                        "POST",
                        "/api/v1/auth/password-recovery/confirm",
                        "{\"token\":\"test-token\",\"newPassword\":\"short\",\"confirmPassword\":\"Password1!\"}",
                        "newPassword",
                        true),
                new Case(
                        "reset blank confirmation",
                        AuthDtos.PasswordResetConfirmRequest.class,
                        "POST",
                        "/api/v1/auth/password-recovery/confirm",
                        "{\"token\":\"test-token\",\"newPassword\":\"Password1!\",\"confirmPassword\":\" \"}",
                        "confirmPassword",
                        true));
    }
}
