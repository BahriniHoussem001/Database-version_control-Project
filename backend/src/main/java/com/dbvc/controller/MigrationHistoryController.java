package com.dbvc.controller;

import com.dbvc.dto.MigrationHistoryItem;
import com.dbvc.dto.LiquibaseExecutionResult;
import com.dbvc.service.LiquibaseCommandService;
import org.springframework.web.bind.annotation.PostMapping;


import com.dbvc.dto.MigrationValidationResult;
import com.dbvc.service.MigrationValidationService;

import com.dbvc.dto.PendingMigrationItem;
import com.dbvc.dto.MigrationSummaryResponse;
import com.dbvc.service.MigrationHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MigrationHistoryController {

	private final MigrationHistoryService migrationHistoryService;
	private final MigrationValidationService migrationValidationService;
	private final LiquibaseCommandService liquibaseCommandService;

	public MigrationHistoryController(
	        MigrationHistoryService migrationHistoryService,
	        MigrationValidationService migrationValidationService,
	        LiquibaseCommandService liquibaseCommandService
	) {
	    this.migrationHistoryService = migrationHistoryService;
	    this.migrationValidationService = migrationValidationService;
	    this.liquibaseCommandService = liquibaseCommandService;
	}
	
    @GetMapping("/api/migrations/history")
    public List<MigrationHistoryItem> getMigrationHistory() {
        return migrationHistoryService.findAll();
    }
    @GetMapping("/api/migrations/summary")
    public MigrationSummaryResponse getMigrationSummary() {
        return migrationHistoryService.getSummary();
    }
    @GetMapping("/api/migrations/pending")
    public List<PendingMigrationItem> getPendingMigrations() {
        return migrationHistoryService.findPendingMigrations();
    }
    @GetMapping("/api/migrations/pending/validation")
    public List<MigrationValidationResult> validatePendingMigrations() {
        return migrationValidationService.validatePendingMigrations();
    }
    @PostMapping("/api/migrations/apply")
    public LiquibaseExecutionResult applyPendingMigrations() {
        List<MigrationValidationResult> validationResults =
                migrationValidationService.validatePendingMigrations();

        boolean hasValidationProblems = validationResults.stream()
                .anyMatch(result -> !"VALID".equals(result.getStatus()));

        if (hasValidationProblems) {
            return LiquibaseExecutionResult.builder()
                    .success(false)
                    .command("docker compose run --rm liquibase update")
                    .exitCode(-1)
                    .output(null)
                    .error("Migration validation failed. Please check /api/migrations/pending/validation before applying.")
                    .build();
        }

        return liquibaseCommandService.runUpdate();
    }
    
}
