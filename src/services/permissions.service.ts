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
    if (!env.useMocks)
      return apiRequest<void>(`/rbac/roles/${role}`, { method: "PATCH", body: { permissions } });
    const def = mockRoleDefinitions.find((r) => r.role === role);
    if (def) def.permissions = permissions;
    return mockDelay(undefined, 150);
  },
};

export function permissionsFor(role: PlatformRole): Permission[] {
  return mockRoleDefinitions.find((r) => r.role === role)?.permissions ?? [];
}

export function can(role: PlatformRole | undefined, permission: Permission): boolean {
  if (!role) return false;
  if (role === "super_admin") return true;
  return permissionsFor(role).includes(permission);
}
