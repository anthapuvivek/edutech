package com.learntrix.edtech.dto.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * What a student submits.
 *
 * <p>Deliberately carries no score, no `passed`, and no correct-option ids. Everything about
 * the result is computed server-side from QuizOption.isCorrect - a client that posts a score
 * is simply ignored, because there is nowhere here to put one.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubmitQuizAttemptRequest {

    @Valid
    private List<Answer> answers;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Answer {
        @NotNull(message = "questionId is required")
        private UUID questionId;

        /** Null means the student skipped the question; it is marked incorrect. */
        private UUID selectedOptionId;
    }
}
