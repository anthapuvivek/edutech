import { env } from "@/lib/env";
import { mockAdminStats, mockEvents } from "@/mock/lms";
import { apiRequest, mockDelay } from "@/services/api-client";
import type { AdminStats, PlatformEvent } from "@/types/lms";

export const adminService = {
  async stats(): Promise<AdminStats> {
    if (!env.useMocks) return apiRequest<AdminStats>("/admin/stats");
    return mockDelay(mockAdminStats);
  },
  async events(): Promise<PlatformEvent[]> {
    if (!env.useMocks) return apiRequest<PlatformEvent[]>("/admin/events");
    return mockDelay(mockEvents);
  },
};
