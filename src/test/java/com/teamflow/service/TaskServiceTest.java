package com.teamflow.service;

import com.teamflow.dto.task.TaskAssignmentRequestDto;
import com.teamflow.dto.task.TaskRequestDto;
import com.teamflow.dto.task.TaskResponseDto;
import com.teamflow.dto.task.TaskStatusRequestDto;
import com.teamflow.entity.Project;
import com.teamflow.entity.Task;
import com.teamflow.entity.TaskPriority;
import com.teamflow.entity.TaskStatus;
import com.teamflow.entity.User;
import com.teamflow.entity.UserRole;
import com.teamflow.exception.ProjectAccessDeniedException;
import com.teamflow.exception.ProjectNotFoundException;
import com.teamflow.exception.TaskNotFoundException;
import com.teamflow.exception.UserNotFoundException;
import com.teamflow.mapper.TaskMapper;
import com.teamflow.repository.ProjectRepository;
import com.teamflow.repository.TaskRepository;
import com.teamflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    @Test
    void projectMemberCanCreateTask() {
        User owner = user(1L, "alice", "alice@example.com");
        User member = user(2L, "bob", "bob@example.com");
        Project project = project(owner, member);
        TaskRequestDto request = request("Implement feature");
        Task task = new Task(request.title(), request.description(), project);
        TaskResponseDto response = response(task, 1L);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(member.getEmail())).thenReturn(Optional.of(member));
        when(taskMapper.toEntity(request, project)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponseDto(task)).thenReturn(response);

        TaskResponseDto result = taskService.create(5L, member.getEmail(), request);

        assertEquals(response, result);
        verify(taskRepository).save(task);
    }

    @Test
    void nonMemberCannotViewTasksInProject() {
        User owner = user(1L, "alice", "alice@example.com");
        User outsider = user(3L, "charlie", "charlie@example.com");
        Project project = project(owner);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(outsider.getEmail())).thenReturn(Optional.of(outsider));

        assertThrows(
                ProjectAccessDeniedException.class,
                () -> taskService.findAll(5L, outsider.getEmail())
        );

        verify(taskRepository, never()).findAllByProject_IdOrderByCreatedAtDesc(5L);
    }

    @Test
    void missingProjectCannotExposeItsTasks() {
        when(projectRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(
                ProjectNotFoundException.class,
                () -> taskService.findAll(5L, "alice@example.com")
        );

        verify(userRepository, never()).findByEmail("alice@example.com");
    }

    @Test
    void projectMemberCanEditTaskAssignedToThem() {
        User owner = user(1L, "alice", "alice@example.com");
        User member = user(2L, "bob", "bob@example.com");
        Project project = project(owner, member);
        Task task = new Task("Old title", null, project);
        task.setAssignedUser(member);
        TaskRequestDto request = request("New title");
        TaskResponseDto response = response(task, 1L);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(member.getEmail())).thenReturn(Optional.of(member));
        when(taskRepository.findByIdAndProject_Id(8L, 5L)).thenReturn(Optional.of(task));
        doAnswer(invocation -> {
            Task target = invocation.getArgument(0);
            TaskRequestDto update = invocation.getArgument(1);
            target.setTitle(update.title());
            target.setDescription(update.description());
            target.setPriority(update.priority());
            target.setDueDate(update.dueDate());
            return null;
        }).when(taskMapper).updateEntity(task, request);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponseDto(task)).thenReturn(response);

        TaskResponseDto result = taskService.update(5L, 8L, member.getEmail(), request);

        assertEquals("New title", task.getTitle());
        assertEquals(response, result);
    }

    @Test
    void projectMemberCannotEditTaskAssignedToSomeoneElse() {
        User owner = user(1L, "alice", "alice@example.com");
        User member = user(2L, "bob", "bob@example.com");
        User otherMember = user(3L, "charlie", "charlie@example.com");
        Project project = project(owner, member, otherMember);
        Task task = new Task("Task", null, project);
        task.setAssignedUser(otherMember);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(member.getEmail())).thenReturn(Optional.of(member));
        when(taskRepository.findByIdAndProject_Id(8L, 5L)).thenReturn(Optional.of(task));

        assertThrows(
                ProjectAccessDeniedException.class,
                () -> taskService.update(5L, 8L, member.getEmail(), request("Changed"))
        );

        verify(taskRepository, never()).save(task);
    }

    @Test
    void anyProjectMemberCanChangeTaskStatus() {
        User owner = user(1L, "alice", "alice@example.com");
        User member = user(2L, "bob", "bob@example.com");
        Project project = project(owner, member);
        Task task = new Task("Task", null, project);
        TaskStatusRequestDto request = new TaskStatusRequestDto(TaskStatus.IN_PROGRESS);
        TaskResponseDto response = response(task, 1L);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(member.getEmail())).thenReturn(Optional.of(member));
        when(taskRepository.findByIdAndProject_Id(8L, 5L)).thenReturn(Optional.of(task));
        doAnswer(invocation -> {
            Task target = invocation.getArgument(0);
            target.setStatus(invocation.getArgument(1));
            return null;
        }).when(taskMapper).updateStatus(task, request.status());
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponseDto(task)).thenReturn(response);

        taskService.updateStatus(5L, 8L, member.getEmail(), request);

        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
    }

    @Test
    void projectMemberCanAssignTaskOnlyToThemself() {
        User owner = user(1L, "alice", "alice@example.com");
        User member = user(2L, "bob", "bob@example.com");
        Project project = project(owner, member);
        Task task = new Task("Task", null, project);
        TaskAssignmentRequestDto request = new TaskAssignmentRequestDto(member.getId());
        TaskResponseDto response = response(task, 1L);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(member.getEmail())).thenReturn(Optional.of(member));
        when(taskRepository.findByIdAndProject_Id(8L, 5L)).thenReturn(Optional.of(task));
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        doAnswer(invocation -> {
            Task target = invocation.getArgument(0);
            target.setAssignedUser(invocation.getArgument(1));
            return null;
        }).when(taskMapper).updateAssignee(task, member);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponseDto(task)).thenReturn(response);

        taskService.assign(5L, 8L, member.getEmail(), request);

        assertEquals(member, task.getAssignedUser());
    }

    @Test
    void projectMemberCannotAssignTaskToAnotherMember() {
        User owner = user(1L, "alice", "alice@example.com");
        User member = user(2L, "bob", "bob@example.com");
        User otherMember = user(3L, "charlie", "charlie@example.com");
        Project project = project(owner, member, otherMember);
        Task task = new Task("Task", null, project);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(member.getEmail())).thenReturn(Optional.of(member));
        when(taskRepository.findByIdAndProject_Id(8L, 5L)).thenReturn(Optional.of(task));
        when(userRepository.findById(otherMember.getId())).thenReturn(Optional.of(otherMember));

        assertThrows(
                ProjectAccessDeniedException.class,
                () -> taskService.assign(
                        5L,
                        8L,
                        member.getEmail(),
                        new TaskAssignmentRequestDto(otherMember.getId())
                )
        );
    }

    @Test
    void ownerCanAssignTaskToProjectMember() {
        User owner = user(1L, "alice", "alice@example.com");
        User member = user(2L, "bob", "bob@example.com");
        Project project = project(owner, member);
        Task task = new Task("Task", null, project);
        TaskResponseDto response = response(task, 1L);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(taskRepository.findByIdAndProject_Id(8L, 5L)).thenReturn(Optional.of(task));
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        doAnswer(invocation -> {
            task.setAssignedUser(invocation.getArgument(1));
            return null;
        }).when(taskMapper).updateAssignee(task, member);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponseDto(task)).thenReturn(response);

        taskService.assign(
                5L,
                8L,
                owner.getEmail(),
                new TaskAssignmentRequestDto(member.getId())
        );

        assertEquals(member, task.getAssignedUser());
    }

    @Test
    void ownerCanUnassignTask() {
        User owner = user(1L, "alice", "alice@example.com");
        User member = user(2L, "bob", "bob@example.com");
        Project project = project(owner, member);
        Task task = new Task("Task", null, project);
        task.setAssignedUser(member);
        TaskResponseDto response = response(task, 1L);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(taskRepository.findByIdAndProject_Id(8L, 5L)).thenReturn(Optional.of(task));
        doAnswer(invocation -> {
            task.setAssignedUser(invocation.getArgument(1));
            return null;
        }).when(taskMapper).updateAssignee(task, null);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponseDto(task)).thenReturn(response);

        taskService.assign(5L, 8L, owner.getEmail(), new TaskAssignmentRequestDto(null));

        assertNull(task.getAssignedUser());
    }

    @Test
    void projectMemberCannotDeleteTask() {
        User owner = user(1L, "alice", "alice@example.com");
        User member = user(2L, "bob", "bob@example.com");
        Project project = project(owner, member);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(member.getEmail())).thenReturn(Optional.of(member));

        assertThrows(
                ProjectAccessDeniedException.class,
                () -> taskService.delete(5L, 8L, member.getEmail())
        );

        verify(taskRepository, never()).delete(org.mockito.ArgumentMatchers.any(Task.class));
    }

    @Test
    void ownerCanDeleteTask() {
        User owner = user(1L, "alice", "alice@example.com");
        Project project = project(owner);
        Task task = new Task("Task", null, project);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(taskRepository.findByIdAndProject_Id(8L, 5L)).thenReturn(Optional.of(task));

        taskService.delete(5L, 8L, owner.getEmail());

        verify(taskRepository).delete(task);
    }

    @Test
    void taskFromDifferentProjectIsNotFound() {
        User owner = user(1L, "alice", "alice@example.com");
        Project project = project(owner);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(taskRepository.findByIdAndProject_Id(8L, 5L)).thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> taskService.findById(5L, 8L, owner.getEmail())
        );
    }

    @Test
    void assignmentRequiresAnExistingUser() {
        User owner = user(1L, "alice", "alice@example.com");
        Project project = project(owner);
        Task task = new Task("Task", null, project);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(taskRepository.findByIdAndProject_Id(8L, 5L)).thenReturn(Optional.of(task));
        when(userRepository.findById(44L)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> taskService.assign(
                        5L,
                        8L,
                        owner.getEmail(),
                        new TaskAssignmentRequestDto(44L)
                )
        );
    }

    private Project project(User owner, User... members) {
        Project project = new Project("TeamFlow", null, owner);
        for (User member : members) {
            project.addMember(member);
        }
        ReflectionTestUtils.setField(project, "id", 5L);
        return project;
    }

    private User user(Long id, String username, String email) {
        User user = new User(username, email, "encoded-password", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private TaskRequestDto request(String title) {
        return new TaskRequestDto(title, "Description", TaskPriority.MEDIUM, LocalDate.now());
    }

    private TaskResponseDto response(Task task, Long projectId) {
        User assignee = task.getAssignedUser();
        return new TaskResponseDto(
                8L,
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                projectId,
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getUsername(),
                null,
                null
        );
    }
}
