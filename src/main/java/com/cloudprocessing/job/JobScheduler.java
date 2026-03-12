package com.cloudprocessing.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Polls for QUEUED jobs whose retry window has elapsed and submits them to
 * the job executor. This is the retry mechanism — new jobs are dispatched
 * immediately via JobCreatedEvent; this class handles re-queued retries.
 */
@Component
public class JobScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobScheduler.class);

    private final JobRepository jobRepository;
    private final CompressionWorker compressionWorker;
    private final Executor jobExecutor;

    public JobScheduler(JobRepository jobRepository,
                        CompressionWorker compressionWorker,
                        @Qualifier("jobExecutor") Executor jobExecutor) {
        this.jobRepository = jobRepository;
        this.compressionWorker = compressionWorker;
        this.jobExecutor = jobExecutor;
    }

    @Scheduled(fixedDelay = 30_000)
    public void dispatchQueuedJobs() {
        List<Job> ready = jobRepository.findQueuedJobsReadyForProcessing(Instant.now());
        if (ready.isEmpty()) return;

        log.info("Scheduler dispatching {} QUEUED job(s)", ready.size());
        for (Job job : ready) {
            UUID jobId              = job.getId();
            String key              = job.getFile().getStorageKey();
            String ct               = job.getFile().getContentType();
            String name             = job.getFile().getOriginalName();
            CompressionFormat fmt   = job.getCompressionFormat();
            jobExecutor.execute(() -> compressionWorker.processJob(jobId, key, ct, name, fmt));
        }
    }
}
