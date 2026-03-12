package com.cloudprocessing.file;

import com.cloudprocessing.auth.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "files", indexes = {
    @Index(name = "idx_files_user_id", columnList = "user_id"),
    @Index(name = "idx_files_status",  columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class FileRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Original filename as provided by the client. */
    @Column(nullable = false, length = 500)
    private String originalName;

    /** S3 object key where the original file is stored. */
    @Column(nullable = false, unique = true, length = 1000)
    private String storageKey;

    /** S3 object key for the compressed output (set after compression). */
    @Column(length = 1000)
    private String outputKey;

    @Column(nullable = false, length = 255)
    private String contentType;

    private Long sizeBytes;

    /** Size of the compressed output in bytes (set after compression). */
    private Long outputSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FileStatus status = FileStatus.PENDING_UPLOAD;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    // ── Getters / setters ─────────────────────────────────────────────────

    public UUID getId()                              { return id; }
    public User getUser()                            { return user; }
    public void setUser(User user)                   { this.user = user; }
    public String getOriginalName()                  { return originalName; }
    public void setOriginalName(String n)            { this.originalName = n; }
    public String getStorageKey()                    { return storageKey; }
    public void setStorageKey(String k)              { this.storageKey = k; }
    public String getOutputKey()                     { return outputKey; }
    public void setOutputKey(String k)               { this.outputKey = k; }
    public String getContentType()                   { return contentType; }
    public void setContentType(String t)             { this.contentType = t; }
    public Long getSizeBytes()                       { return sizeBytes; }
    public void setSizeBytes(Long n)                 { this.sizeBytes = n; }
    public Long getOutputSizeBytes()                 { return outputSizeBytes; }
    public void setOutputSizeBytes(Long n)           { this.outputSizeBytes = n; }
    public FileStatus getStatus()                    { return status; }
    public void setStatus(FileStatus s)              { this.status = s; }
    public Instant getCreatedAt()                    { return createdAt; }
    public Instant getUpdatedAt()                    { return updatedAt; }
}
