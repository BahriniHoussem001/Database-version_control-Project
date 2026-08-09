package com.dbvc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

@Service
public class EnvironmentJdbcTemplateProvider {

    @Value("${dbvc.environment.dev.url}")
    private String devUrl;

    @Value("${dbvc.environment.dev.username}")
    private String devUsername;

    @Value("${dbvc.environment.dev.password}")
    private String devPassword;

    @Value("${dbvc.environment.test.url}")
    private String testUrl;

    @Value("${dbvc.environment.test.username}")
    private String testUsername;

    @Value("${dbvc.environment.test.password}")
    private String testPassword;

    @Value("${dbvc.environment.prod.url}")
    private String prodUrl;

    @Value("${dbvc.environment.prod.username}")
    private String prodUsername;

    @Value("${dbvc.environment.prod.password}")
    private String prodPassword;

    public JdbcTemplate getJdbcTemplate(String environment) {
        String normalizedEnvironment = normalizeEnvironment(environment);

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("oracle.jdbc.OracleDriver");

        if ("DEV".equals(normalizedEnvironment)) {
            dataSource.setUrl(devUrl);
            dataSource.setUsername(devUsername);
            dataSource.setPassword(devPassword);
            return new JdbcTemplate(dataSource);
        }

        if ("TEST".equals(normalizedEnvironment)) {
            dataSource.setUrl(testUrl);
            dataSource.setUsername(testUsername);
            dataSource.setPassword(testPassword);
            return new JdbcTemplate(dataSource);
        }

        if ("PROD".equals(normalizedEnvironment)) {
            dataSource.setUrl(prodUrl);
            dataSource.setUsername(prodUsername);
            dataSource.setPassword(prodPassword);
            return new JdbcTemplate(dataSource);
        }

        throw new IllegalArgumentException("Unsupported environment: " + environment);
    }

    public String normalizeEnvironment(String environment) {
        if (environment == null || environment.trim().isEmpty()) {
            throw new IllegalArgumentException("environment is required");
        }

        String value = environment.trim().toUpperCase();

        if (!"DEV".equals(value) && !"TEST".equals(value) && !"PROD".equals(value)) {
            throw new IllegalArgumentException("environment must be DEV, TEST, or PROD");
        }

        return value;
    }
}