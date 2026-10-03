package com.teamflow.service;

import com.teamflow.dto.user.RegisterUserRequestDto;
import com.teamflow.dto.user.UserResponseDto;
import com.teamflow.entity.User;
import com.teamflow.entity.UserRole;
import com.teamflow.exception.UserAlreadyExistsException;
import com.teamflow.mapper.UserMapper;
import com.teamflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerSavesUserWithEncodedPasswordAndReturnsResponse() {
        RegisterUserRequestDto request =
                new RegisterUserRequestDto("alice", "alice@example.com", "Password123");
        User user = new User("alice", "alice@example.com", "encoded-password", UserRole.USER);
        UserResponseDto expectedResponse =
                new UserResponseDto(1L, "alice", "alice@example.com", UserRole.USER, null);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userMapper.toEntity(request, "encoded-password")).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponseDto(user)).thenReturn(expectedResponse);

        UserResponseDto response = userService.register(request);

        assertEquals(expectedResponse, response);
        verify(userRepository).save(user);
        verify(passwordEncoder).encode(request.password());
    }

    @Test
    void registerRejectsAnAlreadyUsedEmail() {
        RegisterUserRequestDto request =
                new RegisterUserRequestDto("alice", "alice@example.com", "Password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.register(request));

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void registerRejectsAnAlreadyUsedUsername() {
        RegisterUserRequestDto request =
                new RegisterUserRequestDto("alice", "alice@example.com", "Password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByUsername(request.username())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.register(request));

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
    }
}
