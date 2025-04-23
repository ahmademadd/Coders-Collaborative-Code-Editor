package com.example.coders.entities;

import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class ProjectDeveloperId implements Serializable {
    private Integer projectId;
    private Integer developerId;

    // Default constructor
    public ProjectDeveloperId() {
    }

    // Parameterized constructor
    public ProjectDeveloperId(Integer projectId, Integer developerId) {
        this.projectId = projectId;
        this.developerId = developerId;
    }

    // Getters and setters
    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public Integer getDeveloperId() {
        return developerId;
    }

    public void setDeveloperId(Integer developerId) {
        this.developerId = developerId;
    }
}
