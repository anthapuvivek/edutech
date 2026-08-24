package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.PlacementInterview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PlacementInterviewRepository extends JpaRepository<PlacementInterview, UUID> {
    List<PlacementInterview> findByStudentId(UUID studentId);
}
