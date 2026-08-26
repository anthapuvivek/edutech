import { env } from "@/lib/env";
import { mockEvents, mockTeacherStats, mockTeacherStudents } from "@/mock/lms";
import { apiRequest, mockDelay } from "@/services/api-client";
import type {
  CreateLiveClassPayload,
  PlatformEvent,
  TeacherStats,
  TeacherStudentRow,
  UpdateLiveClassPayload,
} from "@/types/lms";

const STORAGE_KEY = "learntrix_teacher_live_classes";

function getStoredMockEvents(): PlatformEvent[] {
  if (typeof window === "undefined") return [...mockEvents];
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(mockEvents));
      return [...mockEvents];
    }
    return JSON.parse(raw) as PlatformEvent[];
  } catch {
    return [...mockEvents];
  }
}

function saveStoredMockEvents(events: PlatformEvent[]) {
  if (typeof window === "undefined") return;
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(events));
  } catch {
    // ignore storage quota errors
  }
}

export const teacherService = {
  async stats(): Promise<TeacherStats> {
    if (!env.useMocks) return apiRequest<TeacherStats>("/teacher/stats");
    return mockDelay(mockTeacherStats);
  },
  async students(courseTitle?: string): Promise<TeacherStudentRow[]> {
    if (!env.useMocks)
      return apiRequest<TeacherStudentRow[]>("/teacher/students", {
        query: { course: courseTitle },
      });
    const rows =
      courseTitle && courseTitle !== "All"
        ? mockTeacherStudents.filter((s) => s.courseTitle === courseTitle)
        : mockTeacherStudents;
    return mockDelay(rows);
  },
  async student(id: string): Promise<TeacherStudentRow | null> {
    if (!env.useMocks) return apiRequest<TeacherStudentRow | null>(`/teacher/students/${id}`);
    return mockDelay(mockTeacherStudents.find((s) => s.id === id) ?? null);
  },

  async liveClasses(): Promise<PlatformEvent[]> {
    if (!env.useMocks) return apiRequest<PlatformEvent[]>("/teacher/live-classes");
    return mockDelay(getStoredMockEvents());
  },

  async liveClass(id: string): Promise<PlatformEvent | null> {
    if (!env.useMocks) return apiRequest<PlatformEvent | null>(`/teacher/live-classes/${id}`);
    const list = getStoredMockEvents();
    return mockDelay(list.find((e) => e.id === id) ?? null);
  },

  async createLiveClass(payload: CreateLiveClassPayload): Promise<PlatformEvent> {
    if (!env.useMocks) {
      return apiRequest<PlatformEvent>("/teacher/live-classes", {
        method: "POST",
        body: payload,
      });
    }

    const list = getStoredMockEvents();
    const newClass: PlatformEvent = {
      id: `ev-${Date.now()}`,
      title: payload.title,
      courseId: payload.courseId,
      courseTitle: payload.courseTitle || "Generative AI & LLM Systems",
      batchId: payload.batchId,
      trainerName: payload.trainerName || "Durga Prasad",
      description: payload.description || "",
      date: payload.date,
      startTime: payload.startTime,
      endTime: payload.endTime,
      platform: payload.platform || "Google Meet",
      meetingUrl: payload.meetingUrl || "",
      type: payload.type || "Regular Class",
      visibility: payload.visibility || "restricted",
      published: payload.published ?? true,
      status: payload.status || "Upcoming",
      registrations: 0,
    };

    const updated = [newClass, ...list];
    saveStoredMockEvents(updated);
    return mockDelay(newClass);
  },

  async updateLiveClass(id: string, payload: UpdateLiveClassPayload): Promise<PlatformEvent> {
    if (!env.useMocks) {
      return apiRequest<PlatformEvent>(`/teacher/live-classes/${id}`, {
        method: "PUT",
        body: payload,
      });
    }

    const list = getStoredMockEvents();
    const index = list.findIndex((e) => e.id === id);
    if (index === -1) {
      throw new Error("Class not found");
    }

    const existing = list[index]!;
    const updatedClass: PlatformEvent = {
      ...existing,
      ...payload,
      trainerName: payload.trainerName ?? existing.trainerName,
      title: payload.title ?? existing.title,
      courseTitle: payload.courseTitle ?? existing.courseTitle,
      date: payload.date ?? existing.date,
      startTime: payload.startTime ?? existing.startTime,
      endTime: payload.endTime ?? existing.endTime,
      platform: payload.platform ?? existing.platform,
      meetingUrl: payload.meetingUrl ?? existing.meetingUrl,
      type: payload.type ?? existing.type,
      description: payload.description ?? existing.description,
      visibility: payload.visibility ?? existing.visibility,
      published: payload.published ?? existing.published,
      status: payload.status ?? existing.status,
    };

    list[index] = updatedClass;
    saveStoredMockEvents(list);
    return mockDelay(updatedClass);
  },

  async cancelLiveClass(id: string): Promise<PlatformEvent> {
    if (!env.useMocks) {
      return apiRequest<PlatformEvent>(`/teacher/live-classes/${id}/cancel`, {
        method: "PATCH",
      });
    }

    const list = getStoredMockEvents();
    const target = list.find((e) => e.id === id);
    if (!target) throw new Error("Class not found");
    target.status = "Cancelled";
    saveStoredMockEvents(list);
    return mockDelay({ ...target });
  },

  async deleteLiveClass(id: string): Promise<void> {
    if (!env.useMocks) {
      return apiRequest<void>(`/teacher/live-classes/${id}`, {
        method: "DELETE",
      });
    }

    const list = getStoredMockEvents();
    const filtered = list.filter((e) => e.id !== id);
    saveStoredMockEvents(filtered);
    return mockDelay(undefined);
  },

  async togglePublish(id: string, published: boolean): Promise<PlatformEvent> {
    return this.updateLiveClass(id, { published });
  },
};

