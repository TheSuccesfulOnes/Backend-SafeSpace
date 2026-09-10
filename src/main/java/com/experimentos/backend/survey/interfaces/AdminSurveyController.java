package com.experimentos.backend.survey.interfaces;

import com.experimentos.backend.survey.application.AdminSurveyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/surveys")
public class AdminSurveyController {
    private final AdminSurveyService service;

    public AdminSurveyController(AdminSurveyService service) {
        this.service = service;
    }

    @GetMapping
    public List<SurveyAdminDtos.SurveyResponse> listAll() {
        return service.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SurveyAdminDtos.SurveyResponse create(
            @Valid @RequestBody SurveyAdminDtos.SurveyRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}/answers")
    public List<SurveyAdminDtos.AnswerResponse> listAnswers(@PathVariable Long id) {
        return service.listAnswers(id);
    }

    @PutMapping("/{id}")
    public SurveyAdminDtos.SurveyResponse update(
            @PathVariable Long id, @Valid @RequestBody SurveyAdminDtos.SurveyRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/publish")
    public SurveyAdminDtos.SurveyResponse publish(@PathVariable Long id) {
        return service.publish(id);
    }

    @PostMapping("/{id}/close")
    public SurveyAdminDtos.SurveyResponse close(@PathVariable Long id) {
        return service.close(id);
    }

    @PostMapping("/{id}/reopen")
    public SurveyAdminDtos.SurveyResponse reopen(@PathVariable Long id) {
        return service.reopen(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
