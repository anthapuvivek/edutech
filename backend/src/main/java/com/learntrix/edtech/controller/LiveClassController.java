package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.live.LiveClassResponse;
import com.learntrix.edtech.entity.LiveClass;
import com.learntrix.edtech.service.LiveClassService;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @GetMapping("/student/live-classes")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<LiveClassResponse>> getStudentLiveClasses() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        List<LiveClassResponse> liveClasses = liveClassService.getStudentLiveClasses(studentId);
        return ApiResponse.success(liveClasses);
    }

    @PostMapping("/teacher/live-classes")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ApiResponse<LiveClassResponse> createLiveClass(@RequestBody Map<String, Object> body) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        LiveClassResponse liveClass = liveClassService.createLiveClass(body, teacherId);
        return ApiResponse.success(liveClass);
    }

    @GetMapping("/teacher/live-classes")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ApiResponse<List<LiveClassResponse>> getTeacherLiveClasses() {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        List<LiveClassResponse> liveClasses = liveClassService.getTeacherLiveClasses(teacherId);
        return ApiResponse.success(liveClasses);
    }
}
