package com.cloudprocessing.job;

import com.cloudprocessing.auth.UserRepository;
import com.cloudprocessing.common.AppException;
import com.cloudprocessing.file.FileRecord;
import com.cloudprocessing.file.FileRepository;
import com.cloudprocessing.file.FileStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public JobService(JobRepository jobRepository,
                      FileRepository fileRepository,
                      UserRepository userRepository,
                      ApplicationEventPublisher eventPublisher) {
        this.jobRepository = jobRepository;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a QUEUED compression job for an uploaded file.
     * Publishes a JobCreatedEvent after commit so the worker picks it up immediately.
     */
    @Transactional
    public JobResponse createJob(CreateJobRequest request, UUID userId) {
        UUID fileId = request.fileId();
        FileRecord file = fileRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow(() -> AppException.notFound("File not found"));

        if (file.getStatus() != FileStatus.UPLOADED) {
            throw AppException.conflict(
                "File must be in UPLOADED state to start a job (current: " + file.getStatus() + ")");
        }

        boolean activeJobExists = jobRepository.existsByFileIdAndStatusIn(
            fileId, List.of(JobStatus.QUEUED, JobStatus.PROCESSING));
        if (activeJobExists) {
            throw AppException.conflict("An active compression job already exists for this file");
        }

        CompressionFormat format = (request.compressionFormat() != null)
            ? request.compressionFormat()
            : CompressionFormat.GZIP;

        Job job = new Job();
        job.setFile(file);
        job.setUser(userRepository.getReferenceById(userId));
        job.setCompressionFormat(format);
        job = jobRepository.save(job);

        // Event is published synchronously but the @TransactionalEventListener on
        // CompressionWorker defers execution until after this transaction commits.
        eventPublisher.publishEvent(new JobCreatedEvent(
            job.getId(), file.getStorageKey(), file.getContentType(),
            file.getOriginalName(), format));

        return JobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(UUID jobId, UUID userId) {
        Job job = jobRepository.findByIdAndUserId(jobId, userId)
            .orElseThrow(() -> AppException.notFound("Job not found"));
        return JobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> listJobs(UUID userId) {
        return jobRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(JobResponse::from)
            .toList();
    }

    /**
     * Transitions job to PROCESSING. Returns false if the job is not QUEUED,
     * preventing two workers from double-processing the same job.
     */
    @Transactional
    public boolean markProcessing(UUID jobId) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("markProcessing: unknown jobId={}", jobId);
            return false;
        }
        if (job.getStatus() != JobStatus.QUEUED) {
            log.debug("Job {} is {} — another worker already claimed it", jobId, job.getStatus());
            return false;
        }
        job.setStatus(JobStatus.PROCESSING);
        job.setProcessingStartedAt(Instant.now());
        job.setNextRetryAt(null);
        jobRepository.save(job);

        FileRecord file = job.getFile();
        file.setStatus(FileStatus.PROCESSING);
        fileRepository.save(file);
        return true;
    }

    @Transactional
    public void markCompleted(UUID jobId, String outputKey, long outputSizeBytes) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));
        job.setStatus(JobStatus.COMPLETED);
        job.setProcessingCompletedAt(Instant.now());
        jobRepository.save(job);

        FileRecord file = job.getFile();
        file.setStatus(FileStatus.COMPLETED);
        file.setOutputKey(outputKey);
        file.setOutputSizeBytes(outputSizeBytes);
        fileRepository.save(file);
    }

    /**
     * On failure: re-queues with exponential backoff if retries remain,
     * otherwise permanently marks the job and its file as FAILED.
     *
     * Backoff: delay = 2^attemptCount * 30 seconds
     *   attempt 1 → 60 s, attempt 2 → 120 s, then permanent failure.
     */
    @Transactional
    public void markFailedOrRetry(UUID jobId, String errorMessage) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));

        int attempts = job.getAttemptCount() + 1;
        job.setAttemptCount(attempts);
        job.setErrorMessage(errorMessage);

        if (attempts < job.getMaxAttempts()) {
            long delaySeconds = (long) Math.pow(2, attempts) * 30L;
            job.setStatus(JobStatus.QUEUED);
            job.setNextRetryAt(Instant.now().plusSeconds(delaySeconds));
            log.info("Job {} failed (attempt {}/{}), retrying in {}s",
                jobId, attempts, job.getMaxAttempts(), delaySeconds);
        } else {
            job.setStatus(JobStatus.FAILED);
            job.setProcessingCompletedAt(Instant.now());
            log.warn("Job {} permanently failed after {} attempt(s): {}",
                jobId, attempts, errorMessage);

            FileRecord file = job.getFile();
            file.setStatus(FileStatus.FAILED);
            fileRepository.save(file);
        }
        jobRepository.save(job);
    }
}
