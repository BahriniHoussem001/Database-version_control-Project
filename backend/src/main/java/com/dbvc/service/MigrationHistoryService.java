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
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MigrationHistoryService {

    private final JdbcTemplate jdbcTemplate;
    private final EnvironmentJdbcTemplateProvider environmentJdbcTemplateProvider;

    @Value("${dbvc.liquibase.changelog-path}")
    private String changelogPath;

    @Value("${dbvc.liquibase.application-changelog-path:../liquibase/changelog/db.application-changelog-master.sql}")
    private String applicationChangelogPath;

    public MigrationHistoryService(
            JdbcTemplate jdbcTemplate,
            EnvironmentJdbcTemplateProvider environmentJdbcTemplateProvider
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.environmentJdbcTemplateProvider = environmentJdbcTemplateProvider;
    }

    public List<MigrationHistoryItem> findAll() {
        return findHistoryUsingJdbcTemplate(jdbcTemplate);
    }

    public List<MigrationHistoryItem> findAllByEnvironment(String environment) {
        JdbcTemplate environmentJdbcTemplate =
                environmentJdbcTemplateProvider.getJdbcTemplate(environment);

        return findHistoryUsingJdbcTemplate(environmentJdbcTemplate);
    }

    public MigrationSummaryResponse getSummary() {
        return getSummaryUsingJdbcTemplate(jdbcTemplate);
    }

    public MigrationSummaryResponse getSummaryByEnvironment(String environment) {
        JdbcTemplate environmentJdbcTemplate =
                environmentJdbcTemplateProvider.getJdbcTemplate(environment);

        return getSummaryUsingJdbcTemplate(environmentJdbcTemplate);
    }

    public List<PendingMigrationItem> findPendingMigrations() {
        return findPendingMigrationsUsingJdbcTemplate(
                jdbcTemplate,
                changelogPath,
                "project/changelog/db.changelog-master.sql"
        );
    }

    public List<PendingMigrationItem> findPendingMigrationsByEnvironment(String environment) {
        JdbcTemplate environmentJdbcTemplate =
                environmentJdbcTemplateProvider.getJdbcTemplate(environment);

        return findPendingMigrationsUsingJdbcTemplate(
                environmentJdbcTemplate,
                applicationChangelogPath,
                "project/changelog/db.application-changelog-master.sql"
        );
    }

    private List<MigrationHistoryItem> findHistoryUsingJdbcTemplate(JdbcTemplate targetJdbcTemplate) {
        String sql = """
                SELECT id,
                       author,
                       filename,
                       dateexecuted,
                       orderexecuted,
                       exectype
                FROM databasechangelog
                ORDER BY orderexecuted
                """;

        return targetJdbcTemplate.query(sql, (rs, rowNum) -> MigrationHistoryItem.builder()
                .id(rs.getString("id"))
                .author(rs.getString("author"))
                .filename(rs.getString("filename"))
                .dateExecuted(toLocalDateTime(rs.getTimestamp("dateexecuted")))
                .orderExecuted(rs.getInt("orderexecuted"))
                .execType(rs.getString("exectype"))
                .build());
    }

    private MigrationSummaryResponse getSummaryUsingJdbcTemplate(JdbcTemplate targetJdbcTemplate) {
        String latestMigrationSql = """
                SELECT id, dateexecuted
                FROM databasechangelog
                ORDER BY orderexecuted DESC
                FETCH FIRST 1 ROWS ONLY
                """;

        Integer total = targetJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog",
                Integer.class
        );

        return targetJdbcTemplate.query(latestMigrationSql, rs -> {
            if (rs.next()) {
                return MigrationSummaryResponse.builder()
                        .totalExecutedMigrations(total != null ? total : 0)
                        .latestMigrationId(rs.getString("id"))
                        .latestExecutedAt(toLocalDateTime(rs.getTimestamp("dateexecuted")))
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

    private List<PendingMigrationItem> findPendingMigrationsUsingJdbcTemplate(
            JdbcTemplate targetJdbcTemplate,
            String targetChangelogPath,
            String filename
    ) {
        Set<String> executedChangesets = targetJdbcTemplate.queryForList(
                        "SELECT id || '::' || author FROM databasechangelog",
                        String.class
                )
                .stream()
                .collect(Collectors.toSet());

        Pattern changesetPattern = Pattern.compile("^--changeset\\s+([^:]+):(.+)$");

        try {
            return Files.readAllLines(Path.of(targetChangelogPath))
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
                                .filename(filename)
                                .status("PENDING")
                                .build();
                    })
                    .filter(item -> item != null)
                    .toList();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to read Liquibase changelog file: " + targetChangelogPath,
                    e
            );
        }
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}