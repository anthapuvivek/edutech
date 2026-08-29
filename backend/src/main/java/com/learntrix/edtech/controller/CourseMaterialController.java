package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.material.CourseMaterialResponse;
import com.learntrix.edtech.dto.material.CreateCourseMaterialRequest;
import com.learntrix.edtech.service.CourseMaterialService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CourseMaterialController {

    private final CourseMaterialService materialService;

    public CourseMaterialController(CourseMaterialService materialService) {
        this.materialService = materialService;
    }

    // --- Teacher Endpoints ---

    @PostMapping("/teacher/materials")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<CourseMaterialResponse> createMaterial(
            @Valid @RequestBody CreateCourseMaterialRequest request) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        CourseMaterialResponse response = materialService.createMaterial(request, teacherId);
        return ApiResponse.success(response);
    }

    @GetMapping("/teacher/materials")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<CourseMaterialResponse>> getTeacherMaterials() {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        List<CourseMaterialResponse> list = materialService.getTeacherMaterials(teacherId);
        return ApiResponse.success(list);
    }

    @DeleteMapping("/teacher/materials/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Map<String, Object>> deleteMaterial(@PathVariable UUID id) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        materialService.deleteMaterial(id, teacherId);
        return ApiResponse.success(Map.of("message", "Material deleted successfully"));
    }

    // --- Student Endpoints ---

    @GetMapping("/student/materials")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<CourseMaterialResponse>> getStudentMaterials() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        List<CourseMaterialResponse> list = materialService.getStudentMaterials(studentId);
        return ApiResponse.success(list);
    }

    @GetMapping("/student/materials/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<CourseMaterialResponse> getStudentMaterialById(@PathVariable UUID id) {
        UUID studentId = SecurityUtil.getCurrentUserId();
        CourseMaterialResponse response = materialService.getStudentMaterialById(id, studentId);
        return ApiResponse.success(response);
    }
}
