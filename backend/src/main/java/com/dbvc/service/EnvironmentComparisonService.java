package com.dbvc.service;

import com.dbvc.dto.ComparisonChangesetItem;
import com.dbvc.dto.EnvironmentComparisonResponse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EnvironmentComparisonService {

    private final EnvironmentJdbcTemplateProvider environmentJdbcTemplateProvider;

    public EnvironmentComparisonService(EnvironmentJdbcTemplateProvider environmentJdbcTemplateProvider) {
        this.environmentJdbcTemplateProvider = environmentJdbcTemplateProvider;
    }

    public EnvironmentComparisonResponse compare(String source, String target) {
        String sourceEnvironment = environmentJdbcTemplateProvider.normalizeEnvironment(source);
        String targetEnvironment = environmentJdbcTemplateProvider.normalizeEnvironment(target);

        if (sourceEnvironment.equals(targetEnvironment)) {
            throw new IllegalArgumentException("source and target environments must be different");
        }

        List<ComparisonChangesetItem> sourceChangesets =
                findExecutedChangesets(sourceEnvironment);

        List<ComparisonChangesetItem> targetChangesets =
                findExecutedChangesets(targetEnvironment);

        Map<String, ComparisonChangesetItem> sourceMap = sourceChangesets.stream()
                .collect(Collectors.toMap(this::changesetKey, item -> item));

        Map<String, ComparisonChangesetItem> targetMap = targetChangesets.stream()
                .collect(Collectors.toMap(this::changesetKey, item -> item));

        List<ComparisonChangesetItem> missingInTarget = sourceChangesets.stream()
                .filter(item -> !targetMap.containsKey(changesetKey(item)))
                .toList();

        List<ComparisonChangesetItem> extraInTarget = targetChangesets.stream()
                .filter(item -> !sourceMap.containsKey(changesetKey(item)))
                .toList();

        return EnvironmentComparisonResponse.builder()
                .sourceEnvironment(sourceEnvironment)
                .targetEnvironment(targetEnvironment)
                .inSync(missingInTarget.isEmpty() && extraInTarget.isEmpty())
                .sourceTotalMigrations(sourceChangesets.size())
                .targetTotalMigrations(targetChangesets.size())
                .missingInTargetCount(missingInTarget.size())
                .extraInTargetCount(extraInTarget.size())
                .missingInTarget(missingInTarget)
                .extraInTarget(extraInTarget)
                .build();
    }

    private List<ComparisonChangesetItem> findExecutedChangesets(String environment) {
        JdbcTemplate jdbcTemplate =
                environmentJdbcTemplateProvider.getJdbcTemplate(environment);

        String sql = """
                SELECT id,
                       author,
                       filename,
                       orderexecuted,
                       exectype
                FROM databasechangelog
                ORDER BY orderexecuted
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> ComparisonChangesetItem.builder()
                .id(rs.getString("id"))
                .author(rs.getString("author"))
                .filename(rs.getString("filename"))
                .orderExecuted(rs.getInt("orderexecuted"))
                .execType(rs.getString("exectype"))
                .build());
    }

    private String changesetKey(ComparisonChangesetItem item) {
        return item.getId() + "::" + item.getAuthor() + "::" + item.getFilename();
    }
}