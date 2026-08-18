/**
 * Public runtime configuration. Only ever expose non-secret values here.
 * Secrets (AWS, AI providers, payment gateways) live exclusively on the backend.
 */
export const env = {
  apiUrl: import.meta.env["VITE_API_URL"] ?? "/api",
  appUrl: import.meta.env["VITE_APP_URL"] ?? "",
  cdnUrl: import.meta.env["VITE_CDN_URL"] ?? "",
  useMocks: (import.meta.env["VITE_USE_MOCKS"] ?? "true") === "true",
};
