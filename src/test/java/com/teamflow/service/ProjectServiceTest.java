package com.teamflow.service;

import com.teamflow.dto.project.ProjectRequestDto;
import com.teamflow.dto.project.ProjectResponseDto;
import com.teamflow.entity.Project;
import com.teamflow.entity.User;
import com.teamflow.entity.UserRole;
import com.teamflow.exception.ProjectNotFoundException;
import com.teamflow.exception.UserNotFoundException;
import com.teamflow.mapper.ProjectMapper;
import com.teamflow.repository.ProjectRepository;
import com.teamflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createAssociatesProjectWithAuthenticatedOwner() {
        String ownerEmail = "alice@example.com";
        ProjectRequestDto request = new ProjectRequestDto("TeamFlow", "Project management");
        User owner = new User("alice", ownerEmail, "encoded-password", UserRole.USER);
        Project project = new Project(request.name(), request.description(), owner);
        ProjectResponseDto response = response(1L, request, owner);
        when(userRepository.findByEmail(ownerEmail)).thenReturn(Optional.of(owner));
        when(projectMapper.toEntity(request, owner)).thenReturn(project);
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toResponseDto(project)).thenReturn(response);

        ProjectResponseDto result = projectService.create(ownerEmail, request);

        assertEquals(response, result);
        verify(projectMapper).toEntity(request, owner);
        verify(projectRepository).save(project);
    }

    @Test
    void createFailsWhenAuthenticatedUserDoesNotExist() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> projectService.create(
                        "unknown@example.com",
                        new ProjectRequestDto("TeamFlow", null)
                )
        );

        verify(projectRepository, never()).save(org.mockito.ArgumentMatchers.any(Project.class));
    }

    @Test
    void findAllReturnsOnlyProjectsOwnedByAuthenticatedUser() {
        String ownerEmail = "alice@example.com";
        User owner = new User("alice", ownerEmail, "encoded-password", UserRole.USER);
        Project first = new Project("First", null, owner);
        Project second = new Project("Second", null, owner);
        when(projectRepository.findAllByOwner_EmailOrderByCreatedAtDesc(ownerEmail))
                .thenReturn(List.of(first, second));
        when(projectMapper.toResponseDto(first)).thenReturn(
                response(1L, new ProjectRequestDto("First", null), owner));
        when(projectMapper.toResponseDto(second)).thenReturn(
                response(2L, new ProjectRequestDto("Second", null), owner));

        List<ProjectResponseDto> results = projectService.findAllForOwner(ownerEmail);

        assertEquals(2, results.size());
        assertEquals("First", results.get(0).name());
        assertEquals("Second", results.get(1).name());
        verify(projectRepository).findAllByOwner_EmailOrderByCreatedAtDesc(ownerEmail);
    }

    @Test
    void findProjectHidesProjectsOwnedBySomeoneElse() {
        when(projectRepository.findByIdAndOwner_Email(9L, "alice@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ProjectNotFoundException.class,
                () -> projectService.findForOwner(9L, "alice@example.com")
        );
    }

    @Test
    void updateChangesOnlyTheOwnedProject() {
        String ownerEmail = "alice@example.com";
        User owner = new User("alice", ownerEmail, "encoded-password", UserRole.USER);
        Project project = new Project("Old name", "Old description", owner);
        ProjectRequestDto request = new ProjectRequestDto("New name", "New description");
        ProjectResponseDto response = response(1L, request, owner);
        when(projectRepository.findByIdAndOwner_Email(1L, ownerEmail))
                .thenReturn(Optional.of(project));
        doAnswer(invocation -> {
            Project target = invocation.getArgument(0);
            ProjectRequestDto update = invocation.getArgument(1);
            target.setName(update.name());
            target.setDescription(update.description());
            return null;
        }).when(projectMapper).updateEntity(project, request);
        when(projectRepository.save(project)).thenReturn(project);
        when(projectMapper.toResponseDto(project)).thenReturn(response);

        ProjectResponseDto result = projectService.update(1L, ownerEmail, request);

        assertEquals("New name", project.getName());
        assertEquals("New description", project.getDescription());
        assertEquals(owner, project.getOwner());
        assertEquals(response, result);
        verify(projectMapper).updateEntity(project, request);
    }

    @Test
    void deleteRemovesOnlyTheOwnedProject() {
        String ownerEmail = "alice@example.com";
        User owner = new User("alice", ownerEmail, "encoded-password", UserRole.USER);
        Project project = new Project("TeamFlow", null, owner);
        when(projectRepository.findByIdAndOwner_Email(1L, ownerEmail))
                .thenReturn(Optional.of(project));

        projectService.delete(1L, ownerEmail);

        verify(projectRepository).delete(project);
    }

    private ProjectResponseDto response(Long id, ProjectRequestDto request, User owner) {
        return new ProjectResponseDto(
                id,
                request.name(),
                request.description(),
                null,
                owner.getId(),
                owner.getUsername()
        );
    }
}
