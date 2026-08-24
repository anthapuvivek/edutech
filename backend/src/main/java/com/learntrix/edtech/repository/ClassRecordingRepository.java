package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.ClassRecording;
import com.learntrix.edtech.entity.RecordingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClassRecordingRepository extends JpaRepository<ClassRecording, UUID> {

    Page<ClassRecording> findByCourseId(UUID courseId, Pageable pageable);

    List<ClassRecording> findByModuleId(UUID moduleId);

    List<ClassRecording> findByLessonId(UUID lessonId);

    Page<ClassRecording> findByTeacherId(UUID teacherId, Pageable pageable);

    List<ClassRecording> findByCourseIdAndPublishedTrueOrderByClassDateDesc(UUID courseId);

    @Query("SELECT r FROM ClassRecording r WHERE r.course.id = :courseId AND r.published = true ORDER BY r.classDate DESC")
    List<ClassRecording> findPublishedByCourseId(@Param("courseId") UUID courseId);

    @Query("SELECT r FROM ClassRecording r JOIN Enrollment e ON r.course.id = e.course.id WHERE e.student.id = :studentId AND r.published = true ORDER BY r.classDate DESC")
    List<ClassRecording> findPublishedByStudentEnrollment(@Param("studentId") UUID studentId);

    Page<ClassRecording> findByStatus(RecordingStatus status, Pageable pageable);
}
