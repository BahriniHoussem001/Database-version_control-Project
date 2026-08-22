package com.dbvc.service;

import com.dbvc.dto.EnvironmentPromotionStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnvironmentPromotionPolicyService {

    private final MigrationHistoryService migrationHistoryService;

    public List<EnvironmentPromotionStatusResponse> getPromotionStatuses() {
        return List.of(
                getPromotionStatus("DEV"),
                getPromotionStatus("TEST"),
                getPromotionStatus("PROD")
        );
    }

    public EnvironmentPromotionStatusResponse getPromotionStatus(String environment) {
        String normalizedEnvironment = normalizeEnvironment(environment);

        int devPending = pendingCount("DEV");
        int testPending = pendingCount("TEST");
        int prodPending = pendingCount("PROD");

        return switch (normalizedEnvironment) {
            case "DEV" -> buildDevStatus(devPending);
            case "TEST" -> buildTestStatus(testPending, devPending);
            case "PROD" -> buildProdStatus(prodPending, devPending, testPending);
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported environment: " + environment
            );
        };
    }

    public void assertCanApply(String environment) {
        EnvironmentPromotionStatusResponse status = getPromotionStatus(environment);

        if (!status.isCanApply()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    status.getBlockedReason()
            );
        }
    }

    private EnvironmentPromotionStatusResponse buildDevStatus(int devPending) {
        if (devPending == 0) {
            return new EnvironmentPromotionStatusResponse(
                    "DEV",
                    devPending,
                    false,
                    "DEV has no pending migrations to apply."
            );
        }

        return new EnvironmentPromotionStatusResponse(
                "DEV",
                devPending,
                true,
                null
        );
    }

    private EnvironmentPromotionStatusResponse buildTestStatus(int testPending, int devPending) {
        if (testPending == 0) {
            return new EnvironmentPromotionStatusResponse(
                    "TEST",
                    testPending,
                    false,
                    "TEST has no pending migrations to apply."
            );
        }

        if (devPending > 0) {
            return new EnvironmentPromotionStatusResponse(
                    "TEST",
                    testPending,
                    false,
                    "DEV must be updated before applying migrations to TEST."
            );
        }

        return new EnvironmentPromotionStatusResponse(
                "TEST",
                testPending,
                true,
                null
        );
    }

    private EnvironmentPromotionStatusResponse buildProdStatus(
            int prodPending,
            int devPending,
            int testPending
    ) {
        if (prodPending == 0) {
            return new EnvironmentPromotionStatusResponse(
                    "PROD",
                    prodPending,
                    false,
                    "PROD has no pending migrations to apply."
            );
        }

        if (devPending > 0 && testPending > 0) {
            return new EnvironmentPromotionStatusResponse(
                    "PROD",
                    prodPending,
                    false,
                    "DEV and TEST must be updated before applying migrations to PROD."
            );
        }

        if (devPending > 0) {
            return new EnvironmentPromotionStatusResponse(
                    "PROD",
                    prodPending,
                    false,
                    "DEV must be updated before applying migrations to PROD."
            );
        }

        if (testPending > 0) {
            return new EnvironmentPromotionStatusResponse(
                    "PROD",
                    prodPending,
                    false,
                    "TEST must be updated before applying migrations to PROD."
            );
        }

        return new EnvironmentPromotionStatusResponse(
                "PROD",
                prodPending,
                true,
                null
        );
    }

    private int pendingCount(String environment) {
        return migrationHistoryService
                .findPendingMigrationsByEnvironment(environment)
                .size();
    }

    private String normalizeEnvironment(String environment) {
        if (environment == null || environment.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Environment is required."
            );
        }

        return environment.trim().toUpperCase();
    }
}