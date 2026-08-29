package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.CourseAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseAnnouncementRepository extends JpaRepository<CourseAnnouncement, UUID> {
    List<CourseAnnouncement> findByCourseIdOrderByCreatedAtDesc(UUID courseId);
    List<CourseAnnouncement> findByCourseIdInOrderByCreatedAtDesc(List<UUID> courseIds);
    List<CourseAnnouncement> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId);
    List<CourseAnnouncement> findByTeacherIdAndCourseIdOrderByCreatedAtDesc(UUID teacherId, UUID courseId);
}
