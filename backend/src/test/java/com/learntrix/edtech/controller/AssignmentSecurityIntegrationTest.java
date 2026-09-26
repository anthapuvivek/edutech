package com.learntrix.edtech.controller;

import java.time.Instant;
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
import com.learntrix.edtech.repository.CourseAnnouncementRepository;
import com.learntrix.edtech.repository.AssignmentRepository;
import com.learntrix.edtech.repository.AssignmentSubmissionRepository;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.EnrollmentRepository;
import com.learntrix.edtech.repository.RoleRepository;
import com.learntrix.edtech.repository.UserRepository;

/**
 * Authorization, batch isolation and grading tests for assignments.
 *
 * <pre>
 *   Java Backend
 *     ├── Morning (teacherA) ── studentA
 *     └── Evening (teacherB) ── studentB
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssignmentSecurityIntegrationTest {

    @Autowired
    private AnnouncementReadRepository announcementReadRepositoryCleanup;
    @Autowired
    private CourseAnnouncementRepository courseAnnouncementRepositoryCleanup;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private BatchRepository batchRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private AssignmentSubmissionRepository submissionRepository;

    private Course course;
    private Batch morning;
    private Batch evening;
    private User teacherA;
    private User teacherB;
    private User studentA;
    private User studentB;

    private static final String PW = "Password123!";

    @BeforeEach
    void setUp() {
        // course_announcements.batch_id references batches; clear announcements (and their
        // read rows) before the batch delete below, or H2 rejects it.
        announcementReadRepositoryCleanup.deleteAll();
        courseAnnouncementRepositoryCleanup.deleteAll();
        submissionRepository.deleteAll();
        assignmentRepository.deleteAll();
        enrollmentRepository.deleteAll();
        batchRepository.deleteAll();

        Role adminRole = role("ADMIN");
        Role teacherRole = role("TEACHER");
        Role studentRole = role("STUDENT");

        user("asg.admin@test.com", "Asg Admin", adminRole);
        teacherA = user("asg.teacher.a@test.com", "Asg Teacher A", teacherRole);
        teacherB = user("asg.teacher.b@test.com", "Asg Teacher B", teacherRole);
        studentA = user("asg.student.a@test.com", "Asg Student A", studentRole);
        studentB = user("asg.student.b@test.com", "Asg Student B", studentRole);

        course = course("Java Backend", "java-backend-assignment");
        morning = batch("Morning", course, teacherA, studentA);
        evening = batch("Evening", course, teacherB, studentB);

        enrol(studentA, course, teacherA, morning);
        enrol(studentB, course, teacherB, evening);
    }

    // ================================================================
    // Teacher authoring
    // ================================================================

    @Test
    void teacherCanCreateAssignmentForOwnBatch() throws Exception {
        mockMvc.perform(post("/api/teacher/assignments")
                        .header("Authorization", "Bearer " + token("asg.teacher.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(morning, "Week 1", 50, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchName").value("Morning"))
                // Draft by default: the old code published everything immediately.
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void teacherCannotCreateAssignmentForAnotherTeachersBatch() throws Exception {
        mockMvc.perform(post("/api/teacher/assignments")
                        .header("Authorization", "Bearer " + token("asg.teacher.b@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(morning, "Hijack", 50, null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotEditAnotherTeachersAssignment() throws Exception {
        UUID id = publishedAssignment(token("asg.teacher.a@test.com"), morning, "Owned By A", 50);
        mockMvc.perform(put("/api/teacher/assignments/" + id)
                        .header("Authorization", "Bearer " + token("asg.teacher.b@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(morning, "Tampered", 50, null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotReachTeacherEndpoints() throws Exception {
        mockMvc.perform(get("/api/teacher/assignments")
                        .header("Authorization", "Bearer " + token("asg.student.a@test.com")))
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // Batch isolation
    // ================================================================

    @Test
    void studentSeesOnlyOwnBatchAssignments() throws Exception {
        publishedAssignment(token("asg.teacher.a@test.com"), morning, "Morning Work", 50);

        mockMvc.perform(get("/api/student/assignments")
                        .header("Authorization", "Bearer " + token("asg.student.a@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Morning Work"));

        mockMvc.perform(get("/api/student/assignments")
                        .header("Authorization", "Bearer " + token("asg.student.b@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void studentCannotOpenAnotherBatchesAssignment() throws Exception {
        UUID id = publishedAssignment(token("asg.teacher.a@test.com"), morning, "Morning Only", 50);
        mockMvc.perform(get("/api/student/assignments/" + id)
                        .header("Authorization", "Bearer " + token("asg.student.b@test.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotSubmitToAnotherBatchesAssignment() throws Exception {
        UUID id = publishedAssignment(token("asg.teacher.a@test.com"), morning, "Morning Only", 50);
        mockMvc.perform(post("/api/student/assignments/" + id + "/submit")
                        .header("Authorization", "Bearer " + token("asg.student.b@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submissionUrl\":\"https://github.com/x/y\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void draftAssignmentIsInvisibleToStudents() throws Exception {
        UUID id = draftAssignment(token("asg.teacher.a@test.com"), morning, "Unfinished", 50);

        mockMvc.perform(get("/api/student/assignments")
                        .header("Authorization", "Bearer " + token("asg.student.a@test.com")))
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/api/student/assignments/" + id)
                        .header("Authorization", "Bearer " + token("asg.student.a@test.com")))
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // Grading - the cross-teacher hole
    // ================================================================

    /**
     * The defect this test exists for: assignment id and submission id were verified
     * independently, so a teacher could pair an assignment they own with another
     * teacher's submission and grade a student who was never theirs.
     */
    @Test
    void teacherCannotGradeAnotherAssignmentsSubmission() throws Exception {
        String tokenA = token("asg.teacher.a@test.com");
        String tokenB = token("asg.teacher.b@test.com");

        UUID morningId = publishedAssignment(tokenA, morning, "Morning Work", 50);
        UUID eveningId = publishedAssignment(tokenB, evening, "Evening Work", 50);

        UUID eveningSubmission = submit(eveningId, token("asg.student.b@test.com"));

        // Teacher A owns morningId, and pairs it with Teacher B's submission id.
        mockMvc.perform(post("/api/teacher/assignments/" + morningId
                        + "/submissions/" + eveningSubmission + "/grade")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":50,\"feedback\":\"not mine to grade\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void teacherCannotGradeAnotherTeachersAssignment() throws Exception {
        String tokenB = token("asg.teacher.b@test.com");
        UUID eveningId = publishedAssignment(tokenB, evening, "Evening Work", 50);
        UUID sub = submit(eveningId, token("asg.student.b@test.com"));

        mockMvc.perform(post("/api/teacher/assignments/" + eveningId
                        + "/submissions/" + sub + "/grade")
                        .header("Authorization", "Bearer " + token("asg.teacher.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":10,\"feedback\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    /** A 50-point assignment must not accept 100, which the hardcoded @Max(100) allowed. */
    @Test
    void gradeCannotExceedAssignmentPoints() throws Exception {
        String t = token("asg.teacher.a@test.com");
        UUID id = publishedAssignment(t, morning, "Half Marks", 50);
        UUID sub = submit(id, token("asg.student.a@test.com"));

        mockMvc.perform(post("/api/teacher/assignments/" + id + "/submissions/" + sub + "/grade")
                        .header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":100,\"feedback\":\"too high\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/teacher/assignments/" + id + "/submissions/" + sub + "/grade")
                        .header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":45,\"feedback\":\"good\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grade").value(45));
    }

    @Test
    void studentSeesOwnGradeAndFeedback() throws Exception {
        String t = token("asg.teacher.a@test.com");
        String s = token("asg.student.a@test.com");
        UUID id = publishedAssignment(t, morning, "Graded Work", 50);
        UUID sub = submit(id, s);

        mockMvc.perform(post("/api/teacher/assignments/" + id + "/submissions/" + sub + "/grade")
                        .header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grade\":42,\"feedback\":\"Solid work\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/student/assignments/" + id)
                        .header("Authorization", "Bearer " + s))
                .andExpect(jsonPath("$.data.grade").value(42))
                .andExpect(jsonPath("$.data.feedback").value("Solid work"))
                .andExpect(jsonPath("$.data.submissionStatus").value("Graded"));
    }

    // ================================================================
    // Submission behaviour
    // ================================================================

    /** Lateness is derived from the due date, never sent by the client. */
    @Test
    void submissionAfterDueDateIsMarkedLate() throws Exception {
        String t = token("asg.teacher.a@test.com");
        UUID id = publishedAssignment(t, morning, "Overdue", 50,
                Instant.now().minusSeconds(3600));

        submit(id, token("asg.student.a@test.com"));

        mockMvc.perform(get("/api/student/assignments/" + id)
                        .header("Authorization", "Bearer " + token("asg.student.a@test.com")))
                .andExpect(jsonPath("$.data.submissionStatus").value("Late"));
    }

    /** Resubmitting updates one row - it must not look like two students handed in. */
    @Test
    void resubmittingDoesNotDuplicateSubmissions() throws Exception {
        String t = token("asg.teacher.a@test.com");
        String s = token("asg.student.a@test.com");
        UUID id = publishedAssignment(t, morning, "Resubmit", 50);
        submit(id, s);
        submit(id, s);

        mockMvc.perform(get("/api/teacher/assignments/" + id + "/submissions")
                        .header("Authorization", "Bearer " + t))
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/teacher/assignments/" + id)
                        .header("Authorization", "Bearer " + t))
                .andExpect(jsonPath("$.data.totalStudents").value(1))
                .andExpect(jsonPath("$.data.submittedCount").value(1))
                .andExpect(jsonPath("$.data.pendingCount").value(0));
    }

    @Test
    void teacherCannotReadAnotherTeachersSubmissions() throws Exception {
        UUID id = publishedAssignment(token("asg.teacher.a@test.com"), morning, "A Only", 50);
        mockMvc.perform(get("/api/teacher/assignments/" + id + "/submissions")
                        .header("Authorization", "Bearer " + token("asg.teacher.b@test.com")))
                .andExpect(status().isForbidden());
    }

    /** An assignment with submitted work must not be deleted out from under students. */
    @Test
    void deletingAnAssignmentWithSubmissionsIsRefused() throws Exception {
        String t = token("asg.teacher.a@test.com");
        UUID id = publishedAssignment(t, morning, "Has Work", 50);
        submit(id, token("asg.student.a@test.com"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/teacher/assignments/" + id)
                        .header("Authorization", "Bearer " + t))
                .andExpect(status().isConflict());
    }

    // ================================================================
    // fixtures
    // ================================================================

    private String body(Batch batch, String title, int points, Instant due) {
        return "{\"courseId\":\"" + course.getId() + "\""
                + (batch != null ? ",\"batchId\":\"" + batch.getId() + "\"" : "")
                + ",\"title\":\"" + title + "\",\"description\":\"Do the work.\""
                + ",\"points\":" + points
                + (due != null ? ",\"dueDate\":\"" + due + "\"" : "")
                + "}";
    }

    private UUID draftAssignment(String token, Batch batch, String title, int points) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/teacher/assignments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(batch, title, points, null)))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("id").asText());
    }

    private UUID publishedAssignment(String token, Batch batch, String title, int points)
            throws Exception {
        return publishedAssignment(token, batch, title, points, null);
    }

    private UUID publishedAssignment(String token, Batch batch, String title, int points, Instant due)
            throws Exception {
        MvcResult r = mockMvc.perform(post("/api/teacher/assignments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(batch, title, points, due)))
                .andExpect(status().isOk())
                .andReturn();
        UUID id = UUID.fromString(objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("id").asText());
        mockMvc.perform(put("/api/teacher/assignments/" + id + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        return id;
    }

    private UUID submit(UUID assignmentId, String studentToken) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/student/assignments/" + assignmentId + "/submit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submissionUrl\":\"https://github.com/x/y\",\"notes\":\"done\"}"))
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
