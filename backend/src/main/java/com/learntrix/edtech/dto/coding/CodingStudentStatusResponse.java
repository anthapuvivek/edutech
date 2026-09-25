package com.learntrix.edtech.dto.coding;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

/** One student's standing on one coding problem, collapsed from all their submissions. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingStudentStatusResponse {
    private UUID studentId;
    private String studentName;
    private String studentEmail;

    /** SOLVED, ATTEMPTED or NOT_ATTEMPTED - derived, never stored. */
    private String status;

    /** Highest passed/total ratio as a percentage, or null when never attempted. */
    private Integer bestScore;
    private Integer bestPassedCount;
    private Integer totalCount;
    private Integer submissionCount;
    private Instant lastSubmissionAt;
}
