package com.teamflow.mapper;

import com.teamflow.dto.project.ProjectRequestDto;
import com.teamflow.dto.project.ProjectResponseDto;
import com.teamflow.dto.project.ProjectMemberResponseDto;
import com.teamflow.entity.Project;
import com.teamflow.entity.User;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ProjectMapper {

    public Project toEntity(ProjectRequestDto request, User owner) {
        return new Project(request.name(), request.description(), owner);
    }

    public void updateEntity(Project project, ProjectRequestDto request) {
        project.setName(request.name());
        project.setDescription(request.description());
    }

    public ProjectResponseDto toResponseDto(Project project) {
        return new ProjectResponseDto(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt(),
                project.getOwner().getId(),
                project.getOwner().getUsername()
        );
    }

    public ProjectMemberResponseDto toMemberResponseDto(User user) {
        return new ProjectMemberResponseDto(user.getId(), user.getUsername());
    }

    public List<ProjectMemberResponseDto> toMemberResponseDtos(Project project) {
        return project.getMembers().stream()
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(this::toMemberResponseDto)
                .toList();
    }
}
