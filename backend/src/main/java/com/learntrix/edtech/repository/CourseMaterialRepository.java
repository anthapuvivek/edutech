package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.CourseMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, UUID> {
    List<CourseMaterial> findByCourseIdOrderByCreatedAtDesc(UUID courseId);
    List<CourseMaterial> findByCourseIdInOrderByCreatedAtDesc(List<UUID> courseIds);
    List<CourseMaterial> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId);
    List<CourseMaterial> findByTeacherIdAndCourseIdOrderByCreatedAtDesc(UUID teacherId, UUID courseId);
}
