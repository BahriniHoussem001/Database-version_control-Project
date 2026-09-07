package com.dbvc.controller;

import com.dbvc.dto.MigrationExecutionResponse;
import com.dbvc.service.ArtifactStorageService;
import com.dbvc.service.MigrationExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/migration-executions")
@RequiredArgsConstructor
public class ExecutionLogArtifactController {

    private final MigrationExecutionService migrationExecutionService;
    private final ArtifactStorageService artifactStorageService;

    @GetMapping(value = "/{id}/artifact-log", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> previewArchivedExecutionLog(@PathVariable("id") Long id) {
        MigrationExecutionResponse execution = migrationExecutionService.findById(id);

        if (execution.getLogArtifactBucket() == null || execution.getLogArtifactKey() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No archived execution log found for execution id: " + id
            );
        }

        String logContent = artifactStorageService.readTextArtifact(
                execution.getLogArtifactBucket(),
                execution.getLogArtifactKey()
        );

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"execution-" + id + ".log\""
                )
                .body(logContent);
    }
}