-- Migration V25: Add status column to courses table
-- Supports publication lifecycle: DRAFT, PUBLISHED, ARCHIVED
-- Existing courses default to PUBLISHED.

ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED';

UPDATE courses
SET status = 'PUBLISHED'
WHERE status IS NULL OR status = '';

CREATE INDEX IF NOT EXISTS idx_courses_status ON courses(status);
