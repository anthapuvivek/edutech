import { env } from "@/lib/env";
import { apiRequest, mockDelay } from "@/services/api-client";
import type { Announcement, CreateAnnouncementPayload } from "@/types/announcement";

/**
 * Announcement service.
 *
 * Batch scoping and read state are both server-side: the student list returns only what
 * their course and batch allow, and the read row is keyed on the authenticated student.
 */
export const announcementService = {
  // ---------------- teacher ----------------

  async teacherAnnouncements(): Promise<Announcement[]> {
    if (!env.useMocks) return apiRequest<Announcement[]>("/teacher/announcements");
    return mockDelay([]);
  },

  async create(payload: CreateAnnouncementPayload): Promise<Announcement> {
    if (!env.useMocks)
      return apiRequest<Announcement>("/teacher/announcements", { method: "POST", body: payload });
    return mockDelay({ ...payload, id: `an-${Date.now()}` } as unknown as Announcement);
  },

  async update(id: string, payload: CreateAnnouncementPayload): Promise<Announcement> {
    if (!env.useMocks)
      return apiRequest<Announcement>(`/teacher/announcements/${id}`, {
        method: "PUT",
        body: payload,
      });
    return mockDelay({ ...payload, id } as unknown as Announcement);
  },

  async remove(id: string): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>(`/teacher/announcements/${id}`, { method: "DELETE" });
    return mockDelay(undefined as void);
  },

  // ---------------- student ----------------

  async studentAnnouncements(): Promise<Announcement[]> {
    if (!env.useMocks) return apiRequest<Announcement[]>("/student/announcements");
    return mockDelay([]);
  },

  /** Drives the unread badge. */
  async unreadCount(): Promise<{ unread: number }> {
    if (!env.useMocks) return apiRequest<{ unread: number }>("/student/announcements/unread-count");
    return mockDelay({ unread: 0 });
  },

  /** Idempotent — re-reading does not add another row. */
  async markRead(id: string): Promise<Announcement> {
    if (!env.useMocks)
      return apiRequest<Announcement>(`/student/announcements/${id}/read`, { method: "PUT" });
    return mockDelay({} as Announcement);
  },
};
