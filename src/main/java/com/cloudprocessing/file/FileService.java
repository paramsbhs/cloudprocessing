package com.cloudprocessing.file;

import com.cloudprocessing.auth.User;
import com.cloudprocessing.auth.UserRepository;
import com.cloudprocessing.common.AppException;
import com.cloudprocessing.storage.PresignedUploadResult;
import com.cloudprocessing.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public FileService(FileRepository fileRepository,
                       UserRepository userRepository,
                       StorageService storageService) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    /**
     * Creates a file metadata record and returns a presigned S3 upload URL.
     * The client uses that URL to PUT the file bytes directly to S3.
     */
    @Transactional
    public FileResponse createFile(UUID userId, CreateFileRequest request) {
        User userRef = userRepository.getReferenceById(userId);

        // Build a unique S3 key before persisting so it can reference a stable UUID
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
     * Marks a file as UPLOADED after the client confirms the PUT completed.
     * In a production system this would be triggered by an S3 event notification.
     */
    @Transactional
    public FileResponse confirmUpload(UUID fileId, UUID userId) {
        FileRecord file = fileRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow(() -> AppException.notFound("File not found"));

        if (file.getStatus() != FileStatus.PENDING_UPLOAD) {
            throw AppException.conflict("File is not in PENDING_UPLOAD state");
        }

        file.setStatus(FileStatus.UPLOADED);
        return FileResponse.from(fileRepository.save(file));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Strips path separators and trims the filename to a safe length. */
    private String sanitizeFilename(String name) {
        return name.replaceAll("[/\\\\]", "_")
                   .replaceAll("[^a-zA-Z0-9._\\-]", "_")
                   .substring(0, Math.min(name.length(), 200));
    }
}
