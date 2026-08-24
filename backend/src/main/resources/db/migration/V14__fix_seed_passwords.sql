-- Migration to update the corrupted password hash for seed/demo users
-- Correct BCrypt hash of 'password' is '$2a$10$iq9BiCjxVz5RmM7HsKjy3OeavCZ7mCzSQEiEtgwj/nzRG/3yzHUDu'

UPDATE users 
SET password_hash = '$2a$10$iq9BiCjxVz5RmM7HsKjy3OeavCZ7mCzSQEiEtgwj/nzRG/3yzHUDu' 
WHERE email IN ('student@learntrix.com', 'teacher@learntrix.com', 'admin@learntrix.com');
