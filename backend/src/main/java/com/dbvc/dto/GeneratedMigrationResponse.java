package com.dbvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedMigrationResponse {

    private String changesetId;
    private String filename;
    private String relativePath;
    private String artifactBucket;
    private String artifactKey;
    private String message;
}