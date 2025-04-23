package com.example.coders.controllers;

import com.example.coders.dtos.ExecuteDto;
import com.example.coders.dtos.ExecutionRequestsDto;
import com.example.coders.dtos.ExecutionResultDto;
import com.example.coders.repositories.ProjectRepository;
import com.example.coders.managers.ExecutionManager;
import com.example.coders.services.ExecutionService;
import com.example.coders.services.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard/project/code")
public class ExecutionController {

    @Autowired
    private ExecutionService executionService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ExecutionManager executionManager;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/execute")
    public ResponseEntity<?> executeCode(@RequestBody ExecuteDto executeDto) {
        try {
            ExecutionRequestsDto executionRequest = new ExecutionRequestsDto(
                    executeDto.getFileDto().getProjectSlug(),
                    executeDto
            );

            executionManager.submitExecutionRequest(executionRequest);

            return ResponseEntity.ok(new ExecutionResultDto("Success", "Execution request submitted"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ExecutionResultDto("Failed", e.getMessage()));
        }
    }
}
