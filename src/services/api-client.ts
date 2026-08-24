import { env } from "@/lib/env";

/**
 * Thin REST client for the future FastAPI backend.
 * Services call this; components never call fetch directly.
 */
export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public details?: unknown,
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

function authHeaders(): Record<string, string> {
  if (typeof window === "undefined") return {};
  try {
    const raw = window.localStorage.getItem("learntrix.session");
    if (!raw) return {};
    const token = (JSON.parse(raw) as { accessToken?: string }).accessToken;
    return token ? { Authorization: `Bearer ${token}` } : {};
  } catch {
    return {};
  }
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, query, signal } = options;
  const response = await fetch(buildUrl(path, query), {
    method,
    signal: signal ?? null,
    credentials: "include",
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
    throw new ApiError(response.status, `Request failed with status ${response.status}`, details);
  }

  if (response.status === 204) return undefined as T;
  const json = await response.json();
  if (json && typeof json === "object" && "success" in json && "data" in json) {
    return json.data as T;
  }
  return json as T;
}

/** Simulates network latency for mock services so loading states are exercised. */
export function mockDelay<T>(data: T, ms = 350): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(data), ms));
}
