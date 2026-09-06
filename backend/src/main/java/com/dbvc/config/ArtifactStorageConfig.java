package com.dbvc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class ArtifactStorageConfig {

    @Value("${dbvc.artifact-storage.endpoint}")
    private String endpoint;

    @Value("${dbvc.artifact-storage.region}")
    private String region;

    @Value("${dbvc.artifact-storage.access-key}")
    private String accessKey;

    @Value("${dbvc.artifact-storage.secret-key}")
    private String secretKey;

    @Value("${dbvc.artifact-storage.path-style-access:true}")
    private boolean pathStyleAccess;

    @Bean
    public S3Client artifactS3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(pathStyleAccess)
                                .build()
                )
                .build();
    }
}