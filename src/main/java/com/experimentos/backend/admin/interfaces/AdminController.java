package com.experimentos.backend.admin.interfaces;

import com.experimentos.backend.admin.application.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/users")
    public List<AdminDtos.UserSummary> users() {
        return service.listUsers();
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminDtos.UserSummary createUser(
            @Valid @RequestBody AdminDtos.CreateUserRequest request) {
        return service.createUser(request);
    }

    @PostMapping("/hr-members")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminDtos.UserSummary createHr(@Valid @RequestBody AdminDtos.CreateHrRequest request) {
        return service.createHr(request);
    }

    @PatchMapping("/users/{id}")
    public AdminDtos.UserSummary updateUser(
            @PathVariable Long id, @Valid @RequestBody AdminDtos.UpdateUserRequest request) {
        return service.updateUser(id, request);
    }

    @PatchMapping("/users/{id}/enable")
    public AdminDtos.UserSummary enable(@PathVariable Long id) {
        return service.enableUser(id);
    }

    @PatchMapping("/users/{id}/disable")
    public AdminDtos.UserSummary disable(@PathVariable Long id) {
        return service.disableUser(id);
    }

    @PostMapping("/users/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(
            @PathVariable Long id, @Valid @RequestBody AdminDtos.ResetPasswordRequest request) {
        service.resetPassword(id, request);
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.deleteUser(id);
    }
}
