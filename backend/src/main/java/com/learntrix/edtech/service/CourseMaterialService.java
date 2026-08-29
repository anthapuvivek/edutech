package com.learntrix.edtech.service;

import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.dto.material.CourseMaterialResponse;
import com.learntrix.edtech.dto.material.CreateCourseMaterialRequest;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.CourseMaterial;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.CourseMaterialRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourseMaterialService {

    private final CourseMaterialRepository materialRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseAccessService courseAccessService;

    public CourseMaterialService(
            CourseMaterialRepository materialRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            CourseAccessService courseAccessService) {
        this.materialRepository = materialRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.courseAccessService = courseAccessService;
    }

    public CourseMaterialResponse createMaterial(CreateCourseMaterialRequest request, UUID teacherId) {
        courseAccessService.verifyTeacherCanManageCourse(teacherId, request.getCourseId());

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));

        CourseMaterial material = CourseMaterial.builder()
                .course(course)
                .teacher(teacher)
                .title(request.getTitle())
                .description(request.getDescription())
                .fileUrl(request.getFileUrl())
                .fileType(request.getFileType())
                .fileSizeBytes(request.getFileSizeBytes() != null ? request.getFileSizeBytes() : 0L)
                .build();

        CourseMaterial saved = materialRepository.save(material);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CourseMaterialResponse> getTeacherMaterials(UUID teacherId) {
        List<UUID> courseIds = courseAccessService.getAuthorizedCourseIdsForTeacher(teacherId);
        if (courseIds.isEmpty()) return List.of();

        return materialRepository.findByCourseIdInOrderByCreatedAtDesc(courseIds).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseMaterialResponse> getStudentMaterials(UUID studentId) {
        List<UUID> courseIds = courseAccessService.getAuthorizedCourseIdsForStudent(studentId);
        if (courseIds.isEmpty()) return List.of();

        return materialRepository.findByCourseIdInOrderByCreatedAtDesc(courseIds).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseMaterialResponse getStudentMaterialById(UUID materialId, UUID studentId) {
        CourseMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseMaterial", "id", materialId));

        courseAccessService.verifyStudentCanAccessCourse(studentId, material.getCourse().getId());
        return mapToResponse(material);
    }

    public void deleteMaterial(UUID materialId, UUID teacherId) {
        CourseMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseMaterial", "id", materialId));

        courseAccessService.verifyTeacherCanManageCourse(teacherId, material.getCourse().getId());
        materialRepository.delete(material);
    }

    private CourseMaterialResponse mapToResponse(CourseMaterial m) {
        String teacherName = m.getTeacher() != null ? m.getTeacher().getName() : "Instructor";

        return CourseMaterialResponse.builder()
                .id(m.getId())
                .courseId(m.getCourse().getId())
                .courseTitle(m.getCourse().getTitle())
                .teacherId(m.getTeacher() != null ? m.getTeacher().getId() : null)
                .teacherName(teacherName)
                .title(m.getTitle())
                .description(m.getDescription())
                .fileUrl(m.getFileUrl())
                .fileType(m.getFileType())
                .fileSizeBytes(m.getFileSizeBytes())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
