package com.teamflow.service;

import com.teamflow.dto.user.LoginRequestDto;
import com.teamflow.dto.user.LoginResponseDto;
import com.teamflow.entity.User;
import com.teamflow.exception.InvalidCredentialsException;
import com.teamflow.mapper.UserMapper;
import com.teamflow.repository.UserRepository;
import com.teamflow.security.JwtTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return new LoginResponseDto(
                jwtTokenService.generateToken(user),
                "Bearer",
                jwtTokenService.getTokenLifetimeSeconds(),
                userMapper.toResponseDto(user)
        );
    }
}
