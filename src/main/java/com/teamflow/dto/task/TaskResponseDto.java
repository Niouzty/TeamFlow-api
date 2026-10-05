package com.teamflow.dto.task;

import com.teamflow.entity.TaskPriority;
import com.teamflow.entity.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponseDto(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        Long projectId,
        Long assignedUserId,
        String assignedUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
