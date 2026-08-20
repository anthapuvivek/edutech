CREATE TABLE student_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    student_id VARCHAR(20) NOT NULL,  -- e.g., LTX-2026-1234
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    photo_url VARCHAR(512),
    date_of_birth DATE,
    gender VARCHAR(20),
    education VARCHAR(255),
    college VARCHAR(255),
    graduation_year INTEGER,
    qualification VARCHAR(255),
    location VARCHAR(255),
    city VARCHAR(100),
    skills TEXT[],  -- PostgreSQL array
    github_url VARCHAR(512),
    linkedin_url VARCHAR(512),
    leetcode_url VARCHAR(512),
    hackerrank_url VARCHAR(512),
    portfolio_url VARCHAR(512),
    career_goal VARCHAR(500),
    preferred_role VARCHAR(255),
    preferred_locations TEXT[],
    experience_level VARCHAR(50),
    bio TEXT,
    profile_completion_percent INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    updated_by VARCHAR(255) DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_student_profiles_user_id UNIQUE (user_id),
    CONSTRAINT uk_student_profiles_student_id UNIQUE (student_id)
);

CREATE INDEX idx_student_profiles_user_id ON student_profiles(user_id);
CREATE INDEX idx_student_profiles_student_id ON student_profiles(student_id);
CREATE INDEX idx_student_profiles_college ON student_profiles(college);
CREATE INDEX idx_student_profiles_graduation_year ON student_profiles(graduation_year);
