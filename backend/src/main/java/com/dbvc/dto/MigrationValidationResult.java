package com.dbvc.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class MigrationValidationResult {
	
	 private String id;
	    private String author;
	    private String status;
	    private List<String> issues;
}
