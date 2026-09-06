package com.dbvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactStorageHealthResponse {

    private boolean available;
    private String provider;
    private String endpoint;
    private String bucket;
    private String message;
}