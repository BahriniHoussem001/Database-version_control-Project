package com.dbvc.service;

import com.dbvc.dto.MigrationHistoryItem;
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
}