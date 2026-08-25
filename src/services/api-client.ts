import { env } from "@/lib/env";

/**
 * Thin REST client for the Spring Boot backend.
 * Services call this; components never call fetch directly.
 */
export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public details?: unknown,
    /** Backend error code, e.g. "AUTH_TOKEN_INVALID" or "VALIDATION_FAILED". */
    public code?: string,
    /** Per-field messages from a failed @Valid check, keyed by field name. */
    public fieldErrors?: Record<string, string>,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

type RequestOptions = {
  method?: "GET" | "POST" | "PATCH" | "PUT" | "DELETE";
  body?: unknown;
  query?: Record<string, string | number | boolean | undefined>;
  signal?: AbortSignal;
};

/** Where the UX-level session is cached. Owned here so the 401 path can clear it. */
export const SESSION_STORAGE_KEY = "learntrix.session";

/** Fired after a 401 clears the stored session, so <AuthProvider> can react. */
export const UNAUTHORIZED_EVENT = "learntrix:unauthorized";

/** Shape of the backend's ApiErrorResponse (common/response/ApiErrorResponse.java). */
type BackendError = {
  error?: {
    code?: string;
    message?: string;
    fieldErrors?: Record<string, string>;
  };
};

function buildUrl(path: string, query?: RequestOptions["query"]) {
  const url = `${env.apiUrl.replace(/\/$/, "")}/${path.replace(/^\//, "")}`;
  if (!query) return url;
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== "") params.set(key, String(value));
  }
  const qs = params.toString();
  return qs ? `${url}?${qs}` : url;
}

/**
 * Bearer header for the current session. Exported because the video upload has to
 * bypass this client (it streams a File via XHR to report progress) but still needs
 * to authenticate against the local upload endpoint.
 */
export function authHeaders(): Record<string, string> {
  if (typeof window === "undefined") return {};
  try {
    const raw = window.localStorage.getItem(SESSION_STORAGE_KEY);
    if (!raw) return {};
    const token = (JSON.parse(raw) as { accessToken?: string }).accessToken;
    return token ? { Authorization: `Bearer ${token}` } : {};
  } catch {
    return {};
  }
}

function clearStoredSession() {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(SESSION_STORAGE_KEY);
  window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT));
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, query, signal } = options;
  const response = await fetch(buildUrl(path, query), {
    method,
    signal: signal ?? null,
    // The token lets the API re-verify the caller's role on every request —
    // the backend, not the UI, is the authority on permissions.
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: body === undefined ? null : JSON.stringify(body),
  });

  if (!response.ok) {
    let details: unknown;
    try {
      details = await response.json();
    } catch {
      details = undefined;
    }

    const backendError = (details as BackendError | undefined)?.error;

    // A dead or missing token can never be recovered by retrying. Drop the stored
    // session so the UI stops rendering a signed-in shell over failing requests.
    if (response.status === 401) clearStoredSession();

    throw new ApiError(
      response.status,
      backendError?.message ?? `Request failed with status ${response.status}`,
      details,
      backendError?.code,
      backendError?.fieldErrors,
    );
  }

  if (response.status === 204) return undefined as T;
  const json = await response.json();
  // Unwrap the ApiResponse envelope. Keyed on "success" rather than "data" so a null
  // payload still unwraps to null instead of handing back the envelope itself.
  if (json && typeof json === "object" && "success" in json) {
    return (json as { data: T }).data;
  }
  return json as T;
}

/** Simulates network latency for mock services so loading states are exercised. */
export function mockDelay<T>(data: T, ms = 350): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(data), ms));
}
