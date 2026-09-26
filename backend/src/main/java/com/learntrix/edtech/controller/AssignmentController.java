package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.assignment.*;
import com.learntrix.edtech.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    // --- Teacher Endpoints ---

    @PostMapping("/teacher/assignments")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<AssignmentResponse> createAssignment(
            @Valid @RequestBody CreateAssignmentRequest request) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        AssignmentResponse response = assignmentService.createAssignment(request, teacherId);
        return ApiResponse.success(response);
    }

    @GetMapping("/teacher/assignments")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<AssignmentResponse>> getTeacherAssignments() {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        List<AssignmentResponse> list = assignmentService.getTeacherAssignments(teacherId);
        return ApiResponse.success(list);
    }

    @GetMapping("/teacher/assignments/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<AssignmentResponse> getTeacherAssignmentById(@PathVariable UUID id) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        AssignmentResponse response = assignmentService.getTeacherAssignmentById(id, teacherId);
        return ApiResponse.success(response);
    }

    @PutMapping("/teacher/assignments/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<AssignmentResponse> updateAssignment(
            @PathVariable UUID id, @RequestBody CreateAssignmentRequest request) {
        return ApiResponse.success(
                assignmentService.updateAssignment(id, request, SecurityUtil.getCurrentUserId()));
    }

    @PutMapping("/teacher/assignments/{id}/publish")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<AssignmentResponse> publish(@PathVariable UUID id) {
        return ApiResponse.success(
                assignmentService.setPublished(id, true, SecurityUtil.getCurrentUserId()));
    }

    @PutMapping("/teacher/assignments/{id}/unpublish")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<AssignmentResponse> unpublish(@PathVariable UUID id) {
        return ApiResponse.success(
                assignmentService.setPublished(id, false, SecurityUtil.getCurrentUserId()));
    }

    @DeleteMapping("/teacher/assignments/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Void> deleteAssignment(@PathVariable UUID id) {
        assignmentService.deleteAssignment(id, SecurityUtil.getCurrentUserId());
        return ApiResponse.success(null);
    }

    @GetMapping("/teacher/assignments/{id}/submissions")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<AssignmentSubmissionResponse>> getAssignmentSubmissions(
            @PathVariable UUID id) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        List<AssignmentSubmissionResponse> submissions = assignmentService.getAssignmentSubmissions(id, teacherId);
        return ApiResponse.success(submissions);
    }

    @PostMapping("/teacher/assignments/{id}/submissions/{submissionId}/grade")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<AssignmentSubmissionResponse> gradeSubmission(
            @PathVariable UUID id,
            @PathVariable UUID submissionId,
            @Valid @RequestBody GradeSubmissionRequest request) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        AssignmentSubmissionResponse response = assignmentService.gradeSubmission(id, submissionId, teacherId, request);
        return ApiResponse.success(response);
    }

    // --- Student Endpoints ---

    @GetMapping("/student/assignments")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<AssignmentResponse>> getStudentAssignments() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        List<AssignmentResponse> list = assignmentService.getStudentAssignments(studentId);
        return ApiResponse.success(list);
    }

    @GetMapping("/student/assignments/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<AssignmentResponse> getStudentAssignmentById(@PathVariable UUID id) {
        UUID studentId = SecurityUtil.getCurrentUserId();
        AssignmentResponse response = assignmentService.getStudentAssignmentById(id, studentId);
        return ApiResponse.success(response);
    }

    @PostMapping("/student/assignments/{id}/submit")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<AssignmentSubmissionResponse> submitAssignment(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitAssignmentRequest request) {
        UUID studentId = SecurityUtil.getCurrentUserId();
        AssignmentSubmissionResponse response = assignmentService.submitAssignment(id, studentId, request);
        return ApiResponse.success(response);
    }
}
