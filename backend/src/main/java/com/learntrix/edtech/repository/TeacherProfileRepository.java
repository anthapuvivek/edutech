package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, UUID> {
    Optional<TeacherProfile> findByUserId(UUID userId);
    Optional<TeacherProfile> findByEmployeeId(String employeeId);
    boolean existsByEmployeeId(String employeeId);
}
