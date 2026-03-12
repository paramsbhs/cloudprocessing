package com.cloudprocessing.job;

import com.cloudprocessing.auth.User;
import com.cloudprocessing.file.FileRecord;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs", indexes = {
    @Index(name = "idx_jobs_file_id",       columnList = "file_id"),
    @Index(name = "idx_jobs_user_id",       columnList = "user_id"),
    @Index(name = "idx_jobs_status",        columnList = "status"),
    @Index(name = "idx_jobs_next_retry_at", columnList = "next_retry_at")
})
@EntityListeners(AuditingEntityListener.class)
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileRecord file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private JobStatus status = JobStatus.QUEUED;

    @Column(nullable = false)
    private int attemptCount = 0;

    @Column(nullable = false)
    private int maxAttempts = 3;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompressionFormat compressionFormat = CompressionFormat.GZIP;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Instant processingStartedAt;
    private Instant processingCompletedAt;
    private Instant nextRetryAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    // ── Getters / setters ──────────────────────────────────────────────────

    public UUID getId()                           { return id; }
    public FileRecord getFile()                   { return file; }
    public void setFile(FileRecord file)          { this.file = file; }
    public User getUser()                         { return user; }
    public void setUser(User user)                { this.user = user; }
    public JobStatus getStatus()                  { return status; }
    public void setStatus(JobStatus status)       { this.status = status; }
    public int getAttemptCount()                  { return attemptCount; }
    public void setAttemptCount(int n)            { this.attemptCount = n; }
    public int getMaxAttempts()                           { return maxAttempts; }
    public void setMaxAttempts(int n)                     { this.maxAttempts = n; }
    public CompressionFormat getCompressionFormat()       { return compressionFormat; }
    public void setCompressionFormat(CompressionFormat f) { this.compressionFormat = f; }
    public String getErrorMessage()               { return errorMessage; }
    public void setErrorMessage(String msg)       { this.errorMessage = msg; }
    public Instant getProcessingStartedAt()       { return processingStartedAt; }
    public void setProcessingStartedAt(Instant t) { this.processingStartedAt = t; }
    public Instant getProcessingCompletedAt()     { return processingCompletedAt; }
    public void setProcessingCompletedAt(Instant t){ this.processingCompletedAt = t; }
    public Instant getNextRetryAt()               { return nextRetryAt; }
    public void setNextRetryAt(Instant t)         { this.nextRetryAt = t; }
    public Instant getCreatedAt()                 { return createdAt; }
    public Instant getUpdatedAt()                 { return updatedAt; }
}
