package com.learntrix.edtech.service;

import com.learntrix.edtech.common.exception.ConflictException;
import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.dto.admin.CreateStudentRequest;
import com.learntrix.edtech.dto.admin.CreateTeacherRequest;
import com.learntrix.edtech.dto.admin.OnboardingResponse;
import com.learntrix.edtech.entity.*;
import com.learntrix.edtech.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminOnboardingService {

    private static final long ACTIVATION_EXPIRATION_SECONDS = 86400L; // 24 hours

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final AccountActivationTokenRepository activationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final BatchRepository batchRepository;

    public AdminOnboardingService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StudentProfileRepository studentProfileRepository,
            TeacherProfileRepository teacherProfileRepository,
            AccountActivationTokenRepository activationTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            BatchRepository batchRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.batchRepository = batchRepository;
    }

    /**
     * Atomically onboards a new Student: creates User (PENDING), StudentProfile,
     * one-time activation token, and dispatches the welcome email.
     */
    @Transactional
    public OnboardingResponse onboardStudent(CreateStudentRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        String email = request.getEmail().trim().toLowerCase();
        String fullName = request.resolveName();
        if (fullName.isEmpty()) {
            throw new IllegalArgumentException("Full name is required");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "A student with email '" + email + "' already exists.");
        }

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "STUDENT"));

        // Use supplied password or generate random temporary secret
        boolean hasExplicitPassword = request.getPassword() != null && !request.getPassword().trim().isEmpty();
        String initialPassword = hasExplicitPassword ? request.getPassword().trim() : UUID.randomUUID().toString();

        // Generate activation token
        String token = generateSecureActivationToken();
        Instant expiresAt = Instant.now().plusSeconds(ACTIVATION_EXPIRATION_SECONDS);

        User user = new User();
        user.setEmail(email);
        user.setName(fullName);
        user.setPasswordHash(passwordEncoder.encode(initialPassword));
        user.setStatus(hasExplicitPassword ? (request.getAccountStatus() != null ? request.getAccountStatus().toUpperCase() : "ACTIVE") : "PENDING");
        user.setEmailVerified(hasExplicitPassword);
        user.setRoles(new HashSet<>(Set.of(studentRole)));
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(expiresAt);

        User savedUser = userRepository.save(user);

        // Generate unique Student ID
        String studentId = generateUniqueStudentId();

        // Build StudentProfile
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(savedUser.getId());
        profile.setStudentId(studentId);
        profile.setFullName(fullName);
        profile.setPhone(request.getPhone());
        profile.setEducation(request.getEducation());
        profile.setCollege(request.getCollege());
        profile.setGraduationYear(request.getGraduationYear());
        profile.setQualification(request.getQualification());
        profile.setLocation(request.getLocation());
        profile.setGender(request.getGender());

        if (request.getDateOfBirth() != null && !request.getDateOfBirth().trim().isEmpty()) {
            try {
                profile.setDateOfBirth(LocalDate.parse(request.getDateOfBirth().trim()));
            } catch (Exception ignored) { }
        }

        if (request.getSkills() != null && !request.getSkills().trim().isEmpty()) {
            profile.setSkills(Arrays.stream(request.getSkills().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        }

        profile.setGithubUrl(request.getGithub());
        profile.setLinkedinUrl(request.getLinkedin());
        profile.setLeetcodeUrl(request.getLeetcode());
        profile.setHackerrankUrl(request.getHackerrank());
        profile.setProfileCompletionPercent(0);
        profile.setPoints(0);
        profile.setRankVal(0);
        profile.setStreakDays(0);
        profile.setLevelName("Beginner");

        studentProfileRepository.save(profile);

        // Persist secure activation token record
        AccountActivationToken activationToken = new AccountActivationToken();
        activationToken.setToken(token);
        activationToken.setUserId(savedUser.getId());
        activationToken.setTokenType("ONBOARDING_ACTIVATION");
        activationToken.setExpiresAt(expiresAt);
        activationTokenRepository.save(activationToken);

        // Optional immediate allocation
        if (request.getCourseId() != null) {
            allocateStudentInternal(savedUser, request.getCourseId(), request.getTeacherId(), request.getBatchId(), "ACTIVE");
        }

        // Dispatch Welcome Email
        boolean emailSent = emailService.sendWelcomeActivationEmail(savedUser, "Student", studentId, token);

        return OnboardingResponse.builder()
                .id(savedUser.getId())
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .role("student")
                .identifier(studentId)
                .onboardingStatus("INVITED")
                .emailStatus(emailSent ? "SENT" : "FAILED")
                .message(emailSent ? "Student onboarded successfully. Activation email sent." : "Student created, but activation email could not be sent.")
                .ok(true)
                .build();
    }

    /**
     * Allocates a student to a teacher and course (with optional batch).
     */
    @Transactional
    public com.learntrix.edtech.dto.admin.StudentAllocationResponse allocateStudent(
            com.learntrix.edtech.dto.admin.AllocateStudentRequest request) {
        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getStudentId()));

        Enrollment enrollment = allocateStudentInternal(
                student,
                request.getCourseId(),
                request.getTeacherId(),
                request.getBatchId(),
                request.getStatus() != null ? request.getStatus() : "ACTIVE"
        );

        return mapToAllocationResponse(enrollment);
    }

    private Enrollment allocateStudentInternal(User student, UUID courseId, UUID teacherId, UUID batchId, String status) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        User teacher = null;
        if (teacherId != null) {
            teacher = userRepository.findById(teacherId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));
        } else if (course.getInstructor() != null) {
            teacher = course.getInstructor();
        }

        Batch batch = null;
        if (batchId != null) {
            batch = batchRepository.findById(batchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId));
            if (batch.getStudents() != null && !batch.getStudents().contains(student)) {
                batch.getStudents().add(student);
                batchRepository.save(batch);
            }
            if (teacher == null && batch.getTeacher() != null) {
                teacher = batch.getTeacher();
            }
        }

        Optional<Enrollment> existingOpt = enrollmentRepository.findByStudentIdAndCourseId(student.getId(), course.getId());
        Enrollment enrollment;
        if (existingOpt.isPresent()) {
            enrollment = existingOpt.get();
            enrollment.setStatus(status != null ? status : "ACTIVE");
            if (teacher != null) enrollment.setTeacher(teacher);
            if (batch != null) enrollment.setBatch(batch);
        } else {
            enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setCourse(course);
            enrollment.setTeacher(teacher);
            enrollment.setBatch(batch);
            enrollment.setStatus(status != null ? status : "ACTIVE");
        }

        return enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public List<com.learntrix.edtech.dto.admin.StudentAllocationResponse> getStudentAllocations(UUID studentId) {
        return enrollmentRepository.findByStudentId(studentId).stream()
                .map(this::mapToAllocationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public com.learntrix.edtech.dto.admin.StudentAllocationResponse reassignAllocation(
            UUID allocationId,
            com.learntrix.edtech.dto.admin.AllocateStudentRequest request) {
        Enrollment enrollment = enrollmentRepository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", allocationId));

        User student = enrollment.getStudent();

        // If batch is changing, remove from previous batch
        if (enrollment.getBatch() != null && (request.getBatchId() == null || !enrollment.getBatch().getId().equals(request.getBatchId()))) {
            Batch prevBatch = enrollment.getBatch();
            if (prevBatch.getStudents() != null) {
                prevBatch.getStudents().removeIf(s -> s.getId().equals(student.getId()));
                batchRepository.save(prevBatch);
            }
            enrollment.setBatch(null);
        }

        if (request.getCourseId() != null && !enrollment.getCourse().getId().equals(request.getCourseId())) {
            Course newCourse = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));
            enrollment.setCourse(newCourse);
        }

        if (request.getTeacherId() != null) {
            User newTeacher = userRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getTeacherId()));
            enrollment.setTeacher(newTeacher);
        }

        if (request.getBatchId() != null) {
            Batch newBatch = batchRepository.findById(request.getBatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", request.getBatchId()));
            if (newBatch.getStudents() != null && !newBatch.getStudents().contains(student)) {
                newBatch.getStudents().add(student);
                batchRepository.save(newBatch);
            }
            enrollment.setBatch(newBatch);
            if (enrollment.getTeacher() == null && newBatch.getTeacher() != null) {
                enrollment.setTeacher(newBatch.getTeacher());
            }
        }

        if (request.getStatus() != null) {
            enrollment.setStatus(request.getStatus());
        }

        Enrollment saved = enrollmentRepository.save(enrollment);
        return mapToAllocationResponse(saved);
    }

    @Transactional
    public void removeAllocation(UUID allocationId) {
        Enrollment enrollment = enrollmentRepository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", allocationId));

        if (enrollment.getBatch() != null) {
            Batch batch = enrollment.getBatch();
            if (batch.getStudents() != null) {
                batch.getStudents().removeIf(s -> s.getId().equals(enrollment.getStudent().getId()));
                batchRepository.save(batch);
            }
        }

        enrollmentRepository.delete(enrollment);
    }

    private com.learntrix.edtech.dto.admin.StudentAllocationResponse mapToAllocationResponse(Enrollment e) {
        String teacherName = "Not Assigned";
        UUID teacherId = null;
        if (e.getTeacher() != null) {
            teacherName = e.getTeacher().getName();
            teacherId = e.getTeacher().getId();
        } else if (e.getCourse().getInstructor() != null) {
            teacherName = e.getCourse().getInstructor().getName();
            teacherId = e.getCourse().getInstructor().getId();
        }

        String batchName = e.getBatch() != null ? e.getBatch().getName() : "Not Assigned";
        UUID batchId = e.getBatch() != null ? e.getBatch().getId() : null;

        return com.learntrix.edtech.dto.admin.StudentAllocationResponse.builder()
                .id(e.getId())
                .studentId(e.getStudent().getId())
                .studentName(e.getStudent().getName())
                .studentEmail(e.getStudent().getEmail())
                .courseId(e.getCourse().getId())
                .courseTitle(e.getCourse().getTitle())
                .courseSlug(e.getCourse().getSlug())
                .teacherId(teacherId)
                .teacherName(teacherName)
                .batchId(batchId)
                .batchName(batchName)
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .build();
    }

    /**
     * Atomically onboards a new Teacher: creates User (PENDING), TeacherProfile,
     * one-time activation token, and dispatches the welcome email.
     */
    @Transactional
    public OnboardingResponse onboardTeacher(CreateTeacherRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        String email = request.getEmail().trim().toLowerCase();
        String fullName = request.resolveName();
        if (fullName.isEmpty()) {
            throw new IllegalArgumentException("Full name is required");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "A trainer with email '" + email + "' already exists.");
        }

        Role teacherRole = roleRepository.findByName("TEACHER")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "TEACHER"));

        String tempSecret = UUID.randomUUID().toString();
        String token = generateSecureActivationToken();
        Instant expiresAt = Instant.now().plusSeconds(ACTIVATION_EXPIRATION_SECONDS);

        User user = new User();
        user.setEmail(email);
        user.setName(fullName);
        user.setPasswordHash(passwordEncoder.encode(tempSecret));
        user.setStatus("PENDING");
        user.setEmailVerified(false);
        user.setRoles(new HashSet<>(Set.of(teacherRole)));
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(expiresAt);

        User savedUser = userRepository.save(user);

        // Generate unique Employee ID
        String employeeId = generateUniqueEmployeeId();

        // Build TeacherProfile
        TeacherProfile profile = new TeacherProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(savedUser.getId());
        profile.setEmployeeId(employeeId);
        profile.setFullName(fullName);
        profile.setPhone(request.getPhone());
        profile.setHeadline(request.getHeadline() != null ? request.getHeadline() : "Trainer");
        profile.setDepartment(request.getDepartment() != null ? request.getDepartment() : "Academics");
        profile.setQualification(request.getQualification());
        profile.setExperienceYears(request.getExperienceYears() != null ? request.getExperienceYears() : 0);

        if (request.getSkills() != null && !request.getSkills().trim().isEmpty()) {
            profile.setSkills(Arrays.stream(request.getSkills().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        }

        profile.setBio(request.getBio());
        profile.setApprovalStatus(request.getApprovalStatus() != null ? request.getApprovalStatus() : "approved");
        profile.setRating(java.math.BigDecimal.valueOf(5.0));

        teacherProfileRepository.save(profile);

        // Persist activation token record
        AccountActivationToken activationToken = new AccountActivationToken();
        activationToken.setToken(token);
        activationToken.setUserId(savedUser.getId());
        activationToken.setTokenType("ONBOARDING_ACTIVATION");
        activationToken.setExpiresAt(expiresAt);
        activationTokenRepository.save(activationToken);

        // Dispatch Welcome Email
        boolean emailSent = emailService.sendWelcomeActivationEmail(savedUser, "Teacher", employeeId, token);

        return OnboardingResponse.builder()
                .id(savedUser.getId())
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .role("teacher")
                .identifier(employeeId)
                .onboardingStatus("INVITED")
                .emailStatus(emailSent ? "SENT" : "FAILED")
                .message(emailSent ? "Teacher onboarded successfully. Activation email sent." : "Trainer created, but activation email could not be sent.")
                .ok(true)
                .build();
    }

    /**
     * Resends the welcome activation email for a student by invalidating prior unused tokens,
     * generating a fresh token, and re-dispatching the email.
     */
    @Transactional
    public OnboardingResponse resendStudentWelcomeEmail(UUID studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("Student ID is required");
        }

        // Support lookup by User ID or StudentProfile ID
        User user = userRepository.findById(studentId)
                .orElseGet(() -> studentProfileRepository.findById(studentId)
                        .map(sp -> userRepository.findById(sp.getUserId()).orElse(null))
                        .orElse(null));

        if (user == null) {
            throw new ResourceNotFoundException("Student", "id", studentId);
        }

        if (user.getRoles().stream().noneMatch(r -> "STUDENT".equalsIgnoreCase(r.getName()))) {
            throw new com.learntrix.edtech.common.exception.BusinessException("INVALID_ROLE", "Target user is not registered as a student.", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new com.learntrix.edtech.common.exception.BusinessException("EMAIL_MISSING", "Student email address is missing.", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        // Invalidate prior unused tokens
        activationTokenRepository.invalidateUnusedTokensForUser(user.getId(), Instant.now());

        // Generate new activation token
        String token = generateSecureActivationToken();
        Instant expiresAt = Instant.now().plusSeconds(ACTIVATION_EXPIRATION_SECONDS);

        AccountActivationToken activationToken = new AccountActivationToken();
        activationToken.setToken(token);
        activationToken.setUserId(user.getId());
        activationToken.setTokenType("ONBOARDING_ACTIVATION");
        activationToken.setExpiresAt(expiresAt);
        activationTokenRepository.save(activationToken);

        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(expiresAt);
        userRepository.save(user);

        String identifier = studentProfileRepository.findByUserId(user.getId())
                .map(StudentProfile::getStudentId)
                .orElse("N/A");

        boolean emailSent = emailService.sendWelcomeActivationEmail(user, "Student", identifier, token);

        return OnboardingResponse.builder()
                .id(user.getId())
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role("student")
                .identifier(identifier)
                .onboardingStatus("INVITED")
                .emailStatus(emailSent ? "SENT" : "FAILED")
                .message(emailSent ? "Welcome activation email resent successfully." : "Failed to send activation email. Please check email server configuration.")
                .ok(true)
                .build();
    }

    /**
     * Resends the welcome activation email for a teacher.
     */
    @Transactional
    public OnboardingResponse resendTeacherWelcomeEmail(UUID teacherId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("Teacher ID is required");
        }

        // Support lookup by User ID or TeacherProfile ID
        User user = userRepository.findById(teacherId)
                .orElseGet(() -> teacherProfileRepository.findById(teacherId)
                        .map(tp -> userRepository.findById(tp.getUserId()).orElse(null))
                        .orElse(null));

        if (user == null) {
            throw new ResourceNotFoundException("Teacher", "id", teacherId);
        }

        if (user.getRoles().stream().noneMatch(r -> "TEACHER".equalsIgnoreCase(r.getName()))) {
            throw new com.learntrix.edtech.common.exception.BusinessException("INVALID_ROLE", "Target user is not registered as a teacher/trainer.", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new com.learntrix.edtech.common.exception.BusinessException("EMAIL_MISSING", "Teacher email address is missing.", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        // Invalidate prior unused tokens
        activationTokenRepository.invalidateUnusedTokensForUser(user.getId(), Instant.now());

        // Generate new activation token
        String token = generateSecureActivationToken();
        Instant expiresAt = Instant.now().plusSeconds(ACTIVATION_EXPIRATION_SECONDS);

        AccountActivationToken activationToken = new AccountActivationToken();
        activationToken.setToken(token);
        activationToken.setUserId(user.getId());
        activationToken.setTokenType("ONBOARDING_ACTIVATION");
        activationToken.setExpiresAt(expiresAt);
        activationTokenRepository.save(activationToken);

        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(expiresAt);
        userRepository.save(user);

        String employeeId = teacherProfileRepository.findByUserId(user.getId())
                .map(TeacherProfile::getEmployeeId)
                .orElse("N/A");

        boolean emailSent = emailService.sendWelcomeActivationEmail(user, "Teacher", employeeId, token);

        return OnboardingResponse.builder()
                .id(user.getId())
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role("teacher")
                .identifier(employeeId)
                .onboardingStatus("INVITED")
                .emailStatus(emailSent ? "SENT" : "FAILED")
                .message(emailSent ? "Welcome activation email resent successfully." : "Failed to send activation email. Please check email server configuration.")
                .ok(true)
                .build();
    }

    /**
     * Securely deactivates and terminates a student account.
     * Prevents admin deletion, revokes tokens, updates enrollment states,
     * and sets account status to INACTIVE.
     */
    @Transactional
    public Map<String, Object> deleteOrDeactivateStudent(UUID studentId, org.springframework.security.core.Authentication authentication) {
        if (studentId == null) {
            throw new IllegalArgumentException("Student ID is required");
        }

        // Support lookup by User ID or StudentProfile ID
        User user = userRepository.findById(studentId)
                .orElseGet(() -> studentProfileRepository.findById(studentId)
                        .map(sp -> userRepository.findById(sp.getUserId()).orElse(null))
                        .orElse(null));

        if (user == null) {
            throw new ResourceNotFoundException("Student", "id", studentId);
        }

        // Prevent deleting administrators
        if (user.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()) || "SUPER_ADMIN".equalsIgnoreCase(r.getName()))) {
            throw new com.learntrix.edtech.common.exception.BusinessException("CANNOT_DELETE_ADMIN", "Cannot delete or deactivate an administrator account.", org.springframework.http.HttpStatus.FORBIDDEN);
        }

        // Validate target role
        if (user.getRoles().stream().noneMatch(r -> "STUDENT".equalsIgnoreCase(r.getName()))) {
            throw new com.learntrix.edtech.common.exception.BusinessException("INVALID_ROLE", "Target user is not a student.", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        // Prevent self-deactivation
        if (authentication != null && authentication.getName() != null && (authentication.getName().equalsIgnoreCase(user.getEmail()) || authentication.getName().equalsIgnoreCase(user.getId().toString()))) {
            throw new com.learntrix.edtech.common.exception.BusinessException("CANNOT_DELETE_SELF", "You cannot delete your own account.", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        // Invalidate unused activation tokens
        activationTokenRepository.invalidateUnusedTokensForUser(user.getId(), Instant.now());

        // Clear sensitive tokens
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);

        // Mark user status as INACTIVE
        user.setStatus("INACTIVE");
        userRepository.save(user);

        // Detach student from active batches
        List<Batch> batches = batchRepository.findByStudentsContaining(user);
        for (Batch batch : batches) {
            batch.getStudents().remove(user);
            batchRepository.save(batch);
        }

        // Update active enrollments to INACTIVE
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(user.getId());
        for (Enrollment enrollment : enrollments) {
            if ("ACTIVE".equalsIgnoreCase(enrollment.getStatus())) {
                enrollment.setStatus("INACTIVE");
                enrollmentRepository.save(enrollment);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);
        response.put("id", user.getId().toString());
        response.put("status", "INACTIVE");
        response.put("message", "Student account has been deactivated successfully.");
        return response;
    }

    /**
     * Securely deactivates and terminates a teacher account.
     * Prevents admin deletion, revokes tokens, updates profile status,
     * and sets account status to INACTIVE.
     */
    @Transactional
    public Map<String, Object> deleteOrDeactivateTeacher(UUID teacherId, org.springframework.security.core.Authentication authentication) {
        if (teacherId == null) {
            throw new IllegalArgumentException("Teacher ID is required");
        }

        // Support lookup by User ID or TeacherProfile ID
        User user = userRepository.findById(teacherId)
                .orElseGet(() -> teacherProfileRepository.findById(teacherId)
                        .map(tp -> userRepository.findById(tp.getUserId()).orElse(null))
                        .orElse(null));

        if (user == null) {
            throw new ResourceNotFoundException("Teacher", "id", teacherId);
        }

        // Prevent deleting administrators
        if (user.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()) || "SUPER_ADMIN".equalsIgnoreCase(r.getName()))) {
            throw new com.learntrix.edtech.common.exception.BusinessException("CANNOT_DELETE_ADMIN", "Cannot delete or deactivate an administrator account.", org.springframework.http.HttpStatus.FORBIDDEN);
        }

        // Validate target role
        if (user.getRoles().stream().noneMatch(r -> "TEACHER".equalsIgnoreCase(r.getName()))) {
            throw new com.learntrix.edtech.common.exception.BusinessException("INVALID_ROLE", "Target user is not registered as a teacher/trainer.", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        // Prevent self-deactivation
        if (authentication != null && authentication.getName() != null && (authentication.getName().equalsIgnoreCase(user.getEmail()) || authentication.getName().equalsIgnoreCase(user.getId().toString()))) {
            throw new com.learntrix.edtech.common.exception.BusinessException("CANNOT_DELETE_SELF", "You cannot delete your own account.", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        // Invalidate unused activation tokens
        activationTokenRepository.invalidateUnusedTokensForUser(user.getId(), Instant.now());

        // Clear sensitive tokens
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);

        // Mark user status as INACTIVE
        user.setStatus("INACTIVE");
        userRepository.save(user);

        // Update TeacherProfile approvalStatus if present
        teacherProfileRepository.findByUserId(user.getId()).ifPresent(profile -> {
            profile.setApprovalStatus("deactivated");
            teacherProfileRepository.save(profile);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);
        response.put("id", user.getId().toString());
        response.put("status", "INACTIVE");
        response.put("message", "Teacher account has been deactivated successfully.");
        return response;
    }

    private String generateSecureActivationToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateUniqueStudentId() {
        for (int i = 0; i < 10; i++) {
            int num = 1000 + (int)(Math.random() * 9000);
            String candidate = "LTX-2026-" + num;
            if (studentProfileRepository.findByStudentId(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "LTX-2026-" + (System.currentTimeMillis() % 1000000);
    }

    private String generateUniqueEmployeeId() {
        for (int i = 0; i < 10; i++) {
            int num = 1000 + (int)(Math.random() * 9000);
            String candidate = "LTX-T-2026-" + num;
            if (!teacherProfileRepository.existsByEmployeeId(candidate)) {
                return candidate;
            }
        }
        return "LTX-T-2026-" + (System.currentTimeMillis() % 1000000);
    }
}
