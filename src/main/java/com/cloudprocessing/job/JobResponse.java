package com.cloudprocessing.job;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobResponse(
    UUID jobId,
    UUID fileId,
    UUID userId,
    String originalName,
    JobStatus status,
    int attemptCount,
    int maxAttempts,
    String errorMessage,
    Instant processingStartedAt,
    Instant processingCompletedAt,
    Instant createdAt,
    Instant updatedAt,
    CompressionFormat compressionFormat,
    Double compressionRatio,   // 1.0 - (outputSizeBytes / sizeBytes); null until COMPLETED
    Long processingTimeMs      // null until COMPLETED
) {
    public static JobResponse from(Job job) {
        var file = job.getFile();
        Double ratio = null;
        if (file.getSizeBytes() != null && file.getOutputSizeBytes() != null
                && file.getSizeBytes() > 0) {
            ratio = 1.0 - ((double) file.getOutputSizeBytes() / file.getSizeBytes());
        }
        Long processingTimeMs = null;
        if (job.getProcessingStartedAt() != null && job.getProcessingCompletedAt() != null) {
            processingTimeMs = job.getProcessingCompletedAt().toEpochMilli()
                - job.getProcessingStartedAt().toEpochMilli();
        }
        return new JobResponse(
            job.getId(),
            file.getId(),
            job.getUser().getId(),
            file.getOriginalName(),
            job.getStatus(),
            job.getAttemptCount(),
            job.getMaxAttempts(),
            job.getErrorMessage(),
            job.getProcessingStartedAt(),
            job.getProcessingCompletedAt(),
            job.getCreatedAt(),
            job.getUpdatedAt(),
            job.getCompressionFormat(),
            ratio,
            processingTimeMs
        );
    }
}
