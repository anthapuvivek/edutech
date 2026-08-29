import { env } from "@/lib/env";
import {
  mockActivity,
  mockEnrollments,
  mockEvents,
  mockLeaderboard,
  mockStudentProfile,
  mockStudentStats,
} from "@/mock/lms";
import { apiRequest, mockDelay } from "@/services/api-client";
import type {
  ActivityItem,
  Enrollment,
  LeaderboardEntry,
  PlatformEvent,
  StudentProfile,
  StudentStats,
} from "@/types/lms";

export const studentService = {
  async profile(): Promise<StudentProfile> {
    if (!env.useMocks) return apiRequest<StudentProfile>("/student/profile");
    return mockDelay(mockStudentProfile);
  },
  async stats(): Promise<StudentStats> {
    if (!env.useMocks) return apiRequest<StudentStats>("/student/stats");
    return mockDelay(mockStudentStats);
  },
  async enrollments(): Promise<Enrollment[]> {
    if (!env.useMocks) return apiRequest<Enrollment[]>("/student/enrollments");
    return mockDelay(mockEnrollments);
  },
  async activity(): Promise<ActivityItem[]> {
    if (!env.useMocks) return apiRequest<ActivityItem[]>("/student/activity");
    return mockDelay(mockActivity);
  },
  async liveClasses(): Promise<PlatformEvent[]> {
    if (!env.useMocks) return apiRequest<PlatformEvent[]>("/student/live-classes");
    return mockDelay(mockEvents);
  },
  async leaderboard(scope = "global"): Promise<LeaderboardEntry[]> {
    if (!env.useMocks)
      return apiRequest<LeaderboardEntry[]>("/student/leaderboard", { query: { scope } });
    return mockDelay(mockLeaderboard);
  },

  async assignments(): Promise<any[]> {
    if (!env.useMocks) return apiRequest<any[]>("/student/assignments");
    return mockDelay([]);
  },

  async assignment(id: string): Promise<any> {
    if (!env.useMocks) return apiRequest(`/student/assignments/${id}`);
    return mockDelay(null);
  },

  async submitAssignment(id: string, payload: Record<string, unknown>): Promise<any> {
    if (!env.useMocks) return apiRequest(`/student/assignments/${id}/submit`, { method: "POST", body: payload });
    return mockDelay({ id: `sub-${Date.now()}`, ...payload });
  },

  async quizzes(): Promise<any[]> {
    if (!env.useMocks) return apiRequest<any[]>("/student/quizzes");
    return mockDelay([]);
  },

  async quiz(id: string): Promise<any> {
    if (!env.useMocks) return apiRequest(`/student/quizzes/${id}`);
    return mockDelay(null);
  },

  async attemptQuiz(id: string, payload: Record<string, unknown>): Promise<any> {
    if (!env.useMocks) return apiRequest(`/student/quizzes/${id}/attempt`, { method: "POST", body: payload });
    return mockDelay({ id: `att-${Date.now()}`, ...payload });
  },

  async quizResults(id: string): Promise<any> {
    if (!env.useMocks) return apiRequest(`/student/quizzes/${id}/results`);
    return mockDelay(null);
  },

  async materials(): Promise<any[]> {
    if (!env.useMocks) return apiRequest<any[]>("/student/materials");
    return mockDelay([]);
  },

  async material(id: string): Promise<any> {
    if (!env.useMocks) return apiRequest(`/student/materials/${id}`);
    return mockDelay(null);
  },

  async announcements(): Promise<any[]> {
    if (!env.useMocks) return apiRequest<any[]>("/student/announcements");
    return mockDelay([]);
  },

  async announcement(id: string): Promise<any> {
    if (!env.useMocks) return apiRequest(`/student/announcements/${id}`);
    return mockDelay(null);
  },

  async recordings(courseId?: string): Promise<any[]> {
    if (!env.useMocks) return apiRequest<any[]>("/student/recordings", { query: { courseId } });
    return mockDelay([]);
  },

  async recording(id: string): Promise<any> {
    if (!env.useMocks) return apiRequest(`/student/recordings/${id}`);
    return mockDelay(null);
  },
};
