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
public class SchemaTableItem {
	private String tableName;
    private LocalDateTime createdAt;
    private LocalDateTime lastDdlTime;
    private String status;

}
