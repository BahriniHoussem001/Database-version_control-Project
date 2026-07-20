package com.dbvc.service;

import java.util.List;
import com.dbvc.dto.SchemaColumnItem;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.dbvc.dto.SchemaTableItem;
import com.dbvc.dto.SchemaSummaryResponse;

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
    public List<SchemaColumnItem> findColumnsByTableName(String tableName) {
        String sql = """
                SELECT column_name,
                       data_type,
                       data_length,
                       data_precision,
                       data_scale,
                       nullable,
                       column_id
                FROM user_tab_columns
                WHERE table_name = UPPER(?)
                ORDER BY column_id
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> SchemaColumnItem.builder()
                .columnName(rs.getString("column_name"))
                .dataType(rs.getString("data_type"))
                .dataLength(rs.getInt("data_length"))
                .dataPrecision(rs.getObject("data_precision") != null ? rs.getInt("data_precision") : null)
                .dataScale(rs.getObject("data_scale") != null ? rs.getInt("data_scale") : null)
                .nullable(rs.getString("nullable"))
                .columnId(rs.getInt("column_id"))
                .defaultValue(null)
                .build(), tableName);
    }
    public SchemaSummaryResponse getSchemaSummary() {
        Integer totalTables = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM user_objects
                WHERE object_type = 'TABLE'
                """,
                Integer.class
        );

        Integer totalColumns = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM user_tab_columns
                """,
                Integer.class
        );

        String latestTableSql = """
                SELECT object_name, last_ddl_time
                FROM user_objects
                WHERE object_type = 'TABLE'
                ORDER BY last_ddl_time DESC
                FETCH FIRST 1 ROWS ONLY
                """;

        return jdbcTemplate.query(latestTableSql, rs -> {
            if (rs.next()) {
                return SchemaSummaryResponse.builder()
                        .totalTables(totalTables != null ? totalTables : 0)
                        .totalColumns(totalColumns != null ? totalColumns : 0)
                        .latestChangedTable(rs.getString("object_name"))
                        .latestDdlTime(
                                rs.getTimestamp("last_ddl_time") != null
                                        ? rs.getTimestamp("last_ddl_time").toLocalDateTime()
                                        : null
                        )
                        .status("READABLE")
                        .build();
            }

            return SchemaSummaryResponse.builder()
                    .totalTables(0)
                    .totalColumns(0)
                    .latestChangedTable(null)
                    .latestDdlTime(null)
                    .status("EMPTY_SCHEMA")
                    .build();
        });
    }
}
