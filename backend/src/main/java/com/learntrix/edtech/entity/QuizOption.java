package com.learntrix.edtech.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * One selectable answer for a {@link QuizQuestion}.
 *
 * <p>{@code isCorrect} is the answer key. It is read by the backend when marking an attempt
 * and must never be serialised into a student response before submission.</p>
 */
@Entity
@Table(name = "quiz_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizOption {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;

    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    private boolean correct = false;

    @Column(name = "sequence_number", nullable = false)
    @Builder.Default
    private Integer sequenceNumber = 0;
}
