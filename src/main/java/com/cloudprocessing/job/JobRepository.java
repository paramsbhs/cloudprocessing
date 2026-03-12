package com.cloudprocessing.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByIdAndUserId(UUID id, UUID userId);

    List<Job> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByFileIdAndStatusIn(UUID fileId, List<JobStatus> statuses);

    /**
     * Loads QUEUED jobs whose retry window has elapsed.
     * JOIN FETCH on file so the scheduler can access file fields without
     * a second query or open session.
     */
    @Query("""
        SELECT j FROM Job j
        JOIN FETCH j.file
        WHERE j.status = 'QUEUED'
          AND (j.nextRetryAt IS NULL OR j.nextRetryAt <= :now)
        ORDER BY j.createdAt ASC
        """)
    List<Job> findQueuedJobsReadyForProcessing(@Param("now") Instant now);

    @Query(value = """
        SELECT status, COUNT(*) FROM jobs WHERE user_id = :userId GROUP BY status
        """, nativeQuery = true)
    List<Object[]> countByStatusForUser(@Param("userId") UUID userId);

    @Query(value = """
        SELECT COALESCE(
          AVG(EXTRACT(EPOCH FROM (processing_completed_at - processing_started_at)) * 1000), 0)
        FROM jobs
        WHERE user_id = :userId AND status = 'COMPLETED'
          AND processing_started_at IS NOT NULL AND processing_completed_at IS NOT NULL
        """, nativeQuery = true)
    double avgProcessingTimeMs(@Param("userId") UUID userId);
}
