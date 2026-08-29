package com.learntrix.edtech.service;

import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.dto.announcement.CourseAnnouncementResponse;
import com.learntrix.edtech.dto.announcement.CreateCourseAnnouncementRequest;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.CourseAnnouncement;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.CourseAnnouncementRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourseAnnouncementService {

    private final CourseAnnouncementRepository announcementRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseAccessService courseAccessService;

    public CourseAnnouncementService(
            CourseAnnouncementRepository announcementRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            CourseAccessService courseAccessService) {
        this.announcementRepository = announcementRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.courseAccessService = courseAccessService;
    }

    public CourseAnnouncementResponse createAnnouncement(CreateCourseAnnouncementRequest request, UUID teacherId) {
        courseAccessService.verifyTeacherCanManageCourse(teacherId, request.getCourseId());

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));

        CourseAnnouncement announcement = CourseAnnouncement.builder()
                .course(course)
                .teacher(teacher)
                .title(request.getTitle())
                .content(request.getContent())
                .priority(request.getPriority() != null ? request.getPriority() : "NORMAL")
                .build();

        CourseAnnouncement saved = announcementRepository.save(announcement);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CourseAnnouncementResponse> getTeacherAnnouncements(UUID teacherId) {
        List<UUID> courseIds = courseAccessService.getAuthorizedCourseIdsForTeacher(teacherId);
        if (courseIds.isEmpty()) return List.of();

        return announcementRepository.findByCourseIdInOrderByCreatedAtDesc(courseIds).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseAnnouncementResponse> getStudentAnnouncements(UUID studentId) {
        List<UUID> courseIds = courseAccessService.getAuthorizedCourseIdsForStudent(studentId);
        if (courseIds.isEmpty()) return List.of();

        return announcementRepository.findByCourseIdInOrderByCreatedAtDesc(courseIds).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseAnnouncementResponse getStudentAnnouncementById(UUID announcementId, UUID studentId) {
        CourseAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseAnnouncement", "id", announcementId));

        courseAccessService.verifyStudentCanAccessCourse(studentId, announcement.getCourse().getId());
        return mapToResponse(announcement);
    }

    private CourseAnnouncementResponse mapToResponse(CourseAnnouncement a) {
        String teacherName = a.getTeacher() != null ? a.getTeacher().getName() : "Instructor";

        return CourseAnnouncementResponse.builder()
                .id(a.getId())
                .courseId(a.getCourse().getId())
                .courseTitle(a.getCourse().getTitle())
                .teacherId(a.getTeacher() != null ? a.getTeacher().getId() : null)
                .teacherName(teacherName)
                .title(a.getTitle())
                .content(a.getContent())
                .priority(a.getPriority())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
