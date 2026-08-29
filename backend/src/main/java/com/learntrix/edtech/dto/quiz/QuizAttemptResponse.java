package com.learntrix.edtech.dto.quiz;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttemptResponse {
    private UUID id;
    private UUID quizId;
    private String quizTitle;
    private UUID studentId;
    private String studentName;
    private Integer score;
    private Integer passingScore;
    private boolean passed;
    private String status;
    private Instant startedAt;
    private Instant submittedAt;
}
