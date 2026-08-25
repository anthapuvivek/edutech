import { env } from "@/lib/env";
import { mockRoleDefinitions, mockStaff } from "@/mock/ops";
import { apiRequest, mockDelay } from "@/services/api-client";
import type { Permission, PlatformRole, RoleDefinition, StaffMember } from "@/types/ops";

/**
 * RBAC abstraction.
 * The UI uses these helpers to hide or disable controls, but authorisation is
 * always re-enforced by the backend on every request. Hiding navigation is
 * never treated as a security control.
 */

/**
 * The active role → permission table.
 *
 * Seeded from the mock definitions so the synchronous `can()` call sites keep working
 * before the network answers, then replaced with the server's own table once
 * `GET /rbac/roles` resolves (see <AuthProvider>). Backend migration V20 seeds
 * role_permissions from the same source, so the two agree.
 */
let roleDefinitions: RoleDefinition[] = mockRoleDefinitions;

/** Installs the authoritative table fetched from the API. Ignores an empty response. */
export function setRoleDefinitions(definitions: RoleDefinition[]): void {
  if (definitions.length > 0) roleDefinitions = definitions;
}

export const permissionsService = {
  async roles(): Promise<RoleDefinition[]> {
    if (!env.useMocks) return apiRequest<RoleDefinition[]>("/rbac/roles");
    return mockDelay(mockRoleDefinitions);
  },

  async staff(role?: PlatformRole | "all"): Promise<StaffMember[]> {
    if (!env.useMocks) return apiRequest<StaffMember[]>("/rbac/staff", { query: { role } });
    return mockDelay(role && role !== "all" ? mockStaff.filter((s) => s.role === role) : mockStaff);
  },

  async updateStaffStatus(id: string, status: StaffMember["status"]): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>(`/rbac/staff/${id}/status`, { method: "PATCH", body: { status } });
    const member = mockStaff.find((s) => s.id === id);
    if (member) member.status = status;
    return mockDelay(undefined, 150);
  },

  async updateRolePermissions(role: PlatformRole, permissions: Permission[]): Promise<void> {
    if (!env.useMocks) {
      await apiRequest<void>(`/rbac/roles/${role}`, { method: "PATCH", body: { permissions } });
      // Keep the local table in step so the change takes effect without a reload.
      setRoleDefinitions(
        roleDefinitions.map((def) => (def.role === role ? { ...def, permissions } : def)),
      );
      return;
    }
    const def = mockRoleDefinitions.find((r) => r.role === role);
    if (def) def.permissions = permissions;
    return mockDelay(undefined, 150);
  },
};

export function permissionsFor(role: PlatformRole): Permission[] {
  return roleDefinitions.find((r) => r.role === role)?.permissions ?? [];
}

export function can(role: PlatformRole | undefined, permission: Permission): boolean {
  if (!role) return false;
  if (role === "super_admin") return true;
  return permissionsFor(role).includes(permission);
}
