package com.learntrix.edtech.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.learntrix.edtech.common.exception.BusinessException;
import com.learntrix.edtech.common.exception.CourseAccessDeniedException;
import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.coding.*;
import com.learntrix.edtech.entity.Batch;
import com.learntrix.edtech.entity.CodingProblem;
import com.learntrix.edtech.entity.CodingSubmission;
import com.learntrix.edtech.entity.CodingTestCase;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.Enrollment;
import com.learntrix.edtech.entity.Module;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.execution.CodeExecutionClient;
import com.learntrix.edtech.execution.ExecutionResult;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.CodingProblemRepository;
import com.learntrix.edtech.repository.CodingSubmissionRepository;
import com.learntrix.edtech.repository.CodingTestCaseRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.EnrollmentRepository;
import com.learntrix.edtech.repository.ModuleRepository;
import com.learntrix.edtech.repository.UserRepository;

/**
 * Coding practice.
 *
 * <p>Authorization reuses {@link CourseAccessService} exactly as quizzes do; there is no
 * second access model here. The one rule unique to this feature is the hidden-test
 * boundary: a hidden case's input and expected output must never reach a student, so
 * student-facing mapping filters to samples before mapping, and Submit reports aggregate
 * counts only.</p>
 */
@Service
@Transactional
public class CodingProblemService {

    private static final Set<String> SOLVED = Set.of("ACCEPTED");

    private final CodingProblemRepository problemRepository;
    private final CodingTestCaseRepository testCaseRepository;
    private final CodingSubmissionRepository submissionRepository;
    private final CourseRepository courseRepository;
    private final BatchRepository batchRepository;
    private final ModuleRepository moduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseAccessService courseAccessService;
    private final CodeExecutionClient executionClient;

    public CodingProblemService(
            CodingProblemRepository problemRepository,
            CodingTestCaseRepository testCaseRepository,
            CodingSubmissionRepository submissionRepository,
            CourseRepository courseRepository,
            BatchRepository batchRepository,
            ModuleRepository moduleRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            CourseAccessService courseAccessService,
            CodeExecutionClient executionClient) {
        this.problemRepository = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.submissionRepository = submissionRepository;
        this.courseRepository = courseRepository;
        this.batchRepository = batchRepository;
        this.moduleRepository = moduleRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseAccessService = courseAccessService;
        this.executionClient = executionClient;
    }

    // ==================================================================
    // Authorization
    // ==================================================================

    private boolean isAdmin() {
        return SecurityUtil.hasRole("ADMIN") || SecurityUtil.hasRole("SUPER_ADMIN");
    }

    private void verifyTeacherCanManage(CodingProblem problem, UUID teacherId) {
        if (isAdmin()) return;
        courseAccessService.verifyTeacherCanManageCourse(teacherId, problem.getCourse().getId());
        Batch batch = problem.getBatch();
        if (batch != null
                && (batch.getTeacher() == null || !batch.getTeacher().getId().equals(teacherId))) {
            throw new CourseAccessDeniedException(
                    "You are not authorized to manage coding problems for that batch");
        }
    }

