package com.learntrix.edtech.controller;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.Role;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.EnrollmentRepository;
import com.learntrix.edtech.repository.RoleRepository;
import com.learntrix.edtech.repository.StudentProfileRepository;
import com.learntrix.edtech.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminBatchWorkflowIntegrationTest {

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
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User admin;
    private User teacher;
    private User otherTeacher;
    private User existingStudent;
    private Course course;

    @BeforeEach
    void setUp() {
        // Clean state — order matters to avoid FK violations
        batchRepository.deleteAll();
        enrollmentRepository.deleteAll();
        studentProfileRepository.deleteAll();
        userRepository.deleteAll();
        courseRepository.deleteAll();

        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
            Role r = new Role();
            r.setName("ADMIN");
            r.setDisplayName("Administrator");
            return roleRepository.save(r);
        });
        Role teacherRole = roleRepository.findByName("TEACHER").orElseGet(() -> {
            Role r = new Role();
            r.setName("TEACHER");
            r.setDisplayName("Teacher");
            return roleRepository.save(r);
        });
        Role studentRole = roleRepository.findByName("STUDENT").orElseGet(() -> {
            Role r = new Role();
            r.setName("STUDENT");
            r.setDisplayName("Student");
            return roleRepository.save(r);
        });

        admin = new User();
        admin.setEmail("admin.flow@test.com");
        admin.setPasswordHash(passwordEncoder.encode("Password123!"));
        admin.setName("Flow Admin");
        admin.setStatus("ACTIVE");
        admin.setEmailVerified(true);
        admin.setRoles(Set.of(adminRole));
        admin = userRepository.save(admin);

        teacher = new User();
        teacher.setEmail("teacher.flow@test.com");
        teacher.setPasswordHash(passwordEncoder.encode("Password123!"));
        teacher.setName("Flow Teacher");
        teacher.setStatus("ACTIVE");
        teacher.setEmailVerified(true);
        teacher.setRoles(Set.of(teacherRole));
        teacher = userRepository.save(teacher);

        otherTeacher = new User();
        otherTeacher.setEmail("other.teacher@test.com");
        otherTeacher.setPasswordHash(passwordEncoder.encode("Password123!"));
        otherTeacher.setName("Other Teacher");
        otherTeacher.setStatus("ACTIVE");
        otherTeacher.setEmailVerified(true);
        otherTeacher.setRoles(Set.of(teacherRole));
        otherTeacher = userRepository.save(otherTeacher);

        existingStudent = new User();
        existingStudent.setEmail("existing.student@test.com");
        existingStudent.setPasswordHash(passwordEncoder.encode("Password123!"));
        existingStudent.setName("Existing Student");
        existingStudent.setStatus("ACTIVE");
        existingStudent.setEmailVerified(true);
        existingStudent.setRoles(Set.of(studentRole));
        existingStudent = userRepository.save(existingStudent);

        course = new Course();
        course.setSlug("flow-course-" + UUID.randomUUID());
        course.setTitle("Flow Course");
        course.setCategory("Java");
        course.setInstructor(teacher);
        course = courseRepository.save(course);
    }

    @Test
    void adminCreatesStudentAndBatchAndTeacherCanSeeAssignedSessions() throws Exception {
        // --- STEP 1: Admin Login ---
        String adminToken = loginToken("admin.flow@test.com", "Password123!");

        // --- STEP 2: Admin creates student via real backend ---
        MvcResult createStudentResult = mockMvc.perform(post("/api/admin/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Student\",\"email\":\"newstudent@test.com\",\"password\":\"Password123!\",\"phone\":\"+91 90000 00000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode studentJson = objectMapper.readTree(createStudentResult.getResponse().getContentAsString());
        String createdStudentId = studentJson.path("data").path("id").asText();

        // Verify student was actually persisted
        assertTrue(userRepository.findByEmail("newstudent@test.com").isPresent(),
                "Student must be persisted in the database");

        // --- STEP 3: Admin creates batch assigned to teacher ---
        MvcResult batchResult = mockMvc.perform(post("/api/admin/batches")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Flow Batch 2026\",\"courseId\":\"" + course.getId() + "\",\"teacherId\":\"" + teacher.getId() + "\",\"capacity\":25,\"status\":\"UPCOMING\",\"startDate\":\"2026-09-01\",\"endDate\":\"2026-12-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode batchJson = objectMapper.readTree(batchResult.getResponse().getContentAsString());
        String batchId = batchJson.path("data").path("id").asText();

        // --- STEP 4: Admin adds student to batch (also creates enrollment) ---
        mockMvc.perform(post("/api/admin/batches/" + batchId + "/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentIds\":[\"" + createdStudentId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify enrollment was auto-created
        UUID createdStudentUUID = UUID.fromString(createdStudentId);
        assertTrue(enrollmentRepository.existsByStudentIdAndCourseId(createdStudentUUID, course.getId()),
                "Enrollment must be auto-created when student is added to batch");

        // --- STEP 5: Teacher Login ---
        String teacherToken = loginToken("teacher.flow@test.com", "Password123!");

        // --- STEP 6: Teacher sees only their assigned batch ---
        MvcResult teacherBatchesResult = mockMvc.perform(get("/api/teacher/batches")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Flow Batch 2026"))
                .andReturn();

        JsonNode teacherBatchesJson = objectMapper.readTree(teacherBatchesResult.getResponse().getContentAsString());
        // Teacher should only see 1 batch (their own)
        assertTrue(teacherBatchesJson.path("data").isArray(), "Batches should be an array");
        assertTrue(teacherBatchesJson.path("data").size() == 1, "Teacher should see exactly 1 batch");

        // --- STEP 7: Teacher sees students in their batch ---
        mockMvc.perform(get("/api/teacher/students")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // --- STEP 8: Other teacher sees NO students from this batch ---
        String otherTeacherToken = loginToken("other.teacher@test.com", "Password123!");
        MvcResult otherTeacherStudentsResult = mockMvc.perform(get("/api/teacher/students")
                        .header("Authorization", "Bearer " + otherTeacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode otherTeacherStudentsJson = objectMapper.readTree(otherTeacherStudentsResult.getResponse().getContentAsString());
        // Other teacher should see no students (they have no batch)
        assertTrue(otherTeacherStudentsJson.path("data").isArray(), "Should return array");
        assertTrue(otherTeacherStudentsJson.path("data").size() == 0,
                "Other teacher must NOT see students from a batch that belongs to a different teacher");

        // --- STEP 9: Teacher creates live class for their batch ---
        MvcResult createLiveClassResult = mockMvc.perform(post("/api/teacher/live-classes")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Flow Live Class\",\"courseId\":\"" + course.getId() + "\",\"batchId\":\"" + batchId + "\",\"classDate\":\"2026-09-05\",\"startTime\":\"09:00\",\"endTime\":\"10:00\",\"platform\":\"Google Meet\",\"meetingUrl\":\"https://meet.google.com/test\",\"visibility\":\"restricted\",\"status\":\"Upcoming\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchName").value("Flow Batch 2026"))
                .andReturn();

        JsonNode liveClassJson = objectMapper.readTree(createLiveClassResult.getResponse().getContentAsString());
        String liveClassId = liveClassJson.path("data").path("id").asText();

        // --- STEP 10: Student login and sees their authorized live class ---
        String studentToken = loginToken("newstudent@test.com", "Password123!");

        MvcResult studentClassesResult = mockMvc.perform(get("/api/student/live-classes")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode studentClassesJson = objectMapper.readTree(studentClassesResult.getResponse().getContentAsString());
        assertTrue(studentClassesJson.path("data").isArray(), "Live classes must be an array");

        // Student must see the live class they are authorized for
        boolean foundClass = false;
        for (JsonNode item : studentClassesJson.path("data")) {
            if (liveClassId.equals(item.path("id").asText())) {
                foundClass = true;
                break;
            }
        }
        assertTrue(foundClass, "Student must see the live class belonging to their batch");

        // --- STEP 11: Existing student (not in batch) should NOT see the restricted class ---
        String existingStudentToken = loginToken("existing.student@test.com", "Password123!");
        MvcResult existingStudentClassesResult = mockMvc.perform(get("/api/student/live-classes")
                        .header("Authorization", "Bearer " + existingStudentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode existingStudentClassesJson = objectMapper.readTree(existingStudentClassesResult.getResponse().getContentAsString());
        boolean unauthorizedStudentSeeClass = false;
        for (JsonNode item : existingStudentClassesJson.path("data")) {
            if (liveClassId.equals(item.path("id").asText())) {
                unauthorizedStudentSeeClass = true;
                break;
            }
        }
        assertFalse(unauthorizedStudentSeeClass,
                "Student not in the batch must NOT see restricted live classes");

        // --- STEP 12: Duplicate email check — must return 409 Conflict ---
        mockMvc.perform(post("/api/admin/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Duplicate\",\"email\":\"newstudent@test.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isConflict());
    }

    private String loginToken(String email, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(login.getResponse().getContentAsString());
        return loginJson.path("data").path("accessToken").asText();
    }
}
