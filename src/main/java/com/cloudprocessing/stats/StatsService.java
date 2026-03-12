package com.cloudprocessing.stats;

import com.cloudprocessing.file.FileRepository;
import com.cloudprocessing.job.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class StatsService {

    private final FileRepository fileRepository;
    private final JobRepository jobRepository;

    public StatsService(FileRepository fileRepository, JobRepository jobRepository) {
        this.fileRepository = fileRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional(readOnly = true)
    public StatsResponse getStats(UUID userId) {
        long totalFiles    = fileRepository.countByUserId(userId);
        long totalBytesSaved = fileRepository.totalBytesSaved(userId);
        double rawRatio    = fileRepository.avgCompressionRatio(userId);
        double avgProcessingTimeMs = jobRepository.avgProcessingTimeMs(userId);

        List<Object[]> rows = jobRepository.countByStatusForUser(userId);
        long totalJobs = 0, completedJobs = 0, failedJobs = 0, processingJobs = 0, queuedJobs = 0;
        for (Object[] row : rows) {
            String status = (String) row[0];
            long count    = ((Number) row[1]).longValue();
            totalJobs    += count;
            switch (status) {
                case "COMPLETED"  -> completedJobs  = count;
                case "FAILED"     -> failedJobs     = count;
                case "PROCESSING" -> processingJobs = count;
                case "QUEUED"     -> queuedJobs     = count;
            }
        }

        // Round avg compression to 1 decimal place as a percentage
        double avgCompressionPercent = Math.round(rawRatio * 1000.0) / 10.0;

        return new StatsResponse(
            totalFiles,
            totalJobs,
            completedJobs,
            failedJobs,
            processingJobs,
            queuedJobs,
            avgCompressionPercent,
            Math.round(avgProcessingTimeMs),
            totalBytesSaved
        );
    }
}
