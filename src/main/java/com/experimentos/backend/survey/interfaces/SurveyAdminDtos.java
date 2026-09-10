package com.experimentos.backend.survey.interfaces;

import com.experimentos.backend.survey.domain.SurveyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class SurveyAdminDtos {
    private SurveyAdminDtos() {}

    public record SurveyRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 500) String question,
            @NotNull SurveyType type,
            boolean allowComments) {}

    public record SurveyResponse(
            Long id,
            String title,
            String question,
            SurveyType type,
            String status,
            boolean allowComments,
            long answers,
            String createdBy,
            Instant createdAt) {}

    public record AnswerResponse(
            Long id,
            Long surveyId,
            String username,
            String email,
            String displayName,
            String answerText,
            Instant createdAt) {}
}
