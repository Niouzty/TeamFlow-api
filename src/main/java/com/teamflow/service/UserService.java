package com.teamflow.service;

import com.teamflow.dto.user.RegisterUserRequestDto;
import com.teamflow.dto.user.ChangePasswordRequestDto;
import com.teamflow.dto.user.UpdateUserProfileRequestDto;
import com.teamflow.dto.user.UserResponseDto;
import com.teamflow.entity.User;
import com.teamflow.exception.InvalidCurrentPasswordException;
import com.teamflow.exception.UserAlreadyExistsException;
import com.teamflow.exception.UserNotFoundException;
import com.teamflow.mapper.UserMapper;
import com.teamflow.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponseDto register(RegisterUserRequestDto request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("An account with this email already exists.");
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("This username is already taken.");
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = userMapper.toEntity(request, encodedPassword);
        User savedUser = userRepository.save(user);

        return userMapper.toResponseDto(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);
        return userMapper.toResponseDto(user);
    }

    @Transactional
    public UserResponseDto updateProfile(String email, UpdateUserProfileRequestDto request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        if (!user.getUsername().equals(request.username())
                && userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("This username is already taken.");
        }
        if (!user.getEmail().equals(request.email())
                && userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("An account with this email already exists.");
        }

        user.setUsername(request.username());
        user.setEmail(request.email());
        User savedUser = userRepository.save(user);
        return userMapper.toResponseDto(savedUser);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequestDto request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidCurrentPasswordException();
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
