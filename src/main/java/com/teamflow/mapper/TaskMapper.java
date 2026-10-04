package com.teamflow.mapper;

import com.teamflow.dto.task.TaskRequestDto;
import com.teamflow.dto.task.TaskResponseDto;
import com.teamflow.entity.Project;
import com.teamflow.entity.Task;
import com.teamflow.entity.TaskStatus;
import com.teamflow.entity.User;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public Task toEntity(TaskRequestDto request, Project project) {
        Task task = new Task(request.title(), request.description(), project);
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        return task;
    }

    public void updateEntity(Task task, TaskRequestDto request) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
    }

    public void updateStatus(Task task, TaskStatus status) {
        task.setStatus(status);
    }

    public void updateAssignee(Task task, User assignee) {
        task.setAssignedUser(assignee);
    }

    public TaskResponseDto toResponseDto(Task task) {
        User assignedUser = task.getAssignedUser();
        return new TaskResponseDto(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getProject().getId(),
                assignedUser == null ? null : assignedUser.getId(),
                assignedUser == null ? null : assignedUser.getUsername(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
