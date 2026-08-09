package com.dbvc.service;

import com.dbvc.dto.SchemaColumnItem;
import com.dbvc.dto.SchemaSummaryResponse;
import com.dbvc.dto.SchemaTableItem;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class SchemaService {

    private final JdbcTemplate jdbcTemplate;
    private final EnvironmentJdbcTemplateProvider environmentJdbcTemplateProvider;

    public SchemaService(
            JdbcTemplate jdbcTemplate,
            EnvironmentJdbcTemplateProvider environmentJdbcTemplateProvider
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.environmentJdbcTemplateProvider = environmentJdbcTemplateProvider;
    }

    public List<SchemaTableItem> findAllTables() {
        return findAllTablesUsingJdbcTemplate(jdbcTemplate);
    }

    public List<SchemaTableItem> findAllTablesByEnvironment(String environment) {
        JdbcTemplate environmentJdbcTemplate =
                environmentJdbcTemplateProvider.getJdbcTemplate(environment);

        return findAllTablesUsingJdbcTemplate(environmentJdbcTemplate);
    }

    public List<SchemaColumnItem> findColumnsByTableName(String tableName) {
        return findColumnsByTableNameUsingJdbcTemplate(jdbcTemplate, tableName);
    }

    public List<SchemaColumnItem> findColumnsByTableNameAndEnvironment(
            String environment,
            String tableName
    ) {
        JdbcTemplate environmentJdbcTemplate =
                environmentJdbcTemplateProvider.getJdbcTemplate(environment);

        return findColumnsByTableNameUsingJdbcTemplate(environmentJdbcTemplate, tableName);
    }

    public SchemaSummaryResponse getSchemaSummary() {
        return getSchemaSummaryUsingJdbcTemplate(jdbcTemplate);
    }

    public SchemaSummaryResponse getSchemaSummaryByEnvironment(String environment) {
        JdbcTemplate environmentJdbcTemplate =
                environmentJdbcTemplateProvider.getJdbcTemplate(environment);

        return getSchemaSummaryUsingJdbcTemplate(environmentJdbcTemplate);
    }

    private List<SchemaTableItem> findAllTablesUsingJdbcTemplate(JdbcTemplate targetJdbcTemplate) {
        String sql = """
                SELECT object_name,
                       created,
                       last_ddl_time,
                       status
                FROM user_objects
                WHERE object_type = 'TABLE'
                ORDER BY object_name
                """;

        return targetJdbcTemplate.query(sql, (rs, rowNum) -> SchemaTableItem.builder()
                .tableName(rs.getString("object_name"))
                .createdAt(toLocalDateTime(rs.getTimestamp("created")))
                .lastDdlTime(toLocalDateTime(rs.getTimestamp("last_ddl_time")))
                .status(rs.getString("status"))
                .build());
    }

    private List<SchemaColumnItem> findColumnsByTableNameUsingJdbcTemplate(
            JdbcTemplate targetJdbcTemplate,
            String tableName
    ) {
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

        return targetJdbcTemplate.query(sql, (rs, rowNum) -> SchemaColumnItem.builder()
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

    private SchemaSummaryResponse getSchemaSummaryUsingJdbcTemplate(JdbcTemplate targetJdbcTemplate) {
        Integer totalTables = targetJdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM user_objects
                WHERE object_type = 'TABLE'
                """,
                Integer.class
        );

        Integer totalColumns = targetJdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM user_tab_columns
                """,
                Integer.class
        );

        String latestTableSql = """
                SELECT object_name,
                       last_ddl_time
                FROM user_objects
                WHERE object_type = 'TABLE'
                ORDER BY last_ddl_time DESC
                FETCH FIRST 1 ROWS ONLY
                """;

        return targetJdbcTemplate.query(latestTableSql, rs -> {
            if (rs.next()) {
                return SchemaSummaryResponse.builder()
                        .totalTables(totalTables != null ? totalTables : 0)
                        .totalColumns(totalColumns != null ? totalColumns : 0)
                        .latestChangedTable(rs.getString("object_name"))
                        .latestDdlTime(toLocalDateTime(rs.getTimestamp("last_ddl_time")))
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

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}