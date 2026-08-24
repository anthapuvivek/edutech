UPDATE users 
SET password_hash = '$2a$10$gRst75Ur8z5Y5i.w6r1qE.gJzN.62L3s5C/w1q8vH2V.P.g1rZtJq' 
WHERE email IN ('student@learntrix.com', 'teacher@learntrix.com', 'admin@learntrix.com');
