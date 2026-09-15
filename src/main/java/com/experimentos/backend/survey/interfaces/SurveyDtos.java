package com.experimentos.backend.survey.interfaces;

import com.experimentos.backend.survey.domain.SurveyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class SurveyDtos {
    private SurveyDtos() {}

    public record CreateSurveyRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 500) String question,
            @NotNull SurveyType type,
            boolean allowComments) {}

    public record AnswerRequest(@NotBlank @Size(max = 1000) String answerText) {}

    public record SurveyResponse(
            Long id,
            String title,
            String question,
            SurveyType type,
            String status,
            boolean allowComments,
            long answers,
            boolean answered) {}
}
