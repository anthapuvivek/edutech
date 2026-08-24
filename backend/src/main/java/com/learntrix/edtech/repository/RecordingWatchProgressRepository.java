package com.learntrix.edtech.repository;

import com.learntrix.edtech.entity.RecordingWatchProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecordingWatchProgressRepository extends JpaRepository<RecordingWatchProgress, UUID> {
    Optional<RecordingWatchProgress> findByStudentIdAndRecordingId(UUID studentId, UUID recordingId);
    List<RecordingWatchProgress> findByStudentId(UUID studentId);
}
