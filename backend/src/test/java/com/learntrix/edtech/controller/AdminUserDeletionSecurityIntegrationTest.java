package com.learntrix.edtech.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learntrix.edtech.entity.AccountActivationToken;
import com.learntrix.edtech.entity.Batch;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.Enrollment;
import com.learntrix.edtech.entity.Role;
import com.learntrix.edtech.entity.StudentProfile;
import com.learntrix.edtech.entity.TeacherProfile;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.AccountActivationTokenRepository;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.EnrollmentRepository;
import com.learntrix.edtech.repository.RoleRepository;
import com.learntrix.edtech.repository.StudentProfileRepository;
import com.learntrix.edtech.repository.TeacherProfileRepository;
import com.learntrix.edtech.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserDeletionSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private TeacherProfileRepository teacherProfileRepository;

    @Autowired
    private AccountActivationTokenRepository activationTokenRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User admin;
    private User otherAdmin;
    private User teacher;
    private User student;
    private Role adminRole;
    private Role teacherRole;
    private Role studentRole;

    @BeforeEach
    void setUp() {
        activationTokenRepository.deleteAll();
        enrollmentRepository.deleteAll();
        batchRepository.deleteAll();
        studentProfileRepository.deleteAll();
        teacherProfileRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
            Role r = new Role();
            r.setName("ADMIN");
            r.setDisplayName("Administrator");
            return roleRepository.save(r);
        });

        teacherRole = roleRepository.findByName("TEACHER").orElseGet(() -> {
            Role r = new Role();
            r.setName("TEACHER");
            r.setDisplayName("Teacher");
            return roleRepository.save(r);
        });

        studentRole = roleRepository.findByName("STUDENT").orElseGet(() -> {
            Role r = new Role();
            r.setName("STUDENT");
            r.setDisplayName("Student");
            return roleRepository.save(r);
        });

        admin = new User();
        admin.setEmail("admin.del@test.com");
        admin.setPasswordHash(passwordEncoder.encode("Password123!"));
        admin.setName("Admin Del");
        admin.setStatus("ACTIVE");
        admin.setEmailVerified(true);
        admin.setRoles(Set.of(adminRole));
        admin = userRepository.save(admin);

        otherAdmin = new User();
        otherAdmin.setEmail("otheradmin.del@test.com");
        otherAdmin.setPasswordHash(passwordEncoder.encode("Password123!"));
        otherAdmin.setName("Other Admin");
        otherAdmin.setStatus("ACTIVE");
        otherAdmin.setEmailVerified(true);
        otherAdmin.setRoles(Set.of(adminRole));
        otherAdmin = userRepository.save(otherAdmin);

        teacher = new User();
        teacher.setEmail("teacher.del@test.com");
        teacher.setPasswordHash(passwordEncoder.encode("Password123!"));
        teacher.setName("Teacher Del");
        teacher.setStatus("ACTIVE");
        teacher.setEmailVerified(true);
        teacher.setRoles(Set.of(teacherRole));
        teacher = userRepository.save(teacher);

        TeacherProfile tp = new TeacherProfile();
        tp.setId(UUID.randomUUID());
        tp.setUserId(teacher.getId());
        tp.setEmployeeId("LTX-T-DEL-01");
        tp.setFullName("Teacher Del");
        tp.setApprovalStatus("approved");
        teacherProfileRepository.save(tp);

        student = new User();
        student.setEmail("student.del@test.com");
        student.setPasswordHash(passwordEncoder.encode("Password123!"));
        student.setName("Student Del");
        student.setStatus("ACTIVE");
        student.setEmailVerified(true);
        student.setRoles(Set.of(studentRole));
        student = userRepository.save(student);

        StudentProfile sp = new StudentProfile();
        sp.setId(UUID.randomUUID());
        sp.setUserId(student.getId());
        sp.setStudentId("LTX-DEL-01");
        studentProfileRepository.save(sp);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
    }

    @Test
    void adminCanDeactivateStudentSuccessfully() throws Exception {
        String adminToken = loginAndGetToken("admin.del@test.com", "Password123!");

        // Add an activation token for this student
        AccountActivationToken token = new AccountActivationToken();
        token.setUserId(student.getId());
        token.setToken("sample-active-token");
        token.setTokenType("ONBOARDING_ACTIVATION");
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        activationTokenRepository.save(token);

        mockMvc.perform(delete("/api/admin/students/" + student.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        User updated = userRepository.findById(student.getId()).orElseThrow();
        assertEquals("INACTIVE", updated.getStatus());

        // Verify tokens are invalidated
        List<AccountActivationToken> tokens = activationTokenRepository.findAll();
        for (AccountActivationToken t : tokens) {
            if (t.getUserId().equals(student.getId())) {
                assertNotNull(t.getUsedAt(), "Token should be marked as invalidated/used");
            }
        }
    }

    @Test
    void adminCanDeactivateTeacherSuccessfully() throws Exception {
        String adminToken = loginAndGetToken("admin.del@test.com", "Password123!");

        mockMvc.perform(delete("/api/admin/teachers/" + teacher.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        User updated = userRepository.findById(teacher.getId()).orElseThrow();
        assertEquals("INACTIVE", updated.getStatus());

        TeacherProfile tp = teacherProfileRepository.findByUserId(teacher.getId()).orElseThrow();
        assertEquals("deactivated", tp.getApprovalStatus());
    }

    @Test
    void studentCannotDeleteStudent() throws Exception {
        String studentToken = loginAndGetToken("student.del@test.com", "Password123!");

        mockMvc.perform(delete("/api/admin/students/" + student.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotDeleteTeacher() throws Exception {
        String studentToken = loginAndGetToken("student.del@test.com", "Password123!");

        mockMvc.perform(delete("/api/admin/teachers/" + teacher.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotDeleteStudent() throws Exception {
        String teacherToken = loginAndGetToken("teacher.del@test.com", "Password123!");

        mockMvc.perform(delete("/api/admin/students/" + student.getId())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotDeleteTeacher() throws Exception {
        String teacherToken = loginAndGetToken("teacher.del@test.com", "Password123!");

        mockMvc.perform(delete("/api/admin/teachers/" + teacher.getId())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(delete("/api/admin/students/" + student.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCannotDeleteAnotherAdmin() throws Exception {
        String adminToken = loginAndGetToken("admin.del@test.com", "Password123!");

        mockMvc.perform(delete("/api/admin/students/" + otherAdmin.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CANNOT_DELETE_ADMIN"));
    }

    @Test
    void nonExistingUserReturns404() throws Exception {
        String adminToken = loginAndGetToken("admin.del@test.com", "Password123!");

        mockMvc.perform(delete("/api/admin/students/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void deactivatedUserCannotLogin() throws Exception {
        String adminToken = loginAndGetToken("admin.del@test.com", "Password123!");

        mockMvc.perform(delete("/api/admin/students/" + student.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Try logging in with the deactivated user credentials
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "student.del@test.com", "password", "Password123!"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("INACTIVE_USER"));
    }

    @Test
    void batchAndEnrollmentRelationshipsHandledSafelyOnDeactivation() throws Exception {
        String adminToken = loginAndGetToken("admin.del@test.com", "Password123!");

        Course course = new Course();
        course.setSlug("del-course-" + UUID.randomUUID());
        course.setTitle("Del Test Course");
        course.setCategory("Java");
        course.setInstructor(teacher);
        course = courseRepository.save(course);

        Batch batch = new Batch();
        batch.setName("Batch Del");
        batch.setCourse(course);
        batch.setTeacher(teacher);
        batch.setCapacity(30);
        batch.setStatus("IN_PROGRESS");
        batch.setStartDate(LocalDate.now());
        batch.setEndDate(LocalDate.now().plusMonths(3));
        Set<User> batchStudents = new HashSet<>();
        batchStudents.add(student);
        batch.setStudents(batchStudents);
        batch = batchRepository.save(batch);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setTeacher(teacher);
        enrollment.setBatch(batch);
        enrollment.setStatus("ACTIVE");
        enrollmentRepository.save(enrollment);

        // Perform deletion/deactivation
        mockMvc.perform(delete("/api/admin/students/" + student.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Verify batch no longer contains student
        List<Batch> remainingBatches = batchRepository.findByStudentsContaining(student);
        assertTrue(remainingBatches.isEmpty(), "Student should be detached from batches");

        // Verify enrollment is marked INACTIVE
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());
        assertEquals("INACTIVE", enrollments.get(0).getStatus());
    }
}
