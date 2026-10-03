package com.teamflow.service;

import com.teamflow.dto.project.ProjectRequestDto;
import com.teamflow.dto.project.ProjectResponseDto;
import com.teamflow.entity.Project;
import com.teamflow.entity.User;
import com.teamflow.exception.ProjectNotFoundException;
import com.teamflow.exception.UserNotFoundException;
import com.teamflow.mapper.ProjectMapper;
import com.teamflow.repository.ProjectRepository;
import com.teamflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            ProjectMapper projectMapper
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectMapper = projectMapper;
    }

    @Transactional
    public ProjectResponseDto create(String ownerEmail, ProjectRequestDto request) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(UserNotFoundException::new);
        Project project = projectMapper.toEntity(request, owner);
        return projectMapper.toResponseDto(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDto> findAllForOwner(String ownerEmail) {
        return projectRepository.findAllByOwner_EmailOrderByCreatedAtDesc(ownerEmail)
                .stream()
                .map(projectMapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponseDto findForOwner(Long projectId, String ownerEmail) {
        return projectMapper.toResponseDto(findProjectForOwner(projectId, ownerEmail));
    }

    @Transactional
    public ProjectResponseDto update(
            Long projectId,
            String ownerEmail,
            ProjectRequestDto request
    ) {
        Project project = findProjectForOwner(projectId, ownerEmail);
        projectMapper.updateEntity(project, request);
        return projectMapper.toResponseDto(projectRepository.save(project));
    }

    @Transactional
    public void delete(Long projectId, String ownerEmail) {
        Project project = findProjectForOwner(projectId, ownerEmail);
        projectRepository.delete(project);
    }

    private Project findProjectForOwner(Long projectId, String ownerEmail) {
        return projectRepository.findByIdAndOwner_Email(projectId, ownerEmail)
                .orElseThrow(ProjectNotFoundException::new);
    }
}
