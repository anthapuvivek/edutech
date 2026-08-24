package com.learntrix.edtech.entity;

import com.learntrix.edtech.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "class_recordings")
@Getter
@Setter
public class ClassRecording extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "class_date", nullable = false)
    private Instant classDate;

    @Column(name = "video_storage_key", length = 512)
    private String videoStorageKey;

    @Column(name = "video_url", length = 1024)
    private String videoUrl;

    @Column(name = "hls_manifest_url", length = 1024)
    private String hlsManifestUrl;

    @Column(name = "thumbnail_url", length = 1024)
    private String thumbnailUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds = 0;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes = 0L;

    @Column(name = "video_format", length = 50)
    private String videoFormat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RecordingStatus status = RecordingStatus.DRAFT;

    @Column(nullable = false)
    private boolean published = false;

    @Column(name = "published_at")
    private Instant publishedAt;
}
