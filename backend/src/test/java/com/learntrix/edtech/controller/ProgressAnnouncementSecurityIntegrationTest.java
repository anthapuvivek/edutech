package com.learntrix.edtech.controller;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learntrix.edtech.entity.Batch;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.Enrollment;
import com.learntrix.edtech.entity.Role;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.AnnouncementReadRepository;
import com.learntrix.edtech.repository.AssignmentRepository;
import com.learntrix.edtech.repository.AssignmentSubmissionRepository;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.CourseAnnouncementRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.EnrollmentRepository;
import com.learntrix.edtech.repository.RoleRepository;
import com.learntrix.edtech.repository.UserRepository;

/**
 * Isolation tests for progress, analytics and announcements.
 *
 * <pre>
 *   Java Backend
 *     ├── Morning (teacherA) ── studentA
 *     └── Evening (teacherB) ── studentB
 * </pre>
 *
 * <p>These three features expose other people's data by nature - a teacher reading a
 * roster, a student reading a noticeboard - so the boundaries matter more here than the
 * numbers do.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProgressAnnouncementSecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private BatchRepository batchRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private CourseAnnouncementRepository announcementRepository;
    @Autowired private AnnouncementReadRepository readRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private AssignmentSubmissionRepository assignmentSubmissionRepository;

    private Course course;
    private Batch morning;
    private Batch evening;
    private User studentA;
    private User studentB;

    private static final String PW = "Password123!";

    @BeforeEach
    void setUp() {
        readRepository.deleteAll();
        announcementRepository.deleteAll();
        assignmentSubmissionRepository.deleteAll();
        assignmentRepository.deleteAll();
        enrollmentRepository.deleteAll();
        batchRepository.deleteAll();

        Role adminRole = role("ADMIN");
        Role teacherRole = role("TEACHER");
        Role studentRole = role("STUDENT");

        user("prg.admin@test.com", "Prg Admin", adminRole);
        User teacherA = user("prg.teacher.a@test.com", "Prg Teacher A", teacherRole);
        User teacherB = user("prg.teacher.b@test.com", "Prg Teacher B", teacherRole);
        studentA = user("prg.student.a@test.com", "Prg Student A", studentRole);
        studentB = user("prg.student.b@test.com", "Prg Student B", studentRole);

        course = course("Java Backend", "java-backend-progress");
        morning = batch("Morning", course, teacherA, studentA);
        evening = batch("Evening", course, teacherB, studentB);

        enrol(studentA, course, teacherA, morning);
        enrol(studentB, course, teacherB, evening);
    }

    // ================================================================
    // Progress
    // ================================================================

    @Test
    void studentSeesOwnProgressOnly() throws Exception {
        mockMvc.perform(get("/api/student/progress")
                        .header("Authorization", "Bearer " + token("prg.student.a@test.com")))
                .andExpect(status().isOk())
                // No id is accepted, so the response can only ever be the caller's own.
                .andExpect(jsonPath("$.data.studentId").value(studentA.getId().toString()));
    }

    @Test
    void teacherSeesOnlyOwnBatchInProgress() throws Exception {
        MvcResult r = mockMvc.perform(get("/api/teacher/progress")
                        .header("Authorization", "Bearer " + token("prg.teacher.a@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].batchName").value("Morning"))
                .andReturn();
        // Evening's student must not appear anywhere in Teacher A's payload.
        org.junit.jupiter.api.Assertions.assertFalse(
                r.getResponse().getContentAsString().contains(studentB.getId().toString()),
                "another batch's student leaked into the teacher progress payload");
    }

    /** A student id is a path variable here, so it is the obvious thing to tamper with. */
    @Test
    void teacherCannotReadAnotherBatchesStudentProgress() throws Exception {
        mockMvc.perform(get("/api/teacher/progress/" + studentB.getId())
                        .header("Authorization", "Bearer " + token("prg.teacher.a@test.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCanReadOwnStudentProgress() throws Exception {
        mockMvc.perform(get("/api/teacher/progress/" + studentA.getId())
                        .header("Authorization", "Bearer " + token("prg.teacher.a@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentId").value(studentA.getId().toString()));
    }

    @Test
    void studentCannotReachTeacherProgress() throws Exception {
        mockMvc.perform(get("/api/teacher/progress")
                        .header("Authorization", "Bearer " + token("prg.student.a@test.com")))
                .andExpect(status().isForbidden());
    }

    /** Attendance has no table; it must be named as unavailable, not silently zero. */
    @Test
    void progressNamesAttendanceAsUnavailable() throws Exception {
        MvcResult r = mockMvc.perform(get("/api/student/progress")
                        .header("Authorization", "Bearer " + token("prg.student.a@test.com")))
                .andExpect(status().isOk())
                .andReturn();
        org.junit.jupiter.api.Assertions.assertTrue(
                r.getResponse().getContentAsString().contains("Attendance is not tracked"),
                "attendance should be reported as unavailable rather than fabricated");
    }

    // ================================================================
    // Analytics
    // ================================================================

    @Test
    void studentSeesOwnAnalyticsOnly() throws Exception {
        mockMvc.perform(get("/api/student/analytics")
                        .header("Authorization", "Bearer " + token("prg.student.a@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scope").value("STUDENT"))
                .andExpect(jsonPath("$.data.subjectId").value(studentA.getId().toString()));
    }

    @Test
    void teacherAnalyticsCoversOnlyOwnStudents() throws Exception {
        mockMvc.perform(get("/api/teacher/analytics")
                        .header("Authorization", "Bearer " + token("prg.teacher.a@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scope").value("TEACHER"))
                // Morning has exactly one student; Evening's must not be counted.
                .andExpect(jsonPath("$.data.studentCount").value(1));
    }

    @Test
    void studentCannotReachTeacherAnalytics() throws Exception {
        mockMvc.perform(get("/api/teacher/analytics")
                        .header("Authorization", "Bearer " + token("prg.student.a@test.com")))
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // Announcements
    // ================================================================

    @Test
    void announcementToOneBatchIsHiddenFromTheOther() throws Exception {
        create(token("prg.teacher.a@test.com"), morning, "Morning notice");

        mockMvc.perform(get("/api/student/announcements")
                        .header("Authorization", "Bearer " + token("prg.student.a@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Morning notice"));

        mockMvc.perform(get("/api/student/announcements")
                        .header("Authorization", "Bearer " + token("prg.student.b@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void teacherCannotAnnounceToAnotherTeachersBatch() throws Exception {
        String body = "{\"courseId\":\"" + course.getId() + "\",\"batchId\":\"" + morning.getId()
                + "\",\"title\":\"Hijack\",\"content\":\"nope\"}";
        mockMvc.perform(post("/api/teacher/announcements")
                        .header("Authorization", "Bearer " + token("prg.teacher.b@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotDeleteAnotherTeachersAnnouncement() throws Exception {
        UUID id = create(token("prg.teacher.a@test.com"), morning, "Owned by A");
        mockMvc.perform(delete("/api/teacher/announcements/" + id)
                        .header("Authorization", "Bearer " + token("prg.teacher.b@test.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotOpenAnotherBatchesAnnouncement() throws Exception {
        UUID id = create(token("prg.teacher.a@test.com"), morning, "Morning only");
        mockMvc.perform(get("/api/student/announcements/" + id)
                        .header("Authorization", "Bearer " + token("prg.student.b@test.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unreadCountFallsWhenAnAnnouncementIsRead() throws Exception {
        UUID id = create(token("prg.teacher.a@test.com"), morning, "Read me");
        String s = token("prg.student.a@test.com");

        mockMvc.perform(get("/api/student/announcements/unread-count")
                        .header("Authorization", "Bearer " + s))
                .andExpect(jsonPath("$.data.unread").value(1));

        mockMvc.perform(put("/api/student/announcements/" + id + "/read")
                        .header("Authorization", "Bearer " + s))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));

        mockMvc.perform(get("/api/student/announcements/unread-count")
                        .header("Authorization", "Bearer " + s))
                .andExpect(jsonPath("$.data.unread").value(0));
    }

    /** Re-reading must stay idempotent, or the read table grows without bound. */
    @Test
    void markingReadTwiceDoesNotDuplicateRows() throws Exception {
        UUID id = create(token("prg.teacher.a@test.com"), morning, "Twice");
        String s = token("prg.student.a@test.com");
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(put("/api/student/announcements/" + id + "/read")
                            .header("Authorization", "Bearer " + s))
                    .andExpect(status().isOk());
        }
        org.junit.jupiter.api.Assertions.assertEquals(1, readRepository.count(),
                "one student reading one announcement must produce exactly one row");
    }

    /** Reading is per student: B must not inherit A's read state. */
    @Test
    void readStateIsPerStudent() throws Exception {
        // Course-wide so both cohorts can see it.
        UUID id = create(token("prg.teacher.a@test.com"), null, "Everyone");
        mockMvc.perform(put("/api/student/announcements/" + id + "/read")
                        .header("Authorization", "Bearer " + token("prg.student.a@test.com")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/student/announcements/unread-count")
                        .header("Authorization", "Bearer " + token("prg.student.b@test.com")))
                .andExpect(jsonPath("$.data.unread").value(1));
    }

    @Test
    void studentCannotReachTeacherAnnouncementManagement() throws Exception {
        mockMvc.perform(get("/api/teacher/announcements")
                        .header("Authorization", "Bearer " + token("prg.student.a@test.com")))
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // fixtures
    // ================================================================

    private UUID create(String token, Batch batch, String title) throws Exception {
        String body = "{\"courseId\":\"" + course.getId() + "\""
                + (batch != null ? ",\"batchId\":\"" + batch.getId() + "\"" : "")
                + ",\"title\":\"" + title + "\",\"content\":\"Body text.\",\"priority\":\"NORMAL\"}";
        MvcResult r = mockMvc.perform(post("/api/teacher/announcements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("id").asText());
    }

    private Role role(String name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role r = new Role();
            r.setName(name);
            r.setDisplayName(name);
            return roleRepository.save(r);
        });
    }

    private User user(String email, String name, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setName(name);
            u.setPasswordHash(passwordEncoder.encode(PW));
            u.setStatus("ACTIVE");
            u.setEmailVerified(true);
            u.setRoles(Set.of(role));
            return userRepository.save(u);
        });
    }

    private Course course(String title, String slug) {
        return courseRepository.findBySlug(slug).orElseGet(() -> {
            Course c = new Course();
            c.setTitle(title);
            c.setSlug(slug);
            c.setCategory("Engineering");
            c.setStatus("PUBLISHED");
            return courseRepository.save(c);
        });
    }

    private Batch batch(String name, Course c, User teacher, User student) {
        Batch b = new Batch();
        b.setName(name);
        b.setCourse(c);
        b.setTeacher(teacher);
        b.setCapacity(10);
        b.setStatus("ACTIVE");
        b.getStudents().add(student);
        return batchRepository.save(b);
    }

    private void enrol(User student, Course c, User teacher, Batch b) {
        Enrollment e = new Enrollment();
        e.setStudent(student);
        e.setCourse(c);
        e.setTeacher(teacher);
        e.setBatch(b);
        e.setStatus("ACTIVE");
        enrollmentRepository.save(e);
    }

    private String token(String email) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PW + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }
}
