package com.learntrix.edtech.dto.progress;

import lombok.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Analytics for one student (own view) or one teacher's batches.
 *
 * <p>Every number is computed from stored records. Where a metric cannot be computed it is
 * null and named in {@code unavailableMetrics}, never filled with a placeholder.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalyticsResponse {
    /** STUDENT or TEACHER. */
    private String scope;
    private UUID subjectId;
    private String subjectName;

    private Integer studentCount;
    private Integer averageProgress;
    private Integer averageQuizScore;
    private Integer averageAssignmentScore;

    private Integer quizzesAttempted;
    private Integer assignmentsSubmitted;
    private Integer codingSolved;
    private Integer recordingsWatched;

    /** Assignment submission rate as a percentage of expected submissions. */
    private Integer assignmentSubmissionRate;
    private Integer quizParticipationRate;

    /** Buckets: "90-100", "75-89", "60-74", "below-60" -> student count. */
    private Map<String, Integer> performanceDistribution;

    private List<AtRiskStudentResponse> atRiskStudents;
    private List<String> unavailableMetrics;
}
