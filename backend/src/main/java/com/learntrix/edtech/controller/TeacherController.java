package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.teacher.*;
import com.learntrix.edtech.entity.*;
import com.learntrix.edtech.entity.Module;
import com.learntrix.edtech.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherController {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final RecordingWatchProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;

    public TeacherController(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            ModuleRepository moduleRepository,
            LessonRepository lessonRepository,
            RecordingWatchProgressRepository progressRepository,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    @GetMapping("/stats")
    public ApiResponse<TeacherStatsResponse> getStats() {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        List<Course> courses = courseRepository.findByInstructorId(teacherId);
        
        if (courses.isEmpty()) {
            return ApiResponse.success(TeacherStatsResponse.builder()
                    .totalStudents(0)
                    .activeStudents(0)
                    .courses(0)
                    .averageCompletion(0.0)
                    .averageQuizScore(0.0)
                    .problemsSolved(0)
                    .engagement(0.0)
                    .upcomingClasses(0)
                    .build());
        }

        List<UUID> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());
        List<Enrollment> enrollments = enrollmentRepository.findByCourseIdIn(courseIds);

        int totalStudents = enrollments.size();
        int activeStudents = (int) enrollments.stream()
                .filter(e -> "active".equalsIgnoreCase(e.getStatus()))
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
                .courses(courses.size())
                .averageCompletion(avgCompletion)
                .averageQuizScore(84.5) // Stub average
                .problemsSolved(148)    // Stub solved
                .engagement(92.4)       // Stub engagement
                .upcomingClasses(2)     // Stub classes
                .build());
    }

    @GetMapping("/students")
    public ApiResponse<List<TeacherStudentResponse>> getStudents(
            @RequestParam(value = "course", required = false) String courseTitle) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        List<Course> courses = courseRepository.findByInstructorId(teacherId);

        if (courses.isEmpty()) {
            return ApiResponse.success(List.of());
        }

        List<UUID> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());
        List<Enrollment> enrollments = enrollmentRepository.findByCourseIdIn(courseIds);

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
    public ApiResponse<TeacherStudentResponse> getStudent(@PathVariable("id") UUID studentId) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        List<Course> courses = courseRepository.findByInstructorId(teacherId);
        List<UUID> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());
        
        List<Enrollment> enrollments = enrollmentRepository.findByCourseIdIn(courseIds).stream()
                .filter(e -> e.getStudent().getId().equals(studentId))
                .collect(Collectors.toList());

        if (enrollments.isEmpty()) {
            return ApiResponse.success(null);
        }

        return ApiResponse.success(mapToStudentResponse(enrollments.get(0)));
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
