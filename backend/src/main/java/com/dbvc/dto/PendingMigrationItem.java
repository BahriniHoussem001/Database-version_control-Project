package com.dbvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class PendingMigrationItem {
	private String id;
    private String author;
    private String filename;
    private String status;

}
