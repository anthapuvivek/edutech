package com.learntrix.edtech.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final com.learntrix.edtech.repository.LiveClassRepository liveClassRepository;
    private final com.learntrix.edtech.repository.QuizAttemptRepository quizAttemptRepository;
    private final com.learntrix.edtech.repository.AssignmentSubmissionRepository assignmentSubmissionRepository;
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
            com.learntrix.edtech.repository.LiveClassRepository liveClassRepository,
            com.learntrix.edtech.repository.QuizAttemptRepository quizAttemptRepository,
            com.learntrix.edtech.repository.AssignmentSubmissionRepository assignmentSubmissionRepository,
            com.learntrix.edtech.service.CourseAccessService courseAccessService) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.batchRepository = batchRepository;
        this.liveClassRepository = liveClassRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
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

        // Real figures across this teacher's own students. Previously these were fixed
        // literals (84.5 / 148 / 92.4 / 2) that never moved regardless of the data.
        List<UUID> studentIds = enrollments.stream()
                .map(e -> e.getStudent().getId())
                .distinct()
                .collect(Collectors.toList());

        double averageQuizScore = studentIds.stream()
                .flatMap(sid -> quizAttemptRepository.findByStudentId(sid).stream())
                .filter(a -> a.getScore() != null)
                .mapToInt(com.learntrix.edtech.entity.QuizAttempt::getScore)
                .average()
                .orElse(0d);

        int submissionsGraded = (int) studentIds.stream()
                .flatMap(sid -> assignmentSubmissionRepository.findByStudentId(sid).stream())
                .count();

        // Engagement: share of this teacher's students who have done anything at all.
        long engagedStudents = studentIds.stream()
                .filter(sid -> !quizAttemptRepository.findByStudentId(sid).isEmpty()
                        || !assignmentSubmissionRepository.findByStudentId(sid).isEmpty()
                        || progressRepository.findByStudentId(sid).stream()
                                .anyMatch(RecordingWatchProgress::isCompleted))
                .count();
        double engagement = studentIds.isEmpty() ? 0d : (engagedStudents * 100.0) / studentIds.size();

        java.time.LocalDate today = java.time.LocalDate.now();
        int upcomingClasses = (int) liveClassRepository.findByTeacherOrBatchTeacher(teacherId).stream()
                .filter(lc -> lc.getClassDate() != null && !lc.getClassDate().isBefore(today))
                .filter(lc -> !"Cancelled".equalsIgnoreCase(String.valueOf(lc.getStatus())))
                .count();

        return ApiResponse.success(TeacherStatsResponse.builder()
                .totalStudents(totalStudents)
                .activeStudents(activeStudents)
                .courses(courseIds.size())
                .averageCompletion(avgCompletion)
                .averageQuizScore(averageQuizScore)
                .problemsSolved(submissionsGraded)
                .engagement(engagement)
                .upcomingClasses(upcomingClasses)
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

        // Real performance, read from the records the student actually produced. These used
        // to be fixed literals (quizScore 85, assignments "3/4", problemsSolved 24), which
        // made every student look identical and hid the fact that nothing had been attempted.
        List<com.learntrix.edtech.entity.QuizAttempt> attempts =
                quizAttemptRepository.findByStudentId(studentId);
        int quizScore = (int) Math.round(attempts.stream()
                .filter(a -> a.getScore() != null)
                .mapToInt(com.learntrix.edtech.entity.QuizAttempt::getScore)
                .average()
                .orElse(0d));

        List<com.learntrix.edtech.entity.AssignmentSubmission> submissions =
                assignmentSubmissionRepository.findByStudentId(studentId);
        long graded = submissions.stream().filter(sub -> sub.getGrade() != null).count();
        String assignments = submissions.isEmpty()
                ? "0/0"
                : graded + "/" + submissions.size();

        // Lessons the student has actually finished watching, for this course only.
        List<RecordingWatchProgress> watched = progressRepository.findByStudentId(studentId);
        UUID courseId = enrollment.getCourse().getId();
        int lessonsCompleted = (int) watched.stream()
                .filter(RecordingWatchProgress::isCompleted)
                .filter(wp -> wp.getRecording() != null
                        && wp.getRecording().getCourse() != null
                        && courseId.equals(wp.getRecording().getCourse().getId()))
                .count();

        // Last activity, derived from the most recent thing the student did.
        Instant lastActivity = Stream.of(
                        attempts.stream().map(com.learntrix.edtech.entity.QuizAttempt::getSubmittedAt),
                        submissions.stream().map(com.learntrix.edtech.entity.AssignmentSubmission::getSubmittedAt),
                        watched.stream().map(RecordingWatchProgress::getLastWatchedAt))
                .flatMap(s -> s)
                .filter(java.util.Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);

        Batch batch = enrollment.getBatch();

        return TeacherStudentResponse.builder()
                .id(studentId)
                .studentId(profileOpt.map(StudentProfile::getStudentId).orElse(null))
                .name(enrollment.getStudent().getName())
                .email(enrollment.getStudent().getEmail())
                .courseTitle(enrollment.getCourse().getTitle())
                .batchName(batch != null ? batch.getName() : null)
                .progressPercent(progress)
                .lessonsCompleted(lessonsCompleted)
                .quizzesAttempted(attempts.size())
                .quizScore(quizScore)
                .assignments(assignments)
                .problemsSolved(submissions.size())
                .points(points)
                .rank(rank)
                .lastActive(lastActivity != null ? lastActivity.toString() : null)
                .enrolledAt(enrollment.getCreatedAt() != null ? enrollment.getCreatedAt().toString() : null)
                .status(enrollment.getStatus() != null ? enrollment.getStatus() : "ACTIVE")
                .build();
    }
}
