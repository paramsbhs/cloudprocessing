package com.cloudprocessing.file;

import com.cloudprocessing.auth.User;
import com.cloudprocessing.auth.UserRepository;
import com.cloudprocessing.common.AppException;
import com.cloudprocessing.config.AppProperties;
import com.cloudprocessing.storage.PresignedUploadResult;
import com.cloudprocessing.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final FileValidator fileValidator;
    private final AppProperties appProperties;

    public FileService(FileRepository fileRepository,
                       UserRepository userRepository,
                       StorageService storageService,
                       FileValidator fileValidator,
                       AppProperties appProperties) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.fileValidator = fileValidator;
        this.appProperties = appProperties;
    }

    /**
     * Creates a file metadata record and returns a presigned S3 upload URL.
     * The client uses that URL to PUT the file bytes directly to S3.
     */
    @Transactional
    public FileResponse createFile(UUID userId, CreateFileRequest request) {
        fileValidator.validate(request);

        User userRef = userRepository.getReferenceById(userId);

        UUID fileUuid = UUID.randomUUID();
        String sanitized = sanitizeFilename(request.originalName());
        String storageKey = "users/%s/files/%s/%s".formatted(userId, fileUuid, sanitized);

        FileRecord file = new FileRecord();
        file.setUser(userRef);
        file.setOriginalName(request.originalName());
        file.setContentType(request.contentType());
        file.setSizeBytes(request.sizeBytes());
        file.setStorageKey(storageKey);
        file = fileRepository.save(file);

        PresignedUploadResult upload =
            storageService.generateUploadUrl(storageKey, request.contentType());

        return FileResponse.withUploadUrl(file, upload.uploadUrl(), upload.expiresInMinutes());
    }

    @Transactional(readOnly = true)
    public FileResponse getFile(UUID fileId, UUID userId) {
        FileRecord file = fileRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow(() -> AppException.notFound("File not found"));
        return FileResponse.from(file);
    }

    @Transactional(readOnly = true)
    public List<FileResponse> listFiles(UUID userId) {
        return fileRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(FileResponse::from)
            .toList();
    }

    /**
     * Confirms the client upload is complete by verifying the object exists in S3.
     * Updates the file record with the actual size reported by S3.
     */
    @Transactional
    public FileResponse confirmUpload(UUID fileId, UUID userId) {
        FileRecord file = fileRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow(() -> AppException.notFound("File not found"));

        if (file.getStatus() != FileStatus.PENDING_UPLOAD) {
            throw AppException.conflict("File is not in PENDING_UPLOAD state");
        }

        // Verify the object actually landed in S3
        Long actualSize = storageService.headObject(file.getStorageKey())
            .orElseThrow(() -> AppException.badRequest(
                "File not found in S3 — complete the upload before confirming"));

        file.setStatus(FileStatus.UPLOADED);
        file.setSizeBytes(actualSize);   // trust S3's reported size over the client's claim
        return FileResponse.from(fileRepository.save(file));
    }

    /**
     * Returns a presigned download URL.
     * If compression is complete the URL points to the output file; otherwise the original.
     */
    @Transactional(readOnly = true)
    public DownloadResponse getDownloadUrl(UUID fileId, UUID userId, boolean forceOriginal) {
        FileRecord file = fileRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow(() -> AppException.notFound("File not found"));

        if (file.getStatus() == FileStatus.PENDING_UPLOAD) {
            throw AppException.badRequest("File has not been uploaded yet");
        }

        boolean useOutput = !forceOriginal
            && file.getStatus() == FileStatus.COMPLETED
            && file.getOutputKey() != null;

        String key = useOutput ? file.getOutputKey() : file.getStorageKey();
        String url = storageService.generateDownloadUrl(key);
        int expiry = appProperties.getS3().getPresignedUrlExpirationMinutes();

        return new DownloadResponse(fileId, url, expiry, useOutput);
    }

    /**
     * Deletes a file — removes the DB record and all associated S3 objects.
     * Files currently being processed cannot be deleted.
     */
    @Transactional
    public void deleteFile(UUID fileId, UUID userId) {
        FileRecord file = fileRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow(() -> AppException.notFound("File not found"));

        if (file.getStatus() == FileStatus.PROCESSING) {
            throw AppException.conflict("Cannot delete a file while it is being processed");
        }

        // Delete original from S3
        try {
            storageService.deleteObject(file.getStorageKey());
        } catch (Exception e) {
            log.warn("Failed to delete S3 object {}: {}", file.getStorageKey(), e.getMessage());
        }

        // Delete compressed output if it exists
        if (file.getOutputKey() != null) {
            try {
                storageService.deleteObject(file.getOutputKey());
            } catch (Exception e) {
                log.warn("Failed to delete S3 output {}: {}", file.getOutputKey(), e.getMessage());
            }
        }

        fileRepository.delete(file);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String sanitizeFilename(String name) {
        return name.replaceAll("[/\\\\]", "_")
                   .replaceAll("[^a-zA-Z0-9._\\-]", "_")
                   .substring(0, Math.min(name.length(), 200));
    }
}
