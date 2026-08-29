package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    List<Assignment> findByCourseId(UUID courseId);
    List<Assignment> findByCourseIdIn(List<UUID> courseIds);
    List<Assignment> findByTeacherId(UUID teacherId);
}
