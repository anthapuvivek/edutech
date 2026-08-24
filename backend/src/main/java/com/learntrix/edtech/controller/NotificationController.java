package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.entity.Notification;
import com.learntrix.edtech.repository.NotificationRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student/notifications")
@PreAuthorize("hasRole('STUDENT')")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public ApiResponse<List<Notification>> getNotifications() {
        UUID userId = SecurityUtil.getCurrentUserId();
        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ApiResponse.success(list);
    }

    @PostMapping("/{id}/read")
    public ApiResponse<String> markAsRead(@PathVariable("id") UUID id) {
        Notification notification = notificationRepository.findById(id).orElse(null);
        if (notification != null) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
        return ApiResponse.success("Notification marked as read");
    }
}
