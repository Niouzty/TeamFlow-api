package com.teamflow.repository;

import com.teamflow.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByUser_EmailOrderByCreatedAtDesc(String email);

    Optional<Notification> findByIdAndUser_Email(Long id, String email);
}
