package com.teamflow.service;

import com.teamflow.dto.dashboard.DashboardResponseDto;
import com.teamflow.dto.dashboard.ProjectTaskCounts;
import com.teamflow.entity.Project;
import com.teamflow.entity.User;
import com.teamflow.entity.UserRole;
import com.teamflow.exception.UserNotFoundException;
import com.teamflow.repository.ProjectRepository;
import com.teamflow.repository.TaskRepository;
import com.teamflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getDashboardAggregatesOwnedAndMemberProjectsAndTaskProgress() {
        String email = "alice@example.com";
        User user = new User("alice", email, "encoded-password", UserRole.USER);
        Project ownedProject = project(1L, "Owned project");
        Project memberProject = project(2L, "Member project");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(projectRepository.findAllVisibleToUser(email))
                .thenReturn(List.of(ownedProject, memberProject));
        when(taskRepository.countTasksByProjectIds(List.of(1L, 2L))).thenReturn(List.of(
                new ProjectTaskCounts(1L, 4L, 1L, 1L, 2L),
                new ProjectTaskCounts(2L, 2L, 1L, 0L, 1L)
        ));

        DashboardResponseDto dashboard = dashboardService.getDashboard(email);

        assertEquals(2, dashboard.totalProjects());
        assertEquals(6, dashboard.totalTasks());
        assertEquals(2, dashboard.todoTasks());
        assertEquals(1, dashboard.inProgressTasks());
        assertEquals(3, dashboard.doneTasks());
        assertEquals(50.0, dashboard.progressPercentage());
        assertEquals(2, dashboard.projects().size());
        assertEquals(50.0, dashboard.projects().get(0).progressPercentage());
        assertEquals(50.0, dashboard.projects().get(1).progressPercentage());
    }

    @Test
    void getDashboardReturnsZerosWhenUserHasNoProjects() {
        String email = "alice@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(
                new User("alice", email, "encoded-password", UserRole.USER)));
        when(projectRepository.findAllVisibleToUser(email)).thenReturn(List.of());

        DashboardResponseDto dashboard = dashboardService.getDashboard(email);

        assertEquals(0, dashboard.totalProjects());
        assertEquals(0, dashboard.totalTasks());
        assertEquals(0.0, dashboard.progressPercentage());
        assertEquals(List.of(), dashboard.projects());
        verify(taskRepository, never()).countTasksByProjectIds(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void getDashboardReturnsZeroProgressForProjectsWithoutTasks() {
        String email = "alice@example.com";
        Project project = project(1L, "Empty project");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(
                new User("alice", email, "encoded-password", UserRole.USER)));
        when(projectRepository.findAllVisibleToUser(email)).thenReturn(List.of(project));
        when(taskRepository.countTasksByProjectIds(List.of(1L)))
                .thenReturn(List.of(new ProjectTaskCounts(1L, 0L, 0L, 0L, 0L)));

        DashboardResponseDto dashboard = dashboardService.getDashboard(email);

        assertEquals(0.0, dashboard.progressPercentage());
        assertEquals(0.0, dashboard.projects().get(0).progressPercentage());
    }

    @Test
    void getDashboardFailsForUnknownUser() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> dashboardService.getDashboard("unknown@example.com")
        );

        verify(projectRepository, never()).findAllVisibleToUser("unknown@example.com");
    }

    private Project project(Long id, String name) {
        User owner = new User("owner", "owner@example.com", "encoded-password", UserRole.USER);
        Project project = new Project(name, null, owner);
        ReflectionTestUtils.setField(project, "id", id);
        ReflectionTestUtils.setField(project, "createdAt", LocalDateTime.now());
        return project;
    }
}
