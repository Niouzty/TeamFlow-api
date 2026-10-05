package com.teamflow.repository;

import com.teamflow.dto.dashboard.ProjectTaskCounts;
import com.teamflow.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByProject_IdOrderByCreatedAtDesc(Long projectId);

    Optional<Task> findByIdAndProject_Id(Long id, Long projectId);

    boolean existsByAssignedUser_IdAndProject_Id(Long userId, Long projectId);

    @Query("""
            select new com.teamflow.dto.dashboard.ProjectTaskCounts(
                p.id,
                count(t),
                count(case when t.status = com.teamflow.entity.TaskStatus.TODO then t.id end),
                count(case when t.status = com.teamflow.entity.TaskStatus.IN_PROGRESS then t.id end),
                count(case when t.status = com.teamflow.entity.TaskStatus.DONE then t.id end)
            )
            from Project p
            left join p.tasks t
            where p.id in :projectIds
            group by p.id
            """)
    List<ProjectTaskCounts> countTasksByProjectIds(@Param("projectIds") List<Long> projectIds);
}
