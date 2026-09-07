package com.dbvc.controller;

import com.dbvc.service.ArtifactStorageService;
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
@RequestMapping("/api/generated-migrations")
@RequiredArgsConstructor
public class GeneratedMigrationArtifactController {

    private final ArtifactStorageService artifactStorageService;

    @GetMapping(value = "/{filename}/artifact", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> previewGeneratedMigrationArtifact(
            @PathVariable("filename") String filename
    ) {
        String cleanFilename = validateGeneratedMigrationFilename(filename);
        String artifactKey = "generated/" + cleanFilename;

        try {
            String sqlContent = artifactStorageService.readTextArtifact(
                    artifactStorageService.getBucket(),
                    artifactKey
            );

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + cleanFilename + "\""
                    )
                    .body(sqlContent);

        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Generated migration artifact not found in storage: " + artifactKey
            );
        }
    }

    private String validateGeneratedMigrationFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Generated migration filename is required."
            );
        }

        String cleanFilename = filename.trim();

        if (
                cleanFilename.contains("/")
                        || cleanFilename.contains("\\")
                        || cleanFilename.contains("..")
                        || !cleanFilename.toLowerCase().endsWith(".sql")
                        || !cleanFilename.matches("(?i)^V\\d{3}__.+\\.sql$")
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid generated migration filename."
            );
        }

        return cleanFilename;
    }
}