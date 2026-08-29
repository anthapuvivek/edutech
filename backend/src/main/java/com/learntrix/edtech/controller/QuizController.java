package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.quiz.*;
import com.learntrix.edtech.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
}
