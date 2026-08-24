ALTER TABLE student_profiles DROP COLUMN IF EXISTS skills;
ALTER TABLE student_profiles DROP COLUMN IF EXISTS preferred_locations;

ALTER TABLE student_profiles ADD COLUMN skills TEXT;
ALTER TABLE student_profiles ADD COLUMN preferred_locations TEXT;
