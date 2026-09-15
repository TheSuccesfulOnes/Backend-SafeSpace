package com.experimentos.backend.activity.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class ActivityDtos {
    private ActivityDtos() {}

    public record CreateActivityRequest(
            @NotBlank @Size(max = 160) String title,
            @Size(max = 500) String description,
            @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 160) String> options) {}

    public record VoteRequest(@NotNull Long optionId) {}

    public record OptionResponse(Long id, String label, long votes, double percentage) {}

    public record ActivityResponse(
            Long id,
            String title,
            String description,
            String status,
            List<OptionResponse> options) {}
}
