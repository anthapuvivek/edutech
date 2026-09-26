package com.learntrix.edtech.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learntrix.edtech.dto.admin.AllocateStudentRequest;
import com.learntrix.edtech.dto.admin.CreateStudentRequest;
import com.learntrix.edtech.dto.admin.CreateTeacherRequest;
import com.learntrix.edtech.dto.auth.ResetPasswordRequest;
import com.learntrix.edtech.dto.announcement.CreateCourseAnnouncementRequest;
import com.learntrix.edtech.dto.assignment.CreateAssignmentRequest;
import com.learntrix.edtech.dto.assignment.GradeSubmissionRequest;
import com.learntrix.edtech.dto.assignment.SubmitAssignmentRequest;
import com.learntrix.edtech.dto.material.CreateCourseMaterialRequest;
import com.learntrix.edtech.dto.quiz.CreateQuizRequest;
import com.learntrix.edtech.dto.quiz.QuizAttemptRequest;
import com.learntrix.edtech.dto.recording.CreateRecordingRequest;
import com.learntrix.edtech.entity.*;
import com.learntrix.edtech.repository.CodingProblemCompletionRepository;
import com.learntrix.edtech.repository.CodingProblemRepository;
import com.learntrix.edtech.repository.CodingSubmissionRepository;
import com.learntrix.edtech.repository.CodingTestCaseRepository;
import com.learntrix.edtech.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LearntrixCompleteAccessControlIntegrationTest {

    @Autowired
    private CodingProblemCompletionRepository codingProblemCompletionRepository;

    @Autowired
    private CodingSubmissionRepository codingSubmissionRepository;
    @Autowired
    private CodingTestCaseRepository codingTestCaseRepository;
    @Autowired
    private CodingProblemRepository codingProblemRepository;

    @Autowired
    private MockMvc mockMvc;

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
    private BatchRepository batchRepository;

    @Autowired
    private LiveClassRepository liveClassRepository;

    @Autowired
    private ClassRecordingRepository classRecordingRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @Autowired
    private com.learntrix.edtech.repository.QuizAttemptAnswerRepository quizAttemptAnswerRepository;

    @Autowired
    private com.learntrix.edtech.repository.QuizOptionRepository quizOptionRepository;

    @Autowired
    private com.learntrix.edtech.repository.QuizQuestionRepository quizQuestionRepository;

    @Autowired
    private CourseMaterialRepository courseMaterialRepository;

    @Autowired
    private CourseAnnouncementRepository courseAnnouncementRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private TeacherProfileRepository teacherProfileRepository;

    @Autowired
    private AccountActivationTokenRepository activationTokenRepository;

    @Autowired
    private RecordingWatchProgressRepository progressRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User admin;
    private User teacherA;
    private User teacherB;
    private User student1;
    private User student2;
    private User student3;

    private Course courseJava;
    private Course coursePython;

    private com.learntrix.edtech.entity.Module moduleJava;
    private Lesson lessonJava;
    private ClassRecording recordingJava;

    private com.learntrix.edtech.entity.Module modulePython;
    private Lesson lessonPython;
    private ClassRecording recordingPython;

    private String adminToken;
    private String teacherAToken;
    private String teacherBToken;
    private String student1Token;
    private String student2Token;
    private String student3Token;

    @BeforeEach
    void setUp() throws Exception {
        // Clean all state
        progressRepository.deleteAll();
        courseAnnouncementRepository.deleteAll();
        courseMaterialRepository.deleteAll();
        // Children first: quiz_attempt_answers and quiz_options reference these rows.
        quizAttemptAnswerRepository.deleteAll();
        quizAttemptRepository.deleteAll();
        quizOptionRepository.deleteAll();
        quizQuestionRepository.deleteAll();
        quizRepository.deleteAll();
        assignmentSubmissionRepository.deleteAll();
        assignmentRepository.deleteAll();
        // coding_problems.batch_id blocks batch deletion in the JPA-generated test
        // schema; production Postgres has ON DELETE SET NULL (V31). Children first.
        // coding_problem_completions references coding_problems; clear it first or
        // the problem delete below is rejected.
        codingProblemCompletionRepository.deleteAll();
        codingSubmissionRepository.deleteAll();
        codingTestCaseRepository.deleteAll();
        codingProblemRepository.deleteAll();
        classRecordingRepository.deleteAll();
        liveClassRepository.deleteAll();
        // enrollments.batch_id references batches - children first, or H2 rejects
        // the batch delete (production Postgres cascades this itself).
        enrollmentRepository.deleteAll();
        batchRepository.deleteAll();
        studentProfileRepository.deleteAll();
        teacherProfileRepository.deleteAll();
        activationTokenRepository.deleteAll();
        lessonRepository.deleteAll();
        moduleRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

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

        // 1. Admin
        admin = new User();
        admin.setEmail("admin@learntrix.com");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123456"));
        admin.setName("Learntrix Admin");
        admin.setStatus("ACTIVE");
        admin.setEmailVerified(true);
        admin.setRoles(Set.of(adminRole));
        admin = userRepository.save(admin);

        // 2. Teachers
        teacherA = new User();
        teacherA.setEmail("teachera@learntrix.com");
        teacherA.setPasswordHash(passwordEncoder.encode("Teacher@123456"));
        teacherA.setName("Teacher A (Java Trainer)");
        teacherA.setStatus("ACTIVE");
        teacherA.setEmailVerified(true);
        teacherA.setRoles(Set.of(teacherRole));
        teacherA = userRepository.save(teacherA);

        teacherB = new User();
        teacherB.setEmail("teacherb@learntrix.com");
        teacherB.setPasswordHash(passwordEncoder.encode("Teacher@123456"));
        teacherB.setName("Teacher B (Python Trainer)");
        teacherB.setStatus("ACTIVE");
        teacherB.setEmailVerified(true);
        teacherB.setRoles(Set.of(teacherRole));
        teacherB = userRepository.save(teacherB);

        // 3. Students
        student1 = new User();
        student1.setEmail("student1@learntrix.com");
        student1.setPasswordHash(passwordEncoder.encode("Student@123456"));
        student1.setName("Student One");
        student1.setStatus("ACTIVE");
        student1.setEmailVerified(true);
        student1.setRoles(Set.of(studentRole));
        student1 = userRepository.save(student1);

        student2 = new User();
        student2.setEmail("student2@learntrix.com");
        student2.setPasswordHash(passwordEncoder.encode("Student@123456"));
        student2.setName("Student Two");
        student2.setStatus("ACTIVE");
        student2.setEmailVerified(true);
        student2.setRoles(Set.of(studentRole));
        student2 = userRepository.save(student2);

        student3 = new User();
        student3.setEmail("student3@learntrix.com");
        student3.setPasswordHash(passwordEncoder.encode("Student@123456"));
        student3.setName("Student Three");
        student3.setStatus("ACTIVE");
        student3.setEmailVerified(true);
        student3.setRoles(Set.of(studentRole));
        student3 = userRepository.save(student3);

        // 4. Courses
        courseJava = new Course();
        courseJava.setTitle("Java Full Stack Development");
        courseJava.setSlug("java-full-stack");
        courseJava.setCategory("Programming");
        courseJava.setStatus("PUBLISHED");
        courseJava.setInstructor(teacherA);
        courseJava = courseRepository.save(courseJava);

        coursePython = new Course();
        coursePython.setTitle("Python & AI Development");
        coursePython.setSlug("python-ai");
        coursePython.setCategory("Data Science");
        coursePython.setStatus("PUBLISHED");
        coursePython.setInstructor(teacherB);
        coursePython = courseRepository.save(coursePython);

        // 5. Recordings
        moduleJava = new com.learntrix.edtech.entity.Module();
        moduleJava.setCourse(courseJava);
        moduleJava.setTitle("Java Fundamentals");
        moduleJava.setSequenceNumber(1);
        moduleJava = moduleRepository.save(moduleJava);

        lessonJava = new Lesson();
        lessonJava.setModule(moduleJava);
        lessonJava.setTitle("OOP Concepts");
        lessonJava.setSequenceNumber(1);
        lessonJava.setDurationSeconds(3600);
        lessonJava = lessonRepository.save(lessonJava);

        recordingJava = new ClassRecording();
        recordingJava.setCourse(courseJava);
        recordingJava.setModule(moduleJava);
        recordingJava.setLesson(lessonJava);
        recordingJava.setTeacher(teacherA);
        recordingJava.setTitle("Java Class 1 - OOP In-Depth");
        recordingJava.setClassDate(Instant.now());
        recordingJava.setStatus(RecordingStatus.PUBLISHED);
        recordingJava.setPublished(true);
        recordingJava.setDurationSeconds(3600);
        recordingJava = classRecordingRepository.save(recordingJava);

        modulePython = new com.learntrix.edtech.entity.Module();
        modulePython.setCourse(coursePython);
        modulePython.setTitle("Python Fundamentals");
        modulePython.setSequenceNumber(1);
        modulePython = moduleRepository.save(modulePython);

        lessonPython = new Lesson();
        lessonPython.setModule(modulePython);
        lessonPython.setTitle("Data Structures in Python");
        lessonPython.setSequenceNumber(1);
        lessonPython.setDurationSeconds(3600);
        lessonPython = lessonRepository.save(lessonPython);

        recordingPython = new ClassRecording();
        recordingPython.setCourse(coursePython);
        recordingPython.setModule(modulePython);
        recordingPython.setLesson(lessonPython);
        recordingPython.setTeacher(teacherB);
        recordingPython.setTitle("Python Class 1 - Lists and Dictionaries");
        recordingPython.setClassDate(Instant.now());
        recordingPython.setStatus(RecordingStatus.PUBLISHED);
        recordingPython.setPublished(true);
        recordingPython.setDurationSeconds(3600);
        recordingPython = classRecordingRepository.save(recordingPython);

        // Fetch tokens
        adminToken = loginToken("admin@learntrix.com", "Admin@123456");
        teacherAToken = loginToken("teachera@learntrix.com", "Teacher@123456");
        teacherBToken = loginToken("teacherb@learntrix.com", "Teacher@123456");
        student1Token = loginToken("student1@learntrix.com", "Student@123456");
        student2Token = loginToken("student2@learntrix.com", "Student@123456");
        student3Token = loginToken("student3@learntrix.com", "Student@123456");
    }

    private String loginToken(String email, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("1. Admin Onboards Student -> Account and Profile Persisted")
    void testAdminStudentOnboarding() throws Exception {
        CreateStudentRequest req = CreateStudentRequest.builder()
                .name("Ravi Kumar")
                .email("ravi.kumar@test.com")
                .password("Student@123456")
                .phone("+91 98765 43210")
                .college("IIT Madras")
                .graduationYear(2026)
                .build();

        MvcResult result = mockMvc.perform(post("/api/admin/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("ravi.kumar@test.com"))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        UUID createdUserId = UUID.fromString(data.path("userId").asText());

        assertTrue(userRepository.findById(createdUserId).isPresent(), "Student must be persisted in database");
        assertTrue(studentProfileRepository.findByUserId(createdUserId).isPresent(), "Student profile must be created");
    }

    @Test
    @DisplayName("2. Admin Allocates Student 1 to Teacher A (Java Course) -> Persisted and Visible")
    void testAdminAllocatesStudentToTeacherAndCourse() throws Exception {
        AllocateStudentRequest req = AllocateStudentRequest.builder()
                .studentId(student1.getId())
                .courseId(courseJava.getId())
                .teacherId(teacherA.getId())
                .status("ACTIVE")
                .build();

        MvcResult result = mockMvc.perform(post("/api/admin/allocations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseId").value(courseJava.getId().toString()))
                .andExpect(jsonPath("$.data.teacherId").value(teacherA.getId().toString()))
                .andReturn();

        // Verify Enrollment is stored in database
        assertTrue(enrollmentRepository.existsByStudentIdAndCourseId(student1.getId(), courseJava.getId()),
                "Enrollment must exist in database");

        // Teacher A sees Student 1 in their student list
        mockMvc.perform(get("/api/teacher/students")
                        .header("Authorization", "Bearer " + teacherAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(student1.getId().toString()));
    }

    @Test
    @DisplayName("3. Teacher Isolation: Teacher A cannot view Teacher B's students or access Teacher B's student details")
    void testTeacherIsolation() throws Exception {
        // Allocate Student 1 -> Teacher A (Java)
        Enrollment enrollA = new Enrollment();
        enrollA.setStudent(student1);
        enrollA.setCourse(courseJava);
        enrollA.setTeacher(teacherA);
        enrollA.setStatus("ACTIVE");
        enrollmentRepository.save(enrollA);

        // Allocate Student 3 -> Teacher B (Python)
        Enrollment enrollB = new Enrollment();
        enrollB.setStudent(student3);
        enrollB.setCourse(coursePython);
        enrollB.setTeacher(teacherB);
        enrollB.setStatus("ACTIVE");
        enrollmentRepository.save(enrollB);

        // Teacher A sees only Student 1 (length = 1)
        mockMvc.perform(get("/api/teacher/students")
                        .header("Authorization", "Bearer " + teacherAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(student1.getId().toString()));

        // Teacher B sees only Student 3 (length = 1)
        mockMvc.perform(get("/api/teacher/students")
                        .header("Authorization", "Bearer " + teacherBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(student3.getId().toString()));

        // Teacher A attempts direct access to Student 3 details -> 403 Forbidden
        mockMvc.perform(get("/api/teacher/students/" + student3.getId())
                        .header("Authorization", "Bearer " + teacherAToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Teacher Course Authorization: Teacher A cannot create content for Course B (Python)")
    void testTeacherCourseAuthorization() throws Exception {
        CreateAssignmentRequest badAssignmentReq = CreateAssignmentRequest.builder()
                .courseId(coursePython.getId()) // Teacher A does not teach Python!
                .title("Unauthorized Python Assignment")
                .points(100)
                .build();

        mockMvc.perform(post("/api/teacher/assignments")
                        .header("Authorization", "Bearer " + teacherAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badAssignmentReq)))
                .andExpect(status().isForbidden());

        CreateQuizRequest badQuizReq = CreateQuizRequest.builder()
                .courseId(coursePython.getId())
                .title("Unauthorized Python Quiz")
                .build();

        mockMvc.perform(post("/api/teacher/quizzes")
                        .header("Authorization", "Bearer " + teacherAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badQuizReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. Online Class: Teacher A creates Java Live Class -> Student 1 ALLOW (200), Student 3 DENY (403 IDOR)")
    void testLiveClassAccessControl() throws Exception {
        // Allocate Student 1 -> Java
        Enrollment enrollA = new Enrollment();
        enrollA.setStudent(student1);
        enrollA.setCourse(courseJava);
        enrollA.setTeacher(teacherA);
        enrollA.setStatus("ACTIVE");
        enrollmentRepository.save(enrollA);

        // Teacher A creates Live Class for Java
        MvcResult classResult = mockMvc.perform(post("/api/teacher/live-classes")
                        .header("Authorization", "Bearer " + teacherAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Java Spring Boot Live\",\"courseId\":\"" + courseJava.getId() + "\",\"platform\":\"Google Meet\",\"meetingUrl\":\"https://meet.google.com/java-live\",\"visibility\":\"restricted\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String classId = objectMapper.readTree(classResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Student 1 (allocated to Java) can view live class
        mockMvc.perform(get("/api/student/live-classes/" + classId)
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Java Spring Boot Live"));

        // Student 3 (NOT allocated to Java) attempts direct access -> 403 Forbidden
        mockMvc.perform(get("/api/student/live-classes/" + classId)
                        .header("Authorization", "Bearer " + student3Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. Assignment: Teacher A creates Java assignment -> Student 1 can view/submit, Student 3 gets 403 IDOR")
    void testAssignmentAccessControl() throws Exception {
        // Allocate Student 1 -> Java
        Enrollment enrollA = new Enrollment();
        enrollA.setStudent(student1);
        enrollA.setCourse(courseJava);
        enrollA.setTeacher(teacherA);
        enrollA.setStatus("ACTIVE");
        enrollmentRepository.save(enrollA);

        // Teacher A creates Assignment
        CreateAssignmentRequest createReq = CreateAssignmentRequest.builder()
                .courseId(courseJava.getId())
                .title("Build a REST API with Spring Boot")
                .description("Create CRUD endpoints for student management")
                .points(100)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/teacher/assignments")
                        .header("Authorization", "Bearer " + teacherAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        String assignmentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Assignments are now created as DRAFT and must be published before students see
        // them, so the teacher publishes before the student lists.
        mockMvc.perform(put("/api/teacher/assignments/" + assignmentId + "/publish")
                        .header("Authorization", "Bearer " + teacherAToken))
                .andExpect(status().isOk());

        // Student 1 lists assignments -> sees 1 assignment
        mockMvc.perform(get("/api/student/assignments")
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        // Student 1 submits assignment -> 200 OK
        SubmitAssignmentRequest submitReq = SubmitAssignmentRequest.builder()
                .submissionUrl("https://github.com/student1/spring-boot-crud")
                .notes("Completed all requirements")
                .build();

        MvcResult submitResult = mockMvc.perform(post("/api/student/assignments/" + assignmentId + "/submit")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("Submitted"))
                .andReturn();

        String submissionId = objectMapper.readTree(submitResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Teacher A grades submission -> 200 OK
        GradeSubmissionRequest gradeReq = GradeSubmissionRequest.builder()
                .grade(95)
                .feedback("Excellent implementation!")
                .build();

        mockMvc.perform(post("/api/teacher/assignments/" + assignmentId + "/submissions/" + submissionId + "/grade")
                        .header("Authorization", "Bearer " + teacherAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gradeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grade").value(95));

        // Student 3 (NOT enrolled in Java) attempts IDOR access to assignment -> 403 Forbidden
        mockMvc.perform(get("/api/student/assignments/" + assignmentId)
                        .header("Authorization", "Bearer " + student3Token))
                .andExpect(status().isForbidden());

        // Student 3 attempts to submit to Java assignment -> 403 Forbidden
        mockMvc.perform(post("/api/student/assignments/" + assignmentId + "/submit")
                        .header("Authorization", "Bearer " + student3Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7. Quiz: Teacher A creates Java Quiz -> Student 1 can attempt, Student 3 gets 403 IDOR")
    void testQuizAccessControl() throws Exception {
        // Allocate Student 1 -> Java
        Enrollment enrollA = new Enrollment();
        enrollA.setStudent(student1);
        enrollA.setCourse(courseJava);
        enrollA.setTeacher(teacherA);
        enrollA.setStatus("ACTIVE");
        enrollmentRepository.save(enrollA);

        // Teacher A creates Quiz
        CreateQuizRequest createReq = CreateQuizRequest.builder()
                .courseId(courseJava.getId())
                .title("Java Core Assessment")
                .description("Test on Collections and Streams")
                .timeLimitMinutes(30)
                .passingScore(70)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/teacher/quizzes")
                        .header("Authorization", "Bearer " + teacherAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        String quizId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // A quiz is now created as DRAFT and needs at least one question before it can be
        // published, so a student never meets an empty or half-built quiz. Complete that
        // lifecycle before attempting - this is the real workflow, not a test workaround.
        mockMvc.perform(post("/api/teacher/quizzes/" + quizId + "/questions")
                        .header("Authorization", "Bearer " + teacherAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionText\":\"Which interface backs ArrayList?\",\"options\":["
                                + "{\"optionText\":\"List\",\"correct\":true},"
                                + "{\"optionText\":\"Map\",\"correct\":false}]}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/teacher/quizzes/" + quizId + "/publish")
                        .header("Authorization", "Bearer " + teacherAToken))
                .andExpect(status().isOk());

        // Student 1 attempts the quiz -> 200 OK.
        //
        // This previously posted score=85 and asserted the response echoed 85/passed=true,
        // which is exactly the hole that has now been closed: the score is computed from the
        // server-side answer key, so an unanswered attempt scores 0 no matter what is posted.
        QuizAttemptRequest attemptReq = QuizAttemptRequest.builder()
                .score(85)
                .build();

        mockMvc.perform(post("/api/student/quizzes/" + quizId + "/attempt")
                        .header("Authorization", "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attemptReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(0))
                .andExpect(jsonPath("$.data.passed").value(false));

        // Student 3 attempts IDOR access to Java quiz -> 403 Forbidden
        mockMvc.perform(get("/api/student/quizzes/" + quizId)
                        .header("Authorization", "Bearer " + student3Token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/student/quizzes/" + quizId + "/attempt")
                        .header("Authorization", "Bearer " + student3Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attemptReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("8. Course Materials: Teacher A uploads Java material -> Student 1 can view, Student 3 gets 403 IDOR")
    void testCourseMaterialAccessControl() throws Exception {
        // Allocate Student 1 -> Java
        Enrollment enrollA = new Enrollment();
        enrollA.setStudent(student1);
        enrollA.setCourse(courseJava);
        enrollA.setTeacher(teacherA);
        enrollA.setStatus("ACTIVE");
        enrollmentRepository.save(enrollA);

        // Teacher A uploads material
        CreateCourseMaterialRequest createReq = CreateCourseMaterialRequest.builder()
                .courseId(courseJava.getId())
                .title("Spring Boot Architecture Cheat Sheet")
                .description("Summary of Spring DI and IoC Container")
                .fileUrl("https://storage.learntrix.com/materials/spring-boot.pdf")
                .fileType("application/pdf")
                .fileSizeBytes(1024000L)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/teacher/materials")
                        .header("Authorization", "Bearer " + teacherAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        String materialId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Student 1 can view material -> 200 OK
        mockMvc.perform(get("/api/student/materials/" + materialId)
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Spring Boot Architecture Cheat Sheet"));

        // Student 3 attempts IDOR access -> 403 Forbidden
        mockMvc.perform(get("/api/student/materials/" + materialId)
                        .header("Authorization", "Bearer " + student3Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("9. Course Announcement: Teacher A posts announcement -> Student 1 can view, Student 3 gets 403 IDOR")
    void testCourseAnnouncementAccessControl() throws Exception {
        // Allocate Student 1 -> Java
        Enrollment enrollA = new Enrollment();
        enrollA.setStudent(student1);
        enrollA.setCourse(courseJava);
        enrollA.setTeacher(teacherA);
        enrollA.setStatus("ACTIVE");
        enrollmentRepository.save(enrollA);

        // Teacher A posts announcement
        CreateCourseAnnouncementRequest createReq = CreateCourseAnnouncementRequest.builder()
                .courseId(courseJava.getId())
                .title("Java Hackathon Scheduled for this Weekend")
                .content("Prepare your Spring Boot projects for presentation.")
                .priority("HIGH")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/teacher/announcements")
                        .header("Authorization", "Bearer " + teacherAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        String announcementId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Student 1 can view announcement -> 200 OK
        mockMvc.perform(get("/api/student/announcements/" + announcementId)
                        .header("Authorization", "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Java Hackathon Scheduled for this Weekend"));

        // Student 3 attempts IDOR access -> 403 Forbidden
        mockMvc.perform(get("/api/student/announcements/" + announcementId)
                        .header("Authorization", "Bearer " + student3Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("10. Reassignment Test: Admin reassigns Student 2 from Teacher A/Java to Teacher B/Python -> Access Transferred")
    void testStudentReassignment() throws Exception {
        // Step 1: Initial Allocation: Student 2 -> Teacher A (Java)
        AllocateStudentRequest initialReq = AllocateStudentRequest.builder()
                .studentId(student2.getId())
                .courseId(courseJava.getId())
                .teacherId(teacherA.getId())
                .status("ACTIVE")
                .build();

        MvcResult allocRes = mockMvc.perform(post("/api/admin/allocations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initialReq)))
                .andExpect(status().isOk())
                .andReturn();

        String allocationId = objectMapper.readTree(allocRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Student 2 can access Java recordings, denied Python recordings
        mockMvc.perform(get("/api/student/recordings/" + recordingJava.getId())
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/student/recordings/" + recordingPython.getId())
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isForbidden());

        // Step 2: Admin reassigns Student 2 to Teacher B (Python)
        AllocateStudentRequest reassignReq = AllocateStudentRequest.builder()
                .studentId(student2.getId())
                .courseId(coursePython.getId())
                .teacherId(teacherB.getId())
                .status("ACTIVE")
                .build();

        mockMvc.perform(put("/api/admin/allocations/" + allocationId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reassignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseId").value(coursePython.getId().toString()))
                .andExpect(jsonPath("$.data.teacherId").value(teacherB.getId().toString()));

        // Step 3: Verify Student 2 now has access to Python and loses access to Java!
        mockMvc.perform(get("/api/student/recordings/" + recordingPython.getId())
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/student/recordings/" + recordingJava.getId())
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11. Removal Test: Admin removes Student 2 allocation -> Student immediately loses all content access")
    void testAllocationRemoval() throws Exception {
        // Step 1: Allocate Student 2 -> Teacher A (Java)
        AllocateStudentRequest initialReq = AllocateStudentRequest.builder()
                .studentId(student2.getId())
                .courseId(courseJava.getId())
                .teacherId(teacherA.getId())
                .status("ACTIVE")
                .build();

        MvcResult allocRes = mockMvc.perform(post("/api/admin/allocations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initialReq)))
                .andExpect(status().isOk())
                .andReturn();

        String allocationId = objectMapper.readTree(allocRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Verify Student 2 has access
        mockMvc.perform(get("/api/student/recordings/" + recordingJava.getId())
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isOk());

        // Step 2: Admin removes allocation
        mockMvc.perform(delete("/api/admin/allocations/" + allocationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Step 3: Student 2 immediately loses access -> 403 Forbidden
        mockMvc.perform(get("/api/student/recordings/" + recordingJava.getId())
                        .header("Authorization", "Bearer " + student2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("12. Admin Onboards Teacher -> Account (PENDING), Profile, and Activation Token Generated")
    void testAdminTeacherOnboardingAndActivationFlow() throws Exception {
        CreateTeacherRequest req = CreateTeacherRequest.builder()
                .fullName("Dr. Ananya Roy")
                .email("ananya.roy@test.com")
                .phone("+91 91234 56789")
                .headline("Senior Cloud Architect")
                .department("Cloud Computing")
                .qualification("Ph.D.")
                .experienceYears(10)
                .skills("AWS, Docker, Kubernetes")
                .bio("10+ years designing distributed systems")
                .build();

        MvcResult result = mockMvc.perform(post("/api/admin/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("ananya.roy@test.com"))
                .andExpect(jsonPath("$.data.role").value("teacher"))
                .andExpect(jsonPath("$.data.onboardingStatus").value("INVITED"))
                .andExpect(jsonPath("$.data.emailStatus").value("NOT_CONFIGURED"))
                .andExpect(jsonPath("$.data.message").value("Teacher onboarded successfully. The activation email could NOT be delivered - share the activation link manually."))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        UUID createdTeacherUserId = UUID.fromString(data.path("userId").asText());

        // Verify User entity state
        User createdUser = userRepository.findById(createdTeacherUserId).orElseThrow();
        assertEquals("PENDING", createdUser.getStatus());
        assertFalse(createdUser.isEmailVerified());
        assertTrue(createdUser.getRoles().stream().anyMatch(r -> "TEACHER".equalsIgnoreCase(r.getName())));

        // Verify TeacherProfile entity state
        assertTrue(teacherProfileRepository.findByUserId(createdTeacherUserId).isPresent());
        TeacherProfile profile = teacherProfileRepository.findByUserId(createdTeacherUserId).get();
        assertNotNull(profile.getEmployeeId());
        assertTrue(profile.getEmployeeId().startsWith("LTX-T-2026-"));

        // Verify Activation Token in DB
        String token = createdUser.getPasswordResetToken();
        assertNotNull(token);
        assertTrue(activationTokenRepository.findByToken(token).isPresent());
        AccountActivationToken actToken = activationTokenRepository.findByToken(token).get();
        assertFalse(actToken.isUsed());
        assertFalse(actToken.isExpired());

        // Teacher activates account via /api/auth/activate
        ResetPasswordRequest activateReq = new ResetPasswordRequest();
        activateReq.setToken(token);
        activateReq.setPassword("NewTeacherSecurePassword123!");

        mockMvc.perform(post("/api/auth/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activated").value(true));

        // Verify user is now ACTIVE and email is verified
        User activatedUser = userRepository.findById(createdTeacherUserId).orElseThrow();
        assertEquals("ACTIVE", activatedUser.getStatus());
        assertTrue(activatedUser.isEmailVerified());

        // Token must now be marked used
        AccountActivationToken usedToken = activationTokenRepository.findByToken(token).orElseThrow();
        assertTrue(usedToken.isUsed());

        // Re-use must be rejected with 400
        mockMvc.perform(post("/api/auth/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activateReq)))
                .andExpect(status().isBadRequest());

        // Teacher logs in with new password
        String newTeacherJwt = loginToken("ananya.roy@test.com", "NewTeacherSecurePassword123!");
        assertNotNull(newTeacherJwt);
    }

    @Test
    @DisplayName("13. Admin Resend Teacher Activation Email Invalidates Prior Token and Generates Fresh Token")
    void testAdminResendTeacherActivationEmailAndTokenInvalidation() throws Exception {
        CreateTeacherRequest req = CreateTeacherRequest.builder()
                .fullName("Prof. Suresh Kumar")
                .email("suresh.kumar@test.com")
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/admin/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data");
        UUID teacherUserId = UUID.fromString(data.path("userId").asText());

        User initialUser = userRepository.findById(teacherUserId).orElseThrow();
        String initialToken = initialUser.getPasswordResetToken();
        assertNotNull(initialToken);

        // Admin resends welcome email
        mockMvc.perform(post("/api/admin/teachers/" + teacherUserId + "/resend-welcome-email")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.emailStatus").value("NOT_CONFIGURED"))
                .andExpect(jsonPath("$.data.message").value("Activation link regenerated. The activation email could NOT be delivered - share the activation link manually."));

        // Prior token must be marked used/invalidated in account_activation_tokens
        AccountActivationToken oldActToken = activationTokenRepository.findByToken(initialToken).orElseThrow();
        assertTrue(oldActToken.isUsed());

        // User must have a new token
        User refreshedUser = userRepository.findById(teacherUserId).orElseThrow();
        String newToken = refreshedUser.getPasswordResetToken();
        assertNotNull(newToken);
        assertNotEquals(initialToken, newToken);

        // Attempting to activate with old token fails
        ResetPasswordRequest oldReq = new ResetPasswordRequest();
        oldReq.setToken(initialToken);
        oldReq.setPassword("Password123!");
        mockMvc.perform(post("/api/auth/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldReq)))
                .andExpect(status().isBadRequest());

        // Activating with new token succeeds
        ResetPasswordRequest newReq = new ResetPasswordRequest();
        newReq.setToken(newToken);
        newReq.setPassword("Password123!");
        mockMvc.perform(post("/api/auth/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activated").value(true));
    }
}
