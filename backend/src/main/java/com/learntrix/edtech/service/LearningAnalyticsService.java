package com.learntrix.edtech.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.learntrix.edtech.dto.progress.AnalyticsResponse;
import com.learntrix.edtech.dto.progress.AtRiskStudentResponse;
import com.learntrix.edtech.dto.progress.BatchProgressResponse;
import com.learntrix.edtech.dto.progress.StudentProgressResponse;

/**
 * Analytics, derived entirely from {@link LearningProgressService}.
 *
 * <p>It deliberately owns no calculation of its own. If analytics computed averages
 * separately, the Analytics page and the Progress page could show different numbers for
 * the same student and there would be no way to say which was correct.</p>
 */
@Service
public class LearningAnalyticsService {

    /**
     * At-risk thresholds.
     *
     * <p>Configurable and documented, per the requirement that no student is labelled by
     * hidden logic. These are the only rules - there is no model, no prediction and no
     * weighting a teacher cannot see.</p>
     */
    private final int progressThreshold;
    private final int quizThreshold;
    private final int missedAssignmentThreshold;

    private final LearningProgressService progressService;

    public LearningAnalyticsService(
            LearningProgressService progressService,
            @Value("${analytics.at-risk.min-progress-percent:50}") int progressThreshold,
            @Value("${analytics.at-risk.min-quiz-score:50}") int quizThreshold,
            @Value("${analytics.at-risk.max-missed-assignments:2}") int missedAssignmentThreshold) {
        this.progressService = progressService;
        this.progressThreshold = progressThreshold;
        this.quizThreshold = quizThreshold;
        this.missedAssignmentThreshold = missedAssignmentThreshold;
    }

    // ==================================================================
    // Student: their own analytics
    // ==================================================================

    @Transactional(readOnly = true)
    public AnalyticsResponse getStudentAnalytics(UUID studentId) {
        StudentProgressResponse p = progressService.getStudentProgress(studentId);

        return AnalyticsResponse.builder()
                .scope("STUDENT")
                .subjectId(studentId)
                .subjectName(p.getStudentName())
                .averageProgress(p.getOverallProgress())
                .averageQuizScore(p.getAverageQuizScore())
                .averageAssignmentScore(p.getAverageAssignmentScore())
                .quizzesAttempted(p.getQuizzesAttempted())
                .assignmentsSubmitted(p.getAssignmentsSubmitted())
                .codingSolved(p.getCodingSolved())
                .recordingsWatched(p.getRecordingsWatched())
                .assignmentSubmissionRate(rate(p.getAssignmentsSubmitted(), p.getAssignmentsTotal()))
                .quizParticipationRate(rate(p.getQuizzesAttempted(), p.getQuizzesTotal()))
                .unavailableMetrics(p.getUnavailableMetrics())
                .build();
    }

    // ==================================================================
    // Teacher: aggregated across their batches
    // ==================================================================

