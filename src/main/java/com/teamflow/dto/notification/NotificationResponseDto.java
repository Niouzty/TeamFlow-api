package com.teamflow.dto.notification;

import java.time.LocalDateTime;

public record NotificationResponseDto(
        Long id,
        String message,
        String type,
        boolean isRead,
        LocalDateTime createdAt
) {
}
