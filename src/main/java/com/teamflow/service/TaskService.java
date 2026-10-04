package com.teamflow.service;

import com.teamflow.dto.task.TaskAssignmentRequestDto;
import com.teamflow.dto.task.TaskRequestDto;
import com.teamflow.dto.task.TaskResponseDto;
import com.teamflow.dto.task.TaskStatusRequestDto;
import com.teamflow.entity.Project;
import com.teamflow.entity.Task;
import com.teamflow.entity.User;
import com.teamflow.exception.ProjectAccessDeniedException;
import com.teamflow.exception.ProjectNotFoundException;
import com.teamflow.exception.TaskNotFoundException;
import com.teamflow.exception.UserNotFoundException;
import com.teamflow.mapper.TaskMapper;
import com.teamflow.repository.ProjectRepository;
import com.teamflow.repository.TaskRepository;
import com.teamflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            TaskMapper taskMapper
    ) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.taskMapper = taskMapper;
    }

    @Transactional
    public TaskResponseDto create(Long projectId, String requesterEmail, TaskRequestDto request) {
        ProjectAccess access = getProjectAccess(projectId, requesterEmail);
        Task task = taskMapper.toEntity(request, access.project());
        return taskMapper.toResponseDto(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDto> findAll(Long projectId, String requesterEmail) {
        getProjectAccess(projectId, requesterEmail);
        return taskRepository.findAllByProject_IdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(taskMapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponseDto findById(Long projectId, Long taskId, String requesterEmail) {
        getProjectAccess(projectId, requesterEmail);
        return taskMapper.toResponseDto(findTask(projectId, taskId));
    }

    @Transactional
    public TaskResponseDto update(
            Long projectId,
            Long taskId,
            String requesterEmail,
            TaskRequestDto request
    ) {
        ProjectAccess access = getProjectAccess(projectId, requesterEmail);
        Task task = findTask(projectId, taskId);
        if (!access.owner() && !isAssignedTo(task, access.user())) {
            throw new ProjectAccessDeniedException();
        }

        taskMapper.updateEntity(task, request);
        return taskMapper.toResponseDto(taskRepository.save(task));
    }

    @Transactional
    public TaskResponseDto updateStatus(
            Long projectId,
            Long taskId,
            String requesterEmail,
            TaskStatusRequestDto request
    ) {
        getProjectAccess(projectId, requesterEmail);
        Task task = findTask(projectId, taskId);
        taskMapper.updateStatus(task, request.status());
        return taskMapper.toResponseDto(taskRepository.save(task));
    }

    @Transactional
    public TaskResponseDto assign(
            Long projectId,
            Long taskId,
            String requesterEmail,
            TaskAssignmentRequestDto request
    ) {
        ProjectAccess access = getProjectAccess(projectId, requesterEmail);
        Task task = findTask(projectId, taskId);
        User assignee = null;

        if (request.userId() != null) {
            assignee = userRepository.findById(request.userId())
                    .orElseThrow(UserNotFoundException::new);
            boolean isProjectMember = access.project().hasMember(assignee.getId());
            boolean isProjectOwner = access.project().getOwner().getId().equals(assignee.getId());
            if (!isProjectMember && !isProjectOwner) {
                throw new ProjectAccessDeniedException();
            }
        }

        if (!access.owner() && (assignee == null || !assignee.getId().equals(access.user().getId()))) {
            throw new ProjectAccessDeniedException();
        }

        taskMapper.updateAssignee(task, assignee);
        return taskMapper.toResponseDto(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long projectId, Long taskId, String requesterEmail) {
        ProjectAccess access = getProjectAccess(projectId, requesterEmail);
        if (!access.owner()) {
            throw new ProjectAccessDeniedException();
        }
        taskRepository.delete(findTask(projectId, taskId));
    }

    private ProjectAccess getProjectAccess(Long projectId, String requesterEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(ProjectNotFoundException::new);
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(UserNotFoundException::new);
        boolean owner = project.getOwner().getEmail().equals(requesterEmail);

        if (!owner && !project.hasMember(requester.getId())) {
            throw new ProjectAccessDeniedException();
        }

        return new ProjectAccess(project, requester, owner);
    }

    private Task findTask(Long projectId, Long taskId) {
        return taskRepository.findByIdAndProject_Id(taskId, projectId)
                .orElseThrow(TaskNotFoundException::new);
    }

    private boolean isAssignedTo(Task task, User user) {
        return task.getAssignedUser() != null
                && task.getAssignedUser().getId().equals(user.getId());
    }

    private record ProjectAccess(Project project, User user, boolean owner) {
    }
}
