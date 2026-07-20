package com.dbvc.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dbvc.dto.SchemaTableItem;
import com.dbvc.service.SchemaService;
import com.dbvc.dto.SchemaColumnItem;
import org.springframework.web.bind.annotation.PathVariable;
import com.dbvc.dto.SchemaSummaryResponse;

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
    public List<SchemaColumnItem> getTableColumns(@PathVariable("tableName") String tableName) {
        return schemaService.findColumnsByTableName(tableName);
    }
    @GetMapping("/api/schema/summary")
    public SchemaSummaryResponse getSchemaSummary() {
        return schemaService.getSchemaSummary();
    }
}
