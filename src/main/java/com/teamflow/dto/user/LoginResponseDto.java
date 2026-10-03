package com.teamflow.dto.user;

public record LoginResponseDto(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponseDto user
) {
}
