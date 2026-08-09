package com.dbvc.controller;

import com.dbvc.dto.SchemaColumnItem;
import com.dbvc.dto.SchemaSummaryResponse;
import com.dbvc.dto.SchemaTableItem;
import com.dbvc.service.SchemaService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SchemaController {

    private final SchemaService schemaService;

    public SchemaController(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @GetMapping("/api/schema/tables")
    public List<SchemaTableItem> getSchemaTables() {
        return schemaService.findAllTables();
    }

    @GetMapping("/api/schema/tables/{tableName}/columns")
    public List<SchemaColumnItem> getTableColumns(
            @PathVariable("tableName") String tableName
    ) {
        return schemaService.findColumnsByTableName(tableName);
    }

    @GetMapping("/api/schema/summary")
    public SchemaSummaryResponse getSchemaSummary() {
        return schemaService.getSchemaSummary();
    }

    @GetMapping("/api/environments/{environment}/schema/tables")
    public List<SchemaTableItem> getEnvironmentSchemaTables(
            @PathVariable("environment") String environment
    ) {
        return schemaService.findAllTablesByEnvironment(environment);
    }

    @GetMapping("/api/environments/{environment}/schema/tables/{tableName}/columns")
    public List<SchemaColumnItem> getEnvironmentTableColumns(
            @PathVariable("environment") String environment,
            @PathVariable("tableName") String tableName
    ) {
        return schemaService.findColumnsByTableNameAndEnvironment(environment, tableName);
    }

    @GetMapping("/api/environments/{environment}/schema/summary")
    public SchemaSummaryResponse getEnvironmentSchemaSummary(
            @PathVariable("environment") String environment
    ) {
        return schemaService.getSchemaSummaryByEnvironment(environment);
    }
}