package com.learntrix.edtech.service;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import com.learntrix.edtech.dto.coding.ExternalCodingProblemRequest;
import com.learntrix.edtech.dto.coding.ExternalCodingProblemResponse;
import com.learntrix.edtech.dto.coding.CodingProblemProgressResponse;
import com.learntrix.edtech.entity.Batch;
import com.learntrix.edtech.entity.CodingProblem;
import com.learntrix.edtech.entity.CodingProblemCompletion;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.CodingProblemCompletionRepository;
import com.learntrix.edtech.repository.CodingProblemRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.UserRepository;

/**
 * Coding Practice as an assignment board for problems hosted elsewhere.
 *
 * <p>LearnTriX stores a link and the teacher's own notes. It does not fetch, scrape or
 * mirror anything from LeetCode or HackerRank, and it does not run code - so there is no
 * execution cost at all.</p>
 *
 * <p>Authorization is unchanged from the rest of the platform: {@link CourseAccessService}
 * decides what a teacher may manage, and the same batch rule decides what a student sees.
 * The acting user always comes from the token.</p>
 */
@Service
@Transactional
public class ExternalCodingProblemService {

    private static final Set<String> PLATFORMS = Set.of(
            "LEETCODE", "HACKERRANK", "GEEKSFORGEEKS", "CODECHEF", "OTHER");

    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");

    private final CodingProblemRepository problemRepository;
    private final CodingProblemCompletionRepository completionRepository;
    private final CourseRepository courseRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final CourseAccessService courseAccessService;

    public ExternalCodingProblemService(
            CodingProblemRepository problemRepository,
            CodingProblemCompletionRepository completionRepository,
            CourseRepository courseRepository,
            BatchRepository batchRepository,
            UserRepository userRepository,
            CourseAccessService courseAccessService) {
        this.problemRepository = problemRepository;
        this.completionRepository = completionRepository;
        this.courseRepository = courseRepository;
        this.batchRepository = batchRepository;
        this.userRepository = userRepository;
        this.courseAccessService = courseAccessService;
    }

    private boolean isAdmin() {
        return SecurityUtil.hasRole("ADMIN") || SecurityUtil.hasRole("SUPER_ADMIN");
    }

    // ==================================================================
    // Validation
    // ==================================================================

