package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.announcement.CourseAnnouncementResponse;
import com.learntrix.edtech.dto.announcement.CreateCourseAnnouncementRequest;
import com.learntrix.edtech.service.CourseAnnouncementService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AnnouncementController {

    private final CourseAnnouncementService announcementService;

    public AnnouncementController(CourseAnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    // --- Teacher Endpoints ---

    @PostMapping("/teacher/announcements")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<CourseAnnouncementResponse> createAnnouncement(
            @Valid @RequestBody CreateCourseAnnouncementRequest request) {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        CourseAnnouncementResponse response = announcementService.createAnnouncement(request, teacherId);
        return ApiResponse.success(response);
    }

    @GetMapping("/teacher/announcements")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<CourseAnnouncementResponse>> getTeacherAnnouncements() {
        UUID teacherId = SecurityUtil.getCurrentUserId();
        List<CourseAnnouncementResponse> list = announcementService.getTeacherAnnouncements(teacherId);
        return ApiResponse.success(list);
    }

    // --- Student Endpoints ---

    @PutMapping("/teacher/announcements/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<CourseAnnouncementResponse> updateAnnouncement(
            @PathVariable UUID id, @RequestBody CreateCourseAnnouncementRequest request) {
        return ApiResponse.success(announcementService.updateAnnouncement(
                id, request, SecurityUtil.getCurrentUserId()));
    }

    @DeleteMapping("/teacher/announcements/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable UUID id) {
        announcementService.deleteAnnouncement(id, SecurityUtil.getCurrentUserId());
        return ApiResponse.success(null);
    }

    /** Drives the "2 unread" badge. Scoped to the authenticated student. */
    @GetMapping("/student/announcements/unread-count")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<java.util.Map<String, Long>> unreadCount() {
        return ApiResponse.success(java.util.Map.of("unread",
                announcementService.getUnreadCount(SecurityUtil.getCurrentUserId())));
    }

    @PutMapping("/student/announcements/{id}/read")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<CourseAnnouncementResponse> markRead(@PathVariable UUID id) {
        return ApiResponse.success(
                announcementService.markRead(id, SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/student/announcements")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<CourseAnnouncementResponse>> getStudentAnnouncements() {
        UUID studentId = SecurityUtil.getCurrentUserId();
        List<CourseAnnouncementResponse> list = announcementService.getStudentAnnouncements(studentId);
        return ApiResponse.success(list);
    }

    @GetMapping("/student/announcements/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<CourseAnnouncementResponse> getStudentAnnouncementById(@PathVariable UUID id) {
        UUID studentId = SecurityUtil.getCurrentUserId();
        CourseAnnouncementResponse response = announcementService.getStudentAnnouncementById(id, studentId);
        return ApiResponse.success(response);
    }
}
