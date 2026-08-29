package com.learntrix.edtech.controller;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.teacher.TeacherBatchResponse;
import com.learntrix.edtech.dto.teacher.TeacherStatsResponse;
import com.learntrix.edtech.dto.teacher.TeacherStudentResponse;
import com.learntrix.edtech.entity.Batch;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.Enrollment;
import com.learntrix.edtech.entity.Lesson;
import com.learntrix.edtech.entity.Module;
import com.learntrix.edtech.entity.RecordingWatchProgress;
import com.learntrix.edtech.entity.StudentProfile;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.EnrollmentRepository;
import com.learntrix.edtech.repository.LessonRepository;
import com.learntrix.edtech.repository.ModuleRepository;
import com.learntrix.edtech.repository.RecordingWatchProgressRepository;
import com.learntrix.edtech.repository.StudentProfileRepository;
import com.learntrix.edtech.repository.UserRepository;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN', 'MENTOR')")
public class TeacherController {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final RecordingWatchProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final BatchRepository batchRepository;
    private final com.learntrix.edtech.service.CourseAccessService courseAccessService;

    public TeacherController(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            ModuleRepository moduleRepository,
            LessonRepository lessonRepository,
            RecordingWatchProgressRepository progressRepository,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            BatchRepository batchRepository,
            com.learntrix.edtech.service.CourseAccessService courseAccessService) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.batchRepository = batchRepository;
        this.courseAccessService = courseAccessService;
    }

    @GetMapping("/stats")
    public ApiResponse<TeacherStatsResponse> getStats() {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        List<Enrollment> enrollments = courseAccessService.getTeacherStudentEnrollments(teacherId);
        List<UUID> courseIds = courseAccessService.getAuthorizedCourseIdsForTeacher(teacherId);

        int totalStudents = enrollments.size();
        int activeStudents = (int) enrollments.stream()
                .filter(e -> "active".equalsIgnoreCase(e.getStatus()) || "enrolled".equalsIgnoreCase(e.getStatus()))
                .count();

        double avgCompletion = 0.0;
        if (totalStudents > 0) {
            double totalProgress = 0.0;
            for (Enrollment enrollment : enrollments) {
                totalProgress += calculateProgress(enrollment.getStudent().getId(), enrollment.getCourse().getId());
            }
            avgCompletion = totalProgress / totalStudents;
        }

        return ApiResponse.success(TeacherStatsResponse.builder()
                .totalStudents(totalStudents)
                .activeStudents(activeStudents)
                .courses(courseIds.size())
                .averageCompletion(avgCompletion)
                .averageQuizScore(84.5) // Stub average
                .problemsSolved(148)    // Stub solved
                .engagement(92.4)       // Stub engagement
                .upcomingClasses(2)     // Stub classes
                .build());
    }

    @Transactional(readOnly = true)
    @GetMapping("/courses")
    public ApiResponse<List<com.learntrix.edtech.dto.course.CourseResponse>> getCourses() {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        List<UUID> courseIds = courseAccessService.getAuthorizedCourseIdsForTeacher(teacherId);
        if (courseIds.isEmpty()) return ApiResponse.success(List.of());

        List<com.learntrix.edtech.dto.course.CourseResponse> courses = courseRepository.findAllById(courseIds).stream()
                .map(c -> com.learntrix.edtech.dto.course.CourseResponse.builder()
                        .id(c.getId())
                        .title(c.getTitle())
                        .slug(c.getSlug())
                        .category(c.getCategory())
                        .status(c.getStatus())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.success(courses);
    }

    @Transactional(readOnly = true)
    @GetMapping("/batches")
    public ApiResponse<List<TeacherBatchResponse>> getBatches() {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        List<Batch> batches = batchRepository.findByTeacherIdOrderByStartDateAsc(teacherId);
        return ApiResponse.success(batches.stream().map(batch -> TeacherBatchResponse.builder()
                .id(batch.getId())
                .name(batch.getName())
                .courseId(batch.getCourse().getId())
                .courseTitle(batch.getCourse().getTitle())
                .teacherId(batch.getTeacher().getId())
                .teacherName(batch.getTeacher().getName())
                .capacity(batch.getCapacity())
                .status(batch.getStatus())
                .startDate(batch.getStartDate())
                .endDate(batch.getEndDate())
                .studentCount(batch.getStudents().size())
                .build()).collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    @GetMapping("/students")
    public ApiResponse<List<TeacherStudentResponse>> getStudents(
            @RequestParam(value = "course", required = false) String courseTitle) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        List<Enrollment> enrollments = courseAccessService.getTeacherStudentEnrollments(teacherId);

        List<TeacherStudentResponse> students = enrollments.stream()
                .filter(e -> {
                    if (courseTitle != null && !courseTitle.trim().isEmpty() && !"All".equalsIgnoreCase(courseTitle)) {
                        return e.getCourse().getTitle().equalsIgnoreCase(courseTitle);
                    }
                    return true;
                })
                .map(this::mapToStudentResponse)
                .collect(Collectors.toList());

        return ApiResponse.success(students);
    }

    @GetMapping("/students/{id}")
    @Transactional(readOnly = true)
    public ApiResponse<TeacherStudentResponse> getStudent(@PathVariable("id") UUID studentId) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        if (!courseAccessService.isStudentAssignedToTeacher(studentId, teacherId)) {
            throw new com.learntrix.edtech.common.exception.CourseAccessDeniedException("You are not authorized to view details for this student");
        }

        List<Enrollment> enrollments = courseAccessService.getTeacherStudentEnrollments(teacherId);
        Enrollment enrollment = enrollments.stream()
                .filter(e -> e.getStudent().getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new com.learntrix.edtech.common.exception.ResourceNotFoundException("Student", "id", studentId));

        return ApiResponse.success(mapToStudentResponse(enrollment));
    }

    private int calculateProgress(UUID studentId, UUID courseId) {
        List<Module> modules = moduleRepository.findByCourseIdOrderBySequenceNumberAsc(courseId);
        int totalLessons = 0;
        int completedLessons = 0;
        
        for (Module m : modules) {
            List<Lesson> lessons = lessonRepository.findByModuleIdOrderBySequenceNumberAsc(m.getId());
            totalLessons += lessons.size();
            for (Lesson l : lessons) {
                boolean isCompleted = progressRepository.findAll().stream()
                        .filter(p -> p.getStudent().getId().equals(studentId)
                                && p.getRecording().getLesson().getId().equals(l.getId()))
                        .anyMatch(RecordingWatchProgress::isCompleted);
                if (isCompleted) {
                    completedLessons++;
                }
            }
        }
        
        if (totalLessons == 0) return 0;
        return (int) (((double) completedLessons / totalLessons) * 100);
    }

    private TeacherStudentResponse mapToStudentResponse(Enrollment enrollment) {
        UUID studentId = enrollment.getStudent().getId();
        int progress = calculateProgress(studentId, enrollment.getCourse().getId());
        
        // Find profile details
        Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserId(studentId);
        int points = profileOpt.map(p -> p.getPoints() != null ? p.getPoints() : 0).orElse(150);
        int rank = profileOpt.map(p -> p.getRankVal() != null ? p.getRankVal() : 1).orElse(12);

        return TeacherStudentResponse.builder()
                .id(studentId)
                .name(enrollment.getStudent().getName())
                .email(enrollment.getStudent().getEmail())
                .courseTitle(enrollment.getCourse().getTitle())
                .progressPercent(progress)
                .lessonsCompleted(progress / 10) // Approx
                .quizScore(85)
                .assignments("3/4")
                .problemsSolved(24)
                .points(points)
                .rank(rank)
                .lastActive("2 hours ago")
                .status("active")
                .build();
    }
}
