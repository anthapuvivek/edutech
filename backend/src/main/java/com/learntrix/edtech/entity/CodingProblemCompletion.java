package com.learntrix.edtech.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A student's own record that they finished an externally hosted problem.
 *
 * <p>This is self-reported and nothing more. LearnTriX has no supported way to ask LeetCode
 * or HackerRank whether a given student solved a given problem, so there is deliberately no
 * score, no verdict and no proof field here - storing one would imply a verification that
 * does not happen.</p>
 *
 * <p>The unique constraint on (problem, student) means toggling updates one row rather than
 * accumulating them, so a completion count cannot be inflated by clicking twice.</p>
 */
@Entity
@Table(name = "coding_problem_completions",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_coding_completion_student_problem",
               columnNames = {"problem_id", "student_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingProblemCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private CodingProblem problem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    @Builder.Default
    private boolean completed = true;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "NOT_STARTED";

    @Column(name = "opened_at")
    private Instant openedAt;

    /** Set when completed flips to true, cleared when it flips back. */
    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
