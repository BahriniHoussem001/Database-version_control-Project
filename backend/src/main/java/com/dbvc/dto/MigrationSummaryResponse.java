package com.dbvc.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class MigrationSummaryResponse {
	private int totalExecutedMigrations;
	private String latestMigrationId;
	private LocalDateTime latestExecutedAt;
	private String status;
	

}
