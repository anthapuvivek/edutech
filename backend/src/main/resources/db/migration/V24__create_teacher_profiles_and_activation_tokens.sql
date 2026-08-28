-- V24__create_teacher_profiles_and_activation_tokens.sql

-- 1. Create teacher_profiles table
CREATE TABLE IF NOT EXISTS teacher_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    employee_id VARCHAR(50) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    headline VARCHAR(255),
    department VARCHAR(100),
    qualification VARCHAR(255),
    experience_years INTEGER DEFAULT 0,
    skills TEXT,
    bio TEXT,
    approval_status VARCHAR(50) NOT NULL DEFAULT 'approved',
    rating NUMERIC(3,2) DEFAULT 0.0,
    photo_url VARCHAR(512),
    cv_url VARCHAR(512),
    linkedin_url VARCHAR(512),
    github_url VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    updated_by VARCHAR(255) DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_teacher_profiles_user_id UNIQUE (user_id),
    CONSTRAINT uk_teacher_profiles_employee_id UNIQUE (employee_id)
);

CREATE INDEX IF NOT EXISTS idx_teacher_profiles_user_id ON teacher_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_teacher_profiles_employee_id ON teacher_profiles(employee_id);

-- 2. Create account_activation_tokens table
CREATE TABLE IF NOT EXISTS account_activation_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    token VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_type VARCHAR(50) NOT NULL DEFAULT 'ONBOARDING_ACTIVATION',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) DEFAULT 'system',
    CONSTRAINT uk_account_activation_tokens_token UNIQUE (token)
);

CREATE INDEX IF NOT EXISTS idx_activation_tokens_user_id ON account_activation_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_activation_tokens_token ON account_activation_tokens(token);

-- 3. Seed default teacher profile for existing seed teacher (Durga Prasad)
INSERT INTO teacher_profiles (
    id, user_id, employee_id, full_name, phone, headline, department, qualification,
    experience_years, skills, bio, approval_status, rating
)
VALUES (
    '00000000-0000-0000-0000-000000000102'::uuid,
    '00000000-0000-0000-0000-000000000002'::uuid,
    'LTX-T-2026-0001',
    'Durga Prasad',
    '+91 98765 43210',
    'Lead Java & Cloud Architect',
    'Computer Science',
    'M.Tech in Software Engineering',
    8,
    'Java,Spring Boot,Microservices,PostgreSQL,Docker,Kubernetes,AWS',
    'Senior Java instructor with 8+ years of enterprise architecture experience.',
    'approved',
    4.90
)
ON CONFLICT (user_id) DO NOTHING;
