package com.dbvc.service;

import com.dbvc.dto.LiquibaseExecutionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;

@Service
public class LiquibaseCommandService {

    @Value("${dbvc.project-root}")
    private String projectRoot;

    public LiquibaseExecutionResult runUpdate() {
        String command = "docker compose run --rm liquibase update";

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "cmd.exe",
                    "/c",
                    command
            );

            processBuilder.directory(new File(projectRoot));

            // Merge normal output and Docker logs into one stream
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
    public LiquibaseExecutionResult previewRollbackLastChangeset() {
        String command = "docker compose run --rm liquibase rollback-count-sql --count=1";

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
    public LiquibaseExecutionResult rollbackLastChangeset() {
        String command = "docker compose run --rm liquibase rollback-count --count=1";

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
}
