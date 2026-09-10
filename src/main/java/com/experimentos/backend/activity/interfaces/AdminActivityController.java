package com.experimentos.backend.activity.interfaces;

import com.experimentos.backend.activity.application.AdminActivityService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/activities")
public class AdminActivityController {
    private final AdminActivityService service;

    public AdminActivityController(AdminActivityService service) {
        this.service = service;
    }

    @GetMapping
    public List<ActivityAdminDtos.ActivityResponse> listAll() {
        return service.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityAdminDtos.ActivityResponse create(
            @Valid @RequestBody ActivityAdminDtos.ActivityRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ActivityAdminDtos.ActivityResponse update(
            @PathVariable Long id, @Valid @RequestBody ActivityAdminDtos.ActivityRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/open")
    public ActivityAdminDtos.ActivityResponse open(@PathVariable Long id) {
        return service.open(id);
    }

    @PatchMapping("/{id}/close")
    public ActivityAdminDtos.ActivityResponse close(@PathVariable Long id) {
        return service.close(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
