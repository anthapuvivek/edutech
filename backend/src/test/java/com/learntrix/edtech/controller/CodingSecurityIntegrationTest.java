package com.learntrix.edtech.controller;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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
import com.learntrix.edtech.execution.CodeExecutionClient;
import com.learntrix.edtech.execution.ExecutionResult;
import com.learntrix.edtech.repository.AnnouncementReadRepository;
import com.learntrix.edtech.repository.CourseAnnouncementRepository;
import com.learntrix.edtech.repository.AssignmentRepository;
import com.learntrix.edtech.repository.AssignmentSubmissionRepository;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.CodingProblemRepository;
import com.learntrix.edtech.repository.CodingSubmissionRepository;
import com.learntrix.edtech.repository.CodingTestCaseRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.EnrollmentRepository;
import com.learntrix.edtech.repository.RoleRepository;
import com.learntrix.edtech.repository.UserRepository;

/**
 * Authorization, cohort isolation and hidden-test-case tests for coding practice.
 *
 * <p>Scenario mirrors {@link QuizSecurityIntegrationTest}, because both features must obey
 * the same cohort boundary:</p>
 * <pre>
 *   Java Backend
 *     ├── Morning (teacherA) ── studentA
 *     └── Evening (teacherB) ── studentB
 * </pre>
 *
 * <p>The judge is stubbed so verdicts are deterministic. That is the point of
 * {@link CodeExecutionClient} being an interface: no test ever runs real student code, and
 * neither does the application.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CodingSecurityIntegrationTest {

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
    @Autowired private CodingTestCaseRepository testCaseRepository;
    @Autowired private CodingSubmissionRepository submissionRepository;

    /**
     * Replaces Judge0 entirely - no network, no sandbox, no real execution in tests.
     *
     * <p>Hand-written rather than mocked: Byte Buddy cannot instrument classes on Java 25,
     * so a Mockito mock of this interface fails to build at runtime. A stub also states the
     * judge's contract more plainly than a stack of {@code when(...)} calls.</p>
     */
    @Autowired private CodeExecutionClient executionClient;

    private StubExecutionClient judge() {
        return (StubExecutionClient) executionClient;
    }

    @TestConfiguration
    static class StubJudgeConfig {
        @Bean
        @Primary
        CodeExecutionClient stubExecutionClient() {
            return new StubExecutionClient();
        }
    }

    /** A judge whose verdict each test decides, keyed on the stdin it was handed. */
    static class StubExecutionClient implements CodeExecutionClient {
        volatile Function<String, ExecutionResult> behaviour = StubExecutionClient::solvesEveryCase;

        /** A correct program: prints whatever the case expects. */
        static ExecutionResult solvesEveryCase(String stdin) {
            return success(HIDDEN_IN.equals(stdin) ? HIDDEN_OUT : SAMPLE_OUT);
        }

        static ExecutionResult success(String stdout) {
            return ExecutionResult.builder()
                    .status(ExecutionResult.Status.SUCCESS)
                    .stdout(stdout)
                    .runtimeMs(12)
                    .build();
        }

        @Override
        public ExecutionResult execute(String language, String sourceCode, String stdin,
                                       int timeLimitMs, int memoryLimitMb) {
            return behaviour.apply(stdin);
        }

        @Override
        public boolean isConfigured() {
            return true;
        }
    }

    private Course course;
    private Batch morning;
    private Batch evening;
    private User teacherA;
    private User teacherB;
    private User studentA;
    private User studentB;

    private static final String PW = "Password123!";

    /**
     * Sentinels, not realistic values.
     *
     * <p>The leak assertions below do a substring search over the whole JSON response, so a
     * short value like "42" would match by chance inside a UUID or timestamp and report a
     * leak that is not there. These strings cannot occur incidentally.</p>
     */
    private static final String SAMPLE_IN = "SAMPLE_INPUT_Q7";
    private static final String SAMPLE_OUT = "SAMPLE_EXPECTED_Q7";
    private static final String HIDDEN_IN = "HIDDEN_INPUT_Z9";
    private static final String HIDDEN_OUT = "HIDDEN_EXPECTED_Z9";

    @BeforeEach
    void setUp() {
        // course_announcements.batch_id references batches; clear announcements (and their
        // read rows) before the batch delete below, or H2 rejects it.
        announcementReadRepositoryCleanup.deleteAll();
        courseAnnouncementRepositoryCleanup.deleteAll();
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

        user("coding.admin@test.com", "Coding Admin", adminRole);
        teacherA = user("coding.teacher.a@test.com", "Coding Teacher A", teacherRole);
        teacherB = user("coding.teacher.b@test.com", "Coding Teacher B", teacherRole);
        studentA = user("coding.student.a@test.com", "Coding Student A", studentRole);
        studentB = user("coding.student.b@test.com", "Coding Student B", studentRole);

        course = course("Java Backend", "java-backend-coding");

        morning = batch("Morning", course, teacherA, studentA);
        evening = batch("Evening", course, teacherB, studentB);

        enrol(studentA, course, teacherA, morning);
        enrol(studentB, course, teacherB, evening);

        // Default: a correct solution. Individual tests override this.
        judge().behaviour = StubExecutionClient::solvesEveryCase;
    }

    // ================================================================
    // Teacher authoring
    // ================================================================

    @Test
    void teacherCanCreateProblemForOwnBatch() throws Exception {
        String t = token("coding.teacher.a@test.com");
        mockMvc.perform(post("/api/teacher/coding-problems")
                        .header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(problemBody(morning, "Add Two Numbers")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.batchName").value("Morning"));
    }

    /** Teacher B teaches the same course but a different cohort. */
    @Test
    void teacherCannotCreateProblemForAnotherTeachersBatch() throws Exception {
        String t = token("coding.teacher.b@test.com");
        mockMvc.perform(post("/api/teacher/coding-problems")
                        .header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(problemBody(morning, "Hijack Attempt")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotAddTestCaseToAnotherTeachersProblem() throws Exception {
        UUID problemId = draftProblem(token("coding.teacher.a@test.com"), morning, "Owned By A");
        mockMvc.perform(post("/api/teacher/coding-problems/" + problemId + "/test-cases")
                        .header("Authorization", "Bearer " + token("coding.teacher.b@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testCaseBody(SAMPLE_IN, SAMPLE_OUT, true)))
                .andExpect(status().isForbidden());
    }

    /** A problem with no cases cannot be judged, so it must not reach students. */
    @Test
    void publishIsRefusedWhenProblemHasNoTestCases() throws Exception {
        String t = token("coding.teacher.a@test.com");
        UUID problemId = draftProblem(t, morning, "Empty Problem");
        mockMvc.perform(put("/api/teacher/coding-problems/" + problemId + "/publish")
                        .header("Authorization", "Bearer " + t))
                .andExpect(status().isConflict());
    }

    @Test
    void studentCannotReachTeacherEndpoints() throws Exception {
        String s = token("coding.student.a@test.com");
        mockMvc.perform(get("/api/teacher/coding-problems")
                        .header("Authorization", "Bearer " + s))
                .andExpect(status().isForbidden());
    }

    /** The whole answer key lives behind this endpoint. */
    @Test
    void studentCannotListTestCasesThroughTeacherEndpoint() throws Exception {
        UUID problemId = publishedProblem(token("coding.teacher.a@test.com"), morning, "Guarded");
        mockMvc.perform(get("/api/teacher/coding-problems/" + problemId + "/test-cases")
                        .header("Authorization", "Bearer " + token("coding.student.a@test.com")))
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // Cohort isolation
    // ================================================================

    @Test
    void studentSeesOnlyOwnBatchProblems() throws Exception {
        publishedProblem(token("coding.teacher.a@test.com"), morning, "Morning Problem");

        mockMvc.perform(get("/api/student/coding-problems")
                        .header("Authorization", "Bearer " + token("coding.student.a@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Morning Problem"));

        // Same course, different cohort - must see nothing.
        mockMvc.perform(get("/api/student/coding-problems")
                        .header("Authorization", "Bearer " + token("coding.student.b@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void studentCannotOpenAnotherBatchesProblem() throws Exception {
        UUID problemId = publishedProblem(token("coding.teacher.a@test.com"), morning, "Morning Only");
        mockMvc.perform(get("/api/student/coding-problems/" + problemId)
                        .header("Authorization", "Bearer " + token("coding.student.b@test.com")))
                .andExpect(status().isForbidden());
    }

    /** batchId null means course-wide, so every enrolled student sees it. */
    @Test
    void courseWideProblemIsVisibleToBothCohorts() throws Exception {
        publishedProblem(token("coding.teacher.a@test.com"), null, "Course Wide");

        mockMvc.perform(get("/api/student/coding-problems")
                        .header("Authorization", "Bearer " + token("coding.student.a@test.com")))
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(get("/api/student/coding-problems")
                        .header("Authorization", "Bearer " + token("coding.student.b@test.com")))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void draftProblemIsInvisibleToStudents() throws Exception {
        UUID problemId = draftProblem(token("coding.teacher.a@test.com"), morning, "Unfinished");

        mockMvc.perform(get("/api/student/coding-problems")
                        .header("Authorization", "Bearer " + token("coding.student.a@test.com")))
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/api/student/coding-problems/" + problemId)
                        .header("Authorization", "Bearer " + token("coding.student.a@test.com")))
                .andExpect(status().isForbidden());
    }

    // ================================================================
    // The hidden-test-case boundary
    // ================================================================

    @Test
    void problemDetailExposesSampleCasesOnlyNeverHiddenOnes() throws Exception {
        UUID problemId = publishedProblem(token("coding.teacher.a@test.com"), morning, "Hidden Check");

        MvcResult r = mockMvc.perform(get("/api/student/coding-problems/" + problemId)
                        .header("Authorization", "Bearer " + token("coding.student.a@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sampleTestCases.length()").value(1))
                .andReturn();

        String body = r.getResponse().getContentAsString();
        assertTrue(body.contains(SAMPLE_OUT), "the sample case should be disclosed");
        assertFalse(body.contains(HIDDEN_IN), "hidden input leaked into the student payload");
        assertFalse(body.contains(HIDDEN_OUT), "hidden expected output leaked into the student payload");
    }

    /** Run executes samples only - the hidden case must not even be counted. */
    @Test
    void runCodeUsesSampleCasesOnly() throws Exception {
        UUID problemId = publishedProblem(token("coding.teacher.a@test.com"), morning, "Run Scope");

        mockMvc.perform(post("/api/student/coding-problems/" + problemId + "/run")
                        .header("Authorization", "Bearer " + token("coding.student.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceCode\":\"print(sum)\",\"language\":\"python\"}"))
                .andExpect(status().isOk())
                // 2 cases exist; only the 1 sample may run.
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.passedCount").value(1));
    }

    @Test
    void failedSubmissionDoesNotRevealHiddenTestData() throws Exception {
        UUID problemId = publishedProblem(token("coding.teacher.a@test.com"), morning, "Wrong Answer");

        // A program that always prints the sample answer: right on the sample, wrong on hidden.
        judge().behaviour = stdin -> StubExecutionClient.success(SAMPLE_OUT);

        MvcResult r = mockMvc.perform(post("/api/student/coding-problems/" + problemId + "/submit")
                        .header("Authorization", "Bearer " + token("coding.student.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceCode\":\"print(4)\",\"language\":\"python\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WRONG_ANSWER"))
                .andExpect(jsonPath("$.data.passedCount").value(1))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andReturn();

        String body = r.getResponse().getContentAsString();
        assertFalse(body.contains(HIDDEN_IN), "hidden input leaked in the submission verdict");
        assertFalse(body.contains(HIDDEN_OUT), "hidden expected output leaked in the submission verdict");
    }

    // ================================================================
    // Judging and performance
    // ================================================================

    @Test
    void correctSubmissionIsAcceptedAndJudgedAgainstEveryCase() throws Exception {
        UUID problemId = publishedProblem(token("coding.teacher.a@test.com"), morning, "All Pass");

        mockMvc.perform(post("/api/student/coding-problems/" + problemId + "/submit")
                        .header("Authorization", "Bearer " + token("coding.student.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceCode\":\"solve()\",\"language\":\"python\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                // Hidden case included: 2 of 2, not 1 of 1.
                .andExpect(jsonPath("$.data.passedCount").value(2))
                .andExpect(jsonPath("$.data.totalCount").value(2));
    }

    @Test
    void studentCannotSubmitToAnotherBatchesProblem() throws Exception {
        UUID problemId = publishedProblem(token("coding.teacher.a@test.com"), morning, "Morning Guarded");
        mockMvc.perform(post("/api/student/coding-problems/" + problemId + "/submit")
                        .header("Authorization", "Bearer " + token("coding.student.b@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceCode\":\"x\",\"language\":\"python\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void performanceCountsDistinctSolvedProblems() throws Exception {
        UUID problemId = publishedProblem(token("coding.teacher.a@test.com"), morning, "Counted Once");
        String s = token("coding.student.a@test.com");

        // Two accepted submissions for the SAME problem must count as one solved.
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/student/coding-problems/" + problemId + "/submit")
                            .header("Authorization", "Bearer " + s)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sourceCode\":\"solve()\",\"language\":\"python\"}"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/student/coding-performance").header("Authorization", "Bearer " + s))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.problemsSolved").value(1))
                .andExpect(jsonPath("$.data.totalSubmissions").value(2))
                .andExpect(jsonPath("$.data.acceptedSubmissions").value(2));
    }

    /**
     * The rule from the spec: four submissions by one student is one solved student.
     */
    @Test
    void problemPerformanceCountsDistinctStudentsNotSubmissions() throws Exception {
        String t = token("coding.teacher.a@test.com");
        UUID problemId = publishedProblem(t, morning, "Two Sum");
        String s = token("coding.student.a@test.com");

        // Wrong, wrong, then accepted, accepted - four submissions, one student.
        judge().behaviour = stdin -> StubExecutionClient.success("NOT_THE_ANSWER");
        submit(problemId, s);
        submit(problemId, s);
        judge().behaviour = StubExecutionClient::solvesEveryCase;
        submit(problemId, s);
        submit(problemId, s);

        mockMvc.perform(get("/api/teacher/coding-problems/" + problemId + "/performance")
                        .header("Authorization", "Bearer " + t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentsInScope").value(1))
                .andExpect(jsonPath("$.data.studentsAttempted").value(1))
                .andExpect(jsonPath("$.data.studentsSolved").value(1))
                .andExpect(jsonPath("$.data.totalSubmissions").value(4))
                .andExpect(jsonPath("$.data.students[0].status").value("SOLVED"))
                .andExpect(jsonPath("$.data.students[0].submissionCount").value(4));
    }

    /** A student in scope who never submitted still appears, as NOT_ATTEMPTED. */
    @Test
    void problemPerformanceListsStudentsWhoNeverAttempted() throws Exception {
        String t = token("coding.teacher.a@test.com");
        UUID problemId = publishedProblem(t, morning, "Untouched");

        mockMvc.perform(get("/api/teacher/coding-problems/" + problemId + "/performance")
                        .header("Authorization", "Bearer " + t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentsInScope").value(1))
                .andExpect(jsonPath("$.data.studentsAttempted").value(0))
                .andExpect(jsonPath("$.data.studentsSolved").value(0))
                .andExpect(jsonPath("$.data.students[0].status").value("NOT_ATTEMPTED"));
    }

    /** Only ACCEPTED counts as solved; passing some hidden cases is not enough. */
    @Test
    void partiallyPassingStudentIsAttemptedNotSolved() throws Exception {
        String t = token("coding.teacher.a@test.com");
        UUID problemId = publishedProblem(t, morning, "Partial");

        judge().behaviour = stdin -> StubExecutionClient.success(SAMPLE_OUT);
        submit(problemId, token("coding.student.a@test.com"));

        mockMvc.perform(get("/api/teacher/coding-problems/" + problemId + "/performance")
                        .header("Authorization", "Bearer " + t))
                .andExpect(jsonPath("$.data.studentsAttempted").value(1))
                .andExpect(jsonPath("$.data.studentsSolved").value(0))
                .andExpect(jsonPath("$.data.students[0].status").value("ATTEMPTED"))
                .andExpect(jsonPath("$.data.students[0].bestScore").value(50));
    }

    @Test
    void teacherCannotReadPerformanceOfAnotherTeachersProblem() throws Exception {
        UUID problemId = publishedProblem(token("coding.teacher.a@test.com"), morning, "A Only");
        mockMvc.perform(get("/api/teacher/coding-problems/" + problemId + "/performance")
                        .header("Authorization", "Bearer " + token("coding.teacher.b@test.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotReadSubmissionsOfAnotherTeachersProblem() throws Exception {
        UUID problemId = publishedProblem(token("coding.teacher.a@test.com"), morning, "A Subs");
        mockMvc.perform(get("/api/teacher/coding-problems/" + problemId + "/submissions")
                        .header("Authorization", "Bearer " + token("coding.teacher.b@test.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherSeesSubmissionsForOwnProblem() throws Exception {
        String t = token("coding.teacher.a@test.com");
        UUID problemId = publishedProblem(t, morning, "Own Subs");
        submit(problemId, token("coding.student.a@test.com"));

        mockMvc.perform(get("/api/teacher/coding-problems/" + problemId + "/submissions")
                        .header("Authorization", "Bearer " + t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].studentName").value("Coding Student A"));
    }

    /** A blank expected output would fail every submission, so publishing is refused. */
    @Test
    void publishIsRefusedWhenATestCaseHasNoExpectedOutput() throws Exception {
        String t = token("coding.teacher.a@test.com");
        UUID problemId = draftProblem(t, morning, "Blank Expected");
        mockMvc.perform(post("/api/teacher/coding-problems/" + problemId + "/test-cases")
                        .header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testCaseBody(SAMPLE_IN, "   ", true)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/teacher/coding-problems/" + problemId + "/publish")
                        .header("Authorization", "Bearer " + t))
                .andExpect(status().isConflict());
    }

    @Test
    void teacherCannotEditAnotherTeachersTestCase() throws Exception {
        String a = token("coding.teacher.a@test.com");
        UUID problemId = draftProblem(a, morning, "Edit Guard");
        MvcResult created = mockMvc.perform(post("/api/teacher/coding-problems/" + problemId + "/test-cases")
                        .header("Authorization", "Bearer " + a)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testCaseBody(SAMPLE_IN, SAMPLE_OUT, true)))
                .andExpect(status().isOk())
                .andReturn();
        UUID tcId = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asText());

        mockMvc.perform(put("/api/teacher/coding-problems/" + problemId + "/test-cases/" + tcId)
                        .header("Authorization", "Bearer " + token("coding.teacher.b@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testCaseBody(SAMPLE_IN, "TAMPERED", true)))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotReadPerformanceOfAnotherTeachersStudent() throws Exception {
        mockMvc.perform(get("/api/teacher/students/" + studentA.getId() + "/coding-performance")
                        .header("Authorization", "Bearer " + token("coding.teacher.b@test.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCanReadPerformanceOfOwnStudent() throws Exception {
        mockMvc.perform(get("/api/teacher/students/" + studentA.getId() + "/coding-performance")
                        .header("Authorization", "Bearer " + token("coding.teacher.a@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentId").value(studentA.getId().toString()));
    }

    /** Submissions are scoped by the token, not by any client-supplied id. */
    @Test
    void studentSubmissionHistoryIsOwnOnly() throws Exception {
        UUID problemId = publishedProblem(token("coding.teacher.a@test.com"), morning, "History");
        mockMvc.perform(post("/api/student/coding-problems/" + problemId + "/submit")
                        .header("Authorization", "Bearer " + token("coding.student.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceCode\":\"solve()\",\"language\":\"python\"}"))
                .andExpect(status().isOk());

        MvcResult r = mockMvc.perform(get("/api/student/coding-submissions")
                        .header("Authorization", "Bearer " + token("coding.student.b@test.com")))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(0, objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").size(), "student B must not see student A's submissions");
    }

    // ================================================================
    // fixtures
    // ================================================================

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

    private String problemBody(Batch batch, String title) {
        return "{\"courseId\":\"" + course.getId() + "\",\"title\":\"" + title + "\""
                + ",\"description\":\"Read two integers and print their sum.\""
                + ",\"difficulty\":\"EASY\",\"language\":\"python\""
                + (batch != null ? ",\"batchId\":\"" + batch.getId() + "\"" : "")
                + "}";
    }

    /** One Submit by the given student, asserted to have been accepted for processing. */
    private void submit(UUID problemId, String studentToken) throws Exception {
        mockMvc.perform(post("/api/student/coding-problems/" + problemId + "/submit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceCode\":\"solve()\",\"language\":\"python\"}"))
                .andExpect(status().isOk());
    }

    private String testCaseBody(String in, String out, boolean sample) {
        return "{\"inputData\":\"" + in + "\",\"expectedOutput\":\"" + out + "\""
                + ",\"sample\":" + sample + ",\"points\":1}";
    }

    private UUID draftProblem(String token, Batch batch, String title) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/teacher/coding-problems")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(problemBody(batch, title)))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("id").asText());
    }

    /** One sample case and one hidden case, then published. */
    private UUID publishedProblem(String token, Batch batch, String title) throws Exception {
        UUID id = draftProblem(token, batch, title);
        mockMvc.perform(post("/api/teacher/coding-problems/" + id + "/test-cases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testCaseBody(SAMPLE_IN, SAMPLE_OUT, true)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/teacher/coding-problems/" + id + "/test-cases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testCaseBody(HIDDEN_IN, HIDDEN_OUT, false)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/teacher/coding-problems/" + id + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        return id;
    }
}
