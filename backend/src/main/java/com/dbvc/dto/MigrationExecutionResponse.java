package com.dbvc.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class MigrationExecutionResponse {
	 private Long id;

	    private String requestType;
	    private String priority;
	    private String reason;

	    private String status;

	    private String command;
	    private Integer exitCode;

	    private String output;
	    private String error;
	    private String validationSummary;

	    private String requestedBy;

	    private LocalDateTime requestedAt;
	    private LocalDateTime startedAt;
	    private LocalDateTime finishedAt;

	    private Long durationMs;
	
	

}
