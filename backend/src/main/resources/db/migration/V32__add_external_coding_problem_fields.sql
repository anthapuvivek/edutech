-- V32__add_external_coding_problem_fields.sql
--
-- Coding Practice becomes an assignment platform for problems hosted elsewhere
-- (LeetCode, HackerRank, GeeksforGeeks, CodeChef, Codeforces, ...). LearnTriX stores
-- the link and the teacher's own notes; it no longer runs anybody's code.
--
-- Purely additive. The existing coding_problems table is extended rather than replaced,
-- and coding_test_cases / coding_submissions are left untouched - they still hold real
-- student submissions, and dropping them would destroy that history. They simply stop
-- being written to by the new flow.

ALTER TABLE coding_problems
    -- LEETCODE / HACKERRANK / GEEKSFORGEEKS / CODECHEF / CODEFORCES / OTHER
    ADD COLUMN platform VARCHAR(40),
    -- The platform's own numbering, for display and search. Deliberately NOT used to
    -- build the URL: LeetCode #1 is /problems/two-sum/, which no number can derive.
    ADD COLUMN problem_number VARCHAR(40),
    -- The single source of truth for opening the problem. Validated as http/https in
    -- the service before it is ever stored.
    ADD COLUMN problem_url TEXT,
    -- Free-text tags, comma separated, e.g. "Arrays, Hash Table".
    ADD COLUMN topics TEXT;

CREATE INDEX idx_coding_problems_platform ON coding_problems(platform);

-- No `active` column: quizzes, recordings and coding problems already express
-- availability through `status` (DRAFT / PUBLISHED), and the student visibility query
-- filters on it. Adding a second flag would let the two disagree about whether a
-- problem is live. Activate/Deactivate maps onto PUBLISHED/DRAFT.

-- ---------------------------------------------------------------------------
-- Student completion
-- ---------------------------------------------------------------------------
-- LearnTriX cannot verify that a student actually solved a problem on LeetCode - there
-- is no supported API for that. This records only what the student says about their own
-- progress, which is why there is no score, no verdict and no proof column.
CREATE TABLE coding_problem_completions (
    id           UUID PRIMARY KEY,
    problem_id   UUID NOT NULL REFERENCES coding_problems(id) ON DELETE CASCADE,
    student_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    completed    BOOLEAN NOT NULL DEFAULT TRUE,
    completed_at TIMESTAMP WITH TIME ZONE,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    -- One row per student per problem. Toggling updates this row rather than adding
    -- another, so a student cannot inflate their own completion count.
    CONSTRAINT uq_coding_completion_student_problem UNIQUE (problem_id, student_id)
);

CREATE INDEX idx_coding_completions_student_id ON coding_problem_completions(student_id);
CREATE INDEX idx_coding_completions_problem_id ON coding_problem_completions(problem_id);
