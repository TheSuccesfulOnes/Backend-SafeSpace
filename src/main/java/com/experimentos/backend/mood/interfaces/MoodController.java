package com.experimentos.backend.mood.interfaces;

import com.experimentos.backend.mood.application.MoodService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mood")
public class MoodController {
    private final MoodService service;

    public MoodController(MoodService service) {
        this.service = service;
    }

    @PostMapping("/today")
    public MoodDtos.MoodResponse submit(@Valid @RequestBody MoodDtos.SubmitMoodRequest request) {
        return service.submit(request);
    }

    @GetMapping("/today")
    public MoodDtos.MoodResponse today() {
        return service.today();
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('HR_MEMBER', 'SYSTEM_ADMIN')")
    public MoodDtos.MoodSummary summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date) {
        return service.summary(date);
    }
}
