package com.learntrix.edtech.entity;

import com.learntrix.edtech.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recording_watch_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "recording_id"})
})
@Getter
@Setter
public class RecordingWatchProgress extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recording_id", nullable = false)
    private ClassRecording recording;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "watched_seconds", nullable = false)
    private Integer watchedSeconds = 0;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds = 0;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "last_watched_at", nullable = false)
    private Instant lastWatchedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;
}
