package com.teamflow.controller;

import com.teamflow.dto.user.RegisterUserRequestDto;
import com.teamflow.dto.user.LoginRequestDto;
import com.teamflow.dto.user.LoginResponseDto;
import com.teamflow.dto.user.UserResponseDto;
import com.teamflow.entity.UserRole;
import com.teamflow.exception.InvalidCredentialsException;
import com.teamflow.exception.UserAlreadyExistsException;
import com.teamflow.service.AuthService;
import com.teamflow.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private final UserService userService = mock(UserService.class);
    private final AuthService authService = mock(AuthService.class);
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(userService, authService))
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void registerReturnsCreatedUserWithoutPassword() throws Exception {
        UserResponseDto response = new UserResponseDto(
                1L,
                "alice",
                "alice@example.com",
                UserRole.USER,
                LocalDateTime.parse("2026-10-03T00:00:00")
        );
        when(userService.register(any(RegisterUserRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "email": "alice@example.com",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registerRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "email": "not-an-email",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any(RegisterUserRequestDto.class));
    }

    @Test
    void registerReturnsConflictWhenUserAlreadyExists() throws Exception {
        when(userService.register(any(RegisterUserRequestDto.class)))
                .thenThrow(new UserAlreadyExistsException("An account with this email already exists."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "email": "alice@example.com",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void loginReturnsAccessTokenAndUser() throws Exception {
        UserResponseDto user = new UserResponseDto(
                1L,
                "alice",
                "alice@example.com",
                UserRole.USER,
                LocalDateTime.parse("2026-10-03T00:00:00")
        );
        when(authService.login(any(LoginRequestDto.class)))
                .thenReturn(new LoginResponseDto("signed-token", "Bearer", 900, user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "alice@example.com",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value("alice@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void loginRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequestDto.class));
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
        when(authService.login(any(LoginRequestDto.class)))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "alice@example.com",
                                  "password": "WrongPassword"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }
}
