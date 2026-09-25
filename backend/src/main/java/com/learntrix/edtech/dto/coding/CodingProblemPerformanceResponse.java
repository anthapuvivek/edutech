package com.learntrix.edtech.dto.coding;

import lombok.*;
import java.util.List;
import java.util.UUID;

/**
 * How one coding problem is going across the cohort it was published to.
 *
 * <p>Every headline count is a count of DISTINCT students, never of submissions. A student
 * who submits four times and is accepted twice contributes 1 to {@code studentsAttempted}
 * and 1 to {@code studentsSolved}.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingProblemPerformanceResponse {
    private UUID problemId;
    private String problemTitle;
    private UUID batchId;
    private String batchName;

    /** Students the problem is visible to: the batch roster, or the whole enrolment. */
    private Integer studentsInScope;
    private Integer studentsAttempted;
    private Integer studentsSolved;
    private Integer totalSubmissions;

    /** One row per student in scope, including those who never attempted. */
    private List<CodingStudentStatusResponse> students;
}
