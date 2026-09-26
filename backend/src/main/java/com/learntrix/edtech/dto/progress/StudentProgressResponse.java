package com.learntrix.edtech.dto.progress;

import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One student's progress, computed from stored activity only.
 *
 * <p>Nothing here is stored or estimated. If an area has no data the breakdown marks it
 * not applicable rather than contributing a zero, so overall progress cannot be dragged
 * down by a course that simply has no quizzes.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentProgressResponse {
    private UUID studentId;
    private String studentName;
    private String studentEmail;

    /** Mean of the applicable areas' completion percentages. */
    private Integer overallProgress;

    private List<ActivityBreakdown> areas;

    private Integer quizzesAttempted;
    private Integer quizzesTotal;
    private Integer averageQuizScore;

    private Integer assignmentsSubmitted;
    private Integer assignmentsTotal;
    private Integer averageAssignmentScore;

    private Integer codingSolved;
    private Integer codingTotal;

    private Integer recordingsWatched;
    private Integer recordingsTotal;

    private Instant lastActivityAt;

    /**
     * Areas the platform cannot measure yet, named explicitly.
     *
     * <p>Attendance is the live example: there is no attendance table, so it is reported
     * as unavailable rather than shown as a number nobody computed.</p>
     */
    private List<String> unavailableMetrics;
}
