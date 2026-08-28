-- ==========================================================
-- Migration V23: CRM, Leads, Enquiries, Notes, Comms, Follow-ups
-- ==========================================================

CREATE TABLE IF NOT EXISTS leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    course_interest VARCHAR(255) NOT NULL,
    source VARCHAR(50) NOT NULL DEFAULT 'Website',
    stage VARCHAR(50) NOT NULL DEFAULT 'New',
    assigned_to_id UUID REFERENCES users(id) ON DELETE SET NULL,
    last_contact_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    next_follow_up_at TIMESTAMPTZ,
    city VARCHAR(100),
    budget INTEGER,
    score INTEGER NOT NULL DEFAULT 50,
    demo_attended BOOLEAN NOT NULL DEFAULT FALSE,
    enrollment_status VARCHAR(50) NOT NULL DEFAULT 'Not Enrolled',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_leads_stage ON leads(stage);
CREATE INDEX IF NOT EXISTS idx_leads_source ON leads(source);
CREATE INDEX IF NOT EXISTS idx_leads_assigned_to ON leads(assigned_to_id);
CREATE INDEX IF NOT EXISTS idx_leads_email ON leads(email);

CREATE TABLE IF NOT EXISTS enquiries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID REFERENCES leads(id) ON DELETE SET NULL,
    course_id UUID REFERENCES courses(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    experience_level VARCHAR(50),
    learning_mode VARCHAR(50),
    message TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'New',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_enquiries_lead_id ON enquiries(lead_id);
CREATE INDEX IF NOT EXISTS idx_enquiries_course_id ON enquiries(course_id);

CREATE TABLE IF NOT EXISTS lead_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    author_id UUID REFERENCES users(id) ON DELETE SET NULL,
    author_name VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_lead_notes_lead_id ON lead_notes(lead_id);

CREATE TABLE IF NOT EXISTS lead_communications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    author_id UUID REFERENCES users(id) ON DELETE SET NULL,
    author_name VARCHAR(255) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    summary TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_lead_comms_lead_id ON lead_communications(lead_id);

CREATE TABLE IF NOT EXISTS lead_follow_ups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    assigned_to_id UUID REFERENCES users(id) ON DELETE SET NULL,
    assigned_to_name VARCHAR(255),
    follow_up_date VARCHAR(20) NOT NULL,
    follow_up_time VARCHAR(20) NOT NULL,
    notes TEXT,
    next_action VARCHAR(100) NOT NULL DEFAULT 'Call',
    status VARCHAR(50) NOT NULL DEFAULT 'Pending',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_lead_follow_ups_lead_id ON lead_follow_ups(lead_id);

-- Seed Counsellor User
INSERT INTO users (id, email, password_hash, name, status, email_verified)
VALUES ('00000000-0000-0000-0000-000000000007'::uuid, 'counsellor@learntrix.com', '$2a$10$iq9BiCjxVz5RmM7HsKjy3OeavCZ7mCzSQEiEtgwj/nzRG/3yzHUDu', 'Priya Menon', 'ACTIVE', TRUE)
ON CONFLICT (email) DO NOTHING;

-- Assign Counsellor Role
INSERT INTO user_roles (user_id, role_id)
SELECT '00000000-0000-0000-0000-000000000007'::uuid, id FROM roles WHERE name = 'COUNSELLOR'
ON CONFLICT DO NOTHING;

-- Seed initial demo leads
INSERT INTO leads (id, name, email, phone, course_interest, source, stage, assigned_to_id, city, budget, score, demo_attended, enrollment_status)
VALUES 
  ('00000000-0000-0000-0000-000000000601'::uuid, 'Rohan Mehta', 'rohan.mehta@gmail.com', '+91 98201 12345', 'Java Backend with Spring Boot', 'Course Enquiry', 'New', '00000000-0000-0000-0000-000000000007'::uuid, 'Mumbai', 45000, 75, false, 'Not Enrolled'),
  ('00000000-0000-0000-0000-000000000602'::uuid, 'Ananya Sharma', 'ananya.sharma@gmail.com', '+91 98302 23456', 'Full Stack Development with React & Node.js', 'Website', 'Contacted', '00000000-0000-0000-0000-000000000007'::uuid, 'Bangalore', 55000, 80, false, 'Not Enrolled'),
  ('00000000-0000-0000-0000-000000000603'::uuid, 'Siddharth Rao', 'siddharth.rao@gmail.com', '+91 98403 34567', 'Java Backend with Spring Boot', 'Demo Class', 'Demo Scheduled', '00000000-0000-0000-0000-000000000007'::uuid, 'Hyderabad', 35000, 85, true, 'Not Enrolled'),
  ('00000000-0000-0000-0000-000000000604'::uuid, 'Pooja Nair', 'pooja.nair@gmail.com', '+91 98504 45678', 'Full Stack Development with React & Node.js', 'Referral', 'Payment Pending', '00000000-0000-0000-0000-000000000007'::uuid, 'Chennai', 50000, 90, true, 'Payment Pending'),
  ('00000000-0000-0000-0000-000000000605'::uuid, 'Vikram Malhotra', 'vikram.malhotra@gmail.com', '+91 98605 56789', 'Java Backend with Spring Boot', 'WhatsApp', 'Converted', '00000000-0000-0000-0000-000000000007'::uuid, 'Delhi', 60000, 95, true, 'Enrolled')
ON CONFLICT (id) DO NOTHING;

-- Seed initial notes & communications
INSERT INTO lead_notes (id, lead_id, author_id, author_name, body)
VALUES 
  ('00000000-0000-0000-0000-000000000611'::uuid, '00000000-0000-0000-0000-000000000602'::uuid, '00000000-0000-0000-0000-000000000007'::uuid, 'Priya Menon', 'Prefers weekend batch. Working as QA engineer looking to switch to dev.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO lead_communications (id, lead_id, author_id, author_name, channel, summary)
VALUES 
  ('00000000-0000-0000-0000-000000000621'::uuid, '00000000-0000-0000-0000-000000000602'::uuid, '00000000-0000-0000-0000-000000000007'::uuid, 'Priya Menon', 'Call', 'Discussed course roadmap, timings and weekend schedules.')
ON CONFLICT (id) DO NOTHING;
