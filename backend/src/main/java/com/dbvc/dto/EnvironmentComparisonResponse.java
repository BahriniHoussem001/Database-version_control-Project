package com.dbvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentComparisonResponse {

    private String sourceEnvironment;
    private String targetEnvironment;

    private boolean inSync;

    private int sourceTotalMigrations;
    private int targetTotalMigrations;

    private int missingInTargetCount;
    private int extraInTargetCount;

    private List<ComparisonChangesetItem> missingInTarget;
    private List<ComparisonChangesetItem> extraInTarget;
}