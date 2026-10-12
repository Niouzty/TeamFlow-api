package com.teamflow.dto.user;

import com.teamflow.entity.UserRole;

import java.time.LocalDateTime;

public record UserResponseDto(
        Long id,
        String username,
        String email,
        UserRole role,
        LocalDateTime createdAt
) {
}
