-- V30__create_quiz_questions_options_and_answers.sql
--
-- Completes the quiz model. V16 created `quizzes` and `quiz_attempts` but no storage for
-- the questions themselves, so a quiz could record a score with no record of what was
-- asked and no way for the backend to mark an attempt. This adds the missing three tables
-- and gives a quiz the same batch scope live_classes already has.
--
-- Deliberately NOT added: a `published` boolean. `quizzes.status` already carries that
-- meaning (the entity defaults it to 'PUBLISHED'), and a second flag would let the two
-- disagree. Publication stays a status transition: DRAFT -> PUBLISHED.
--
-- Existing rows are preserved: every change is additive and the new column is nullable.

-- ---------------------------------------------------------------------------
-- Batch scope for quizzes
-- ---------------------------------------------------------------------------
-- NULL          = course-wide, visible to everyone enrolled in the course
-- a batch id    = that cohort only, so a Morning quiz stays invisible to Evening
--
-- ON DELETE SET NULL mirrors live_classes.batch_id. Note that clearing batch_id widens
-- visibility, so the service layer must unpublish a detached quiz rather than rely on
-- this alone - the same rule batch deletion already applies to classes.
ALTER TABLE quizzes ADD COLUMN IF NOT EXISTS batch_id UUID;

ALTER TABLE quizzes
    ADD CONSTRAINT fk_quizzes_batch
    FOREIGN KEY (batch_id) REFERENCES batches(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_quizzes_batch_id ON quizzes(batch_id);
CREATE INDEX IF NOT EXISTS idx_quizzes_course_id ON quizzes(course_id);

-- ---------------------------------------------------------------------------
-- Questions
-- ---------------------------------------------------------------------------
CREATE TABLE quiz_questions (
    id              UUID PRIMARY KEY,
    quiz_id         UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    question_text   TEXT NOT NULL,
    -- SINGLE_CHOICE today. Kept as a column so TRUE_FALSE / MULTI_CHOICE can be added
    -- without another migration.
    question_type   VARCHAR(50) NOT NULL DEFAULT 'SINGLE_CHOICE',
    points          INT NOT NULL DEFAULT 1,
    sequence_number INT NOT NULL DEFAULT 0,
    -- Shown only after submission, never in the student's pre-submission payload.
    explanation     TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_quiz_questions_quiz_id ON quiz_questions(quiz_id);

-- ---------------------------------------------------------------------------
-- Options (the answer key)
-- ---------------------------------------------------------------------------
-- is_correct is the answer key. It must never appear in any student-facing DTO before
-- the attempt is submitted; the backend reads it to mark, the student never receives it.
CREATE TABLE quiz_options (
    id              UUID PRIMARY KEY,
    question_id     UUID NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    option_text     TEXT NOT NULL,
    is_correct      BOOLEAN NOT NULL DEFAULT FALSE,
    sequence_number INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_quiz_options_question_id ON quiz_options(question_id);

-- ---------------------------------------------------------------------------
-- Answers given during an attempt
-- ---------------------------------------------------------------------------
-- is_correct and points_awarded are written by the backend when it marks the attempt.
-- They are never accepted from the client - a score sent from React is ignored.
CREATE TABLE quiz_attempt_answers (
    id                 UUID PRIMARY KEY,
    attempt_id         UUID NOT NULL REFERENCES quiz_attempts(id) ON DELETE CASCADE,
    question_id        UUID NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    -- SET NULL rather than CASCADE: if an option is later edited away, the answer record
    -- and the marked result survive for reporting.
    selected_option_id UUID REFERENCES quiz_options(id) ON DELETE SET NULL,
    is_correct         BOOLEAN NOT NULL DEFAULT FALSE,
    points_awarded     INT NOT NULL DEFAULT 0,
    answered_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    -- One answer per question per attempt. This makes the "duplicate answers" case a
    -- database guarantee rather than something the service has to remember to check.
    CONSTRAINT uk_quiz_attempt_answer UNIQUE (attempt_id, question_id)
);

CREATE INDEX idx_quiz_attempt_answers_attempt_id ON quiz_attempt_answers(attempt_id);

-- ---------------------------------------------------------------------------
-- Attempt result detail
-- ---------------------------------------------------------------------------
-- `score` already exists. These two let a result be reported ("7 of 10 correct") without
-- re-joining and re-counting the answer rows on every read.
ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS total_questions INT NOT NULL DEFAULT 0;
ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS correct_answers INT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_quiz_attempts_student_id ON quiz_attempts(student_id);
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_quiz_id ON quiz_attempts(quiz_id);
