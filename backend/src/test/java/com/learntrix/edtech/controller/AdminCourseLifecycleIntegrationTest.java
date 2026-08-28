package com.learntrix.edtech.controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learntrix.edtech.dto.course.CreateCourseRequest;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.Enrollment;
import com.learntrix.edtech.entity.Role;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.AccountActivationTokenRepository;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.ClassRecordingRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.EnrollmentRepository;
import com.learntrix.edtech.repository.LiveClassRepository;
import com.learntrix.edtech.repository.RecordingWatchProgressRepository;
import com.learntrix.edtech.repository.RoleRepository;
import com.learntrix.edtech.repository.StudentProfileRepository;
import com.learntrix.edtech.repository.TeacherProfileRepository;
import com.learntrix.edtech.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCourseLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private LiveClassRepository liveClassRepository;

    @Autowired
    private ClassRecordingRepository classRecordingRepository;

    @Autowired
    private RecordingWatchProgressRepository progressRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private TeacherProfileRepository teacherProfileRepository;

    @Autowired
    private AccountActivationTokenRepository activationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User admin;
    private User student;

    @BeforeEach
    void setUp() {
        progressRepository.deleteAll();
        classRecordingRepository.deleteAll();
        liveClassRepository.deleteAll();
        batchRepository.deleteAll();
        enrollmentRepository.deleteAll();
        studentProfileRepository.deleteAll();
        teacherProfileRepository.deleteAll();
        activationTokenRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
            Role r = new Role();
            r.setName("ADMIN");
            r.setDisplayName("Administrator");
            return roleRepository.save(r);
        });

        Role studentRole = roleRepository.findByName("STUDENT").orElseGet(() -> {
            Role r = new Role();
            r.setName("STUDENT");
            r.setDisplayName("Student");
            return roleRepository.save(r);
        });

        admin = new User();
        admin.setEmail("admin@learntrix.com");
        admin.setName("Admin User");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123456"));
        admin.setStatus("ACTIVE");
        admin.setEmailVerified(true);
        admin.setRoles(Set.of(adminRole));
        admin = userRepository.save(admin);

        student = new User();
        student.setEmail("student@learntrix.com");
        student.setName("Student Learner");
        student.setPasswordHash(passwordEncoder.encode("Student@123456"));
        student.setStatus("ACTIVE");
        student.setEmailVerified(true);
        student.setRoles(Set.of(studentRole));
        student = userRepository.save(student);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Complete Course Lifecycle: Admin Create -> Draft Hidden -> Admin Publish -> Catalog Visible -> Student Enroll -> My Courses -> Prevent Duplicate")
    void testCompleteCourseLifecycle() throws Exception {
        String adminToken = loginAndGetToken("admin@learntrix.com", "Admin@123456");
        String studentToken = loginAndGetToken("student@learntrix.com", "Student@123456");

        // ---------------------------------------------------------------------
        // 1. Admin creates a new course as DRAFT
        // ---------------------------------------------------------------------
        CreateCourseRequest createReq = CreateCourseRequest.builder()
                .title("Java Full Stack Engineering Masterclass")
                .subtitle("Master Spring Boot, React, and Microservices")
                .category("Full Stack Development")
                .level("Intermediate")
                .language("English")
                .durationHours(48)
                .lessonCount(24)
                .price(14999)
                .originalPrice(24999)
                .currency("INR")
                .status("DRAFT")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Java Full Stack Engineering Masterclass"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();

        JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data");
        String courseIdStr = createNode.path("id").asText();
        String slug = createNode.path("slug").asText();
        UUID courseId = UUID.fromString(courseIdStr);

        // Database Verification: Course exists in PostgreSQL with status = 'DRAFT'
        Course dbCourse = courseRepository.findById(courseId).orElse(null);
        assertNotNull(dbCourse, "Course must be persisted in PostgreSQL");
        assertEquals("DRAFT", dbCourse.getStatus());
        assertEquals("Java Full Stack Engineering Masterclass", dbCourse.getTitle());

        // ---------------------------------------------------------------------
        // 2. Draft Course is NOT visible on Public Storefront / Catalog
        // ---------------------------------------------------------------------
        MvcResult publicListResult = mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode publicCourses = objectMapper.readTree(publicListResult.getResponse().getContentAsString())
                .path("data").path("items");
        boolean containsDraft = false;
        for (JsonNode c : publicCourses) {
            if (c.path("id").asText().equals(courseIdStr)) {
                containsDraft = true;
                break;
            }
        }
        assertFalse(containsDraft, "DRAFT course must NOT appear on the public storefront");

        // ---------------------------------------------------------------------
        // 3. Student cannot enroll in DRAFT course
        // ---------------------------------------------------------------------
        mockMvc.perform(post("/api/courses/" + courseIdStr + "/enroll")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COURSE_NOT_PUBLISHED"));

        // ---------------------------------------------------------------------
        // 4. Admin publishes the course
        // ---------------------------------------------------------------------
        mockMvc.perform(patch("/api/admin/courses/" + courseIdStr + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // Database Verification: status is updated to 'PUBLISHED'
        Course updatedCourse = courseRepository.findById(courseId).orElseThrow();
        assertEquals("PUBLISHED", updatedCourse.getStatus());

        // ---------------------------------------------------------------------
        // 5. Published course IS NOW visible on Public Home Page / Catalog
        // ---------------------------------------------------------------------
        MvcResult publishedListResult = mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode publishedItems = objectMapper.readTree(publishedListResult.getResponse().getContentAsString())
                .path("data").path("items");
        boolean foundPublished = false;
        for (JsonNode c : publishedItems) {
            if (c.path("id").asText().equals(courseIdStr)) {
                foundPublished = true;
                assertEquals("Java Full Stack Engineering Masterclass", c.path("title").asText());
                assertEquals("PUBLISHED", c.path("status").asText());
                break;
            }
        }
        assertTrue(foundPublished, "PUBLISHED course must appear in public catalogue");

        // Course details by slug
        mockMvc.perform(get("/api/courses/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(courseIdStr));

        // ---------------------------------------------------------------------
        // 6. Student Enrolls in the Course
        // ---------------------------------------------------------------------
        MvcResult enrollResult = mockMvc.perform(post("/api/courses/" + courseIdStr + "/enroll")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseId").value(courseIdStr))
                .andExpect(jsonPath("$.data.status").value("active"))
                .andReturn();

        JsonNode enrollNode = objectMapper.readTree(enrollResult.getResponse().getContentAsString()).path("data");
        String enrollmentIdStr = enrollNode.path("id").asText();

        // Database Verification: Enrollment row exists in PostgreSQL enrollments table
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());
        assertEquals(1, enrollments.size());
        assertEquals(courseId, enrollments.get(0).getCourse().getId());
        assertEquals("ACTIVE", enrollments.get(0).getStatus());

        // Course student count incremented
        Course enrolledCourse = courseRepository.findById(courseId).orElseThrow();
        assertEquals(1, enrolledCourse.getStudentCount());

        // ---------------------------------------------------------------------
        // 7. Student My Courses (`/api/student/enrollments`) shows the enrolled course
        // ---------------------------------------------------------------------
        mockMvc.perform(get("/api/student/enrollments")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].courseId").value(courseIdStr))
                .andExpect(jsonPath("$.data[0].courseTitle").value("Java Full Stack Engineering Masterclass"));

        // ---------------------------------------------------------------------
        // 8. Prevent Duplicate Enrollment
        // ---------------------------------------------------------------------
        mockMvc.perform(post("/api/courses/" + courseIdStr + "/enroll")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ENROLLMENT_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.error.message").value("You are already enrolled in this course."));

        // Database still contains only 1 enrollment
        assertEquals(1, enrollmentRepository.findByStudentId(student.getId()).size());

        // ---------------------------------------------------------------------
        // 9. Admin Unpublishes Course
        // ---------------------------------------------------------------------
        mockMvc.perform(patch("/api/admin/courses/" + courseIdStr + "/unpublish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        assertEquals("DRAFT", courseRepository.findById(courseId).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Security Check: Unauthorized users cannot access Admin Course endpoints")
    void testSecurityConstraints() throws Exception {
        String studentToken = loginAndGetToken("student@learntrix.com", "Student@123456");

        CreateCourseRequest req = CreateCourseRequest.builder()
                .title("Unauthorized Course Creation")
                .category("Full Stack Development")
                .build();

        // 1. Unauthenticated request
        mockMvc.perform(post("/api/admin/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        // 2. Student role request (Forbidden)
        mockMvc.perform(post("/api/admin/courses")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
