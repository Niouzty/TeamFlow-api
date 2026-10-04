package com.teamflow.config;

import com.teamflow.dto.project.AddProjectMemberRequestDto;
import com.teamflow.dto.project.ProjectMemberResponseDto;
import com.teamflow.dto.project.ProjectRequestDto;
import com.teamflow.dto.project.ProjectResponseDto;
import com.teamflow.dto.task.TaskRequestDto;
import com.teamflow.dto.task.TaskResponseDto;
import com.teamflow.dto.task.TaskStatusRequestDto;
import com.teamflow.dto.user.UserResponseDto;
import com.teamflow.entity.TaskPriority;
import com.teamflow.entity.TaskStatus;
import com.teamflow.entity.UserRole;
import com.teamflow.service.AuthService;
import com.teamflow.service.ProjectService;
import com.teamflow.service.TaskService;
import com.teamflow.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private TaskService taskService;

    @Test
    void protectedRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/private"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRouteIsPublic() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void profileRouteUsesTheEmailFromTheAuthenticatedToken() throws Exception {
        UserResponseDto response =
                new UserResponseDto(1L, "alice", "alice@example.com", UserRole.USER, null);
        when(userService.getProfile("alice@example.com")).thenReturn(response);

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + createToken("alice@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(userService).getProfile("alice@example.com");
    }

    @Test
    void profileUpdateRouteIsProtectedAndReturnsTheUpdatedProfile() throws Exception {
        UserResponseDto response =
                new UserResponseDto(1L, "alice-updated", "alice@example.com", UserRole.USER, null);
        when(userService.updateProfile(any(), any())).thenReturn(response);

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + createToken("alice@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice-updated","email":"alice@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice-updated"));
    }

    @Test
    void passwordChangeRouteRequiresAuthentication() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"OldPassword123","newPassword":"NewPassword123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void projectRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"TeamFlow"}
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"TeamFlow"}
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/projects/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/projects/1/members"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/projects/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bob@example.com"}
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/projects/1/members/2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void projectCreationUsesTheAuthenticatedUserAsOwner() throws Exception {
        ProjectResponseDto response =
                new ProjectResponseDto(5L, "TeamFlow", "Description", null, 2L, "alice");
        when(projectService.create(
                org.mockito.ArgumentMatchers.eq("alice@example.com"),
                any(ProjectRequestDto.class)
        )).thenReturn(response);

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + createToken("alice@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"TeamFlow","description":"Description"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.ownerUsername").value("alice"));
    }

    @Test
    void memberRoutesUseTheAuthenticatedOwner() throws Exception {
        ProjectMemberResponseDto member = new ProjectMemberResponseDto(2L, "bob");
        when(projectService.addMember(
                org.mockito.ArgumentMatchers.eq(5L),
                org.mockito.ArgumentMatchers.eq("alice@example.com"),
                org.mockito.ArgumentMatchers.eq("bob@example.com")
        )).thenReturn(member);
        when(projectService.findMembers(5L, "alice@example.com"))
                .thenReturn(java.util.List.of(member));

        mockMvc.perform(post("/api/projects/5/members")
                        .header("Authorization", "Bearer " + createToken("alice@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bob@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("bob"));

        mockMvc.perform(get("/api/projects/5/members")
                        .header("Authorization", "Bearer " + createToken("alice@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("bob"));

        verify(projectService).addMember(5L, "alice@example.com", "bob@example.com");
        verify(projectService).findMembers(5L, "alice@example.com");
    }

    @Test
    void removingMemberUsesTheAuthenticatedOwner() throws Exception {
        mockMvc.perform(delete("/api/projects/5/members/2")
                        .header("Authorization", "Bearer " + createToken("alice@example.com")))
                .andExpect(status().isNoContent());

        verify(projectService).removeMember(5L, "alice@example.com", 2L);
    }

    @Test
    void taskRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/projects/5/tasks"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/projects/5/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Implement task","priority":"MEDIUM"}
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/projects/5/tasks/8/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DONE"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void taskCreationUsesTheAuthenticatedMember() throws Exception {
        TaskResponseDto response = new TaskResponseDto(
                8L,
                "Implement task",
                "Description",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                null,
                5L,
                null,
                null,
                null,
                null
        );
        when(taskService.create(
                org.mockito.ArgumentMatchers.eq(5L),
                org.mockito.ArgumentMatchers.eq("bob@example.com"),
                any(TaskRequestDto.class)
        )).thenReturn(response);

        mockMvc.perform(post("/api/projects/5/tasks")
                        .header("Authorization", "Bearer " + createToken("bob@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Implement task","description":"Description","priority":"MEDIUM"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void taskStatusRouteUsesTheAuthenticatedMember() throws Exception {
        TaskResponseDto response = new TaskResponseDto(
                8L,
                "Implement task",
                "Description",
                TaskStatus.DONE,
                TaskPriority.MEDIUM,
                null,
                5L,
                null,
                null,
                null,
                null
        );
        when(taskService.updateStatus(
                org.mockito.ArgumentMatchers.eq(5L),
                org.mockito.ArgumentMatchers.eq(8L),
                org.mockito.ArgumentMatchers.eq("bob@example.com"),
                any(TaskStatusRequestDto.class)
        )).thenReturn(response);

        mockMvc.perform(patch("/api/projects/5/tasks/8/status")
                        .header("Authorization", "Bearer " + createToken("bob@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DONE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    private String createToken(String subject) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
        )).getTokenValue();
    }
}
