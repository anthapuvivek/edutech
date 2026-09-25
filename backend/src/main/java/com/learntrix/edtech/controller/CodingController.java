package com.learntrix.edtech.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.coding.*;
import com.learntrix.edtech.service.CodingProblemService;

import jakarta.validation.Valid;

/**
 * Coding practice endpoints.
 *
 * <p>{@code @PreAuthorize} only gates the role. Every ownership and enrolment check lives in
 * {@link CodingProblemService}, and the acting user id always comes from the token - never
 * from the request body or a path variable - so a caller cannot act as someone else.</p>
 */
@RestController
@RequestMapping("/api")
public class CodingController {

    private final CodingProblemService codingService;

    public CodingController(CodingProblemService codingService) {
        this.codingService = codingService;
    }

    // ==================================================================
    // Teacher
    // ==================================================================

    @PostMapping("/teacher/coding-problems")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<CodingProblemResponse> createProblem(
            @Valid @RequestBody CreateCodingProblemRequest request) {
        return ApiResponse.success(
                codingService.createProblem(request, SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/teacher/coding-problems")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<CodingProblemResponse>> getTeacherProblems() {
        return ApiResponse.success(
                codingService.getTeacherProblems(SecurityUtil.getCurrentUserId()));
    }

    /** Full test-case list, hidden cases included. Teacher-only by construction. */
    @GetMapping("/teacher/coding-problems/{id}/test-cases")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<TeacherCodingTestCaseResponse>> getTestCases(@PathVariable UUID id) {
        return ApiResponse.success(
                codingService.getTestCasesForTeacher(id, SecurityUtil.getCurrentUserId()));
    }

    @PostMapping("/teacher/coding-problems/{id}/test-cases")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<TeacherCodingTestCaseResponse> addTestCase(
            @PathVariable UUID id,
            @Valid @RequestBody CodingTestCaseRequest request) {
        return ApiResponse.success(
                codingService.addTestCase(id, request, SecurityUtil.getCurrentUserId()));
    }

    @PutMapping("/teacher/coding-problems/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<CodingProblemResponse> updateProblem(
            @PathVariable UUID id,
            @RequestBody CreateCodingProblemRequest request) {
        return ApiResponse.success(
                codingService.updateProblem(id, request, SecurityUtil.getCurrentUserId()));
    }

    @PutMapping("/teacher/coding-problems/{id}/test-cases/{testCaseId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<TeacherCodingTestCaseResponse> updateTestCase(
            @PathVariable UUID id,
            @PathVariable UUID testCaseId,
            @Valid @RequestBody CodingTestCaseRequest request) {
        return ApiResponse.success(
                codingService.updateTestCase(id, testCaseId, request, SecurityUtil.getCurrentUserId()));
    }

    /** Cohort-wide standing on one problem. Counts are of students, never of submissions. */
    @GetMapping("/teacher/coding-problems/{id}/performance")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<CodingProblemPerformanceResponse> problemPerformance(@PathVariable UUID id) {
        return ApiResponse.success(
                codingService.getProblemPerformance(id, SecurityUtil.getCurrentUserId()));
    }

    /** Submission history for one problem, restricted to the teacher who owns it. */
    @GetMapping("/teacher/coding-problems/{id}/submissions")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<CodingSubmissionResponse>> problemSubmissions(@PathVariable UUID id) {
        return ApiResponse.success(
                codingService.getProblemSubmissions(id, SecurityUtil.getCurrentUserId()));
    }

    @DeleteMapping("/teacher/coding-problems/{id}/test-cases/{testCaseId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Void> deleteTestCase(
            @PathVariable UUID id, @PathVariable UUID testCaseId) {
        codingService.deleteTestCase(id, testCaseId, SecurityUtil.getCurrentUserId());
        return ApiResponse.success(null);
    }

    @PutMapping("/teacher/coding-problems/{id}/publish")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<CodingProblemResponse> publish(@PathVariable UUID id) {
        return ApiResponse.success(
                codingService.setPublished(id, true, SecurityUtil.getCurrentUserId()));
    }

    @PutMapping("/teacher/coding-problems/{id}/unpublish")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<CodingProblemResponse> unpublish(@PathVariable UUID id) {
        return ApiResponse.success(
                codingService.setPublished(id, false, SecurityUtil.getCurrentUserId()));
    }

    /** A teacher may read this only for a student actually assigned to them. */
    @GetMapping("/teacher/students/{studentId}/coding-performance")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<CodingPerformanceResponse> studentPerformance(@PathVariable UUID studentId) {
        return ApiResponse.success(
                codingService.getPerformance(studentId, SecurityUtil.getCurrentUserId()));
    }

    // ==================================================================
    // Student
    // ==================================================================

    @GetMapping("/student/coding-problems")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<CodingProblemResponse>> getStudentProblems() {
        return ApiResponse.success(
                codingService.getStudentProblems(SecurityUtil.getCurrentUserId()));
    }

    /** Detail with SAMPLE test cases only. */
    @GetMapping("/student/coding-problems/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<CodingProblemResponse> getStudentProblem(@PathVariable UUID id) {
        return ApiResponse.success(
                codingService.getStudentProblem(id, SecurityUtil.getCurrentUserId()));
    }

    /** Run against sample cases. Nothing is stored and hidden cases are not executed. */
    @PostMapping("/student/coding-problems/{id}/run")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<RunCodeResponse> run(
            @PathVariable UUID id, @Valid @RequestBody RunCodeRequest request) {
        return ApiResponse.success(
                codingService.runCode(id, SecurityUtil.getCurrentUserId(), request));
    }

    /** Judge against every case and store the verdict. Counts only come back. */
    @PostMapping("/student/coding-problems/{id}/submit")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<CodingSubmissionResponse> submit(
            @PathVariable UUID id, @Valid @RequestBody RunCodeRequest request) {
        return ApiResponse.success(
                codingService.submit(id, SecurityUtil.getCurrentUserId(), request));
    }

    @GetMapping("/student/coding-submissions")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<CodingSubmissionResponse>> mySubmissions() {
        return ApiResponse.success(
                codingService.getMySubmissions(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/student/coding-performance")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<CodingPerformanceResponse> myPerformance() {
        UUID me = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(codingService.getPerformance(me, me));
    }
}
