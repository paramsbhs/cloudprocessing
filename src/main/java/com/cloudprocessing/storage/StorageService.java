package com.cloudprocessing.storage;

import com.cloudprocessing.config.AppProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;

/**
 * Thin abstraction over S3 presigned URL operations.
 * All other packages interact with object storage through this service.
 */
@Service
public class StorageService {

    private final S3Presigner s3Presigner;
    private final AppProperties appProperties;

    public StorageService(S3Presigner s3Presigner, AppProperties appProperties) {
        this.s3Presigner = s3Presigner;
        this.appProperties = appProperties;
    }

    /**
     * Generates a presigned PUT URL that allows the client to upload directly to S3.
     *
     * @param storageKey  S3 object key (e.g. "users/{userId}/files/{uuid}/{filename}")
     * @param contentType MIME type declared by the client (e.g. "application/pdf")
     * @return result containing the presigned URL and expiry metadata
     */
    public PresignedUploadResult generateUploadUrl(String storageKey, String contentType) {
        int expiryMinutes = appProperties.getS3().getPresignedUrlExpirationMinutes();
        String bucket = appProperties.getS3().getBucket();

        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(storageKey)
            .contentType(contentType)
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(expiryMinutes))
            .putObjectRequest(putRequest)
            .build();

        String url = s3Presigner.presignPutObject(presignRequest).url().toString();
        return new PresignedUploadResult(url, expiryMinutes, storageKey);
    }

    /**
     * Generates a presigned GET URL for downloading a file from S3.
     */
    public String generateDownloadUrl(String storageKey) {
        int expiryMinutes = appProperties.getS3().getPresignedUrlExpirationMinutes();
        String bucket = appProperties.getS3().getBucket();

        GetObjectRequest getRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(storageKey)
            .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(expiryMinutes))
            .getObjectRequest(getRequest)
            .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
