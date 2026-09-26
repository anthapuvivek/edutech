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
import com.learntrix.edtech.repository.CourseAnnouncementRepository;
import com.learntrix.edtech.repository.AssignmentRepository;
import com.learntrix.edtech.repository.AssignmentSubmissionRepository;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.CodingProblemCompletionRepository;
import com.learntrix.edtech.repository.CodingProblemRepository;
import com.learntrix.edtech.repository.CodingSubmissionRepository;
import com.learntrix.edtech.repository.CodingTestCaseRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.EnrollmentRepository;
import com.learntrix.edtech.repository.RoleRepository;
import com.learntrix.edtech.repository.UserRepository;

/**
 * Authorization, batch isolation, URL validation and completion tests for externally
 * hosted coding problems.
 *
 * <pre>
 *   Java Backend
 *     ├── Morning (teacherA) ── studentA
 *     └── Evening (teacherB) ── studentB
 * </pre>
 *
 * <p>No judge is involved anywhere here - that is the point of the feature.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExternalCodingSecurityIntegrationTest {

    @Autowired
    private AnnouncementReadRepository announcementReadRepositoryCleanup;
    @Autowired
    private CourseAnnouncementRepository courseAnnouncementRepositoryCleanup;

    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private BatchRepository batchRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private CodingProblemRepository problemRepository;
    @Autowired private CodingProblemCompletionRepository completionRepository;
    @Autowired private CodingTestCaseRepository testCaseRepository;
    @Autowired private CodingSubmissionRepository submissionRepository;

    private Course course;
    private Batch morning;
    private Batch evening;
    private User teacherA;
    private User teacherB;
    private User studentA;
    private User studentB;

    private static final String PW = "Password123!";
    private static final String URL = "https://leetcode.com/problems/two-sum/";

    @BeforeEach
    void setUp() {
        // course_announcements.batch_id references batches; clear announcements (and their
        // read rows) before the batch delete below, or H2 rejects it.
        announcementReadRepositoryCleanup.deleteAll();
        courseAnnouncementRepositoryCleanup.deleteAll();
        completionRepository.deleteAll();
        submissionRepository.deleteAll();
        testCaseRepository.deleteAll();
        problemRepository.deleteAll();
        // assignments.batch_id blocks batch deletion in the JPA-generated test schema
        // (production Postgres has ON DELETE SET NULL, see V34). Children first.
        assignmentSubmissionRepository.deleteAll();
        assignmentRepository.deleteAll();
        enrollmentRepository.deleteAll();
        batchRepository.deleteAll();

        Role adminRole = role("ADMIN");
        Role teacherRole = role("TEACHER");
        Role studentRole = role("STUDENT");

        user("ext.admin@test.com", "Ext Admin", adminRole);
        teacherA = user("ext.teacher.a@test.com", "Ext Teacher A", teacherRole);
        teacherB = user("ext.teacher.b@test.com", "Ext Teacher B", teacherRole);
        studentA = user("ext.student.a@test.com", "Ext Student A", studentRole);
        studentB = user("ext.student.b@test.com", "Ext Student B", studentRole);

        course = course("Java Backend", "java-backend-external");
        morning = batch("Morning", course, teacherA, studentA);
        evening = batch("Evening", course, teacherB, studentB);

        enrol(studentA, course, teacherA, morning);
        enrol(studentB, course, teacherB, evening);
    }

    // ================================================================
    // Teacher authoring
    // ================================================================

    @Test
    void teacherCanAssignProblemToOwnBatch() throws Exception {
        mockMvc.perform(post("/api/teacher/coding-assignments")
                        .header("Authorization", "Bearer " + token("ext.teacher.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(morning, URL, "Two Sum")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platform").value("LEETCODE"))
                .andExpect(jsonPath("$.data.problemUrl").value(URL))
                .andExpect(jsonPath("$.data.batchName").value("Morning"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void teacherCannotAssignToAnotherTeachersBatch() throws Exception {
        mockMvc.perform(post("/api/teacher/coding-assignments")
                        .header("Authorization", "Bearer " + token("ext.teacher.b@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(morning, URL, "Hijack")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotEditAnotherTeachersProblem() throws Exception {
        UUID id = create(token("ext.teacher.a@test.com"), morning, URL, "Owned By A");
        mockMvc.perform(put("/api/teacher/coding-assignments/" + id)
                        .header("Authorization", "Bearer " + token("ext.teacher.b@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(morning, URL, "Tampered")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotDeleteAnotherTeachersProblem() throws Exception {
        UUID id = create(token("ext.teacher.a@test.com"), morning, URL, "Owned By A");
        mockMvc.perform(delete("/api/teacher/coding-assignments/" + id)
                        .header("Authorization", "Bearer " + token("ext.teacher.b@test.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotReachTeacherEndpoints() throws Exception {
        mockMvc.perform(get("/api/teacher/coding-assignments")
                        .header("Authorization", "Bearer " + token("ext.student.a@test.com")))
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // URL validation
    // ================================================================

    /** An allow list of schemes, so javascript: is rejected without being named. */
    @Test
    void javascriptUrlIsRejected() throws Exception {
        mockMvc.perform(post("/api/teacher/coding-assignments")
                        .header("Authorization", "Bearer " + token("ext.teacher.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(morning, "javascript:alert(1)", "Nasty")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonHttpSchemesAreRejected() throws Exception {
        String t = token("ext.teacher.a@test.com");
        for (String bad : new String[]{"data:text/html,<script>", "file:///etc/passwd", "ftp://x.com/a"}) {
            mockMvc.perform(post("/api/teacher/coding-assignments")
                            .header("Authorization", "Bearer " + t)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(morning, bad, "Bad scheme")))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void urlWithoutHostIsRejected() throws Exception {
        mockMvc.perform(post("/api/teacher/coding-assignments")
                        .header("Authorization", "Bearer " + token("ext.teacher.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(morning, "https://", "No host")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingTitleIsRejected() throws Exception {
        String json = "{\"courseId\":\"" + course.getId() + "\",\"platform\":\"LEETCODE\","
                + "\"problemUrl\":\"" + URL + "\"}";
        mockMvc.perform(post("/api/teacher/coding-assignments")
                        .header("Authorization", "Bearer " + token("ext.teacher.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownPlatformIsRejected() throws Exception {
        String json = "{\"courseId\":\"" + course.getId() + "\",\"platform\":\"MYSPACE\","
                + "\"problemUrl\":\"" + URL + "\",\"title\":\"X\"}";
        mockMvc.perform(post("/api/teacher/coding-assignments")
                        .header("Authorization", "Bearer " + token("ext.teacher.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // ================================================================
    // Batch isolation
    // ================================================================

    @Test
    void studentSeesOnlyOwnBatchProblems() throws Exception {
        create(token("ext.teacher.a@test.com"), morning, URL, "Morning Problem");

        mockMvc.perform(get("/api/student/coding-assignments")
                        .header("Authorization", "Bearer " + token("ext.student.a@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Morning Problem"));

        mockMvc.perform(get("/api/student/coding-assignments")
                        .header("Authorization", "Bearer " + token("ext.student.b@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    /** Changing the id in the URL must not work either. */
    @Test
    void studentCannotOpenAnotherBatchesProblemById() throws Exception {
        UUID id = create(token("ext.teacher.a@test.com"), morning, URL, "Morning Only");
        mockMvc.perform(get("/api/student/coding-assignments/" + id)
                        .header("Authorization", "Bearer " + token("ext.student.b@test.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void inactiveProblemIsHiddenFromStudents() throws Exception {
        String t = token("ext.teacher.a@test.com");
        UUID id = create(t, morning, URL, "Draft Problem");
        mockMvc.perform(put("/api/teacher/coding-assignments/" + id + "/active")
                        .header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(get("/api/student/coding-assignments")
                        .header("Authorization", "Bearer " + token("ext.student.a@test.com")))
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/api/student/coding-assignments/" + id)
                        .header("Authorization", "Bearer " + token("ext.student.a@test.com")))
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // Completion
    // ================================================================

    @Test
    void studentCanToggleOwnCompletion() throws Exception {
        UUID id = create(token("ext.teacher.a@test.com"), morning, URL, "Two Sum");
        String s = token("ext.student.a@test.com");

        mockMvc.perform(put("/api/student/coding-assignments/" + id + "/completion")
                        .header("Authorization", "Bearer " + s)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(true));

        // Survives a re-read, which is what the student sees after a refresh.
        mockMvc.perform(get("/api/student/coding-assignments/" + id)
                        .header("Authorization", "Bearer " + s))
                .andExpect(jsonPath("$.data.completed").value(true));

        mockMvc.perform(put("/api/student/coding-assignments/" + id + "/completion")
                        .header("Authorization", "Bearer " + s)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":false}"))
                .andExpect(jsonPath("$.data.completed").value(false));
    }

    /** Toggling repeatedly must not accumulate rows - the unique constraint holds. */
    @Test
    void togglingDoesNotDuplicateCompletionRows() throws Exception {
        UUID id = create(token("ext.teacher.a@test.com"), morning, URL, "Two Sum");
        String s = token("ext.student.a@test.com");
        for (boolean v : new boolean[]{true, false, true, true}) {
            mockMvc.perform(put("/api/student/coding-assignments/" + id + "/completion")
                            .header("Authorization", "Bearer " + s)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"completed\":" + v + "}"))
                    .andExpect(status().isOk());
        }
        org.junit.jupiter.api.Assertions.assertEquals(1, completionRepository.count(),
                "one student on one problem must have exactly one completion row");
    }

    /** Student B cannot mark a problem they cannot even see. */
    @Test
    void studentCannotCompleteAnotherBatchesProblem() throws Exception {
        UUID id = create(token("ext.teacher.a@test.com"), morning, URL, "Morning Only");
        mockMvc.perform(put("/api/student/coding-assignments/" + id + "/completion")
                        .header("Authorization", "Bearer " + token("ext.student.b@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isForbidden());
    }

    /** One student's completion must not appear on another student's listing. */
    @Test
    void completionIsPerStudent() throws Exception {
        String t = token("ext.teacher.a@test.com");
        // Put a second enrolled student in the same batch: progress remains per student.
        morning.getStudents().add(studentB);
        batchRepository.save(morning);
        UUID id = create(t, morning, URL, "Shared Problem");

        mockMvc.perform(put("/api/student/coding-assignments/" + id + "/completion")
                        .header("Authorization", "Bearer " + token("ext.student.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/student/coding-assignments/" + id)
                        .header("Authorization", "Bearer " + token("ext.student.b@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completed").value(false));
    }

    @Test
    void teacherSeesHowManyStudentsCompleted() throws Exception {
        String t = token("ext.teacher.a@test.com");
        UUID id = create(t, morning, URL, "Counted");
        mockMvc.perform(put("/api/student/coding-assignments/" + id + "/completion")
                        .header("Authorization", "Bearer " + token("ext.student.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/teacher/coding-assignments/" + id)
                        .header("Authorization", "Bearer " + t))
                .andExpect(jsonPath("$.data.completedCount").value(1));
    }

    @Test
    void openingRecordsProgressAndTeacherCanSeeBatchRoster() throws Exception {
        String teacherToken = token("ext.teacher.a@test.com");
        UUID id = create(teacherToken, morning, URL, "Opened Problem");

        mockMvc.perform(post("/api/student/coding-assignments/" + id + "/open")
                        .header("Authorization", "Bearer " + token("ext.student.a@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.progressStatus").value("OPENED"))
                .andExpect(jsonPath("$.data.openedAt").isNotEmpty());

        mockMvc.perform(get("/api/teacher/coding-assignments/" + id + "/progress")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalStudents").value(1))
                .andExpect(jsonPath("$.data.inProgress").value(1))
                .andExpect(jsonPath("$.data.students[0].status").value("OPENED"));
    }

    @Test
    void completionEndpointUsesAuthenticatedStudent() throws Exception {
        UUID id = create(token("ext.teacher.a@test.com"), morning, URL, "Complete Problem");
        mockMvc.perform(post("/api/student/coding-assignments/" + id + "/complete")
                        .header("Authorization", "Bearer " + token("ext.student.a@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.progressStatus").value("COMPLETED"));
        org.junit.jupiter.api.Assertions.assertEquals(studentA.getId(),
                completionRepository.findByProblemIdAndStudentId(id, studentA.getId()).orElseThrow()
                        .getStudent().getId());
    }

    // ================================================================
    // No execution involved
    // ================================================================

    /** The student payload carries a link, never test data or a judge verdict. */
    @Test
    void studentPayloadCarriesNoExecutionData() throws Exception {
        UUID id = create(token("ext.teacher.a@test.com"), morning, URL, "Two Sum");
        MvcResult r = mockMvc.perform(get("/api/student/coding-assignments/" + id)
                        .header("Authorization", "Bearer " + token("ext.student.a@test.com")))
                .andExpect(status().isOk())
                .andReturn();
        String body = r.getResponse().getContentAsString();
        assertTrue(body.contains(URL), "the link is the point of the feature");
        assertFalse(body.contains("testCases"), "no test cases in an external problem");
        assertFalse(body.contains("expectedOutput"), "no expected output in an external problem");
        assertFalse(body.contains("sourceCode"), "no source code in an external problem");
    }

    // ================================================================
    // fixtures
    // ================================================================

    private String body(Batch batch, String url, String title) {
        return "{\"courseId\":\"" + course.getId() + "\""
                + (batch != null ? ",\"batchId\":\"" + batch.getId() + "\"" : "")
                + ",\"platform\":\"LEETCODE\",\"problemNumber\":\"1\""
                + ",\"problemUrl\":\"" + url + "\",\"title\":\"" + title + "\""
                + ",\"difficulty\":\"EASY\",\"topics\":\"Arrays, Hash Table\""
                + ",\"description\":\"Solve it and bring questions to class.\"}";
    }

    private UUID create(String token, Batch batch, String url, String title) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/teacher/coding-assignments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(batch, url, title)))
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
