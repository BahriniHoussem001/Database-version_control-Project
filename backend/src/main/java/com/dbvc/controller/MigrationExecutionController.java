package com.dbvc.controller;

import com.dbvc.dto.CreateMigrationExecutionRequest;
import com.dbvc.dto.MigrationExecutionSummaryResponse;
import com.dbvc.dto.MigrationExecutionResponse;
import com.dbvc.service.MigrationExecutionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MigrationExecutionController {

    private final MigrationExecutionService migrationExecutionService;

    public MigrationExecutionController(MigrationExecutionService migrationExecutionService) {
        this.migrationExecutionService = migrationExecutionService;
    }

    @PostMapping("/api/migration-executions")
    public MigrationExecutionResponse createExecutionRequest(
            @RequestBody CreateMigrationExecutionRequest request
    ) {
        return migrationExecutionService.createExecutionRequest(request);
    }

    @GetMapping("/api/migration-executions")
    public List<MigrationExecutionResponse> getExecutionRequests() {
        return migrationExecutionService.findAll();
    }

    @GetMapping("/api/migration-executions/{id}")
    public MigrationExecutionResponse getExecutionRequestById(@PathVariable("id") Long id) {
        return migrationExecutionService.findById(id);
    }
    @PostMapping("/api/migration-executions/process-next")
    public MigrationExecutionResponse processNextQueuedRequest() {
        return migrationExecutionService.processNextQueuedRequest();
    }
    @GetMapping("/api/migration-executions/summary")
    public MigrationExecutionSummaryResponse getExecutionSummary() {
        return migrationExecutionService.getExecutionSummary();
    }
    
}