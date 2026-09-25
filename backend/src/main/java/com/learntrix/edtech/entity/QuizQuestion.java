package com.learntrix.edtech.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One question in a quiz.
 *
 * <p>The answer key lives on {@link QuizOption#isCorrect()}, not here. Student-facing DTOs
 * must map options without that flag until the attempt has been submitted.</p>
 */
@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    /** SINGLE_CHOICE today; the column exists so other types need no migration. */
    @Column(name = "question_type", nullable = false, length = 50)
    @Builder.Default
    private String questionType = "SINGLE_CHOICE";

    @Column(nullable = false)
    @Builder.Default
    private Integer points = 1;

    @Column(name = "sequence_number", nullable = false)
    @Builder.Default
    private Integer sequenceNumber = 0;

    /** Revealed only after submission. */
    @Column(columnDefinition = "TEXT")
    private String explanation;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuizOption> options = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
