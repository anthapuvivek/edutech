CREATE TABLE live_classes (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    course_id UUID REFERENCES courses(id) ON DELETE SET NULL,
    trainer_name VARCHAR(255) NOT NULL,
    description TEXT,
    class_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    platform VARCHAR(50) NOT NULL,
    meeting_url VARCHAR(512),
    type VARCHAR(50) NOT NULL,
    visibility VARCHAR(50) NOT NULL DEFAULT 'restricted',
    published BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(50) NOT NULL DEFAULT 'Upcoming',
    registrations INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed some upcoming live classes
INSERT INTO live_classes (id, title, course_id, trainer_name, description, class_date, start_time, end_time, platform, meeting_url, type, visibility, published, status)
VALUES (
    '00000000-0000-0000-0000-000000000301'::uuid,
    'Spring Security Deep Dive',
    '00000000-0000-0000-0000-000000000201'::uuid,
    'Durga Prasad',
    'Deep dive walkthrough of JWT setup, filter chain configuration, and custom role mappings in Spring Security 6.',
    CURRENT_DATE + 1,
    '09:00:00'::time,
    '10:30:00'::time,
    'Google Meet',
    'https://meet.google.com/abc-defg-hij',
    'Live Class',
    'restricted',
    TRUE,
    'Upcoming'
),
(
    '00000000-0000-0000-0000-000000000302'::uuid,
    'Advanced Hibernate Query Optimization',
    '00000000-0000-0000-0000-000000000201'::uuid,
    'Durga Prasad',
    'Analyzing Hibernate SQL queries, resolving N+1 select problems, and configuring entity graph caches.',
    CURRENT_DATE + 3,
    '11:00:00'::time,
    '12:30:00'::time,
    'Zoom',
    'https://zoom.us/j/123456789',
    'Live Class',
    'restricted',
    TRUE,
    'Upcoming'
);
