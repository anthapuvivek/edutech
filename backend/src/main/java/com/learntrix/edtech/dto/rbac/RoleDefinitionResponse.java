package com.learntrix.edtech.dto.rbac;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Mirrors the frontend RoleDefinition in src/types/ops.ts. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDefinitionResponse {
    /** Lower-cased role key, e.g. "placement_officer" — matches PlatformRole on the client. */
    private String role;
    private String label;
    private String description;
    private List<String> permissions;
}
