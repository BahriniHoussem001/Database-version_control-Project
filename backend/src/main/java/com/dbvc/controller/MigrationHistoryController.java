package com.dbvc.controller;

import com.dbvc.dto.MigrationHistoryItem;


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

	public MigrationHistoryController(
	        MigrationHistoryService migrationHistoryService,
	        MigrationValidationService migrationValidationService
	) {
	    this.migrationHistoryService = migrationHistoryService;
	    this.migrationValidationService = migrationValidationService;
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
}
