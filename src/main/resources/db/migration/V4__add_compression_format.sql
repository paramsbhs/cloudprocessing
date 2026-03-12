-- V4: add compression_format to jobs
ALTER TABLE jobs
    ADD COLUMN compression_format VARCHAR(20) NOT NULL DEFAULT 'GZIP';
