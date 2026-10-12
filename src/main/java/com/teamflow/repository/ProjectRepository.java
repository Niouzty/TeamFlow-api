package com.teamflow.repository;

import com.teamflow.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByOwner_EmailOrderByCreatedAtDesc(String ownerEmail);

    List<Project> findAllByOrderByCreatedAtDesc();

    Optional<Project> findByIdAndOwner_Email(Long id, String ownerEmail);

    @Query("""
            select distinct p
            from Project p
            left join p.members member
            where p.owner.email = :email or member.email = :email
            order by p.createdAt desc
            """)
    List<Project> findAllVisibleToUser(@Param("email") String email);
}
