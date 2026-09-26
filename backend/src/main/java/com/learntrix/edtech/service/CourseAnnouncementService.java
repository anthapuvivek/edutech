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
import com.learntrix.edtech.common.exception.BusinessException;
import com.learntrix.edtech.common.exception.CourseAccessDeniedException;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.entity.AnnouncementRead;
import com.learntrix.edtech.entity.Batch;
import com.learntrix.edtech.repository.AnnouncementReadRepository;
import com.learntrix.edtech.repository.BatchRepository;
import org.springframework.http.HttpStatus;
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
    private final BatchRepository batchRepository;
    private final AnnouncementReadRepository readRepository;

    public CourseAnnouncementService(
            CourseAnnouncementRepository announcementRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            CourseAccessService courseAccessService,
            BatchRepository batchRepository,
            AnnouncementReadRepository readRepository) {
        this.announcementRepository = announcementRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.courseAccessService = courseAccessService;
        this.batchRepository = batchRepository;
        this.readRepository = readRepository;
    }

    private boolean isAdmin() {
        return SecurityUtil.hasRole("ADMIN") || SecurityUtil.hasRole("SUPER_ADMIN");
    }

    /** Course authority is not enough for a batch announcement - the cohort must be yours. */
    private void verifyTeacherCanManage(CourseAnnouncement a, UUID teacherId) {
        if (isAdmin()) return;
        courseAccessService.verifyTeacherCanManageCourse(teacherId, a.getCourse().getId());
        Batch batch = a.getBatch();
        if (batch != null
                && (batch.getTeacher() == null || !batch.getTeacher().getId().equals(teacherId))) {
            throw new CourseAccessDeniedException(
                    "You are not authorized to manage announcements for that batch");
        }
    }

    private Batch resolveBatch(UUID batchId, UUID courseId, UUID teacherId) {
        if (batchId == null) return null;
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId));
        if (batch.getCourse() == null || !batch.getCourse().getId().equals(courseId)) {
            throw new BusinessException("BATCH_COURSE_MISMATCH",
                    "That batch does not belong to the selected course.", HttpStatus.BAD_REQUEST);
        }
        if (!isAdmin()
                && (batch.getTeacher() == null || !batch.getTeacher().getId().equals(teacherId))) {
            throw new CourseAccessDeniedException(
                    "You are not authorized to announce to that batch");
        }
        return batch;
    }

    /** Enrolled + (for a batch announcement) a member of that batch. */
    private void verifyStudentCanAccess(CourseAnnouncement a, UUID studentId) {
        if (isAdmin()) return;
        courseAccessService.verifyStudentCanAccessCourse(studentId, a.getCourse().getId());
        Batch batch = a.getBatch();
        if (batch != null) {
            boolean member = batch.getStudents() != null
                    && batch.getStudents().stream().anyMatch(u -> u.getId().equals(studentId));
            if (!member) {
                throw new CourseAccessDeniedException("This announcement is not available.");
            }
        }
    }

    public CourseAnnouncementResponse createAnnouncement(CreateCourseAnnouncementRequest request, UUID teacherId) {
        if (!isAdmin()) {
            courseAccessService.verifyTeacherCanManageCourse(teacherId, request.getCourseId());
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));

        CourseAnnouncement announcement = CourseAnnouncement.builder()
                .course(course)
                .teacher(teacher)
                .batch(resolveBatch(request.getBatchId(), course.getId(), teacherId))
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

        // Batch-aware: the previous call returned every announcement on the course.
        java.util.Set<UUID> readIds = readRepository.findByStudentId(studentId).stream()
                .map(r -> r.getAnnouncement().getId())
                .collect(Collectors.toSet());

        return announcementRepository.findAccessible(studentId, courseIds).stream()
                .map(a -> {
                    CourseAnnouncementResponse r = mapToResponse(a);
                    r.setRead(readIds.contains(a.getId()));
                    return r;
                })
                .collect(Collectors.toList());
    }

    /** Unread = accessible announcements with no read row for this student. */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID studentId) {
        List<UUID> courseIds = courseAccessService.getAuthorizedCourseIdsForStudent(studentId);
        if (courseIds.isEmpty()) return 0;
        java.util.Set<UUID> readIds = readRepository.findByStudentId(studentId).stream()
                .map(r -> r.getAnnouncement().getId())
                .collect(Collectors.toSet());
        return announcementRepository.findAccessible(studentId, courseIds).stream()
                .filter(a -> !readIds.contains(a.getId()))
                .count();
    }

    /** Marks read for the authenticated student only; repeat calls are idempotent. */
    public CourseAnnouncementResponse markRead(UUID announcementId, UUID studentId) {
        CourseAnnouncement a = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseAnnouncement", "id", announcementId));
        verifyStudentCanAccess(a, studentId);

        readRepository.findByAnnouncementIdAndStudentId(announcementId, studentId)
                .orElseGet(() -> {
                    AnnouncementRead r = new AnnouncementRead();
                    r.setAnnouncement(a);
                    r.setStudent(userRepository.findById(studentId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId)));
                    r.setReadAt(java.time.Instant.now());
                    return readRepository.save(r);
                });

        CourseAnnouncementResponse res = mapToResponse(a);
        res.setRead(true);
        return res;
    }

    public CourseAnnouncementResponse updateAnnouncement(
            UUID announcementId, CreateCourseAnnouncementRequest request, UUID teacherId) {
        CourseAnnouncement a = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseAnnouncement", "id", announcementId));
        verifyTeacherCanManage(a, teacherId);

        if (request.getTitle() != null) a.setTitle(request.getTitle());
        if (request.getContent() != null) a.setContent(request.getContent());
        if (request.getPriority() != null) a.setPriority(request.getPriority());
        a.setUpdatedAt(java.time.Instant.now());
        return mapToResponse(announcementRepository.save(a));
    }

    public void deleteAnnouncement(UUID announcementId, UUID teacherId) {
        CourseAnnouncement a = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseAnnouncement", "id", announcementId));
        verifyTeacherCanManage(a, teacherId);
        announcementRepository.delete(a);
    }

    @Transactional(readOnly = true)
    public CourseAnnouncementResponse getStudentAnnouncementById(UUID announcementId, UUID studentId) {
        CourseAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseAnnouncement", "id", announcementId));

        verifyStudentCanAccess(announcement, studentId);
        CourseAnnouncementResponse r = mapToResponse(announcement);
        r.setRead(readRepository.findByAnnouncementIdAndStudentId(announcementId, studentId).isPresent());
        return r;
    }

    private CourseAnnouncementResponse mapToResponse(CourseAnnouncement a) {
        String teacherName = a.getTeacher() != null ? a.getTeacher().getName() : "Instructor";

        return CourseAnnouncementResponse.builder()
                .id(a.getId())
                .courseId(a.getCourse().getId())
                .courseTitle(a.getCourse().getTitle())
                .batchId(a.getBatch() != null ? a.getBatch().getId() : null)
                .batchName(a.getBatch() != null ? a.getBatch().getName() : null)
                .teacherId(a.getTeacher() != null ? a.getTeacher().getId() : null)
                .teacherName(teacherName)
                .title(a.getTitle())
                .content(a.getContent())
                .priority(a.getPriority())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
