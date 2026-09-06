package com.dbvc.service;

import com.dbvc.dto.CreateGeneratedMigrationRequest;
import com.dbvc.dto.GeneratedMigrationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class GeneratedMigrationService {

    private static final Pattern CHANGESET_VERSION_PATTERN =
            Pattern.compile("--changeset\\s+[^:]+:V(\\d+)[A-Za-z0-9_-]*", Pattern.CASE_INSENSITIVE);

    private static final Pattern GENERATED_FILENAME_PATTERN =
            Pattern.compile("V(\\d+)__.*\\.sql", Pattern.CASE_INSENSITIVE);

    private final ArtifactStorageService artifactStorageService;

    @Value("${dbvc.liquibase.generated-changelog-directory:../liquibase/changelog/generated}")
    private String generatedChangelogDirectory;

    @Value("${dbvc.liquibase.application-changelog-path:../liquibase/changelog/db.application-changelog-master.sql}")
    private String applicationChangelogPath;

    public GeneratedMigrationResponse createMigration(CreateGeneratedMigrationRequest request) {
        String name = cleanRequired(request.getName(), "Migration name is required");
        String author = cleanRequired(request.getAuthor(), "Author is required");
        String sql = cleanRequired(request.getSql(), "SQL change is required");
        String rollbackSql = cleanRequired(request.getRollbackSql(), "Rollback SQL is required");

        validateMigrationContent(sql, "SQL change");
        validateMigrationContent(rollbackSql, "Rollback SQL");

        String slug = toSlug(name);
        int nextVersion = findNextVersion();
        String version = String.format("V%03d", nextVersion);
        String changesetId = version + "-" + slug;
        String filename = version + "__" + slug + ".sql";

        Path generatedDirectory = Paths.get(generatedChangelogDirectory).normalize();
        Path filePath = generatedDirectory.resolve(filename).normalize();

        if (!filePath.startsWith(generatedDirectory)) {
            throw new IllegalArgumentException("Invalid generated migration path");
        }

        try {
            Files.createDirectories(generatedDirectory);

            if (Files.exists(filePath)) {
                throw new IllegalStateException("Migration file already exists: " + filename);
            }

            String content = buildLiquibaseFormattedSql(author, changesetId, sql, rollbackSql);

            Files.writeString(
                    filePath,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW
            );

            String artifactKey = "generated/" + filename;

            artifactStorageService.uploadTextArtifact(
                    artifactKey,
                    content,
                    "text/x-sql"
            );

            return new GeneratedMigrationResponse(
                    changesetId,
                    filename,
                    "liquibase/changelog/generated/" + filename,
                    artifactStorageService.getBucket(),
                    artifactKey,
                    "Generated migration file created locally and archived in artifact storage"
            );

        } catch (IOException e) {
            throw new IllegalStateException("Unable to create generated migration file: " + e.getMessage(), e);
        }
    }

    private int findNextVersion() {
        int maxVersionFromMaster = findMaxVersionInMasterChangelog();
        int maxVersionFromGeneratedFiles = findMaxVersionInGeneratedDirectory();

        return Math.max(maxVersionFromMaster, maxVersionFromGeneratedFiles) + 1;
    }

    private int findMaxVersionInMasterChangelog() {
        Path masterPath = Paths.get(applicationChangelogPath).normalize();

        if (!Files.exists(masterPath)) {
            return 0;
        }

        try {
            String content = Files.readString(masterPath, StandardCharsets.UTF_8);
            Matcher matcher = CHANGESET_VERSION_PATTERN.matcher(content);

            int max = 0;

            while (matcher.find()) {
                int version = Integer.parseInt(matcher.group(1));
                max = Math.max(max, version);
            }

            return max;

        } catch (IOException e) {
            throw new IllegalStateException("Unable to read application changelog: " + e.getMessage(), e);
        }
    }

    private int findMaxVersionInGeneratedDirectory() {
        Path generatedDirectory = Paths.get(generatedChangelogDirectory).normalize();

        if (!Files.exists(generatedDirectory)) {
            return 0;
        }

        try (Stream<Path> files = Files.list(generatedDirectory)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .map(GENERATED_FILENAME_PATTERN::matcher)
                    .filter(Matcher::matches)
                    .map(matcher -> Integer.parseInt(matcher.group(1)))
                    .max(Comparator.naturalOrder())
                    .orElse(0);

        } catch (IOException e) {
            throw new IllegalStateException("Unable to scan generated migrations: " + e.getMessage(), e);
        }
    }

    private String buildLiquibaseFormattedSql(
            String author,
            String changesetId,
            String sql,
            String rollbackSql
    ) {
        return """
                --liquibase formatted sql

                --changeset %s:%s
                %s

                %s
                """
                .formatted(
                        author,
                        changesetId,
                        ensureSemicolon(sql),
                        formatRollbackSql(rollbackSql)
                );
    }

    private String formatRollbackSql(String rollbackSql) {
        String normalized = ensureSemicolon(rollbackSql);

        StringBuilder builder = new StringBuilder();

        for (String line : normalized.split("\\R")) {
            if (!line.trim().isEmpty()) {
                builder.append("--rollback ")
                        .append(line.trim())
                        .append(System.lineSeparator());
            }
        }

        return builder.toString().trim();
    }

    private String ensureSemicolon(String value) {
        String trimmed = value.trim();

        if (trimmed.endsWith(";")) {
            return trimmed;
        }

        return trimmed + ";";
    }

    private void validateMigrationContent(String content, String fieldName) {
        String normalized = content.trim().toLowerCase(Locale.ROOT);

        if (normalized.contains("--changeset")) {
            throw new IllegalArgumentException(fieldName + " must not contain Liquibase --changeset directive");
        }

        if (normalized.contains("--liquibase formatted sql")) {
            throw new IllegalArgumentException(fieldName + " must not contain Liquibase formatted SQL header");
        }

        if (normalized.contains("../") || normalized.contains("..\\")) {
            throw new IllegalArgumentException(fieldName + " contains invalid path characters");
        }
    }

    private String cleanRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private String toSlug(String value) {
        String slug = value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");

        if (slug.isBlank()) {
            throw new IllegalArgumentException("Migration name must contain letters or numbers");
        }

        return slug;
    }
}