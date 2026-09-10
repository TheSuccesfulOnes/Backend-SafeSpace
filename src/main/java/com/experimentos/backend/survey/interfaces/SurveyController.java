package com.experimentos.backend.survey.interfaces;

import com.experimentos.backend.survey.application.SurveyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/surveys")
public class SurveyController {
    private final SurveyService service;

    public SurveyController(SurveyService service) {
        this.service = service;
    }

    @GetMapping
    public List<SurveyDtos.SurveyResponse> published() {
        return service.published();
    }

    /** Returns every survey so HR can recover drafts and closed surveys after a reload. */
    @GetMapping("/managed")
    @PreAuthorize("hasAnyRole('HR_MEMBER', 'SYSTEM_ADMIN')")
    public List<SurveyDtos.SurveyResponse> managed() {
        return service.managed();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR_MEMBER', 'SYSTEM_ADMIN')")
    public SurveyDtos.SurveyResponse create(
            @Valid @RequestBody SurveyDtos.CreateSurveyRequest request) {
        return service.create(request);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('HR_MEMBER', 'SYSTEM_ADMIN')")
    public SurveyDtos.SurveyResponse publish(@PathVariable Long id) {
        return service.publish(id);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('HR_MEMBER', 'SYSTEM_ADMIN')")
    public SurveyDtos.SurveyResponse close(@PathVariable Long id) {
        return service.close(id);
    }

    @PostMapping("/{id}/answers")
    public void answer(
            @PathVariable Long id, @Valid @RequestBody SurveyDtos.AnswerRequest request) {
        service.answer(id, request);
    }
}
