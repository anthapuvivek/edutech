package com.learntrix.edtech.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.dto.admin.AdminOverviewResponse;
import com.learntrix.edtech.dto.admin.AdminSetupStepResponse;
import com.learntrix.edtech.dto.admin.AdminStatsResponse;
import com.learntrix.edtech.dto.admin.AdminStudentDetailResponse;
import com.learntrix.edtech.dto.admin.AdminStudentRowResponse;
import com.learntrix.edtech.dto.course.CourseResponse;
import com.learntrix.edtech.dto.course.CreateCourseRequest;
import com.learntrix.edtech.dto.course.UpdateCourseRequest;
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
import com.learntrix.edtech.service.CourseService;
import com.learntrix.edtech.service.LiveClassService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final com.learntrix.edtech.repository.TeacherProfileRepository teacherProfileRepository;
    private final BatchRepository batchRepository;
    private final com.learntrix.edtech.repository.LiveClassRepository liveClassRepository;
    private final LiveClassService liveClassService;
    private final com.learntrix.edtech.service.AdminOnboardingService adminOnboardingService;
    private final CourseService courseService;
    private final PasswordEncoder passwordEncoder;
    private final com.learntrix.edtech.service.EmailService emailService;

    public AdminController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            StudentProfileRepository studentProfileRepository,
            com.learntrix.edtech.repository.TeacherProfileRepository teacherProfileRepository,
            BatchRepository batchRepository,
            com.learntrix.edtech.repository.LiveClassRepository liveClassRepository,
            LiveClassService liveClassService,
            com.learntrix.edtech.service.AdminOnboardingService adminOnboardingService,
            CourseService courseService,
            PasswordEncoder passwordEncoder,
            com.learntrix.edtech.service.EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.batchRepository = batchRepository;
        this.liveClassRepository = liveClassRepository;
        this.liveClassService = liveClassService;
        this.adminOnboardingService = adminOnboardingService;
        this.courseService = courseService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Reports whether outbound SMTP is actually configured, so an admin can tell a real
     * delivery failure apart from a backend that was never given mail credentials.
     */
    @GetMapping("/mail/status")
    public ApiResponse<Map<String, Object>> mailStatus() {
        return ApiResponse.success(emailService.describeConfiguration());
    }

    /**
     * Sends a diagnostic message to prove SMTP works end to end before onboarding anyone.
     */
    @PostMapping("/mail/test")
    public ApiResponse<Map<String, Object>> sendTestMail(@RequestBody Map<String, String> body) {
        String to = body.get("to");
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("A 'to' address is required");
        }
        com.learntrix.edtech.service.MailDispatchResult result = emailService.sendTestEmail(to.trim());
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("to", to.trim());
        payload.put("status", result.getStatusName());
        payload.put("sent", result.isSent());
        if (result.getDetail() != null) {
            payload.put("error", result.getDetail());
        }
        return ApiResponse.success(payload);
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

    // Read-only transaction required: mapToStudentRow walks Enrollment.course, which is a
    // LAZY association, and open-in-view is disabled. Without this the whole endpoint 500s
    // with LazyInitializationException.
    @Transactional(readOnly = true)
    @GetMapping("/students")
    public ApiResponse<List<AdminStudentRowResponse>> getStudents() {
        List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "STUDENT".equalsIgnoreCase(r.getName())))
                .collect(Collectors.toList());

        List<AdminStudentRowResponse> rows = students.stream().map(this::mapToStudentRow).collect(Collectors.toList());
        return ApiResponse.success(rows);
    }

    @Transactional(readOnly = true)
    @GetMapping("/students/{id}")
    public ApiResponse<AdminStudentDetailResponse> getStudent(@PathVariable("id") UUID id) {
        // An id that matches no user is a missing resource, not a successful empty read.
        // Answering 200 with a null payload made a deleted or mistyped student look like a
        // server-confirmed record, the same trap PATCH /students/{id}/status already avoids.
        User student = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));

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

        // Every enrolment this student actually holds. A student can be enrolled in several
        // courses at once (enrollments is UNIQUE on student_id + course_id, not student_id),
        // so this must be a real list. It previously returned one synthetic row with a fixed
        // id and invented dates, which made additional courses invisible to the admin.
        List<AdminStudentDetailResponse.EnrollmentDetail> enrollmentDetails =
                enrollmentRepository.findByStudentId(id).stream()
                        .map(e -> AdminStudentDetailResponse.EnrollmentDetail.builder()
                                .id(e.getId().toString())
                                .courseId(e.getCourse() != null ? e.getCourse().getId().toString() : null)
                                .courseTitle(e.getCourse() != null ? e.getCourse().getTitle() : null)
                                .batchName(e.getBatch() != null ? e.getBatch().getName() : null)
                                .trainerName(e.getTeacher() != null ? e.getTeacher().getName() : null)
                                .startDate(e.getBatch() != null && e.getBatch().getStartDate() != null
                                        ? e.getBatch().getStartDate().toString() : null)
                                .endDate(e.getBatch() != null && e.getBatch().getEndDate() != null
                                        ? e.getBatch().getEndDate().toString() : null)
                                .courseStatus(e.getStatus())
                                .paymentStatus(row.getPaymentStatus())
                                .careerEligible(true)
                                .build())
                        .collect(Collectors.toList());

        List<String> skills = profileOpt.map(StudentProfile::getSkills)
                .orElse(List.of("Java", "Spring Boot", "SQL", "Git"));

        AdminStudentDetailResponse.PaymentDetail pay = AdminStudentDetailResponse.PaymentDetail.builder()
                .id("pay-1")
                .description("Course Enrollment Fee")
                .amount(45000.0)
                .status(row.getPaymentStatus() != null ? row.getPaymentStatus() : "paid")
                .paidAt("2026-08-15T10:30:00Z")
                .build();

        AdminStudentDetailResponse.CertificateDetail cert = AdminStudentDetailResponse.CertificateDetail.builder()
                .id("cert-1")
                .title("Foundations of Programming")
                .issuedAt("2026-08-20")
                .build();

        AdminStudentDetailResponse.ActivityDetail act = AdminStudentDetailResponse.ActivityDetail.builder()
                .id("act-1")
                .label("Enrolled in " + (row.getCourseTitle() != null ? row.getCourseTitle() : "Full Stack Program"))
                .occurredAt("2026-08-15 09:00")
                .build();

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
                .enrollments(enrollmentDetails)
                .attendance(List.of())
                .assessments(List.of())
                .applications(List.of())
                .payments(List.of(pay))
                .certificates(List.of(cert))
                .activity(List.of(act))
                .build());
    }

    @PatchMapping("/students/{id}/status")
    public ApiResponse<Map<String, Object>> setStudentStatus(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("A 'status' value is required");
        }
        // Reporting ok:true for an id that does not exist made a failed update look like a
        // successful one in the admin UI, so an unknown id is a 404 rather than a silent no-op.
        User student = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        student.setStatus(status.trim().toUpperCase());
        userRepository.save(student);
        return ApiResponse.success(Map.of("ok", true));
    }

    /**
     * Admin student onboarding — creates User (PENDING), StudentProfile, secure activation token,
     * and dispatches the welcome activation email.
     */
    @PostMapping("/students")
    public ApiResponse<com.learntrix.edtech.dto.admin.OnboardingResponse> createStudent(
            @jakarta.validation.Valid @RequestBody com.learntrix.edtech.dto.admin.CreateStudentRequest request) {
        com.learntrix.edtech.dto.admin.OnboardingResponse response = adminOnboardingService.onboardStudent(request);
        return ApiResponse.success(response);
    }

    /**
     * Resends the welcome activation email for a student.
     */
    @PostMapping({"/students/{id}/resend-welcome-email", "/students/{id}/resend-invite"})
    public ApiResponse<com.learntrix.edtech.dto.admin.OnboardingResponse> resendStudentWelcomeEmail(
            @PathVariable("id") UUID id) {
        com.learntrix.edtech.dto.admin.OnboardingResponse response = adminOnboardingService.resendStudentWelcomeEmail(id);
        return ApiResponse.success(response);
    }

    /**
     * Secure ADMIN-only endpoint that permanently deletes a student account and all
     * of its associated records. Not recoverable.
     */
    @DeleteMapping("/students/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Map<String, Object>> deleteStudent(
            @PathVariable("id") UUID id,
            org.springframework.security.core.Authentication authentication) {
        Map<String, Object> result = adminOnboardingService.deleteStudentPermanently(id, authentication);
        return ApiResponse.success(result);
    }

    /**
     * Admin teacher onboarding — creates User (PENDING), TeacherProfile, secure activation token,
     * and dispatches the welcome activation email.
     */
    @PostMapping({"/teachers", "/trainers"})
    public ApiResponse<com.learntrix.edtech.dto.admin.OnboardingResponse> createTeacher(
            @jakarta.validation.Valid @RequestBody com.learntrix.edtech.dto.admin.CreateTeacherRequest request) {
        com.learntrix.edtech.dto.admin.OnboardingResponse response = adminOnboardingService.onboardTeacher(request);
        return ApiResponse.success(response);
    }

    /**
     * Resends the welcome activation email for a teacher.
     */
    @PostMapping({"/teachers/{id}/resend-welcome-email", "/trainers/{id}/resend-welcome-email", "/teachers/{id}/resend-invite", "/trainers/{id}/resend-invite"})
    public ApiResponse<com.learntrix.edtech.dto.admin.OnboardingResponse> resendTeacherWelcomeEmail(
            @PathVariable("id") UUID id) {
        com.learntrix.edtech.dto.admin.OnboardingResponse response = adminOnboardingService.resendTeacherWelcomeEmail(id);
        return ApiResponse.success(response);
    }

    /**
     * Secure ADMIN-only endpoint to delete/deactivate a teacher account.
     */
    @DeleteMapping({"/teachers/{id}", "/trainers/{id}"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Map<String, Object>> deleteTeacher(
            @PathVariable("id") UUID id,
            org.springframework.security.core.Authentication authentication) {
        Map<String, Object> result = adminOnboardingService.deleteOrDeactivateTeacher(id, authentication);
        return ApiResponse.success(result);
    }

    /**
     * Returns all TEACHER-role users as a trainer list with TeacherProfile metadata and statistics.
     */
    // open-in-view is off, so the lazy Batch.students / Batch.course associations read below
    // need an open session — without this the whole trainer directory 500s as soon as any
    // trainer owns a batch.
    @Transactional(readOnly = true)
    @GetMapping({"/trainers", "/teachers"})
    public ApiResponse<List<Map<String, Object>>> getTrainers() {
        List<User> teachers = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "TEACHER".equalsIgnoreCase(r.getName())))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = teachers.stream().map(t -> {
            Optional<com.learntrix.edtech.entity.TeacherProfile> profileOpt = teacherProfileRepository.findByUserId(t.getId());
            List<Batch> teacherBatches = batchRepository.findByTeacherIdOrderByStartDateAsc(t.getId());

            int totalStudents = teacherBatches.stream()
                    .mapToInt(b -> b.getStudents() != null ? b.getStudents().size() : 0)
                    .sum();

            List<String> taughtCourses = teacherBatches.stream()
                    .map(b -> b.getCourse() != null ? b.getCourse().getTitle() : "")
                    .filter(title -> !title.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());

            if (taughtCourses.isEmpty()) {
                taughtCourses = courseRepository.findAll().stream()
                        .filter(c -> c.getInstructor() != null && c.getInstructor().getId().equals(t.getId()))
                        .map(Course::getTitle)
                        .collect(Collectors.toList());
            }

            Map<String, Object> row = new HashMap<>();
            row.put("id", t.getId());
            row.put("employeeId", profileOpt.map(com.learntrix.edtech.entity.TeacherProfile::getEmployeeId).orElse("LTX-T-2026-0001"));
            row.put("name", t.getName());
            row.put("email", t.getEmail());
            row.put("phone", profileOpt.map(com.learntrix.edtech.entity.TeacherProfile::getPhone).orElse(""));
            row.put("headline", profileOpt.map(com.learntrix.edtech.entity.TeacherProfile::getHeadline).orElse("Trainer"));
            row.put("department", profileOpt.map(com.learntrix.edtech.entity.TeacherProfile::getDepartment).orElse("Academics"));
            row.put("approvalStatus", profileOpt.map(com.learntrix.edtech.entity.TeacherProfile::getApprovalStatus).orElse("approved"));
            row.put("skills", profileOpt.map(com.learntrix.edtech.entity.TeacherProfile::getSkills).orElse(List.of()));
            row.put("bio", profileOpt.map(com.learntrix.edtech.entity.TeacherProfile::getBio).orElse(""));
            row.put("rating", profileOpt.map(p -> p.getRating() != null ? p.getRating().doubleValue() : 5.0).orElse(5.0));
            row.put("experienceYears", profileOpt.map(com.learntrix.edtech.entity.TeacherProfile::getExperienceYears).orElse(0));
            row.put("courses", taughtCourses);
            row.put("totalClasses", 0);
            row.put("totalStudents", totalStudents);
            row.put("students", totalStudents);
            row.put("classesThisMonth", 0);
            row.put("batches", teacherBatches.size());
            row.put("activeBatches", teacherBatches.size());
            row.put("status", t.getStatus() != null ? t.getStatus().toLowerCase() : "pending");
            row.put("onboardingStatus", "ACTIVE".equalsIgnoreCase(t.getStatus()) ? "ACTIVE" : "INVITED");
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
        UUID courseId = requiredUuid(body.get("courseId"), "courseId");
        UUID teacherId = requiredUuid(body.get("teacherId"), "teacherId");
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

    /**
     * Reads a required UUID out of a raw JSON body. Going through String.valueOf turned a
     * missing field into the literal "null" and then into "Invalid UUID string: null",
     * which named neither the field nor the fact that it was simply absent.
     */
    private static UUID requiredUuid(Object raw, String field) {
        if (raw == null || String.valueOf(raw).isBlank()) {
            throw new IllegalArgumentException("'" + field + "' is required");
        }
        try {
            return UUID.fromString(String.valueOf(raw).trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("'" + field + "' is not a valid id");
        }
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
     * Deletes a batch without touching the people or the course it grouped.
     *
     * <p>A batch is a cohort, not an owner: students, their accounts and their course
     * enrolments all outlive it. So every reference is detached first and only the batch
     * row itself is removed:</p>
     * <ul>
     *   <li>batch_students - membership rows are dropped (the users are not)</li>
     *   <li>enrollments.batch_id - set to null, the enrolment and its course survive</li>
     *   <li>live_classes.batch_id - set to null, the class history survives</li>
     * </ul>
     *
     * <p>PostgreSQL already declares SET NULL / CASCADE for these, but the mappings carry
     * no {@code @OnDelete}, so a schema generated from the entities would restrict instead.
     * Detaching explicitly makes the outcome identical on either schema.</p>
     *
     * <p>Authorization comes from the class-level
     * {@code @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")} - a teacher or student
     * calling this is rejected by Spring Security before the method runs.</p>
     */
    @Transactional
    @DeleteMapping("/batches/{batchId}")
    public ApiResponse<Map<String, Object>> deleteBatch(@PathVariable("batchId") UUID batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId));

        String name = batch.getName();
        int studentCount = batch.getStudents() != null ? batch.getStudents().size() : 0;

        // 1. Drop membership rows only. The User entities are untouched.
        if (batch.getStudents() != null && !batch.getStudents().isEmpty()) {
            batch.getStudents().clear();
            batchRepository.save(batch);
        }

        // 2. Keep every enrolment; just forget which batch it belonged to. The student stays
        //    enrolled in the course, and their enrolments in *other* courses are never read
        //    here, so they cannot be affected.
        List<Enrollment> affected = enrollmentRepository.findByBatchId(batchId);
        for (Enrollment enrollment : affected) {
            enrollment.setBatch(null);
            enrollmentRepository.save(enrollment);
        }

        // 3. Scheduled classes keep their record but lose their cohort.
        //
        // Clearing batch_id alone would WIDEN visibility: the student query treats a class
        // with no batch as course-wide, so every student on the course - including cohorts
        // that were never in this batch - would suddenly gain access. Unpublishing the
        // detached classes keeps the history intact while holding visibility closed until
        // an admin deliberately reassigns and republishes them.
        List<com.learntrix.edtech.entity.LiveClass> classes = liveClassRepository.findByBatchId(batchId);
        int unpublished = 0;
        for (com.learntrix.edtech.entity.LiveClass liveClass : classes) {
            liveClass.setBatch(null);
            if (Boolean.TRUE.equals(liveClass.getPublished())) {
                liveClass.setPublished(false);
                unpublished++;
            }
            liveClassRepository.save(liveClass);
        }

        try {
            batchRepository.delete(batch);
            batchRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Something still references the batch that this method does not know about.
            // Report it as a conflict rather than a 500 so the admin sees a real reason.
            throw new com.learntrix.edtech.common.exception.ConflictException(
                    "BATCH_IN_USE",
                    "Batch '" + name + "' is still referenced by other records and could not be "
                            + "deleted. Detach those records first.");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("id", batchId.toString());
        result.put("name", name);
        result.put("studentsReleased", studentCount);
        result.put("enrollmentsDetached", affected.size());
        result.put("classesDetached", classes.size());
        result.put("classesUnpublished", unpublished);
        String message = studentCount > 0
                ? "Batch '" + name + "' deleted. " + studentCount + " student(s) kept their course "
                        + "enrolment and were released from the batch."
                : "Batch '" + name + "' deleted.";
        if (unpublished > 0) {
            message += " " + unpublished + " scheduled class(es) were unpublished so they do not "
                    + "become visible to the whole course.";
        }
        result.put("message", message);
        return ApiResponse.success(result);
    }

    /**
     * Students belonging to one batch, read from the batch_students relationship.
     *
     * <p>Each row is joined to that student's enrolment for the batch's course so the admin
     * sees the course, the assigned teacher, the enrolment status and when it started -
     * rather than a bare name list. Everything comes from the database; nothing is mocked.</p>
     */
    @Transactional(readOnly = true)
    @GetMapping("/batches/{batchId}/students")
    public ApiResponse<List<Map<String, Object>>> getBatchStudents(@PathVariable UUID batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId));

        UUID courseId = batch.getCourse() != null ? batch.getCourse().getId() : null;

        List<Map<String, Object>> rows = batch.getStudents().stream()
                .sorted(Comparator.comparing(u -> u.getName() == null ? "" : u.getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .map(student -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", student.getId());
                    row.put("name", student.getName());
                    row.put("email", student.getEmail());
                    row.put("accountStatus", student.getStatus());

                    studentProfileRepository.findByUserId(student.getId()).ifPresent(p -> {
                        row.put("studentId", p.getStudentId());
                        row.put("phone", p.getPhone());
                    });
                    row.putIfAbsent("studentId", null);
                    row.putIfAbsent("phone", null);

                    row.put("courseTitle", batch.getCourse() != null ? batch.getCourse().getTitle() : null);
                    row.put("teacherName", batch.getTeacher() != null ? batch.getTeacher().getName() : null);

                    // Enrolment for this batch's course carries status and start date.
                    if (courseId != null) {
                        enrollmentRepository.findByStudentIdAndCourseId(student.getId(), courseId)
                                .ifPresent(e -> {
                                    row.put("enrollmentStatus", e.getStatus());
                                    row.put("enrolledAt", e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
                                    if (e.getTeacher() != null) {
                                        row.put("teacherName", e.getTeacher().getName());
                                    }
                                });
                    }
                    row.putIfAbsent("enrollmentStatus", null);
                    row.putIfAbsent("enrolledAt", null);
                    return row;
                })
                .collect(Collectors.toList());

        return ApiResponse.success(rows);
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

    @PostMapping("/allocations")
    public ApiResponse<com.learntrix.edtech.dto.admin.StudentAllocationResponse> allocateStudent(
            @jakarta.validation.Valid @RequestBody com.learntrix.edtech.dto.admin.AllocateStudentRequest request) {
        return ApiResponse.success(adminOnboardingService.allocateStudent(request));
    }

    @GetMapping("/students/{id}/allocations")
    public ApiResponse<List<com.learntrix.edtech.dto.admin.StudentAllocationResponse>> getStudentAllocations(
            @PathVariable("id") UUID id) {
        return ApiResponse.success(adminOnboardingService.getStudentAllocations(id));
    }

    @PutMapping({"/allocations/{id}", "/students/{studentId}/allocations/{id}", "/students/{id}/reassign"})
    public ApiResponse<com.learntrix.edtech.dto.admin.StudentAllocationResponse> reassignAllocation(
            @PathVariable("id") UUID id,
            @RequestBody com.learntrix.edtech.dto.admin.AllocateStudentRequest request) {
        return ApiResponse.success(adminOnboardingService.reassignAllocation(id, request));
    }

    @DeleteMapping({"/allocations/{id}", "/students/{studentId}/allocations/{id}"})
    public ApiResponse<Map<String, Object>> removeAllocation(@PathVariable("id") UUID id) {
        adminOnboardingService.removeAllocation(id);
        return ApiResponse.success(Map.of("ok", true, "message", "Student allocation removed successfully"));
    }

    @PostMapping("/enrollments")
    public ApiResponse<Map<String, Object>> createEnrollment(@RequestBody Map<String, Object> body) {
        if (body.containsKey("studentId") && body.containsKey("courseId")) {
            UUID studentId = UUID.fromString(String.valueOf(body.get("studentId")));
            UUID courseId = UUID.fromString(String.valueOf(body.get("courseId")));
            UUID teacherId = body.get("teacherId") != null ? UUID.fromString(String.valueOf(body.get("teacherId"))) : null;
            UUID batchId = body.get("batchId") != null ? UUID.fromString(String.valueOf(body.get("batchId"))) : null;
            String status = body.getOrDefault("status", "ACTIVE").toString();

            com.learntrix.edtech.dto.admin.AllocateStudentRequest req = com.learntrix.edtech.dto.admin.AllocateStudentRequest.builder()
                    .studentId(studentId)
                    .courseId(courseId)
                    .teacherId(teacherId)
                    .batchId(batchId)
                    .status(status)
                    .build();
            adminOnboardingService.allocateStudent(req);
        }
        return ApiResponse.success(Map.of("ok", true));
    }

    @PatchMapping("/students/{id}/eligibility")
    public ApiResponse<Map<String, Object>> overrideEligibility(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> body) {
        return ApiResponse.success(Map.of("ok", true));
    }

    @GetMapping("/events")
    public ApiResponse<List<com.learntrix.edtech.dto.live.LiveClassResponse>> getEvents() {
        return ApiResponse.success(liveClassService.getTeacherLiveClasses(null));
    }

    @PatchMapping("/trainers/{id}/approval")
    public ApiResponse<Map<String, Object>> approveTrainer(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> body) {
        return ApiResponse.success(Map.of("ok", true));
    }

    @GetMapping("/attendance")
    public ApiResponse<List<Map<String, Object>>> getAttendance() {
        return ApiResponse.success(List.of());
    }

    @PatchMapping("/attendance")
    public ApiResponse<Map<String, Object>> correctAttendance(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(Map.of("ok", true));
    }

    @GetMapping("/articles")
    public ApiResponse<List<Map<String, Object>>> getArticles() {
        return ApiResponse.success(List.of());
    }

    @PostMapping("/articles")
    public ApiResponse<Map<String, Object>> saveArticle(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(Map.of("ok", true));
    }

    @GetMapping("/communication/whatsapp")
    public ApiResponse<Map<String, Object>> getWhatsappSettings() {
        return ApiResponse.success(Map.of("enabled", true, "phoneNumber", "+91 99999 88888"));
    }

    @PutMapping("/communication/whatsapp")
    public ApiResponse<Map<String, Object>> saveWhatsappSettings(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(Map.of("ok", true));
    }

    @PostMapping("/communication/notifications")
    public ApiResponse<Map<String, Object>> sendNotification(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(Map.of("ok", true));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<List<Map<String, Object>>> getAuditLogs() {
        return ApiResponse.success(List.of(
                Map.of("id", "log-1", "action", "USER_LOGIN", "userId", "system", "timestamp", LocalDate.now().toString(), "details", "System started successfully")
        ));
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private AdminStudentRowResponse mapToStudentRow(User student) {
        Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserId(student.getId());
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());

        String courseTitle = "Unenrolled";
        String trainerName = "Not Assigned";
        String batchName = "Not Assigned";

        if (!enrollments.isEmpty()) {
            Enrollment first = enrollments.get(0);
            courseTitle = first.getCourse().getTitle();
            if (first.getTeacher() != null) {
                trainerName = first.getTeacher().getName();
            } else if (first.getCourse().getInstructor() != null) {
                trainerName = first.getCourse().getInstructor().getName();
            }
            if (first.getBatch() != null) {
                batchName = first.getBatch().getName();
                if ("Not Assigned".equals(trainerName) && first.getBatch().getTeacher() != null) {
                    trainerName = first.getBatch().getTeacher().getName();
                }
            }
        }

        // Also check batchRepository if not assigned via enrollment
        if ("Not Assigned".equals(batchName)) {
            List<Batch> allBatches = batchRepository.findAll();
            for (Batch batch : allBatches) {
                if (batch.getStudents().stream().anyMatch(s -> s.getId().equals(student.getId()))) {
                    batchName = batch.getName();
                    if ("Not Assigned".equals(trainerName) && batch.getTeacher() != null) {
                        trainerName = batch.getTeacher().getName();
                    }
                    if ("Unenrolled".equals(courseTitle) && batch.getCourse() != null) {
                        courseTitle = batch.getCourse().getTitle();
                    }
                    break;
                }
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

    // =========================================================================
    // Course Management Endpoints
    // =========================================================================

    /**
     * Lists all courses with admin metadata (including DRAFT, PUBLISHED, ARCHIVED).
     */
    @GetMapping("/courses")
    public ApiResponse<List<CourseResponse>> getAdminCourses(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "sort", required = false) String sort) {
        List<CourseResponse> courses = courseService.listAdminCourses(search, category, status, sort);
        return ApiResponse.success(courses);
    }

    /**
     * Gets a single course by ID for admin inspection/editing.
     */
    @GetMapping("/courses/{id}")
    public ApiResponse<CourseResponse> getAdminCourseById(@PathVariable("id") UUID id) {
        CourseResponse course = courseService.getCourseById(id);
        return ApiResponse.success(course);
    }

    /**
     * Creates a new Course entity and persists it to PostgreSQL.
     */
    @PostMapping("/courses")
    public ApiResponse<CourseResponse> createCourse(
            @jakarta.validation.Valid @RequestBody CreateCourseRequest request) {
        CourseResponse course = courseService.createCourse(request);
        return ApiResponse.success(course);
    }

    /**
     * Updates an existing Course entity and persists changes to PostgreSQL.
     */
    @PutMapping("/courses/{id}")
    public ApiResponse<CourseResponse> updateCourse(
            @PathVariable("id") UUID id,
            @jakarta.validation.Valid @RequestBody UpdateCourseRequest request) {
        CourseResponse course = courseService.updateCourse(id, request);
        return ApiResponse.success(course);
    }

    /**
     * Publishes a course: sets status = PUBLISHED in PostgreSQL.
     */
    @RequestMapping(value = "/courses/{id}/publish", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ApiResponse<CourseResponse> publishCourse(@PathVariable("id") UUID id) {
        CourseResponse course = courseService.publishCourse(id);
        return ApiResponse.success(course);
    }

    /**
     * Unpublishes a course: sets status = DRAFT in PostgreSQL.
     */
    @RequestMapping(value = "/courses/{id}/unpublish", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ApiResponse<CourseResponse> unpublishCourse(@PathVariable("id") UUID id) {
        CourseResponse course = courseService.unpublishCourse(id);
        return ApiResponse.success(course);
    }

    /**
     * Updates course status (DRAFT, PUBLISHED, ARCHIVED).
     */
    @PatchMapping("/courses/{id}/status")
    public ApiResponse<CourseResponse> setCourseStatus(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        CourseResponse course = courseService.setCourseStatus(id, status);
        return ApiResponse.success(course);
    }

    /**
     * Deletes or archives a course in PostgreSQL.
     */
    @DeleteMapping("/courses/{id}")
    public ApiResponse<Map<String, Object>> deleteCourse(@PathVariable("id") UUID id) {
        courseService.deleteCourse(id);
        return ApiResponse.success(Map.of("ok", true, "message", "Course removed successfully"));
    }
}
