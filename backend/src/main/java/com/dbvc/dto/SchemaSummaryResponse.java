package com.dbvc.dto;

import java.time.LocalDateTime;

import aQute.bnd.annotation.headers.BundleLicense;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class SchemaSummaryResponse {
    private int totalTables;
    private int totalColumns;
    private String latestChangedTable;
    private LocalDateTime latestDdlTime;
    private String status;
	

}
