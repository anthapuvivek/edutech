package com.learntrix.edtech.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A coding practice problem.
 *
 * <p>Scoped exactly like a quiz: {@code batch} null means course-wide, a batch means that
 * cohort only. Publication uses {@code status} (DRAFT / PUBLISHED), matching Quiz.</p>
 */
@Entity
@Table(name = "coding_problems")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private Module module;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @Column(nullable = false)
    private String title;

    private String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String difficulty = "EASY";

    /** Where the problem actually lives: LEETCODE, HACKERRANK, CODECHEF, OTHER... */
    @Column(length = 40)
    private String platform;

    /** The platform's own numbering, for display and search only - never for the URL. */
    @Column(name = "problem_number", length = 40)
    private String problemNumber;

    /**
     * The link students open. Source of truth for reaching the problem.
     *
     * <p>Stored exactly as the teacher entered it, after the service has validated that it
     * is http or https. A number cannot substitute for this: LeetCode #1 is
     * /problems/two-sum/, which no arithmetic derives.</p>
     */
    @Column(name = "problem_url", columnDefinition = "TEXT")
    private String problemUrl;

    /** Comma-separated tags, e.g. "Arrays, Hash Table". */
    @Column(columnDefinition = "TEXT")
    private String topics;

    @Column(name = "constraints_text", columnDefinition = "TEXT")
    private String constraintsText;

    @Column(name = "starter_code", columnDefinition = "TEXT")
    private String starterCode;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String language = "java";

    @Column(name = "time_limit_ms", nullable = false)
    @Builder.Default
    private Integer timeLimitMs = 2000;

    @Column(name = "memory_limit_mb", nullable = false)
    @Builder.Default
    private Integer memoryLimitMb = 128;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
