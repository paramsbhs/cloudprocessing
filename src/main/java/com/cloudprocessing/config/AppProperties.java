package com.cloudprocessing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Typed binding for all app.* properties.
 * Avoids scattered @Value annotations across the codebase.
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final S3 s3 = new S3();
    private final Async async = new Async();
    private final FileUpload file = new FileUpload();

    public Jwt getJwt() { return jwt; }
    public S3 getS3() { return s3; }
    public Async getAsync() { return async; }
    public FileUpload getFile() { return file; }

    public static class Jwt {
        private String secret = "change-me-in-production-must-be-at-least-32-chars";
        private long expirationMs = 86_400_000L; // 24 h

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }

        public long getExpirationMs() { return expirationMs; }
        public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
    }

    public static class S3 {
        private String bucket = "cloud-processing-files";
        private String region = "us-east-1";
        /** Internal S3/LocalStack endpoint used by S3Client (Docker service name). */
        private String endpoint = "";
        /**
         * Public-facing endpoint embedded in presigned URLs.
         * Defaults to endpoint if not set. Override to localhost:4566 in local dev
         * so clients outside Docker can reach LocalStack.
         */
        private String presignedEndpoint = "";
        private int presignedUrlExpirationMinutes = 15;

        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

        public String getPresignedEndpoint() {
            return presignedEndpoint.isBlank() ? endpoint : presignedEndpoint;
        }
        public void setPresignedEndpoint(String e) { this.presignedEndpoint = e; }

        public int getPresignedUrlExpirationMinutes() { return presignedUrlExpirationMinutes; }
        public void setPresignedUrlExpirationMinutes(int mins) { this.presignedUrlExpirationMinutes = mins; }
    }

    public static class FileUpload {
        private long maxSizeBytes = 104_857_600L; // 100 MB
        private java.util.List<String> allowedContentTypes = java.util.List.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/gif",
            "text/plain",
            "application/zip",
            "application/x-zip-compressed",
            "application/gzip"
        );

        public long getMaxSizeBytes() { return maxSizeBytes; }
        public void setMaxSizeBytes(long n) { this.maxSizeBytes = n; }

        public java.util.List<String> getAllowedContentTypes() { return allowedContentTypes; }
        public void setAllowedContentTypes(java.util.List<String> t) { this.allowedContentTypes = t; }
    }

    public static class Async {
        private int corePoolSize = 4;
        private int maxPoolSize = 8;
        private int queueCapacity = 100;

        public int getCorePoolSize() { return corePoolSize; }
        public void setCorePoolSize(int n) { this.corePoolSize = n; }

        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int n) { this.maxPoolSize = n; }

        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int n) { this.queueCapacity = n; }
    }
}
