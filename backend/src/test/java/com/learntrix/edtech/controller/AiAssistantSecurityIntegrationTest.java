package com.learntrix.edtech.controller;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learntrix.edtech.ai.AiProvider;
import com.learntrix.edtech.common.exception.BusinessException;
import com.learntrix.edtech.entity.Batch;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.Enrollment;
import com.learntrix.edtech.entity.Role;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.AnnouncementReadRepository;
import com.learntrix.edtech.repository.CourseAnnouncementRepository;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.CodingProblemRepository;
import com.learntrix.edtech.repository.CodingSubmissionRepository;
import com.learntrix.edtech.repository.CodingTestCaseRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.EnrollmentRepository;
import com.learntrix.edtech.repository.RoleRepository;
import com.learntrix.edtech.repository.UserRepository;

/**
 * Authorization and output-validation tests for the AI Question Assistant.
 *
 * <pre>
 *   Java Backend
 *     ├── Morning (teacherA) ── studentA
 *     └── Evening (teacherB) ── studentB
 * </pre>
 *
 * <p>The provider is stubbed, so no test spends a token or needs network access. What is
 * under test is not Gemini - it is whether this application trusts Gemini, and whether it
 * lets the wrong person ask.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiAssistantSecurityIntegrationTest {

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
    @Autowired private CodingProblemRepository codingProblemRepository;
    @Autowired private CodingTestCaseRepository codingTestCaseRepository;
    @Autowired private CodingSubmissionRepository codingSubmissionRepository;

    @Autowired private AiProvider aiProvider;

    private StubAiProvider ai() {
        return (StubAiProvider) aiProvider;
    }

    @TestConfiguration
    static class StubAiConfig {
        @Bean
        @Primary
        AiProvider stubAiProvider() {
            return new StubAiProvider();
        }
    }

    /** An AI whose reply each test dictates - including replies that are wrong. */
    static class StubAiProvider implements AiProvider {
        volatile Supplier<String> reply = () -> GOOD_QUIZ;
        volatile boolean configured = true;

        @Override
        public String generateJson(String systemInstruction, String userPrompt,
                                   Map<String, Object> responseSchema) {
            if (!configured) {
                throw new BusinessException("AI_NOT_CONFIGURED",
                        "The AI assistant is not configured on this server.",
                        HttpStatus.SERVICE_UNAVAILABLE);
            }
            return reply.get();
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }
    }

    // ---- canned provider replies -------------------------------------

    private static final String GOOD_QUIZ = """
        {"questions":[
          {"question":"Which keyword inherits a class in Java?",
           "options":["implements","extends","inherits","super"],
           "correctOption":1,"marks":1,"difficulty":"MEDIUM",
           "explanation":"extends is used for class inheritance."}
        ]}""";

    /** correctOption points past the end of the options array. */
    private static final String BAD_INDEX_QUIZ = """
        {"questions":[
          {"question":"Which keyword inherits a class in Java?",
           "options":["implements","extends","inherits","super"],
           "correctOption":9,"marks":1,"difficulty":"MEDIUM",
           "explanation":"out of range"}
        ]}""";

    /** Two options are the same answer with different casing. */
    private static final String DUPLICATE_OPTION_QUIZ = """
        {"questions":[
          {"question":"Which keyword inherits a class in Java?",
           "options":["extends","Extends ","inherits","super"],
           "correctOption":0,"marks":1,"difficulty":"MEDIUM",
           "explanation":"duplicate distractors"}
        ]}""";

    private static final String GOOD_CODING = """
        {"problems":[
          {"title":"Second Largest Element",
           "description":"Read N then N integers. Print the second largest.",
           "language":"JAVA","difficulty":"MEDIUM","constraints":["2 <= N <= 100000"],
           "sampleInput":"5\\n10 5 8 20 15","sampleOutput":"15",
           "testCases":[
             {"input":"5\\n10 5 8 20 15","expectedOutput":"15","sample":true},
             {"input":"2\\n1 2","expectedOutput":"1","sample":false}]}
        ]}""";

    /** A problem the existing publish rule could never accept. */
    private static final String NO_TEST_CASE_CODING = """
        {"problems":[
          {"title":"Broken Problem","description":"Does something.",
           "language":"JAVA","difficulty":"EASY","constraints":[],
           "sampleInput":"1","sampleOutput":"1","testCases":[]}
        ]}""";

    private Course course;
    private Batch morning;
    private Batch evening;
    private User teacherA;
    private User teacherB;
    private User studentA;

    private static final String PW = "Password123!";

    @BeforeEach
    void setUp() {
        // course_announcements.batch_id references batches; clear announcements (and their
        // read rows) before the batch delete below, or H2 rejects it.
        announcementReadRepositoryCleanup.deleteAll();
        courseAnnouncementRepositoryCleanup.deleteAll();
        codingSubmissionRepository.deleteAll();
        codingTestCaseRepository.deleteAll();
        codingProblemRepository.deleteAll();
        enrollmentRepository.deleteAll();
        batchRepository.deleteAll();

        Role adminRole = role("ADMIN");
        Role teacherRole = role("TEACHER");
        Role studentRole = role("STUDENT");

        user("ai.admin@test.com", "AI Admin", adminRole);
        teacherA = user("ai.teacher.a@test.com", "AI Teacher A", teacherRole);
        teacherB = user("ai.teacher.b@test.com", "AI Teacher B", teacherRole);
        studentA = user("ai.student.a@test.com", "AI Student A", studentRole);
        User studentB = user("ai.student.b@test.com", "AI Student B", studentRole);

        course = course("Java Backend", "java-backend-ai");
        morning = batch("Morning", course, teacherA, studentA);
        evening = batch("Evening", course, teacherB, studentB);

        enrol(studentA, course, teacherA, morning);
        enrol(studentB, course, teacherB, evening);

        ai().configured = true;
        ai().reply = () -> GOOD_QUIZ;
    }

    // ================================================================
    // Who may ask
    // ================================================================

    @Test
    void teacherCanGenerateForOwnBatch() throws Exception {
        mockMvc.perform(post("/api/teacher/ai/question-assistant")
                        .header("Authorization", "Bearer " + token("ai.teacher.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizBody(morning)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("QUIZ"))
                .andExpect(jsonPath("$.data.batchName").value("Morning"))
                .andExpect(jsonPath("$.data.questions.length()").value(1));
    }

    /** Teacher B teaches the same course, but not this cohort. */
    @Test
    void teacherCannotGenerateForAnotherTeachersBatch() throws Exception {
        mockMvc.perform(post("/api/teacher/ai/question-assistant")
                        .header("Authorization", "Bearer " + token("ai.teacher.b@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizBody(morning)))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentIsForbidden() throws Exception {
        mockMvc.perform(post("/api/teacher/ai/question-assistant")
                        .header("Authorization", "Bearer " + token("ai.student.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizBody(morning)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(post("/api/teacher/ai/question-assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizBody(morning)))
                .andExpect(status().isUnauthorized());
    }

    /** A batch from another course must not be attachable to this one. */
    @Test
    void batchFromAnotherCourseIsRejected() throws Exception {
        Course other = course("Other Course", "other-course-ai");
        Batch otherBatch = batch("Other Morning", other, teacherA, studentA);

        String body = "{\"courseId\":\"" + course.getId() + "\",\"batchId\":\"" + otherBatch.getId()
                + "\",\"type\":\"QUIZ\",\"topic\":\"Java\",\"count\":1}";
        mockMvc.perform(post("/api/teacher/ai/question-assistant")
                        .header("Authorization", "Bearer " + token("ai.teacher.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ================================================================
    // Not trusting the model
    // ================================================================

    @Test
    void missingApiKeyIsReportedCleanly() throws Exception {
        ai().configured = false;
        MvcResult r = mockMvc.perform(post("/api/teacher/ai/question-assistant")
                        .header("Authorization", "Bearer " + token("ai.teacher.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizBody(morning)))
                .andExpect(status().isServiceUnavailable())
                .andReturn();
        String body = r.getResponse().getContentAsString();
        assertTrue(body.contains("not configured"), "should say the assistant is not configured");
        assertFalse(body.toLowerCase().contains("gemini_api_key="), "must never echo a key");
    }

    @Test
    void malformedProviderResponseIsRejected() throws Exception {
        ai().reply = () -> "this is not json at all";
        mockMvc.perform(post("/api/teacher/ai/question-assistant")
                        .header("Authorization", "Bearer " + token("ai.teacher.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizBody(morning)))
                .andExpect(status().isBadGateway());
    }

    /** Schema-conformant but semantically wrong: the index points nowhere. */
    @Test
    void quizWithOutOfRangeCorrectOptionIsDiscarded() throws Exception {
        ai().reply = () -> BAD_INDEX_QUIZ;
        mockMvc.perform(post("/api/teacher/ai/question-assistant")
                        .header("Authorization", "Bearer " + token("ai.teacher.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizBody(morning)))
                .andExpect(status().isBadGateway());
    }

    @Test
    void quizWithDuplicateOptionsIsDiscarded() throws Exception {
        ai().reply = () -> DUPLICATE_OPTION_QUIZ;
        mockMvc.perform(post("/api/teacher/ai/question-assistant")
                        .header("Authorization", "Bearer " + token("ai.teacher.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizBody(morning)))
                .andExpect(status().isBadGateway());
    }

    @Test
    void codingProblemWithNoTestCasesIsDiscarded() throws Exception {
        ai().reply = () -> NO_TEST_CASE_CODING;
        mockMvc.perform(post("/api/teacher/ai/question-assistant")
                        .header("Authorization", "Bearer " + token("ai.teacher.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(codingBody(morning)))
                .andExpect(status().isBadGateway());
    }

    @Test
    void codingGenerationReturnsSampleAndHiddenCases() throws Exception {
        ai().reply = () -> GOOD_CODING;
        mockMvc.perform(post("/api/teacher/ai/question-assistant")
                        .header("Authorization", "Bearer " + token("ai.teacher.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(codingBody(morning)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.problems[0].testCases.length()").value(2))
                .andExpect(jsonPath("$.data.problems[0].testCases[0].sample").value(true))
                .andExpect(jsonPath("$.data.problems[0].testCases[1].sample").value(false));
    }

    // ================================================================
    // The assistant cannot write to the database
    // ================================================================

    /**
     * Generation must leave no trace. If drafts were being persisted, a coding problem row
     * would appear here - and could then be published without a teacher ever approving it.
     */
    @Test
    void generationPersistsNothing() throws Exception {
        long problemsBefore = codingProblemRepository.count();

        ai().reply = () -> GOOD_CODING;
        mockMvc.perform(post("/api/teacher/ai/question-assistant")
                        .header("Authorization", "Bearer " + token("ai.teacher.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(codingBody(morning)))
                .andExpect(status().isOk());

        ai().reply = () -> GOOD_QUIZ;
        mockMvc.perform(post("/api/teacher/ai/question-assistant")
                        .header("Authorization", "Bearer " + token("ai.teacher.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quizBody(morning)))
                .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertEquals(problemsBefore, codingProblemRepository.count(),
                "AI generation must not create coding problems");
    }

    /** Nothing generated may arrive already published. */
    @Test
    void generatedDraftsCarryNoPublishedState() throws Exception {
        ai().reply = () -> GOOD_CODING;
        MvcResult r = mockMvc.perform(post("/api/teacher/ai/question-assistant")
                        .header("Authorization", "Bearer " + token("ai.teacher.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(codingBody(morning)))
                .andExpect(status().isOk())
                .andReturn();
        assertFalse(r.getResponse().getContentAsString().contains("PUBLISHED"),
                "a draft must not come back marked PUBLISHED");
    }

    // ================================================================
    // fixtures
    // ================================================================

    private String quizBody(Batch batch) {
        return "{\"courseId\":\"" + course.getId() + "\",\"batchId\":\"" + batch.getId()
                + "\",\"type\":\"QUIZ\",\"difficulty\":\"MEDIUM\",\"topic\":\"Java inheritance\","
                + "\"count\":1}";
    }

    private String codingBody(Batch batch) {
        return "{\"courseId\":\"" + course.getId() + "\",\"batchId\":\"" + batch.getId()
                + "\",\"type\":\"CODING\",\"difficulty\":\"MEDIUM\",\"topic\":\"arrays\","
                + "\"count\":1,\"language\":\"JAVA\"}";
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
