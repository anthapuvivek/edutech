package com.learntrix.edtech.dto.quiz;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResponse {
    private UUID id;
    private UUID courseId;
    private String courseTitle;
    private UUID teacherId;
    private String teacherName;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private Integer passingScore;
    private String status;
    private Instant createdAt;
    private boolean attempted;
    private Integer score;
    private String attemptStatus;
}
