package com.dbvc.controller;

import com.dbvc.dto.CreateGeneratedMigrationRequest;
import com.dbvc.dto.GeneratedMigrationResponse;
import com.dbvc.service.GeneratedMigrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/generated-migrations")
@RequiredArgsConstructor
public class GeneratedMigrationController {

    private final GeneratedMigrationService generatedMigrationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public GeneratedMigrationResponse createMigration(
            @Valid @RequestBody CreateGeneratedMigrationRequest request
    ) {
        return generatedMigrationService.createMigration(request);
    }
}