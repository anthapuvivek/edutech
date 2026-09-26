package com.learntrix.edtech.service;

import com.learntrix.edtech.common.exception.BusinessException;
import com.learntrix.edtech.common.exception.CourseAccessDeniedException;
import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.assignment.*;
import com.learntrix.edtech.entity.Assignment;
import com.learntrix.edtech.entity.Batch;
import com.learntrix.edtech.entity.AssignmentSubmission;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.AssignmentRepository;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.EnrollmentRepository;
import com.learntrix.edtech.repository.AssignmentSubmissionRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseAccessService courseAccessService;
    private final BatchRepository batchRepository;
    private final EnrollmentRepository enrollmentRepository;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            CourseAccessService courseAccessService,
            BatchRepository batchRepository,
            EnrollmentRepository enrollmentRepository) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.courseAccessService = courseAccessService;
        this.batchRepository = batchRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    // ------------------------------------------------------------------
    // Authorization helpers - the same shape quizzes and coding problems use.
    // ------------------------------------------------------------------

    private boolean isAdmin() {
        return SecurityUtil.hasRole("ADMIN") || SecurityUtil.hasRole("SUPER_ADMIN");
    }

    /**
     * Course authority alone is not enough for a batch assignment: another teacher on the
     * same course would otherwise be able to edit this cohort's work.
     */
    private void verifyTeacherCanManage(Assignment assignment, UUID teacherId) {
        if (isAdmin()) return;
        courseAccessService.verifyTeacherCanManageCourse(teacherId, assignment.getCourse().getId());
        Batch batch = assignment.getBatch();
        if (batch != null
                && (batch.getTeacher() == null || !batch.getTeacher().getId().equals(teacherId))) {
            throw new CourseAccessDeniedException(
                    "You are not authorized to manage assignments for that batch");
        }
    }

    /** A batch must belong to the course AND be taught by this teacher. */
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
                    "You are not authorized to create assignments for that batch");
        }
        return batch;
    }

    /** Published + enrolled + (for a batch assignment) a member of that batch. */
    private void verifyStudentCanAccess(Assignment assignment, UUID studentId) {
        if (isAdmin()) return;
        if (!"PUBLISHED".equalsIgnoreCase(assignment.getStatus())) {
            throw new CourseAccessDeniedException("This assignment is not available.");
        }
        courseAccessService.verifyStudentCanAccessCourse(studentId, assignment.getCourse().getId());
        Batch batch = assignment.getBatch();
        if (batch != null) {
            boolean member = batch.getStudents() != null
                    && batch.getStudents().stream().anyMatch(u -> u.getId().equals(studentId));
            if (!member) {
                throw new CourseAccessDeniedException("This assignment is not available.");
            }
        }
    }

    public AssignmentResponse createAssignment(CreateAssignmentRequest request, UUID teacherId) {
        if (!isAdmin()) {
            courseAccessService.verifyTeacherCanManageCourse(teacherId, request.getCourseId());
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));

        Assignment assignment = new Assignment();
        assignment.setCourse(course);
        assignment.setTeacher(teacher);
        assignment.setBatch(resolveBatch(request.getBatchId(), course.getId(), teacherId));
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setDueDate(request.getDueDate() != null ? request.getDueDate() : Instant.now().plusSeconds(86400 * 7));
        assignment.setPoints(request.getPoints() != null ? request.getPoints() : 100);
        // Default DRAFT so an unfinished assignment is never visible to students. The
        // previous hardcoded PUBLISHED made every assignment live the moment it existed.
        assignment.setStatus("PUBLISHED".equalsIgnoreCase(request.getStatus()) ? "PUBLISHED" : "DRAFT");
        assignment.setCreatedAt(Instant.now());
        assignment.setUpdatedAt(Instant.now());

        Assignment saved = assignmentRepository.save(assignment);
        return mapToResponse(saved, null);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getTeacherAssignments(UUID teacherId) {
        List<UUID> courseIds = courseAccessService.getAuthorizedCourseIdsForTeacher(teacherId);
        if (courseIds.isEmpty()) return List.of();

        return assignmentRepository.findByCourseIdIn(courseIds).stream()
                .map(this::mapForTeacher)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getTeacherAssignmentById(UUID assignmentId, UUID teacherId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));

        verifyTeacherCanManage(assignment, teacherId);
        return mapForTeacher(assignment);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getStudentAssignments(UUID studentId) {
        List<UUID> courseIds = courseAccessService.getAuthorizedCourseIdsForStudent(studentId);
        if (courseIds.isEmpty()) return List.of();

        // Batch-aware and published-only. findByCourseIdIn returned every assignment on
        // the course, including drafts and other cohorts' work.
        List<Assignment> assignments = assignmentRepository.findAccessiblePublished(studentId, courseIds);
        return assignments.stream()
                .map(a -> {
                    Optional<AssignmentSubmission> sub = submissionRepository.findByAssignmentIdAndStudentId(a.getId(), studentId);
                    return mapToResponse(a, sub.orElse(null));
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getStudentAssignmentById(UUID assignmentId, UUID studentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));

        verifyStudentCanAccess(assignment, studentId);

        Optional<AssignmentSubmission> sub = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId);
        return mapToResponse(assignment, sub.orElse(null));
    }

    public AssignmentSubmissionResponse submitAssignment(UUID assignmentId, UUID studentId, SubmitAssignmentRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));

        verifyStudentCanAccess(assignment, studentId);

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        AssignmentSubmission submission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseGet(() -> {
                    AssignmentSubmission s = new AssignmentSubmission();
                    s.setAssignment(assignment);
                    s.setStudent(student);
                    return s;
                });

        submission.setSubmissionUrl(request.getSubmissionUrl());
        submission.setNotes(request.getNotes());
        Instant now = Instant.now();
        submission.setSubmittedAt(now);
        // Derived, never sent by the client: the due date is the only authority on lateness.
        boolean late = assignment.getDueDate() != null && now.isAfter(assignment.getDueDate());
        submission.setStatus(late ? "Late" : "Submitted");

        AssignmentSubmission saved = submissionRepository.save(submission);
        return mapToSubmissionResponse(saved);
    }

    public AssignmentSubmissionResponse gradeSubmission(UUID assignmentId, UUID submissionId, UUID teacherId, GradeSubmissionRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));

        verifyTeacherCanManage(assignment, teacherId);

        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("AssignmentSubmission", "id", submissionId));

        // Without this the assignment id and the submission id were checked independently:
        // a teacher could pair an assignment they own with another teacher's submission and
        // grade a student who was never theirs.
        if (submission.getAssignment() == null
                || !submission.getAssignment().getId().equals(assignmentId)) {
            throw new BusinessException("SUBMISSION_NOT_IN_ASSIGNMENT",
                    "That submission does not belong to this assignment.", HttpStatus.BAD_REQUEST);
        }

        // The DTO caps at 100, which is wrong for any assignment not worth 100 points.
        int max = assignment.getPoints() == null ? 100 : assignment.getPoints();
        if (request.getGrade() == null || request.getGrade() < 0 || request.getGrade() > max) {
            throw new BusinessException("INVALID_GRADE",
                    "Grade must be between 0 and " + max + ".", HttpStatus.BAD_REQUEST);
        }

        submission.setGrade(request.getGrade());
        submission.setFeedback(request.getFeedback());
        submission.setStatus("Graded");

        AssignmentSubmission saved = submissionRepository.save(submission);
        return mapToSubmissionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> getAssignmentSubmissions(UUID assignmentId, UUID teacherId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));

        verifyTeacherCanManage(assignment, teacherId);

        return submissionRepository.findByAssignmentId(assignmentId).stream()
                .map(this::mapToSubmissionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Publish or withdraw. Mirrors the quiz and coding-problem flows so a teacher meets
     * one convention everywhere.
     */
    public AssignmentResponse setPublished(UUID assignmentId, boolean publish, UUID teacherId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));
        verifyTeacherCanManage(assignment, teacherId);

        if (publish && (assignment.getTitle() == null || assignment.getTitle().isBlank())) {
            throw new BusinessException("INVALID_ASSIGNMENT",
                    "The assignment needs a title before publishing.", HttpStatus.CONFLICT);
        }
        assignment.setStatus(publish ? "PUBLISHED" : "DRAFT");
        assignment.setUpdatedAt(Instant.now());
        return mapToResponse(assignmentRepository.save(assignment), null);
    }

    /** Edit wording, due date and marks. Course is not moved - that would change audience. */
    public AssignmentResponse updateAssignment(
            UUID assignmentId, CreateAssignmentRequest request, UUID teacherId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));
        verifyTeacherCanManage(assignment, teacherId);

        if (request.getTitle() != null) assignment.setTitle(request.getTitle());
        if (request.getDescription() != null) assignment.setDescription(request.getDescription());
        if (request.getDueDate() != null) assignment.setDueDate(request.getDueDate());
        if (request.getPoints() != null) assignment.setPoints(request.getPoints());
        if (request.getStatus() != null) {
            assignment.setStatus("PUBLISHED".equalsIgnoreCase(request.getStatus()) ? "PUBLISHED" : "DRAFT");
        }
        assignment.setUpdatedAt(Instant.now());
        return mapToResponse(assignmentRepository.save(assignment), null);
    }

    public void deleteAssignment(UUID assignmentId, UUID teacherId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));
        verifyTeacherCanManage(assignment, teacherId);

        // Refuse rather than silently destroy submitted work.
        List<AssignmentSubmission> existing = submissionRepository.findByAssignmentId(assignmentId);
        if (!existing.isEmpty()) {
            throw new BusinessException("ASSIGNMENT_HAS_SUBMISSIONS",
                    "This assignment has " + existing.size() + " submission(s). "
                            + "Unpublish it instead of deleting it.", HttpStatus.CONFLICT);
        }
        assignmentRepository.delete(assignment);
    }

    /**
     * Who the assignment is visible to - the batch roster, or everyone enrolled.
     *
     * <p>Kept in step with findAccessiblePublished, so "pending" means students who could
     * actually have submitted rather than the whole school.</p>
     */
    private List<User> studentsInScope(Assignment assignment) {
        if (assignment.getBatch() != null) {
            return assignment.getBatch().getStudents() == null
                    ? List.of()
                    : new java.util.ArrayList<>(assignment.getBatch().getStudents());
        }
        return enrollmentRepository.findByCourseId(assignment.getCourse().getId()).stream()
                .map(com.learntrix.edtech.entity.Enrollment::getStudent)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a, java.util.LinkedHashMap::new))
                .values().stream().collect(Collectors.toList());
    }

    /**
     * Teacher view: the same response plus cohort counts.
     *
     * <p>Counts DISTINCT students, not submissions - one student resubmitting must not
     * read as two people having handed work in.</p>
     */
    private AssignmentResponse mapForTeacher(Assignment assignment) {
        List<User> scope = studentsInScope(assignment);
        List<AssignmentSubmission> subs = submissionRepository.findByAssignmentId(assignment.getId());

        java.util.Set<UUID> submitters = subs.stream()
                .map(x -> x.getStudent().getId()).collect(Collectors.toSet());
        long late = subs.stream().filter(x -> "Late".equalsIgnoreCase(x.getStatus()))
                .map(x -> x.getStudent().getId()).distinct().count();
        long graded = subs.stream().filter(x -> x.getGrade() != null)
                .map(x -> x.getStudent().getId()).distinct().count();

        AssignmentResponse base = mapToResponse(assignment, null);
        base.setTotalStudents(scope.size());
        base.setSubmittedCount(submitters.size());
        base.setPendingCount(Math.max(0, scope.size() - submitters.size()));
        base.setLateCount((int) late);
        base.setGradedCount((int) graded);
        return base;
    }

    private AssignmentResponse mapToResponse(Assignment assignment, AssignmentSubmission submission) {
        String teacherName = assignment.getTeacher() != null
                ? assignment.getTeacher().getName()
                : (assignment.getCourse().getInstructor() != null ? assignment.getCourse().getInstructor().getName() : "Instructor");

        return AssignmentResponse.builder()
                .id(assignment.getId())
                .courseId(assignment.getCourse().getId())
                .courseTitle(assignment.getCourse().getTitle())
                .batchId(assignment.getBatch() != null ? assignment.getBatch().getId() : null)
                .batchName(assignment.getBatch() != null ? assignment.getBatch().getName() : null)
                .teacherId(assignment.getTeacher() != null ? assignment.getTeacher().getId() : null)
                .teacherName(teacherName)
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .dueDate(assignment.getDueDate())
                .points(assignment.getPoints())
                .status(assignment.getStatus())
                .createdAt(assignment.getCreatedAt())
                .submitted(submission != null)
                .submissionStatus(submission != null ? submission.getStatus() : "Not Submitted")
                .grade(submission != null ? submission.getGrade() : null)
                .feedback(submission != null ? submission.getFeedback() : null)
                .build();
    }

    private AssignmentSubmissionResponse mapToSubmissionResponse(AssignmentSubmission s) {
        return AssignmentSubmissionResponse.builder()
                .id(s.getId())
                .assignmentId(s.getAssignment().getId())
                .assignmentTitle(s.getAssignment().getTitle())
                .studentId(s.getStudent().getId())
                .studentName(s.getStudent().getName())
                .studentEmail(s.getStudent().getEmail())
                .submissionUrl(s.getSubmissionUrl())
                .submittedAt(s.getSubmittedAt())
                .grade(s.getGrade())
                .feedback(s.getFeedback())
                .status(s.getStatus())
                .build();
    }
}
