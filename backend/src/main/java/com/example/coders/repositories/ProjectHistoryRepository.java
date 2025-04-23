package com.example.coders.repositories;

import com.example.coders.entities.Project;
import com.example.coders.entities.ProjectHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectHistoryRepository extends JpaRepository<ProjectHistory, Integer> {
    // Custom query to find history by project
    List<ProjectHistory> findByProject(Project project);
}
