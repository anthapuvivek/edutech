package com.learntrix.edtech.dto.quiz;

import lombok.*;
import java.util.UUID;

/**
 * Student view of an option.
 *
 * <p>There is deliberately no `correct` field. The answer key stays server-side; a student
 * receives only what they need to choose. Do not add one - a mapper that copies
 * QuizOption.isCorrect into this class would leak every answer before submission.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentQuizOptionResponse {
    private UUID id;
    private String optionText;
    private Integer sequenceNumber;
}
