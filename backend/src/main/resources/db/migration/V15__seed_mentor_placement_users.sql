-- Seed Mentor User
INSERT INTO users (id, email, password_hash, name, status, email_verified)
VALUES ('00000000-0000-0000-0000-000000000004'::uuid, 'mentor@learntrix.com', '$2a$10$iq9BiCjxVz5RmM7HsKjy3OeavCZ7mCzSQEiEtgwj/nzRG/3yzHUDu', 'Rajesh Kumar', 'ACTIVE', TRUE)
ON CONFLICT (email) DO NOTHING;

-- Seed Placement Officer User
INSERT INTO users (id, email, password_hash, name, status, email_verified)
VALUES ('00000000-0000-0000-0000-000000000005'::uuid, 'placement@learntrix.com', '$2a$10$iq9BiCjxVz5RmM7HsKjy3OeavCZ7mCzSQEiEtgwj/nzRG/3yzHUDu', 'Sneha Reddy', 'ACTIVE', TRUE)
ON CONFLICT (email) DO NOTHING;

-- Assign Mentor Role
INSERT INTO user_roles (user_id, role_id)
SELECT '00000000-0000-0000-0000-000000000004'::uuid, id FROM roles WHERE name = 'MENTOR'
ON CONFLICT DO NOTHING;

-- Assign Placement Officer Role
INSERT INTO user_roles (user_id, role_id)
SELECT '00000000-0000-0000-0000-000000000005'::uuid, id FROM roles WHERE name = 'PLACEMENT_OFFICER'
ON CONFLICT DO NOTHING;
