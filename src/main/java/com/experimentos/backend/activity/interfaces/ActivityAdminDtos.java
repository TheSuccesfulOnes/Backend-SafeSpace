package com.experimentos.backend.activity.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class ActivityAdminDtos {
    private ActivityAdminDtos() {}

    public record ActivityRequest(
            @NotBlank @Size(max = 160) String title,
            @Size(max = 500) String description,
            @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 160) String> options) {}

    public record ActivityResponse(
            Long id,
            String title,
            String description,
            String status,
            List<ActivityDtos.OptionResponse> options,
            String createdBy,
            Instant createdAt) {}
}
