-- V34__add_assignment_batch_scope.sql
--
-- Assignments were the last teaching surface with no cohort scoping: every assignment was
-- course-wide, so a Morning batch and an Evening batch necessarily saw the same ones.
-- Quizzes (V30) and coding problems (V31) both carry batch_id already; this brings
-- assignments into line with them.
--
-- Additive and backwards compatible. Existing rows keep batch_id NULL, which means
-- course-wide - exactly the behaviour they have today, so nothing changes for them.

ALTER TABLE assignments
    -- SET NULL matches live_classes, quizzes and coding_problems. Clearing it widens
    -- visibility rather than orphaning the row.
    ADD COLUMN batch_id UUID REFERENCES batches(id) ON DELETE SET NULL;

CREATE INDEX idx_assignments_batch_id ON assignments(batch_id);
CREATE INDEX idx_assignments_course_status ON assignments(course_id, status);

-- No new status column: `status` already exists and carries DRAFT / PUBLISHED / CLOSED.
-- The service previously hardcoded PUBLISHED on create, which is a code fix, not a
-- schema one.
