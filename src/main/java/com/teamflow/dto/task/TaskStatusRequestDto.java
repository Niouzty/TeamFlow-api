package com.teamflow.dto.task;

import com.teamflow.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusRequestDto(
        @NotNull
        TaskStatus status
) {
}
