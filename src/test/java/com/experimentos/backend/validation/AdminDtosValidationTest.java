package com.experimentos.backend.validation;

import static org.mockito.Mockito.mock;

import com.experimentos.backend.admin.application.AdminService;
import com.experimentos.backend.admin.interfaces.AdminController;
import com.experimentos.backend.admin.interfaces.AdminDtos;
import java.util.List;

class AdminDtosValidationTest extends DtoContract {
    private final AdminService service = mock(AdminService.class);

    @Override
    protected Object controller() {
        return new AdminController(service);
    }

    @Override
    protected List<Object> services() {
        return List.of(service);
    }

    @Override
    protected List<Case> cases() {
        return List.of(
                new Case(
                        "username null",
                        AdminDtos.CreateUserRequest.class,
                        "POST",
                        "/api/v1/admin/users",
                        "{\"username\":null,\"email\":\"maria@example.com\",\"password\":\"Password1!\",\"displayName\":\"Maria\",\"role\":\"EMPLOYEE\"}",
                        "username",
                        false),
                new Case(
                        "username fifty",
                        AdminDtos.CreateUserRequest.class,
                        "POST",
                        "/api/v1/admin/users",
                        "{\"username\":\"uuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu\",\"email\":\"maria@example.com\",\"password\":\"Password1!\",\"displayName\":\"Maria\",\"role\":\"EMPLOYEE\"}",
                        null,
                        false),
                new Case(
                        "username overflow",
                        AdminDtos.CreateUserRequest.class,
                        "POST",
                        "/api/v1/admin/users",
                        "{\"username\":\"uuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu\",\"email\":\"maria@example.com\",\"password\":\"Password1!\",\"displayName\":\"Maria\",\"role\":\"EMPLOYEE\"}",
                        "username",
                        false),
                new Case(
                        "email null",
                        AdminDtos.CreateUserRequest.class,
                        "POST",
                        "/api/v1/admin/users",
                        "{\"username\":\"maria\",\"email\":null,\"password\":\"Password1!\",\"displayName\":\"Maria\",\"role\":\"EMPLOYEE\"}",
                        "email",
                        false),
                new Case(
                        "malformed email",
                        AdminDtos.CreateUserRequest.class,
                        "POST",
                        "/api/v1/admin/users",
                        "{\"username\":\"maria\",\"email\":\"bad\",\"password\":\"Password1!\",\"displayName\":\"Maria\",\"role\":\"EMPLOYEE\"}",
                        "email",
                        false),
                new Case(
                        "password below minimum",
                        AdminDtos.CreateUserRequest.class,
                        "POST",
                        "/api/v1/admin/users",
                        "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"password\":\"short\",\"displayName\":\"Maria\",\"role\":\"EMPLOYEE\"}",
                        "password",
                        false),
                new Case(
                        "password maximum",
                        AdminDtos.CreateUserRequest.class,
                        "POST",
                        "/api/v1/admin/users",
                        "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"password\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"displayName\":\"Maria\",\"role\":\"EMPLOYEE\"}",
                        null,
                        false),
                new Case(
                        "password overflow",
                        AdminDtos.CreateUserRequest.class,
                        "POST",
                        "/api/v1/admin/users",
                        "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"password\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"displayName\":\"Maria\",\"role\":\"EMPLOYEE\"}",
                        "password",
                        false),
                new Case(
                        "displayName blank",
                        AdminDtos.CreateUserRequest.class,
                        "POST",
                        "/api/v1/admin/users",
                        "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"password\":\"Password1!\",\"displayName\":\" \",\"role\":\"EMPLOYEE\"}",
                        "displayName",
                        false),
                new Case(
                        "role null",
                        AdminDtos.CreateUserRequest.class,
                        "POST",
                        "/api/v1/admin/users",
                        "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"password\":\"Password1!\",\"displayName\":\"Maria\",\"role\":null}",
                        "role",
                        false),
                new Case(
                        "employee create",
                        AdminDtos.CreateUserRequest.class,
                        "POST",
                        "/api/v1/admin/users",
                        "{\"username\":\"maria\",\"email\":\"maria@example.com\",\"password\":\"Password1!\",\"displayName\":\"Maria\",\"role\":\"EMPLOYEE\"}",
                        null,
                        true),
                new Case(
                        "hr without optional email",
                        AdminDtos.CreateHrRequest.class,
                        "POST",
                        "/api/v1/admin/hr-members",
                        "{\"username\":\"hr\",\"password\":\"Password1!\",\"displayName\":\"HR\"}",
                        null,
                        true),
                new Case(
                        "hr malformed optional email",
                        AdminDtos.CreateHrRequest.class,
                        "POST",
                        "/api/v1/admin/hr-members",
                        "{\"username\":\"hr\",\"password\":\"Password1!\",\"displayName\":\"HR\",\"email\":\"bad\"}",
                        "email",
                        true),
                new Case(
                        "hr displayName overflow",
                        AdminDtos.CreateHrRequest.class,
                        "POST",
                        "/api/v1/admin/hr-members",
                        "{\"username\":\"hr\",\"password\":\"Password1!\",\"displayName\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}",
                        "displayName",
                        true),
                new Case(
                        "update valid",
                        AdminDtos.UpdateUserRequest.class,
                        "PATCH",
                        "/api/v1/admin/users/1",
                        "{\"displayName\":\"Maria\"}",
                        null,
                        true),
                new Case(
                        "update oversized username",
                        AdminDtos.UpdateUserRequest.class,
                        "PATCH",
                        "/api/v1/admin/users/1",
                        "{\"displayName\":\"Maria\",\"username\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}",
                        "username",
                        true),
                new Case(
                        "update unknown role",
                        AdminDtos.UpdateUserRequest.class,
                        "PATCH",
                        "/api/v1/admin/users/1",
                        "{\"displayName\":\"Maria\",\"role\":\"OWNER\"}",
                        "binding",
                        true),
                new Case(
                        "reset password valid",
                        AdminDtos.ResetPasswordRequest.class,
                        "POST",
                        "/api/v1/admin/users/1/password",
                        "{\"newPassword\":\"Password1!\"}",
                        null,
                        true),
                new Case(
                        "reset short password",
                        AdminDtos.ResetPasswordRequest.class,
                        "POST",
                        "/api/v1/admin/users/1/password",
                        "{\"newPassword\":\"short\"}",
                        "newPassword",
                        true),
                new Case(
                        "reset blank password",
                        AdminDtos.ResetPasswordRequest.class,
                        "POST",
                        "/api/v1/admin/users/1/password",
                        "{\"newPassword\":\" \"}",
                        "newPassword",
                        true));
    }
}
