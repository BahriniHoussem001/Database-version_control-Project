package com.dbvc.service;

import com.dbvc.dto.MigrationHistoryItem;
import com.dbvc.dto.MigrationSummaryResponse;
import com.dbvc.dto.PendingMigrationItem;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MigrationHistoryService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${dbvc.liquibase.changelog-path}")
    private String changelogPath;

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

    public MigrationSummaryResponse getSummary() {
        String latestMigrationSql = """
                SELECT id, dateexecuted
                FROM databasechangelog
                ORDER BY orderexecuted DESC
                FETCH FIRST 1 ROWS ONLY
                """;

        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog",
                Integer.class
        );

        return jdbcTemplate.query(latestMigrationSql, rs -> {
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

    public List<PendingMigrationItem> findPendingMigrations() {
        Set<String> executedChangesets = jdbcTemplate.queryForList(
                        "SELECT id || '::' || author FROM databasechangelog",
                        String.class
                )
                .stream()
                .collect(Collectors.toSet());

        Pattern changesetPattern = Pattern.compile("^--changeset\\s+([^:]+):(.+)$");

        try {
            return Files.readAllLines(Path.of(changelogPath))
                    .stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("--changeset"))
                    .map(line -> {
                        Matcher matcher = changesetPattern.matcher(line);

                        if (!matcher.matches()) {
                            return null;
                        }

                        String author = matcher.group(1).trim();
                        String id = matcher.group(2).trim();
                        String key = id + "::" + author;

                        if (executedChangesets.contains(key)) {
                            return null;
                        }

                        return PendingMigrationItem.builder()
                                .id(id)
                                .author(author)
                                .filename("project/changelog/db.changelog-master.sql")
                                .status("PENDING")
                                .build();
                    })
                    .filter(item -> item != null)
                    .toList();

        } catch (IOException e) {
            throw new IllegalStateException("Unable to read Liquibase changelog file: " + changelogPath, e);
        }
    }
}