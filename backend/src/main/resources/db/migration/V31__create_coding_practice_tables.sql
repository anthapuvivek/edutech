-- V31__create_coding_practice_tables.sql
--
-- Coding practice: problems, their test cases, and student submissions.
--
-- Scoping follows the model live_classes and quizzes already use, so a coding problem
-- obeys the same cohort boundary as everything else:
--   batch_id IS NULL      -> course-wide, every enrolled student
--   batch_id = <a batch>  -> that cohort only (Morning cannot see Evening)
--
-- Publication reuses the `status` convention from quizzes (DRAFT / PUBLISHED) rather than
-- adding a separate boolean that could disagree with it.

CREATE TABLE coding_problems (
    id              UUID PRIMARY KEY,
    course_id       UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    -- Optional curriculum placement, mirroring class_recordings after V29.
    module_id       UUID REFERENCES modules(id) ON DELETE SET NULL,
    -- SET NULL matches live_classes. Clearing it widens visibility, so the service
    -- unpublishes a detached problem rather than relying on this alone.
    batch_id        UUID REFERENCES batches(id) ON DELETE SET NULL,
    teacher_id      UUID REFERENCES users(id) ON DELETE SET NULL,

    title           VARCHAR(255) NOT NULL,
    slug            VARCHAR(255),
    description     TEXT NOT NULL,
    difficulty      VARCHAR(20) NOT NULL DEFAULT 'EASY',
    constraints_text TEXT,
    starter_code    TEXT,
    -- Default language the editor opens with; a submission carries its own language.
    language        VARCHAR(50) NOT NULL DEFAULT 'java',

    time_limit_ms   INT NOT NULL DEFAULT 2000,
    memory_limit_mb INT NOT NULL DEFAULT 128,

    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_coding_problems_course_id ON coding_problems(course_id);
CREATE INDEX idx_coding_problems_batch_id ON coding_problems(batch_id);
CREATE INDEX idx_coding_problems_teacher_id ON coding_problems(teacher_id);

-- ---------------------------------------------------------------------------
-- Test cases
-- ---------------------------------------------------------------------------
-- is_sample is the disclosure boundary. Sample cases are shown to the student and used by
-- Run Code; hidden cases are used only by Submit and must never appear in any student
-- response - not their input, not their expected output.
CREATE TABLE coding_test_cases (
    id              UUID PRIMARY KEY,
    problem_id      UUID NOT NULL REFERENCES coding_problems(id) ON DELETE CASCADE,
    input_data      TEXT,
    expected_output TEXT NOT NULL,
    is_sample       BOOLEAN NOT NULL DEFAULT FALSE,
    sequence_number INT NOT NULL DEFAULT 0,
    points          INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_coding_test_cases_problem_id ON coding_test_cases(problem_id);

-- ---------------------------------------------------------------------------
-- Submissions
-- ---------------------------------------------------------------------------
-- One row per Submit. Run Code is deliberately not persisted: it is a scratch execution
-- against sample cases only, and storing it would distort acceptance-rate statistics.
CREATE TABLE coding_submissions (
    id            UUID PRIMARY KEY,
    problem_id    UUID NOT NULL REFERENCES coding_problems(id) ON DELETE CASCADE,
    student_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    language      VARCHAR(50) NOT NULL,
    source_code   TEXT NOT NULL,

    -- QUEUED / RUNNING / ACCEPTED / WRONG_ANSWER / COMPILE_ERROR / RUNTIME_ERROR
    -- TIME_LIMIT_EXCEEDED / MEMORY_LIMIT_EXCEEDED / ERROR
    status        VARCHAR(32) NOT NULL DEFAULT 'QUEUED',
    passed_count  INT NOT NULL DEFAULT 0,
    total_count   INT NOT NULL DEFAULT 0,
    runtime_ms    INT,
    -- Compiler or judge message. Never contains hidden test data.
    message       TEXT,

    submitted_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_coding_submissions_problem_id ON coding_submissions(problem_id);
CREATE INDEX idx_coding_submissions_student_id ON coding_submissions(student_id);
CREATE INDEX idx_coding_submissions_student_problem
    ON coding_submissions(student_id, problem_id);
