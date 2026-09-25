package com.learntrix.edtech.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "quizzes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    /**
     * Cohort scope, mirroring LiveClass.batch. Null means the quiz is course-wide; a batch
     * means only that cohort may see it, which is what keeps Morning and Evening separate.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "time_limit_minutes")
    @Builder.Default
    private Integer timeLimitMinutes = 30;

    @Column(name = "passing_score")
    @Builder.Default
    private Integer passingScore = 60;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PUBLISHED";

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private java.time.Instant createdAt = java.time.Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private java.time.Instant updatedAt = java.time.Instant.now();
}
