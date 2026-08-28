package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.pagination.PageResponse;
import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.course.CourseResponse;
import com.learntrix.edtech.dto.course.EnrollmentResponse;
import com.learntrix.edtech.service.CourseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
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

    // Read-only transaction: the response mappers walk LAZY @ManyToOne relations
    // (Enrollment.course, Job.company, ...) and open-in-view is disabled, so without
    // an open session these endpoints fail with LazyInitializationException.
    @Transactional(readOnly = true)
    @GetMapping("/courses")
    public ApiResponse<PageResponse<CourseResponse>> listCourses(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "maxPrice", required = false) Integer maxPrice,
            @RequestParam(value = "minRating", required = false) Double minRating,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "9") int pageSize) {

        List<CourseResponse> allCourses =
                courseService.listCourses(search, category, level, maxPrice, minRating, sort);
        
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

    @Transactional(readOnly = true)
    @GetMapping("/courses/featured")
    public ApiResponse<List<CourseResponse>> getFeaturedCourses(@RequestParam(value = "limit", defaultValue = "6") int limit) {
        // "Featured" means most popular first, matching courseService.featured() on the client.
        List<CourseResponse> allCourses = courseService.listCourses(null, null, null, null, null, "popular");
        List<CourseResponse> featured = allCourses.stream()
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
        return ApiResponse.success(featured);
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    @GetMapping("/courses/{id}/access")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<java.util.Map<String, Object>> checkAccess(@PathVariable("id") UUID id) {
        UUID studentId = SecurityUtil.getCurrentUserId();
        java.util.Map<String, Object> access = courseService.checkCourseAccess(id, studentId);
        return ApiResponse.success(access);
    }

    @Transactional(readOnly = true)
    @GetMapping("/student/enrollments")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<EnrollmentResponse>> getStudentEnrollments() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        List<EnrollmentResponse> enrollments = courseService.getStudentEnrollments(studentId);
        return ApiResponse.success(enrollments);
    }
}
