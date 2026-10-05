package com.teamflow.dto.dashboard;

import java.time.LocalDateTime;

public record ProjectDashboardDto(
        Long projectId,
        String name,
        LocalDateTime createdAt,
        long totalTasks,
        long todoTasks,
        long inProgressTasks,
        long doneTasks,
        double progressPercentage
) {
}
