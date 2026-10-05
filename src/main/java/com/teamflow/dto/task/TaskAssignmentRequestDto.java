package com.teamflow.dto.task;

import jakarta.validation.constraints.Positive;

public record TaskAssignmentRequestDto(
        @Positive
        Long userId
) {
}
