package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.student.ActivityItemResponse;
import com.learntrix.edtech.dto.student.StudentProfileResponse;
import com.learntrix.edtech.dto.student.StudentStatsResponse;
import com.learntrix.edtech.service.StudentService;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @GetMapping("/profile")
    public ApiResponse<StudentProfileResponse> getProfile() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        StudentProfileResponse profile = studentService.getProfile(studentId);
        return ApiResponse.success(profile);
    }

    @GetMapping("/stats")
    public ApiResponse<StudentStatsResponse> getStats() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        StudentStatsResponse stats = studentService.getStats(studentId);
        return ApiResponse.success(stats);
    }

    @GetMapping("/activity")
    public ApiResponse<List<ActivityItemResponse>> getActivity() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        List<ActivityItemResponse> activity = studentService.getActivity(studentId);
        return ApiResponse.success(activity);
    }
}
