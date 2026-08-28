package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.student.ActivityItemResponse;
import com.learntrix.edtech.dto.student.StudentProfileResponse;
import com.learntrix.edtech.dto.student.StudentStatsResponse;
import com.learntrix.edtech.service.StudentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // Read-only transaction: the response mappers walk LAZY @ManyToOne relations
    // (Enrollment.course, Job.company, ...) and open-in-view is disabled, so without
    // an open session these endpoints fail with LazyInitializationException.
    @Transactional(readOnly = true)
    @GetMapping("/profile")
    public ApiResponse<StudentProfileResponse> getProfile() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        StudentProfileResponse profile = studentService.getProfile(studentId);
        return ApiResponse.success(profile);
    }

    @Transactional(readOnly = true)
    @GetMapping("/stats")
    public ApiResponse<StudentStatsResponse> getStats() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        StudentStatsResponse stats = studentService.getStats(studentId);
        return ApiResponse.success(stats);
    }

    @Transactional(readOnly = true)
    @GetMapping("/activity")
    public ApiResponse<List<ActivityItemResponse>> getActivity() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        List<ActivityItemResponse> activity = studentService.getActivity(studentId);
        return ApiResponse.success(activity);
    }

    @Transactional(readOnly = true)
    @GetMapping("/leaderboard")
    public ApiResponse<List<com.learntrix.edtech.dto.student.LeaderboardEntryResponse>> getLeaderboard(
            @org.springframework.web.bind.annotation.RequestParam(value = "scope", required = false, defaultValue = "global") String scope) {
        UUID studentId = SecurityUtil.getCurrentUserId();
        List<com.learntrix.edtech.dto.student.LeaderboardEntryResponse> leaderboard = studentService.getLeaderboard(studentId, scope);
        return ApiResponse.success(leaderboard);
    }
}
