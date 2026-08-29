package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.live.LiveClassResponse;
import com.learntrix.edtech.entity.LiveClass;
import com.learntrix.edtech.service.LiveClassService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class LiveClassController {

    private final LiveClassService liveClassService;

    public LiveClassController(LiveClassService liveClassService) {
        this.liveClassService = liveClassService;
    }

    // Read-only transaction: the response mappers walk LAZY @ManyToOne relations
    // (Enrollment.course, Job.company, ...) and open-in-view is disabled, so without
    // an open session these endpoints fail with LazyInitializationException.
    @Transactional(readOnly = true)
    @GetMapping("/student/live-classes")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<LiveClassResponse>> getStudentLiveClasses() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        List<LiveClassResponse> liveClasses = liveClassService.getStudentLiveClasses(studentId);
        return ApiResponse.success(liveClasses);
    }

    @Transactional(readOnly = true)
    @GetMapping("/student/live-classes/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<LiveClassResponse> getStudentLiveClassById(@PathVariable UUID id) {
        UUID studentId = SecurityUtil.getCurrentUserId();
        LiveClassResponse liveClass = liveClassService.getStudentLiveClassById(id, studentId);
        return ApiResponse.success(liveClass);
    }

    @PostMapping("/teacher/live-classes")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<LiveClassResponse> createLiveClass(@RequestBody Map<String, Object> body) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        LiveClassResponse liveClass = liveClassService.createLiveClass(body, teacherId);
        return ApiResponse.success(liveClass);
    }

    @Transactional(readOnly = true)
    @GetMapping("/teacher/live-classes")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<LiveClassResponse>> getTeacherLiveClasses() {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        List<LiveClassResponse> liveClasses = liveClassService.getTeacherLiveClasses(teacherId);
        return ApiResponse.success(liveClasses);
    }

    @Transactional(readOnly = true)
    @GetMapping("/teacher/live-classes/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<LiveClassResponse> getTeacherLiveClass(@PathVariable UUID id) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        LiveClassResponse liveClass = liveClassService.getLiveClassById(id, teacherId);
        return ApiResponse.success(liveClass);
    }

    @PutMapping("/teacher/live-classes/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<LiveClassResponse> updateLiveClass(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        LiveClassResponse liveClass = liveClassService.updateLiveClass(id, body, teacherId);
        return ApiResponse.success(liveClass);
    }

    @PatchMapping("/teacher/live-classes/{id}/cancel")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<LiveClassResponse> cancelLiveClass(@PathVariable UUID id) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        LiveClassResponse liveClass = liveClassService.cancelLiveClass(id, teacherId);
        return ApiResponse.success(liveClass);
    }

    @DeleteMapping("/teacher/live-classes/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Void> deleteLiveClass(@PathVariable UUID id) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        liveClassService.deleteLiveClass(id, teacherId);
        return ApiResponse.success(null);
    }
}
