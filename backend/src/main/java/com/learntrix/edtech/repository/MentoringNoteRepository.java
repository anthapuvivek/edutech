package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.MentoringNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface MentoringNoteRepository extends JpaRepository<MentoringNote, UUID> {
    List<MentoringNote> findByStudentId(UUID studentId);
    List<MentoringNote> findByMentorId(UUID mentorId);
}
