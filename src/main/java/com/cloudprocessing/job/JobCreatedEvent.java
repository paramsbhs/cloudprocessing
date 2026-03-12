package com.cloudprocessing.job;

import java.util.UUID;

/**
 * Published after a Job is saved and the transaction commits.
 * Carries only primitives — no JPA entities — so it is safe to pass
 * across thread and transaction boundaries.
 */
public record JobCreatedEvent(
    UUID jobId,
    String storageKey,
    String contentType,
    String originalName,
    CompressionFormat compressionFormat
) {}
