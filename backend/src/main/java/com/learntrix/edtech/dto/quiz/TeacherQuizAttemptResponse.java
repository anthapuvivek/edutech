package com.learntrix.edtech.dto.quiz;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * One student's attempt as the teacher sees it, for the performance view.
 * Contains no source of answers beyond the aggregate result.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeacherQuizAttemptResponse {
    private UUID attemptId;
    private UUID quizId;
    private String quizTitle;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private Integer score;
    private boolean passed;
    private Integer correctAnswers;
    private Integer totalQuestions;
    private String status;
    private Instant submittedAt;
}
