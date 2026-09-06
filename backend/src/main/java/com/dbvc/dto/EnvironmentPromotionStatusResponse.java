package com.dbvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentPromotionStatusResponse {

    private String environment;
    private int pendingMigrations;

    private int promotedMigrationsAvailable;
    private String nextPendingMigrationId;

    private boolean canApply;
    private String blockedReason;

    private boolean canApplyAll;
    private String applyAllBlockedReason;
}