package com.teamflow.service;

import com.teamflow.dto.user.LoginRequestDto;
import com.teamflow.dto.user.LoginResponseDto;
import com.teamflow.dto.user.UserResponseDto;
import com.teamflow.entity.User;
import com.teamflow.entity.UserRole;
import com.teamflow.exception.InvalidCredentialsException;
import com.teamflow.mapper.UserMapper;
import com.teamflow.repository.UserRepository;
import com.teamflow.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginReturnsTokenWhenCredentialsAreValid() {
        LoginRequestDto request = new LoginRequestDto("alice@example.com", "Password123");
        User user = new User("alice", "alice@example.com", "encoded-password", UserRole.USER);
        UserResponseDto userResponse =
                new UserResponseDto(1L, "alice", "alice@example.com", UserRole.USER, null);
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
        when(jwtTokenService.generateToken(user)).thenReturn("signed-token");
        when(jwtTokenService.getTokenLifetimeSeconds()).thenReturn(900L);
        when(userMapper.toResponseDto(user)).thenReturn(userResponse);

        LoginResponseDto response = authService.login(request);

        assertEquals("signed-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());
        assertEquals(userResponse, response.user());
        verify(passwordEncoder).matches(request.password(), user.getPassword());
    }

    @Test
    void loginRejectsUnknownEmail() {
        LoginRequestDto request = new LoginRequestDto("unknown@example.com", "Password123");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));

        verify(passwordEncoder, never()).matches(request.password(), "encoded-password");
        verify(jwtTokenService, never()).generateToken(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void loginRejectsIncorrectPassword() {
        LoginRequestDto request = new LoginRequestDto("alice@example.com", "WrongPassword");
        User user = new User("alice", "alice@example.com", "encoded-password", UserRole.USER);
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));

        verify(jwtTokenService, never()).generateToken(user);
    }
}
