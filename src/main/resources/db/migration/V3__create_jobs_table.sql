-- V3: jobs
-- Tracks async compression jobs; one job per file compression request.

CREATE TABLE jobs (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id                 UUID         NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    user_id                 UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status                  VARCHAR(50)  NOT NULL DEFAULT 'QUEUED',
    attempt_count           INT          NOT NULL DEFAULT 0,
    max_attempts            INT          NOT NULL DEFAULT 3,
    error_message           TEXT,
    processing_started_at   TIMESTAMPTZ,
    processing_completed_at TIMESTAMPTZ,
    next_retry_at           TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_jobs_file_id       ON jobs (file_id);
CREATE INDEX idx_jobs_user_id       ON jobs (user_id);
CREATE INDEX idx_jobs_status        ON jobs (status);
CREATE INDEX idx_jobs_next_retry_at ON jobs (next_retry_at) WHERE status = 'QUEUED';
