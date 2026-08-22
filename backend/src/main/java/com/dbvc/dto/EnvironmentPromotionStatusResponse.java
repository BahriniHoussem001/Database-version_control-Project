package com.dbvc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentPromotionStatusResponse {

    private String environment;
    private int pendingMigrations;
    private boolean canApply;
    private String blockedReason;
}