package com.dbvc.service;

import com.dbvc.dto.CreateMigrationExecutionRequest;
import com.dbvc.dto.MigrationExecutionResponse;
import com.dbvc.dto.LiquibaseExecutionResult;
import com.dbvc.dto.MigrationValidationResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

@Service
public class MigrationExecutionService {
	private final LiquibaseCommandService liquibaseCommandService;
	private final MigrationValidationService migrationValidationService;

    private final JdbcTemplate jdbcTemplate;

    public MigrationExecutionService(
            JdbcTemplate jdbcTemplate,
            LiquibaseCommandService liquibaseCommandService,
            MigrationValidationService migrationValidationService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.liquibaseCommandService = liquibaseCommandService;
        this.migrationValidationService = migrationValidationService;
    }

    public MigrationExecutionResponse createExecutionRequest(CreateMigrationExecutionRequest request) {
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

        String command = resolveCommand(requestType);
        String validationSummary = "Request accepted and queued";

        String sql = """
                INSERT INTO migration_execution_request (
                    request_type,
                    priority,
                    reason,
                    status,
                    command,
                    validation_summary,
                    requested_by,
                    requested_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        String finalRequestedBy = requestedBy;
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"ID"});
            ps.setString(1, requestType);
            ps.setString(2, priority);
            ps.setString(3, reason);
            ps.setString(4, "QUEUED");
            ps.setString(5, command);
            ps.setString(6, validationSummary);
            ps.setString(7, finalRequestedBy);
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

    private MigrationExecutionResponse mapExecutionResponse(java.sql.ResultSet rs) throws java.sql.SQLException {
        return MigrationExecutionResponse.builder()
                .id(rs.getLong("id"))
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

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
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

    private String resolveCommand(String requestType) {
        if ("UPDATE".equals(requestType)) {
            return "docker compose run --rm liquibase update";
        }

        if ("ROLLBACK".equals(requestType)) {
            return "docker compose run --rm liquibase rollback-count --count=1";
        }

        throw new IllegalArgumentException("Unsupported requestType: " + requestType);
    }

    private String cleanText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
    
    ///////////
    
    //////////////
    
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
            return "No validation required for request type: " + request.getRequestType();
        }

        List<MigrationValidationResult> validationResults =
                migrationValidationService.validatePendingMigrations();

        boolean hasProblems = validationResults.stream()
                .anyMatch(result -> !"VALID".equals(result.getStatus()));

        if (hasProblems) {
            throw new IllegalStateException("Migration validation failed. Please check pending migration validation results.");
        }

        return "Validation passed before execution";
    }

    private LiquibaseExecutionResult executeRequest(MigrationExecutionResponse request) {
        if ("UPDATE".equals(request.getRequestType())) {
            return liquibaseCommandService.runUpdate();
        }

        if ("ROLLBACK".equals(request.getRequestType())) {
            return liquibaseCommandService.rollbackLastChangeset();
        }

        throw new IllegalArgumentException("Unsupported request type: " + request.getRequestType());
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
  }
    
