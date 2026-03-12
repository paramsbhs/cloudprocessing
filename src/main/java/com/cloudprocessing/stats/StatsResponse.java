package com.cloudprocessing.stats;

public record StatsResponse(
    long totalFiles,
    long totalJobs,
    long completedJobs,
    long failedJobs,
    long processingJobs,
    long queuedJobs,
    double avgCompressionPercent,   // e.g. 42.3 means 42.3% smaller
    long avgProcessingTimeMs,
    long totalBytesSaved
) {}
