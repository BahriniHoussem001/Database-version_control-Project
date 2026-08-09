package com.dbvc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonChangesetItem {

    private String id;
    private String author;
    private String filename;
    private Integer orderExecuted;
    private String execType;
}