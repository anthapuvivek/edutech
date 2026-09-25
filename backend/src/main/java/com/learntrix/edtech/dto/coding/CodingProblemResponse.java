package com.learntrix.edtech.dto.coding;

import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A problem. `sampleTestCases` is populated for students; teachers get the full set separately. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingProblemResponse {
    private UUID id;
    private UUID courseId;
    private String courseTitle;
    private UUID batchId;
    private String batchName;
    private UUID teacherId;
    private String teacherName;
    private String title;
    private String slug;
    private String description;
    private String difficulty;
    private String constraintsText;
    private String starterCode;
    private String language;
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    private String status;
    private Integer testCaseCount;
    private Instant createdAt;
    private List<StudentCodingTestCaseResponse> sampleTestCases;
    /** The requesting student's best outcome so far, or null. */
    private String myStatus;
}
