package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.quiz.*;
import com.learntrix.edtech.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    // --- Teacher Endpoints ---

    @PostMapping("/teacher/quizzes")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<QuizResponse> createQuiz(
            @Valid @RequestBody CreateQuizRequest request) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        QuizResponse response = quizService.createQuiz(request, teacherId);
        return ApiResponse.success(response);
    }

    @GetMapping("/teacher/quizzes")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<QuizResponse>> getTeacherQuizzes() {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        List<QuizResponse> list = quizService.getTeacherQuizzes(teacherId);
        return ApiResponse.success(list);
    }

    @GetMapping("/teacher/quizzes/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<QuizResponse> getTeacherQuizById(@PathVariable UUID id) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        QuizResponse response = quizService.getTeacherQuizById(id, teacherId);
        return ApiResponse.success(response);
    }

    // --- Teacher: questions, options, publication, performance ---
    // All authorization happens in QuizService (course + batch ownership via
    // CourseAccessService). The teacher id always comes from the token, never the body.

    @GetMapping("/teacher/quizzes/{id}/questions")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<TeacherQuizQuestionResponse>> getQuestions(@PathVariable UUID id) {
        return ApiResponse.success(
                quizService.getQuestionsForTeacher(id, SecurityUtil.getCurrentUserId()));
    }

    @PostMapping("/teacher/quizzes/{id}/questions")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<TeacherQuizQuestionResponse> addQuestion(
            @PathVariable UUID id,
            @Valid @RequestBody QuizQuestionRequest request) {
        return ApiResponse.success(
                quizService.addQuestion(id, request, SecurityUtil.getCurrentUserId()));
    }

    @PutMapping("/teacher/quizzes/{id}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<TeacherQuizQuestionResponse> updateQuestion(
            @PathVariable UUID id,
            @PathVariable UUID questionId,
            @Valid @RequestBody QuizQuestionRequest request) {
        return ApiResponse.success(
                quizService.updateQuestion(id, questionId, request, SecurityUtil.getCurrentUserId()));
    }

    @DeleteMapping("/teacher/quizzes/{id}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Map<String, Object>> deleteQuestion(
            @PathVariable UUID id, @PathVariable UUID questionId) {
        quizService.deleteQuestion(id, questionId, SecurityUtil.getCurrentUserId());
        return ApiResponse.success(Map.of("ok", true, "id", questionId.toString()));
    }

    @PostMapping("/teacher/quizzes/{id}/publish")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<QuizResponse> publishQuiz(@PathVariable UUID id) {
        return ApiResponse.success(
                quizService.setPublished(id, true, SecurityUtil.getCurrentUserId()));
    }

    @PostMapping("/teacher/quizzes/{id}/unpublish")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<QuizResponse> unpublishQuiz(@PathVariable UUID id) {
        return ApiResponse.success(
                quizService.setPublished(id, false, SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/teacher/quizzes/{id}/performance")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<TeacherQuizAttemptResponse>> getQuizPerformance(@PathVariable UUID id) {
        return ApiResponse.success(
                quizService.getQuizPerformance(id, SecurityUtil.getCurrentUserId()));
    }

    // --- Student Endpoints ---

    @GetMapping("/student/quizzes")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<QuizResponse>> getStudentQuizzes() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        List<QuizResponse> list = quizService.getStudentQuizzes(studentId);
        return ApiResponse.success(list);
    }

    @GetMapping("/student/quizzes/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<QuizResponse> getStudentQuizById(@PathVariable UUID id) {
        UUID studentId = SecurityUtil.getCurrentUserId();
        QuizResponse response = quizService.getStudentQuizById(id, studentId);
        return ApiResponse.success(response);
    }

    @PostMapping("/student/quizzes/{id}/attempt")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<QuizAttemptResponse> attemptQuiz(
            @PathVariable UUID id,
            @Valid @RequestBody QuizAttemptRequest request) {
        UUID studentId = SecurityUtil.getCurrentUserId();
        QuizAttemptResponse response = quizService.attemptQuiz(id, studentId, request);
        return ApiResponse.success(response);
    }

    @GetMapping("/student/quizzes/{id}/results")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<QuizAttemptResponse> getStudentQuizResults(@PathVariable UUID id) {
        UUID studentId = SecurityUtil.getCurrentUserId();
        QuizAttemptResponse response = quizService.getStudentQuizResults(id, studentId);
        return ApiResponse.success(response);
    }

    /**
     * The quiz to attempt: questions and options with no answer key. Access is checked
     * server-side, so changing the id in the URL cannot reach another cohort's quiz.
     */
    @GetMapping("/student/quizzes/{id}/questions")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<StudentQuizDetailResponse> getStudentQuizQuestions(@PathVariable UUID id) {
        return ApiResponse.success(
                quizService.getStudentQuizDetail(id, SecurityUtil.getCurrentUserId()));
    }

    @PostMapping("/student/quizzes/{id}/start")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<QuizAttemptResultResponse> startAttempt(@PathVariable UUID id) {
        return ApiResponse.success(
                quizService.startAttempt(id, SecurityUtil.getCurrentUserId()));
    }

    /**
     * Submits answers. The body carries only questionId/selectedOptionId pairs - the score
     * is computed from the server-side answer key and nothing sent here can change it.
     */
    @PostMapping("/student/quizzes/{id}/submit")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<QuizAttemptResultResponse> submitAttempt(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitQuizAttemptRequest request) {
        return ApiResponse.success(
                quizService.submitAttempt(id, SecurityUtil.getCurrentUserId(), request));
    }

    /** The caller's own attempts only - scoped by the token, not by any request parameter. */
    @GetMapping("/student/quizzes/attempts/me")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<QuizAttemptResultResponse>> getMyAttempts() {
        return ApiResponse.success(quizService.getMyAttempts(SecurityUtil.getCurrentUserId()));
    }
}
