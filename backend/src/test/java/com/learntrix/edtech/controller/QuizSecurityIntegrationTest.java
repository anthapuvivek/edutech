package com.learntrix.edtech.controller;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.learntrix.edtech.entity.Batch;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.Enrollment;
import com.learntrix.edtech.entity.Role;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.AnnouncementReadRepository;
import com.learntrix.edtech.repository.CourseAnnouncementRepository;
import com.learntrix.edtech.repository.CodingProblemCompletionRepository;
import com.learntrix.edtech.repository.CodingProblemRepository;
import com.learntrix.edtech.repository.CodingSubmissionRepository;
import com.learntrix.edtech.repository.CodingTestCaseRepository;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.EnrollmentRepository;
import com.learntrix.edtech.repository.QuizAttemptAnswerRepository;
import com.learntrix.edtech.repository.QuizAttemptRepository;
import com.learntrix.edtech.repository.QuizOptionRepository;
import com.learntrix.edtech.repository.QuizQuestionRepository;
import com.learntrix.edtech.repository.QuizRepository;
import com.learntrix.edtech.repository.RoleRepository;
import com.learntrix.edtech.repository.UserRepository;

/**
 * Authorization and evaluation tests for the quiz backend.
 *
 * <p>Scenario: one course with two cohorts run by different teachers.</p>
 * <pre>
 *   Java Backend
 *     ├── Morning (teacherA) ── studentA
 *     └── Evening (teacherB) ── studentB
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuizSecurityIntegrationTest {

    @Autowired
    private AnnouncementReadRepository announcementReadRepositoryCleanup;
    @Autowired
    private CourseAnnouncementRepository courseAnnouncementRepositoryCleanup;

    @Autowired
    private CodingProblemCompletionRepository codingProblemCompletionRepository;

    @Autowired
    private CodingSubmissionRepository codingSubmissionRepository;
    @Autowired
    private CodingTestCaseRepository codingTestCaseRepository;
    @Autowired
    private CodingProblemRepository codingProblemRepository;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private BatchRepository batchRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private QuizRepository quizRepository;
    @Autowired private QuizQuestionRepository questionRepository;
    @Autowired private QuizOptionRepository optionRepository;
    @Autowired private QuizAttemptRepository attemptRepository;
    @Autowired private QuizAttemptAnswerRepository attemptAnswerRepository;

    private Course course;
    private Course otherCourse;
    private Batch morning;
    private Batch evening;
    private User teacherA;
    private User teacherB;
    private User studentA;
    private User studentB;

    private static final String PW = "Password123!";

    @BeforeEach
    void setUp() {
        // coding_problems.batch_id blocks batch deletion in the JPA-generated test
        // schema; production Postgres has ON DELETE SET NULL (V31). Children first.
        // coding_problem_completions references coding_problems; clear it first or
        // the problem delete below is rejected.
        // course_announcements.batch_id references batches; clear announcements (and their
        // read rows) before the batch delete below, or H2 rejects it.
        announcementReadRepositoryCleanup.deleteAll();
        courseAnnouncementRepositoryCleanup.deleteAll();
        codingProblemCompletionRepository.deleteAll();
        codingSubmissionRepository.deleteAll();
        codingTestCaseRepository.deleteAll();
        codingProblemRepository.deleteAll();
        attemptAnswerRepository.deleteAll();
        attemptRepository.deleteAll();
        optionRepository.deleteAll();
        questionRepository.deleteAll();
        quizRepository.deleteAll();
        enrollmentRepository.deleteAll();
        batchRepository.deleteAll();

        Role adminRole = role("ADMIN");
        Role teacherRole = role("TEACHER");
        Role studentRole = role("STUDENT");

        user("quizadmin@test.com", "Quiz Admin", adminRole);
        teacherA = user("teacher.a@test.com", "Teacher A", teacherRole);
        teacherB = user("teacher.b@test.com", "Teacher B", teacherRole);
        studentA = user("student.a@test.com", "Student A", studentRole);
        studentB = user("student.b@test.com", "Student B", studentRole);

        course = course("Java Backend", "java-backend-quiz");
        otherCourse = course("Unrelated Course", "unrelated-quiz");

        morning = batch("Morning", course, teacherA, studentA);
        evening = batch("Evening", course, teacherB, studentB);

        enrol(studentA, course, teacherA, morning);
        enrol(studentB, course, teacherB, evening);
    }

    // ---------------- fixtures ----------------

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

    /** Creates a quiz, adds one question with a known correct option, and publishes it. */
    private QuizFixture publishedQuiz(String token, Course c, Batch batch, String title) throws Exception {
        String body = "{\"courseId\":\"" + c.getId() + "\",\"title\":\"" + title + "\""
                + (batch != null ? ",\"batchId\":\"" + batch.getId() + "\"" : "")
                + ",\"passingScore\":50}";
        MvcResult created = mockMvc.perform(post("/api/teacher/quizzes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        UUID quizId = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asText());

        MvcResult q = mockMvc.perform(post("/api/teacher/quizzes/" + quizId + "/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionText\":\"2 + 2 = ?\",\"points\":10,\"options\":["
                                + "{\"optionText\":\"3\",\"correct\":false},"
                                + "{\"optionText\":\"4\",\"correct\":true}]}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode qn = objectMapper.readTree(q.getResponse().getContentAsString()).path("data");
        UUID questionId = UUID.fromString(qn.path("id").asText());
        UUID correctId = null, wrongId = null;
        for (JsonNode o : qn.path("options")) {
            if (o.path("correct").asBoolean()) correctId = UUID.fromString(o.path("id").asText());
            else wrongId = UUID.fromString(o.path("id").asText());
        }

        mockMvc.perform(post("/api/teacher/quizzes/" + quizId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        return new QuizFixture(quizId, questionId, correctId, wrongId);
    }

    private record QuizFixture(UUID quizId, UUID questionId, UUID correctOptionId, UUID wrongOptionId) {}

    // ================= teacher authorization =================

    @Test
    void authorizedTeacherCanCreateQuizAndUnauthorizedTeacherCannot() throws Exception {
        String tokenA = token("teacher.a@test.com");

        mockMvc.perform(post("/api/teacher/quizzes")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + course.getId() + "\",\"title\":\"Mine\"}"))
                .andExpect(status().isOk())
                // A new quiz must not be student-visible before it has questions.
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        // teacherA has no relationship to otherCourse at all.
        mockMvc.perform(post("/api/teacher/quizzes")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + otherCourse.getId() + "\",\"title\":\"Nope\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotCreateQuizForAnotherTeachersBatch() throws Exception {
        String tokenA = token("teacher.a@test.com");

        // teacherA is authorized for the course, but Evening belongs to teacherB.
        mockMvc.perform(post("/api/teacher/quizzes")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + course.getId() + "\",\"title\":\"Sneaky\""
                                + ",\"batchId\":\"" + evening.getId() + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotCreateQuiz() throws Exception {
        mockMvc.perform(post("/api/teacher/quizzes")
                        .header("Authorization", "Bearer " + token("student.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + course.getId() + "\",\"title\":\"Hack\"}"))
                .andExpect(status().isForbidden());
    }

    // ================= student visibility & batch isolation =================

    @Test
    void studentSeesPublishedCourseWideQuizButNotDrafts() throws Exception {
        String tokenA = token("teacher.a@test.com");
        publishedQuiz(tokenA, course, null, "Course Wide");

        // A draft on the same course must stay hidden.
        mockMvc.perform(post("/api/teacher/quizzes")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + course.getId() + "\",\"title\":\"Draft Only\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/student/quizzes")
                        .header("Authorization", "Bearer " + token("student.a@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Course Wide"));
    }

    @Test
    void morningStudentCannotSeeOrOpenEveningBatchQuiz() throws Exception {
        QuizFixture evenings = publishedQuiz(token("teacher.b@test.com"), course, evening, "Evening Only");

        String studentAToken = token("student.a@test.com");

        // Not in the list...
        mockMvc.perform(get("/api/student/quizzes")
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        // ...and not reachable by guessing the id.
        mockMvc.perform(get("/api/student/quizzes/" + evenings.quizId() + "/questions")
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isForbidden());

        // The owning cohort does see it.
        mockMvc.perform(get("/api/student/quizzes")
                        .header("Authorization", "Bearer " + token("student.b@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Evening Only"));
    }

    @Test
    void eveningStudentCannotOpenMorningBatchQuiz() throws Exception {
        QuizFixture mornings = publishedQuiz(token("teacher.a@test.com"), course, morning, "Morning Only");

        mockMvc.perform(get("/api/student/quizzes/" + mornings.quizId() + "/questions")
                        .header("Authorization", "Bearer " + token("student.b@test.com")))
                .andExpect(status().isForbidden());
    }

    // ================= answer-key secrecy =================

    @Test
    void studentQuestionPayloadNeverContainsTheAnswerKey() throws Exception {
        QuizFixture fx = publishedQuiz(token("teacher.a@test.com"), course, morning, "Secrecy");

        MvcResult res = mockMvc.perform(get("/api/student/quizzes/" + fx.quizId() + "/questions")
                        .header("Authorization", "Bearer " + token("student.a@test.com")))
                .andExpect(status().isOk())
                .andReturn();

        String body = res.getResponse().getContentAsString();
        assertFalse(body.contains("correct"),
                "the student payload must not mention correctness at all: " + body);
        assertTrue(body.contains("optionText"), "options should still be present");
    }

    // ================= evaluation =================

    @Test
    void backendScoresTheAttemptAndIgnoresAnythingTheClientClaims() throws Exception {
        QuizFixture fx = publishedQuiz(token("teacher.a@test.com"), course, morning, "Scoring");
        String studentToken = token("student.a@test.com");

        mockMvc.perform(post("/api/student/quizzes/" + fx.quizId() + "/start")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        // Answer WRONG, while also posting a bogus score of 100 in the same body.
        mockMvc.perform(post("/api/student/quizzes/" + fx.quizId() + "/submit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":100,\"passed\":true,\"answers\":[{\"questionId\":\""
                                + fx.questionId() + "\",\"selectedOptionId\":\""
                                + fx.wrongOptionId() + "\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(0))
                .andExpect(jsonPath("$.data.passed").value(false))
                .andExpect(jsonPath("$.data.correctAnswers").value(0))
                .andExpect(jsonPath("$.data.totalQuestions").value(1));
    }

    @Test
    void startingQuizKeepsItResumableUntilItIsSubmitted() throws Exception {
        QuizFixture fx = publishedQuiz(token("teacher.a@test.com"), course, morning, "Resume Me");
        String studentToken = token("student.a@test.com");

        mockMvc.perform(post("/api/student/quizzes/" + fx.quizId() + "/start")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.submittedAt").doesNotExist());

        // The list must not call an in-progress attempt completed/failed. That previously
        // made the frontend render a result and prevented the student from resuming.
        mockMvc.perform(get("/api/student/quizzes")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].attempted").value(false))
                .andExpect(jsonPath("$.data[0].attemptStatus").value("IN_PROGRESS"));

        // Start is idempotent for the same unfinished attempt.
        mockMvc.perform(post("/api/student/quizzes/" + fx.quizId() + "/start")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        assertEquals(1, attemptRepository.findByQuizId(fx.quizId()).size());
    }

    @Test
    void correctAnswerScoresFullMarksAndPasses() throws Exception {
        QuizFixture fx = publishedQuiz(token("teacher.a@test.com"), course, morning, "Correct");
        String studentToken = token("student.a@test.com");

        mockMvc.perform(post("/api/student/quizzes/" + fx.quizId() + "/submit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":\"" + fx.questionId()
                                + "\",\"selectedOptionId\":\"" + fx.correctOptionId() + "\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(100))
                .andExpect(jsonPath("$.data.passed").value(true))
                .andExpect(jsonPath("$.data.correctAnswers").value(1));

        assertEquals(1, attemptAnswerRepository.findAll().size(),
                "exactly one answer row is persisted for the attempt");
    }

    @Test
    void resubmittingAnAlreadySubmittedAttemptIsRejected() throws Exception {
        QuizFixture fx = publishedQuiz(token("teacher.a@test.com"), course, morning, "Once Only");
        String studentToken = token("student.a@test.com");
        String payload = "{\"answers\":[{\"questionId\":\"" + fx.questionId()
                + "\",\"selectedOptionId\":\"" + fx.correctOptionId() + "\"}]}";

        mockMvc.perform(post("/api/student/quizzes/" + fx.quizId() + "/submit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/student/quizzes/" + fx.quizId() + "/submit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void optionFromAnotherQuestionIsRejected() throws Exception {
        String tokenA = token("teacher.a@test.com");
        QuizFixture first = publishedQuiz(tokenA, course, morning, "First");
        QuizFixture second = publishedQuiz(tokenA, course, morning, "Second");

        // Answer first quiz's question using an option id belonging to the second quiz.
        mockMvc.perform(post("/api/student/quizzes/" + first.quizId() + "/submit")
                        .header("Authorization", "Bearer " + token("student.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":\"" + first.questionId()
                                + "\",\"selectedOptionId\":\"" + second.correctOptionId() + "\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void studentCannotSubmitToAQuizTheyAreNotEnrolledIn() throws Exception {
        // A quiz on a course studentA has no enrolment in.
        String adminToken = token("quizadmin@test.com");
        QuizFixture fx = publishedQuiz(adminToken, otherCourse, null, "Foreign");

        mockMvc.perform(post("/api/student/quizzes/" + fx.quizId() + "/submit")
                        .header("Authorization", "Bearer " + token("student.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[]}"))
                .andExpect(status().isForbidden());
    }

    // ================= teacher performance =================

    @Test
    void teacherPerformanceIsScopedToTheirOwnQuiz() throws Exception {
        QuizFixture mornings = publishedQuiz(token("teacher.a@test.com"), course, morning, "Perf");

        mockMvc.perform(post("/api/student/quizzes/" + mornings.quizId() + "/submit")
                        .header("Authorization", "Bearer " + token("student.a@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[{\"questionId\":\"" + mornings.questionId()
                                + "\",\"selectedOptionId\":\"" + mornings.correctOptionId() + "\"}]}"))
                .andExpect(status().isOk());

        // The owning teacher sees the attempt.
        mockMvc.perform(get("/api/teacher/quizzes/" + mornings.quizId() + "/performance")
                        .header("Authorization", "Bearer " + token("teacher.a@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].studentName").value("Student A"))
                .andExpect(jsonPath("$.data[0].score").value(100));

        // The other cohort's teacher must not.
        mockMvc.perform(get("/api/teacher/quizzes/" + mornings.quizId() + "/performance")
                        .header("Authorization", "Bearer " + token("teacher.b@test.com")))
                .andExpect(status().isForbidden());
    }
}
