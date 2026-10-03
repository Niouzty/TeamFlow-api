package com.teamflow.dto.project;

import java.time.LocalDateTime;

public record ProjectResponseDto(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        Long ownerId,
        String ownerUsername
) {
}
