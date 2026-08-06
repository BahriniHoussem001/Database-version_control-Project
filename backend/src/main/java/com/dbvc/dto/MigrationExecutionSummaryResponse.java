package com.dbvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationExecutionSummaryResponse {

    private int totalRequests;
    private int queuedRequests;
    private int runningRequests;
    private int successfulRequests;
    private int failedRequests;

    private Long latestRequestId;
    private String latestRequestType;
    private String latestStatus;
    private String latestPriority;
    private LocalDateTime latestRequestedAt;
    private LocalDateTime latestFinishedAt;
}