CREATE TABLE batches (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    course_id UUID NOT NULL REFERENCES courses(id),
    teacher_id UUID NOT NULL REFERENCES users(id),
    capacity INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'UPCOMING',
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL DEFAULT 'system',
    updated_by VARCHAR(255) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE batch_students (
    batch_id UUID NOT NULL REFERENCES batches(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (batch_id, student_id)
);

ALTER TABLE live_classes
    ADD COLUMN batch_id UUID REFERENCES batches(id) ON DELETE SET NULL,
    ADD COLUMN teacher_id UUID REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_live_classes_batch_id ON live_classes(batch_id);
CREATE INDEX idx_live_classes_teacher_id ON live_classes(teacher_id);
CREATE INDEX idx_batches_teacher_id ON batches(teacher_id);
CREATE INDEX idx_batches_course_id ON batches(course_id);
