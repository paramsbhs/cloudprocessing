-- V2: files
-- Stores file metadata; actual bytes live in S3 (referenced by storage_key).

CREATE TABLE files (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    original_name   VARCHAR(500)  NOT NULL,
    storage_key     VARCHAR(1000) NOT NULL UNIQUE,
    output_key      VARCHAR(1000),
    content_type    VARCHAR(255)  NOT NULL,
    size_bytes      BIGINT,
    output_size_bytes BIGINT,
    status          VARCHAR(50)   NOT NULL DEFAULT 'PENDING_UPLOAD',
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_files_user_id ON files (user_id);
CREATE INDEX idx_files_status  ON files (status);
