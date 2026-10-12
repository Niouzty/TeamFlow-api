package com.teamflow.dto.dashboard;

import java.util.List;

public record DashboardResponseDto(
        long totalProjects,
        long totalTasks,
        long todoTasks,
        long inProgressTasks,
        long doneTasks,
        double progressPercentage,
        List<ProjectDashboardDto> projects
) {
}
