package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {
    List<AssignmentSubmission> findByStudentId(UUID studentId);
    List<AssignmentSubmission> findByAssignmentId(UUID assignmentId);
}
