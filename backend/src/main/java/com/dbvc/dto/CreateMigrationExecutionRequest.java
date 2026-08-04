package com.dbvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateMigrationExecutionRequest {
	private String requestType;
	private String priority;
	private String reason;
	private String requestedBy;

}
