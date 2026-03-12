package com.cloudprocessing.file;

public enum FileStatus {
    /** Record created; waiting for client to PUT the file to S3. */
    PENDING_UPLOAD,

    /** Client has uploaded to S3; ready to be compressed. */
    UPLOADED,

    /** A compression job is actively processing this file. */
    PROCESSING,

    /** Compression completed; compressed file is available for download. */
    COMPLETED,

    /** Processing failed; see the associated job for the error reason. */
    FAILED
}
