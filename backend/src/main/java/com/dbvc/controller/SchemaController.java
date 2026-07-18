package com.dbvc.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dbvc.dto.SchemaTableItem;
import com.dbvc.service.SchemaService;

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
}
