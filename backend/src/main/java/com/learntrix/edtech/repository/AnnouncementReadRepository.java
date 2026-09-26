package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.AnnouncementRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, UUID> {

    /** Always looked up by the authenticated student id, never one supplied by a client. */
    Optional<AnnouncementRead> findByAnnouncementIdAndStudentId(UUID announcementId, UUID studentId);

    List<AnnouncementRead> findByStudentId(UUID studentId);
}
