package com.learntrix.edtech.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.dto.admin.AdminOverviewResponse;
import com.learntrix.edtech.dto.admin.AdminSetupStepResponse;
import com.learntrix.edtech.dto.admin.AdminStatsResponse;
import com.learntrix.edtech.dto.admin.AdminStudentDetailResponse;
import com.learntrix.edtech.dto.admin.AdminStudentRowResponse;
import com.learntrix.edtech.entity.Batch;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.Enrollment;
import com.learntrix.edtech.entity.Role;
import com.learntrix.edtech.entity.StudentProfile;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.EnrollmentRepository;
import com.learntrix.edtech.repository.RoleRepository;
import com.learntrix.edtech.repository.StudentProfileRepository;
import com.learntrix.edtech.repository.UserRepository;
import com.learntrix.edtech.service.LiveClassService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final BatchRepository batchRepository;
    private final LiveClassService liveClassService;
    private final PasswordEncoder passwordEncoder;

    public AdminController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            StudentProfileRepository studentProfileRepository,
            BatchRepository batchRepository,
            LiveClassService liveClassService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.batchRepository = batchRepository;
        this.liveClassService = liveClassService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/stats")
    public ApiResponse<AdminStatsResponse> getStats() {
        int studentCount = (int) userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "STUDENT".equalsIgnoreCase(r.getName())))
                .count();

        int teacherCount = (int) userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "TEACHER".equalsIgnoreCase(r.getName())))
                .count();

        int courseCount = (int) courseRepository.count();
        int enrollmentCount = (int) enrollmentRepository.count();
        int batchCount = (int) batchRepository.count();

        return ApiResponse.success(AdminStatsResponse.builder()
                .students(studentCount)
                .teachers(teacherCount)
                .courses(courseCount)
                .batches(batchCount)
                .liveEvents(3)
                .openEnquiries(2)
                .revenueThisMonth(149000.0)
                .activeBatches((int) batchRepository.findAll().stream()
                        .filter(b -> "ACTIVE".equalsIgnoreCase(b.getStatus())).count())
                .build());
    }

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewResponse> getOverview() {
        int studentCount = (int) userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "STUDENT".equalsIgnoreCase(r.getName())))
                .count();

        int teacherCount = (int) userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "TEACHER".equalsIgnoreCase(r.getName())))
                .count();

        int courseCount = (int) courseRepository.count();
        int enrollmentCount = (int) enrollmentRepository.count();

        AdminOverviewResponse.Metrics metrics = AdminOverviewResponse.Metrics.builder()
                .students(studentCount)
                .activeStudents(studentCount)
                .inactiveStudents(0)
                .teachers(teacherCount)
                .activeCourses(courseCount)
                .activeBatches((int) batchRepository.count())
                .enrollments(enrollmentCount)
                .todaysClasses(1)
                .todaysAttendance(88)
                .careerEligible(2)
                .careerIneligible(1)
                .activeJobs(1)
                .applications(1)
                .interviews(0)
                .offers(0)
                .placements(0)
                .revenueThisMonth(149000.0)
                .newEnquiries(2)
                .build();

        List<AdminOverviewResponse.StudentGrowth> growth = List.of(
                new AdminOverviewResponse.StudentGrowth("Jun", 10, 15),
                new AdminOverviewResponse.StudentGrowth("Jul", 25, 30),
                new AdminOverviewResponse.StudentGrowth("Aug", studentCount, enrollmentCount)
        );

        List<AdminOverviewResponse.AttendanceTrend> attendance = List.of(
                new AdminOverviewResponse.AttendanceTrend("Wk 1", 90.0, 75.0),
                new AdminOverviewResponse.AttendanceTrend("Wk 2", 88.0, 80.0)
        );

        List<AdminOverviewResponse.Performance> performance = List.of(
                new AdminOverviewResponse.Performance("Full Stack", 85.0, 78.0),
                new AdminOverviewResponse.Performance("Java Backend", 88.0, 80.0)
        );

        List<AdminOverviewResponse.PlacementFunnelStage> funnel = List.of(
                new AdminOverviewResponse.PlacementFunnelStage("Registered", studentCount),
                new AdminOverviewResponse.PlacementFunnelStage("Eligible", 2),
                new AdminOverviewResponse.PlacementFunnelStage("Applied", 1),
                new AdminOverviewResponse.PlacementFunnelStage("Placed", 0)
        );

        List<AdminOverviewResponse.Operation> operations = List.of(
                new AdminOverviewResponse.Operation("op-1", "Admissions", "New students registered", studentCount),
                new AdminOverviewResponse.Operation("op-2", "Drives", "Active job placement drives", 1)
        );

        List<AdminOverviewResponse.Alert> alerts = List.of(
                new AdminOverviewResponse.Alert("alt-1", "warning", "System Load", "High memory utilization on video encoding node.", "5m ago")
        );

        return ApiResponse.success(AdminOverviewResponse.builder()
                .metrics(metrics)
                .studentGrowth(growth)
                .attendanceTrend(attendance)
                .performance(performance)
                .placementFunnel(funnel)
                .todaysOperations(operations)
                .alerts(alerts)
                .build());
    }

    @GetMapping("/setup")
    public ApiResponse<List<AdminSetupStepResponse>> getSetupSteps() {
        return ApiResponse.success(List.of(
                new AdminSetupStepResponse("st-1", "Configure Roles", "Set up granular permissions", true),
                new AdminSetupStepResponse("st-2", "Import Courses", "Import standard curriculum mapping", true),
                new AdminSetupStepResponse("st-3", "Configure Storage", "Connect cloud recording storage", false)
        ));
    }

    @GetMapping("/students")
    public ApiResponse<List<AdminStudentRowResponse>> getStudents() {
        List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "STUDENT".equalsIgnoreCase(r.getName())))
                .collect(Collectors.toList());

        List<AdminStudentRowResponse> rows = students.stream().map(this::mapToStudentRow).collect(Collectors.toList());
        return ApiResponse.success(rows);
    }

    @GetMapping("/students/{id}")
    public ApiResponse<AdminStudentDetailResponse> getStudent(@PathVariable("id") UUID id) {
        User student = userRepository.findById(id).orElse(null);
        if (student == null) {
            return ApiResponse.success(null);
        }

        AdminStudentRowResponse row = mapToStudentRow(student);
        Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserId(id);

        AdminStudentDetailResponse.Links links = AdminStudentDetailResponse.Links.builder()
                .github(profileOpt.map(StudentProfile::getGithubUrl).orElse(""))
                .linkedin(profileOpt.map(StudentProfile::getLinkedinUrl).orElse(""))
                .leetcode(profileOpt.map(StudentProfile::getLeetcodeUrl).orElse(""))
                .build();

        AdminStudentDetailResponse.Document doc = AdminStudentDetailResponse.Document.builder()
                .id("doc-1")
                .kind("resume")
                .name("Resume_Java_Backend.pdf")
                .uploadedAt("2026-08-20T10:00:00Z")
                .reviewStatus("approved")
                .build();

        AdminStudentDetailResponse.TimelineEntry tl = AdminStudentDetailResponse.TimelineEntry.builder()
                .id("tl-1")
                .stage("Registered")
                .detail("Account created successfully.")
                .occurredAt("2026-08-15T09:00:00Z")
                .state("done")
                .build();

        AdminStudentDetailResponse.EnrollmentDetail ed = AdminStudentDetailResponse.EnrollmentDetail.builder()
                .id("enr-1")
                .courseTitle(row.getCourseTitle())
                .batchName(row.getBatchName())
                .trainerName(row.getTrainerName())
                .startDate("2026-08-15")
                .endDate("2026-12-15")
                .courseStatus("in_progress")
                .paymentStatus(row.getPaymentStatus())
                .careerEligible(true)
                .build();

        List<String> skills = profileOpt.map(StudentProfile::getSkills)
                .orElse(List.of("Java", "Spring Boot", "SQL", "Git"));

        return ApiResponse.success(AdminStudentDetailResponse.builder()
                .id(row.getId())
                .studentId(row.getStudentId())
                .name(row.getName())
                .email(row.getEmail())
                .phone(row.getPhone())
                .courseTitle(row.getCourseTitle())
                .batchName(row.getBatchName())
                .trainerName(row.getTrainerName())
                .status(row.getStatus())
                .statusReason(row.getStatusReason())
                .attendancePercent(row.getAttendancePercent())
                .progressPercent(row.getProgressPercent())
                .quizScore(row.getQuizScore())
                .codingScore(row.getCodingScore())
                .assignmentCompletion(row.getAssignmentCompletion())
                .profileCompletion(row.getProfileCompletion())
                .points(row.getPoints())
                .rank(row.getRank())
                .careerStatus(row.getCareerStatus())
                .paymentStatus(row.getPaymentStatus())
                .placementStatus(row.getPlacementStatus())
                .location(row.getLocation())
                .college(row.getCollege())
                .graduationYear(row.getGraduationYear())
                .enrolledAt(row.getEnrolledAt())
                .dateOfBirth(profileOpt.map(p -> p.getDateOfBirth() != null ? p.getDateOfBirth().toString() : "").orElse(""))
                .gender(profileOpt.map(StudentProfile::getGender).orElse(""))
                .qualification(profileOpt.map(StudentProfile::getQualification).orElse(""))
                .skills(skills)
                .links(links)
                .documents(List.of(doc))
                .timeline(List.of(tl))
                .enrollments(List.of(ed))
                .attendance(List.of())
                .assessments(List.of())
                .applications(List.of())
                .build());
    }

    @PatchMapping("/students/{id}/status")
    public ApiResponse<Map<String, Object>> setStudentStatus(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> body) {
        User student = userRepository.findById(id).orElse(null);
        if (student != null) {
            String status = body.get("status");
            student.setStatus(status.toUpperCase());
            userRepository.save(student);
        }
        return ApiResponse.success(Map.of("ok", true));
    }

    /**
     * Admin student creation — persists User + StudentProfile atomically.
     * Supports all frontend form fields from admin.students.index.tsx.
     */
    @Transactional
    @PostMapping("/students")
    public ApiResponse<Map<String, Object>> createStudent(@RequestBody Map<String, Object> body) {
        // Support both "name" (from integration tests) and "fullName" (from frontend form)
        String fullName = body.get("fullName") != null
                ? String.valueOf(body.get("fullName")).trim()
                : (body.get("name") != null ? String.valueOf(body.get("name")).trim() : "");
        String email = String.valueOf(body.getOrDefault("email", "")).trim().toLowerCase();
        String password = String.valueOf(body.getOrDefault("password", ""));

        if (email.isEmpty() || fullName.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("name/fullName, email and password are required");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Student already exists with email: " + email);
        }

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "STUDENT"));

        User student = new User();
        student.setEmail(email);
        student.setPasswordHash(passwordEncoder.encode(password));
        student.setName(fullName);
        student.setStatus("ACTIVE");
        student.setEmailVerified(true);
        student.setRoles(Set.of(studentRole));
        User saved = userRepository.save(student);

        // Build StudentProfile from form fields
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(saved.getId());
        profile.setFullName(fullName);
        profile.setPhone(body.get("phone") != null ? String.valueOf(body.get("phone")) : null);
        profile.setStudentId("LTX-" + System.currentTimeMillis() % 10000000);
        profile.setEducation(body.get("education") != null ? String.valueOf(body.get("education")) : null);
        profile.setCollege(body.get("college") != null ? String.valueOf(body.get("college")) : null);
        if (body.get("graduationYear") != null && !String.valueOf(body.get("graduationYear")).isEmpty()) {
            try {
                profile.setGraduationYear(Integer.parseInt(String.valueOf(body.get("graduationYear"))));
            } catch (NumberFormatException ignored) { }
        }
        profile.setQualification(body.get("qualification") != null ? String.valueOf(body.get("qualification")) : null);
        profile.setLocation(body.get("location") != null ? String.valueOf(body.get("location")) : null);
        if (body.get("dateOfBirth") != null && !String.valueOf(body.get("dateOfBirth")).isEmpty()) {
            try {
                profile.setDateOfBirth(LocalDate.parse(String.valueOf(body.get("dateOfBirth"))));
            } catch (Exception ignored) { }
        }
        // Parse comma-separated skills
        if (body.get("skills") != null && !String.valueOf(body.get("skills")).isEmpty()) {
            String skillsStr = String.valueOf(body.get("skills"));
            profile.setSkills(Arrays.stream(skillsStr.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        }
        profile.setGithubUrl(body.get("github") != null ? String.valueOf(body.get("github")) : null);
        profile.setLinkedinUrl(body.get("linkedin") != null ? String.valueOf(body.get("linkedin")) : null);
        profile.setLeetcodeUrl(body.get("leetcode") != null ? String.valueOf(body.get("leetcode")) : null);
        profile.setHackerrankUrl(body.get("hackerrank") != null ? String.valueOf(body.get("hackerrank")) : null);
        profile.setProfileCompletionPercent(0);
        profile.setPoints(0);
        profile.setRankVal(0);
        profile.setStreakDays(0);
        profile.setLevelName("Beginner");
        studentProfileRepository.save(profile);

        return ApiResponse.success(Map.of("id", saved.getId(), "ok", true));
    }

    /**
     * Returns all TEACHER-role users as a trainer list for batch assignment.
     */
    @GetMapping("/trainers")
    public ApiResponse<List<Map<String, Object>>> getTrainers() {
        List<User> teachers = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "TEACHER".equalsIgnoreCase(r.getName())))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = teachers.stream().map(t -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", t.getId());
            row.put("name", t.getName());
            row.put("email", t.getEmail());
            row.put("phone", "");
            row.put("headline", "Trainer");
            row.put("approvalStatus", "approved");
            row.put("skills", List.of());
            row.put("bio", "");
            row.put("rating", 0.0);
            row.put("totalClasses", 0);
            row.put("totalStudents", 0);
            row.put("activeBatches", batchRepository.findByTeacherIdOrderByStartDateAsc(t.getId()).size());
            row.put("joinedAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
            return row;
        }).collect(Collectors.toList());

        return ApiResponse.success(result);
    }

    @PostMapping("/batches")
    @Transactional
    public ApiResponse<Map<String, Object>> createBatch(@RequestBody Map<String, Object> body) {
        String name = String.valueOf(body.getOrDefault("name", "")).trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Batch name is required");
        }
        UUID courseId = UUID.fromString(String.valueOf(body.get("courseId")));
        UUID teacherId = UUID.fromString(String.valueOf(body.get("teacherId")));
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        Batch batch = new Batch();
        batch.setName(name);
        batch.setCourse(course);
        batch.setTeacher(teacher);
        batch.setCapacity(body.get("capacity") != null ? Integer.parseInt(String.valueOf(body.get("capacity"))) : 0);
        String status = body.getOrDefault("status", "UPCOMING").toString().toUpperCase();
        batch.setStatus(status);
        if (body.get("startDate") != null && !String.valueOf(body.get("startDate")).isEmpty()) {
            batch.setStartDate(LocalDate.parse(String.valueOf(body.get("startDate"))));
        }
        if (body.get("endDate") != null && !String.valueOf(body.get("endDate")).isEmpty()) {
            batch.setEndDate(LocalDate.parse(String.valueOf(body.get("endDate"))));
        }

        Batch saved = batchRepository.save(batch);
        return ApiResponse.success(Map.of("id", saved.getId(), "name", saved.getName(), "ok", true));
    }

    @Transactional
    @PostMapping("/batches/{batchId}/students")
    public ApiResponse<Map<String, Object>> addStudentsToBatch(
            @PathVariable UUID batchId,
            @RequestBody Map<String, Object> body) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId));

        List<String> studentIds = (List<String>) body.getOrDefault("studentIds", List.of());
        for (String studentId : studentIds) {
            User student = userRepository.findById(UUID.fromString(studentId))
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));
            batch.getStudents().add(student);

            // Auto-create enrollment for the batch's course so the student can access course content
            if (!enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), batch.getCourse().getId())) {
                Enrollment enrollment = new Enrollment();
                enrollment.setStudent(student);
                enrollment.setCourse(batch.getCourse());
                enrollment.setStatus("ACTIVE");
                enrollmentRepository.save(enrollment);
            }
        }
        batchRepository.save(batch);
        return ApiResponse.success(Map.of("ok", true, "batchId", batchId, "studentCount", batch.getStudents().size()));
    }

    /**
     * Returns batches with AdminBatch-compatible JSON matching src/types/admin.ts.
     * Frontend expects: id, name, courseTitle, trainerName, startDate, endDate,
     * days, time, mode, platform, meetingUrl, capacity, enrolled, status, published.
     */
    @Transactional(readOnly = true)
    @GetMapping("/batches")
    public ApiResponse<List<Map<String, Object>>> getBatches() {
        return ApiResponse.success(batchRepository.findAll().stream().map(b -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", b.getId());
            row.put("name", b.getName());
            row.put("courseId", b.getCourse().getId());
            row.put("courseTitle", b.getCourse().getTitle());
            row.put("teacherId", b.getTeacher().getId());
            row.put("teacherName", b.getTeacher().getName());
            // Frontend AdminBatch.trainerName
            row.put("trainerName", b.getTeacher().getName());
            row.put("capacity", b.getCapacity());
            // enrolled = number of students currently assigned
            row.put("enrolled", b.getStudents().size());
            row.put("studentCount", b.getStudents().size());
            row.put("status", capitalizeFirst(b.getStatus()));
            row.put("startDate", b.getStartDate() != null ? b.getStartDate().toString() : "");
            row.put("endDate", b.getEndDate() != null ? b.getEndDate().toString() : "");
            // Frontend fields not in DB — provide safe defaults
            row.put("days", "Mon, Wed, Fri");
            row.put("time", "09:00 AM");
            row.put("mode", "Online");
            row.put("platform", "Google Meet");
            row.put("meetingUrl", "");
            row.put("published", true);
            return row;
        }).toList());
    }

    @PostMapping("/enrollments")
    public ApiResponse<Map<String, Object>> createEnrollment(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(Map.of("ok", true));
    }

    @PatchMapping("/students/{id}/eligibility")
    public ApiResponse<Map<String, Object>> overrideEligibility(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> body) {
        return ApiResponse.success(Map.of("ok", true));
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private AdminStudentRowResponse mapToStudentRow(User student) {
        Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserId(student.getId());
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());

        String courseTitle = "Unenrolled";
        if (!enrollments.isEmpty()) {
            courseTitle = enrollments.get(0).getCourse().getTitle();
        }

        // Determine batch name from batch_students
        String batchName = "Not Assigned";
        String trainerName = "N/A";
        List<Batch> allBatches = batchRepository.findAll();
        for (Batch batch : allBatches) {
            if (batch.getStudents().stream().anyMatch(s -> s.getId().equals(student.getId()))) {
                batchName = batch.getName();
                trainerName = batch.getTeacher().getName();
                break;
            }
        }

        String studentIdStr = profileOpt.map(StudentProfile::getStudentId).orElse("LTX-2026-STU");
        int points = profileOpt.map(p -> p.getPoints() != null ? p.getPoints() : 0).orElse(0);
        int rank = profileOpt.map(p -> p.getRankVal() != null ? p.getRankVal() : 0).orElse(0);
        String phone = profileOpt.map(StudentProfile::getPhone).orElse("");
        String location = profileOpt.map(StudentProfile::getLocation).orElse("");
        String college = profileOpt.map(StudentProfile::getCollege).orElse("");
        int graduationYear = profileOpt.map(p -> p.getGraduationYear() != null ? p.getGraduationYear() : 0).orElse(0);

        return AdminStudentRowResponse.builder()
                .id(student.getId())
                .studentId(studentIdStr)
                .name(student.getName())
                .email(student.getEmail())
                .phone(phone != null && !phone.isEmpty() ? phone : "+91 00000 00000")
                .courseTitle(courseTitle)
                .batchName(batchName)
                .trainerName(trainerName)
                .status(student.getStatus() != null ? student.getStatus().toLowerCase() : "pending")
                .statusReason(null)
                .attendancePercent(0.0)
                .progressPercent(0.0)
                .quizScore(0.0)
                .codingScore(0.0)
                .assignmentCompletion(0.0)
                .profileCompletion(profileOpt.map(p -> (double) p.getProfileCompletionPercent()).orElse(0.0))
                .points(points)
                .rank(rank)
                .careerStatus("eligible")
                .paymentStatus("pending")
                .placementStatus("not_started")
                .location(location != null ? location : "")
                .college(college != null ? college : "")
                .graduationYear(graduationYear)
                .enrolledAt(student.getCreatedAt() != null ? student.getCreatedAt().toString() : "")
                .build();
    }
}
