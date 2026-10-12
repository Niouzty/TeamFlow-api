package com.teamflow.dto.project;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddProjectMemberRequestDto(
        @NotBlank
        @Email
        @Size(max = 254)
        String email
) {
}
