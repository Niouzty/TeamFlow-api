package com.teamflow.repository;

import com.teamflow.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByOwner_EmailOrderByCreatedAtDesc(String ownerEmail);

    Optional<Project> findByIdAndOwner_Email(Long id, String ownerEmail);
}
