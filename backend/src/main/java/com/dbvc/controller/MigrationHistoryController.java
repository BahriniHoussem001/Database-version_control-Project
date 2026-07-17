package com.dbvc.controller;

import com.dbvc.dto.MigrationHistoryItem;
import com.dbvc.service.MigrationHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MigrationHistoryController {

    private final MigrationHistoryService migrationHistoryService;

    public MigrationHistoryController(MigrationHistoryService migrationHistoryService) {
        this.migrationHistoryService = migrationHistoryService;
    }

    @GetMapping("/api/migrations/history")
    public List<MigrationHistoryItem> getMigrationHistory() {
        return migrationHistoryService.findAll();
    }
}
