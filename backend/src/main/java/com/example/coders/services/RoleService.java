package com.example.coders.services;

import com.example.coders.entities.Project;
import com.example.coders.entities.ProjectDeveloperRole;
import com.example.coders.entities.ProjectDeveloperId;
import com.example.coders.entities.User;
import com.example.coders.repositories.ProjectRepository;
import com.example.coders.repositories.ProjectDeveloperRoleRepository;
import com.example.coders.repositories.UserRepository;
import com.example.coders.responses.RoleResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoleService {
    @Autowired
    private ProjectDeveloperRoleRepository projectDeveloperRoleRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public ProjectDeveloperRole assignRole(Integer projectId, Integer developerId, String role) {
        Optional<Project> project = projectRepository.findById(projectId);
        Optional<User> developer = userRepository.findById(developerId);

        if (project.isPresent() && developer.isPresent()) {
            ProjectDeveloperRole projectDeveloperRole = new ProjectDeveloperRole();
            ProjectDeveloperId id = new ProjectDeveloperId(projectId, developerId);
            projectDeveloperRole.setId(id);
            projectDeveloperRole.setProject(project.get());
            projectDeveloperRole.setDeveloper(developer.get());
            projectDeveloperRole.setRole(role);
            return projectDeveloperRoleRepository.save(projectDeveloperRole);
        }
        return null; // You may want to throw an exception or return an error response
    }

    @Transactional
    public ProjectDeveloperRole updateRole(Integer projectId, Integer developerId, String newRole) {
        ProjectDeveloperId id = new ProjectDeveloperId(projectId, developerId);
        Optional<ProjectDeveloperRole> role = projectDeveloperRoleRepository.findById(id);
        if (role.isPresent()) {
            role.get().setRole(newRole);
            return projectDeveloperRoleRepository.save(role.get());
        }
        return null; // Handle as needed
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getUsersInProject(Integer projectId) {
        List<User> users = projectDeveloperRoleRepository.findAllUsersByProjectIdExceptOwner(projectId);

        return users.stream()
                .map(user -> {
                    ProjectDeveloperId id = new ProjectDeveloperId(projectId, user.getId());
                    Optional<ProjectDeveloperRole> roleOpt = projectDeveloperRoleRepository.findById(id);
                    String roleName = roleOpt.map(ProjectDeveloperRole::getRole).orElse("No role assigned");

                    return new RoleResponse(user.getName(), user.getEmail(), roleName);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeRole(Integer projectId, Integer developerId) {
        ProjectDeveloperId id = new ProjectDeveloperId(projectId, developerId);
        projectDeveloperRoleRepository.deleteById(id);
    }
}
