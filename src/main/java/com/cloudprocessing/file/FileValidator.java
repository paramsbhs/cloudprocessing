package com.cloudprocessing.file;

import com.cloudprocessing.common.AppException;
import com.cloudprocessing.config.AppProperties;
import org.springframework.stereotype.Component;

/**
 * Validates file upload requests against configured limits.
 * Runs before any DB record or S3 URL is created.
 */
@Component
public class FileValidator {

    private final AppProperties appProperties;

    public FileValidator(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public void validate(CreateFileRequest request) {
        AppProperties.FileUpload config = appProperties.getFile();

        if (request.sizeBytes() != null && request.sizeBytes() > config.getMaxSizeBytes()) {
            throw AppException.badRequest(
                "File exceeds maximum allowed size of %d MB"
                    .formatted(config.getMaxSizeBytes() / 1_048_576));
        }

        String contentType = request.contentType().toLowerCase();
        boolean allowed = config.getAllowedContentTypes().stream()
            .anyMatch(contentType::startsWith);

        if (!allowed) {
            throw AppException.badRequest(
                "Content type '%s' is not supported. Allowed types: %s"
                    .formatted(request.contentType(),
                               String.join(", ", config.getAllowedContentTypes())));
        }
    }
}
