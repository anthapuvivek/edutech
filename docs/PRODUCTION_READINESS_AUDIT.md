# Learntrix Production Readiness Audit

This audit evaluates the current state of the Learntrix project (React frontend + Spring Boot backend) to identify what is working, what is broken or partially implemented, and what is missing to make the system production-ready for real-world deployment.

---

## 1. Executive Summary & Status Classification

| Feature Area | Status | Frontend Pages / Components | Backend Controller / Services / DB |
| :--- | :--- | :--- | :--- |
| **Authentication & Registration** | ⚠️ PARTIALLY WORKING | `login.tsx`, `register.tsx` | `AuthController.java`, `users` & `roles` tables |
| **Student Dashboard & Profile** | ✅ WORKING END-TO-END | `student.dashboard.tsx` | `StudentController.java`, `StudentService.java` |
| **Courses & Module Learning** | ✅ WORKING END-TO-END | `student.courses.tsx`, `courses.$slug.tsx` | `CourseController.java`, `CourseService.java` |
| **Class Recordings & Watch Progress** | ✅ WORKING END-TO-END | `student.recordings.tsx`, `student.recordings.$id.tsx` | `ClassRecordingController.java`, `StudentRecordingController.java` |
| **Live Classes (Student)** | ✅ WORKING END-TO-END | `student.live-classes.tsx` | `LiveClassController.java`, `LiveClassService.java` |
| **Teacher Dashboard & Student Lists** | ❌ BACKEND ONLY / STUBBED | `teacher.dashboard.tsx`, `teacher.students.tsx` | **Missing backend controller and service** |
| **Admin Console & Portal Stats** | ❌ BACKEND ONLY / STUBBED | `admin.dashboard.tsx`, `admin.students.index.tsx` | **Missing backend controller and service** |
| **Mentors & Student Session Booking** | ❌ FRONTEND ONLY (MOCK DATA) | `student.career.tsx`, `teacher.students.tsx` (mentor view) | **Missing entities, repositories, and controller** |
| **Career & Job Placements** | ❌ FRONTEND ONLY (MOCK DATA) | `student.career.jobs.tsx`, `admin.career.jobs.tsx` | **Missing entities, repositories, and controller** |
| **Assignments & Grading** | ❌ NOT IMPLEMENTED (FRONTEND SOON) | *Nav sidebar only (No React routes)* | **Missing entities, repositories, and controller** |
| **Quizzes & Attempts** | ❌ NOT IMPLEMENTED (FRONTEND SOON) | *Nav sidebar only (No React routes)* | **Missing entities, repositories, and controller** |

---

## 2. Detailed Feature Audits

