package com.teamflow.dto.dashboard;

public record ProjectTaskCounts(
        Long projectId,
        Long totalTasks,
        Long todoTasks,
        Long inProgressTasks,
        Long doneTasks
) {
}
