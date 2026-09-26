package com.learntrix.edtech.dto.progress;

import lombok.*;
import java.util.List;
import java.util.UUID;

/**
 * A student flagged for attention, with the reasons shown.
 *
 * <p>There is no model and no prediction here - a student appears only because a stated
 * threshold was crossed, and every reason is listed so a teacher can disagree with it.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AtRiskStudentResponse {
    private UUID studentId;
    private String studentName;
    private Integer overallProgress;
    private Integer averageQuizScore;
    private Integer missedAssignments;
    /** Human-readable, one per triggered rule. */
    private List<String> reasons;
}