    private Batch resolveBatch(UUID batchId, UUID courseId, UUID teacherId) {
        if (batchId == null) return null;
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId));
        if (batch.getCourse() == null || !batch.getCourse().getId().equals(courseId)) {
            throw new BusinessException("BATCH_COURSE_MISMATCH",
                    "That batch does not belong to the selected course.", HttpStatus.BAD_REQUEST);
        }
        if (!isAdmin()
                && (batch.getTeacher() == null || !batch.getTeacher().getId().equals(teacherId))) {
            throw new CourseAccessDeniedException(
                    "You are not authorized to create problems for that batch");
        }
        return batch;
    }

    /** Optional curriculum placement; must sit inside the same course. */
    private Module resolveModule(UUID moduleId, UUID courseId) {
        if (moduleId == null) return null;
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));
        if (module.getCourse() == null || !module.getCourse().getId().equals(courseId)) {
            throw new BusinessException("MODULE_COURSE_MISMATCH",
                    "That module does not belong to the selected course.", HttpStatus.BAD_REQUEST);
        }
        return module;
    }

    /** Published + enrolled + (for a batch problem) a member of that batch. */
    private void verifyStudentCanAccess(CodingProblem problem, UUID studentId) {
        if (isAdmin()) return;
        if (!"PUBLISHED".equalsIgnoreCase(problem.getStatus())) {
            throw new CourseAccessDeniedException("This problem is not available.");
        }
        courseAccessService.verifyStudentCanAccessCourse(studentId, problem.getCourse().getId());
        Batch batch = problem.getBatch();
        if (batch != null) {
            boolean member = batch.getStudents() != null
                    && batch.getStudents().stream().anyMatch(u -> u.getId().equals(studentId));
            if (!member) {
                throw new CourseAccessDeniedException("This problem is not available.");
            }
        }
    }

    // ==================================================================
    // Teacher: problem management
    // ==================================================================

    public CodingProblemResponse createProblem(CreateCodingProblemRequest request, UUID teacherId) {
        if (!isAdmin()) {
            courseAccessService.verifyTeacherCanManageCourse(teacherId, request.getCourseId());
        }
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));

        CodingProblem p = new CodingProblem();
        p.setCourse(course);
        p.setTeacher(teacher);
        p.setBatch(resolveBatch(request.getBatchId(), course.getId(), teacherId));
        p.setModule(resolveModule(request.getModuleId(), course.getId()));
        p.setTitle(request.getTitle());
        p.setSlug(slugify(request.getTitle()));
        p.setDescription(request.getDescription());
        p.setDifficulty(request.getDifficulty() != null ? request.getDifficulty().toUpperCase() : "EASY");
        p.setConstraintsText(request.getConstraintsText());
        p.setStarterCode(request.getStarterCode());
        p.setLanguage(request.getLanguage() != null ? request.getLanguage() : "java");
        p.setTimeLimitMs(request.getTimeLimitMs() != null ? request.getTimeLimitMs() : 2000);
        p.setMemoryLimitMb(request.getMemoryLimitMb() != null ? request.getMemoryLimitMb() : 128);
        // DRAFT by default: a problem with no test cases must not reach students.
        p.setStatus("PUBLISHED".equalsIgnoreCase(request.getStatus()) ? "PUBLISHED" : "DRAFT");
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());

        return mapProblem(problemRepository.save(p), null, false);
    }

    @Transactional(readOnly = true)
    public List<CodingProblemResponse> getTeacherProblems(UUID teacherId) {
        List<UUID> courseIds = isAdmin()
                ? courseRepository.findAll().stream().map(Course::getId).collect(Collectors.toList())
                : courseAccessService.getAuthorizedCourseIdsForTeacher(teacherId);
        if (courseIds.isEmpty()) return List.of();
        return problemRepository.findByCourseIdIn(courseIds).stream()
                .map(p -> mapProblem(p, null, false))
                .collect(Collectors.toList());
    }

    public TeacherCodingTestCaseResponse addTestCase(
            UUID problemId, CodingTestCaseRequest request, UUID teacherId) {
        CodingProblem problem = loadProblem(problemId);
        verifyTeacherCanManage(problem, teacherId);

        CodingTestCase tc = new CodingTestCase();
        tc.setProblem(problem);
        tc.setInputData(request.getInputData());
        tc.setExpectedOutput(request.getExpectedOutput());
        tc.setSample(request.isSample());
        tc.setSequenceNumber(request.getSequenceNumber() != null ? request.getSequenceNumber()
                : testCaseRepository.findByProblemIdOrderBySequenceNumberAsc(problemId).size());
        tc.setPoints(request.getPoints() != null ? request.getPoints() : 1);
        return mapTeacherTestCase(testCaseRepository.save(tc));
    }

    /** Teacher view of every case, hidden ones included. */
    @Transactional(readOnly = true)
    public List<TeacherCodingTestCaseResponse> getTestCasesForTeacher(UUID problemId, UUID teacherId) {
        CodingProblem problem = loadProblem(problemId);
        verifyTeacherCanManage(problem, teacherId);
        return testCaseRepository.findByProblemIdOrderBySequenceNumberAsc(problemId).stream()
                .map(this::mapTeacherTestCase)
                .collect(Collectors.toList());
    }

    public void deleteTestCase(UUID problemId, UUID testCaseId, UUID teacherId) {
        CodingProblem problem = loadProblem(problemId);
        verifyTeacherCanManage(problem, teacherId);
        CodingTestCase tc = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("CodingTestCase", "id", testCaseId));
        if (tc.getProblem() == null || !tc.getProblem().getId().equals(problemId)) {
            throw new BusinessException("TEST_CASE_NOT_IN_PROBLEM",
                    "That test case does not belong to this problem.", HttpStatus.BAD_REQUEST);
        }
        testCaseRepository.delete(tc);
    }

    /**
     * Publishing is where a problem is last checked before students depend on it.
     *
     * <p>A problem with no test cases cannot be judged at all, and one with a blank expected
     * output would mark every submission wrong, so both are refused here rather than
     * discovered by a student mid-attempt.</p>
     */
    public CodingProblemResponse setPublished(UUID problemId, boolean publish, UUID teacherId) {
        CodingProblem problem = loadProblem(problemId);
        verifyTeacherCanManage(problem, teacherId);
        if (publish) {
            validateProblemIsPublishable(problem);
        }
        problem.setStatus(publish ? "PUBLISHED" : "DRAFT");
        problem.setUpdatedAt(Instant.now());
        return mapProblem(problemRepository.save(problem), null, false);
    }

    private void validateProblemIsPublishable(CodingProblem problem) {
        if (problem.getTitle() == null || problem.getTitle().isBlank()) {
            throw new BusinessException("INVALID_PROBLEM",
                    "The problem needs a title before publishing.", HttpStatus.CONFLICT);
        }
        if (problem.getDescription() == null || problem.getDescription().isBlank()) {
            throw new BusinessException("INVALID_PROBLEM",
                    "The problem needs a description before publishing.", HttpStatus.CONFLICT);
        }

        List<CodingTestCase> cases =
                testCaseRepository.findByProblemIdOrderBySequenceNumberAsc(problem.getId());
        if (cases.isEmpty()) {
            throw new BusinessException("PROBLEM_HAS_NO_TEST_CASES",
                    "Add at least one test case before publishing.", HttpStatus.CONFLICT);
        }
        for (int i = 0; i < cases.size(); i++) {
            CodingTestCase tc = cases.get(i);
            if (tc.getExpectedOutput() == null || tc.getExpectedOutput().isBlank()) {
                throw new BusinessException("INVALID_TEST_CASE",
                        "Test case " + (i + 1) + " has no expected output.", HttpStatus.CONFLICT);
            }
        }
    }

    /** Edit a problem's wording and limits. Course and batch are deliberately not moved here. */
    public CodingProblemResponse updateProblem(
            UUID problemId, CreateCodingProblemRequest request, UUID teacherId) {
        CodingProblem problem = loadProblem(problemId);
        verifyTeacherCanManage(problem, teacherId);

        if (request.getTitle() != null) problem.setTitle(request.getTitle());
        if (request.getDescription() != null) problem.setDescription(request.getDescription());
        if (request.getDifficulty() != null) problem.setDifficulty(request.getDifficulty().toUpperCase());
        if (request.getConstraintsText() != null) problem.setConstraintsText(request.getConstraintsText());
        if (request.getStarterCode() != null) problem.setStarterCode(request.getStarterCode());
        if (request.getLanguage() != null) problem.setLanguage(request.getLanguage());
        if (request.getTimeLimitMs() != null) problem.setTimeLimitMs(request.getTimeLimitMs());
        if (request.getMemoryLimitMb() != null) problem.setMemoryLimitMb(request.getMemoryLimitMb());
        problem.setUpdatedAt(Instant.now());

        return mapProblem(problemRepository.save(problem), null, false);
    }

    /** Edit one test case, including flipping it between sample and hidden. */
    public TeacherCodingTestCaseResponse updateTestCase(
            UUID problemId, UUID testCaseId, CodingTestCaseRequest request, UUID teacherId) {
        CodingProblem problem = loadProblem(problemId);
        verifyTeacherCanManage(problem, teacherId);

        CodingTestCase tc = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("CodingTestCase", "id", testCaseId));
        if (tc.getProblem() == null || !tc.getProblem().getId().equals(problemId)) {
            throw new BusinessException("TEST_CASE_NOT_IN_PROBLEM",
                    "That test case does not belong to this problem.", HttpStatus.BAD_REQUEST);
        }

        tc.setInputData(request.getInputData());
        tc.setExpectedOutput(request.getExpectedOutput());
        tc.setSample(request.isSample());
        if (request.getSequenceNumber() != null) tc.setSequenceNumber(request.getSequenceNumber());
        if (request.getPoints() != null) tc.setPoints(request.getPoints());

        return mapTeacherTestCase(testCaseRepository.save(tc));
    }

    // ==================================================================
    // Student: browsing and solving
    // ==================================================================

    @Transactional(readOnly = true)
    public List<CodingProblemResponse> getStudentProblems(UUID studentId) {
        List<UUID> courseIds = courseAccessService.getAuthorizedCourseIdsForStudent(studentId);
        if (courseIds.isEmpty()) return List.of();
        return problemRepository.findAccessiblePublished(studentId, courseIds).stream()
                .map(p -> mapProblem(p, studentId, false))
                .collect(Collectors.toList());
    }

    /** Problem detail with SAMPLE cases only. */
    @Transactional(readOnly = true)
    public CodingProblemResponse getStudentProblem(UUID problemId, UUID studentId) {
        CodingProblem problem = loadProblem(problemId);
        verifyStudentCanAccess(problem, studentId);
        return mapProblem(problem, studentId, true);
    }

    /**
     * Run Code: sample cases only, nothing persisted.
     *
     * <p>Input and expected output are returned here because the student can already see
     * them on the problem page. Hidden cases are not executed and not mentioned.</p>
     */
    public RunCodeResponse runCode(UUID problemId, UUID studentId, RunCodeRequest request) {
        CodingProblem problem = loadProblem(problemId);
        verifyStudentCanAccess(problem, studentId);

        List<CodingTestCase> samples =
                testCaseRepository.findByProblemIdAndSampleTrueOrderBySequenceNumberAsc(problemId);
        if (samples.isEmpty()) {
            return RunCodeResponse.builder()
                    .status("ERROR")
                    .message("This problem has no sample test cases to run against.")
                    .passedCount(0).totalCount(0).results(List.of())
                    .build();
        }

        String language = request.getLanguage() != null ? request.getLanguage() : problem.getLanguage();
        List<TestCaseResultResponse> results = new ArrayList<>();
        int passed = 0;
        String overall = "ACCEPTED";
        String message = null;

        for (CodingTestCase tc : samples) {
            ExecutionResult exec = executionClient.execute(
                    language, request.getSourceCode(), tc.getInputData(),
                    problem.getTimeLimitMs(), problem.getMemoryLimitMb());

            boolean ok = exec.isSuccess() && outputsMatch(exec.getStdout(), tc.getExpectedOutput());
            if (ok) passed++;

            String caseStatus = exec.isSuccess()
                    ? (ok ? "PASSED" : "WRONG_ANSWER")
                    : exec.getStatus().name();
            if (!ok && "ACCEPTED".equals(overall)) {
                overall = exec.isSuccess() ? "WRONG_ANSWER" : exec.getStatus().name();
                message = exec.getMessage();
            }

            results.add(TestCaseResultResponse.builder()
                    .sequenceNumber(tc.getSequenceNumber())
                    .input(tc.getInputData())
                    .expectedOutput(tc.getExpectedOutput())
                    .actualOutput(exec.getStdout())
                    .passed(ok)
                    .status(caseStatus)
                    .message(exec.getMessage())
                    .runtimeMs(exec.getRuntimeMs())
                    .build());

            // A compile error is the same for every case; stop rather than repeat it.
            if (exec.getStatus() == ExecutionResult.Status.COMPILE_ERROR) break;
        }

        return RunCodeResponse.builder()
                .status(overall)
                .message(message)
                .passedCount(passed)
                .totalCount(samples.size())
                .results(results)
                .build();
    }

    /**
     * Submit: every test case, hidden included, then persist the verdict.
     *
     * <p>The response carries counts and a verdict only. No hidden input, no hidden expected
     * output, and no per-case detail that would let a student reconstruct them.</p>
     */
    public CodingSubmissionResponse submit(UUID problemId, UUID studentId, RunCodeRequest request) {
        CodingProblem problem = loadProblem(problemId);
        verifyStudentCanAccess(problem, studentId);

        List<CodingTestCase> cases = testCaseRepository.findByProblemIdOrderBySequenceNumberAsc(problemId);
        if (cases.isEmpty()) {
            throw new BusinessException("PROBLEM_HAS_NO_TEST_CASES",
                    "This problem cannot be judged yet.", HttpStatus.CONFLICT);
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));
        String language = request.getLanguage() != null ? request.getLanguage() : problem.getLanguage();

        int passed = 0;
        String verdict = "ACCEPTED";
        String message = null;
        Integer slowest = null;

        for (CodingTestCase tc : cases) {
            ExecutionResult exec = executionClient.execute(
                    language, request.getSourceCode(), tc.getInputData(),
                    problem.getTimeLimitMs(), problem.getMemoryLimitMb());

            if (exec.getRuntimeMs() != null
                    && (slowest == null || exec.getRuntimeMs() > slowest)) {
                slowest = exec.getRuntimeMs();
            }

            if (exec.isSuccess() && outputsMatch(exec.getStdout(), tc.getExpectedOutput())) {
                passed++;
                continue;
            }

            // First failure decides the verdict.
            if ("ACCEPTED".equals(verdict)) {
                verdict = exec.isSuccess() ? "WRONG_ANSWER" : exec.getStatus().name();
                message = exec.getMessage();
            }
            if (exec.getStatus() == ExecutionResult.Status.COMPILE_ERROR) break;
        }

        CodingSubmission submission = new CodingSubmission();
        submission.setProblem(problem);
        submission.setStudent(student);
        submission.setLanguage(language);
        submission.setSourceCode(request.getSourceCode());
        submission.setStatus(verdict);
        submission.setPassedCount(passed);
        submission.setTotalCount(cases.size());
        submission.setRuntimeMs(slowest);
        submission.setMessage(message);
        submission.setSubmittedAt(Instant.now());

        return mapSubmission(submissionRepository.save(submission), true);
    }

    /** The caller's own submissions, scoped by the authenticated id. */
    @Transactional(readOnly = true)
    public List<CodingSubmissionResponse> getMySubmissions(UUID studentId) {
        return submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
                .map(s -> mapSubmission(s, false))
                .collect(Collectors.toList());
    }

    // ==================================================================
    // Performance
    // ==================================================================

    /**
     * Coding performance for one student. Callable by the student themselves, or by a
     * teacher the student is actually assigned to - checked here, not in the UI.
     */
    @Transactional(readOnly = true)
    public CodingPerformanceResponse getPerformance(UUID studentId, UUID requesterId) {
        if (!isAdmin() && !requesterId.equals(studentId)
                && !courseAccessService.isStudentAssignedToTeacher(studentId, requesterId)) {
            throw new CourseAccessDeniedException(
                    "You are not authorized to view this student's coding performance");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        List<CodingSubmission> subs = submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId);

        Set<UUID> attempted = new HashSet<>();
        Set<UUID> solved = new HashSet<>();
        int accepted = 0, wrong = 0, compileErr = 0, runtimeErr = 0, tle = 0;

        for (CodingSubmission s : subs) {
            UUID pid = s.getProblem().getId();
            attempted.add(pid);
            switch (s.getStatus()) {
                case "ACCEPTED" -> { accepted++; solved.add(pid); }
                case "WRONG_ANSWER" -> wrong++;
                case "COMPILE_ERROR" -> compileErr++;
                case "RUNTIME_ERROR" -> runtimeErr++;
                case "TIME_LIMIT_EXCEEDED" -> tle++;
                default -> { /* ERROR / MEMORY_LIMIT_EXCEEDED counted only in the total */ }
            }
        }

        List<UUID> courseIds = courseAccessService.getAuthorizedCourseIdsForStudent(studentId);
        int available = courseIds.isEmpty() ? 0
                : problemRepository.findAccessiblePublished(studentId, courseIds).size();

        return CodingPerformanceResponse.builder()
                .studentId(studentId)
                .studentName(student.getName())
                .problemsAvailable(available)
                .problemsAttempted(attempted.size())
                .problemsSolved(solved.size())
                .totalSubmissions(subs.size())
                .acceptedSubmissions(accepted)
                .wrongAnswers(wrong)
                .compileErrors(compileErr)
                .runtimeErrors(runtimeErr)
                .timeLimitExceeded(tle)
                .acceptanceRate(subs.isEmpty() ? 0d
                        : Math.round((accepted * 1000.0) / subs.size()) / 10.0)
                .lastSubmissionAt(subs.isEmpty() ? null : subs.get(0).getSubmittedAt())
                .build();
    }

    /**
     * Per-problem performance for a teacher.
     *
     * <p>The counts here are counts of students, not of submissions. Collapsing every
     * student's submissions to one row first is what makes that true by construction:
     * Raju submitting wrong, wrong, accepted, accepted is one attempted student and one
     * solved student, not four of each.</p>
     */
    @Transactional(readOnly = true)
    public CodingProblemPerformanceResponse getProblemPerformance(UUID problemId, UUID teacherId) {
        CodingProblem problem = loadProblem(problemId);
        verifyTeacherCanManage(problem, teacherId);

        // Scope = exactly the students who can see this problem, so "not attempted" is
        // meaningful rather than listing the whole school.
        List<User> scope = studentsInScope(problem);

        List<CodingSubmission> all = submissionRepository.findByProblemIdOrderBySubmittedAtDesc(problemId);
        Map<UUID, List<CodingSubmission>> byStudent = all.stream()
                .collect(Collectors.groupingBy(s -> s.getStudent().getId()));

        List<CodingStudentStatusResponse> rows = new ArrayList<>();
        int attempted = 0;
        int solved = 0;

        for (User student : scope) {
            List<CodingSubmission> mine = byStudent.getOrDefault(student.getId(), List.of());

            if (mine.isEmpty()) {
                rows.add(CodingStudentStatusResponse.builder()
                        .studentId(student.getId())
                        .studentName(student.getName())
                        .studentEmail(student.getEmail())
                        .status("NOT_ATTEMPTED")
                        .submissionCount(0)
                        .build());
                continue;
            }

            attempted++;
            boolean isSolved = mine.stream().anyMatch(s -> SOLVED.contains(s.getStatus()));
            if (isSolved) solved++;

            CodingSubmission best = mine.stream()
                    .max(Comparator.comparingDouble(CodingProblemService::ratio))
                    .orElse(mine.get(0));

            rows.add(CodingStudentStatusResponse.builder()
                    .studentId(student.getId())
                    .studentName(student.getName())
                    .studentEmail(student.getEmail())
                    .status(isSolved ? "SOLVED" : "ATTEMPTED")
                    .bestScore((int) Math.round(ratio(best) * 100))
                    .bestPassedCount(best.getPassedCount())
                    .totalCount(best.getTotalCount())
                    .submissionCount(mine.size())
                    // Repository sorts newest first.
                    .lastSubmissionAt(mine.get(0).getSubmittedAt())
                    .build());
        }

        return CodingProblemPerformanceResponse.builder()
                .problemId(problem.getId())
                .problemTitle(problem.getTitle())
                .batchId(problem.getBatch() != null ? problem.getBatch().getId() : null)
                .batchName(problem.getBatch() != null ? problem.getBatch().getName() : null)
                .studentsInScope(scope.size())
                .studentsAttempted(attempted)
                .studentsSolved(solved)
                .totalSubmissions(all.size())
                .students(rows)
                .build();
    }

    /** Every submission on one problem, for an authorized teacher only. */
    @Transactional(readOnly = true)
    public List<CodingSubmissionResponse> getProblemSubmissions(UUID problemId, UUID teacherId) {
        CodingProblem problem = loadProblem(problemId);
        verifyTeacherCanManage(problem, teacherId);
        return submissionRepository.findByProblemIdOrderBySubmittedAtDesc(problemId).stream()
                // The teacher owns this problem, so they may read the source they are grading.
                .map(s -> mapSubmission(s, true))
                .collect(Collectors.toList());
    }

    /**
     * Who the problem is visible to.
     *
     * <p>Mirrors {@code findAccessiblePublished}: a batch problem is scoped to that batch's
     * roster, a course-wide one to everybody enrolled in the course. If the two ever
     * disagree, the performance page would quietly misreport, so they are kept in step.</p>
     */
    private List<User> studentsInScope(CodingProblem problem) {
        if (problem.getBatch() != null) {
            return problem.getBatch().getStudents() == null
                    ? List.of()
                    : new ArrayList<>(problem.getBatch().getStudents());
        }
        return enrollmentRepository.findByCourseId(problem.getCourse().getId()).stream()
                .map(Enrollment::getStudent)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .collect(Collectors.toList());
    }

    /** Fraction of cases passed; guards the zero-case row rather than dividing by zero. */
    private static double ratio(CodingSubmission s) {
        int total = s.getTotalCount() == null ? 0 : s.getTotalCount();
        int passed = s.getPassedCount() == null ? 0 : s.getPassedCount();
        return total == 0 ? 0d : (double) passed / total;
    }

    /** Count of distinct problems a student has solved - used by the teacher roster. */
    @Transactional(readOnly = true)
    public int countSolved(UUID studentId) {
        return (int) submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
                .filter(s -> SOLVED.contains(s.getStatus()))
                .map(s -> s.getProblem().getId())
                .distinct()
                .count();
    }

    // ==================================================================
    // Internals
    // ==================================================================

    private CodingProblem loadProblem(UUID id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CodingProblem", "id", id));
    }

    /**
     * Output comparison.
     *
     * <p>Trailing whitespace and line-ending differences are forgiven, because those are
     * artefacts of printing rather than wrong answers. Nothing else is: internal spacing and
     * ordering still have to match, so a wrong solution cannot slip through.</p>
     */
    private boolean outputsMatch(String actual, String expected) {
        return normalise(actual).equals(normalise(expected));
    }

    private String normalise(String s) {
        if (s == null) return "";
        String[] lines = s.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (out.length() > 0) out.append('\n');
            out.append(line.stripTrailing());
        }
        // Drop trailing blank lines.
        return out.toString().stripTrailing();
    }

    private String slugify(String title) {
        String base = title == null ? "problem" : title.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        base = base.replaceAll("(^-|-$)", "");
        return (base.isEmpty() ? "problem" : base) + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private CodingProblemResponse mapProblem(CodingProblem p, UUID studentId, boolean withSamples) {
        List<StudentCodingTestCaseResponse> samples = null;
        if (withSamples) {
            // Filter to samples BEFORE mapping: a hidden case is never constructed into a
            // student-facing object, so it cannot be serialised by accident.
            samples = testCaseRepository
                    .findByProblemIdAndSampleTrueOrderBySequenceNumberAsc(p.getId()).stream()
                    .map(tc -> StudentCodingTestCaseResponse.builder()
                            .id(tc.getId())
                            .inputData(tc.getInputData())
                            .expectedOutput(tc.getExpectedOutput())
                            .sequenceNumber(tc.getSequenceNumber())
                            .build())
                    .collect(Collectors.toList());
        }

        // Best outcome so far: one ACCEPTED anywhere in the history means solved, otherwise
        // the most recent verdict. Sorted newest-first by the repository.
        String myStatus = null;
        if (studentId != null) {
            List<CodingSubmission> mine = submissionRepository
                    .findByStudentIdAndProblemIdOrderBySubmittedAtDesc(studentId, p.getId());
            if (!mine.isEmpty()) {
                myStatus = mine.stream().anyMatch(s -> SOLVED.contains(s.getStatus()))
                        ? "ACCEPTED"
                        : mine.get(0).getStatus();
            }
        }

        return CodingProblemResponse.builder()
                .id(p.getId())
                .courseId(p.getCourse().getId())
                .courseTitle(p.getCourse().getTitle())
                .batchId(p.getBatch() != null ? p.getBatch().getId() : null)
                .batchName(p.getBatch() != null ? p.getBatch().getName() : null)
                .teacherId(p.getTeacher() != null ? p.getTeacher().getId() : null)
                .teacherName(p.getTeacher() != null ? p.getTeacher().getName() : null)
                .title(p.getTitle())
                .slug(p.getSlug())
                .description(p.getDescription())
                .difficulty(p.getDifficulty())
                .constraintsText(p.getConstraintsText())
                .starterCode(p.getStarterCode())
                .language(p.getLanguage())
                .timeLimitMs(p.getTimeLimitMs())
                .memoryLimitMb(p.getMemoryLimitMb())
                .status(p.getStatus())
                .testCaseCount(testCaseRepository.findByProblemIdOrderBySequenceNumberAsc(p.getId()).size())
                .createdAt(p.getCreatedAt())
                .sampleTestCases(samples)
                .myStatus(myStatus)
                .build();
    }

    private TeacherCodingTestCaseResponse mapTeacherTestCase(CodingTestCase tc) {
        return TeacherCodingTestCaseResponse.builder()
                .id(tc.getId())
                .inputData(tc.getInputData())
                .expectedOutput(tc.getExpectedOutput())
                .sample(tc.isSample())
                .sequenceNumber(tc.getSequenceNumber())
                .points(tc.getPoints())
                .build();
    }

    private CodingSubmissionResponse mapSubmission(CodingSubmission s, boolean includeSource) {
        return CodingSubmissionResponse.builder()
                .id(s.getId())
                .problemId(s.getProblem().getId())
                .problemTitle(s.getProblem().getTitle())
                .studentId(s.getStudent().getId())
                .studentName(s.getStudent().getName())
                .language(s.getLanguage())
                .status(s.getStatus())
                .passedCount(s.getPassedCount())
                .totalCount(s.getTotalCount())
                .runtimeMs(s.getRuntimeMs())
                .message(s.getMessage())
                .submittedAt(s.getSubmittedAt())
                .sourceCode(includeSource ? s.getSourceCode() : null)
                .build();
    }
}
