-- V28__add_auditing_columns_to_materials_and_announcements.sql

ALTER TABLE course_materials
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) DEFAULT 'system';

ALTER TABLE course_announcements
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) DEFAULT 'system';
