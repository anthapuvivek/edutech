-- Align the RBAC tables with the permission vocabulary the frontend actually checks.
--
-- Before this migration the UI read its permission table from src/mock/ops.ts, because
-- three permissions it depends on did not exist in the database and the V4 role grants did
-- not match. Serving GET /api/rbac/roles from these tables without this alignment would
-- have locked every role out of its own landing page:
--   * TEACHER had no 'teaching.manage'  -> /teacher/dashboard denied
--   * STUDENT had no 'learning.own'     -> /student/dashboard denied
--   * ADMIN was explicitly denied 'platform.manage' -> /admin/dashboard denied
--
-- Grants are additive: the finer-grained V4 permissions stay in place for future
-- server-side checks. Everything here is idempotent.

-- 1. Permissions the UI checks that V4 never created.
INSERT INTO permissions (name, display_name, description, module) VALUES
('teaching.manage', 'Manage Teaching', 'Manage assigned courses, batches and students', 'Teaching'),
('learning.view', 'View Learning Data', 'View student learning progress', 'Learning'),
('learning.own', 'Own Learning', 'Access own enrolments, classes and progress', 'Learning')
ON CONFLICT (name) DO NOTHING;

-- 2. SUPER_ADMIN keeps every permission, including the three added above.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

-- 3. ADMIN: platform administration plus oversight of teaching, career and placement.
--    V4 deliberately withheld platform.manage; the admin console requires it.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN' AND p.name IN (
    'platform.manage', 'teaching.manage', 'learning.view', 'users.manage',
    'coupons.manage',
    'crm.view', 'crm.manage', 'crm.assign',
    'support.view', 'support.manage', 'support.internal_notes',
    'mentor.assign',
    'placement.students.view', 'placement.jobs.view', 'placement.jobs.manage',
    'placement.jobs.publish', 'placement.drives.manage', 'placement.applications.manage',
    'placement.interviews.manage', 'placement.offers.record', 'placement.salary.view',
    'audit.view'
)
ON CONFLICT DO NOTHING;

-- 4. TEACHER: own courses, batches and students.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'TEACHER' AND p.name IN (
    'teaching.manage', 'learning.view', 'mentor.students',
    'support.view', 'support.internal_notes'
)
ON CONFLICT DO NOTHING;

-- 5. MENTOR: assigned students, sessions, notes and action plans.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'MENTOR' AND p.name IN (
    'mentor.students', 'mentor.sessions', 'learning.view',
    'support.view', 'support.internal_notes'
)
ON CONFLICT DO NOTHING;

-- 6. PLACEMENT_OFFICER: jobs, drives, applications, interviews and offers.
--    Salary visibility stays with ADMIN and SUPER_ADMIN only.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'PLACEMENT_OFFICER' AND p.name IN (
    'placement.students.view', 'placement.jobs.view', 'placement.jobs.manage',
    'placement.jobs.publish', 'placement.drives.manage', 'placement.applications.manage',
    'placement.interviews.manage', 'placement.offers.record',
    'learning.view', 'support.view'
)
ON CONFLICT DO NOTHING;

-- 7. SUPPORT_AGENT: support desk only.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPPORT_AGENT' AND p.name IN (
    'support.view', 'support.manage', 'support.internal_notes'
)
ON CONFLICT DO NOTHING;

-- 8. COUNSELLOR: CRM leads and follow-ups.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'COUNSELLOR' AND p.name IN ('crm.view', 'crm.manage')
ON CONFLICT DO NOTHING;

-- 9. STUDENT: own learning and own career data.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'STUDENT' AND p.name IN ('learning.own', 'career.own')
ON CONFLICT DO NOTHING;

-- 10. PLACEMENT_OFFICER must not inherit salary visibility from any earlier grant.
DELETE FROM role_permissions rp
USING roles r, permissions p
WHERE rp.role_id = r.id AND rp.permission_id = p.id
  AND r.name = 'PLACEMENT_OFFICER' AND p.name = 'placement.salary.view';
