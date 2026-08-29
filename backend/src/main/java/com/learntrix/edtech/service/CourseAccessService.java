package com.learntrix.edtech.service;

import com.learntrix.edtech.common.exception.CourseAccessDeniedException;
import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.entity.Batch;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.Enrollment;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CourseAccessService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final BatchRepository batchRepository;

    public CourseAccessService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            BatchRepository batchRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.batchRepository = batchRepository;
    }

    /**
     * Verifies that a teacher is authorized to manage or create content for a given course.
     * A teacher can manage a course if:
     * 1. They are the assigned instructor on the Course entity, or
     * 2. They teach a Batch associated with the course, or
     * 3. They have student enrollments allocated to them for the course.
     */
    public void verifyTeacherCanManageCourse(UUID teacherId, UUID courseId) {
        if (teacherId == null || courseId == null) {
            throw new CourseAccessDeniedException("Invalid teacher or course identifier");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        boolean isInstructor = course.getInstructor() != null && course.getInstructor().getId().equals(teacherId);
        if (isInstructor) {
            return;
        }

        boolean teachesBatch = batchRepository.findByTeacherIdOrderByStartDateAsc(teacherId).stream()
                .anyMatch(b -> b.getCourse() != null && b.getCourse().getId().equals(courseId));
        if (teachesBatch) {
            return;
        }

        boolean hasAllocations = !enrollmentRepository.findByTeacherIdAndCourseId(teacherId, courseId).isEmpty();
        if (hasAllocations) {
            return;
        }

        throw new CourseAccessDeniedException("You are not authorized to manage content for this course");
    }

    /**
     * Verifies that a student has active access to a given course.
     */
    public void verifyStudentCanAccessCourse(UUID studentId, UUID courseId) {
        if (studentId == null || courseId == null) {
            throw new CourseAccessDeniedException("Invalid student or course identifier");
        }

        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId);
        if (enrollmentOpt.isEmpty()) {
            throw new CourseAccessDeniedException("You must be enrolled in the course to access this content");
        }

        String status = enrollmentOpt.get().getStatus();
        if (!"ACTIVE".equalsIgnoreCase(status) && !"ENROLLED".equalsIgnoreCase(status)) {
            throw new CourseAccessDeniedException("Your enrollment in this course is not active");
        }
    }

    /**
     * Checks if a student is allocated to a specific teacher for a specific course.
     */
    public boolean isStudentAllocatedToTeacherCourse(UUID studentId, UUID teacherId, UUID courseId) {
        if (studentId == null || teacherId == null || courseId == null) {
            return false;
        }

        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId);
        if (enrollmentOpt.isEmpty()) {
            return false;
        }

        Enrollment enrollment = enrollmentOpt.get();
        if (!"ACTIVE".equalsIgnoreCase(enrollment.getStatus()) && !"ENROLLED".equalsIgnoreCase(enrollment.getStatus())) {
            return false;
        }

        if (enrollment.getTeacher() != null && enrollment.getTeacher().getId().equals(teacherId)) {
            return true;
        }

        if (enrollment.getCourse() != null && enrollment.getCourse().getInstructor() != null
                && enrollment.getCourse().getInstructor().getId().equals(teacherId)) {
            return true;
        }

        // Check if student is in a batch taught by this teacher for this course
        return batchRepository.findByTeacherIdOrderByStartDateAsc(teacherId).stream()
                .filter(b -> b.getCourse() != null && b.getCourse().getId().equals(courseId))
                .anyMatch(b -> b.getStudents() != null && b.getStudents().stream().anyMatch(s -> s.getId().equals(studentId)));
    }

    /**
     * Returns list of Course IDs for which the student has active enrollment.
     */
    public List<UUID> getAuthorizedCourseIdsForStudent(UUID studentId) {
        if (studentId == null) return List.of();
        return enrollmentRepository.findByStudentId(studentId).stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus()) || "ENROLLED".equalsIgnoreCase(e.getStatus()))
                .map(e -> e.getCourse().getId())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Returns list of Course IDs that the teacher is authorized to teach/manage.
     */
    public List<UUID> getAuthorizedCourseIdsForTeacher(UUID teacherId) {
        if (teacherId == null) return List.of();

        Set<UUID> courseIds = new HashSet<>();

        // Instructor on courses
        courseRepository.findByInstructorId(teacherId).forEach(c -> courseIds.add(c.getId()));

        // Batches taught by teacher
        batchRepository.findByTeacherIdOrderByStartDateAsc(teacherId).forEach(b -> {
            if (b.getCourse() != null) courseIds.add(b.getCourse().getId());
        });

        // Direct allocations
        enrollmentRepository.findByTeacherId(teacherId).forEach(e -> {
            if (e.getCourse() != null) courseIds.add(e.getCourse().getId());
        });

        return new ArrayList<>(courseIds);
    }

    /**
     * Returns all active enrollments belonging to a teacher across courses and batches.
     */
    public List<Enrollment> getTeacherStudentEnrollments(UUID teacherId) {
        if (teacherId == null) return List.of();

        List<Enrollment> directTeacherEnrollments = enrollmentRepository.findByTeacherId(teacherId);
        List<Course> instructorCourses = courseRepository.findByInstructorId(teacherId);
        List<UUID> instructorCourseIds = instructorCourses.stream().map(Course::getId).collect(Collectors.toList());
        List<Enrollment> instructorCourseEnrollments = instructorCourseIds.isEmpty()
                ? List.of()
                : enrollmentRepository.findByCourseIdIn(instructorCourseIds);

        List<Batch> teacherBatches = batchRepository.findByTeacherIdOrderByStartDateAsc(teacherId);
        Set<UUID> batchStudentIds = teacherBatches.stream()
                .filter(b -> b.getStudents() != null)
                .flatMap(b -> b.getStudents().stream())
                .map(User::getId)
                .collect(Collectors.toSet());

        Map<UUID, Enrollment> enrollmentMap = new LinkedHashMap<>();

        for (Enrollment e : directTeacherEnrollments) {
            enrollmentMap.put(e.getId(), e);
        }

        for (Enrollment e : instructorCourseEnrollments) {
            // If batch students are defined, only include if student is in batch or enrollment has no competing batch
            if (batchStudentIds.isEmpty() || batchStudentIds.contains(e.getStudent().getId()) || e.getTeacher() != null && e.getTeacher().getId().equals(teacherId)) {
                enrollmentMap.putIfAbsent(e.getId(), e);
            }
        }

        return new ArrayList<>(enrollmentMap.values());
    }

    /**
     * Checks if a student is assigned to a teacher.
     */
    public boolean isStudentAssignedToTeacher(UUID studentId, UUID teacherId) {
        if (studentId == null || teacherId == null) return false;

        List<Enrollment> enrollments = getTeacherStudentEnrollments(teacherId);
        return enrollments.stream().anyMatch(e -> e.getStudent().getId().equals(studentId));
    }
}
