package com.dbvc.service;

import com.dbvc.dto.CreateMigrationExecutionRequest;
import com.dbvc.dto.LiquibaseExecutionResult;
import com.dbvc.dto.MigrationExecutionResponse;
import com.dbvc.dto.MigrationExecutionSummaryResponse;
import com.dbvc.dto.MigrationValidationResult;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

@Service
public class MigrationExecutionService {

    private final JdbcTemplate jdbcTemplate;
    private final MigrationValidationService migrationValidationService;

    @Value("${dbvc.project-root}")
    private String projectRoot;

    public MigrationExecutionService(
            JdbcTemplate jdbcTemplate,
            MigrationValidationService migrationValidationService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.migrationValidationService = migrationValidationService;
    }

    public MigrationExecutionResponse createExecutionRequest(CreateMigrationExecutionRequest request) {
        String environment = normalizeEnvironment(request.getEnvironment());
        String requestType = normalizeRequestType(request.getRequestType());
        String priority = normalizePriority(request.getPriority());
        String reason = cleanText(request.getReason());
        String requestedBy = cleanText(request.getRequestedBy());

        if (requestedBy == null) {
            requestedBy = "SYSTEM";
        }

        if ("URGENT".equals(priority) && reason == null) {
            throw new IllegalArgumentException("Reason is required when priority is URGENT");
        }

        String command = resolveCommand(environment, requestType);
        String validationSummary = "Request accepted and queued";

        String sql = """
                INSERT INTO migration_execution_request (
                    environment,
                    request_type,
                    priority,
                    reason,
                    status,
                    command,
                    validation_summary,
                    requested_by,
                    requested_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        String finalRequestedBy = requestedBy;

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"ID"});
            ps.setString(1, environment);
            ps.setString(2, requestType);
            ps.setString(3, priority);
            ps.setString(4, reason);
            ps.setString(5, "QUEUED");
            ps.setString(6, command);
            ps.setString(7, validationSummary);
            ps.setString(8, finalRequestedBy);
            return ps;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException("Unable to retrieve generated execution request ID");
        }

        return findById(generatedId.longValue());
    }

    public List<MigrationExecutionResponse> findAll() {
        String sql = """
                SELECT *
                FROM migration_execution_request
                ORDER BY requested_at DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapExecutionResponse(rs));
    }

    public MigrationExecutionResponse findById(Long id) {
        String sql = """
                SELECT *
                FROM migration_execution_request
                WHERE id = ?
                """;

        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return mapExecutionResponse(rs);
            }

            throw new IllegalArgumentException("Migration execution request not found with id: " + id);
        }, id);
    }

    public MigrationExecutionResponse processNextQueuedRequestIfAvailable() {
        Integer runningCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM migration_execution_request
                WHERE status = 'RUNNING'
                """,
                Integer.class
        );

        if (runningCount != null && runningCount > 0) {
            return null;
        }

        Long nextRequestId = findNextQueuedRequestId();

        if (nextRequestId == null) {
            return null;
        }

        return processNextQueuedRequest();
    }

    public MigrationExecutionResponse processNextQueuedRequest() {
        Integer runningCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM migration_execution_request
                WHERE status = 'RUNNING'
                """,
                Integer.class
        );

        if (runningCount != null && runningCount > 0) {
            throw new IllegalStateException("A migration execution request is already running");
        }

        Long nextRequestId = findNextQueuedRequestId();

        if (nextRequestId == null) {
            throw new IllegalStateException("No queued migration execution request found");
        }

        MigrationExecutionResponse request = findById(nextRequestId);

        long startTime = System.currentTimeMillis();

        markAsRunning(nextRequestId);

        try {
            String validationSummary = validateBeforeExecution(request);

            LiquibaseExecutionResult result = executeRequest(request);

            long durationMs = System.currentTimeMillis() - startTime;

            String finalStatus = result.isSuccess() ? "SUCCESS" : "FAILED";

            jdbcTemplate.update(
                    """
                    UPDATE migration_execution_request
                    SET status = ?,
                        exit_code = ?,
                        output = ?,
                        error = ?,
                        validation_summary = ?,
                        finished_at = CURRENT_TIMESTAMP,
                        duration_ms = ?
                    WHERE id = ?
                    """,
                    finalStatus,
                    result.getExitCode(),
                    result.getOutput(),
                    result.getError(),
                    validationSummary,
                    durationMs,
                    nextRequestId
            );

            return findById(nextRequestId);

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;

            jdbcTemplate.update(
                    """
                    UPDATE migration_execution_request
                    SET status = 'FAILED',
                        error = ?,
                        finished_at = CURRENT_TIMESTAMP,
                        duration_ms = ?
                    WHERE id = ?
                    """,
                    e.getMessage(),
                    durationMs,
                    nextRequestId
            );

            return findById(nextRequestId);
        }
    }

    public MigrationExecutionSummaryResponse getExecutionSummary() {
        Integer totalRequests = countRequestsByStatus(null);
        Integer queuedRequests = countRequestsByStatus("QUEUED");
        Integer runningRequests = countRequestsByStatus("RUNNING");
        Integer successfulRequests = countRequestsByStatus("SUCCESS");
        Integer failedRequests = countRequestsByStatus("FAILED");

        String latestSql = """
                SELECT id,
                       request_type,
                       status,
                       priority,
                       requested_at,
                       finished_at
                FROM migration_execution_request
                ORDER BY requested_at DESC
                FETCH FIRST 1 ROWS ONLY
                """;

        return jdbcTemplate.query(latestSql, rs -> {
            if (rs.next()) {
                return MigrationExecutionSummaryResponse.builder()
                        .totalRequests(totalRequests != null ? totalRequests : 0)
                        .queuedRequests(queuedRequests != null ? queuedRequests : 0)
                        .runningRequests(runningRequests != null ? runningRequests : 0)
                        .successfulRequests(successfulRequests != null ? successfulRequests : 0)
                        .failedRequests(failedRequests != null ? failedRequests : 0)
                        .latestRequestId(rs.getLong("id"))
                        .latestRequestType(rs.getString("request_type"))
                        .latestStatus(rs.getString("status"))
                        .latestPriority(rs.getString("priority"))
                        .latestRequestedAt(toLocalDateTime(rs.getTimestamp("requested_at")))
                        .latestFinishedAt(toLocalDateTime(rs.getTimestamp("finished_at")))
                        .build();
            }

            return MigrationExecutionSummaryResponse.builder()
                    .totalRequests(0)
                    .queuedRequests(0)
                    .runningRequests(0)
                    .successfulRequests(0)
                    .failedRequests(0)
                    .latestRequestId(null)
                    .latestRequestType(null)
                    .latestStatus(null)
                    .latestPriority(null)
                    .latestRequestedAt(null)
                    .latestFinishedAt(null)
                    .build();
        });
    }

    private MigrationExecutionResponse mapExecutionResponse(java.sql.ResultSet rs) throws java.sql.SQLException {
        return MigrationExecutionResponse.builder()
                .id(rs.getLong("id"))
                .environment(rs.getString("environment"))
                .requestType(rs.getString("request_type"))
                .priority(rs.getString("priority"))
                .reason(rs.getString("reason"))
                .status(rs.getString("status"))
                .command(rs.getString("command"))
                .exitCode(rs.getObject("exit_code") != null ? rs.getInt("exit_code") : null)
                .output(rs.getString("output"))
                .error(rs.getString("error"))
                .validationSummary(rs.getString("validation_summary"))
                .requestedBy(rs.getString("requested_by"))
                .requestedAt(toLocalDateTime(rs.getTimestamp("requested_at")))
                .startedAt(toLocalDateTime(rs.getTimestamp("started_at")))
                .finishedAt(toLocalDateTime(rs.getTimestamp("finished_at")))
                .durationMs(rs.getObject("duration_ms") != null ? rs.getLong("duration_ms") : null)
                .build();
    }

    private Long findNextQueuedRequestId() {
        String sql = """
                SELECT id
                FROM migration_execution_request
                WHERE status = 'QUEUED'
                ORDER BY
                    CASE priority
                        WHEN 'URGENT' THEN 1
                        ELSE 2
                    END,
                    requested_at ASC
                FETCH FIRST 1 ROWS ONLY
                """;

        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return rs.getLong("id");
            }

            return null;
        });
    }

    private void markAsRunning(Long id) {
        jdbcTemplate.update(
                """
                UPDATE migration_execution_request
                SET status = 'RUNNING',
                    started_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                id
        );
    }

    private String validateBeforeExecution(MigrationExecutionResponse request) {
        if (!"UPDATE".equals(request.getRequestType())) {
            return "No validation required for request type: " + request.getRequestType()
                    + " on environment: " + request.getEnvironment();
        }

        List<MigrationValidationResult> validationResults =
                migrationValidationService.validatePendingMigrationsByEnvironment(request.getEnvironment());

        boolean hasValidationProblems = validationResults.stream()
                .anyMatch(result -> !"VALID".equals(result.getStatus()));

        if (hasValidationProblems) {
            throw new IllegalStateException(
                    "Migration validation failed for environment: "
                            + request.getEnvironment()
                            + ". Please check /api/environments/"
                            + request.getEnvironment()
                            + "/migrations/pending/validation before applying."
            );
        }

        return "Validation passed before execution for environment: "
                + request.getEnvironment()
                + ". Pending changesets checked: "
                + validationResults.size();
    }

    private LiquibaseExecutionResult executeRequest(MigrationExecutionResponse request) {
        if ("ROLLBACK".equals(request.getRequestType())) {
            LiquibaseExecutionResult previewResult = runCommand(resolveRollbackPreviewCommand(request.getEnvironment()));

            if (!previewResult.isSuccess()) {
                return LiquibaseExecutionResult.builder()
                        .success(false)
                        .command(request.getCommand())
                        .exitCode(-1)
                        .output(previewResult.getOutput())
                        .error("Rollback preview failed. Real rollback was not executed.")
                        .build();
            }

            if (isRollbackTouchingProtectedTables(previewResult.getOutput())) {
                return LiquibaseExecutionResult.builder()
                        .success(false)
                        .command(request.getCommand())
                        .exitCode(-1)
                        .output(previewResult.getOutput())
                        .error("Rollback blocked because it would affect DBVC internal tables.")
                        .build();
            }
        }

        return runCommand(request.getCommand());
    }

    private LiquibaseExecutionResult runCommand(String command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "cmd.exe",
                    "/c",
                    command
            );

            processBuilder.directory(new File(projectRoot));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            int exitCode = process.waitFor();

            return LiquibaseExecutionResult.builder()
                    .success(exitCode == 0)
                    .command(command)
                    .exitCode(exitCode)
                    .output(output)
                    .error(exitCode == 0 ? null : output)
                    .build();

        } catch (Exception e) {
            return LiquibaseExecutionResult.builder()
                    .success(false)
                    .command(command)
                    .exitCode(-1)
                    .output(null)
                    .error(e.getMessage())
                    .build();
        }
    }

    private boolean isRollbackTouchingProtectedTables(String rollbackPreviewOutput) {
        if (rollbackPreviewOutput == null) {
            return false;
        }

        String normalizedOutput = rollbackPreviewOutput
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase();

        return normalizedOutput.contains("MIGRATION_EXECUTION_REQUEST");
    }

    private String resolveCommand(String environment, String requestType) {
        String defaultsFile = resolveEnvironmentDefaultsFile(environment);

        if ("UPDATE".equals(requestType)) {
            return "docker compose run --rm liquibase --defaults-file=" + defaultsFile + " update";
        }

        if ("ROLLBACK".equals(requestType)) {
            return "docker compose run --rm liquibase --defaults-file=" + defaultsFile + " rollback-count --count=1";
        }

        throw new IllegalArgumentException("Unsupported requestType: " + requestType);
    }

    private String resolveRollbackPreviewCommand(String environment) {
        String defaultsFile = resolveEnvironmentDefaultsFile(environment);

        return "docker compose run --rm liquibase --defaults-file="
                + defaultsFile
                + " rollback-count-sql --count=1";
    }

    private String resolveEnvironmentDefaultsFile(String environment) {
        if ("DEV".equals(environment)) {
            return "/liquibase/project/environments/liquibase-dev.properties";
        }

        if ("TEST".equals(environment)) {
            return "/liquibase/project/environments/liquibase-test.properties";
        }

        if ("PROD".equals(environment)) {
            return "/liquibase/project/environments/liquibase-prod.properties";
        }

        throw new IllegalArgumentException("Unsupported environment: " + environment);
    }

    private String normalizeEnvironment(String environment) {
        String value = cleanText(environment);

        if (value == null) {
            throw new IllegalArgumentException("environment is required");
        }

        value = value.toUpperCase();

        if (!"DEV".equals(value) && !"TEST".equals(value) && !"PROD".equals(value)) {
            throw new IllegalArgumentException("environment must be DEV, TEST, or PROD");
        }

        return value;
    }

    private String normalizeRequestType(String requestType) {
        String value = cleanText(requestType);

        if (value == null) {
            throw new IllegalArgumentException("requestType is required");
        }

        value = value.toUpperCase();

        if (!"UPDATE".equals(value) && !"ROLLBACK".equals(value)) {
            throw new IllegalArgumentException("requestType must be UPDATE or ROLLBACK");
        }

        return value;
    }

    private String normalizePriority(String priority) {
        String value = cleanText(priority);

        if (value == null) {
            return "NORMAL";
        }

        value = value.toUpperCase();

        if (!"NORMAL".equals(value) && !"URGENT".equals(value)) {
            throw new IllegalArgumentException("priority must be NORMAL or URGENT");
        }

        return value;
    }

    private Integer countRequestsByStatus(String status) {
        if (status == null) {
            return jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM migration_execution_request",
                    Integer.class
            );
        }

        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM migration_execution_request
                WHERE status = ?
                """,
                Integer.class,
                status
        );
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private String cleanText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}