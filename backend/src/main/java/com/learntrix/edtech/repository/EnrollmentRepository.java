package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    Optional<Enrollment> findByStudentIdAndCourseId(UUID studentId, UUID courseId);
    List<Enrollment> findByStudentId(UUID studentId);
    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);
    List<Enrollment> findByCourseId(UUID courseId);
    List<Enrollment> findByCourseIdIn(List<UUID> courseIds);
}
