package com.dbvc.worker;

import com.dbvc.service.MigrationExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MigrationExecutionWorker {

    private static final Logger log = LoggerFactory.getLogger(MigrationExecutionWorker.class);

    private final MigrationExecutionService migrationExecutionService;

    public MigrationExecutionWorker(MigrationExecutionService migrationExecutionService) {
        this.migrationExecutionService = migrationExecutionService;
    }

    @Scheduled(fixedDelayString = "${dbvc.execution-worker.fixed-delay-ms:10000}")
    public void processQueue() {
        try {
            migrationExecutionService.processNextQueuedRequestIfAvailable();
        } catch (Exception e) {
            log.error("Migration execution worker failed", e);
        }
    }
}