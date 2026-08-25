import { env } from "@/lib/env";
import { mockAccounts } from "@/mock/lms";
import { apiRequest, mockDelay, SESSION_STORAGE_KEY } from "@/services/api-client";
import type { AuthSession, AuthUser, LoginPayload, RegisterPayload, Role } from "@/types/lms";

const STORAGE_KEY = SESSION_STORAGE_KEY;

/**
 * Auth abstraction over the Spring Boot backend, with a mock branch for offline UI work.
 * NOTE: this is a UX-level session only. Real authorization is enforced server side.
 */
export const authService = {
  readSession(): AuthSession | null {
    if (typeof window === "undefined") return null;
    try {
      const raw = window.localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const session = JSON.parse(raw) as AuthSession;
      if (Date.parse(session.expiresAt) < Date.now()) return null;
      return session;
    } catch {
      return null;
    }
  },

  persist(session: AuthSession | null) {
    if (typeof window === "undefined") return;
    if (session) window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    else window.localStorage.removeItem(STORAGE_KEY);
  },

  async login(payload: LoginPayload): Promise<AuthSession> {
    if (!env.useMocks)
      return apiRequest<AuthSession>("/auth/login", { method: "POST", body: payload });

    const account = mockAccounts.find(
      (a) => a.email.toLowerCase() === payload.email.trim().toLowerCase(),
    );
    if (!account || payload.password.length < 4) {
      await mockDelay(null, 400);
      throw new Error("Invalid email or password.");
    }
    const { password: _password, ...user } = account;
    return mockDelay(buildSession(user, payload.remember ? 30 : 1), 500);
  },

  async register(payload: RegisterPayload): Promise<AuthSession> {
    if (!env.useMocks)
      return apiRequest<AuthSession>("/auth/register", { method: "POST", body: payload });

    const user: AuthUser = {
      id: `u-${Date.now()}`,
      studentId: `LTX-2026-${Math.floor(1000 + Math.random() * 8999)}`,
      name: payload.name,
      email: payload.email,
      role: "student",
      status: "active",
      createdAt: new Date().toISOString(),
    };
    return mockDelay(buildSession(user, 1), 600);
  },

  async requestPasswordReset(email: string): Promise<{ sent: boolean }> {
    if (!env.useMocks)
      return apiRequest("/auth/forgot-password", { method: "POST", body: { email } });
    return mockDelay({ sent: true }, 500);
  },

  async resetPassword(token: string, password: string): Promise<{ ok: boolean }> {
    if (!env.useMocks)
      return apiRequest("/auth/reset-password", { method: "POST", body: { token, password } });
    return mockDelay({ ok: true }, 500);
  },

  async verifyEmail(token: string): Promise<{ verified: boolean }> {
    if (!env.useMocks) return apiRequest("/auth/verify-email", { method: "POST", body: { token } });
    return mockDelay({ verified: true }, 800);
  },

  /**
   * Re-reads the signed-in user from the API. The stored session is only a cache —
   * this is what proves the token is still valid, so it runs on every app start.
   */
  async profile(): Promise<AuthUser> {
    if (!env.useMocks) return apiRequest<AuthUser>("/auth/profile");
    const session = this.readSession();
    if (!session) throw new Error("No active session.");
    return mockDelay(session.user, 150);
  },

  async logout(): Promise<void> {
    try {
      if (!env.useMocks) await apiRequest<void>("/auth/logout", { method: "POST" });
    } catch {
      // Server-side logout is best effort. An already-expired token 401s here, and that
      // must never strand the user inside a signed-in shell.
    } finally {
      this.persist(null);
    }
  },
};

function buildSession(user: AuthUser, days: number): AuthSession {
  return {
    user,
    accessToken: `mock.${user.id}.${Date.now()}`,
    expiresAt: new Date(Date.now() + days * 86_400_000).toISOString(),
  };
}

export const roleHome: Record<Role, string> = {
  student: "/student/dashboard",
  teacher: "/teacher/dashboard",
  admin: "/admin/dashboard",
  super_admin: "/admin/dashboard",
  mentor: "/teacher/students",
  placement_officer: "/admin/career/jobs",
  support_agent: "/admin/support",
  counsellor: "/admin/crm",
};
