-- Create Companies Table
CREATE TABLE companies (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    logo_url VARCHAR(500),
    industry VARCHAR(255),
    website VARCHAR(255),
    description TEXT
);

-- Create Jobs Table
CREATE TABLE jobs (
    id UUID PRIMARY KEY,
    company_id UUID REFERENCES companies(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    requirements TEXT,
    type VARCHAR(50),
    work_mode VARCHAR(50),
    location VARCHAR(255),
    ctc_range VARCHAR(100),
    eligibility_summary VARCHAR(500),
    application_deadline TIMESTAMP WITH TIME ZONE,
    posted_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    status VARCHAR(50) DEFAULT 'Active',
    openings INT DEFAULT 1
);

-- Create Job Applications Table
CREATE TABLE job_applications (
    id UUID PRIMARY KEY,
    job_id UUID REFERENCES jobs(id) ON DELETE CASCADE,
    student_id UUID REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) DEFAULT 'Applied',
    resume_url VARCHAR(500),
    applied_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    notes TEXT,
    CONSTRAINT uk_job_applications_job_student UNIQUE (job_id, student_id)
);

-- Create Placement Drives Table
CREATE TABLE placement_drives (
    id UUID PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    description TEXT,
    eligibility_summary VARCHAR(500),
    application_deadline TIMESTAMP WITH TIME ZONE,
    assessment_date TIMESTAMP WITH TIME ZONE,
    interview_date TIMESTAMP WITH TIME ZONE,
    openings INT DEFAULT 1,
    stage VARCHAR(50) DEFAULT 'Draft',
    location VARCHAR(255),
    ctc_range VARCHAR(100)
);

-- Create Placement Interviews Table
CREATE TABLE placement_interviews (
    id UUID PRIMARY KEY,
    student_id UUID REFERENCES users(id) ON DELETE CASCADE,
    company_name VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    round VARCHAR(255) NOT NULL,
    interview_date DATE,
    interview_time VARCHAR(50),
    interviewer VARCHAR(255),
    platform VARCHAR(100),
    meeting_url VARCHAR(500),
    status VARCHAR(50) DEFAULT 'Scheduled'
);

-- Create Placement Offers Table
CREATE TABLE placement_offers (
    id UUID PRIMARY KEY,
    student_id UUID REFERENCES users(id) ON DELETE CASCADE,
    company_name VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    offer_date DATE,
    joining_date DATE,
    annual_salary DOUBLE PRECISION,
    location VARCHAR(255),
    status VARCHAR(50) DEFAULT 'Offer Received'
);

-- Create Mentoring Sessions Table
CREATE TABLE mentoring_sessions (
    id UUID PRIMARY KEY,
    mentor_id UUID REFERENCES users(id) ON DELETE CASCADE,
    student_id UUID REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    kind VARCHAR(100),
    session_date DATE,
    session_time VARCHAR(50),
    duration_minutes INT DEFAULT 45,
    platform VARCHAR(100),
    meeting_url VARCHAR(500),
    agenda TEXT,
    notes TEXT,
    status VARCHAR(50) DEFAULT 'Scheduled'
);

-- Create Mentoring Notes Table
CREATE TABLE mentoring_notes (
    id UUID PRIMARY KEY,
    mentor_id UUID REFERENCES users(id) ON DELETE CASCADE,
    student_id UUID REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(100),
    content TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Create Assignments Table
CREATE TABLE assignments (
    id UUID PRIMARY KEY,
    course_id UUID REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_date TIMESTAMP WITH TIME ZONE,
    points INT DEFAULT 100
);

-- Create Assignment Submissions Table
CREATE TABLE assignment_submissions (
    id UUID PRIMARY KEY,
    assignment_id UUID REFERENCES assignments(id) ON DELETE CASCADE,
    student_id UUID REFERENCES users(id) ON DELETE CASCADE,
    submission_url VARCHAR(500),
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    grade INT,
    feedback TEXT,
    status VARCHAR(50) DEFAULT 'Submitted'
);

-- Create Quizzes Table
CREATE TABLE quizzes (
    id UUID PRIMARY KEY,
    course_id UUID REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    time_limit_minutes INT DEFAULT 30,
    passing_score INT DEFAULT 60
);

-- Create Quiz Attempts Table
CREATE TABLE quiz_attempts (
    id UUID PRIMARY KEY,
    quiz_id UUID REFERENCES quizzes(id) ON DELETE CASCADE,
    student_id UUID REFERENCES users(id) ON DELETE CASCADE,
    score INT,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    submitted_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) DEFAULT 'Completed'
);

-- Seed mock company and jobs for E2E flow
INSERT INTO companies (id, name, logo_url, industry, website, description) VALUES
('11111111-1111-1111-1111-111111111111', 'Razorpay', 'https://images.unsplash.com/photo-1614741118887-7a4ee193a5fa?w=100&h=100&fit=crop', 'FinTech', 'https://razorpay.com', 'Razorpay is a leading payment solutions provider in India.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO jobs (id, company_id, title, description, requirements, type, work_mode, location, ctc_range, eligibility_summary, application_deadline, openings) VALUES
('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'Associate Java Backend Engineer', 'Looking for an entry level Java Backend Engineer to join our payments platform team.', 'Java, Spring Boot, PostgreSQL, Git', 'Full-time', 'Hybrid', 'Bangalore', '12-15 LPA', 'Minimum 75% quiz score & 80% progress', now() + interval '30 days', 3)
ON CONFLICT (id) DO NOTHING;
