package com.dbvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class LiquibaseExecutionResult {

	private boolean success;
	private String command;
	private int exitCode;
	private String output;
	private String error;
}
