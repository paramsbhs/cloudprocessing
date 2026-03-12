package com.cloudprocessing.job;

public enum CompressionFormat {
    /** GZIP — .gz output, works for any file type. */
    GZIP,
    /** ZIP archive — .zip output, works for any file type. */
    ZIP,
    /** Lossy JPEG re-encode — .jpg output, best for image/jpeg and image/png inputs. */
    JPEG
}
