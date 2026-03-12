package com.cloudprocessing.file;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FileResponse(
    UUID id,
    String originalName,
    String contentType,
    Long sizeBytes,
    FileStatus status,

    /** Presigned S3 PUT URL — only present immediately after file creation. */
    String uploadUrl,
    Integer uploadUrlExpiresInMinutes,

    Instant createdAt,
    Instant updatedAt
) {
    /** Factory for list/get responses where upload URL is not included. */
    public static FileResponse from(FileRecord f) {
        return new FileResponse(
            f.getId(), f.getOriginalName(), f.getContentType(),
            f.getSizeBytes(), f.getStatus(),
            null, null,
            f.getCreatedAt(), f.getUpdatedAt());
    }

    /** Factory for the creation response that includes the upload URL. */
    public static FileResponse withUploadUrl(FileRecord f, String uploadUrl, int expiresMinutes) {
        return new FileResponse(
            f.getId(), f.getOriginalName(), f.getContentType(),
            f.getSizeBytes(), f.getStatus(),
            uploadUrl, expiresMinutes,
            f.getCreatedAt(), f.getUpdatedAt());
    }
}
