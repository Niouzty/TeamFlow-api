package com.teamflow.repository;

import com.teamflow.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByProject_IdOrderByCreatedAtDesc(Long projectId);

    Optional<Task> findByIdAndProject_Id(Long id, Long projectId);

    boolean existsByAssignedUser_IdAndProject_Id(Long userId, Long projectId);
}
