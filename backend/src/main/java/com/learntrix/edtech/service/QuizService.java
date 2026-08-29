package com.learntrix.edtech.service;

import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.dto.quiz.*;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.Quiz;
import com.learntrix.edtech.entity.QuizAttempt;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.QuizAttemptRepository;
import com.learntrix.edtech.repository.QuizRepository;
import com.learntrix.edtech.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseAccessService courseAccessService;

    public QuizService(
            QuizRepository quizRepository,
            QuizAttemptRepository quizAttemptRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            CourseAccessService courseAccessService) {
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.courseAccessService = courseAccessService;
    }

    public QuizResponse createQuiz(CreateQuizRequest request, UUID teacherId) {
        courseAccessService.verifyTeacherCanManageCourse(teacherId, request.getCourseId());

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));

        Quiz quiz = new Quiz();
        quiz.setCourse(course);
        quiz.setTeacher(teacher);
        quiz.setTitle(request.getTitle());
        quiz.setDescription(request.getDescription());
        quiz.setTimeLimitMinutes(request.getTimeLimitMinutes() != null ? request.getTimeLimitMinutes() : 30);
        quiz.setPassingScore(request.getPassingScore() != null ? request.getPassingScore() : 60);
        quiz.setStatus("PUBLISHED");
        quiz.setCreatedAt(Instant.now());
        quiz.setUpdatedAt(Instant.now());

        Quiz saved = quizRepository.save(quiz);
        return mapToResponse(saved, null);
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> getTeacherQuizzes(UUID teacherId) {
        List<UUID> courseIds = courseAccessService.getAuthorizedCourseIdsForTeacher(teacherId);
        if (courseIds.isEmpty()) return List.of();

        return quizRepository.findByCourseIdIn(courseIds).stream()
                .map(q -> mapToResponse(q, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuizResponse getTeacherQuizById(UUID quizId, UUID teacherId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));

        courseAccessService.verifyTeacherCanManageCourse(teacherId, quiz.getCourse().getId());
        return mapToResponse(quiz, null);
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> getStudentQuizzes(UUID studentId) {
        List<UUID> courseIds = courseAccessService.getAuthorizedCourseIdsForStudent(studentId);
        if (courseIds.isEmpty()) return List.of();

        List<Quiz> quizzes = quizRepository.findByCourseIdIn(courseIds);
        return quizzes.stream()
                .map(q -> {
                    Optional<QuizAttempt> attempt = quizAttemptRepository.findByQuizIdAndStudentId(q.getId(), studentId);
                    return mapToResponse(q, attempt.orElse(null));
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuizResponse getStudentQuizById(UUID quizId, UUID studentId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));

        courseAccessService.verifyStudentCanAccessCourse(studentId, quiz.getCourse().getId());

        Optional<QuizAttempt> attempt = quizAttemptRepository.findByQuizIdAndStudentId(quizId, studentId);
        return mapToResponse(quiz, attempt.orElse(null));
    }

    public QuizAttemptResponse attemptQuiz(UUID quizId, UUID studentId, QuizAttemptRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));

        courseAccessService.verifyStudentCanAccessCourse(studentId, quiz.getCourse().getId());

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        int score = request.getScore() != null ? request.getScore() : 100;
        boolean passed = score >= (quiz.getPassingScore() != null ? quiz.getPassingScore() : 60);

        QuizAttempt attempt = quizAttemptRepository.findByQuizIdAndStudentId(quizId, studentId)
                .orElseGet(() -> {
                    QuizAttempt a = new QuizAttempt();
                    a.setQuiz(quiz);
                    a.setStudent(student);
                    a.setStartedAt(Instant.now().minusSeconds(600));
                    return a;
                });

        attempt.setScore(score);
        attempt.setPassed(passed);
        attempt.setSubmittedAt(Instant.now());
        attempt.setStatus("COMPLETED");

        QuizAttempt saved = quizAttemptRepository.save(attempt);
        return mapToAttemptResponse(saved);
    }

    @Transactional(readOnly = true)
    public QuizAttemptResponse getStudentQuizResults(UUID quizId, UUID studentId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));

        courseAccessService.verifyStudentCanAccessCourse(studentId, quiz.getCourse().getId());

        QuizAttempt attempt = quizAttemptRepository.findByQuizIdAndStudentId(quizId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("QuizAttempt", "quizId", quizId));

        return mapToAttemptResponse(attempt);
    }

    private QuizResponse mapToResponse(Quiz quiz, QuizAttempt attempt) {
        String teacherName = quiz.getTeacher() != null
                ? quiz.getTeacher().getName()
                : (quiz.getCourse().getInstructor() != null ? quiz.getCourse().getInstructor().getName() : "Instructor");

        return QuizResponse.builder()
                .id(quiz.getId())
                .courseId(quiz.getCourse().getId())
                .courseTitle(quiz.getCourse().getTitle())
                .teacherId(quiz.getTeacher() != null ? quiz.getTeacher().getId() : null)
                .teacherName(teacherName)
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .passingScore(quiz.getPassingScore())
                .status(quiz.getStatus())
                .createdAt(quiz.getCreatedAt())
                .attempted(attempt != null)
                .score(attempt != null ? attempt.getScore() : null)
                .attemptStatus(attempt != null ? (Boolean.TRUE.equals(attempt.getPassed()) ? "PASSED" : "FAILED") : "NOT_ATTEMPTED")
                .build();
    }

    private QuizAttemptResponse mapToAttemptResponse(QuizAttempt a) {
        return QuizAttemptResponse.builder()
                .id(a.getId())
                .quizId(a.getQuiz().getId())
                .quizTitle(a.getQuiz().getTitle())
                .studentId(a.getStudent().getId())
                .studentName(a.getStudent().getName())
                .score(a.getScore())
                .passingScore(a.getQuiz().getPassingScore())
                .passed(Boolean.TRUE.equals(a.getPassed()))
                .status(a.getStatus())
                .startedAt(a.getStartedAt())
                .submittedAt(a.getSubmittedAt())
                .build();
    }
}
