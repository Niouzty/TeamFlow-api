package com.teamflow.repository;

import com.teamflow.dto.dashboard.ProjectTaskCounts;
import com.teamflow.entity.Project;
import com.teamflow.entity.Task;
import com.teamflow.entity.TaskStatus;
import com.teamflow.entity.User;
import com.teamflow.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dashboard-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DashboardRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void dashboardQueriesIncludeOwnedAndMemberProjectsAndAggregateTaskStatuses() {
        User owner = userRepository.save(new User(
                "owner", "owner@example.com", "encoded-password", UserRole.USER));
        User member = userRepository.save(new User(
                "member", "member@example.com", "encoded-password", UserRole.USER));
        User outsider = userRepository.save(new User(
                "outsider", "outsider@example.com", "encoded-password", UserRole.USER));
        Project ownedProject = projectRepository.save(new Project("Owned", null, owner));
        Project memberProject = new Project("Member", null, outsider);
        memberProject.addMember(member);
        memberProject = projectRepository.save(memberProject);

        Task firstTask = taskRepository.save(new Task("Todo", null, ownedProject));
        Task secondTask = taskRepository.save(new Task("In progress", null, ownedProject));
        secondTask.setStatus(TaskStatus.IN_PROGRESS);
        taskRepository.save(secondTask);
        Task thirdTask = taskRepository.save(new Task("Done", null, memberProject));
        thirdTask.setStatus(TaskStatus.DONE);
        taskRepository.save(thirdTask);

        List<Project> visibleProjects = projectRepository.findAllVisibleToUser(member.getEmail());
        List<ProjectTaskCounts> taskCounts = taskRepository.countTasksByProjectIds(
                visibleProjects.stream().map(Project::getId).toList()
        );

        assertEquals(1, visibleProjects.size());
        assertEquals(memberProject.getId(), visibleProjects.get(0).getId());
        assertEquals(1, taskCounts.size());
        assertEquals(memberProject.getId(), taskCounts.get(0).projectId());
        assertEquals(1L, taskCounts.get(0).totalTasks());
        assertEquals(0L, taskCounts.get(0).todoTasks());
        assertEquals(0L, taskCounts.get(0).inProgressTasks());
        assertEquals(1L, taskCounts.get(0).doneTasks());

        List<Project> ownerProjects = projectRepository.findAllVisibleToUser(owner.getEmail());
        List<ProjectTaskCounts> ownerTaskCounts = taskRepository.countTasksByProjectIds(
                ownerProjects.stream().map(Project::getId).toList()
        );
        assertEquals(1, ownerProjects.size());
        assertEquals(2L, ownerTaskCounts.get(0).totalTasks());
        assertEquals(1L, ownerTaskCounts.get(0).todoTasks());
        assertEquals(1L, ownerTaskCounts.get(0).inProgressTasks());
        assertEquals(0L, ownerTaskCounts.get(0).doneTasks());

        assertEquals(List.of(), projectRepository.findAllVisibleToUser("nobody@example.com"));
    }
}
