package com.cloudprocessing.file;

import java.util.UUID;

public record DownloadResponse(
    UUID fileId,
    String downloadUrl,
    int expiresInMinutes,
    /** True if this URL points to the compressed output; false if original. */
    boolean isCompressedOutput
) {}
