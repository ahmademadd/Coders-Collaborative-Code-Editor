package com.example.coders.controllers;

import com.example.coders.dtos.RoleDto;
import com.example.coders.entities.ProjectDeveloperRole;
import com.example.coders.repositories.ProjectDeveloperRoleRepository;
import com.example.coders.repositories.ProjectRepository;
import com.example.coders.repositories.UserRepository;
import com.example.coders.responses.RoleResponse;
import com.example.coders.services.ProjectService;
import com.example.coders.services.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard/project/role")
public class RoleController {
    @Autowired
    private RoleService roleService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectDeveloperRoleRepository projectDeveloperRoleRepository;

    @PostMapping("/assign")
    public ResponseEntity<String> assignRole(@RequestBody RoleDto roleDto) {

        ProjectDeveloperRole assignedRole = roleService.assignRole(
                projectRepository.findBySlug(roleDto.getProjectSlug()).get().getProjectId(),
                userRepository.findByEmail(roleDto.getUserEmail()).get().getId(),
                roleDto.getRole());

        if (assignedRole != null) {
            return ResponseEntity.ok("Success");
        }
        return ResponseEntity.badRequest().build();
    }

    @PutMapping("/update")
    public ResponseEntity<String> updateRole(@RequestBody RoleDto roleDto) {

        ProjectDeveloperRole updatedRole = roleService.updateRole(
                projectRepository.findBySlug(roleDto.getProjectSlug()).get().getProjectId(),
                userRepository.findByEmail(roleDto.getUserEmail()).get().getId(),
                roleDto.getRole());

        if (updatedRole != null) {
            return ResponseEntity.ok("Success");
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/userRole")
    public ResponseEntity<?> getDeveloperRole(
            @RequestParam String projectSlug,
            @RequestParam String userEmail) {

        Integer projectId = projectRepository.findBySlug(projectSlug).get().getProjectId();
        Integer userId = userRepository.findByEmail(userEmail).get().getId();

        if (projectId == null || userId == null) {
            return ResponseEntity.badRequest().body("Invalid project slug or user email.");
        }

        ProjectDeveloperRole projectDeveloperRole = projectDeveloperRoleRepository
                .findByProject_ProjectIdAndDeveloper_Id(projectId, userId);

        RoleResponse roleResponse = new RoleResponse();
        roleResponse.setName("");
        roleResponse.setEmail(userEmail);
        roleResponse.setRole(projectDeveloperRole.getRole());
        if (projectDeveloperRole != null) {
            return ResponseEntity.ok(roleResponse);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/inProject")
    public ResponseEntity<?> getUserInProject(@RequestParam String projectSlug) {
        try {
            List<RoleResponse> users = roleService.getUsersInProject(projectService.getProjectBySlug(projectSlug));
            if (users.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "No results found");
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving users: " + e.getMessage());
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Void> removeRole(@RequestBody RoleDto roleDto) {

        roleService.removeRole(
                projectRepository.findBySlug(roleDto.getProjectSlug()).get().getProjectId(),
                userRepository.findByEmail(roleDto.getUserEmail()).get().getId());

        return ResponseEntity.noContent().build();
    }
}
