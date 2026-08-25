import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

import { UNAUTHORIZED_EVENT } from "@/services/api-client";
import { authService } from "@/services/auth.service";
import { permissionsService, setRoleDefinitions } from "@/services/permissions.service";
import type { AuthSession, AuthUser, LoginPayload, RegisterPayload } from "@/types/lms";

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  /** false until the stored session has been read and revalidated against the API. */
  isReady: boolean;
  login: (payload: LoginPayload) => Promise<AuthUser>;
  register: (payload: RegisterPayload) => Promise<AuthUser>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/** Portal areas that are meaningless without a session, so a 401 there must bounce to login. */
const PROTECTED_PREFIXES = ["/student", "/teacher", "/admin"];

function isOnProtectedRoute() {
  if (typeof window === "undefined") return false;
  return PROTECTED_PREFIXES.some((prefix) => window.location.pathname.startsWith(prefix));
}

/**
 * Pulls the authoritative role → permission table so the UI stops relying on its
 * bundled copy. Failure is non-fatal: the seeded table stays in place.
 */
async function loadRoleDefinitions() {
  try {
    setRoleDefinitions(await permissionsService.roles());
  } catch {
    // Non-fatal — `can()` keeps using the table it was seeded with.
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [isReady, setReady] = useState(false);

  // Revalidate the cached session against the API before trusting it. localStorage only
  // says a token existed, never that it is still good — a revoked or expired token would
  // otherwise render a fully signed-in shell over requests that all fail.
  useEffect(() => {
    let cancelled = false;

    const stored = authService.readSession();
    if (!stored) {
      setReady(true);
      return;
    }

    // Show the cached session immediately; correct it once the API answers.
    setSession(stored);

    void (async () => {
      try {
        const user = await authService.profile();
        if (cancelled) return;
        const verified: AuthSession = { ...stored, user };
        authService.persist(verified);
        setSession(verified);
        await loadRoleDefinitions();
      } catch {
        if (cancelled) return;
        authService.persist(null);
        setSession(null);
      } finally {
        if (!cancelled) setReady(true);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  // A 401 anywhere in the app has already cleared storage; mirror that into React state.
  useEffect(() => {
    if (typeof window === "undefined") return;
    const onUnauthorized = () => {
      setSession(null);
      if (isOnProtectedRoute()) window.location.assign("/login");
    };
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
  }, []);

  const apply = useCallback((next: AuthSession) => {
    authService.persist(next);
    setSession(next);
    void loadRoleDefinitions();
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
