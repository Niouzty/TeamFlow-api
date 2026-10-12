package com.teamflow.config;

import com.teamflow.dto.project.AddProjectMemberRequestDto;
import com.teamflow.dto.project.ProjectMemberResponseDto;
import com.teamflow.dto.project.ProjectRequestDto;
import com.teamflow.dto.project.ProjectResponseDto;
import com.teamflow.dto.dashboard.DashboardResponseDto;
import com.teamflow.dto.dashboard.ProjectDashboardDto;
import com.teamflow.dto.notification.NotificationResponseDto;
import com.teamflow.dto.task.TaskRequestDto;
import com.teamflow.dto.task.TaskResponseDto;
import com.teamflow.dto.task.TaskStatusRequestDto;
import com.teamflow.dto.user.UserResponseDto;
import com.teamflow.dto.user.UpdateUserProfileRequestDto;
import com.teamflow.entity.TaskPriority;
import com.teamflow.entity.TaskStatus;
import com.teamflow.entity.UserRole;
import com.teamflow.service.AuthService;
import com.teamflow.service.DashboardService;
import com.teamflow.service.NotificationService;
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
@Import({SecurityConfig.class, OpenApiConfig.class})
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

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private NotificationService notificationService;

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

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardIsScopedToTheAuthenticatedUser() throws Exception {
        DashboardResponseDto response = new DashboardResponseDto(
                1,
                2,
                1,
                0,
                1,
                50.0,
                java.util.List.of(new ProjectDashboardDto(
                        5L, "TeamFlow", null, 2, 1, 0, 1, 50.0
                ))
        );
        when(dashboardService.getDashboard("alice@example.com")).thenReturn(response);

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + createToken("alice@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(1))
                .andExpect(jsonPath("$.totalTasks").value(2))
                .andExpect(jsonPath("$.projects[0].projectId").value(5))
                .andExpect(jsonPath("$.projects[0].progressPercentage").value(50.0));

        verify(dashboardService).getDashboard("alice@example.com");
    }

    @Test
    void adminUserRoutesRequireTheAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + createToken("alice@example.com", "USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListUsers() throws Exception {
        when(userService.findAllUsers()).thenReturn(java.util.List.of(
                new UserResponseDto(2L, "bob", "bob@example.com", UserRole.USER, null)
        ));

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + createToken("admin@example.com", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("bob@example.com"))
                .andExpect(jsonPath("$[0].role").value("USER"));
    }

    @Test
    void adminCanViewAndUpdateUserDetailsWithoutChangingTheRole() throws Exception {
        UserResponseDto response = new UserResponseDto(
                2L, "bob-updated", "bob@example.com", UserRole.USER, null
        );
        when(userService.findUserById(2L)).thenReturn(
                new UserResponseDto(2L, "bob", "bob@example.com", UserRole.USER, null)
        );
        when(userService.updateUserById(
                org.mockito.ArgumentMatchers.eq(2L),
                any(UpdateUserProfileRequestDto.class)
        )).thenReturn(response);
        String token = createToken("admin@example.com", "ADMIN");

        mockMvc.perform(get("/api/admin/users/2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));

        mockMvc.perform(patch("/api/admin/users/2")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"bob-updated","email":"bob@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob-updated"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void notificationRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/notifications/1/read"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void notificationRoutesUseTheAuthenticatedUser() throws Exception {
        NotificationResponseDto response = new NotificationResponseDto(
                4L, "You have been assigned to task \"Implement feature\".",
                "TASK_ASSIGNED", true, null
        );
        when(notificationService.findAllForUser("alice@example.com"))
                .thenReturn(java.util.List.of(response));
        when(notificationService.markAsRead(4L, "alice@example.com")).thenReturn(response);

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + createToken("alice@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("TASK_ASSIGNED"))
                .andExpect(jsonPath("$[0].isRead").value(true));

        mockMvc.perform(patch("/api/notifications/4/read")
                        .header("Authorization", "Bearer " + createToken("alice@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));

        verify(notificationService).findAllForUser("alice@example.com");
        verify(notificationService).markAsRead(4L, "alice@example.com");
    }

    private String createToken(String subject) {
        return createToken(subject, null);
    }

    private String createToken(String subject, String role) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60));
        if (role != null) {
            claimsBuilder.claim("role", role);
        }
        JwtClaimsSet claims = claimsBuilder.build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims
        )).getTokenValue();
    }
}
