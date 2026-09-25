package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.CodingTestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CodingTestCaseRepository extends JpaRepository<CodingTestCase, UUID> {

    List<CodingTestCase> findByProblemIdOrderBySequenceNumberAsc(UUID problemId);

    /** Sample cases only - the set Run Code may execute and disclose. */
    List<CodingTestCase> findByProblemIdAndSampleTrueOrderBySequenceNumberAsc(UUID problemId);
}
