package com.learntrix.edtech.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.learntrix.edtech.common.exception.CourseAccessDeniedException;
import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.progress.ActivityBreakdown;
import com.learntrix.edtech.dto.progress.BatchProgressResponse;
import com.learntrix.edtech.dto.progress.StudentProgressResponse;
import com.learntrix.edtech.entity.Assignment;
import com.learntrix.edtech.entity.AssignmentSubmission;
import com.learntrix.edtech.entity.Batch;
import com.learntrix.edtech.entity.CodingProblemCompletion;
import com.learntrix.edtech.entity.Quiz;
import com.learntrix.edtech.entity.QuizAttempt;
import com.learntrix.edtech.entity.RecordingWatchProgress;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.AssignmentRepository;
import com.learntrix.edtech.repository.AssignmentSubmissionRepository;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.ClassRecordingRepository;
import com.learntrix.edtech.repository.CodingProblemCompletionRepository;
import com.learntrix.edtech.repository.CodingProblemRepository;
import com.learntrix.edtech.repository.QuizAttemptRepository;
import com.learntrix.edtech.repository.QuizRepository;
import com.learntrix.edtech.repository.RecordingWatchProgressRepository;
import com.learntrix.edtech.repository.UserRepository;

/**
 * The single definition of "progress" in LearntriX.
 *
 * <p>Progress, analytics and the leaderboard all read from this class rather than each
 * computing their own version, because three competing definitions would disagree in front
 * of a student and nobody could say which was right.</p>
 *
 * <h2>How progress is calculated</h2>
 * <p>For each learning area the student is measured against what their course actually
 * contains: quizzes attempted / quizzes available, assignments submitted / assignments set,
 * coding problems completed / problems assigned, recordings watched / recordings published.
 * Overall progress is the mean of the areas that <em>apply</em>.</p>
 *
 * <p>An area with nothing in it is excluded, not counted as zero. A course with no quizzes
 * would otherwise cap every student at 75% forever, which would be a reporting bug that
 * looks like a student problem.</p>
 *
 * <h2>What is deliberately absent</h2>
 * <p>Attendance. There is no attendance table in this database, so it is reported through
 * {@code unavailableMetrics} rather than invented. Nothing here estimates, seeds or
 * back-fills a number.</p>
 */
@Service
public class LearningProgressService {

    /** Named so the UI can say why a metric is missing instead of rendering a silent zero. */
    private static final String ATTENDANCE_UNAVAILABLE =
            "Attendance is not tracked yet - no attendance records exist in this system.";

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final CodingProblemRepository codingProblemRepository;
    private final CodingProblemCompletionRepository codingCompletionRepository;
    private final ClassRecordingRepository recordingRepository;
    private final RecordingWatchProgressRepository watchRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final CourseAccessService courseAccessService;

    public LearningProgressService(
            QuizRepository quizRepository,
            QuizAttemptRepository quizAttemptRepository,
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository assignmentSubmissionRepository,
            CodingProblemRepository codingProblemRepository,
            CodingProblemCompletionRepository codingCompletionRepository,
            ClassRecordingRepository recordingRepository,
            RecordingWatchProgressRepository watchRepository,
            BatchRepository batchRepository,
            UserRepository userRepository,
            CourseAccessService courseAccessService) {
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
        this.codingProblemRepository = codingProblemRepository;
        this.codingCompletionRepository = codingCompletionRepository;
        this.recordingRepository = recordingRepository;
        this.watchRepository = watchRepository;
        this.batchRepository = batchRepository;
        this.userRepository = userRepository;
        this.courseAccessService = courseAccessService;
    }

    private boolean isAdmin() {
        return SecurityUtil.hasRole("ADMIN") || SecurityUtil.hasRole("SUPER_ADMIN");
    }

    // ==================================================================
    // Student's own progress
    // ==================================================================

