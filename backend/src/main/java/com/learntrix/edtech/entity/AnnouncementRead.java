package com.learntrix.edtech.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Records that one student has opened one announcement.
 *
 * <p>Absence of a row means unread, so nothing has to be written until a student actually
 * reads something. The unique constraint keeps re-opening idempotent.</p>
 */
@Entity
@Table(name = "announcement_reads",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_announcement_read",
               columnNames = {"announcement_id", "student_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnnouncementRead {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private CourseAnnouncement announcement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "read_at", nullable = false)
    @Builder.Default
    private Instant readAt = Instant.now();
}
