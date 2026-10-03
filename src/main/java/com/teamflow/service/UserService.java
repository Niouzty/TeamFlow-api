package com.teamflow.service;

import com.teamflow.dto.user.RegisterUserRequestDto;
import com.teamflow.dto.user.UserResponseDto;
import com.teamflow.entity.User;
import com.teamflow.exception.UserAlreadyExistsException;
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
}
