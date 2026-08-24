package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.config.JwtProvider;
import com.learntrix.edtech.common.exception.BusinessException;
import com.learntrix.edtech.common.exception.ConflictException;
import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.auth.LoginRequest;
import com.learntrix.edtech.dto.auth.LoginResponse;
import com.learntrix.edtech.dto.auth.RegisterRequest;
import com.learntrix.edtech.dto.auth.UserResponse;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpirationMs;

    public AuthController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StudentProfileRepository studentProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
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

    @PostMapping("/logout")
    public ApiResponse<String> logout() {
        return ApiResponse.success("Successfully logged out");
    }

    private LoginResponse buildLoginResponse(User user, boolean remember) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName()))
                .collect(Collectors.toList());

        Authentication auth = new UsernamePasswordAuthenticationToken(user.getId().toString(), null, authorities);
        String token = jwtProvider.generateToken(auth, user.getId().toString());

        long expiration = remember ? (30L * 24 * 3600 * 1000) : accessTokenExpirationMs;
        Instant expiresAt = Instant.now().plusMillis(expiration);

        return LoginResponse.builder()
                .user(mapToUserResponse(user))
                .accessToken(token)
                .expiresAt(expiresAt)
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        String roleStr = "student";
        if (user.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()) || "SUPER_ADMIN".equalsIgnoreCase(r.getName()))) {
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
                .build();
    }
}
