-- Hashed password for 'password' is '$2a$10$gRst75Ur8z5Y5i.w6r1qE.gJzN.62L3s5C/w1q8vH2V.P.g1rZtJq'

-- Seed Student User
INSERT INTO users (id, email, password_hash, name, status, email_verified)
VALUES ('00000000-0000-0000-0000-000000000001'::uuid, 'student@learntrix.com', '$2a$10$gRst75Ur8z5Y5i.w6r1qE.gJzN.62L3s5C/w1q8vH2V.P.g1rZtJq', 'Aarav Sharma', 'ACTIVE', TRUE)
ON CONFLICT (email) DO NOTHING;

-- Seed Teacher User
INSERT INTO users (id, email, password_hash, name, status, email_verified)
VALUES ('00000000-0000-0000-0000-000000000002'::uuid, 'teacher@learntrix.com', '$2a$10$gRst75Ur8z5Y5i.w6r1qE.gJzN.62L3s5C/w1q8vH2V.P.g1rZtJq', 'Durga Prasad', 'ACTIVE', TRUE)
ON CONFLICT (email) DO NOTHING;

-- Seed Admin User
INSERT INTO users (id, email, password_hash, name, status, email_verified)
VALUES ('00000000-0000-0000-0000-000000000003'::uuid, 'admin@learntrix.com', '$2a$10$gRst75Ur8z5Y5i.w6r1qE.gJzN.62L3s5C/w1q8vH2V.P.g1rZtJq', 'Nisha Verma', 'ACTIVE', TRUE)
ON CONFLICT (email) DO NOTHING;

-- Assign Roles
INSERT INTO user_roles (user_id, role_id)
SELECT '00000000-0000-0000-0000-000000000001'::uuid, id FROM roles WHERE name = 'STUDENT'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT '00000000-0000-0000-0000-000000000002'::uuid, id FROM roles WHERE name = 'TEACHER'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT '00000000-0000-0000-0000-000000000003'::uuid, id FROM roles WHERE name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Seed Student Profile
INSERT INTO student_profiles (id, user_id, student_id, full_name, profile_completion_percent)
VALUES ('00000000-0000-0000-0000-000000000101'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'LTX-2026-0412', 'Aarav Sharma', 100)
ON CONFLICT (user_id) DO NOTHING;

-- Seed Course
INSERT INTO courses (id, slug, title, subtitle, category, thumbnail_url, instructor_id)
VALUES ('00000000-0000-0000-0000-000000000201'::uuid, 'java-backend-spring-boot', 'Java Backend with Spring Boot', 'Enterprise-grade services, testing and deployment', 'Java', 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=800&q=70', '00000000-0000-0000-0000-000000000002'::uuid)
ON CONFLICT (slug) DO NOTHING;

-- Seed Modules
INSERT INTO modules (id, course_id, title, sequence_number)
VALUES ('00000000-0000-0000-0000-000000000301'::uuid, '00000000-0000-0000-0000-000000000201'::uuid, 'Introduction & Setup', 1)
ON CONFLICT DO NOTHING;

INSERT INTO modules (id, course_id, title, sequence_number)
VALUES ('00000000-0000-0000-0000-000000000302'::uuid, '00000000-0000-0000-0000-000000000201'::uuid, 'JPA & Databases', 2)
ON CONFLICT DO NOTHING;

-- Seed Lessons
INSERT INTO lessons (id, module_id, title, sequence_number, duration_seconds)
VALUES ('00000000-0000-0000-0000-000000000401'::uuid, '00000000-0000-0000-0000-000000000301'::uuid, 'Introduction to Java', 1, 600)
ON CONFLICT DO NOTHING;

INSERT INTO lessons (id, module_id, title, sequence_number, duration_seconds)
VALUES ('00000000-0000-0000-0000-000000000402'::uuid, '00000000-0000-0000-0000-000000000301'::uuid, 'Spring Boot Core Concepts', 2, 1200)
ON CONFLICT DO NOTHING;

INSERT INTO lessons (id, module_id, title, sequence_number, duration_seconds)
VALUES ('00000000-0000-0000-0000-000000000403'::uuid, '00000000-0000-0000-0000-000000000302'::uuid, 'PostgreSQL Integration', 1, 1800)
ON CONFLICT DO NOTHING;

-- Seed Enrollments
INSERT INTO enrollments (id, student_id, course_id, status)
VALUES ('00000000-0000-0000-0000-000000000501'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, '00000000-0000-0000-0000-000000000201'::uuid, 'ACTIVE')
ON CONFLICT (student_id, course_id) DO NOTHING;
