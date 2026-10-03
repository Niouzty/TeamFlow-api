package com.teamflow.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequestDto(
        @NotBlank
        @Size(max = 120)
        String name,

        @Size(max = 5000)
        String description
) {
}
