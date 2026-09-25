package com.learntrix.edtech.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * One answer a student gave during an attempt.
 *
 * <p>{@code correct} and {@code pointsAwarded} are written by the backend when it marks the
 * attempt. They are never accepted from the client - a score posted from the browser is
 * ignored and recomputed from {@link QuizOption#isCorrect()}.</p>
 *
 * <p>A unique constraint on (attempt, question) makes duplicate answers impossible at the
 * database level rather than something the service has to guard.</p>
 */
@Entity
@Table(
    name = "quiz_attempt_answers",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_quiz_attempt_answer",
        columnNames = {"attempt_id", "question_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    /** Null when the student skipped the question. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private QuizOption selectedOption;

    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    private boolean correct = false;

    @Column(name = "points_awarded", nullable = false)
    @Builder.Default
    private Integer pointsAwarded = 0;

    @Column(name = "answered_at", nullable = false)
    @Builder.Default
    private Instant answeredAt = Instant.now();
}
