-- V29__make_recording_module_lesson_optional.sql
--
-- A trainer must be able to upload a class recording even when the course curriculum
-- has not been built out yet - a course with no modules, or a module with no lessons,
-- previously made the upload impossible because module_id and lesson_id were NOT NULL.
-- Recordings are never public: access is limited to the students allocated to the
-- uploading trainer, so an unmapped recording is safe to store and serve.
--
-- The columns keep their foreign keys, so a mapping that IS supplied is still verified.

ALTER TABLE class_recordings
    ALTER COLUMN module_id DROP NOT NULL;

ALTER TABLE class_recordings
    ALTER COLUMN lesson_id DROP NOT NULL;

-- ON DELETE CASCADE would delete the whole recording when a lesson or module is removed
-- during a curriculum edit. Now that the mapping is optional, detach it instead so the
-- uploaded video survives a curriculum change.
ALTER TABLE class_recordings
    DROP CONSTRAINT IF EXISTS class_recordings_module_id_fkey;

ALTER TABLE class_recordings
    ADD CONSTRAINT class_recordings_module_id_fkey
    FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE SET NULL;

ALTER TABLE class_recordings
    DROP CONSTRAINT IF EXISTS class_recordings_lesson_id_fkey;

ALTER TABLE class_recordings
    ADD CONSTRAINT class_recordings_lesson_id_fkey
    FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE SET NULL;
