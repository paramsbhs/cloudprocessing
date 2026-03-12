package com.cloudprocessing.storage;

public record PresignedUploadResult(
    String uploadUrl,
    int expiresInMinutes,
    String storageKey
) {}
