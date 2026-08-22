package com.dbvc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateGeneratedMigrationRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String author;

    @NotBlank
    private String sql;

    @NotBlank
    private String rollbackSql;
}