    /**
     * Accepts only absolute http/https links.
     *
     * <p>Parsed rather than pattern-matched, and the scheme is checked against an allow
     * list rather than a deny list. A deny list would have to anticipate every dangerous
     * scheme; an allow list rejects {@code javascript:}, {@code data:} and
     * {@code file:} without having to name them.</p>
     */
    private String validateUrl(String raw) {
        String url = raw == null ? "" : raw.trim();
        if (url.isEmpty()) {
            throw new BusinessException("INVALID_PROBLEM_URL",
                    "The problem URL is required.", HttpStatus.BAD_REQUEST);
        }
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_PROBLEM_URL",
                    "That does not look like a valid link. Paste the full problem URL.",
                    HttpStatus.BAD_REQUEST);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new BusinessException("INVALID_PROBLEM_URL",
                    "The problem URL must start with http:// or https://",
                    HttpStatus.BAD_REQUEST);
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new BusinessException("INVALID_PROBLEM_URL",
                    "The problem URL is missing a website address.", HttpStatus.BAD_REQUEST);
        }
        return url;
    }

    private String validatePlatform(String raw) {
        String p = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!PLATFORMS.contains(p)) {
            throw new BusinessException("INVALID_PLATFORM",
                    "Choose one of: LeetCode, HackerRank, GeeksforGeeks, CodeChef, "
                            + "Other.", HttpStatus.BAD_REQUEST);
        }
        return p;
    }

    private String normaliseDifficulty(String raw) {
        String d = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!DIFFICULTIES.contains(d)) {
            throw new BusinessException("INVALID_DIFFICULTY", "Choose Easy, Medium, or Hard.", HttpStatus.BAD_REQUEST);
        }
        return d;
    }

    // ==================================================================
    // Authorization - same rules as quizzes and live classes
    // ==================================================================

    private void verifyTeacherCanManage(CodingProblem problem, UUID teacherId) {
        if (isAdmin()) return;
        courseAccessService.verifyTeacherCanManageCourse(teacherId, problem.getCourse().getId());
        Batch batch = problem.getBatch();
        if (batch != null
                && (batch.getTeacher() == null || !batch.getTeacher().getId().equals(teacherId))) {
            throw new CourseAccessDeniedException(
                    "You are not authorized to manage problems for that batch");
        }
    }

    /** A batch must belong to the course AND be taught by this teacher. */
    private Batch resolveBatch(UUID batchId, UUID courseId, UUID teacherId) {
        if (batchId == null) {
            throw new BusinessException("BATCH_REQUIRED", "A batch is required.", HttpStatus.BAD_REQUEST);
        }
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId));
        if (batch.getCourse() == null || !batch.getCourse().getId().equals(courseId)) {
            throw new BusinessException("BATCH_COURSE_MISMATCH",
                    "That batch does not belong to the selected course.", HttpStatus.BAD_REQUEST);
        }
        if (!isAdmin()
                && (batch.getTeacher() == null || !batch.getTeacher().getId().equals(teacherId))) {
            throw new CourseAccessDeniedException(
                    "You are not authorized to assign problems to that batch");
        }
        return batch;
    }

    /** Published + enrolled + (for a batch problem) a member of that batch. */
    private void verifyStudentCanAccess(CodingProblem problem, UUID studentId) {
        if (isAdmin()) return;
        if (!"PUBLISHED".equalsIgnoreCase(problem.getStatus())) {
            throw new CourseAccessDeniedException("This problem is not available.");
        }
        courseAccessService.verifyStudentCanAccessCourse(studentId, problem.getCourse().getId());
        Batch batch = problem.getBatch();
        if (batch == null) {
            throw new CourseAccessDeniedException("This problem is not available.");
        }
        boolean member = batch.getStudents() != null
                && batch.getStudents().stream().anyMatch(u -> u.getId().equals(studentId));
        if (!member) {
            throw new CourseAccessDeniedException("This problem is not available.");
        }
    }

    // ==================================================================
    // Teacher
    // ==================================================================

    public ExternalCodingProblemResponse create(ExternalCodingProblemRequest request, UUID teacherId) {
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
        apply(p, request);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        // An externally hosted problem needs no test cases, so it can go live immediately.
        p.setStatus(Boolean.FALSE.equals(request.getActive()) ? "DRAFT" : "PUBLISHED");

        return mapForTeacher(problemRepository.save(p));
    }

    public ExternalCodingProblemResponse update(
            UUID problemId, ExternalCodingProblemRequest request, UUID teacherId) {
        CodingProblem p = load(problemId);
        verifyTeacherCanManage(p, teacherId);
        apply(p, request);
        if (request.getActive() != null) {
            p.setStatus(request.getActive() ? "PUBLISHED" : "DRAFT");
        }
        p.setUpdatedAt(Instant.now());
        return mapForTeacher(problemRepository.save(p));
    }

    /**
     * Fields the teacher owns. Course and batch are deliberately not moved here - changing
     * the cohort of a live problem would silently alter who can see it.
     */
    private void apply(CodingProblem p, ExternalCodingProblemRequest request) {
        p.setPlatform(validatePlatform(request.getPlatform()));
        p.setProblemUrl(validateUrl(request.getProblemUrl()));
        p.setProblemNumber(blankToNull(request.getProblemNumber()));
        p.setTitle(request.getTitle() == null ? null : request.getTitle().trim());
        p.setDifficulty(normaliseDifficulty(request.getDifficulty()));
        p.setTopics(blankToNull(request.getTopics()));
        p.setDescription(request.getDescription() == null ? "" : request.getDescription());
        p.setDeadline(request.getDeadline());
        // Execution fields are meaningless now; keep the columns satisfied without pretending.
        if (p.getLanguage() == null) p.setLanguage("external");
    }

    public ExternalCodingProblemResponse setActive(UUID problemId, boolean active, UUID teacherId) {
        CodingProblem p = load(problemId);
        verifyTeacherCanManage(p, teacherId);
        p.setStatus(active ? "PUBLISHED" : "DRAFT");
        p.setUpdatedAt(Instant.now());
        return mapForTeacher(problemRepository.save(p));
    }

    public ExternalCodingProblemResponse archive(UUID problemId, UUID teacherId) {
        CodingProblem p = load(problemId);
        verifyTeacherCanManage(p, teacherId);
        p.setStatus("ARCHIVED");
        p.setUpdatedAt(Instant.now());
        return mapForTeacher(problemRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<ExternalCodingProblemResponse> teacherList(UUID teacherId) {
        List<UUID> courseIds = isAdmin()
                ? courseRepository.findAll().stream().map(Course::getId).collect(Collectors.toList())
                : courseAccessService.getAuthorizedCourseIdsForTeacher(teacherId);
        if (courseIds.isEmpty()) return List.of();
        return problemRepository.findByCourseIdIn(courseIds).stream()
                .filter(this::isExternal)
                .filter(p -> isAdmin() || (p.getBatch() != null && p.getBatch().getTeacher() != null
                        && p.getBatch().getTeacher().getId().equals(teacherId)))
                .map(this::mapForTeacher)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExternalCodingProblemResponse teacherGet(UUID problemId, UUID teacherId) {
        CodingProblem p = load(problemId);
        verifyTeacherCanManage(p, teacherId);
        return mapForTeacher(p);
    }

    // ==================================================================
    // Student
    // ==================================================================

    @Transactional(readOnly = true)
    public List<ExternalCodingProblemResponse> studentList(UUID studentId) {
        List<UUID> courseIds = courseAccessService.getAuthorizedCourseIdsForStudent(studentId);
        if (courseIds.isEmpty()) return List.of();

        // One lookup for the caller's completions, rather than one per problem.
        Map<UUID, CodingProblemCompletion> mine = new HashMap<>();
        for (CodingProblemCompletion c : completionRepository.findByStudentId(studentId)) {
            mine.put(c.getProblem().getId(), c);
        }

        return problemRepository.findAccessiblePublished(studentId, courseIds).stream()
                .filter(this::isExternal)
                .filter(p -> p.getBatch() != null)
                .map(p -> mapForStudent(p, mine.get(p.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExternalCodingProblemResponse studentGet(UUID problemId, UUID studentId) {
        CodingProblem p = load(problemId);
        verifyStudentCanAccess(p, studentId);
        return mapForStudent(p,
                completionRepository.findByProblemIdAndStudentId(problemId, studentId).orElse(null));
    }

    /**
     * Records what the authenticated student says about their own progress.
     *
     * <p>Access is re-checked here, not just when listing: a student could otherwise POST a
     * problem id belonging to another cohort. And the row is keyed on the token's student
     * id, so there is no way to mark someone else's problem done.</p>
     */
    public ExternalCodingProblemResponse setCompleted(
            UUID problemId, boolean completed, UUID studentId) {
        CodingProblem p = load(problemId);
        verifyStudentCanAccess(p, studentId);

        CodingProblemCompletion c = completionRepository
                .findByProblemIdAndStudentId(problemId, studentId)
                .orElseGet(() -> {
                    CodingProblemCompletion fresh = new CodingProblemCompletion();
                    fresh.setProblem(p);
                    fresh.setStudent(userRepository.findById(studentId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId)));
                    return fresh;
                });

        c.setCompleted(completed);
        c.setCompletedAt(completed ? Instant.now() : null);
        c.setStatus(completed ? "COMPLETED" : (c.getOpenedAt() == null ? "NOT_STARTED" : "IN_PROGRESS"));
        c.setUpdatedAt(Instant.now());

        return mapForStudent(p, completionRepository.save(c));
    }

    public ExternalCodingProblemResponse markOpened(UUID problemId, UUID studentId) {
        CodingProblem p = load(problemId);
        verifyStudentCanAccess(p, studentId);
        CodingProblemCompletion c = completionRepository.findByProblemIdAndStudentId(problemId, studentId)
                .orElseGet(() -> newProgress(p, studentId));
        if (!"COMPLETED".equals(c.getStatus())) {
            if (c.getOpenedAt() == null) c.setOpenedAt(Instant.now());
            c.setStatus("OPENED");
            c.setCompleted(false);
            c.setUpdatedAt(Instant.now());
        }
        return mapForStudent(p, completionRepository.save(c));
    }

    private CodingProblemCompletion newProgress(CodingProblem p, UUID studentId) {
        CodingProblemCompletion c = new CodingProblemCompletion();
        c.setProblem(p);
        c.setStudent(userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId)));
        c.setCreatedAt(Instant.now());
        return c;
    }

    @Transactional(readOnly = true)
    public CodingProblemProgressResponse progress(UUID problemId, UUID teacherId) {
        CodingProblem p = load(problemId);
        verifyTeacherCanManage(p, teacherId);
        if (p.getBatch() == null) throw new BusinessException("BATCH_REQUIRED", "Problem has no batch.", HttpStatus.BAD_REQUEST);
        Map<UUID, CodingProblemCompletion> records = completionRepository.findByProblemId(problemId).stream()
                .collect(Collectors.toMap(c -> c.getStudent().getId(), c -> c));
        List<CodingProblemProgressResponse.StudentProgress> students = p.getBatch().getStudents().stream()
                .map(student -> {
                    CodingProblemCompletion c = records.get(student.getId());
                    return CodingProblemProgressResponse.StudentProgress.builder()
                            .studentId(student.getId()).studentName(student.getName()).studentEmail(student.getEmail())
                            .status(c == null ? "NOT_STARTED" : c.getStatus())
                            .openedAt(c == null ? null : c.getOpenedAt())
                            .completedAt(c == null ? null : c.getCompletedAt()).build();
                }).toList();
        int completed = (int) students.stream().filter(s -> "COMPLETED".equals(s.getStatus())).count();
        int inProgress = (int) students.stream().filter(s -> "OPENED".equals(s.getStatus()) || "IN_PROGRESS".equals(s.getStatus())).count();
        return CodingProblemProgressResponse.builder().problemId(p.getId()).problemTitle(p.getTitle())
                .totalStudents(students.size()).completed(completed).inProgress(inProgress)
                .notStarted(students.size() - completed - inProgress).students(students).build();
    }

    // ==================================================================
    // Internals
    // ==================================================================

    private CodingProblem load(UUID id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CodingProblem", "id", id));
    }

    /**
     * Separates the new external problems from the older executable ones that still sit in
     * the same table. A problem with no URL belongs to the old judge-based flow.
     */
    private boolean isExternal(CodingProblem p) {
        return p.getProblemUrl() != null && !p.getProblemUrl().isBlank();
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private ExternalCodingProblemResponse mapForTeacher(CodingProblem p) {
        return base(p)
                .completedCount(completionRepository.findByProblemIdAndCompletedTrue(p.getId()).size())
                .build();
    }

    private ExternalCodingProblemResponse mapForStudent(
            CodingProblem p, CodingProblemCompletion completion) {
        return base(p)
                .completed(completion != null && completion.isCompleted())
                .completedAt(completion == null ? null : completion.getCompletedAt())
                .openedAt(completion == null ? null : completion.getOpenedAt())
                .progressStatus(completion == null ? "NOT_STARTED" : completion.getStatus())
                .build();
    }

    private ExternalCodingProblemResponse.ExternalCodingProblemResponseBuilder base(CodingProblem p) {
        return ExternalCodingProblemResponse.builder()
                .id(p.getId())
                .courseId(p.getCourse().getId())
                .courseTitle(p.getCourse().getTitle())
                .batchId(p.getBatch() != null ? p.getBatch().getId() : null)
                .batchName(p.getBatch() != null ? p.getBatch().getName() : null)
                .teacherId(p.getTeacher() != null ? p.getTeacher().getId() : null)
                .teacherName(p.getTeacher() != null ? p.getTeacher().getName() : null)
                .platform(p.getPlatform())
                .problemNumber(p.getProblemNumber())
                .problemUrl(p.getProblemUrl())
                .title(p.getTitle())
                .difficulty(p.getDifficulty())
                .topics(p.getTopics())
                .description(p.getDescription())
                .status(p.getStatus())
                .active("PUBLISHED".equalsIgnoreCase(p.getStatus()))
                .createdAt(p.getCreatedAt())
                .deadline(p.getDeadline());
                
    }
}
