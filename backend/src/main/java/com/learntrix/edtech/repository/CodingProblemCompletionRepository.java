package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.CodingProblemCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CodingProblemCompletionRepository
        extends JpaRepository<CodingProblemCompletion, UUID> {

    /** The caller's own row. Always looked up by the authenticated id, never a supplied one. */
    Optional<CodingProblemCompletion> findByProblemIdAndStudentId(UUID problemId, UUID studentId);

    List<CodingProblemCompletion> findByStudentId(UUID studentId);

    List<CodingProblemCompletion> findByProblemId(UUID problemId);

    /** Used by the teacher view to count who has marked a problem done. */
    List<CodingProblemCompletion> findByProblemIdAndCompletedTrue(UUID problemId);
}
