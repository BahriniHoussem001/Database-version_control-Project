package com.dbvc.service;

import com.dbvc.dto.ArtifactStorageHealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class ArtifactStorageService {

    private final S3Client artifactS3Client;

    @Value("${dbvc.artifact-storage.enabled:true}")
    private boolean enabled;

    @Value("${dbvc.artifact-storage.provider:minio}")
    private String provider;

    @Value("${dbvc.artifact-storage.endpoint}")
    private String endpoint;

    @Value("${dbvc.artifact-storage.bucket}")
    private String bucket;

    public ArtifactStorageHealthResponse checkHealth() {
        try {
            artifactS3Client.headBucket(
                    HeadBucketRequest.builder()
                            .bucket(bucket)
                            .build()
            );

            return ArtifactStorageHealthResponse.builder()
                    .available(true)
                    .provider(provider)
                    .endpoint(endpoint)
                    .bucket(bucket)
                    .message("Artifact storage is reachable and bucket exists")
                    .build();

        } catch (Exception e) {
            return ArtifactStorageHealthResponse.builder()
                    .available(false)
                    .provider(provider)
                    .endpoint(endpoint)
                    .bucket(bucket)
                    .message("Artifact storage check failed: " + e.getMessage())
                    .build();
        }
    }

    public String uploadTextArtifact(String objectKey, String content, String contentType) {
        if (!enabled) {
            return null;
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        artifactS3Client.putObject(
                putObjectRequest,
                RequestBody.fromString(content, StandardCharsets.UTF_8)
        );

        return objectKey;
    }

    public String getBucket() {
        return bucket;
    }
}