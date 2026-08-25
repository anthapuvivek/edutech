package com.learntrix.edtech.dto.rbac;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Mirrors the frontend StaffMember in src/types/ops.ts. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffMemberResponse {
    private UUID id;
    private String name;
    private String email;
    /** Lower-cased role key — matches PlatformRole on the client. */
    private String role;
    /** "active" | "inactive" | "suspended" */
    private String status;
    private int assignedCount;
    private Instant createdAt;
}
