package com.learntrix.edtech.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.coding.ExternalCodingProblemRequest;
import com.learntrix.edtech.dto.coding.ExternalCodingProblemResponse;
import com.learntrix.edtech.dto.coding.CodingProblemProgressResponse;
import com.learntrix.edtech.service.ExternalCodingProblemService;

import jakarta.validation.Valid;

/**
 * Coding Practice as assigned external problems.
 *
 * <p>Mounted under {@code /coding-assignments} rather than reusing
 * {@code /coding-problems}, so the older judge-based endpoints keep working for the
 * submissions already in the database instead of changing shape underneath them.</p>
 *
 * <p>As everywhere else: the annotation gates the role, the service settles ownership, and
 * the acting user id comes from the token - never the request body.</p>
 */
@RestController
@RequestMapping("/api")
public class ExternalCodingProblemController {

    private final ExternalCodingProblemService service;

    public ExternalCodingProblemController(ExternalCodingProblemService service) {
        this.service = service;
    }

    // ---------------- teacher ----------------

    @PostMapping("/teacher/coding-assignments")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<ExternalCodingProblemResponse> create(
            @Valid @RequestBody ExternalCodingProblemRequest request) {
        return ApiResponse.success(service.create(request, SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/teacher/coding-assignments")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<ExternalCodingProblemResponse>> teacherList() {
        return ApiResponse.success(service.teacherList(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/teacher/coding-assignments/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<ExternalCodingProblemResponse> teacherGet(@PathVariable UUID id) {
        return ApiResponse.success(service.teacherGet(id, SecurityUtil.getCurrentUserId()));
    }

    @PutMapping("/teacher/coding-assignments/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<ExternalCodingProblemResponse> update(
            @PathVariable UUID id, @Valid @RequestBody ExternalCodingProblemRequest request) {
        return ApiResponse.success(service.update(id, request, SecurityUtil.getCurrentUserId()));
    }

    @PutMapping("/teacher/coding-assignments/{id}/active")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<ExternalCodingProblemResponse> setActive(
            @PathVariable UUID id, @RequestBody Map<String, Boolean> body) {
        boolean active = Boolean.TRUE.equals(body.get("active"));
        return ApiResponse.success(service.setActive(id, active, SecurityUtil.getCurrentUserId()));
    }

    @DeleteMapping("/teacher/coding-assignments/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.archive(id, SecurityUtil.getCurrentUserId());
        return ApiResponse.success(null);
    }

    @GetMapping("/teacher/coding-assignments/{id}/progress")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<CodingProblemProgressResponse> progress(@PathVariable UUID id) {
        return ApiResponse.success(service.progress(id, SecurityUtil.getCurrentUserId()));
    }

    // ---------------- student ----------------

    @GetMapping("/student/coding-assignments")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<ExternalCodingProblemResponse>> studentList() {
        return ApiResponse.success(service.studentList(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/student/coding-assignments/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<ExternalCodingProblemResponse> studentGet(@PathVariable UUID id) {
        return ApiResponse.success(service.studentGet(id, SecurityUtil.getCurrentUserId()));
    }

    /**
     * Self-reported progress. LearnTriX cannot verify a solve on an external platform, and
     * the row is keyed on the authenticated student, so nobody can mark another's problem.
     */
    @PutMapping("/student/coding-assignments/{id}/completion")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<ExternalCodingProblemResponse> setCompleted(
            @PathVariable UUID id, @RequestBody Map<String, Boolean> body) {
        boolean completed = Boolean.TRUE.equals(body.get("completed"));
        return ApiResponse.success(
                service.setCompleted(id, completed, SecurityUtil.getCurrentUserId()));
    }

    @PostMapping("/student/coding-assignments/{id}/open")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<ExternalCodingProblemResponse> markOpened(@PathVariable UUID id) {
        return ApiResponse.success(service.markOpened(id, SecurityUtil.getCurrentUserId()));
    }

    @PostMapping("/student/coding-assignments/{id}/complete")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<ExternalCodingProblemResponse> complete(@PathVariable UUID id) {
        return ApiResponse.success(service.setCompleted(id, true, SecurityUtil.getCurrentUserId()));
    }
}
