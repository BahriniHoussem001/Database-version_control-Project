package com.dbvc.service;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.dbvc.dto.SchemaTableItem;

@Service
public class SchemaService {

    private final JdbcTemplate jdbcTemplate;

    public SchemaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SchemaTableItem> findAllTables() {
        String sql = """
                SELECT object_name, created, last_ddl_time, status
                FROM user_objects
                WHERE object_type = 'TABLE'
                ORDER BY object_name
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> SchemaTableItem.builder()
                .tableName(rs.getString("object_name"))
                .createdAt(
                        rs.getTimestamp("created") != null
                                ? rs.getTimestamp("created").toLocalDateTime()
                                : null
                )
                .lastDdlTime(
                        rs.getTimestamp("last_ddl_time") != null
                                ? rs.getTimestamp("last_ddl_time").toLocalDateTime()
                                : null
                )
                .status(rs.getString("status"))
                .build());
    }
}
