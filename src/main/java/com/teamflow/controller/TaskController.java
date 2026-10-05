package com.teamflow.controller;

import com.teamflow.dto.task.TaskAssignmentRequestDto;
import com.teamflow.dto.task.TaskRequestDto;
import com.teamflow.dto.task.TaskResponseDto;
import com.teamflow.dto.task.TaskStatusRequestDto;
import com.teamflow.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponseDto> create(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId,
            @Valid @RequestBody TaskRequestDto request
    ) {
        TaskResponseDto response = taskService.create(projectId, jwt.getSubject(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{taskId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponseDto>> findAll(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(taskService.findAll(projectId, jwt.getSubject()));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponseDto> findById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId,
            @PathVariable Long taskId
    ) {
        return ResponseEntity.ok(taskService.findById(projectId, taskId, jwt.getSubject()));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponseDto> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequestDto request
    ) {
        return ResponseEntity.ok(taskService.update(projectId, taskId, jwt.getSubject(), request));
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<TaskResponseDto> updateStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskStatusRequestDto request
    ) {
        return ResponseEntity.ok(taskService.updateStatus(projectId, taskId, jwt.getSubject(), request));
    }

    @PatchMapping("/{taskId}/assignee")
    public ResponseEntity<TaskResponseDto> assign(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskAssignmentRequestDto request
    ) {
        return ResponseEntity.ok(taskService.assign(projectId, taskId, jwt.getSubject(), request));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId,
            @PathVariable Long taskId
    ) {
        taskService.delete(projectId, taskId, jwt.getSubject());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
