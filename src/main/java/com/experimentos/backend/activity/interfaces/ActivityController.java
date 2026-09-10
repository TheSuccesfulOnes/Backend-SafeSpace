package com.experimentos.backend.activity.interfaces;

import com.experimentos.backend.activity.application.ActivityService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {
    private final ActivityService service;

    public ActivityController(ActivityService service) {
        this.service = service;
    }

    @GetMapping
    public List<ActivityDtos.ActivityResponse> open() {
        return service.open();
    }

    /** Returns every activity state for the HR management view. */
    @GetMapping("/managed")
    @PreAuthorize("hasAnyRole('HR_MEMBER', 'SYSTEM_ADMIN')")
    public List<ActivityDtos.ActivityResponse> managed() {
        return service.managed();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR_MEMBER', 'SYSTEM_ADMIN')")
    public ActivityDtos.ActivityResponse create(
            @Valid @RequestBody ActivityDtos.CreateActivityRequest request) {
        return service.create(request);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('HR_MEMBER', 'SYSTEM_ADMIN')")
    public void close(@PathVariable Long id) {
        service.close(id);
    }

    @PostMapping("/{id}/votes")
    public void vote(@PathVariable Long id, @Valid @RequestBody ActivityDtos.VoteRequest request) {
        service.vote(id, request);
    }
}
