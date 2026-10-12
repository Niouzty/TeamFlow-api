package com.teamflow.service;

import com.teamflow.dto.project.ProjectRequestDto;
import com.teamflow.dto.project.ProjectResponseDto;
import com.teamflow.dto.project.ProjectMemberResponseDto;
import com.teamflow.entity.Project;
import com.teamflow.entity.User;
import com.teamflow.entity.UserRole;
import com.teamflow.exception.ProjectMemberAlreadyExistsException;
import com.teamflow.exception.ProjectMemberHasAssignedTasksException;
import com.teamflow.exception.ProjectMemberNotFoundException;
import com.teamflow.exception.ProjectNotFoundException;
import com.teamflow.exception.UserNotFoundException;
import com.teamflow.mapper.ProjectMapper;
import com.teamflow.repository.ProjectRepository;
import com.teamflow.repository.TaskRepository;
import com.teamflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;

    public ProjectService(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            ProjectMapper projectMapper
    ) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
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
        User requester = findUser(ownerEmail);
        List<Project> projects = requester.getRole() == UserRole.ADMIN
                ? projectRepository.findAllByOrderByCreatedAtDesc()
                : projectRepository.findAllByOwner_EmailOrderByCreatedAtDesc(ownerEmail);
        return projects
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
        Project project = projectRepository.findByIdAndOwner_Email(projectId, ownerEmail)
                .orElseThrow(ProjectNotFoundException::new);
        projectRepository.delete(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponseDto> findMembers(Long projectId, String ownerEmail) {
        Project project = findProjectForOwner(projectId, ownerEmail);
        return projectMapper.toMemberResponseDtos(project);
    }

    @Transactional
    public ProjectMemberResponseDto addMember(
            Long projectId,
            String ownerEmail,
            String memberEmail
    ) {
        Project project = findProjectForOwner(projectId, ownerEmail);
        User member = userRepository.findByEmail(memberEmail)
                .orElseThrow(UserNotFoundException::new);

        if (project.hasMember(member.getId())) {
            throw new ProjectMemberAlreadyExistsException();
        }

        project.addMember(member);
        projectRepository.save(project);
        return projectMapper.toMemberResponseDto(member);
    }

    @Transactional
    public void removeMember(Long projectId, String ownerEmail, Long memberId) {
        Project project = findProjectForOwner(projectId, ownerEmail);
        if (!project.hasMember(memberId)) {
            throw new ProjectMemberNotFoundException();
        }
        if (taskRepository.existsByAssignedUser_IdAndProject_Id(memberId, projectId)) {
            throw new ProjectMemberHasAssignedTasksException();
        }

        project.removeMember(memberId);
        projectRepository.save(project);
    }

    private Project findProjectForOwner(Long projectId, String ownerEmail) {
        User requester = findUser(ownerEmail);
        if (requester.getRole() == UserRole.ADMIN) {
            return projectRepository.findById(projectId)
                    .orElseThrow(ProjectNotFoundException::new);
        }
        return projectRepository.findByIdAndOwner_Email(projectId, ownerEmail)
                .orElseThrow(ProjectNotFoundException::new);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);
    }
}
