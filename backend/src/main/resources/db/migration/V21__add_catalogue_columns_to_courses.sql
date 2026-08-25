-- Give courses the catalogue attributes the storefront filters on.
--
-- CourseService.mapToCourseResponse previously returned hardcoded constants for level,
-- price, rating and duration. Because every course carried identical values, the filter
-- sidebar in src/routes/courses.index.tsx could not do anything: the client sent level,
-- maxPrice, minRating and sort, and CourseController had nowhere to apply them.
--
-- Additive with NOT NULL DEFAULT so existing rows stay valid; the backfill below
-- reproduces exactly the constants the mapper used, so behaviour is unchanged until
-- real per-course values are entered.

ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS level           VARCHAR(20)      NOT NULL DEFAULT 'Intermediate',
    ADD COLUMN IF NOT EXISTS language        VARCHAR(50)      NOT NULL DEFAULT 'English',
    ADD COLUMN IF NOT EXISTS duration_hours  INTEGER          NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS lesson_count    INTEGER          NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS rating          DOUBLE PRECISION NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS rating_count    INTEGER          NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS student_count   INTEGER          NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS price           INTEGER          NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS original_price  INTEGER,
    ADD COLUMN IF NOT EXISTS currency        VARCHAR(3)       NOT NULL DEFAULT 'INR';

-- Backfill rows created before this migration with the previous hardcoded values.
UPDATE courses
SET level          = 'Intermediate',
    language       = 'English',
    duration_hours = 48,
    lesson_count   = 12,
    rating         = 4.8,
    rating_count   = 1240,
    student_count  = 3560,
    price          = 14999,
    original_price = 24999,
    currency       = 'INR'
WHERE duration_hours = 0 AND price = 0;

CREATE INDEX IF NOT EXISTS idx_courses_level ON courses(level);
CREATE INDEX IF NOT EXISTS idx_courses_price ON courses(price);
