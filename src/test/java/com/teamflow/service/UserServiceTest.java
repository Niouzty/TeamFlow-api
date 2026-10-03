package com.teamflow.service;

import com.teamflow.dto.user.RegisterUserRequestDto;
import com.teamflow.dto.user.ChangePasswordRequestDto;
import com.teamflow.dto.user.UpdateUserProfileRequestDto;
import com.teamflow.dto.user.UserResponseDto;
import com.teamflow.entity.User;
import com.teamflow.entity.UserRole;
import com.teamflow.exception.InvalidCurrentPasswordException;
import com.teamflow.exception.UserAlreadyExistsException;
import com.teamflow.exception.UserNotFoundException;
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

    @Test
    void getProfileReturnsTheAuthenticatedUsersProfile() {
        User user = new User("alice", "alice@example.com", "encoded-password", UserRole.USER);
        UserResponseDto expectedResponse =
                new UserResponseDto(1L, "alice", "alice@example.com", UserRole.USER, null);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(java.util.Optional.of(user));
        when(userMapper.toResponseDto(user)).thenReturn(expectedResponse);

        UserResponseDto response = userService.getProfile("alice@example.com");

        assertEquals(expectedResponse, response);
    }

    @Test
    void getProfileFailsWhenAuthenticatedUserNoLongerExists() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(java.util.Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getProfile("alice@example.com"));
    }

    @Test
    void updateProfileChangesUsernameAndEmailWithoutChangingRole() {
        User user = new User("alice", "alice@example.com", "encoded-password", UserRole.ADMIN);
        UpdateUserProfileRequestDto request =
                new UpdateUserProfileRequestDto("alice-new", "alice-new@example.com");
        UserResponseDto expectedResponse =
                new UserResponseDto(1L, "alice-new", "alice-new@example.com", UserRole.ADMIN, null);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(java.util.Optional.of(user));
        when(userRepository.existsByUsername("alice-new")).thenReturn(false);
        when(userRepository.existsByEmail("alice-new@example.com")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponseDto(user)).thenReturn(expectedResponse);

        UserResponseDto response = userService.updateProfile("alice@example.com", request);

        assertEquals("alice-new", user.getUsername());
        assertEquals("alice-new@example.com", user.getEmail());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertEquals(expectedResponse, response);
        verify(userRepository).save(user);
    }

    @Test
    void updateProfileRejectsAnAlreadyUsedUsername() {
        User user = new User("alice", "alice@example.com", "encoded-password", UserRole.USER);
        UpdateUserProfileRequestDto request =
                new UpdateUserProfileRequestDto("bob", "alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(java.util.Optional.of(user));
        when(userRepository.existsByUsername("bob")).thenReturn(true);

        assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.updateProfile("alice@example.com", request)
        );

        assertEquals("alice", user.getUsername());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void updateProfileRejectsAnAlreadyUsedEmail() {
        User user = new User("alice", "alice@example.com", "encoded-password", UserRole.USER);
        UpdateUserProfileRequestDto request =
                new UpdateUserProfileRequestDto("alice", "bob@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(java.util.Optional.of(user));
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.updateProfile("alice@example.com", request)
        );

        assertEquals("alice@example.com", user.getEmail());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void changePasswordVerifiesAndEncodesTheNewPassword() {
        User user = new User("alice", "alice@example.com", "old-hash", UserRole.USER);
        ChangePasswordRequestDto request =
                new ChangePasswordRequestDto("OldPassword123", "NewPassword123");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("OldPassword123", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123")).thenReturn("new-hash");

        userService.changePassword("alice@example.com", request);

        assertEquals("new-hash", user.getPassword());
        verify(userRepository).save(user);
        verify(passwordEncoder).encode("NewPassword123");
    }

    @Test
    void changePasswordRejectsAnIncorrectCurrentPassword() {
        User user = new User("alice", "alice@example.com", "old-hash", UserRole.USER);
        ChangePasswordRequestDto request =
                new ChangePasswordRequestDto("WrongPassword", "NewPassword123");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "old-hash")).thenReturn(false);

        assertThrows(
                InvalidCurrentPasswordException.class,
                () -> userService.changePassword("alice@example.com", request)
        );

        assertEquals("old-hash", user.getPassword());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
        verify(passwordEncoder, never()).encode("NewPassword123");
    }
}
