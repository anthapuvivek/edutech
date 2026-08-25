-- V19: Add missing `version` column to live_classes table.
-- The LiveClass entity extends AuditableEntity which has @Version private Long version.
-- Without this column, spring.jpa.hibernate.ddl-auto=validate will fail on startup.
ALTER TABLE live_classes ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
