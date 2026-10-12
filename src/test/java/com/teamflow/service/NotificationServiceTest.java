package com.teamflow.service;

import com.teamflow.dto.notification.NotificationResponseDto;
import com.teamflow.entity.Notification;
import com.teamflow.entity.User;
import com.teamflow.entity.UserRole;
import com.teamflow.exception.NotificationNotFoundException;
import com.teamflow.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void listsOnlyTheAuthenticatedUsersNotifications() {
        User user = user(2L, "bob@example.com");
        Notification notification = new Notification("Assigned to task", "TASK_ASSIGNED", user);
        ReflectionTestUtils.setField(notification, "id", 4L);
        when(notificationRepository.findAllByUser_EmailOrderByCreatedAtDesc(user.getEmail()))
                .thenReturn(List.of(notification));

        List<NotificationResponseDto> result =
                notificationService.findAllForUser(user.getEmail());

        assertEquals(1, result.size());
        assertEquals(4L, result.get(0).id());
        assertEquals("TASK_ASSIGNED", result.get(0).type());
        assertFalse(result.get(0).isRead());
        verify(notificationRepository).findAllByUser_EmailOrderByCreatedAtDesc(user.getEmail());
    }

    @Test
    void marksTheAuthenticatedUsersNotificationAsRead() {
        User user = user(2L, "bob@example.com");
        Notification notification = new Notification("Assigned to task", "TASK_ASSIGNED", user);
        ReflectionTestUtils.setField(notification, "id", 4L);
        when(notificationRepository.findByIdAndUser_Email(4L, user.getEmail()))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponseDto result = notificationService.markAsRead(4L, user.getEmail());

        assertTrue(notification.isRead());
        assertTrue(result.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void cannotMarkAnotherUsersNotificationAsRead() {
        when(notificationRepository.findByIdAndUser_Email(4L, "bob@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                NotificationNotFoundException.class,
                () -> notificationService.markAsRead(4L, "bob@example.com")
        );
    }

    private User user(Long id, String email) {
        User user = new User("bob", email, "encoded-password", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
