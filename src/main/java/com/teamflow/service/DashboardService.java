package com.teamflow.service;

import com.teamflow.dto.dashboard.DashboardResponseDto;
import com.teamflow.dto.dashboard.ProjectDashboardDto;
import com.teamflow.dto.dashboard.ProjectTaskCounts;
import com.teamflow.entity.Project;
import com.teamflow.entity.User;
import com.teamflow.entity.UserRole;
import com.teamflow.exception.UserNotFoundException;
import com.teamflow.repository.ProjectRepository;
import com.teamflow.repository.TaskRepository;
import com.teamflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public DashboardService(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            UserRepository userRepository
    ) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponseDto getDashboard(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(UserNotFoundException::new);

        List<Project> projects = user.getRole() == UserRole.ADMIN
                ? projectRepository.findAllByOrderByCreatedAtDesc()
                : projectRepository.findAllVisibleToUser(userEmail);
        if (projects.isEmpty()) {
            return new DashboardResponseDto(0, 0, 0, 0, 0, 0, List.of());
        }

        List<Long> projectIds = projects.stream()
                .map(Project::getId)
                .toList();
        Map<Long, ProjectTaskCounts> countsByProjectId = new HashMap<>();
        for (ProjectTaskCounts counts : taskRepository.countTasksByProjectIds(projectIds)) {
            countsByProjectId.put(counts.projectId(), counts);
        }

        long totalTasks = 0;
        long todoTasks = 0;
        long inProgressTasks = 0;
        long doneTasks = 0;
        List<ProjectDashboardDto> projectSummaries = new java.util.ArrayList<>();

        for (Project project : projects) {
            ProjectTaskCounts counts = countsByProjectId.get(project.getId());
            long projectTotal = counts == null ? 0 : counts.totalTasks();
            long projectTodo = counts == null ? 0 : counts.todoTasks();
            long projectInProgress = counts == null ? 0 : counts.inProgressTasks();
            long projectDone = counts == null ? 0 : counts.doneTasks();

            totalTasks += projectTotal;
            todoTasks += projectTodo;
            inProgressTasks += projectInProgress;
            doneTasks += projectDone;

            projectSummaries.add(new ProjectDashboardDto(
                    project.getId(),
                    project.getName(),
                    project.getCreatedAt(),
                    projectTotal,
                    projectTodo,
                    projectInProgress,
                    projectDone,
                    calculateProgress(projectDone, projectTotal)
            ));
        }

        return new DashboardResponseDto(
                projects.size(),
                totalTasks,
                todoTasks,
                inProgressTasks,
                doneTasks,
                calculateProgress(doneTasks, totalTasks),
                List.copyOf(projectSummaries)
        );
    }

    private double calculateProgress(long doneTasks, long totalTasks) {
        if (totalTasks == 0) {
            return 0;
        }
        return Math.round((double) doneTasks * 100 / totalTasks * 100.0) / 100.0;
    }
}
