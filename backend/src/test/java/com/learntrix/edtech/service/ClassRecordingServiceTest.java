package com.learntrix.edtech.service;

import com.learntrix.edtech.dto.recording.*;
import com.learntrix.edtech.entity.*;
import com.learntrix.edtech.entity.Module;
import com.learntrix.edtech.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ClassRecordingServiceTest {

    @Autowired
    private ClassRecordingService recordingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ClassRecordingRepository recordingRepository;

    private User teacher;
    private User student;
    private Course course;
    private Module module;
    private Lesson lesson;

    @BeforeEach
    public void setUp() {
        // Setup Role
        Role teacherRole = new Role();
        teacherRole.setName("TEACHER");
        teacherRole.setDisplayName("Teacher");
        roleRepository.save(teacherRole);

        Role studentRole = new Role();
        studentRole.setName("STUDENT");
        studentRole.setDisplayName("Student");
        roleRepository.save(studentRole);

        // Setup Teacher
        teacher = new User();
        teacher.setEmail("teacher-test@learntrix.com");
        teacher.setName("Test Teacher");
        teacher.setPasswordHash("hash");
        teacher.setStatus("ACTIVE");
        teacher.setRoles(Set.of(teacherRole));
        userRepository.save(teacher);

        // Setup Student
        student = new User();
        student.setEmail("student-test@learntrix.com");
        student.setName("Test Student");
        student.setPasswordHash("hash");
        student.setStatus("ACTIVE");
        student.setRoles(Set.of(studentRole));
        userRepository.save(student);

        // Setup Course
        course = new Course();
        course.setTitle("Test Java Course");
        course.setSlug("test-java-course");
        course.setInstructor(teacher);
        courseRepository.save(course);

        // Setup Module
        module = new Module();
        module.setCourse(course);
        module.setTitle("Module 1");
        module.setSequenceNumber(1);
        moduleRepository.save(module);

        // Setup Lesson
        lesson = new Lesson();
        lesson.setModule(module);
        lesson.setTitle("Lesson 1");
        lesson.setSequenceNumber(1);
        lesson.setDurationSeconds(1000);
        lessonRepository.save(lesson);

        // Setup Enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus("ACTIVE");
        enrollmentRepository.save(enrollment);
    }

    @Test
    public void testCreateRecording() {
        CreateRecordingRequest request = new CreateRecordingRequest();
        request.setCourseId(course.getId());
        request.setModuleId(module.getId());
        request.setLessonId(lesson.getId());
        request.setTitle("Introduction to JPA");
        request.setClassDate(Instant.now());

        RecordingResponse response = recordingService.createRecording(request, teacher.getId());

        assertNotNull(response);
        assertEquals("Introduction to JPA", response.getTitle());
        assertEquals("DRAFT", response.getStatus());
        assertFalse(response.isPublished());
    }

    @Test
    public void testUpdateProgressAndCompletion() {
        // Create base recording
        ClassRecording recording = new ClassRecording();
        recording.setCourse(course);
        recording.setModule(module);
        recording.setLesson(lesson);
        recording.setTeacher(teacher);
        recording.setTitle("OOP Basics");
        recording.setClassDate(Instant.now());
        recording.setStatus(RecordingStatus.PUBLISHED);
        recording.setPublished(true);
        recordingRepository.save(recording);

        // Update progress under completion threshold (e.g. 50%)
        RecordingProgressRequest progressRequest = new RecordingProgressRequest();
        progressRequest.setWatchedSeconds(500);
        progressRequest.setDurationSeconds(1000);

        RecordingProgressResponse progressResponse = recordingService.updateProgress(
                recording.getId(), student.getId(), progressRequest);

        assertNotNull(progressResponse);
        assertEquals(500, progressResponse.getWatchedSeconds());
        assertFalse(progressResponse.isCompleted());

        // Update progress over completion threshold (e.g. 95%)
        progressRequest.setWatchedSeconds(950);
        progressResponse = recordingService.updateProgress(
                recording.getId(), student.getId(), progressRequest);

        assertTrue(progressResponse.isCompleted());
        assertNotNull(progressResponse.getLastWatchedAt());
    }
}
