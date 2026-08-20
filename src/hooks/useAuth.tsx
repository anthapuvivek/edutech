import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import { authService } from "@/services/auth.service";
import type { AuthSession, AuthUser, LoginPayload, RegisterPayload } from "@/types/lms";

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  /** false until the stored session has been read on the client. */
  isReady: boolean;
  login: (payload: LoginPayload) => Promise<AuthUser>;
  register: (payload: RegisterPayload) => Promise<AuthUser>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [isReady, setReady] = useState(false);

  useEffect(() => {
    setSession(authService.readSession());
    setReady(true);
  }, []);

  const apply = useCallback((next: AuthSession) => {
    authService.persist(next);
    setSession(next);
    return next.user;
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user: session?.user ?? null,
      isAuthenticated: Boolean(session),
      isReady,
      login: async (payload) => apply(await authService.login(payload)),
      register: async (payload) => apply(await authService.register(payload)),
      logout: async () => {
        await authService.logout();
        setSession(null);
      },
    }),
    [session, isReady, apply],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}
