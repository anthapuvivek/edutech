package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {

    Optional<Lead> findByEmail(String email);

    List<Lead> findByEmailOrPhone(String email, String phone);

    long countByStage(String stage);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.nextFollowUpAt >= :start AND l.nextFollowUpAt <= :end")
    long countFollowUpsBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.assignedTo.id = :staffId")
    long countByAssignedToId(@Param("staffId") UUID staffId);
}
