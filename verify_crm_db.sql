SELECT id, name, email, phone, course_interest, stage, assigned_to_id, created_at FROM leads WHERE email = 'kavya.deshmukh@gmail.com';
SELECT id, lead_id, name, email, phone, experience_level, learning_mode, status, created_at FROM enquiries WHERE email = 'kavya.deshmukh@gmail.com';
SELECT id, lead_id, author_name, body, created_at FROM lead_notes WHERE lead_id IN (SELECT id FROM leads WHERE email = 'kavya.deshmukh@gmail.com');
SELECT id, lead_id, author_name, channel, summary, created_at FROM lead_communications WHERE lead_id IN (SELECT id FROM leads WHERE email = 'kavya.deshmukh@gmail.com');
SELECT id, lead_id, assigned_to_name, follow_up_date, follow_up_time, next_action, status, created_at FROM lead_follow_ups WHERE lead_id IN (SELECT id FROM leads WHERE email = 'kavya.deshmukh@gmail.com');
