package com.dbvc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedMigrationResponse {

    private String changesetId;
    private String filename;
    private String relativePath;
    private String message;
}