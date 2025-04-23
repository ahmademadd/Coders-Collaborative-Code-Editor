package com.example.coders.repositories;

import com.example.coders.entities.Project;
import com.example.coders.entities.ProjectDeveloperId;
import com.example.coders.entities.ProjectDeveloperRole;
import com.example.coders.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectDeveloperRepository extends JpaRepository<ProjectDeveloperRole, ProjectDeveloperId> {
    // Custom query to find developers by project
    List<ProjectDeveloperRole> findByProject(Project project);

    // Custom query to find projects by developer
    List<ProjectDeveloperRole> findByDeveloper(User developer);
}
