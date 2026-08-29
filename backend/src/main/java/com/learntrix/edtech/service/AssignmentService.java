package com.learntrix.edtech.service;

import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.dto.assignment.*;
import com.learntrix.edtech.entity.Assignment;
import com.learntrix.edtech.entity.AssignmentSubmission;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.AssignmentRepository;
import com.learntrix.edtech.repository.AssignmentSubmissionRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.UserRepository;
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

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            CourseAccessService courseAccessService) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.courseAccessService = courseAccessService;
    }

    public AssignmentResponse createAssignment(CreateAssignmentRequest request, UUID teacherId) {
        courseAccessService.verifyTeacherCanManageCourse(teacherId, request.getCourseId());

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));

        Assignment assignment = new Assignment();
        assignment.setCourse(course);
        assignment.setTeacher(teacher);
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setDueDate(request.getDueDate() != null ? request.getDueDate() : Instant.now().plusSeconds(86400 * 7));
        assignment.setPoints(request.getPoints() != null ? request.getPoints() : 100);
        assignment.setStatus("PUBLISHED");
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
                .map(a -> mapToResponse(a, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getTeacherAssignmentById(UUID assignmentId, UUID teacherId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));

        courseAccessService.verifyTeacherCanManageCourse(teacherId, assignment.getCourse().getId());
        return mapToResponse(assignment, null);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getStudentAssignments(UUID studentId) {
        List<UUID> courseIds = courseAccessService.getAuthorizedCourseIdsForStudent(studentId);
        if (courseIds.isEmpty()) return List.of();

        List<Assignment> assignments = assignmentRepository.findByCourseIdIn(courseIds);
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

        courseAccessService.verifyStudentCanAccessCourse(studentId, assignment.getCourse().getId());

        Optional<AssignmentSubmission> sub = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId);
        return mapToResponse(assignment, sub.orElse(null));
    }

    public AssignmentSubmissionResponse submitAssignment(UUID assignmentId, UUID studentId, SubmitAssignmentRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));

        courseAccessService.verifyStudentCanAccessCourse(studentId, assignment.getCourse().getId());

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
        submission.setSubmittedAt(Instant.now());
        submission.setStatus("Submitted");

        AssignmentSubmission saved = submissionRepository.save(submission);
        return mapToSubmissionResponse(saved);
    }

    public AssignmentSubmissionResponse gradeSubmission(UUID assignmentId, UUID submissionId, UUID teacherId, GradeSubmissionRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));

        courseAccessService.verifyTeacherCanManageCourse(teacherId, assignment.getCourse().getId());

        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("AssignmentSubmission", "id", submissionId));

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

        courseAccessService.verifyTeacherCanManageCourse(teacherId, assignment.getCourse().getId());

        return submissionRepository.findByAssignmentId(assignmentId).stream()
                .map(this::mapToSubmissionResponse)
                .collect(Collectors.toList());
    }

    private AssignmentResponse mapToResponse(Assignment assignment, AssignmentSubmission submission) {
        String teacherName = assignment.getTeacher() != null
                ? assignment.getTeacher().getName()
                : (assignment.getCourse().getInstructor() != null ? assignment.getCourse().getInstructor().getName() : "Instructor");

        return AssignmentResponse.builder()
                .id(assignment.getId())
                .courseId(assignment.getCourse().getId())
                .courseTitle(assignment.getCourse().getTitle())
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
