package com.teamflow.controller;

import com.teamflow.dto.project.ProjectRequestDto;
import com.teamflow.dto.project.ProjectResponseDto;
import com.teamflow.dto.project.AddProjectMemberRequestDto;
import com.teamflow.dto.project.ProjectMemberResponseDto;
import com.teamflow.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponseDto> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProjectRequestDto request
    ) {
        ProjectResponseDto response = projectService.create(jwt.getSubject(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponseDto>> findAll(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(projectService.findAllForOwner(jwt.getSubject()));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponseDto> findById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(projectService.findForOwner(projectId, jwt.getSubject()));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponseDto> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectRequestDto request
    ) {
        return ResponseEntity.ok(projectService.update(projectId, jwt.getSubject(), request));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId
    ) {
        projectService.delete(projectId, jwt.getSubject());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectMemberResponseDto>> findMembers(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(projectService.findMembers(projectId, jwt.getSubject()));
    }

    @PostMapping("/{projectId}/members")
    public ResponseEntity<ProjectMemberResponseDto> addMember(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId,
            @Valid @RequestBody AddProjectMemberRequestDto request
    ) {
        ProjectMemberResponseDto response = projectService.addMember(projectId, jwt.getSubject(), request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{projectId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long projectId,
            @PathVariable Long memberId
    ) {
        projectService.removeMember(projectId, jwt.getSubject(), memberId);
        return ResponseEntity.noContent().build();
    }
}
