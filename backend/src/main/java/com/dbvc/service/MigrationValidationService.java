package com.dbvc.service;

import com.dbvc.dto.MigrationValidationResult;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MigrationValidationService {

    private final JdbcTemplate jdbcTemplate;
    private final EnvironmentJdbcTemplateProvider environmentJdbcTemplateProvider;

    @Value("${dbvc.liquibase.changelog-path}")
    private String changelogPath;

    @Value("${dbvc.liquibase.application-changelog-path:../liquibase/changelog/db.application-changelog-master.sql}")
    private String applicationChangelogPath;

    @Value("${dbvc.liquibase.generated-changelog-directory:../liquibase/changelog/generated}")
    private String generatedChangelogDirectory;

    public MigrationValidationService(
            JdbcTemplate jdbcTemplate,
            EnvironmentJdbcTemplateProvider environmentJdbcTemplateProvider
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.environmentJdbcTemplateProvider = environmentJdbcTemplateProvider;
    }

    public List<MigrationValidationResult> validatePendingMigrations() {
        return validatePendingMigrationsUsingJdbcTemplate(
                jdbcTemplate,
                List.of(Path.of(changelogPath))
        );
    }

    public List<MigrationValidationResult> validatePendingMigrationsByEnvironment(String environment) {
        JdbcTemplate environmentJdbcTemplate =
                environmentJdbcTemplateProvider.getJdbcTemplate(environment);

        return validatePendingMigrationsUsingJdbcTemplate(
                environmentJdbcTemplate,
                getApplicationChangelogFiles()
        );
    }

    private List<MigrationValidationResult> validatePendingMigrationsUsingJdbcTemplate(
            JdbcTemplate targetJdbcTemplate,
            List<Path> changelogFiles
    ) {
        Set<String> executedChangesets = targetJdbcTemplate.queryForList(
                        "SELECT id || '::' || author FROM databasechangelog",
                        String.class
                )
                .stream()
                .collect(Collectors.toSet());

        List<ParsedChangeset> changesets = changelogFiles.stream()
                .flatMap(path -> parseChangesets(path).stream())
                .toList();

        return changesets.stream()
                .filter(changeset -> !executedChangesets.contains(changeset.id() + "::" + changeset.author()))
                .map(this::validateChangeset)
                .toList();
    }

    private List<ParsedChangeset> parseChangesets(Path changelogFilePath) {
        Pattern changesetPattern = Pattern.compile("^--changeset\\s+([^:]+):(.+)$");

        try {
            List<String> lines = Files.readAllLines(changelogFilePath);
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
            throw new IllegalStateException(
                    "Unable to read Liquibase changelog file: " + changelogFilePath,
                    e
            );
        }
    }

    private List<Path> getApplicationChangelogFiles() {
        List<Path> generatedFiles = getGeneratedChangelogFiles();

        return Stream.concat(
                        Stream.of(Path.of(applicationChangelogPath)),
                        generatedFiles.stream()
                )
                .toList();
    }

    private List<Path> getGeneratedChangelogFiles() {
        Path generatedDirectory = Path.of(generatedChangelogDirectory);

        if (!Files.exists(generatedDirectory)) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(generatedDirectory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".sql"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to scan generated changelog directory: " + generatedChangelogDirectory,
                    e
            );
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

        String sql = normalizeSql(String.join(" ", changeset.sqlLines()));

        if (matchesSqlPattern(sql, "\\bDROP\\s+TABLE\\b")) {
            issues.add("Dangerous operation detected: DROP TABLE");
        }

        if (matchesSqlPattern(sql, "\\bDROP\\s+COLUMN\\b")) {
            issues.add("Dangerous operation detected: DROP COLUMN");
        }

        if (matchesSqlPattern(sql, "\\bTRUNCATE\\s+TABLE\\b")) {
            issues.add("Dangerous operation detected: TRUNCATE TABLE");
        }

        if (matchesSqlPattern(sql, "\\bDELETE\\s+FROM\\b")) {
            issues.add("Dangerous operation detected: DELETE FROM");
        }

        if (matchesSqlPattern(sql, "\\bALTER\\s+TABLE\\b.*\\bMODIFY\\b")) {
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

    private String normalizeSql(String sql) {
        return sql
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase();
    }

    private boolean matchesSqlPattern(String sql, String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
                .matcher(sql)
                .find();
    }

    private record ParsedChangeset(
            String id,
            String author,
            List<String> sqlLines,
            boolean hasRollback
    ) {
    }
}