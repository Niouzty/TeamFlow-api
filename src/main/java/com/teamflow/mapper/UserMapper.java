package com.teamflow.mapper;

import com.teamflow.dto.user.RegisterUserRequestDto;
import com.teamflow.dto.user.UserResponseDto;
import com.teamflow.entity.User;
import com.teamflow.entity.UserRole;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(RegisterUserRequestDto request, String encodedPassword) {
        return new User(
                request.username(),
                request.email(),
                encodedPassword,
                UserRole.USER
        );
    }

    public UserResponseDto toResponseDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
