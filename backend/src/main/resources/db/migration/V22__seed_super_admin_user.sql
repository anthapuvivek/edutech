-- Seed Super Admin User
INSERT INTO users (id, email, password_hash, name, status, email_verified)
VALUES ('00000000-0000-0000-0000-000000000006'::uuid, 'super@learntrix.com', '$2a$10$iq9BiCjxVz5RmM7HsKjy3OeavCZ7mCzSQEiEtgwj/nzRG/3yzHUDu', 'Super Admin', 'ACTIVE', TRUE)
ON CONFLICT (email) DO NOTHING;

-- Assign SUPER_ADMIN Role
INSERT INTO user_roles (user_id, role_id)
SELECT '00000000-0000-0000-0000-000000000006'::uuid, id FROM roles WHERE name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;
