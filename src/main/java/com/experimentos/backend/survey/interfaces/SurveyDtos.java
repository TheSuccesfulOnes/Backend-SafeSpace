package com.experimentos.backend.survey.interfaces;

import com.experimentos.backend.survey.domain.SurveyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class SurveyDtos {
    private SurveyDtos() {}

    public record CreateSurveyRequest(
            @NotBlank String title,
            @NotBlank String question,
            @NotNull SurveyType type,
            boolean allowComments) {}

    public record AnswerRequest(@NotBlank String answerText) {}

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
