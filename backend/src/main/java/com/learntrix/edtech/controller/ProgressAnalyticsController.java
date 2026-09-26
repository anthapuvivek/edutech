package com.learntrix.edtech.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.progress.AnalyticsResponse;
import com.learntrix.edtech.dto.progress.BatchProgressResponse;
import com.learntrix.edtech.dto.progress.StudentProgressResponse;
import com.learntrix.edtech.service.LearningAnalyticsService;
import com.learntrix.edtech.service.LearningProgressService;

/**
 * Progress and analytics.
 *
 * <p>Students read only their own figures - there is no student-id parameter on the
 * student endpoints, so there is nothing to tamper with. A teacher reading one student
 * passes an id, and the service checks that student is actually on their roster.</p>
 */
@RestController
@RequestMapping("/api")
public class ProgressAnalyticsController {

    private final LearningProgressService progressService;
    private final LearningAnalyticsService analyticsService;

    public ProgressAnalyticsController(
            LearningProgressService progressService,
            LearningAnalyticsService analyticsService) {
        this.progressService = progressService;
        this.analyticsService = analyticsService;
    }

    // ---------------- student ----------------

    @GetMapping("/student/progress")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<StudentProgressResponse> myProgress() {
        return ApiResponse.success(
                progressService.getStudentProgress(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/student/analytics")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<AnalyticsResponse> myAnalytics() {
        return ApiResponse.success(
                analyticsService.getStudentAnalytics(SecurityUtil.getCurrentUserId()));
    }

    // ---------------- teacher ----------------

    @GetMapping("/teacher/progress")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<BatchProgressResponse>> batchProgress() {
        return ApiResponse.success(
                progressService.getTeacherBatchProgress(SecurityUtil.getCurrentUserId()));
    }

    /** Refused unless the student is on a batch this teacher runs. */
    @GetMapping("/teacher/progress/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<StudentProgressResponse> studentProgress(@PathVariable UUID studentId) {
        return ApiResponse.success(progressService.getStudentProgressForTeacher(
                studentId, SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/teacher/analytics")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<AnalyticsResponse> teacherAnalytics() {
        return ApiResponse.success(
                analyticsService.getTeacherAnalytics(SecurityUtil.getCurrentUserId()));
    }
}
