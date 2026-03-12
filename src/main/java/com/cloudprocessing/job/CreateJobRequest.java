package com.cloudprocessing.job;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateJobRequest(
    @NotNull UUID fileId,
    CompressionFormat compressionFormat   // null → GZIP default
) {}
