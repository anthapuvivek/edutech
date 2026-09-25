package com.learntrix.edtech.dto.coding;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

/** Aggregate coding performance for one student. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingPerformanceResponse {
    private UUID studentId;
    private String studentName;
    private Integer problemsAvailable;
    private Integer problemsAttempted;
    private Integer problemsSolved;
    private Integer totalSubmissions;
    private Integer acceptedSubmissions;
    private Integer wrongAnswers;
    private Integer compileErrors;
    private Integer runtimeErrors;
    private Integer timeLimitExceeded;
    private Double acceptanceRate;
    private Instant lastSubmissionAt;
}
