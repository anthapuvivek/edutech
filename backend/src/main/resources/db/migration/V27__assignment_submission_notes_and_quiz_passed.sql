-- V27__assignment_submission_notes_and_quiz_passed.sql

ALTER TABLE assignment_submissions
    ADD COLUMN IF NOT EXISTS notes TEXT;

ALTER TABLE quiz_attempts
    ADD COLUMN IF NOT EXISTS passed BOOLEAN DEFAULT FALSE;
