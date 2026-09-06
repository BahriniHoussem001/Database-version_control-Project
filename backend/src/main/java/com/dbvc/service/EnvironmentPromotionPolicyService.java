package com.dbvc.service;

import com.dbvc.dto.EnvironmentPromotionStatusResponse;
import com.dbvc.dto.MigrationValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EnvironmentPromotionPolicyService {

    private final MigrationValidationService migrationValidationService;

    public List<EnvironmentPromotionStatusResponse> getPromotionStatuses() {
        return List.of(
                getPromotionStatus("DEV"),
                getPromotionStatus("TEST"),
                getPromotionStatus("PROD")
        );
    }

    public EnvironmentPromotionStatusResponse getPromotionStatus(String environment) {
        String normalizedEnvironment = normalizeEnvironment(environment);

        List<MigrationValidationResult> devPending = pendingMigrations("DEV");
        List<MigrationValidationResult> testPending = pendingMigrations("TEST");
        List<MigrationValidationResult> prodPending = pendingMigrations("PROD");

        return switch (normalizedEnvironment) {
            case "DEV" -> buildDevStatus(devPending);
            case "TEST" -> buildTestStatus(testPending, devPending);
            case "PROD" -> buildProdStatus(prodPending, testPending, devPending);
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported environment: " + environment
            );
        };
    }

    public void assertCanApply(String environment) {
        assertCanApply(environment, "NEXT");
    }

    public void assertCanApply(String environment, String executionMode) {
        String normalizedExecutionMode = normalizeExecutionMode(executionMode);
        EnvironmentPromotionStatusResponse status = getPromotionStatus(environment);

        if ("ALL".equals(normalizedExecutionMode)) {
            if (!status.isCanApplyAll()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        status.getApplyAllBlockedReason()
                );
            }

            return;
        }

        if (!status.isCanApply()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    status.getBlockedReason()
            );
        }
    }

    public int resolveAllowedUpdateCount(String environment, String executionMode) {
        String normalizedExecutionMode = normalizeExecutionMode(executionMode);
        EnvironmentPromotionStatusResponse status = getPromotionStatus(environment);

        if ("ALL".equals(normalizedExecutionMode)) {
            if (!status.isCanApplyAll()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        status.getApplyAllBlockedReason()
                );
            }

            return status.getPendingMigrations();
        }

        if (!status.isCanApply()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    status.getBlockedReason()
            );
        }

        return 1;
    }

    private EnvironmentPromotionStatusResponse buildDevStatus(List<MigrationValidationResult> devPending) {
        int pendingCount = devPending.size();
        String nextPendingMigrationId = nextPendingMigrationId(devPending);

        if (pendingCount == 0) {
            return EnvironmentPromotionStatusResponse.builder()
                    .environment("DEV")
                    .pendingMigrations(0)
                    .promotedMigrationsAvailable(0)
                    .nextPendingMigrationId(null)
                    .canApply(false)
                    .blockedReason("DEV has no pending migrations to apply.")
                    .canApplyAll(false)
                    .applyAllBlockedReason("DEV has no pending migrations to apply.")
                    .build();
        }

        return EnvironmentPromotionStatusResponse.builder()
                .environment("DEV")
                .pendingMigrations(pendingCount)
                .promotedMigrationsAvailable(pendingCount)
                .nextPendingMigrationId(nextPendingMigrationId)
                .canApply(true)
                .blockedReason(null)
                .canApplyAll(true)
                .applyAllBlockedReason(null)
                .build();
    }

    private EnvironmentPromotionStatusResponse buildTestStatus(
            List<MigrationValidationResult> testPending,
            List<MigrationValidationResult> devPending
    ) {
        int pendingCount = testPending.size();
        String nextPendingMigrationId = nextPendingMigrationId(testPending);

        if (pendingCount == 0) {
            return EnvironmentPromotionStatusResponse.builder()
                    .environment("TEST")
                    .pendingMigrations(0)
                    .promotedMigrationsAvailable(0)
                    .nextPendingMigrationId(null)
                    .canApply(false)
                    .blockedReason("TEST has no pending migrations to apply.")
                    .canApplyAll(false)
                    .applyAllBlockedReason("TEST has no pending migrations to apply.")
                    .build();
        }

        int promotedAvailableCount = countPromotedPendingPrefix(testPending, devPending);

        boolean canApplyNext = promotedAvailableCount > 0;
        boolean canApplyAll = devPending.isEmpty();

        return EnvironmentPromotionStatusResponse.builder()
                .environment("TEST")
                .pendingMigrations(pendingCount)
                .promotedMigrationsAvailable(promotedAvailableCount)
                .nextPendingMigrationId(nextPendingMigrationId)
                .canApply(canApplyNext)
                .blockedReason(
                        canApplyNext
                                ? null
                                : "TEST is waiting for DEV to apply the next migration first: "
                                + nextPendingMigrationId
                )
                .canApplyAll(canApplyAll)
                .applyAllBlockedReason(
                        canApplyAll
                                ? null
                                : "Apply all is blocked for TEST because DEV still has "
                                + devPending.size()
                                + " pending migration"
                                + (devPending.size() == 1 ? "" : "s")
                                + ". Finish applying all pending migrations in DEV first, or use NEXT mode for migrations already applied in DEV."
                )
                .build();
    }

    private EnvironmentPromotionStatusResponse buildProdStatus(
            List<MigrationValidationResult> prodPending,
            List<MigrationValidationResult> testPending,
            List<MigrationValidationResult> devPending
    ) {
        int pendingCount = prodPending.size();
        String nextPendingMigrationId = nextPendingMigrationId(prodPending);

        if (pendingCount == 0) {
            return EnvironmentPromotionStatusResponse.builder()
                    .environment("PROD")
                    .pendingMigrations(0)
                    .promotedMigrationsAvailable(0)
                    .nextPendingMigrationId(null)
                    .canApply(false)
                    .blockedReason("PROD has no pending migrations to apply.")
                    .canApplyAll(false)
                    .applyAllBlockedReason("PROD has no pending migrations to apply.")
                    .build();
        }

        int promotedAvailableCount = countPromotedPendingPrefix(prodPending, testPending);

        boolean canApplyNext = promotedAvailableCount > 0;
        boolean canApplyAll = devPending.isEmpty() && testPending.isEmpty();

        return EnvironmentPromotionStatusResponse.builder()
                .environment("PROD")
                .pendingMigrations(pendingCount)
                .promotedMigrationsAvailable(promotedAvailableCount)
                .nextPendingMigrationId(nextPendingMigrationId)
                .canApply(canApplyNext)
                .blockedReason(
                        canApplyNext
                                ? null
                                : "PROD is waiting for TEST to apply the next migration first: "
                                + nextPendingMigrationId
                )
                .canApplyAll(canApplyAll)
                .applyAllBlockedReason(
                        canApplyAll
                                ? null
                                : buildProdApplyAllBlockedReason(devPending.size(), testPending.size())
                )
                .build();
    }

    private String buildProdApplyAllBlockedReason(int devPendingCount, int testPendingCount) {
        if (devPendingCount > 0 && testPendingCount > 0) {
            return "Apply all is blocked for PROD because DEV still has "
                    + devPendingCount
                    + " pending migration"
                    + (devPendingCount == 1 ? "" : "s")
                    + " and TEST still has "
                    + testPendingCount
                    + " pending migration"
                    + (testPendingCount == 1 ? "" : "s")
                    + ". Finish DEV and TEST first, or use NEXT mode for migrations already applied in TEST.";
        }

        if (testPendingCount > 0) {
            return "Apply all is blocked for PROD because TEST still has "
                    + testPendingCount
                    + " pending migration"
                    + (testPendingCount == 1 ? "" : "s")
                    + ". Finish applying all pending migrations in TEST first, or use NEXT mode for migrations already applied in TEST.";
        }

        return "Apply all is blocked for PROD because DEV still has "
                + devPendingCount
                + " pending migration"
                + (devPendingCount == 1 ? "" : "s")
                + ". The environment chain must be fully promoted before applying all to PROD.";
    }

    private int countPromotedPendingPrefix(
            List<MigrationValidationResult> currentPending,
            List<MigrationValidationResult> previousEnvironmentPending
    ) {
        Set<String> previousPendingIds = new HashSet<>();

        for (MigrationValidationResult migration : previousEnvironmentPending) {
            if (migration.getId() != null) {
                previousPendingIds.add(migration.getId());
            }
        }

        int count = 0;

        for (MigrationValidationResult migration : currentPending) {
            String migrationId = migration.getId();

            if (migrationId == null || previousPendingIds.contains(migrationId)) {
                break;
            }

            count++;
        }

        return count;
    }

    private List<MigrationValidationResult> pendingMigrations(String environment) {
        return migrationValidationService.validatePendingMigrationsByEnvironment(environment);
    }

    private String nextPendingMigrationId(List<MigrationValidationResult> pendingMigrations) {
        if (pendingMigrations == null || pendingMigrations.isEmpty()) {
            return null;
        }

        return pendingMigrations.get(0).getId();
    }

    private String normalizeEnvironment(String environment) {
        if (environment == null || environment.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Environment is required."
            );
        }

        String value = environment.trim().toUpperCase();

        if (!"DEV".equals(value) && !"TEST".equals(value) && !"PROD".equals(value)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Environment must be DEV, TEST, or PROD."
            );
        }

        return value;
    }

    private String normalizeExecutionMode(String executionMode) {
        if (executionMode == null || executionMode.isBlank()) {
            return "NEXT";
        }

        String value = executionMode.trim().toUpperCase();

        if (!"NEXT".equals(value) && !"ALL".equals(value)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Execution mode must be NEXT or ALL."
            );
        }

        return value;
    }
}