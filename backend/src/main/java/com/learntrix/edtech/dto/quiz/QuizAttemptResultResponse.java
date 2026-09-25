package com.learntrix.edtech.dto.quiz;

import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The marked result of an attempt. Every figure here is computed by the backend. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuizAttemptResultResponse {
    private UUID attemptId;
    private UUID quizId;
    private String quizTitle;
    private Integer score;
    private Integer passingScore;
    private boolean passed;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private String status;
    private Instant startedAt;
    private Instant submittedAt;
    private List<QuizAnswerResultResponse> answers;
}
