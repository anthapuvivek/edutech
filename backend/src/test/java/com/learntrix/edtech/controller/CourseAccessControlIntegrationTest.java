package com.learntrix.edtech.controller;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learntrix.edtech.dto.recording.CreateRecordingRequest;
import com.learntrix.edtech.dto.recording.RecordingProgressRequest;
import com.learntrix.edtech.dto.recording.UpdateRecordingRequest;
import com.learntrix.edtech.entity.ClassRecording;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.Enrollment;
import com.learntrix.edtech.entity.Lesson;
import com.learntrix.edtech.entity.Module;
import com.learntrix.edtech.entity.RecordingStatus;
import com.learntrix.edtech.entity.Role;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.CodingProblemRepository;
import com.learntrix.edtech.repository.CodingSubmissionRepository;
import com.learntrix.edtech.repository.CodingTestCaseRepository;
import com.learntrix.edtech.repository.AccountActivationTokenRepository;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.ClassRecordingRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.EnrollmentRepository;
import com.learntrix.edtech.repository.LessonRepository;
import com.learntrix.edtech.repository.LiveClassRepository;
import com.learntrix.edtech.repository.ModuleRepository;
import com.learntrix.edtech.repository.RecordingWatchProgressRepository;
import com.learntrix.edtech.repository.RoleRepository;
import com.learntrix.edtech.repository.StudentProfileRepository;
import com.learntrix.edtech.repository.TeacherProfileRepository;
import com.learntrix.edtech.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseAccessControlIntegrationTest {

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

    private User studentA;
    private User studentB;
    private User studentC;
    private User teacher1;
    private User teacher2;

    private Course courseJava;
    private Course courseDataScience;
    private Course courseCloud;

    private Module moduleJava;
    private Lesson lessonJava;
    private ClassRecording recordingJava;

    private Module moduleDataScience;
    private Lesson lessonDataScience;
    private ClassRecording recordingDataScience;

    @BeforeEach
    void setUp() {
        progressRepository.deleteAll();
        classRecordingRepository.deleteAll();
        liveClassRepository.deleteAll();
        // coding_problems.batch_id blocks batch deletion in the JPA-generated test
        // schema; production Postgres has ON DELETE SET NULL (V31). Children first.
        codingSubmissionRepository.deleteAll();
        codingTestCaseRepository.deleteAll();
        codingProblemRepository.deleteAll();
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

        Role studentRole = roleRepository.findByName("STUDENT").orElseGet(() -> {
            Role r = new Role();
            r.setName("STUDENT");
            r.setDisplayName("Student");
            return roleRepository.save(r);
        });

        Role teacherRole = roleRepository.findByName("TEACHER").orElseGet(() -> {
            Role r = new Role();
            r.setName("TEACHER");
            r.setDisplayName("Teacher");
            return roleRepository.save(r);
        });

        // 1. Create Teachers
        teacher1 = new User();
        teacher1.setEmail("teacher1@learntrix.com");
        teacher1.setName("Teacher One (Java)");
        teacher1.setPasswordHash(passwordEncoder.encode("Teacher@123456"));
        teacher1.setStatus("ACTIVE");
        teacher1.setEmailVerified(true);
        teacher1.setRoles(Set.of(teacherRole));
        teacher1 = userRepository.save(teacher1);

        teacher2 = new User();
        teacher2.setEmail("teacher2@learntrix.com");
        teacher2.setName("Teacher Two (Data Science)");
        teacher2.setPasswordHash(passwordEncoder.encode("Teacher@123456"));
        teacher2.setStatus("ACTIVE");
        teacher2.setEmailVerified(true);
        teacher2.setRoles(Set.of(teacherRole));
        teacher2 = userRepository.save(teacher2);

        // 2. Create Students
        studentA = new User();
        studentA.setEmail("studenta@learntrix.com");
        studentA.setName("Student A");
        studentA.setPasswordHash(passwordEncoder.encode("Student@123456"));
        studentA.setStatus("ACTIVE");
        studentA.setEmailVerified(true);
        studentA.setRoles(Set.of(studentRole));
        studentA = userRepository.save(studentA);

        studentB = new User();
        studentB.setEmail("studentb@learntrix.com");
        studentB.setName("Student B");
        studentB.setPasswordHash(passwordEncoder.encode("Student@123456"));
        studentB.setStatus("ACTIVE");
        studentB.setEmailVerified(true);
        studentB.setRoles(Set.of(studentRole));
        studentB = userRepository.save(studentB);

        studentC = new User();
        studentC.setEmail("studentc@learntrix.com");
        studentC.setName("Student C");
        studentC.setPasswordHash(passwordEncoder.encode("Student@123456"));
        studentC.setStatus("ACTIVE");
        studentC.setEmailVerified(true);
        studentC.setRoles(Set.of(studentRole));
        studentC = userRepository.save(studentC);

        // 3. Create Courses
        courseJava = new Course();
        courseJava.setTitle("Java Full Stack");
        courseJava.setSlug("java-full-stack");
        courseJava.setCategory("Full Stack");
        courseJava.setStatus("PUBLISHED");
        courseJava.setInstructor(teacher1);
        courseJava = courseRepository.save(courseJava);

        courseDataScience = new Course();
        courseDataScience.setTitle("Data Science");
        courseDataScience.setSlug("data-science");
        courseDataScience.setCategory("Data");
        courseDataScience.setStatus("PUBLISHED");
        courseDataScience.setInstructor(teacher2);
        courseDataScience = courseRepository.save(courseDataScience);

        courseCloud = new Course();
        courseCloud.setTitle("Cloud Computing");
        courseCloud.setSlug("cloud-computing");
        courseCloud.setCategory("Cloud");
        courseCloud.setStatus("PUBLISHED");
        courseCloud.setInstructor(teacher1);
        courseCloud = courseRepository.save(courseCloud);

        // 4. Create Modules and Lessons
        moduleJava = new Module();
        moduleJava.setCourse(courseJava);
        moduleJava.setTitle("Module 1: Spring Boot Core");
        moduleJava.setSequenceNumber(1);
        moduleJava = moduleRepository.save(moduleJava);

        lessonJava = new Lesson();
        lessonJava.setModule(moduleJava);
        lessonJava.setTitle("Lesson 1: Dependency Injection");
        lessonJava.setSequenceNumber(1);
        lessonJava.setDurationSeconds(3600);
        lessonJava = lessonRepository.save(lessonJava);

        recordingJava = new ClassRecording();
        recordingJava.setCourse(courseJava);
        recordingJava.setModule(moduleJava);
        recordingJava.setLesson(lessonJava);
        recordingJava.setTeacher(teacher1);
        recordingJava.setTitle("Class 1 - Intro to Spring Boot");
        recordingJava.setClassDate(Instant.now());
        recordingJava.setStatus(RecordingStatus.PUBLISHED);
        recordingJava.setPublished(true);
        recordingJava.setDurationSeconds(3600);
        recordingJava = classRecordingRepository.save(recordingJava);

        moduleDataScience = new Module();
        moduleDataScience.setCourse(courseDataScience);
        moduleDataScience.setTitle("Module 1: Python for Data Science");
        moduleDataScience.setSequenceNumber(1);
        moduleDataScience = moduleRepository.save(moduleDataScience);

        lessonDataScience = new Lesson();
        lessonDataScience.setModule(moduleDataScience);
        lessonDataScience.setTitle("Lesson 1: Pandas DataFrames");
        lessonDataScience.setSequenceNumber(1);
        lessonDataScience.setDurationSeconds(3600);
        lessonDataScience = lessonRepository.save(lessonDataScience);

        recordingDataScience = new ClassRecording();
        recordingDataScience.setCourse(courseDataScience);
        recordingDataScience.setModule(moduleDataScience);
        recordingDataScience.setLesson(lessonDataScience);
        recordingDataScience.setTeacher(teacher2);
        recordingDataScience.setTitle("Class 1 - Intro to Pandas");
        recordingDataScience.setClassDate(Instant.now());
        recordingDataScience.setStatus(RecordingStatus.PUBLISHED);
        recordingDataScience.setPublished(true);
        recordingDataScience.setDurationSeconds(3600);
        recordingDataScience = classRecordingRepository.save(recordingDataScience);

        // 5. Enrollments:
        // Student A -> Java Full Stack
        Enrollment enrollA = new Enrollment();
        enrollA.setStudent(studentA);
        enrollA.setCourse(courseJava);
        enrollA.setStatus("ACTIVE");
        enrollmentRepository.save(enrollA);

        // Student B -> Data Science
        Enrollment enrollB = new Enrollment();
        enrollB.setStudent(studentB);
        enrollB.setCourse(courseDataScience);
        enrollB.setStatus("ACTIVE");
        enrollmentRepository.save(enrollB);

        // Student C -> Cloud Computing
        Enrollment enrollC = new Enrollment();
        enrollC.setStudent(studentC);
        enrollC.setCourse(courseCloud);
        enrollC.setStatus("ACTIVE");
        enrollmentRepository.save(enrollC);
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
    @DisplayName("Student A: Enrolled in Java Full Stack -> ALLOW Java, DENY Data Science, DENY Cloud Computing")
    void testStudentAAccessRules() throws Exception {
        String tokenA = loginAndGetToken("studenta@learntrix.com", "Student@123456");

        // 1. ALLOW Java Full Stack course recordings
        mockMvc.perform(get("/api/student/recordings/course/" + courseJava.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(recordingJava.getId().toString()));

        // 2. ALLOW Java single recording detail
        mockMvc.perform(get("/api/student/recordings/" + recordingJava.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. ALLOW Java progress query and update
        mockMvc.perform(get("/api/student/recordings/" + recordingJava.getId() + "/progress")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        RecordingProgressRequest progressReq = new RecordingProgressRequest();
        progressReq.setWatchedSeconds(1200);
        progressReq.setDurationSeconds(3600);

        mockMvc.perform(post("/api/student/recordings/" + recordingJava.getId() + "/progress")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(progressReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.watchedSeconds").value(1200));

        // 4. Access check API
        mockMvc.perform(get("/api/courses/" + courseJava.getId() + "/access")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasAccess").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // 5. DENY Data Science (Student A is NOT enrolled) -> 403 Forbidden
        mockMvc.perform(get("/api/student/recordings/course/" + courseDataScience.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/student/recordings/" + recordingDataScience.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/student/recordings/" + recordingDataScience.getId() + "/progress")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(post("/api/student/recordings/" + recordingDataScience.getId() + "/progress")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(progressReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/courses/" + courseDataScience.getId() + "/access")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasAccess").value(false))
                .andExpect(jsonPath("$.data.status").value("NOT_ENROLLED"));

        // 6. DENY Cloud Computing (Student A is NOT enrolled) -> 403 Forbidden
        mockMvc.perform(get("/api/student/recordings/course/" + courseCloud.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Student B: Enrolled in Data Science -> ALLOW Data Science, DENY Java & Cloud")
    void testStudentBAccessRules() throws Exception {
        String tokenB = loginAndGetToken("studentb@learntrix.com", "Student@123456");

        // ALLOW Data Science
        mockMvc.perform(get("/api/student/recordings/course/" + courseDataScience.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(recordingDataScience.getId().toString()));

        // DENY Java
        mockMvc.perform(get("/api/student/recordings/course/" + courseJava.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // DENY Cloud
        mockMvc.perform(get("/api/student/recordings/course/" + courseCloud.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Student C: Enrolled in Cloud Computing -> ALLOW Cloud, DENY Java & Data Science")
    void testStudentCAccessRules() throws Exception {
        String tokenC = loginAndGetToken("studentc@learntrix.com", "Student@123456");

        // ALLOW Cloud
        mockMvc.perform(get("/api/student/recordings/course/" + courseCloud.getId())
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // DENY Java
        mockMvc.perform(get("/api/student/recordings/course/" + courseJava.getId())
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isForbidden());

        // DENY Data Science
        mockMvc.perform(get("/api/student/recordings/course/" + courseDataScience.getId())
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("My Courses returns strictly enrolled courses from PostgreSQL")
    void testMyCoursesReturnsOnlyEnrolledCourses() throws Exception {
        String tokenA = loginAndGetToken("studenta@learntrix.com", "Student@123456");
        String tokenB = loginAndGetToken("studentb@learntrix.com", "Student@123456");
        String tokenC = loginAndGetToken("studentc@learntrix.com", "Student@123456");

        // Student A -> Only Java
        mockMvc.perform(get("/api/student/enrollments")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].courseId").value(courseJava.getId().toString()));

        // Student B -> Only Data Science
        mockMvc.perform(get("/api/student/enrollments")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].courseId").value(courseDataScience.getId().toString()));

        // Student C -> Only Cloud Computing
        mockMvc.perform(get("/api/student/enrollments")
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].courseId").value(courseCloud.getId().toString()));
    }

    @Test
    @DisplayName("Subsequent Enrollment: Student A later enrolls in Data Science -> Access Granted to both")
    void testSubsequentEnrollment() throws Exception {
        String tokenA = loginAndGetToken("studenta@learntrix.com", "Student@123456");

        // 1. Initial State: Denied for Data Science
        mockMvc.perform(get("/api/student/recordings/course/" + courseDataScience.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());

        // 2. Student A enrolls in Data Science
        mockMvc.perform(post("/api/courses/" + courseDataScience.getId() + "/enroll")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Now Student A can access Data Science recordings!
        mockMvc.perform(get("/api/student/recordings/course/" + courseDataScience.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. Student A still denied Cloud Computing
        mockMvc.perform(get("/api/student/recordings/course/" + courseCloud.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());

        // 5. My Courses now returns 2 enrolled courses
        mockMvc.perform(get("/api/student/enrollments")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("Trainer Course Isolation: Teacher 1 cannot manage Teacher 2's course/recordings")
    void testTrainerCourseIsolation() throws Exception {
        String tokenTeacher1 = loginAndGetToken("teacher1@learntrix.com", "Teacher@123456");

        // 1. Teacher 1 tries to create a recording in Data Science (Teacher 2's course) -> 403 Forbidden
        CreateRecordingRequest createReq = new CreateRecordingRequest();
        createReq.setCourseId(courseDataScience.getId());
        createReq.setModuleId(moduleDataScience.getId());
        createReq.setLessonId(lessonDataScience.getId());
        createReq.setTitle("Unauthorized Recording");
        createReq.setClassDate(Instant.now());

        mockMvc.perform(post("/api/recordings")
                        .header("Authorization", "Bearer " + tokenTeacher1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // 2. Teacher 1 tries to update Teacher 2's recording -> 403 Forbidden
        UpdateRecordingRequest updateReq = new UpdateRecordingRequest();
        updateReq.setTitle("Hacked Title");

        mockMvc.perform(put("/api/recordings/" + recordingDataScience.getId())
                        .header("Authorization", "Bearer " + tokenTeacher1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // 3. Teacher 1 tries to delete Teacher 2's recording -> 403 Forbidden
        mockMvc.perform(delete("/api/recordings/" + recordingDataScience.getId())
                        .header("Authorization", "Bearer " + tokenTeacher1))
                .andExpect(status().isForbidden());
    }
}
