package com.cloudprocessing.storage;

import com.cloudprocessing.config.AppProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Optional;

/**
 * Thin abstraction over S3 operations.
 * All other packages interact with object storage through this service only.
 */
@Service
public class StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AppProperties appProperties;

    public StorageService(S3Client s3Client,
                          S3Presigner s3Presigner,
                          AppProperties appProperties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.appProperties = appProperties;
    }

    /**
     * Generates a presigned PUT URL so the client can upload directly to S3
     * without needing AWS credentials.
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
     * Generates a presigned GET URL so the client can download a file directly from S3.
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

    /**
     * Verifies an object exists in S3 and returns its actual size in bytes.
     * Used to confirm the client actually completed the upload.
     *
     * @return actual size in bytes from S3 metadata, or empty if the object does not exist
     */
    public Optional<Long> headObject(String storageKey) {
        try {
            HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(appProperties.getS3().getBucket())
                .key(storageKey)
                .build());
            return Optional.of(head.contentLength());
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    /**
     * Downloads an object from S3 and returns its bytes.
     * Used by compression workers to fetch the original file.
     */
    public byte[] downloadObject(String storageKey) {
        return s3Client.getObjectAsBytes(GetObjectRequest.builder()
            .bucket(appProperties.getS3().getBucket())
            .key(storageKey)
            .build()).asByteArray();
    }

    /**
     * Uploads raw bytes to S3 and returns the number of bytes stored.
     * Used by compression workers to persist the compressed output.
     */
    public long uploadObject(String storageKey, byte[] data, String contentType) {
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(appProperties.getS3().getBucket())
                .key(storageKey)
                .contentType(contentType)
                .contentLength((long) data.length)
                .build(),
            RequestBody.fromBytes(data));
        return data.length;
    }

    /**
     * Permanently deletes an object from S3.
     * Silently succeeds if the key does not exist.
     */
    public void deleteObject(String storageKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(appProperties.getS3().getBucket())
            .key(storageKey)
            .build());
    }
}
