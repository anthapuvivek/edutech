package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.LeadFollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeadFollowUpRepository extends JpaRepository<LeadFollowUp, UUID> {
    List<LeadFollowUp> findByLeadIdOrderByCreatedAtDesc(UUID leadId);
    List<LeadFollowUp> findByFollowUpDateAndStatus(String followUpDate, String status);
}