### 2.1 Authentication & Registration
* **Classification:** ⚠️ PARTIALLY WORKING
* **Frontend Page/Component:** [login.tsx](file:///c:/Desktop/learntrix/src/routes/login.tsx) & [register.tsx](file:///c:/Desktop/learntrix/src/routes/register.tsx)
* **Frontend Service:** [auth.service.ts](file:///c:/Desktop/learntrix/src/services/auth.service.ts)
* **API Endpoints:** `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/profile`, `POST /api/auth/logout`
* **Controller:** [AuthController.java](file:///c:/Desktop/learntrix/backend/src/main/java/com/learntrix/edtech/controller/AuthController.java)
* **DTO:** `RegisterRequest`, `LoginRequest`, `LoginResponse`, `UserResponse`
* **Repository:** `UserRepository`, `RoleRepository`, `StudentProfileRepository`
* **Entity:** `User`, `Role`, `StudentProfile`
* **Database Table:** `users`, `roles`, `user_roles`, `student_profiles`
* **Flyway Migrations:** `V1__create_users_table.sql`, `V2__create_roles_and_permissions.sql`, `V3__create_student_profiles.sql`, `V4__seed_roles_and_permissions.sql`, `V9__seed_test_data.sql`, `V12__update_seed_passwords.sql`, `V14__fix_seed_passwords.sql`
* **Authentication Requirement:** Public (permitAll)
* **Authorization Requirement:** None for registration/login; authenticated context for profile/logout.
* **Validation:** Spring `@Valid` on request body properties (`email`, `password`, `name`).
* **Current Problems:**
  1. **CORS preflight rejection:** Browser sends `OPTIONS /api/auth/register` which gets blocked (403 Forbidden) by Spring Security because `OPTIONS` requests are not explicitly permitted in `SecurityConfig.java`.
  2. **Role mapping mismatch:** `mapToUserResponse` in `AuthController.java` only maps `"admin"`, `"teacher"`, and `"student"`. It maps `"mentor"`, `"placement_officer"`, etc. to `"student"`, breaking login redirects for those portals.
  3. **Missing DB seed accounts:** Seed users for `mentor@learntrix.com` and `placement@learntrix.com` are not created in PostgreSQL.
* **Required Fix:**
  * Update `SecurityConfig.java` to explicitly permit all HTTP `OPTIONS` preflight requests.
  * Correct the role mapping in `AuthController.mapToUserResponse` to map `MENTOR` to `"mentor"` and `PLACEMENT_OFFICER` to `"placement_officer"`.
  * Add a new Flyway migration `V15__seed_mentor_placement_users.sql` to insert demo mentor and placement officer accounts into `users` and assign their roles.

---

### 2.2 Student Dashboard & Profile
* **Classification:** ✅ WORKING END-TO-END
* **Frontend Page/Component:** [student.dashboard.tsx](file:///c:/Desktop/learntrix/src/routes/student.dashboard.tsx)
* **Frontend Service:** [student.service.ts](file:///c:/Desktop/learntrix/src/services/student.service.ts)
* **API Endpoints:** `GET /api/student/profile`, `GET /api/student/stats`, `GET /api/student/activity`
* **Controller:** [StudentController.java](file:///c:/Desktop/learntrix/backend/src/main/java/com/learntrix/edtech/controller/StudentController.java)
* **DTO:** `StudentProfileResponse`, `StudentStatsResponse`, `ActivityItemResponse`
* **Service:** [StudentService.java](file:///c:/Desktop/learntrix/backend/src/main/java/com/learntrix/edtech/service/StudentService.java)
* **Repository:** `StudentProfileRepository`, `UserRepository`, `EnrollmentRepository`, `RecordingWatchProgressRepository`
* **Entity:** `StudentProfile`, `User`, `Enrollment`, `RecordingWatchProgress`
* **Flyway Migrations:** `V3__create_student_profiles.sql`, `V10__add_gamification_columns_to_student_profiles.sql`, `V11__change_student_profiles_arrays_to_text.sql`
* **Authentication/Authorization:** Requires `ROLE_STUDENT`.
* **Current Problems:** Coding stats (`problemsSolved`) and quiz average are hardcoded/stubbed in `StudentService.java` because those backend modules are not implemented. This is acceptable for dashboard visualization, but should be backed by database counts once placement/coding tables are live.
* **Required Fix:** Clean up stubbed metrics to count from real database tables (e.g. counting from newly implemented jobs/applications or quiz/attempt tables).

---

### 2.3 Course Catalog & Study Player
* **Classification:** ✅ WORKING END-TO-END
* **Frontend Page/Component:** [student.courses.tsx](file:///c:/Desktop/learntrix/src/routes/student.courses.tsx), [courses.index.tsx](file:///c:/Desktop/learntrix/src/routes/courses.index.tsx), [courses.$slug.tsx](file:///c:/Desktop/learntrix/src/routes/courses.%24slug.tsx)
* **Frontend Service:** [course.service.ts](file:///c:/Desktop/learntrix/src/services/course.service.ts)
* **API Endpoints:** `GET /api/courses`, `GET /api/courses/featured`, `GET /api/courses/{slug}`, `POST /api/courses/{id}/enroll`, `GET /api/student/enrollments`, `GET /api/categories`, `GET /api/testimonials`, `GET /api/stats`
* **Controller:** [CourseController.java](file:///c:/Desktop/learntrix/backend/src/main/java/com/learntrix/edtech/controller/CourseController.java), [PublicContentController.java](file:///c:/Desktop/learntrix/backend/src/main/java/com/learntrix/edtech/controller/PublicContentController.java)
* **DTO:** `CourseResponse`, `EnrollmentResponse`, `CategoryResponse`, `TestimonialResponse`, `PlatformStatResponse`
* **Service:** [CourseService.java](file:///c:/Desktop/learntrix/backend/src/main/java/com/learntrix/edtech/service/CourseService.java)
* **Repository:** `CourseRepository`, `EnrollmentRepository`, `ModuleRepository`, `LessonRepository`
* **Entity:** `Course`, `Enrollment`, `Module`, `Lesson`
* **Database Table:** `courses`, `enrollments`, `modules`, `lessons`
* **Flyway Migrations:** `V7__create_courses_modules_lessons_enrollments.sql`, `V9__seed_test_data.sql`
* **Authentication/Authorization:** Public for course catalogs; requires `ROLE_STUDENT` for enrolling and viewing enrolled courses.
* **Current Problems:** None. Database structure is robust and matches the page details.
* **Required Fix:** None.

---

### 2.4 Class Recordings & Watch Progress
* **Classification:** ✅ WORKING END-TO-END
* **Frontend Page/Component:** [student.recordings.tsx](file:///c:/Desktop/learntrix/src/routes/student.recordings.tsx), [student.recordings.$id.tsx](file:///c:/Desktop/learntrix/src/routes/student.recordings.%24id.tsx), [student.courses.$slug.recordings.tsx](file:///c:/Desktop/learntrix/src/routes/student.courses.%24slug.recordings.tsx)
* **Frontend Service:** [recording.service.ts](file:///c:/Desktop/learntrix/src/services/recording.service.ts)
* **API Endpoints:** `GET /api/student/recordings`, `GET /api/student/recordings/course/{courseId}`, `GET /api/student/recordings/{id}`, `GET /api/student/recordings/{id}/progress`, `POST /api/student/recordings/{id}/progress`, `POST /api/student/recordings/{id}/complete`
* **Controller:** [StudentRecordingController.java](file:///c:/Desktop/learntrix/backend/src/main/java/com/learntrix/edtech/controller/StudentRecordingController.java), [ClassRecordingController.java](file:///c:/Desktop/learntrix/backend/src/main/java/com/learntrix/edtech/controller/ClassRecordingController.java) (Teacher), [AdminRecordingController.java](file:///c:/Desktop/learntrix/backend/src/main/java/com/learntrix/edtech/controller/AdminRecordingController.java) (Admin)
* **Service:** [ClassRecordingService.java](file:///c:/Desktop/learntrix/backend/src/main/java/com/learntrix/edtech/service/ClassRecordingService.java)
* **Repository:** `ClassRecordingRepository`, `RecordingWatchProgressRepository`
* **Entity:** `ClassRecording`, `RecordingWatchProgress`
* **Database Table:** `class_recordings`, `recording_watch_progress`
* **Flyway Migrations:** `V8__create_class_recordings_and_progress.sql`
* **Authentication/Authorization:** Requires `ROLE_STUDENT`, `ROLE_TEACHER`, or `ROLE_ADMIN` respectively.
* **Current Problems:** None. Endpoints and services are cleanly mapped.
* **Required Fix:** None.

---

### 2.5 Teacher Dashboard & Course Management
* **Classification:** ❌ BACKEND ONLY / STUBBED (API Contract Mismatch / Missing Controller)
* **Frontend Page/Component:** [teacher.dashboard.tsx](file:///c:/Desktop/learntrix/src/routes/teacher.dashboard.tsx), [teacher.students.tsx](file:///c:/Desktop/learntrix/src/routes/teacher.students.tsx)
* **Frontend Service:** [teacher.service.ts](file:///c:/Desktop/learntrix/src/services/teacher.service.ts)
* **API Endpoints:** `GET /api/teacher/stats`, `GET /api/teacher/students`, `GET /api/teacher/students/{id}`
* **Current Problems:**
  1. There is no `TeacherController` on the backend to handle stats or teacher-student views. When the frontend sets `VITE_USE_MOCKS=false`, the dashboard fails with 404.
  2. The teacher portal navigation bar contains items like "My Courses", "Batches", "Assignments", and "Quizzes" marked as "soon" (not linking anywhere) because no React routes exist for them.
* **Required Fix:**
  * Implement `TeacherController.java` to expose `/api/teacher/stats` and `/api/teacher/students` endpoints returning database stats (e.g. course counts, enrolled student count, average quiz scores) and students lists associated with the logged-in teacher's courses.

---

### 2.6 Admin Console & Platform Management
* **Classification:** ❌ BACKEND ONLY / STUBBED (API Contract Mismatch / Missing Controller)
* **Frontend Page/Component:** [admin.dashboard.tsx](file:///c:/Desktop/learntrix/src/routes/admin.dashboard.tsx), [admin.students.index.tsx](file:///c:/Desktop/learntrix/src/routes/admin.students.index.tsx)
* **Frontend Service:** [admin.service.ts](file:///c:/Desktop/learntrix/src/services/admin.service.ts)
* **API Endpoints:** `GET /api/admin/stats`, `GET /api/admin/overview`, `GET /api/admin/setup`, `GET /api/admin/events`, `GET /api/admin/students`, `POST /api/admin/students`
* **Current Problems:**
  1. No admin endpoints are defined on the backend except the `AdminRecordingController`. The dashboard fails with 404 when mocks are off.
* **Required Fix:**
  * Implement `AdminController.java` to expose `/api/admin/stats`, `/api/admin/overview`, and `/api/admin/students` returning platform statistics, student account lists, batches, and auditor logs.

---

### 2.7 Mentors & Student Guidance
* **Classification:** ❌ FRONTEND ONLY (MOCK DATA)
* **Frontend Page/Component:** `mentor.*` layouts and navigation views
* **Frontend Service:** [mentor.service.ts](file:///c:/Desktop/learntrix/src/services/mentor.service.ts)
* **API Endpoints:** `GET /api/mentors`, `GET /api/mentor/stats`, `GET /api/mentor/students`, `POST /api/mentor/sessions`, `POST /api/mentor/notes`
* **Current Problems:** No backend models, tables, repositories, or controllers exist.
* **Required Fix:**
  * Create `mentoring_sessions` and `mentoring_notes` tables in a new Flyway migration.
  * Implement corresponding JPA entities (`MentoringSession`, `MentoringNote`), repositories, and `MentorController.java` in the backend to store and retrieve mentoring schedules and notes.

---

### 2.8 Career & Job Placements (Placement Office)
* **Classification:** ❌ FRONTEND ONLY (MOCK DATA)
* **Frontend Page/Component:** `student.career.jobs.tsx`, `admin.career.jobs.tsx`, `admin.career.applications.tsx`
* **Frontend Service:** [career.service.ts](file:///c:/Desktop/learntrix/src/services/career.service.ts), [job.service.ts](file:///c:/Desktop/learntrix/src/services/job.service.ts), [placement.service.ts](file:///c:/Desktop/learntrix/src/services/placement.service.ts)
* **API Endpoints:** `/api/career/eligibility`, `/api/career/dashboard`, `/api/career/jobs`, `/api/career/companies`, `/api/career/applications`, `/api/placement/stats`, `/api/placement/drives`, `/api/placement/applications`, `/api/placement/interviews`, `/api/placement/offers`
* **Current Problems:** No database tables or controllers exist on the backend for companies, job listings, drives, applications, or interviews.
* **Required Fix:**
  * Create `companies`, `jobs`, `job_applications`, `placement_drives`, `placement_interviews`, `placement_offers` tables via a new Flyway migration.
  * Implement JPA entities, repositories, and controllers (`JobController.java` and `PlacementController.java`) to allow posting jobs/drives, letting students apply, and updating application status.

---

### 2.9 Assignments & Quizzes
* **Classification:** ❌ NOT IMPLEMENTED (Blocked on Frontend)
* **Current Problems:**
  1. The React router tree ([routeTree.gen.ts](file:///c:/Desktop/learntrix/src/routeTree.gen.ts)) has NO pages or routes defined for quizzes or assignments.
  2. The navigation layouts render these items as "soon" (disabled spans), meaning they are not interactive.
* **Required Fix (Reported Restriction):**
  * Because the UI layouts and routing paths are entirely missing on the frontend for assignments/quizzes, these user flows are **blocked** from end-to-end execution. 
  * However, to ensure the backend is fully prepared, we will define the JPA models and SQL schema for `assignments`, `assignment_submissions`, `quizzes`, and `quiz_attempts` so that the database structure is complete.

---

## 3. Summary of Core Blockers

1. **CORS OPTIONS Preflight Blocks (403 Forbidden):** Standard security filters intercept preflight requests, causing registration and logins to fail immediately when mocks are disabled.
2. **Missing Mentor / Placement Role Handling:** Mentor and placement officers are parsed as "student" due to hardcoded mapping in `AuthController.java`.
3. **Missing DB Seed Users:** No mentor or placement officer accounts exist in the initial test SQL dataset.
4. **Missing Placement, Mentor, Teacher, and Admin controllers:** Any request made by these portals fails with a `404 Not Found` when mock data is bypassed.
