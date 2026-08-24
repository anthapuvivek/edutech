package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.PlacementOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PlacementOfferRepository extends JpaRepository<PlacementOffer, UUID> {
    List<PlacementOffer> findByStudentId(UUID studentId);
}
