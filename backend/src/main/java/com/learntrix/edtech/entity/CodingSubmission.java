package com.learntrix.edtech.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * One Submit by a student. Run Code is not persisted - it is a scratch execution against
 * sample cases only, and recording it would distort acceptance-rate statistics.
 */
@Entity
@Table(name = "coding_submissions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private CodingProblem problem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false, length = 50)
    private String language;

    @Column(name = "source_code", nullable = false, columnDefinition = "TEXT")
    private String sourceCode;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "QUEUED";

    @Column(name = "passed_count", nullable = false)
    @Builder.Default
    private Integer passedCount = 0;

    @Column(name = "total_count", nullable = false)
    @Builder.Default
    private Integer totalCount = 0;

    @Column(name = "runtime_ms")
    private Integer runtimeMs;

    /** Compiler or judge message. Never carries hidden test data. */
    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "submitted_at", nullable = false)
    @Builder.Default
    private Instant submittedAt = Instant.now();
}
