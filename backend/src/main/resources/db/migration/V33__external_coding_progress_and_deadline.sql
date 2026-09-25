ALTER TABLE coding_problems ADD COLUMN deadline DATE;

ALTER TABLE coding_problem_completions
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    ADD COLUMN opened_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();

UPDATE coding_problem_completions
SET status = CASE WHEN completed THEN 'COMPLETED' ELSE 'IN_PROGRESS' END;

ALTER TABLE coding_problem_completions
    ADD CONSTRAINT chk_coding_progress_status
    CHECK (status IN ('NOT_STARTED', 'OPENED', 'IN_PROGRESS', 'COMPLETED'));

CREATE INDEX idx_coding_problems_batch_status ON coding_problems(batch_id, status);
