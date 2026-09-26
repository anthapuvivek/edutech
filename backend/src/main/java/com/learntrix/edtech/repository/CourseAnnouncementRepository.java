package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.CourseAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseAnnouncementRepository extends JpaRepository<CourseAnnouncement, UUID> {
    List<CourseAnnouncement> findByCourseIdOrderByCreatedAtDesc(UUID courseId);
    List<CourseAnnouncement> findByCourseIdInOrderByCreatedAtDesc(List<UUID> courseIds);
    List<CourseAnnouncement> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId);
    List<CourseAnnouncement> findByTeacherIdAndCourseIdOrderByCreatedAtDesc(UUID teacherId, UUID courseId);

    /**
     * Announcements a student may see: course-wide, or their own batch.
     *
     * <p>Same rule as quizzes, assignments and coding problems, enforced in the query so
     * no caller can forget it.</p>
     */
    @Query("SELECT DISTINCT a FROM CourseAnnouncement a LEFT JOIN a.batch b LEFT JOIN b.students s "
         + "WHERE a.course.id IN :courseIds AND (a.batch IS NULL OR s.id = :studentId) "
         + "ORDER BY a.createdAt DESC")
    List<CourseAnnouncement> findAccessible(
            @Param("studentId") UUID studentId,
            @Param("courseIds") List<UUID> courseIds);
}
