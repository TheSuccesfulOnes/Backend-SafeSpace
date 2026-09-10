package com.experimentos.backend.mood.interfaces;

import com.experimentos.backend.mood.domain.Mood;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Map;

public final class MoodDtos {
    private MoodDtos() {}

    public record SubmitMoodRequest(@NotNull Mood mood) {}

    public record MoodResponse(Mood mood, LocalDate date) {}

    public record MoodSummary(
            LocalDate date,
            long totalResponses,
            Map<Mood, Long> distribution,
            long activeEmployees,
            int responseRate) {}
}
