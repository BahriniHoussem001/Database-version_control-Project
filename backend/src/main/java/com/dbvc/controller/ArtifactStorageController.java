package com.dbvc.controller;

import com.dbvc.dto.ArtifactStorageHealthResponse;
import com.dbvc.service.ArtifactStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ArtifactStorageController {

    private final ArtifactStorageService artifactStorageService;

    @GetMapping("/api/artifact-storage/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ArtifactStorageHealthResponse checkArtifactStorageHealth() {
        return artifactStorageService.checkHealth();
    }
}