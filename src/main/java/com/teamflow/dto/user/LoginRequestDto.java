package com.teamflow.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        String password
) {
}
