package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.config.JwtProvider;
import com.learntrix.edtech.common.exception.BusinessException;
import com.learntrix.edtech.common.exception.ConflictException;
import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.auth.ForgotPasswordRequest;
import com.learntrix.edtech.dto.auth.LoginRequest;
import com.learntrix.edtech.dto.auth.LoginResponse;
import com.learntrix.edtech.dto.auth.RegisterRequest;
import com.learntrix.edtech.dto.auth.ResetPasswordRequest;
import com.learntrix.edtech.dto.auth.UserResponse;
import com.learntrix.edtech.dto.auth.VerifyEmailRequest;
import com.learntrix.edtech.entity.Role;
import com.learntrix.edtech.entity.StudentProfile;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.RoleRepository;
import com.learntrix.edtech.repository.StudentProfileRepository;
import com.learntrix.edtech.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final com.learntrix.edtech.repository.AccountActivationTokenRepository activationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final com.learntrix.edtech.service.EmailService emailService;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.remember-me-expiration:2592000000}")
    private long rememberMeExpirationMs;

    public AuthController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StudentProfileRepository studentProfileRepository,
            com.learntrix.edtech.repository.AccountActivationTokenRepository activationTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider,
            com.learntrix.edtech.service.EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.emailService = emailService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "Invalid email or password.", HttpStatus.BAD_REQUEST));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid email or password.", HttpStatus.BAD_REQUEST);
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException("INACTIVE_USER", "Your account is not active. Status: " + user.getStatus(), HttpStatus.FORBIDDEN);
        }

        LoginResponse response = buildLoginResponse(user, request.isRemember());
        return ApiResponse.success(response);
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "An account already exists with this email address");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName().trim());
        user.setStatus("ACTIVE");
        user.setEmailVerified(true);

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "STUDENT"));
        user.setRoles(Set.of(studentRole));

        User savedUser = userRepository.save(user);

        // Create student profile
        StudentProfile profile = new StudentProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(savedUser.getId());
        profile.setFullName(savedUser.getName());
        // Generate student ID: LTX-2026-XXXX
        int randomNum = 1000 + (int)(Math.random() * 9000);
        profile.setStudentId("LTX-2026-" + randomNum);
        studentProfileRepository.save(profile);

        LoginResponse response = buildLoginResponse(savedUser, false);
        return ApiResponse.success(response);
    }

    @GetMapping("/profile")
    public ApiResponse<UserResponse> getProfile() {
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new ResourceNotFoundException("User", "id", null);
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        return ApiResponse.success(mapToUserResponse(user));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Map<String, Boolean>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(email);
        boolean sent = false;
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String resetToken = UUID.randomUUID().toString().replace("-", "")
                    + UUID.randomUUID().toString().replace("-", "");
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetTokenExpiry(Instant.now().plusSeconds(3600)); // 1 hour
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user, resetToken, 60);
        }
        // Always the same response regardless of whether the address exists, so this endpoint
        // cannot be used to enumerate registered users.
        return ApiResponse.success(Map.of("sent", true));
    }

    @PostMapping({"/reset-password", "/activate"})
    public ApiResponse<Map<String, Boolean>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String token = request.getToken();

        // 1. Check account activation token repository
        Optional<com.learntrix.edtech.entity.AccountActivationToken> activationTokenOpt = activationTokenRepository.findByToken(token);
        if (activationTokenOpt.isPresent()) {
            com.learntrix.edtech.entity.AccountActivationToken activationToken = activationTokenOpt.get();
            if (activationToken.isUsed()) {
                throw new BusinessException("TOKEN_ALREADY_USED", "This activation link has already been used.", HttpStatus.BAD_REQUEST);
            }
            if (activationToken.isExpired()) {
                throw new BusinessException("TOKEN_EXPIRED", "The activation link has expired. Please request a new one.", HttpStatus.BAD_REQUEST);
            }

            User user = userRepository.findById(activationToken.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", activationToken.getUserId()));

            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setStatus(statusAfterPasswordChange(user));
            user.setEmailVerified(true);
            user.setPasswordResetToken(null);
            user.setPasswordResetTokenExpiry(null);
            userRepository.save(user);

            activationToken.setUsedAt(Instant.now());
            activationTokenRepository.save(activationToken);

            return ApiResponse.success(Map.of("ok", true, "activated", true));
        }

        // 2. Fallback to password reset token on user entity
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "Invalid or expired password reset token.", HttpStatus.BAD_REQUEST));

        if (user.getPasswordResetTokenExpiry() != null && user.getPasswordResetTokenExpiry().isBefore(Instant.now())) {
            throw new BusinessException("TOKEN_EXPIRED", "The password reset token has expired.", HttpStatus.BAD_REQUEST);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(statusAfterPasswordChange(user));
        user.setEmailVerified(true);
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        return ApiResponse.success(Map.of("ok", true));
    }

    @PostMapping("/verify-email")
    public ApiResponse<Map<String, Boolean>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        String token = request.getToken();
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "Invalid or expired email verification token.", HttpStatus.BAD_REQUEST));

        if (user.getEmailVerificationTokenExpiry() != null && user.getEmailVerificationTokenExpiry().isBefore(Instant.now())) {
            throw new BusinessException("TOKEN_EXPIRED", "The email verification token has expired.", HttpStatus.BAD_REQUEST);
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);
        userRepository.save(user);

        return ApiResponse.success(Map.of("verified", true));
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout() {
        return ApiResponse.success("Successfully logged out");
    }

    /**
     * Status to persist once a user has set a new password.
     *
     * Setting a password proves control of the mailbox, so it clears PENDING - that is the
     * whole point of the activation link. It must NOT clear an administrative lock: a
     * SUSPENDED account that ran forgot-password would otherwise restore its own access,
     * because the reset endpoint is public and needs no prior session.
     */
    private String statusAfterPasswordChange(User user) {
        String current = user.getStatus();
        if (current == null || current.isBlank()) {
            return "ACTIVE";
        }
        return "PENDING".equalsIgnoreCase(current) ? "ACTIVE" : current.toUpperCase();
    }

    private LoginResponse buildLoginResponse(User user, boolean remember) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getId().toString(), null, resolveAuthorities(user));

        // One lifetime for both the signed token and the expiry we advertise. Deriving
        // them separately let the client keep a session alive long after the JWT died.
        long ttlMillis = remember ? rememberMeExpirationMs : accessTokenExpirationMs;
        String token = jwtProvider.generateToken(auth, user.getId().toString(), ttlMillis);

        return LoginResponse.builder()
                .user(mapToUserResponse(user))
                .accessToken(token)
                .expiresAt(Instant.now().plusMillis(ttlMillis))
                .build();
    }

    /**
     * SUPER_ADMIN additionally carries ROLE_ADMIN so a single hasRole('ADMIN') guard
     * covers both roles. Without this a super admin is routed to the admin console by
     * the UI and then rejected by every endpoint in it.
     */
    private List<GrantedAuthority> resolveAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            if ("SUPER_ADMIN".equalsIgnoreCase(role.getName())) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
        }
        return authorities;
    }

    private UserResponse mapToUserResponse(User user) {
        String roleStr = "student";
        if (user.getRoles().stream().anyMatch(r -> "SUPER_ADMIN".equalsIgnoreCase(r.getName()))) {
            roleStr = "super_admin";
        } else if (user.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()))) {
            roleStr = "admin";
        } else if (user.getRoles().stream().anyMatch(r -> "TEACHER".equalsIgnoreCase(r.getName()))) {
            roleStr = "teacher";
        } else if (user.getRoles().stream().anyMatch(r -> "MENTOR".equalsIgnoreCase(r.getName()))) {
            roleStr = "mentor";
        } else if (user.getRoles().stream().anyMatch(r -> "PLACEMENT_OFFICER".equalsIgnoreCase(r.getName()))) {
            roleStr = "placement_officer";
        } else if (user.getRoles().stream().anyMatch(r -> "SUPPORT_AGENT".equalsIgnoreCase(r.getName()))) {
            roleStr = "support_agent";
        } else if (user.getRoles().stream().anyMatch(r -> "COUNSELLOR".equalsIgnoreCase(r.getName()))) {
            roleStr = "counsellor";
        }

        String studentId = null;
        if ("student".equals(roleStr)) {
            Optional<StudentProfile> profile = studentProfileRepository.findByUserId(user.getId());
            if (profile.isPresent()) {
                studentId = profile.get().getStudentId();
            }
        }

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(roleStr)
                .avatarUrl(user.getAvatarUrl())
                .studentId(studentId)
                .status(user.getStatus() == null ? null : user.getStatus().toLowerCase())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
