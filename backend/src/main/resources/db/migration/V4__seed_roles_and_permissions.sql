-- Insert Roles
INSERT INTO roles (name, display_name, description) VALUES
('SUPER_ADMIN', 'Super Administrator', 'Full platform access'),
('ADMIN', 'Administrator', 'Platform management'),
('TEACHER', 'Teacher/Trainer', 'Course delivery and student management'),
('MENTOR', 'Mentor', 'Student guidance and career mentoring'),
('PLACEMENT_OFFICER', 'Placement Officer', 'Job and placement management'),
('SUPPORT_AGENT', 'Support Agent', 'Help desk and ticket management'),
('COUNSELLOR', 'Counsellor', 'Lead management and counselling'),
('STUDENT', 'Student', 'Learning and career access')
ON CONFLICT (name) DO NOTHING;

-- Insert Permissions
INSERT INTO permissions (name, display_name, description, module) VALUES
-- Platform
('platform.manage', 'Manage Platform', 'Manage platform-wide settings', 'Platform'),
-- User
('users.view', 'View Users', 'View user profiles', 'User'),
('users.manage', 'Manage Users', 'Manage existing users', 'User'),
('users.create', 'Create Users', 'Create new users', 'User'),
('users.suspend', 'Suspend Users', 'Suspend user accounts', 'User'),
-- Student
('students.view', 'View Students', 'View student profiles', 'Student'),
('students.manage', 'Manage Students', 'Manage student profiles', 'Student'),
('students.create', 'Create Students', 'Create student profiles', 'Student'),
-- Course
('courses.view', 'View Courses', 'View courses', 'Course'),
('courses.create', 'Create Courses', 'Create new courses', 'Course'),
('courses.update', 'Update Courses', 'Update existing courses', 'Course'),
('courses.publish', 'Publish Courses', 'Publish courses', 'Course'),
('courses.delete', 'Delete Courses', 'Delete courses', 'Course'),
-- Enrollment
('enrollments.view', 'View Enrollments', 'View course enrollments', 'Enrollment'),
('enrollments.manage', 'Manage Enrollments', 'Manage course enrollments', 'Enrollment'),
('enrollments.create', 'Create Enrollments', 'Enroll students to courses', 'Enrollment'),
-- Batch
('batches.view', 'View Batches', 'View learning batches', 'Batch'),
('batches.manage', 'Manage Batches', 'Manage learning batches', 'Batch'),
('batches.create', 'Create Batches', 'Create learning batches', 'Batch'),
-- Attendance
('attendance.view', 'View Attendance', 'View student attendance', 'Attendance'),
('attendance.manage', 'Manage Attendance', 'Mark and manage attendance', 'Attendance'),
('attendance.correct', 'Correct Attendance', 'Correct attendance records', 'Attendance'),
-- Payment
('payments.view', 'View Payments', 'View payment records', 'Payment'),
('payments.manage', 'Manage Payments', 'Manage payment records', 'Payment'),
('payments.verify', 'Verify Payments', 'Verify manual payments', 'Payment'),
-- Coupon
('coupons.view', 'View Coupons', 'View discount coupons', 'Coupon'),
('coupons.manage', 'Manage Coupons', 'Create and manage coupons', 'Coupon'),
-- CRM
('crm.view', 'View Leads', 'View CRM leads', 'CRM'),
('crm.manage', 'Manage Leads', 'Manage CRM leads', 'CRM'),
('crm.assign', 'Assign Leads', 'Assign leads to counsellors', 'CRM'),
-- Support
('support.view', 'View Support Tickets', 'View support tickets', 'Support'),
('support.manage', 'Manage Support Tickets', 'Manage and resolve support tickets', 'Support'),
('support.internal_notes', 'Support Internal Notes', 'Add internal notes to tickets', 'Support'),
-- Mentor
('mentor.students', 'Mentor Students', 'View assigned mentee profiles', 'Mentor'),
('mentor.sessions', 'Mentor Sessions', 'Manage mentoring sessions', 'Mentor'),
('mentor.assign', 'Assign Mentors', 'Assign mentors to students', 'Mentor'),
-- Placement
('placement.students.view', 'View Placement Students', 'View students for placement', 'Placement'),
('placement.jobs.view', 'View Jobs', 'View job postings', 'Placement'),
('placement.jobs.manage', 'Manage Jobs', 'Manage job postings', 'Placement'),
('placement.jobs.publish', 'Publish Jobs', 'Publish job postings', 'Placement'),
('placement.drives.manage', 'Manage Placement Drives', 'Manage placement drives', 'Placement'),
('placement.applications.manage', 'Manage Job Applications', 'Manage student job applications', 'Placement'),
('placement.interviews.manage', 'Manage Interviews', 'Manage job interviews', 'Placement'),
('placement.offers.record', 'Record Job Offers', 'Record student job offers', 'Placement'),
('placement.salary.view', 'View Salary Info', 'View student salary info', 'Placement'),
-- Career
('career.own', 'Own Career Profile', 'Manage own career profile', 'Career'),
('career.manage', 'Manage Career Profiles', 'Manage student career profiles', 'Career'),
-- Article
('articles.view', 'View Articles', 'View knowledge base articles', 'Article'),
('articles.create', 'Create Articles', 'Create knowledge base articles', 'Article'),
('articles.publish', 'Publish Articles', 'Publish knowledge base articles', 'Article'),
-- CMS
('cms.manage', 'Manage CMS', 'Manage content management system', 'CMS'),
-- Event
('events.view', 'View Events', 'View platform events', 'Event'),
('events.manage', 'Manage Events', 'Manage platform events', 'Event'),
-- Notification
('notifications.manage', 'Manage Notifications', 'Manage system notifications', 'Notification'),
-- Audit
('audit.view', 'View Audit Logs', 'View system audit logs', 'Audit'),
-- Analytics
('analytics.view', 'View Analytics', 'View platform analytics', 'Analytics')
ON CONFLICT (name) DO NOTHING;

-- Assign Permissions to Roles
-- SUPER_ADMIN: ALL permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

-- ADMIN: all except platform.manage
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ADMIN' AND p.name != 'platform.manage'
ON CONFLICT DO NOTHING;

-- TEACHER: courses.view, students.view (own), attendance.view, attendance.manage
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'TEACHER' AND p.name IN ('courses.view', 'students.view', 'attendance.view', 'attendance.manage')
ON CONFLICT DO NOTHING;

-- MENTOR: mentor.students, mentor.sessions, students.view (assigned)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'MENTOR' AND p.name IN ('mentor.students', 'mentor.sessions', 'students.view')
ON CONFLICT DO NOTHING;

-- PLACEMENT_OFFICER: all placement.*, career.manage, students.view
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'PLACEMENT_OFFICER' AND (p.name LIKE 'placement.%' OR p.name IN ('career.manage', 'students.view'))
ON CONFLICT DO NOTHING;

-- SUPPORT_AGENT: support.view, support.manage, support.internal_notes, students.view
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'SUPPORT_AGENT' AND p.name IN ('support.view', 'support.manage', 'support.internal_notes', 'students.view')
ON CONFLICT DO NOTHING;

-- COUNSELLOR: crm.view, crm.manage, crm.assign, students.view
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'COUNSELLOR' AND p.name IN ('crm.view', 'crm.manage', 'crm.assign', 'students.view')
ON CONFLICT DO NOTHING;

-- STUDENT: career.own (learning.own implied by enrollment)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'STUDENT' AND p.name IN ('career.own')
ON CONFLICT DO NOTHING;
