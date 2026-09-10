package com.experimentos.backend.activity.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public final class ActivityDtos {
    private ActivityDtos() {}

    public record CreateActivityRequest(
            @NotBlank String title, String description, @NotEmpty List<@NotBlank String> options) {}

    public record VoteRequest(Long optionId) {}

    public record OptionResponse(Long id, String label, long votes, double percentage) {}

    public record ActivityResponse(
            Long id,
            String title,
            String description,
            String status,
            List<OptionResponse> options) {}
}
