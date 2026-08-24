-- Add created_by and updated_by columns to live_classes table
ALTER TABLE live_classes ADD COLUMN created_by VARCHAR(255) NOT NULL DEFAULT 'SYSTEM';
ALTER TABLE live_classes ADD COLUMN updated_by VARCHAR(255) NOT NULL DEFAULT 'SYSTEM';
