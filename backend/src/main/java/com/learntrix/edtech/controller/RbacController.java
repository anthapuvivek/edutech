package com.learntrix.edtech.controller;

import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.common.response.ApiResponse;
import com.learntrix.edtech.dto.rbac.RoleDefinitionResponse;
import com.learntrix.edtech.dto.rbac.StaffMemberResponse;
import com.learntrix.edtech.entity.Permission;
import com.learntrix.edtech.entity.Role;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.PermissionRepository;
import com.learntrix.edtech.repository.RoleRepository;
import com.learntrix.edtech.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Role and permission registry.
 *
 * The client caches {@code GET /rbac/roles} at sign-in and uses it to hide navigation and
 * to fail privileged calls fast. It is defence in depth only — every endpoint still
 * re-checks its own authority server side.
 */
@RestController
@RequestMapping("/api/rbac")
public class RbacController {

    /** Roles that are staff-facing; STUDENT is excluded from the staff directory. */
    private static final List<String> STAFF_ROLES = List.of(
            "SUPER_ADMIN", "ADMIN", "TEACHER", "MENTOR", "PLACEMENT_OFFICER", "SUPPORT_AGENT", "COUNSELLOR");

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public RbacController(RoleRepository roleRepository,
                          PermissionRepository permissionRepository,
                          UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Readable by any authenticated user: the client needs its own role's permission list
     * to render. It exposes role definitions only, never user assignments.
     */
    @Transactional(readOnly = true)
    @GetMapping("/roles")
    public ApiResponse<List<RoleDefinitionResponse>> getRoles() {
        List<RoleDefinitionResponse> definitions = roleRepository.findAll().stream()
                .map(this::mapToRoleDefinition)
                .sorted(Comparator.comparing(RoleDefinitionResponse::getRole))
                .collect(Collectors.toList());
        return ApiResponse.success(definitions);
    }

    @Transactional(readOnly = true)
    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<StaffMemberResponse>> getStaff(
            @RequestParam(value = "role", required = false) String role) {

        boolean allRoles = role == null || role.isBlank() || "all".equalsIgnoreCase(role);
        String wanted = allRoles ? null : role.trim().toUpperCase();

        List<StaffMemberResponse> staff = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> STAFF_ROLES.contains(r.getName().toUpperCase())))
                .filter(u -> allRoles || u.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase(wanted)))
                .map(this::mapToStaffMember)
                .sorted(Comparator.comparing(StaffMemberResponse::getName))
                .collect(Collectors.toList());

        return ApiResponse.success(staff);
    }

    @Transactional
    @PatchMapping("/staff/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<StaffMemberResponse> updateStaffStatus(@PathVariable("id") UUID id,
                                                              @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        String status = body.get("status");
        if (status != null && !status.isBlank()) {
            user.setStatus(status.trim().toUpperCase());
            userRepository.save(user);
        }
        return ApiResponse.success(mapToStaffMember(user));
    }

    @Transactional
    @PatchMapping("/roles/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<RoleDefinitionResponse> updateRolePermissions(@PathVariable("role") String role,
                                                                     @RequestBody Map<String, List<String>> body) {
        Role entity = roleRepository.findByName(role.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", role));

        List<String> names = body.getOrDefault("permissions", List.of());
        Set<Permission> resolved = names.isEmpty()
                ? new HashSet<>()
                : new HashSet<>(permissionRepository.findByNameIn(names));

        entity.setPermissions(resolved);
        roleRepository.save(entity);

        return ApiResponse.success(mapToRoleDefinition(entity));
    }

    private RoleDefinitionResponse mapToRoleDefinition(Role role) {
        List<String> permissions = role.getPermissions().stream()
                .map(Permission::getName)
                .sorted()
                .collect(Collectors.toList());

        return RoleDefinitionResponse.builder()
                .role(role.getName().toLowerCase())
                .label(role.getDisplayName())
                .description(role.getDescription())
                .permissions(permissions)
                .build();
    }

    private StaffMemberResponse mapToStaffMember(User user) {
        String roleKey = user.getRoles().stream()
                .map(Role::getName)
                .filter(name -> STAFF_ROLES.contains(name.toUpperCase()))
                .min(Comparator.comparingInt((String name) -> STAFF_ROLES.indexOf(name.toUpperCase())))
                .map(String::toLowerCase)
                .orElse("student");

        return StaffMemberResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(roleKey)
                .status(mapStatus(user.getStatus()))
                .assignedCount(0)
                .createdAt(user.getCreatedAt())
                .build();
    }

    /** users.status is ACTIVE/PENDING/SUSPENDED; the client models inactive rather than pending. */
    private String mapStatus(String status) {
        if (status == null) return "inactive";
        return switch (status.toUpperCase()) {
            case "ACTIVE" -> "active";
            case "SUSPENDED" -> "suspended";
            default -> "inactive";
        };
    }
}
