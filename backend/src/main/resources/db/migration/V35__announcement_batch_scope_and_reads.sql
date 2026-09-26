-- V35__announcement_batch_scope_and_reads.sql
--
-- Two gaps in the existing announcement feature:
--   1. Announcements were course-wide only, so a Morning batch could not be told something
--      an Evening batch should not see. Every other teaching surface (quizzes V30, coding
--      V31, assignments V34) already carries batch_id.
--   2. There was no read state, so an unread count had nothing to count.
--
-- Additive. Existing announcements keep batch_id NULL, which means course-wide - exactly
-- the behaviour they have today.

-- updated_at / created_at / version already exist: CourseAnnouncement extends
-- AuditableEntity, so only batch_id is genuinely new here.
ALTER TABLE course_announcements
    ADD COLUMN batch_id UUID REFERENCES batches(id) ON DELETE SET NULL;

CREATE INDEX idx_course_announcements_batch_id ON course_announcements(batch_id);

-- ---------------------------------------------------------------------------
-- Read state
-- ---------------------------------------------------------------------------
-- A row exists only once a student has opened the announcement; absence means unread.
-- That keeps the table proportional to what has actually been read rather than creating a
-- row per student per announcement up front.
CREATE TABLE announcement_reads (
    id              UUID PRIMARY KEY,
    announcement_id UUID NOT NULL REFERENCES course_announcements(id) ON DELETE CASCADE,
    student_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    read_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    -- One row per student per announcement, so re-opening cannot inflate anything.
    CONSTRAINT uq_announcement_read UNIQUE (announcement_id, student_id)
);

CREATE INDEX idx_announcement_reads_student_id ON announcement_reads(student_id);
