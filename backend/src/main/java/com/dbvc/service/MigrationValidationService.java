package com.dbvc.service;

import com.dbvc.dto.MigrationValidationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class MigrationValidationService {
	private final JdbcTemplate jdbcTemplate;

    @Value("${dbvc.liquibase.changelog-path}")
    private String changelogPath;

    public MigrationValidationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MigrationValidationResult> validatePendingMigrations() {
        Set<String> executedChangesets = jdbcTemplate.queryForList(
                        "SELECT id || '::' || author FROM databasechangelog",
                        String.class
                )
                .stream()
                .collect(Collectors.toSet());

        List<ParsedChangeset> changesets = parseChangesets();

        return changesets.stream()
                .filter(changeset -> !executedChangesets.contains(changeset.id() + "::" + changeset.author()))
                .map(this::validateChangeset)
                .toList();
    }

    private List<ParsedChangeset> parseChangesets() {
        Pattern changesetPattern = Pattern.compile("^--changeset\\s+([^:]+):(.+)$");

        try {
            List<String> lines = Files.readAllLines(Path.of(changelogPath));
            List<ParsedChangeset> changesets = new ArrayList<>();

            String currentAuthor = null;
            String currentId = null;
            List<String> currentSqlLines = new ArrayList<>();
            boolean hasRollback = false;

            for (String rawLine : lines) {
                String line = rawLine.trim();
                Matcher matcher = changesetPattern.matcher(line);

                if (line.startsWith("--changeset")) {
                    if (currentId != null) {
                        changesets.add(new ParsedChangeset(currentId, currentAuthor, currentSqlLines, hasRollback));
                    }

                    currentSqlLines = new ArrayList<>();
                    hasRollback = false;

                    if (matcher.matches()) {
                        currentAuthor = matcher.group(1).trim();
                        currentId = matcher.group(2).trim();
                    } else {
                        currentAuthor = "UNKNOWN";
                        currentId = "INVALID_CHANGESET_FORMAT";
                    }

                    continue;
                }

                if (currentId != null) {
                    if (line.startsWith("--rollback")) {
                        hasRollback = true;
                    } else if (!line.isBlank() && !line.startsWith("--")) {
                        currentSqlLines.add(line);
                    }
                }
            }

            if (currentId != null) {
                changesets.add(new ParsedChangeset(currentId, currentAuthor, currentSqlLines, hasRollback));
            }

            return changesets;

        } catch (IOException e) {
            throw new IllegalStateException("Unable to read Liquibase changelog file: " + changelogPath, e);
        }
    }

    private MigrationValidationResult validateChangeset(ParsedChangeset changeset) {
        List<String> issues = new ArrayList<>();

        if ("INVALID_CHANGESET_FORMAT".equals(changeset.id())) {
            issues.add("Invalid changeset format. Expected: --changeset author:id");
        }

        if (!changeset.hasRollback()) {
            issues.add("Rollback instruction is required");
        }

        String sql = String.join(" ", changeset.sqlLines()).toUpperCase();

        if (sql.contains("DROP TABLE")) {
            issues.add("Dangerous operation detected: DROP TABLE");
        }

        if (sql.contains("DROP COLUMN")) {
            issues.add("Dangerous operation detected: DROP COLUMN");
        }

        if (sql.contains("TRUNCATE TABLE")) {
            issues.add("Dangerous operation detected: TRUNCATE TABLE");
        }

        if (sql.contains("DELETE FROM")) {
            issues.add("Dangerous operation detected: DELETE FROM");
        }

        if (sql.contains("ALTER TABLE") && sql.contains("MODIFY")) {
            issues.add("Potentially risky operation detected: ALTER TABLE MODIFY");
        }

        String status = issues.isEmpty() ? "VALID" : "WARNING";

        if ("INVALID_CHANGESET_FORMAT".equals(changeset.id())) {
            status = "INVALID";
        }

        return MigrationValidationResult.builder()
                .id(changeset.id())
                .author(changeset.author())
                .status(status)
                .issues(issues)
                .build();
    }

    private record ParsedChangeset(
            String id,
            String author,
            List<String> sqlLines,
            boolean hasRollback
    ) {
    }
}


