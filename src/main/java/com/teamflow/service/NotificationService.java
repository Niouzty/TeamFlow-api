package com.teamflow.service;

import com.teamflow.dto.notification.NotificationResponseDto;
import com.teamflow.entity.Notification;
import com.teamflow.exception.NotificationNotFoundException;
import com.teamflow.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> findAllForUser(String email) {
        return notificationRepository.findAllByUser_EmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public NotificationResponseDto markAsRead(Long notificationId, String email) {
        Notification notification = notificationRepository.findByIdAndUser_Email(notificationId, email)
                .orElseThrow(NotificationNotFoundException::new);
        notification.setRead(true);
        return toResponseDto(notificationRepository.save(notification));
    }

    private NotificationResponseDto toResponseDto(Notification notification) {
        return new NotificationResponseDto(
                notification.getId(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
