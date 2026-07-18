package com.dbvc.service;

import com.dbvc.dto.MigrationHistoryItem;
import com.dbvc.dto.MigrationSummaryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MigrationHistoryService {

    private final JdbcTemplate jdbcTemplate;

    public MigrationHistoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MigrationHistoryItem> findAll() {
        String sql = """
                SELECT id, author, filename, dateexecuted, orderexecuted, exectype
                FROM databasechangelog
                ORDER BY orderexecuted
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> MigrationHistoryItem.builder()
                .id(rs.getString("id"))
                .author(rs.getString("author"))
                .filename(rs.getString("filename"))
                .dateExecuted(
                        rs.getTimestamp("dateexecuted") != null
                                ? rs.getTimestamp("dateexecuted").toLocalDateTime()
                                : null
                )
                .orderExecuted(rs.getInt("orderexecuted"))
                .execType(rs.getString("exectype"))
                .build());
    }
    
    /////////////////////////////////////////////////////
    
    /* migration summury */
    
    public MigrationSummaryResponse getSummary() {
        String sql = """
                SELECT id, dateexecuted
                FROM databasechangelog
                ORDER BY orderexecuted DESC
                FETCH FIRST 1 ROWS ONLY
                """;

        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog",
                Integer.class
        );

        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return MigrationSummaryResponse.builder()
                        .totalExecutedMigrations(total != null ? total : 0)
                        .latestMigrationId(rs.getString("id"))
                        .latestExecutedAt(
                                rs.getTimestamp("dateexecuted") != null
                                        ? rs.getTimestamp("dateexecuted").toLocalDateTime()
                                        : null
                        )
                        .status("TRACKED")
                        .build();
            }

            return MigrationSummaryResponse.builder()
                    .totalExecutedMigrations(0)
                    .latestMigrationId(null)
                    .latestExecutedAt(null)
                    .status("NO_MIGRATION_EXECUTED")
                    .build();
        });
    }
    
}