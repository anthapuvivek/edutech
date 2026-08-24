package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.dto.admin.*;
import com.learntrix.edtech.dto.live.LiveClassResponse;
import com.learntrix.edtech.entity.*;
import com.learntrix.edtech.repository.*;
import com.learntrix.edtech.service.LiveClassService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final LiveClassService liveClassService;

    public AdminController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            StudentProfileRepository studentProfileRepository,
            LiveClassService liveClassService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.liveClassService = liveClassService;
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

        return ApiResponse.success(AdminStatsResponse.builder()
                .students(studentCount)
                .teachers(teacherCount)
                .courses(courseCount)
                .batches(4)
                .liveEvents(3)
                .openEnquiries(2)
                .revenueThisMonth(149000.0)
                .activeBatches(3)
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
                .activeBatches(4)
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

        // Build mock/stub detailed profiles for detail views
        AdminStudentDetailResponse.Links links = AdminStudentDetailResponse.Links.builder()
                .github("https://github.com/learntrix-demo")
                .linkedin("https://linkedin.com/in/learntrix-demo")
                .leetcode("https://leetcode.com/learntrix")
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
                // Details
                .dateOfBirth("2003-05-15")
                .gender("Male")
                .qualification("B.Tech Computer Science")
                .skills(List.of("Java", "Spring Boot", "SQL", "Git"))
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

    @PostMapping("/students")
    public ApiResponse<Map<String, Object>> createStudent(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(Map.of("ok", true));
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

    private AdminStudentRowResponse mapToStudentRow(User student) {
        Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserId(student.getId());
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());

        String courseTitle = "Unenrolled";
        if (!enrollments.isEmpty()) {
            courseTitle = enrollments.get(0).getCourse().getTitle();
        }

        String studentIdStr = profileOpt.map(StudentProfile::getStudentId).orElse("LTX-2026-STU");
        int points = profileOpt.map(p -> p.getPoints() != null ? p.getPoints() : 0).orElse(150);
        int rank = profileOpt.map(p -> p.getRankVal() != null ? p.getRankVal() : 1).orElse(12);

        return AdminStudentRowResponse.builder()
                .id(student.getId())
                .studentId(studentIdStr)
                .name(student.getName())
                .email(student.getEmail())
                .phone("+91 98765 43210")
                .courseTitle(courseTitle)
                .batchName("Batch 2026-A")
                .trainerName("Durga Prasad")
                .status(student.getStatus().toLowerCase())
                .statusReason(null)
                .attendancePercent(92.5)
                .progressPercent(75.0)
                .quizScore(84.0)
                .codingScore(78.0)
                .assignmentCompletion(80.0)
                .profileCompletion(95.0)
                .points(points)
                .rank(rank)
                .careerStatus("eligible")
                .paymentStatus("paid")
                .placementStatus("job_seeking")
                .location("Hyderabad")
                .college("JNTU Engineering College")
                .graduationYear(2026)
                .enrolledAt(student.getCreatedAt().toString())
                .build();
    }
}