    @Transactional(readOnly = true)
    public AnalyticsResponse getTeacherAnalytics(UUID teacherId) {
        List<BatchProgressResponse> batches = progressService.getTeacherBatchProgress(teacherId);

        List<StudentProgressResponse> students = batches.stream()
                .flatMap(b -> b.getStudents() == null ? List.<StudentProgressResponse>of().stream()
                        : b.getStudents().stream())
                .toList();

        int expectedAssignments = students.stream()
                .mapToInt(s -> s.getAssignmentsTotal() == null ? 0 : s.getAssignmentsTotal()).sum();
        int actualAssignments = students.stream()
                .mapToInt(s -> s.getAssignmentsSubmitted() == null ? 0 : s.getAssignmentsSubmitted()).sum();
        int expectedQuizzes = students.stream()
                .mapToInt(s -> s.getQuizzesTotal() == null ? 0 : s.getQuizzesTotal()).sum();
        int actualQuizzes = students.stream()
                .mapToInt(s -> s.getQuizzesAttempted() == null ? 0 : s.getQuizzesAttempted()).sum();

        return AnalyticsResponse.builder()
                .scope("TEACHER")
                .subjectId(teacherId)
                .studentCount(students.size())
                .averageProgress(LearningProgressService.mean(
                        students.stream().map(StudentProgressResponse::getOverallProgress).toList()))
                .averageQuizScore(LearningProgressService.mean(
                        students.stream().map(StudentProgressResponse::getAverageQuizScore).toList()))
                .averageAssignmentScore(LearningProgressService.mean(
                        students.stream().map(StudentProgressResponse::getAverageAssignmentScore).toList()))
                .quizzesAttempted(actualQuizzes)
                .assignmentsSubmitted(actualAssignments)
                .codingSolved(students.stream()
                        .mapToInt(s -> s.getCodingSolved() == null ? 0 : s.getCodingSolved()).sum())
                .recordingsWatched(students.stream()
                        .mapToInt(s -> s.getRecordingsWatched() == null ? 0 : s.getRecordingsWatched()).sum())
                .assignmentSubmissionRate(rate(actualAssignments, expectedAssignments))
                .quizParticipationRate(rate(actualQuizzes, expectedQuizzes))
                .performanceDistribution(distribution(students))
                .atRiskStudents(atRisk(students))
                .unavailableMetrics(List.of(
                        "Attendance is not tracked yet - no attendance records exist in this system."))
                .build();
    }

    // ==================================================================
    // Explainable rules
    // ==================================================================

    /**
     * Flags students against stated thresholds and returns the reason for each.
     *
     * <p>A student with no activity at all is not flagged for a low quiz average - having
     * attempted nothing is reported as a missed-work reason instead, which is the accurate
     * statement.</p>
     */
    private List<AtRiskStudentResponse> atRisk(List<StudentProgressResponse> students) {
        List<AtRiskStudentResponse> flagged = new ArrayList<>();

        for (StudentProgressResponse s : students) {
            List<String> reasons = new ArrayList<>();

            int progress = s.getOverallProgress() == null ? 0 : s.getOverallProgress();
            if (progress < progressThreshold) {
                reasons.add("Course progress is " + progress + "%, below the "
                        + progressThreshold + "% threshold");
            }

            // Only meaningful once they have actually attempted something.
            if (s.getAverageQuizScore() != null && s.getAverageQuizScore() < quizThreshold) {
                reasons.add("Average quiz score is " + s.getAverageQuizScore() + "%, below the "
                        + quizThreshold + "% threshold");
            }

            int total = s.getAssignmentsTotal() == null ? 0 : s.getAssignmentsTotal();
            int done = s.getAssignmentsSubmitted() == null ? 0 : s.getAssignmentsSubmitted();
            int missed = Math.max(0, total - done);
            if (missed > missedAssignmentThreshold) {
                reasons.add(missed + " assignment(s) not submitted");
            }

            if (!reasons.isEmpty()) {
                flagged.add(AtRiskStudentResponse.builder()
                        .studentId(s.getStudentId())
                        .studentName(s.getStudentName())
                        .overallProgress(progress)
                        .averageQuizScore(s.getAverageQuizScore())
                        .missedAssignments(missed)
                        .reasons(reasons)
                        .build());
            }
        }
        return flagged;
    }

    /** Buckets students by overall progress. Order is preserved for the UI. */
    private Map<String, Integer> distribution(List<StudentProgressResponse> students) {
        Map<String, Integer> buckets = new LinkedHashMap<>();
        buckets.put("90-100", 0);
        buckets.put("75-89", 0);
        buckets.put("60-74", 0);
        buckets.put("below-60", 0);

        for (StudentProgressResponse s : students) {
            int p = s.getOverallProgress() == null ? 0 : s.getOverallProgress();
            String key = p >= 90 ? "90-100" : p >= 75 ? "75-89" : p >= 60 ? "60-74" : "below-60";
            buckets.merge(key, 1, Integer::sum);
        }
        return buckets;
    }

    /** Null when there is nothing expected, rather than a misleading 0% or 100%. */
    private Integer rate(Integer actual, Integer expected) {
        if (expected == null || expected == 0) return null;
        int a = actual == null ? 0 : actual;
        return (int) Math.round(a * 100.0 / expected);
    }
}
