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

    public TeacherController(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            ModuleRepository moduleRepository,
            LessonRepository lessonRepository,
            RecordingWatchProgressRepository progressRepository,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            BatchRepository batchRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.batchRepository = batchRepository;
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
        List<Batch> batches = batchRepository.findByTeacherIdOrderByStartDateAsc(teacherId);

        if (batches.isEmpty()) {
            return ApiResponse.success(List.of());
        }

        Set<UUID> teacherStudentIds = batches.stream()
                .flatMap(batch -> batch.getStudents().stream())
                .map(User::getId)
                .collect(Collectors.toSet());

        List<TeacherStudentResponse> students = batches.stream()
                .flatMap(batch -> batch.getStudents().stream())
                .distinct()
                .filter(student -> {
                    if (courseTitle != null && !courseTitle.trim().isEmpty() && !"All".equalsIgnoreCase(courseTitle)) {
                        return batchRepository.findByTeacherIdOrderByStartDateAsc(teacherId).stream()
                                .filter(b -> b.getCourse().getTitle().equalsIgnoreCase(courseTitle))
                                .flatMap(b -> b.getStudents().stream())
                                .anyMatch(s -> s.getId().equals(student.getId()));
                    }
                    return true;
                })
                .map(student -> {
                    Optional<Enrollment> enrollment = enrollmentRepository.findByStudentId(student.getId()).stream()
                            .filter(e -> e.getCourse() != null && batches.stream().anyMatch(b -> b.getCourse().getId().equals(e.getCourse().getId())))
                            .findFirst();
                    Enrollment e = enrollment.orElse(null);
                    if (e == null) {
                        return TeacherStudentResponse.builder()
                                .id(student.getId())
                                .name(student.getName())
                                .email(student.getEmail())
                                .courseTitle(batches.get(0).getCourse().getTitle())
                                .progressPercent(0)
                                .lessonsCompleted(0)
                                .quizScore(0)
                                .assignments("0/0")
                                .problemsSolved(0)
                                .points(0)
                                .rank(0)
                                .lastActive("never")
                                .status("active")
                                .build();
                    }
                    return mapToStudentResponse(e);
                })
                .collect(Collectors.toList());

        return ApiResponse.success(students);
    }

    @GetMapping("/students/{id}")
    @Transactional(readOnly = true)
    public ApiResponse<TeacherStudentResponse> getStudent(@PathVariable("id") UUID studentId) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        // Only return student if they belong to a batch assigned to this teacher
        List<Batch> batches = batchRepository.findByTeacherIdOrderByStartDateAsc(teacherId);
        boolean studentIsInTeacherBatch = batches.stream()
                .flatMap(b -> b.getStudents().stream())
                .anyMatch(s -> s.getId().equals(studentId));

        if (!studentIsInTeacherBatch) {
            return ApiResponse.success(null);
        }

        // Find matching enrollment for progress data
        List<UUID> courseIds = batches.stream().map(b -> b.getCourse().getId()).collect(Collectors.toList());
        Optional<Enrollment> enrollment = enrollmentRepository.findByCourseIdIn(courseIds).stream()
                .filter(e -> e.getStudent().getId().equals(studentId))
                .findFirst();

        if (enrollment.isEmpty()) {
            // Student is in the batch but not enrolled — return basic info
            User student = userRepository.findById(studentId).orElse(null);
            if (student == null) return ApiResponse.success(null);
            return ApiResponse.success(TeacherStudentResponse.builder()
                    .id(student.getId())
                    .name(student.getName())
                    .email(student.getEmail())
                    .courseTitle(batches.get(0).getCourse().getTitle())
                    .progressPercent(0).lessonsCompleted(0).quizScore(0)
                    .assignments("0/0").problemsSolved(0).points(0).rank(0)
                    .lastActive("never").status("active").build());
        }

        return ApiResponse.success(mapToStudentResponse(enrollment.get()));
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
