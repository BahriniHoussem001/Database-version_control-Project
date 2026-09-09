package com.dbvc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class ArtifactStorageConfig {

    @Value("${dbvc.artifact-storage.endpoint:}")
    private String endpoint;

    @Value("${dbvc.artifact-storage.region}")
    private String region;

    @Value("${dbvc.artifact-storage.credentials-mode:static}")
    private String credentialsMode;

    @Value("${dbvc.artifact-storage.access-key:}")
    private String accessKey;

    @Value("${dbvc.artifact-storage.secret-key:}")
    private String secretKey;

    @Value("${dbvc.artifact-storage.path-style-access:true}")
    private boolean pathStyleAccess;

    @Bean
    public S3Client artifactS3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(resolveCredentialsProvider())
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(pathStyleAccess)
                                .build()
                );

        if (hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint.trim()));
        }

        return builder.build();
    }

    private AwsCredentialsProvider resolveCredentialsProvider() {
        if ("default".equalsIgnoreCase(credentialsMode)) {
            return DefaultCredentialsProvider.create();
        }

        if (!hasText(accessKey) || !hasText(secretKey)) {
            throw new IllegalStateException(
                    "Artifact storage static credentials require access-key and secret-key."
            );
        }

        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                        accessKey.trim(),
                        secretKey.trim()
                )
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}