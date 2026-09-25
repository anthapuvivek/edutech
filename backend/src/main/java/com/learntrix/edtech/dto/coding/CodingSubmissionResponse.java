package com.learntrix.edtech.dto.coding;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

/** A stored submission. Carries aggregate counts only - no hidden test detail. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingSubmissionResponse {
    private UUID id;
    private UUID problemId;
    private String problemTitle;
    private UUID studentId;
    private String studentName;
    private String language;
    private String status;
    private Integer passedCount;
    private Integer totalCount;
    private Integer runtimeMs;
    private String message;
    private Instant submittedAt;
    /** Returned only to the owner of the submission and to an authorised teacher. */
    private String sourceCode;
}
