package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.pagination.PageResponse;
import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.course.CourseResponse;
import com.learntrix.edtech.dto.course.EnrollmentResponse;
import com.learntrix.edtech.service.CourseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/courses")
    public ApiResponse<PageResponse<CourseResponse>> listCourses(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "9") int pageSize) {
        
        List<CourseResponse> allCourses = courseService.listCourses(search, category);
        
        int total = allCourses.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        
        List<CourseResponse> pagedCourses;
        if (fromIndex >= total) {
            pagedCourses = List.of();
        } else {
            pagedCourses = allCourses.subList(fromIndex, toIndex);
        }

        PageResponse<CourseResponse> pageResponse = PageResponse.<CourseResponse>builder()
                .items(pagedCourses)
                .page(page)
                .pageSize(pageSize)
                .total(total)
                .totalPages((int) Math.ceil((double) total / pageSize))
                .build();

        return ApiResponse.success(pageResponse);
    }

    @GetMapping("/courses/featured")
    public ApiResponse<List<CourseResponse>> getFeaturedCourses(@RequestParam(value = "limit", defaultValue = "6") int limit) {
        List<CourseResponse> allCourses = courseService.listCourses(null, null);
        List<CourseResponse> featured = allCourses.stream()
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
        return ApiResponse.success(featured);
    }

    @GetMapping("/courses/{slug}")
    public ApiResponse<CourseResponse> getCourseBySlug(@PathVariable("slug") String slug) {
        CourseResponse course = courseService.getCourseBySlug(slug);
        return ApiResponse.success(course);
    }

    @PostMapping("/courses/{id}/enroll")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<EnrollmentResponse> enroll(@PathVariable("id") UUID id) {
        UUID studentId = SecurityUtil.getCurrentUserId();
        EnrollmentResponse enrollment = courseService.enrollStudent(id, studentId);
        return ApiResponse.success(enrollment);
    }

    @GetMapping("/student/enrollments")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<EnrollmentResponse>> getStudentEnrollments() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        List<EnrollmentResponse> enrollments = courseService.getStudentEnrollments(studentId);
        return ApiResponse.success(enrollments);
    }
}
