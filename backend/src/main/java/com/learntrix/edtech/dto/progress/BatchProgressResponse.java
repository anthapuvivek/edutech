package com.learntrix.edtech.dto.progress;

import lombok.*;
import java.util.List;
import java.util.UUID;

/** Batch-level rollup for a teacher, plus one row per student in that batch. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BatchProgressResponse {
    private UUID batchId;
    private String batchName;
    private UUID courseId;
    private String courseTitle;

    private Integer studentCount;
    private Integer averageProgress;
    private Integer averageQuizScore;
    private Integer averageAssignmentScore;

    private List<StudentProgressResponse> students;
    private List<String> unavailableMetrics;
}
