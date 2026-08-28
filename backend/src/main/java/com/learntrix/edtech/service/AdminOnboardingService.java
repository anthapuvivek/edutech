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

    public AdminOnboardingService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StudentProfileRepository studentProfileRepository,
            TeacherProfileRepository teacherProfileRepository,
            AccountActivationTokenRepository activationTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
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
                .message("Student onboarded successfully. Activation email sent.")
                .ok(true)
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
                .message("Teacher onboarded successfully. Activation email sent.")
                .ok(true)
                .build();
    }

    /**
     * Resends the welcome activation email for a student by invalidating prior unused tokens,
     * generating a fresh token, and re-dispatching the email.
     */
    @Transactional
    public OnboardingResponse resendStudentWelcomeEmail(UUID studentUserId) {
        User user = userRepository.findById(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentUserId));

        if (user.getRoles().stream().noneMatch(r -> "STUDENT".equalsIgnoreCase(r.getName()))) {
            throw new IllegalArgumentException("Target user is not a student");
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

        String studentId = studentProfileRepository.findByUserId(user.getId())
                .map(StudentProfile::getStudentId)
                .orElse("N/A");

        boolean emailSent = emailService.sendWelcomeActivationEmail(user, "Student", studentId, token);

        return OnboardingResponse.builder()
                .id(user.getId())
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role("student")
                .identifier(studentId)
                .onboardingStatus("INVITED")
                .emailStatus(emailSent ? "SENT" : "FAILED")
                .message("Welcome activation email resent successfully.")
                .ok(true)
                .build();
    }

    /**
     * Resends the welcome activation email for a teacher.
     */
    @Transactional
    public OnboardingResponse resendTeacherWelcomeEmail(UUID teacherUserId) {
        User user = userRepository.findById(teacherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherUserId));

        if (user.getRoles().stream().noneMatch(r -> "TEACHER".equalsIgnoreCase(r.getName()))) {
            throw new IllegalArgumentException("Target user is not a teacher");
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
                .message("Welcome activation email resent successfully.")
                .ok(true)
                .build();
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