    @Transactional(readOnly = true)
    public StudentProgressResponse getStudentProgress(UUID studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));
        return compute(student, courseAccessService.getAuthorizedCourseIdsForStudent(studentId));
    }

    /**
     * A teacher reading one student's progress.
     *
     * <p>Checked here rather than in the controller: the student id is a path variable, so
     * without this a teacher could walk other teachers' rosters by guessing ids.</p>
     */
    @Transactional(readOnly = true)
    public StudentProgressResponse getStudentProgressForTeacher(UUID studentId, UUID teacherId) {
        if (!isAdmin() && !courseAccessService.isStudentAssignedToTeacher(studentId, teacherId)) {
            throw new CourseAccessDeniedException(
                    "You are not authorized to view this student's progress");
        }
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));
        return compute(student, courseAccessService.getAuthorizedCourseIdsForStudent(studentId));
    }

    // ==================================================================
    // Teacher: batch rollups
    // ==================================================================

    @Transactional(readOnly = true)
    public List<BatchProgressResponse> getTeacherBatchProgress(UUID teacherId) {
        List<Batch> batches = isAdmin()
                ? batchRepository.findAll()
                : batchRepository.findAll().stream()
                        .filter(b -> b.getTeacher() != null && b.getTeacher().getId().equals(teacherId))
                        .collect(Collectors.toList());

        List<BatchProgressResponse> out = new ArrayList<>();
        for (Batch batch : batches) {
            List<User> students = batch.getStudents() == null
                    ? List.of() : new ArrayList<>(batch.getStudents());
            List<UUID> courseIds = batch.getCourse() == null
                    ? List.of() : List.of(batch.getCourse().getId());

            List<StudentProgressResponse> rows = students.stream()
                    .map(s -> compute(s, courseIds))
                    .collect(Collectors.toList());

            out.add(BatchProgressResponse.builder()
                    .batchId(batch.getId())
                    .batchName(batch.getName())
                    .courseId(batch.getCourse() != null ? batch.getCourse().getId() : null)
                    .courseTitle(batch.getCourse() != null ? batch.getCourse().getTitle() : null)
                    .studentCount(rows.size())
                    .averageProgress(mean(rows.stream().map(StudentProgressResponse::getOverallProgress).toList()))
                    .averageQuizScore(mean(rows.stream().map(StudentProgressResponse::getAverageQuizScore).toList()))
                    .averageAssignmentScore(mean(rows.stream().map(StudentProgressResponse::getAverageAssignmentScore).toList()))
                    .students(rows)
                    .unavailableMetrics(List.of(ATTENDANCE_UNAVAILABLE))
                    .build());
        }
        return out;
    }

    // ==================================================================
    // The calculation
    // ==================================================================

    /**
     * Measures one student against the content of the given courses.
     *
     * <p>Only content the student can actually reach counts towards the denominator, so a
     * draft quiz or another cohort's assignment never makes someone look behind.</p>
     */
    private StudentProgressResponse compute(User student, List<UUID> courseIds) {
        UUID studentId = student.getId();
        List<ActivityBreakdown> areas = new ArrayList<>();
        Instant lastActivity = null;

        // ---------- quizzes ----------
        List<Quiz> quizzes = courseIds.isEmpty() ? List.of()
                : quizRepository.findAccessiblePublishedQuizzes(studentId, courseIds);
        List<QuizAttempt> attempts = quizAttemptRepository.findByStudentId(studentId);
        Set<UUID> quizIds = quizzes.stream().map(Quiz::getId).collect(Collectors.toSet());
        List<QuizAttempt> relevantAttempts = attempts.stream()
                .filter(a -> a.getQuiz() != null && quizIds.contains(a.getQuiz().getId()))
                .toList();
        Integer avgQuiz = mean(relevantAttempts.stream().map(QuizAttempt::getScore).toList());
        areas.add(area("Quizzes", relevantAttempts.size(), quizzes.size(), avgQuiz));
        for (QuizAttempt a : relevantAttempts) {
            lastActivity = later(lastActivity, a.getSubmittedAt());
        }

        // ---------- assignments ----------
        List<Assignment> assignments = courseIds.isEmpty() ? List.of()
                : assignmentRepository.findAccessiblePublished(studentId, courseIds);
        Set<UUID> assignmentIds = assignments.stream().map(Assignment::getId).collect(Collectors.toSet());
        List<AssignmentSubmission> subs = assignmentSubmissionRepository.findByStudentId(studentId).stream()
                .filter(s -> s.getAssignment() != null && assignmentIds.contains(s.getAssignment().getId()))
                .toList();
        // Percentage of that assignment's own marks, so a 50-point and a 100-point piece
        // of work contribute on the same scale.
        List<Integer> assignmentPercents = subs.stream()
                .filter(s -> s.getGrade() != null)
                .map(s -> {
                    int max = s.getAssignment().getPoints() == null ? 100 : s.getAssignment().getPoints();
                    return max == 0 ? 0 : (int) Math.round(s.getGrade() * 100.0 / max);
                })
                .toList();
        Integer avgAssignment = mean(assignmentPercents);
        areas.add(area("Assignments", subs.size(), assignments.size(), avgAssignment));
        for (AssignmentSubmission s : subs) {
            lastActivity = later(lastActivity, s.getSubmittedAt());
        }

        // ---------- coding ----------
        int codingTotal = courseIds.isEmpty() ? 0
                : codingProblemRepository.findAccessiblePublished(studentId, courseIds).size();
        List<CodingProblemCompletion> completions = codingCompletionRepository.findByStudentId(studentId);
        int codingSolved = (int) completions.stream().filter(CodingProblemCompletion::isCompleted).count();
        areas.add(area("Coding practice", Math.min(codingSolved, codingTotal), codingTotal, null));
        for (CodingProblemCompletion c : completions) {
            lastActivity = later(lastActivity, c.getCompletedAt());
        }

        // ---------- recordings ----------
        int recordingsTotal = courseIds.isEmpty() ? 0
                : (int) recordingRepository.findAll().stream()
                        .filter(r -> r.getCourse() != null && courseIds.contains(r.getCourse().getId()))
                        .count();
        List<RecordingWatchProgress> watched = watchRepository.findByStudentId(studentId);
        int recordingsDone = (int) watched.stream().filter(RecordingWatchProgress::isCompleted).count();
        areas.add(area("Recorded classes", Math.min(recordingsDone, recordingsTotal), recordingsTotal, null));
        for (RecordingWatchProgress w : watched) {
            lastActivity = later(lastActivity, w.getLastWatchedAt());
        }

        Integer overall = mean(areas.stream()
                .filter(ActivityBreakdown::isApplicable)
                .map(ActivityBreakdown::getPercent)
                .toList());

        return StudentProgressResponse.builder()
                .studentId(studentId)
                .studentName(student.getName())
                .studentEmail(student.getEmail())
                .overallProgress(overall == null ? 0 : overall)
                .areas(areas)
                .quizzesAttempted(relevantAttempts.size())
                .quizzesTotal(quizzes.size())
                .averageQuizScore(avgQuiz)
                .assignmentsSubmitted(subs.size())
                .assignmentsTotal(assignments.size())
                .averageAssignmentScore(avgAssignment)
                .codingSolved(codingSolved)
                .codingTotal(codingTotal)
                .recordingsWatched(recordingsDone)
                .recordingsTotal(recordingsTotal)
                .lastActivityAt(lastActivity)
                .unavailableMetrics(List.of(ATTENDANCE_UNAVAILABLE))
                .build();
    }

    /** An area with a zero denominator is not applicable, rather than 0% complete. */
    private ActivityBreakdown area(String name, int done, int total, Integer avgScore) {
        boolean applicable = total > 0;
        return ActivityBreakdown.builder()
                .area(name)
                .applicable(applicable)
                .completed(done)
                .total(total)
                .percent(applicable ? (int) Math.round(done * 100.0 / total) : null)
                .averageScore(avgScore)
                .build();
    }

    /** Null-skipping mean; returns null when there is nothing to average. */
    static Integer mean(List<Integer> values) {
        List<Integer> present = values.stream().filter(java.util.Objects::nonNull).toList();
        if (present.isEmpty()) return null;
        return (int) Math.round(present.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private Instant later(Instant current, Instant candidate) {
        if (candidate == null) return current;
        return current == null || candidate.isAfter(current) ? candidate : current;
    }

    /** Distinct problems a student has marked done - reused by the leaderboard. */
    @Transactional(readOnly = true)
    public int countCodingSolved(UUID studentId) {
        return (int) codingCompletionRepository.findByStudentId(studentId).stream()
                .filter(CodingProblemCompletion::isCompleted)
                .map(c -> c.getProblem().getId())
                .collect(Collectors.toCollection(HashSet::new))
                .size();
    }

    /** Mean quiz score across every attempt, or null when the student has attempted none. */
    @Transactional(readOnly = true)
    public Integer averageQuizScore(UUID studentId) {
        return mean(quizAttemptRepository.findByStudentId(studentId).stream()
                .map(QuizAttempt::getScore).toList());
    }
}
