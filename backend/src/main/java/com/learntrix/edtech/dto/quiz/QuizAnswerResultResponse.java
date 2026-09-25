package com.learntrix.edtech.dto.quiz;

import lombok.*;
import java.util.UUID;

/**
 * Per-question outcome, returned only after submission.
 *
 * <p>This is the one place a correct option id is disclosed, and only for an attempt the
 * requesting student owns and has already submitted.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuizAnswerResultResponse {
    private UUID questionId;
    private String questionText;
    private UUID selectedOptionId;
    private UUID correctOptionId;
    private boolean correct;
    private Integer pointsAwarded;
    private String explanation;
}
