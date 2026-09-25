package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.CodingSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CodingSubmissionRepository extends JpaRepository<CodingSubmission, UUID> {

    List<CodingSubmission> findByStudentIdOrderBySubmittedAtDesc(UUID studentId);

    List<CodingSubmission> findByProblemIdOrderBySubmittedAtDesc(UUID problemId);

    List<CodingSubmission> findByStudentIdAndProblemIdOrderBySubmittedAtDesc(
            UUID studentId, UUID problemId);
}
