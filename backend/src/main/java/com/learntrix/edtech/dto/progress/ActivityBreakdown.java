package com.learntrix.edtech.dto.progress;

import lombok.*;

/**
 * One learning area, e.g. quizzes or assignments.
 *
 * <p>{@code applicable} is false when the course has nothing of this kind. That is
 * different from "the student did none of it", and the UI must not show 0% for a course
 * with no quizzes - so the distinction is carried explicitly rather than inferred.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ActivityBreakdown {
    private String area;
    private boolean applicable;
    private Integer completed;
    private Integer total;
    /** Completion percentage, or null when not applicable. */
    private Integer percent;
    /** Average achieved score where the area is graded; null where it is only completion. */
    private Integer averageScore;
}
