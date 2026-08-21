/**
 * Service-layer authorization guard.
 *
 * Hiding navigation is never treated as a security control. Every privileged
 * service call runs through `assertPermission`, which fails closed with a 403
 * before any request is issued — and the same permission name is sent to (and
 * re-checked by) the FastAPI backend once mocks are switched off. The frontend
 * check is defence in depth, not the authority.
 */
import { authService } from "@/services/auth.service";
import { ApiError } from "@/services/api-client";
import { can } from "@/services/permissions.service";
import type { Permission, PlatformRole } from "@/types/ops";

export function currentRole(): PlatformRole | undefined {
  return authService.readSession()?.user.role as PlatformRole | undefined;
}

export function assertPermission(permission: Permission): void {
  const role = currentRole();
  if (!role) throw new ApiError(401, "Your session has expired. Please sign in again.");
  if (!can(role, permission)) {
    throw new ApiError(403, `Your role (${role.replace(/_/g, " ")}) is not authorized for this action.`);
  }
}

/**
 * Wraps a service object so every method asserts a permission before running.
 * Method-specific overrides win over the module default.
 */
export function guardService<T extends object>(
  service: T,
  defaultPermission: Permission,
  overrides: Partial<Record<keyof T, Permission>> = {},
): T {
  return new Proxy(service, {
    get(target, prop, receiver) {
      const value = Reflect.get(target, prop, receiver);
      if (typeof value !== "function") return value;
      const permission = (overrides as Record<string, Permission | undefined>)[prop as string] ?? defaultPermission;
      return (...args: unknown[]) => {
        assertPermission(permission);
        return (value as unknown as (...a: unknown[]) => unknown).apply(target, args);
      };
    },
  }) as T;
}
