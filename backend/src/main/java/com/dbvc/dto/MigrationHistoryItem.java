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

public class MigrationHistoryItem {
	 private String id;
	    private String author;
	    private String filename;
	    private LocalDateTime dateExecuted;
	    private Integer orderExecuted;
	    private String execType;

}
