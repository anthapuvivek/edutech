package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.MentoringSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface MentoringSessionRepository extends JpaRepository<MentoringSession, UUID> {
    List<MentoringSession> findByMentorId(UUID mentorId);
    List<MentoringSession> findByStudentId(UUID studentId);
}
